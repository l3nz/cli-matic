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
  (:require [cli-matic.core :refer [run-cmd]]))

; Custom help text generation

(defn header
  [cfg]
  (str "=- "
       (get-in cfg [:app :command])
       " command help"
       " -="))

(defn commands-segment
  [cfg]
  (str "Commands:\n"
       (clojure.string/join
         "\n"
         (map
           #(format "   %-10s %s"
                    (get % :command)
                    (get % :description))
           (cfg :commands)))))

(def footer
  (str "Thank you for looking."))

(defn my-global-help
  "Generates custom help text for the command in general.
  This function takes cli-matic's configuration as its argument.
  cli-matic will print the string returned from this function verbatim."
  [cfg]
  (clojure.string/join
    "\n\n"
    [(header cfg)
     (commands-segment cfg)
     footer]))

(defn sub-cmd-entry
  [cfg sub-cmd]
  (first
    (filter #(= sub-cmd (get % :command))
            (cfg :commands))))

(defn my-subcmd-help
  "Generates custom help text for the specified sub-command (2nd function argument)."
  [cfg sub-cmd]
  (let [entry (sub-cmd-entry cfg sub-cmd)]
    (clojure.string/join
      "\n"
      [(str "Specific help for the "
            sub-cmd
            " sub-command of "
            (get-in cfg [:app :command])
            ".")
       (format "This sub-command conveniently %s" (get entry :description))
       (str "You can set the following options:")
       (clojure.string/join
         "\n"
         (map
           #(format "  --%s ... %s"
                    (get % :option)
                    (get % :as))
           (entry :opts)))])))

; Commands

(defn echo-message
  [{:keys [message]}]
  (println message))

(def cli-configuration
  {:app         {:command      "helpgen"
                 :description  "Demonstrates how to customize the generation of help text"
                 :global-help  my-global-help
                 :subcmd-help  my-subcmd-help
                 :version      "0.1.18"}
   :global-opts []
   :commands    [{:command     "echo"
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
