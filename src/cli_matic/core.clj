(ns cli-matic.core
  (:require [cli-matic.specs :as S]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]
            ))

;
; Cli-matic has one main entry-point: run!
; Actually, most of the logic will be run in run*
; to make testing easier.
;

;
; Known presets
;

(defn parseInt [s]
  (Integer/parseInt s))


(def known-presets
  {:int {:parse-fn parseInt
         :placeholder "N"}
   :string {:placeholder "S"}})


;
; Rewrite options to our format
;
; {:opt "x" :as "Port number" :type :int}
; ["-x" nil "Port number"
;     :parse-fn #(Integer/parseInt %)]

(defn mk-cli-option
  [{:keys [option shortened as type] :as cm-option}]
  (let [preset (get known-presets type :unknown)
        head [(if (string? shortened)
                (str "-" shortened)
                nil)
              (str "--" option " " (:placeholder preset))
              as]

        opts  (dissoc preset :placeholder)]

    (apply
      conj head
      (flatten (seq opts)))

    ))


(s/fdef
  mk-cli-option
  :args (s/cat :opts ::S/climatic-option)
  :ret some?)


(defn get-subcommand
  [climatic-args subcmd]
  (let [subcommands (:commands climatic-args)]
    (first (filter #(= (:command %) subcmd) subcommands))))

(s/fdef
  get-subcommand
  :args (s/cat :args ::S/climatic-cfg :subcmd string?)
  :ret ::S/a-command)


(defn all-subcommands
  "Returns all subcommands, as strings"
  [climatic-args]
  (let [subcommands (:commands climatic-args)]
    (into #{}
      (map :command subcommands))))

(s/fdef
  all-subcommands
  :args (s/cat :args ::S/climatic-cfg)
  :ret set?)


;
; Out of a cli-matic arg list,
; generates a set of commands for tools.cli
;

(defn rewrite-opts
  [climatic-args subcmd]

  (let [opts (if (nil? subcmd)
               (:global-opts climatic-args)
               (:opts (get-subcommand climatic-args subcmd)))]
    (map mk-cli-option opts)))


(s/fdef
  rewrite-opts
  :args (s/cat :args some?
               :mode (s/or :common nil?
                           :a-subcommand string?))
  :ret some?)


;
; Generate pages
;
;

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
                           (= error :ERR-PARMS-GLOBAL))
                     nil
                     (get-subcommand config subcommand))
   :commandline    {}
   :parse-errors   error
   :error-text     text
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
      (mkError config nil :ERR-PARMS-GLOBAL "")

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
                parsed-cmd-opts (parse-opts subcommand-parms cli-cmd-options)
                ;_ (prn "Subcmd cmdline" parsed-subcmd-cmdline)
                {cmd-errs :errors cmd-opts :options cmd-args :arguments} parsed-cmd-opts]

            (if (nil? cmd-errs)

              {:subcommand     subcommand
               :subcommand-def (get-subcommand config subcommand)
               :commandline     (into
                                  (into gl-opts cmd-opts)
                                  {:_arguments cmd-args})
               :parse-errors    :NONE
               :error-text     ""
               }
              )

          ))))))


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
      (->RV -1 :EXCEPTION nil nil "**exception**")
      )
    ))


;
; Executes our code.
; It will try and parse the arguments via clojure.tools.cli
; and detect our subcommand.

; If no subcommand was found, it will print the error reminder.
; On exceptions, it will raise an exception message.
;

(defn run-cmd*
  [setup args]

    (let [parsed-opts (parse-cmds args setup)]

      ; maybe there was an error parsing
      (condp = (:parse-errors parsed-opts)

        :ERR-CFG (->RV -1 :ERR-CFG nil nil  "Error in cli-matic configuration")
        :ERR-NO-SUBCMD (->RV -1 :ERR-NO-SUBCMD :HELP-GLOBAL nil "No sub-command specified")
        :ERR-UNKNOWN-SUBCMD (->RV -1 :ERR-UNKNOWN-SUBCMD :HELP-GLOBAL nil "Unknown sub-command")
        :HELP-COMMON (->RV -1 :OK :HELP-GLOBAL nil nil)
        :ERR-PARMS-GLOBAL (->RV -1 :ERR-PARMS-GLOBAL :HELP-GLOBAL nil "Error: ")
        :HELP-SUBCMD (->RV -1 :OK :HELP-SUBCMD nil nil)
        :ERR-PARMS-SUBCMD (->RV -1 :ERR-PARMS-SUBCMD :ERR-SUBCMD nil "Error: ")

        :NONE (invoke-subcmd (:subcommand-def parsed-opts) (:commandline parsed-opts))

        )))


(defn run-cmd
  [args setup]

  (let [result (run-cmd* setup args)
        manual (:help result)
        errmsg (:stderr result)
        subcmd (:subcmd result)]

    (if (some? errmsg)
      (println errmsg))

    (cond
      (= :HELP-GLOBAL manual)
      (println (generate-global-help setup))

      (= :HELP-SUBCMD manual)
      (println (generate-subcmd-help setup subcmd)))

    (System/exit (:retval result))))



(st/instrument)