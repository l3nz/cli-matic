#!/bin/sh
#_(
#_This runs clj so we use the deps.edn but specify which module 
#_we want to run. 
exec clj -J-Xms256m -J-Xmx256m -J-client  -J-Dclojure.spec.skip-macros=true -i "$0" -m toycalc "$@"
)

(ns toycalc
  (:require [cli-matic.core :refer [run-cmd]]))

;; To run this, try from the project root:

;; ./toycalc-nested.clj
;      toycalc - A command-line
;; ./toycalc-nested.clj --help
;      toycalc - A command-line
;; ./toycalc-nested.clj --base 16 add  --a 1 --b 26
;      1b
;; ./toycalc-nested.clj subc sub --a 16 --b 3
;      13
;; ./toycalc-nested.clj --base 16 subc sub --a 16 --b 3
;;     d
;; ./toycalc-nested.clj --base 16 subc sub --a 16 --b 3
;;     d
;; /toycalc-nested.clj --base 16 subc --scale 2 sub --a 16 --b 3
;      1a
;; ./toycalc-nested.clj  subc --scale 2 sub --a 16 --b 3
;      26
;; ./toycalc-nested.clj subc sub --help
;      Parameter A to subtract from
;; ./toycalc-nested.clj subc --help 
;      toycalc subc - Subtracts
;; ./toycalc-nested.clj subc 
;      toycalc subc - Subtracts


(defn add_numbers
  "Sums A and B together, and prints it in base `base`"
  [{:keys [a b base]}]
  (println
   (Integer/toString (+ a b) base)))

(defn subtract_numbers
  "Subtracts B from A, and prints it in base `base` "
  [{:keys [a b base scale]}]
  (println
   (Integer/toString (* scale (- a b)) base)))

(def CONFIGURATION
    {:command     "toycalc"
     :description "A command-line toy calculator"
     :version     "0.0.1"
     :opts        [{:as      "The number base for output"
                    :default 10
                    :option  "base"
                    :type    :int}]
     :subcommands [{:command     "add"
                    :description "Adds two numbers together"
                    :opts        [{:as     "Addendum 1"
                                   :option "a"
                                   :type   :int}
                                  {:as      "Addendum 2"
                                   :default 0
                                   :option  "b"
                                   :type    :int}]
                    :runs        add_numbers}
                   {:command     "subc"
                    :description "Subtracts parameter B from A"
                    :opts        [{:as      "Scale factor for our substraction"
                                   :default 1
                                   :option  "scale"
                                   :short   "s"
                                   :type    :int}]
                    :subcommands [{:command     "sub"
                                   :description "Subtracts"
                                   :opts        [{:as      "Parameter A to subtract from"
                                                  :default 0
                                                  :option  "a"
                                                  :type    :int}
                                                 {:as      "Parameter B"
                                                  :default 0
                                                  :option  "b"
                                                  :type    :int}]
                                   :runs        subtract_numbers}]}]})

  
(defn -main
  "This is our entry point.
  Just pass parameters and configuration.
  Commands (functions) will be invoked as appropriate."
  [& args]
  (run-cmd args CONFIGURATION))
