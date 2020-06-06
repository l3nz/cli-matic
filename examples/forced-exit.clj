#!/bin/sh
#_(
#_This runs clj so we use the deps.edn but specify which module 
#_we want to run. 
exec clj -J-Xms256m -J-Xmx256m -J-client  -J-Dclojure.spec.skip-macros=true -i "$0" -m forcedexit "$@"
)

; Execute the following from the project root directory to:
;

(ns forcedexit
  (:require [cli-matic.core :refer [run-cmd]]
            [cli-matic.utils :as U]))

; Custom help text generation

; Commands

(defn managed-exit
  [_]
  (U/exit! "Yo man" 0))

(defn div-by-zero
  [_]
  (/ 1 0 ))


(def cli-configuration
  {:command      "forcedexit"
   :description  "Shows how to track managed exceptions"
   :version      "1"
   :opts         []
   :subcommands  [{:command     "managed"
                   :description "a managed exception - forced exit"
                   :opts        []
                   :runs        managed-exit}
                   {:command     "exception"
                   :description "a normal exception"
                   :opts        []
                   :runs        div-by-zero}
                 ]})

; Main entry point

(defn -main
  [& args]
  (run-cmd args cli-configuration))
