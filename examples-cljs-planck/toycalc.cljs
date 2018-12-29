#!/usr/bin/env bash
"exec" "plk" "-Sdeps" "{:deps {cli-matic {:mvn/version \"0.2.10\"}}}" "-Ksf" "$0" "$@"

(ns toycalc
  (:require [cli-matic.core :refer [run-cmd]]
            [cljs.pprint :refer [cl-format]]))

;; To run this, try from the project root:
;; ./toycalc.cljs add -a 1 -b 80

(defn to_base
  "Converts a number to a given base" 
  [num base]
    (let [fmt (str "~" base "r")]
      (cl-format nil fmt num)))


(defn add_numbers
  "Sums A and B together, and prints it in base `base`"
  [{:keys [a1 a2 base]}]
  (println
   (to_base (+ a1 a2) base)))

(defn subtract_numbers
  "Subtracts B from A, and prints it in base `base` "
  [{:keys [pa pb base]}]
  (println
   (to_base (- pa pb) base)))

(def CONFIGURATION
  {:app         {:command     "toycalc"
                 :description "A command-line toy calculator"
                 :version     "0.0.1"}
   :global-opts [{:option  "base"
                  :as      "The number base for output"
                  :type    :int
                  :default 10}]
   :commands    [{:command     "add" :short "a"
                  :description ["Adds two numbers together"
                                ""
                                "Looks great, doesn't it?"]
                  :opts        [{:option "a1" :short "a" :env "AA" :as "First addendum" :type :int :default 0}
                                {:option "a2" :short "b" :as "Second addendum" :type :int :default 0}]
                  :runs        add_numbers}
                 {:command     "sub"  :short "s"
                  :description "Subtracts parameter B from A"
                  :opts        [{:option "pa" :short "a" :as "Parameter A" :type :int :default 0}
                                {:option "pb" :short "b" :as "Parameter B" :type :int :default 0}]
                  :runs        subtract_numbers}]})

(defn -main
  "This is our entry point.
  Just pass parameters and configuration.
  Commands (functions) will be invoked as appropriate."
  [& args]
  (run-cmd args CONFIGURATION))

(set! *main-cli-fn* -main)

