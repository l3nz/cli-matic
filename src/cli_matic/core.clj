(ns cli-matic.core
  (:require [cli-matic.specs :as S]
            [clojure.tools.cli :refer [parse-opts]]))

;
; Cli-matic has one main entry-point: run!
; Actually, most of the logic will be run in run*
; to make testing easier.
;


(defn rewrite-args [args] args)


;
; We parse our command line here
;
;

(defn parse-cmds [args opts]

  (let [cli-options (rewrite-args opts)
        parsed-args (parse-opts args cli-options)]

    {:options (:options parsed-args)
     :arguments (:arguments parsed-args)
     :subcommand ""
     :errors     :NONE
     }

    )

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

    (let [cli-options (rewrite-args setup)
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
