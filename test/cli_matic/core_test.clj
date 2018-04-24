(ns cli-matic.core-test
  (:require [clojure.test :refer :all]
            [cli-matic.core :refer :all]))


(defn cmd_foo [& opts])
(defn cmd_bar [& opts])


(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   ["-p" "--port PORT" "Port number"
    :default 80
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-H" "--hostname HOST" "Remote host"
    :default 0
    ;; Specify a string to output in the default column in the options summary
    ;; if the default value's string representation is very ugly
    :default-desc "localhost"
    :parse-fn #(Integer/parseInt %)]
   ;; If no required argument description is given, the option is assumed to
   ;; be a boolean option defaulting to nil
   [nil "--detach" "Detach from controlling process"]
   ["-v" nil "Verbosity level; may be specified multiple times to increase value"
    ;; If no long-option is specified, an option :id must be given
    :id :verbosity
    :default 0
    ;; Use assoc-fn to create non-idempotent options
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ;; A boolean option that can explicitly be set to false
   ["-d" "--[no-]daemon" "Daemonize the process" :default true]
   ["-h" "--help"]])




(def SIMPLE-SUBCOMMAND-CFG
  {:_common {:descr "I am some command"
             :opts [{:option "aa" :as "A" :type :int}
                    {:option "bb" :as "B" :type :int}]}

   :foo    {:descr "I am function foo"
             :opts [{:option "cc" :as "C" :type :int}
                    {:option "dd" :as "D" :type :int}]
             :runs cmd_foo}

   :bar    {:descr "I am function bar"
             :opts [{:option "ee" :as "E" :type :int}
                    {:option "ff" :as "F" :type :int}]
             :runs cmd_bar}
   })




(deftest simple-subcommand
  (testing "A simple subcommand"
    (is (= (parse-cmds
             [ "--bb" "1" "foo" "--cc" "2" "--dd" "3"]
             SIMPLE-SUBCOMMAND-CFG)

           {:commandline {:bb 1 :cc 2 :dd 3 :_arguments []}
           :subcommand "foo"
           :parse-errors :NONE
            :error-text ""
           :subcommand-def {:descr "I am function foo"
                            :opts  [{:as     "C"
                                     :option "cc"
                                     :type   :int}
                                    {:as     "D"
                                     :option "dd"
                                     :type   :int}]
                            :runs  cmd_foo}}
           ))))

(deftest make-option
  (testing "Build a tools.cli option"
    (are [i o]
      (= o (mk-cli-option i))

      ; simplest example
      {:option "extra" :shortened "x" :as "Port number" :type :int}
      ["-x" "--extra N" "Port number"
       :parse-fn parseInt]

      ; no shorthand
      {:option "extra"  :as "Port number" :type :int}
      [nil "--extra N" "Port number"
       :parse-fn parseInt]

      )))



(deftest run-examples
  (testing "Some real-life behavior for our SIMPLE case"
    (are [i o]
      (= (run-cmd* SIMPLE-SUBCOMMAND-CFG i) o)

      ; no parameters - displays cmd help
      []
      (->RV -1 :ERR-NO-SUBCMD nil nil "No sub-command specified")

      ["x"]
      (->RV -1 :ERR-UNKNOWN-SUBCMD nil nil "Unknown sub-command")

      ["--lippa foo"]
      (->RV -1 :ERR-PARMS-COMMON nil nil "Error: ")



      )


    ))

