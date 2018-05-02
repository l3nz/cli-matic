(ns cli-matic.core
  (:require [cli-matic.specs :as S]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]
            [cli-matic.presets :as PRESETS]))

;; Cli-matic has one main entry-point: run!
;; Actually, most of the logic will be run in run*
;; to make testing easier.
(defn assoc-new-multivalue
  "associates a new multiple value to the
  current parameter map.
  If the current value is not a vector, creates
  a new vector with the new value."
  [parameter-map option v]
  (let [curr-val (get parameter-map option [])
        new-val (if (vector? curr-val)
                  (conj curr-val v)
                  [v])]
    (assoc parameter-map option new-val)))

;; Rewrite options to our format
;; {:opt "x" :as "Port number" :type :int}
;; ["-x" nil "Port number"
;;  :parse-fn #(Integer/parseInt %)]

(defn mk-cli-option
  [{:keys [option shortened as type default multiple] :as cm-option}]
  (let [preset (get PRESETS/known-presets type :unknown)
        positional-opts [(if (string? shortened)
                           (str "-" shortened)
                           nil)
                         (str "--" option " " (:placeholder preset))
                         as]
        ;; step 1 - remove :placeholder
        opts-1 (dissoc preset :placeholder)

        ;; step 2 - add default if present
        opts-2 (if (some? default)
                 (assoc opts-1 :default default)
                 opts-1)
        ;; step 3 - if multivalue, add correct assoc-fns
        opts-3 (if multiple
                 (assoc opts-2 :assoc-fn assoc-new-multivalue)
                 opts-2)]
    (apply
     conj positional-opts
     (flatten (seq opts-3)))))

(s/fdef mk-cli-option
        :args (s/cat :opts ::S/climatic-option)
        :ret some?)

(defn get-subcommand
  [climatic-args subcmd]
  (let [subcommands (:commands climatic-args)]
    (first (filter #(= (:command %) subcmd) subcommands))))

(s/fdef get-subcommand
        :args (s/cat :args ::S/climatic-cfg :subcmd string?)
        :ret ::S/a-command)

(defn all-subcommands
  "Returns all subcommands, as strings"
  [climatic-args]
  (let [subcommands (:commands climatic-args)]
    (into #{}
          (map :command subcommands))))

(s/fdef all-subcommands
        :args (s/cat :args ::S/climatic-cfg)
        :ret set?)

;; Out of a cli-matic arg list,
;; generates a set of commands for tools.cli
(defn rewrite-opts
  [climatic-args subcmd]
  (let [opts (if (nil? subcmd)
               (:global-opts climatic-args)
               (:opts (get-subcommand climatic-args subcmd)))]
    (conj
     (mapv mk-cli-option opts)
     ["-?" "--help" "" :id :_help_trigger]
     )))

(s/fdef rewrite-opts
        :args (s/cat :args some?
                     :mode (s/or :common nil?
                                 :a-subcommand string?))
        :ret some?)


;
; Generate pages
;
;

(defn asString [s]
  (if (string? s)
    s
    (clojure.string/join "\n" s)
    ))


(defn indent-string [s]
  (str " " s))

(defn indent [s]
  (if (string? s)
    (indent-string s)
    (map indent-string s)
    ))

(defn generate-section [title lines]
  (if (empty? lines)
    []

    [(str title ":")
     (indent lines)
     ""]
    ))


(defn generate-sections
  [name version usage commands opts-title opts]

  (vec
    (flatten
         [(generate-section "NAME" name)
          (generate-section "USAGE" usage)
          (generate-section "VERSION" version)
          (generate-section "COMMANDS" commands)
          (generate-section opts-title opts)])))


(defn get-options-summary
  [cfg subcmd]
  (let [cli-cfg (rewrite-opts cfg subcmd)
        options-str (:summary
                       (parse-opts [] cli-cfg))]
    (clojure.string/split-lines options-str)))




(defn generate-global-help [cfg]

  (let [name (get-in cfg [:app :command])
        version (get-in cfg [:app :version])
        descr (get-in cfg [:app :description])
        ]

    (generate-sections
      (str name " - " descr)
      version
      (str name " [global-options] command [command options] [arguments...]")
      (map #(str (:command %) "    " (:description %)) (:commands cfg))
      "GLOBAL OPTIONS"
      (get-options-summary cfg nil)
      )))

(s/fdef
  generate-global-help
  :args (s/cat :cfg ::S/climatic-cfg)
  :ret (s/coll-of string?))




(defn generate-subcmd-help [cfg cmd]

  (let [glname (get-in cfg [:app :command])
        cmd-cfg (get-subcommand cfg cmd )
        name (:command cmd-cfg)
        descr (:description cmd-cfg)]

    (generate-sections
      (str glname " " name " - " descr)
      nil
      (str glname " " name " [command options] [arguments...]")
      nil
      "OPTIONS"
      (get-options-summary cfg cmd)
      )))

(s/fdef
  generate-subcmd-help
  :args (s/cat :cfg ::S/climatic-cfg :cmd ::S/command)
  :ret (s/coll-of string?))



;
; We parse our command line here
;
;

(defn mkError
  [config subcommand error text]
  {:subcommand     subcommand
   :subcommand-def (if (or (= error :ERR-UNKNOWN-SUBCMD)
                           (= error :ERR-NO-SUBCMD)
                           (= error :ERR-PARMS-GLOBAL)
                           (= error :HELP-GLOBAL)
                           )
                     nil
                     (get-subcommand config subcommand))
   :commandline    {}
   :parse-errors   error
   :error-text     (asString text)
   })


(defn parse-cmds
  [cmdline config]

  (let [possible-subcmds (all-subcommands config)

        cli-gl-options (rewrite-opts config nil)
        ;_ (prn "Options" cli-top-options)
        parsed-gl-opts (parse-opts cmdline cli-gl-options :in-order true)
        ;_ (prn "Common cmdline" parsed-common-cmdline)

        {gl-errs :errors gl-opts :options gl-args :arguments} parsed-gl-opts]

    (cond
      (some? gl-errs)
      (mkError config nil :ERR-PARMS-GLOBAL gl-errs)

      (some? (:_help_trigger gl-opts))
      (mkError config nil :HELP-GLOBAL nil)



      :else
      (let [subcommand (first gl-args)
            subcommand-parms (vec (rest gl-args))]

        (cond
          (nil? subcommand)
          (mkError config nil :ERR-NO-SUBCMD "")

          (nil? (possible-subcmds subcommand))
          (mkError config subcommand :ERR-UNKNOWN-SUBCMD "")

          :else
          (let [cli-cmd-options (rewrite-opts config subcommand)
                ;_ (prn "O" cli-cmd-options)
                parsed-cmd-opts (parse-opts subcommand-parms cli-cmd-options)
                ;_ (prn "Subcmd cmdline" parsed-cmd-opts)
                {cmd-errs :errors cmd-opts :options cmd-args :arguments} parsed-cmd-opts]

            (cond

              (some? (:_help_trigger cmd-opts))
              (mkError config subcommand :HELP-SUBCMD nil)


              (nil? cmd-errs)
              {:subcommand     subcommand
               :subcommand-def (get-subcommand config subcommand)
               :commandline     (into
                                  (into gl-opts cmd-opts)
                                  {:_arguments cmd-args})
               :parse-errors    :NONE
               :error-text     ""
               }

              :else
              (mkError config subcommand :ERR-PARMS-SUBCMD cmd-errs)
              )))))))


(s/fdef
  parse-cmds
  :args (s/cat :args (s/coll-of string?)
               :opts ::S/climatic-cfg)
  :ret ::S/lineParseResult
  )


;
; builds a return value
;



(defn ->RV
  [return-code type stdout subcmd stderr]
  (let [fnStrVec (fn [s]
                   (cond
                     (nil? s) []
                     (string? s) [s]
                     :else  s ))]

  {:retval return-code
   :status type
   :help   stdout
   :subcmd subcmd
   :stderr (fnStrVec stderr)}
  ))

(s/fdef
  ->RV
  :args (s/cat :rv int? :status some? :help any? :subcmd any? :stderr any?)
  :rets ::S/RV)



;
; Invokes a subcommand.
;
; The subcommand may:
; - return an integer (to specify exit code)
; - return nil
; - throw a Throwable object
;

(defn invoke-subcmd
  [subcommand-def options]

  (try
    (let [rv ((:runs subcommand-def)  options)]
      (cond
        (nil? rv)    (->RV 0 :OK nil nil nil)
        (zero? rv)   (->RV 0 :OK nil nil nil)
        (int? rv)    (->RV rv :ERR nil nil nil)
        :else        (->RV -1 :ERR nil nil nil)
        ))

    (catch Throwable t
      (->RV -1 :EXCEPTION nil nil
            (str "JVM Exception: "
                 (with-out-str (println t)))))))

;; Executes our code.
;; It will try and parse the arguments via clojure.tools.cli
;; and detect our subcommand.

;; If no subcommand was found, it will print the error reminder.
;; On exceptions, it will raise an exception message.
(defn run-cmd*
  [setup args]
  (let [parsed-opts (parse-cmds args setup)]
    ;; maybe there was an error parsing
    (condp = (:parse-errors parsed-opts)
      :ERR-CFG (->RV -1 :ERR-CFG nil nil  "Error in cli-matic configuration")
      :ERR-NO-SUBCMD (->RV -1 :ERR-NO-SUBCMD :HELP-GLOBAL nil "No sub-command specified")
      :ERR-UNKNOWN-SUBCMD (->RV -1 :ERR-UNKNOWN-SUBCMD :HELP-GLOBAL nil "Unknown sub-command")
      :HELP-GLOBAL (->RV 0 :OK :HELP-GLOBAL nil nil)
      :ERR-PARMS-GLOBAL (->RV -1 :ERR-PARMS-GLOBAL :HELP-GLOBAL nil
                              (str "Global option error: " (:error-text parsed-opts)))
      :HELP-SUBCMD (->RV 0 :OK :HELP-SUBCMD (:subcommand parsed-opts) nil)
      :ERR-PARMS-SUBCMD (->RV -1 :ERR-PARMS-SUBCMD :HELP-SUBCMD (:subcommand parsed-opts)
                              (str "Option error: " (:error-text parsed-opts)))
      :NONE (invoke-subcmd (:subcommand-def parsed-opts) (:commandline parsed-opts)))))

(defn run-cmd
  [args setup]
  (let [{:keys [help stderr subcmd retval]}
        (run-cmd* setup (if (nil? args) [] args))]
    (if (not (empty? stderr))
      (println
       (asString
        (flatten
         [ "** ERROR: **" stderr "" ""]))))
    (cond
      (= :HELP-GLOBAL help)
      (println (asString (generate-global-help setup)))
      (= :HELP-SUBCMD help)
      (println (asString (generate-subcmd-help setup subcmd))))
    (System/exit retval)))

(st/instrument)
