#!/usr/bin/env bash
"exec" "plk" "-Sdeps" "{:deps {cli-matic {:mvn/version \"0.3.1\"}}}" "-Ksf" "$0" "$@"

(ns toycalc-spec
  (:require [cli-matic.core :refer [run-cmd]]
            [clojure.spec.alpha :as spec]
            [expound.alpha :as expound]
            [cljs.pprint :refer [cl-format]]
    ))

;; To run this, try from the project root:
;; ./toycalc-spec.cljs add -a 1 -b 80

(defn to_base
  "Converts a number to a given base" 
  [num base]
    (let [fmt (str "~" base "r")]
      (cl-format nil fmt num)))

(defn add_numbers
  "Sums A and B together, and prints it in base `base`"
  [{:keys [addendum-1 addendum-2 base]}]
  (println
   (to_base (+ addendum-1 addendum-2) base)))


;(expound/def ::SMALL #(< % 100) "should be a small number (<100)")
(spec/def ::ODD-SMALL (spec/and odd? ))
(spec/def ::EVEN-SMALL (spec/and even? ))

;(expound/def ::GLOBAL-ADD-VALIDATION
;  (fn [{:keys [addendum-1 addendum-2]}]
;      (<= addendum-1 addendum-2))
;  "addendum-1 should be less than addendum-2")

(def CONFIGURATION
  {:app         {:command     "toycalc-spec"
                 :description "A command-line toy calculator for small numbers"
                 :version     "0.0.2"}
   :global-opts [{:option  "base"
                  :as      "The number base for output"
                  :type    :int
                  :default 10}]
   :commands    [{:command     "add" :short "a"
 ;                 :spec        ::GLOBAL-ADD-VALIDATION
                  :description ["Adds two numbers together"
                                ""
                                "Both numbers should be small (<100) and either odd or even. a <= b."]
                  :opts        [{:option "addendum-1" :short "a" :as "First addendum (odd)"
                                 :type :int :default 0 :spec ::ODD-SMALL}
                                {:option "addendum-2" :short "b" :as "Second addendum (even)"
                                 :type :int :default 0 :spec ::EVEN-SMALL}]
                  :runs        add_numbers}
                 ]})

(defn -main
  "This is our entry point.
  Just pass parameters and configuration.
  Commands (functions) will be invoked as appropriate."
  [& args]
  (run-cmd args CONFIGURATION))

(set! *main-cli-fn* -main)
