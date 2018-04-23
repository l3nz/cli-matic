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


(defn exception [s]
  (prn "Exception: " s))

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
  (let [cmd-to-find (if (nil? subcmd)
                      :_common
                      (keyword subcmd))

        cmd-found   (get climatic-args cmd-to-find nil)]

    (if (nil? cmd-found)
      (exception "No subcommand found")
      cmd-found

      )



  ))


(defn all-subcommands
  "Returns all subcommands, as strings"
  [climatic-args]
  (let [just-subcmds (dissoc climatic-args :_common)
        keys-subcmds (keys just-subcmds)
        names-subcmds (map name keys-subcmds)]
    (set names-subcmds)))

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

  (let [cmd-found   (get-subcommand climatic-args subcmd)
        opts        (:opts cmd-found)]
    (map mk-cli-option opts)))


(s/fdef
  rewrite-opts
  :args (s/cat :args some?
               :mode (s/or :common nil?
                           :a-subcommand string?))
  :ret some?)



;
; We parse our command line here
;
;

(defn mkError
  [config subcommand error text]
  {:subcommand     subcommand
   :subcommand-def (get-subcommand config subcommand)
   :commandline    {}
   :parse-errors   error
   :error-text     text
   })


(defn parse-cmds
  [cmdline config]

  (let [possible-subcmds (all-subcommands config)

        cli-cmn-options (rewrite-opts config nil)
        ;_ (prn "Options" cli-top-options)
        parsed-cmn-opts (parse-opts cmdline cli-cmn-options :in-order true)
        ;_ (prn "Common cmdline" parsed-common-cmdline)

        {cmn-errs :errors cmn-opts :options cmn-args :arguments} parsed-cmn-opts]

    (cond
      (some? cmn-errs)
      (mkError config nil :ERR-COMMON "")

      :else
      (let [subcommand (first cmn-args)
            subcommand-parms (vec (rest cmn-args))]

        (cond
          (nil? subcommand)
          (mkError config nil :ERR-NO-SUBCMD "")

          (nil? (possible-subcmds subcommand))
          (mkError config subcommand :ERR-SUBCMD-NOT-FOUND "")

          :else
          (let [cli-scmd-options (rewrite-opts config subcommand)
                parsed-scmd-opts (parse-opts subcommand-parms cli-scmd-options)
                ;_ (prn "Subcmd cmdline" parsed-subcmd-cmdline)
                {scmd-errs :errors scmd-opts :options scmd-args :arguments} parsed-scmd-opts]

            (if (nil? scmd-errs)

              {:subcommand     subcommand
               :subcommand-def (get-subcommand config subcommand)
               :commandline     (into
                                  (into cmn-opts scmd-opts)
                                  {:_arguments scmd-args})
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
; Executes our code.
; It will try and parse the arguments via clojure.tools.cli
; and detect our subcommand.

; If no subcommand was found, it will print the error reminder.
; On exceptions, it will raise an exception message.
;

(defn run-cmd*
  [setup args]

  (try

    (let [cli-options (rewrite-opts setup)
          parsed-args (parse-opts args cli-options)]

      ; maybe there was an error parsing
      (if (some? (:error parsed-args))
        {:retval -1
         :status ::S/PARSE-ERR
         :stdout []
         :stderr []}

        (let [cmd "a"]

          {:retval 0
           :status ::S/OK
           :stdout []
           :stderr []}

          )
      ))



    (catch Throwable t
      {:retval -1
       :status ::S/EXCEPTION
       :stdout ["exc"]
       :stderr []}
    )))


(defn run-cmd
  [args setup]

  (let [result (run-cmd* setup args)]
  ;  (System.out/println  (:stdout result))
  ;  (System.err/println  (:stderr result))
    (System/exit (:retval result))))



(st/instrument)