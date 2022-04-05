#!/usr/bin/env bb

;; To run this, try from the project root:
;; ./toycalc.bb -m toycalc add -a 1 -b 80


; To avoid having an external bb.edn file, we reference dependencies
; within the script itself.
(require '[babashka.deps :as deps])
(deps/add-deps '{:deps {org.babashka/spec.alpha {:git/url "https://github.com/babashka/spec.alpha"
                                                 :sha "1a841c4cc1d4f6dab7505a98ed2d532dd9d56b78"}
                        cli-matic/cli-matic {:mvn/version "0.5.2"}}})

(require '[cli-matic.core :refer [run-cmd]])

(defn add_numbers
  "Sums A and B together, and prints it in base `base`"
  [{:keys [a1 a2 base]}]
  (println
   (Integer/toString (+ a1 a2) base)))

(defn subtract_numbers
  "Subtracts B from A, and prints it in base `base` "
  [{:keys [pa pb base]}]
  (println
   (Integer/toString (- pa pb) base)))

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


; This is our entry point.
; Commands (functions) will be invoked as appropriate.


(run-cmd *command-line-args* CONFIGURATION)
