#!/bin/sh
#_(
#_This runs clj so we use the deps.edn but specify which module 
#_we want to run. 
exec clj -J-Xms256m -J-Xmx256m -J-client  -J-Dclojure.spec.skip-macros=true -M -i "$0" -m toycalc "$@"
)

(ns toycalc
  (:require [cli-matic.core :refer [run-cmd]]))

;; To run this, try from the project root:
;; clj -i examples/toycalc.clj -m toycalc add -a 1 -b 80

(defn add_numbers
  "Sums A and B together, and prints it in base `base`"
  [{:keys [addendum]}]
  (println
   (reduce + 0 addendum)))

(def CONFIGURATION
  {:app         {:command     "toycalc"
                 :description "A command-line toy calculator"
                 :version     "0.0.1"}
   :global-opts []
   :commands    [{:command     "add" :short "a"
                  :description ["Adds all numbers together"]
                  :opts        [{:option "addendum" :short 0 :as "Addendum" :type :int :multiple true}]
                  :runs        add_numbers}
                 ]})

(defn -main
  "This is our entry point.
  Just pass parameters and configuration.
  Commands (functions) will be invoked as appropriate."
  [& args]
  (run-cmd args CONFIGURATION))
