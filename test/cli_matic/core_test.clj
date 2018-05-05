(ns cli-matic.core-test
  (:require [clojure.test :refer :all]
            [cli-matic.core :refer :all]
            [cli-matic.presets :as PRESETS :refer [parseInt]]))

(defn cmd_foo [& opts])
(defn cmd_bar [& opts])

(defn cmd_returnstructure [opts]
  {:myopts opts
   :somedata "hiyo"})

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
  {:app         {:command     "dummy"
                 :description "I am some command"
                 :version     "0.1.2"}
   :global-opts [{:option "aa" :as "A" :type :int}
                 {:option "bb" :as "B" :type :int}]
   :commands    [{:command     "foo" :short       "f"
                  :description "I am function foo"
                  :opts        [{:option "cc" :as "C" :type :int}
                                {:option "dd" :as "D" :type :int}]
                  :runs        cmd_foo}

                 ; another one
                 {:command     "bar"
                  :description "I am function bar"
                  :opts        [{:option "ee" :as "E" :type :int}
                                {:option "ff" :as "F" :type :int}]
                  :runs        cmd_bar}

                 ; this one to check return structs
                 {:command     "rets"
                  :description "I return a structure"
                  :opts        []
                  :runs        cmd_returnstructure}]})

(deftest simple-subcommand
  (testing "A simple subcommand"

    ;; Normmal subcomamnd
    (is (= (parse-cmds
            ["--bb" "1" "foo" "--cc" "2" "--dd" "3"]
            SIMPLE-SUBCOMMAND-CFG)

           {:commandline {:bb 1 :cc 2 :dd 3 :_arguments []}
            :subcommand "foo"
            :parse-errors :NONE
            :error-text ""
            :subcommand-def {:command "foo"
                             :short "f"
                             :description "I am function foo"
                             :opts  [{:as     "C"
                                      :option "cc"
                                      :type   :int}
                                     {:as     "D"
                                      :option "dd"
                                      :type   :int}]
                             :runs  cmd_foo}}))

    ;; short subcommand
    (is (= (parse-cmds
            ["--bb" "1" "f" "--cc" "2" "--dd" "3"]
            SIMPLE-SUBCOMMAND-CFG)

           {:commandline {:bb 1 :cc 2 :dd 3 :_arguments []}
            :subcommand "foo"
            :parse-errors :NONE
            :error-text ""
            :subcommand-def {:command "foo"
                             :short "f"
                             :description "I am function foo"
                             :opts  [{:as     "C"
                                      :option "cc"
                                      :type   :int}
                                     {:as     "D"
                                      :option "dd"
                                      :type   :int}]
                             :runs  cmd_foo}}))

    ;; unknown subcommand
    (is (= (parse-cmds
            ["--bb" "1" "unknown" "--cc" "2" "--dd" "3"]
            SIMPLE-SUBCOMMAND-CFG)

           {:commandline    {}
            :error-text     ""
            :parse-errors   :ERR-UNKNOWN-SUBCMD
            :subcommand     "unknown"
            :subcommand-def nil}))))

(deftest subcommands-and-aliases
  (testing "Subcommands and aliases"
    (is (= (all-subcommands-aliases SIMPLE-SUBCOMMAND-CFG)
           {"bar" "bar"
            "f"   "foo"
            "foo" "foo"
            "rets" "rets"})))

  (testing "All subcommands"
    (is (= (all-subcommands SIMPLE-SUBCOMMAND-CFG)
           #{"bar"
             "f"
             "foo"
             "rets"})))

  (testing "Canonicalize-subcommand"
    (is (= (canonicalize-subcommand SIMPLE-SUBCOMMAND-CFG "foo")
           "foo"))
    (is (= (canonicalize-subcommand SIMPLE-SUBCOMMAND-CFG "f")
           "foo"))
    (is (= (canonicalize-subcommand SIMPLE-SUBCOMMAND-CFG "bar")
           "bar"))))

(deftest make-option
  (testing "Build a tools.cli option"
    (are [i o]
         (= o (mk-cli-option i))

      ; simplest example
      {:option "extra" :short "x" :as "Port number" :type :int}
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

     ;  :present means there is no default
      {:option "extra"  :as "Port number" :type :int :default :present}
      [nil "--extra N*" "Port number"
       :parse-fn parseInt])))

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

      ; help sub-commands (incl short version)
      ["foo"  "-?"]
      (->RV 0 :OK :HELP-SUBCMD "foo" nil)

      ["bar" "--help"]
      (->RV 0 :OK :HELP-SUBCMD "bar" nil)

      ["f"  "-?"]
      (->RV 0 :OK :HELP-SUBCMD "foo" nil)

      ["rets"]
      (->RV 0 :OK nil nil nil))))

(def MANDATORY-SUBCOMMAND-CFG
  {:app         {:command     "dummy"
                 :description "I am some command"
                 :version     "0.1.2"}
   :global-opts [{:option "aa" :as "A" :type :int :default :present}
                 {:option "bb" :as "B" :type :int}]
   :commands    [{:command     "foo" :short       "f"
                  :description "I am function foo"
                  :opts        [{:option "cc" :as "C" :type :int :default :present}
                                {:option "dd" :as "D" :type :int}]
                  :runs        cmd_foo}]})

(deftest check-mandatory-options
  (testing "Some real-life behavior with mandatory options"
    (are [i o]
         (= (run-cmd* MANDATORY-SUBCOMMAND-CFG i) o)

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

      ; help sub-commands (incl short version)
      ["foo"  "-?"]
      (->RV 0 :OK :HELP-SUBCMD "foo" nil)

      ; error no global cmd
      ["foo"  "--cc" "1"]
      (->RV -1 :ERR-PARMS-GLOBAL :HELP-GLOBAL nil "Global option error: Missing option: aa"); error no sub cmd
      ["--aa" "1" "foo"  "--dd" "1"]
      (->RV -1 :ERR-PARMS-SUBCMD :HELP-SUBCMD "foo" "Option error: Missing option: cc"); works
      ["--aa" "1" "foo"  "--cc" "1"]
      (->RV 0 :OK nil nil nil))))

; Problems
; --------
;
; Types
;
; lein run -m cli-matic.toycalc -- add --a x
; ** ERROR: **
; Error:
; and nothing else


