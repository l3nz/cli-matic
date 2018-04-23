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

(defn parse-cmds
  [cmdline config]

  (let [cli-top-options (rewrite-opts config nil)
        ;_ (prn "Options" cli-top-options)
        parsed-common-args (parse-opts cmdline cli-top-options :in-order true)
        ;_ (prn "Common cmdline" parsed-common-cmdline)
        parse-errors-common (:errors parsed-common-args)
        opts-common (:options parsed-common-args)]

    (if (nil? parse-errors-common)

      (let [subcommand (first (:arguments parsed-common-args))
            subcommand-parms (vec (rest (:arguments parsed-common-args)))
            cli-subcmd-options (rewrite-opts config subcommand)
            fnToCall (:runs (get-subcommand config subcommand))
            parsed-subcmd-args (parse-opts subcommand-parms cli-subcmd-options)
            ;_ (prn "Subcmd cmdline" parsed-subcmd-cmdline)
            parse-errors-subcmd (:errors parsed-subcmd-args)
            opts-subcmd (:options parsed-subcmd-args)]

        (if (nil? parse-errors-subcmd)

          {:subcommand     subcommand
           :subcommand-def (get-subcommand config subcommand)
           :commandline     (into
                              (into opts-common opts-subcmd)
                              {:_arguments (:arguments parsed-subcmd-args)})
           :parse-errors    :NONE
           :error-text     ""
           }


          (exception (str "Parse error subcmd" parse-errors-subcmd))

          ))


      (exception (str "Err" parse-errors-common))
      )))



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