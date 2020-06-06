#!/bin/sh
#_(
#_This runs clj so we use the deps.edn but specify which module 
#_we want to run. 
exec clj -J-Xms256m -J-Xmx256m -J-client  -J-Dclojure.spec.skip-macros=true -i "$0" -m helpgen "$@"
)

; Execute the following from the project root directory to:
;
; Print customized global help:
; clj -i examples/helpgen.clj -m helpgen
;
; Print customized sub-command help:
; clj -i examples/helpgen.clj -m helpgen echo --help
;
; Execute the "echo" sub-command:
; clj -i examples/helpgen.clj -m helpgen echo --message "Eh, what's up, Doc?"

(ns helpgen
  (:require [cli-matic.core :refer [run-cmd]]
            [cli-matic.utils-v2 :as U2]))

; Custom help text generation

(defn my-global-help
  "Generates custom help text for the command in general.
  This function takes cli-matic's configuration as its argument.
  cli-matic will print the string returned from this function verbatim.

  To walk through a CLI-matic config, there are plenty of useful fns in
  cli-matic.utils-v2
  "
  [cfg sub-cmd]
  (let [branch (U2/get-subcommand cfg sub-cmd)]
    (str
     "====== GENERIC 'global' HELP CONFIG: " sub-cmd "\n" 
      branch)))


(defn my-subcmd-help
  "Generates custom help text for the specified sub-command (2nd function argument).

  To walk through a CLI-matic config, there are plenty of useful fns in
  cli-matic.utils-v2
  "
  [cfg sub-cmd]
  (let [leaf (U2/get-subcommand cfg sub-cmd)]
    (str
     "====== SPECIFIC SUBCMD HELP CONFIG: " sub-cmd "\n" 
      leaf)))

; Commands

(defn echo-message
  [{:keys [message]}]
  (println message))

(def cli-configuration
  {:command      "helpgen"
   :description  "Demonstrates how to customize the generation of help text"
   :global-help  my-global-help
   :subcmd-help  my-subcmd-help
   :version      "0.1.18"
   :opts         []
   :subcommands  [{:command     "echo"
                   :description "echoes a message."
                   :opts        [{:option "message"
                                 :as "The message string to echo back."
                                 :type :string
                                 :default "oh-la-la"}]
                   :runs        echo-message}
                 ]})

; Main entry point

(defn -main
  [& args]
  (run-cmd args cli-configuration))
