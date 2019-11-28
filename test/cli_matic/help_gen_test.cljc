(ns cli-matic.help-gen-test
  (:require [clojure.test :refer [is are deftest testing]]
            #?(:clj [cli-matic.platform-macros :refer [try-catch-all]]
               :cljs [cli-matic.platform-macros :refer-macros [try-catch-all]])
            [cli-matic.help-gen :refer [generate-possible-mistypes
                                        generate-section
                                        generate-a-command
                                        generate-global-help
                                        generate-subcmd-help]]))

(deftest generate-possible-mistypes-test

  (are [w c a o]
       (= o (generate-possible-mistypes w c a))

    ;
    "purchasse"
    ["purchase" "purchasing" "add" "remove"]
    [nil "PP" "A" "R"]
    ["purchase" "purchasing"]))

(deftest generate-section-test

  (are [t l o]
       (= o (-> (generate-section t l)
                flatten
                vec))

    ; full
    "tit"
    ["a" "b"]
    ["tit:" " a" " b" ""]

    ; empty
    "tittel"
    []
    []))

(deftest generate-a-command-test

  (are [c s d o]
       (= o (generate-a-command {:command c :short s :description d}))

    ; full
    "tit" "t" ["a" "b"]
    "  tit, t               a"

    ; linea singola
    "tit" "t" "singel"
    "  tit, t               singel"

    ; no description
    "tit" "t" []
    "  tit, t               ?"))

(defn dummy-cmd [_])

(def CONFIGURATION-TOYCALC
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
                  :runs        dummy-cmd}
                 {:command     "sub"  :short "s"
                  :description "Subtracts parameter B from A"
                  :opts        [{:option "pa" :short "a" :as "Parameter A" :type :int :default 0}
                                {:option "pb" :short "b" :as "Parameter B" :type :int :default 0}]
                  :runs        dummy-cmd}]})

(deftest generate-global-help-test

  (is
   (= ["NAME:"
       " toycalc - A command-line toy calculator"
       ""
       "USAGE:"
       " toycalc [global-options] command [command options] [arguments...]"
       ""
       "VERSION:"
       " 0.0.1"
       ""
       "COMMANDS:"
       "   add, a               Adds two numbers together"
       "   sub, s               Subtracts parameter B from A"
       ""
       "GLOBAL OPTIONS:"
       "       --base N  10  The number base for output"
       "   -?, --help"
       ""]
      (generate-global-help CONFIGURATION-TOYCALC))))

(deftest generate-subcmd-help-test

  (is
   (= ["NAME:"
       " toycalc add - Adds two numbers together"
       " "
       " Looks great, doesn't it?"
       ""
       "USAGE:"
       " toycalc [add|a] [command options] [arguments...]"
       ""
       "OPTIONS:"
       "   -a, --a1 N  0  First addendum [$AA]"
       "   -b, --a2 N  0  Second addendum"
       "   -?, --help"
       ""]
      (generate-subcmd-help CONFIGURATION-TOYCALC "add")))

  (is
   (= ["NAME:"
       " toycalc sub - Subtracts parameter B from A"
       ""
       "USAGE:"
       " toycalc [sub|s] [command options] [arguments...]"
       ""
       "OPTIONS:"
       "   -a, --pa N  0  Parameter A"
       "   -b, --pb N  0  Parameter B"
       "   -?, --help"
       ""]
      (generate-subcmd-help CONFIGURATION-TOYCALC "sub")))

  (is
   (= :ERR
      (try-catch-all
       (generate-subcmd-help CONFIGURATION-TOYCALC "undefined-cmd")
       (fn [_] :ERR)))))

(def CONFIGURATION-POSITIONAL-TOYCALC
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
                  :opts        [{:option "a1" :short 0 :env "AA" :as "First addendum" :type :int :default 0}
                                {:option "a2" :short 1 :as "Second addendum" :type :int :default 0}]
                  :runs        dummy-cmd}]})

(deftest generate-subcmd-positional-test
  (is
   (= ["NAME:"
       " toycalc add - Adds two numbers together"
       " "
       " Looks great, doesn't it?"
       ""
       "USAGE:"
       " toycalc [add|a] [command options] a1 a2"
       ""
       "OPTIONS:"
       "       --a1 N  0  First addendum [$AA]"
       "       --a2 N  0  Second addendum"
       "   -?, --help"
       ""]
      (generate-subcmd-help CONFIGURATION-POSITIONAL-TOYCALC "add"))))
