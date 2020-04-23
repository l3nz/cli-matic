(ns cli-matic.utils-v2-test
  (:require [clojure.test :refer [is are deftest testing]]

            [cli-matic.optionals :as OPT]
            [cli-matic.utils-v2 :refer [convert]]))

(defn add_numbers [x] x)
(defn subtract_numbers [x] x)

(deftest convert-test

  (are [i o]  (= (convert i) o)

    ; a string
    {:app         {:command     "toycalc"
                   :description "A command-line toy calculator"
                   :version     "0.0.1"}

     :global-opts [{:option  "base"
                    :as      "The number base for output"
                    :type    :int
                    :default 10}]

     :commands    [{:command     "add"
                    :description "Adds two numbers together"
                    :opts        [{:option "a" :as "Addendum 1" :type :int}
                                  {:option "b" :as "Addendum 2" :type :int :default 0}]
                    :runs        add_numbers}

                   {:command     "sub"
                    :description "Subtracts parameter B from A"
                    :opts        [{:option "a" :as "Parameter A" :type :int :default 0}
                                  {:option "b" :as "Parameter B" :type :int :default 0}]
                    :runs        subtract_numbers}]}

              ; ret


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
                   {:command     "sub"
                    :description "Subtracts parameter B from A"
                    :opts        [{:as      "Parameter A"
                                   :default 0
                                   :option  "a"
                                   :type    :int}
                                  {:as      "Parameter B"
                                   :default 0
                                   :option  "b"
                                   :type    :int}]
                    :runs        subtract_numbers}]}))

(OPT/orchestra-instrument)