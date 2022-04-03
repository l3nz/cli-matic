#!/bin/sh
#_(
#_This runs clj so we use the deps.edn but specify which module 
#_we want to run. 
exec clj -J-Xms256m -J-Xmx256m -J-client  -J-Dclojure.spec.skip-macros=true -M -i "$0" -m toycalc "$@"
)

(ns toycalc
  (:require [cli-matic.core :refer [run-cmd]]))

;; To run this, try from the project root:
;; ./toycalc-nosub.clj -a 1 -b 80

(defn add_numbers
  "Sums A and B together, and prints it in base 10"
  [{:keys [a b]}]
  (println
   (Integer/toString (+ a b) 10)))


(def CONFIGURATION
    {:command     "toycalc"
     :description "A command-line toy adder"
     :version     "0.0.1"
     :opts        [{:as      "Parameter A"
                    :default 0
                    :option  "a"
                    :type    :int}
                   {:as      "Parameter B"
                    :default 0
                    :option  "b"
                    :type    :int}]
     :runs        add_numbers})


  
(defn -main
  "This is our entry point.
  Just pass parameters and configuration.
  Commands (functions) will be invoked as appropriate."
  [& args]
  (run-cmd args CONFIGURATION))
