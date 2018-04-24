(ns cli-matic.core-test
  (:require [clojure.test :refer :all]
            [cli-matic.core :refer :all]
            [cli-matic.presets :as PRESETS :refer [parseInt]]))


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
  {:app         {:command   "dummy"
                 :description "I am some command"
                 :version     "0.1.2"}
   :global-opts [{:option "aa" :as "A" :type :int}
                 {:option "bb" :as "B" :type :int}]
   :commands [{:command    "foo"
                  :description "I am function foo"
                  :opts  [{:option "cc" :as "C" :type :int}
                          {:option "dd" :as "D" :type :int}]
                  :runs  cmd_foo}

                 {:command    "bar"
                  :description "I am function bar"
                  :opts  [{:option "ee" :as "E" :type :int}
                          {:option "ff" :as "F" :type :int}]
                  :runs  cmd_bar}
                 ]
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
           :subcommand-def {:command "foo"
                            :description "I am function foo"
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

      ;  with a default
      {:option "extra"  :as "Port number" :type :int :default 13}
      [nil "--extra N" "Port number"
       :parse-fn parseInt :default 13]

      )))



(deftest run-examples
  (testing "Some real-life behavior for our SIMPLE case"
    (are [i o]
      (= (run-cmd* SIMPLE-SUBCOMMAND-CFG i) o)

      ; no parameters - displays cmd help
      []
      (->RV -1 :ERR-NO-SUBCMD :HELP-GLOBAL nil "No sub-command specified")

      ["x"]
      (->RV -1 :ERR-UNKNOWN-SUBCMD :HELP-GLOBAL nil "Unknown sub-command")

      ["--lippa" "foo"]
      (->RV -1 :ERR-PARMS-GLOBAL :HELP-GLOBAL nil "Global option error: Unknown option: \"--lippa\"")

      ; help globale
      ["-?"]
      (->RV 0 :OK :HELP-GLOBAL nil nil)

      ["--help"]
      (->RV 0 :OK :HELP-GLOBAL nil nil)

      ; help sub-command
      ["foo"  "-?"]
      (->RV 0 :OK :HELP-SUBCMD "foo" nil)

      ["bar" "--help"]
      (->RV 0 :OK :HELP-SUBCMD "bar" nil)



      )))

; Problems
; --------
;
; Types
;
; lein run -m cli-matic.toycalc -- add --a x
; ** ERROR: **
; Error:
; and nothing else


