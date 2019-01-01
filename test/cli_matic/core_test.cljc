(ns cli-matic.core-test
  (:require [clojure.test :refer [is are deftest testing]]
            [cli-matic.platform :as P ]
            [cli-matic.platform-macros :refer [try-catch-all]]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [cli-matic.core :refer [parse-cmds
                                    run-cmd* ->RV
                                    assert-unique-values
                                    assert-cfg-sanity
                                    parse-cmds-with-defaults]]))

(defn cmd_foo [& opts])
(defn cmd_bar [& opts])
(defn cmd_save_opts [& opts]
  ;(prn "Called" opts)
  opts)

(defn cmd_returnstructure [opts]
  {:myopts opts
   :somedata "hiyo"})

(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   ["-p" "--port PORT" "Port number"
    :default 80
    :parse-fn #(P/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-H" "--hostname HOST" "Remote host"
    :default 0
    ;; Specify a string to output in the default column in the options summary
    ;; if the default value's string representation is very ugly
    :default-desc "localhost"
    :parse-fn #(P/parseInt %)]
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
            :error-text     "dummy: unknown sub-command 'unknown'."
            :parse-errors   :ERR-UNKNOWN-SUBCMD
            :subcommand     "unknown"
            :subcommand-def nil}))))



(deftest run-examples
  (testing "Some real-life behavior for our SIMPLE case"
    (are [i o]
         (= (run-cmd* SIMPLE-SUBCOMMAND-CFG i) o)

      ; no parameters - displays cmd help
      []
      (->RV -1 :ERR-NO-SUBCMD :HELP-GLOBAL nil "No sub-command specified.")

      ["x"]
      (->RV -1 :ERR-UNKNOWN-SUBCMD :HELP-GLOBAL nil "dummy: unknown sub-command 'x'.")

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
      (->RV -1 :ERR-NO-SUBCMD :HELP-GLOBAL nil "No sub-command specified.")

      ["x"]
      (->RV -1 :ERR-UNKNOWN-SUBCMD :HELP-GLOBAL nil "dummy: unknown sub-command 'x'.")

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
      (->RV -1 :ERR-PARMS-GLOBAL :HELP-GLOBAL nil "Global option error: Missing option: aa")

         ;; error no sub cmd
      ["--aa" "1" "foo"  "--dd" "1"]
      (->RV -1 :ERR-PARMS-SUBCMD :HELP-SUBCMD "foo" "Option error: Missing option: cc")

         ;; works
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


;; VALIDATION OF CONFIGURATION
;;


(deftest check-unique-options
  (testing "Unique options"
    (are [i o]
         (= (try-catch-all
              (apply assert-unique-values i)
              (fn [e] :ERR))
            o)

      ; empty
      ["a" [] :x]
      nil

      ; ok
      ["pippo"
       [{:option "a" :as "Parameter A" :type :int :default 0}
        {:option "b" :as "Parameter B" :type :int :default 0}]
       :option]
      nil

      ; dupe
      ["pippo"
       [{:option "a" :as "Parameter A" :type :int :default 0}
        {:option "a" :as "Parameter B" :type :int :default 0}]
       :option]
      :ERR)))

(deftest check-cfg-format
  (testing "Cfg format"
    (are [i o]
         (= (try-catch-all
              (assert-cfg-sanity i)
              (fn [e]
                ;(prn e)
                :ERR))
            o)

      ;; OK
      {:app         {:command     "toycalc" :description "A" :version     "0.0.1"}

       :global-opts [{:option  "base" :as      "T"  :type    :int :default 10}]

       :commands    [{:command     "add"                      :description "Adds" :runs identity
                      :opts        [{:option "a" :as "Addendum 1" :type :int}
                                    {:option "b" :as "Addendum 2" :type :int :default 0}]}]}
      nil

           ;; double in global
      {:app         {:command "toycalc" :description "A" :version "0.0.1"}

       :global-opts [{:option "base" :as "T" :type :int :default 10}
                     {:option "base" :as "X" :type :int :default 10}]

       :commands    [{:command "add" :description "Adds" :runs identity
                      :opts    [{:option "a" :as "Addendum 1" :type :int}
                                {:option "b" :as "Addendum 2" :type :int :default 0}]}]}
      :ERR

           ;; double in specific
      {:app         {:command "toycalc" :description "A" :version "0.0.1"}

       :global-opts [{:option "base" :as "T" :type :int :default 10}]

       :commands    [{:command "add" :description "Adds" :runs identity
                      :opts    [{:option "a" :short "q" :as "Addendum 1" :type :int}
                                {:option "b" :short "q" :as "Addendum 2" :type :int :default 0}]}]}
      :ERR

           ;; positional subcmds in global opts
      {:app         {:command "toycalc" :description "A" :version "0.0.1"}

       :global-opts [{:option "base" :short 0 :as "T" :type :int :default 10}]

       :commands    [{:command "add" :description "Adds" :runs identity
                      :opts    [{:option "a" :short "q" :as "Addendum 1" :type :int}
                                {:option "b" :short "d" :as "Addendum 2" :type :int :default 0}]}]}
      :ERR)))

(def POSITIONAL-SUBCOMMAND-CFG
  {:app         {:command     "dummy"
                 :description "I am some command"
                 :version     "0.1.2"}
   :global-opts [{:option "aa" :as "A" :type :int :default :present}
                 {:option "bb" :as "B" :type :int}]
   :commands    [{:command     "foo" :short       "f"
                  :description "I am function foo"
                  :opts        [{:option "cc" :short 0 :as "C" :type :int :default :present}
                                {:option "dd" :as "D" :type :int}
                                {:option "ee"  :short 1 :as "E" :type :int}]
                  :runs        cmd_save_opts}]})

(deftest check-positional-options
  (testing "Some real-life behavior with mandatory options"
    (are [i o]
         (= (select-keys
             (parse-cmds i POSITIONAL-SUBCOMMAND-CFG)
             [:commandline :error-text]) o)

      ;; a simple case
      ["--aa" "10" "foo"  "1" "2"]
      {:commandline {:_arguments ["1"  "2"]
                     :aa         10
                     :cc         1
                     :ee         2}
       :error-text  ""}

      ;; positional arg does not exist but is default present
      ["--aa" "10" "foo"]
      {:commandline {}
       :error-text  "Missing option: cc"}

      ;; positional arg does not exist and it is not default present
      ["--aa" "10" "foo" "1"]
      {:commandline {:_arguments ["1"]
                     :aa         10
                     :cc         1}
       :error-text  ""})))

(defn env-helper [s]
  (get {"VARA" "10"
        "VARB" "HELLO"} s))

(deftest check-environmental-vars
  (testing "Parsing with env - global opts"
    (are [opts cmdline result]
         (= (dissoc (parse-cmds-with-defaults opts cmdline true env-helper) :summary) result)

      ;; a simple case - no env vars
      [{:option "cc" :short 0 :as "C" :type :int :default :present}
       {:option "dd" :as "D" :type :string}]

      ["--cc" "0" "pippo" "pluto"]

      {:arguments ["pippo" "pluto"]
       :errors    nil
       :options   {:cc 0}}

      ;; a simple case - absent, with env set, integer
      [{:option "cc" :short 0 :as "C" :type :int :default :present}
       {:option "dd" :as "D" :type :int :env "VARA"}]

      ["--cc" "0" "pippo" "pluto"]

      {:arguments ["pippo" "pluto"]
       :errors    nil
       :options   {:cc 0 :dd 10}}

      ;; present, with env set, integer
      [{:option "cc" :short 0 :as "C" :type :int :default :present}
       {:option "dd" :as "D" :type :int :env "VARA"}]

      ["--cc" "0" "--dd" "23" "pippo" "pluto"]

      {:arguments ["pippo" "pluto"]
       :errors    nil
       :options   {:cc 0 :dd 23}}

      ;; absent, with env missing, integer
      [{:option "cc" :short 0 :as "C" :type :int :default :present}
       {:option "dd" :as "D" :type :int :env "NO-VARA"}]

      ["--cc" "0"  "pippo" "pluto"]

      {:arguments ["pippo" "pluto"]
       :errors    nil
       :options   {:cc 0}})))

; =======================================================================
; ========                    S P E C S                        ==========
; =======================================================================

; We add a stupid spec check
; Specs are checked after parsing, both on parameters and globally.

(s/def ::ODD-NUMBER odd?)

(s/def ::GENERAL-SPEC-FOO #(= 99 (:ee %)))

(def SPEC-CFG
  {:app         {:command     "dummy"
                 :description "I am some command"
                 :version     "0.1.2"}
   :global-opts [{:option "aa" :as "A" :type :int :default :present :spec ::ODD-NUMBER}
                 {:option "bb" :as "B" :type :int :spec ::ODD-NUMBER}]
   :commands    [{:command     "foo"
                  :short       "f"
                  :description "I am function foo"
                  :opts        [{:option "cc" :short 0 :as "C" :type :int :default :present}
                                {:option "dd" :as "D" :type :int :spec ::ODD-NUMBER}
                                {:option "ee"  :short 1 :as "E" :type :int :spec ::ODD-NUMBER}]
                  :spec        ::GENERAL-SPEC-FOO
                  :runs        cmd_save_opts}]})

(defn keep-1st-line-stderr
  "To avoid issues with expound changing messages, we remove all
  but the first line in stderr for testing."
  [{:keys [stderr] :as all}]

  (let [nv

        (if (and (vector? stderr) (pos? (count stderr)))

          (let [lines (str/split-lines (first stderr))
                vec-of-fline [(first lines)]]

            vec-of-fline) stderr)]

    (assoc all
           :stderr nv)))

;; ------------

(deftest check-specs
  (are [i o]
       (= (keep-1st-line-stderr (run-cmd* SPEC-CFG i))  o)

    ; all of the should pass
    ["--aa" "3" "--bb" "7" "foo" "--cc" "2" "--dd" "3" "--ee" "99"]
    (->RV 0 :OK nil nil [])

    ; aa (global) non è dispari
    ["--aa" "2" "--bb" "7" "foo" "--cc" "2" "--dd" "3" "--ee" "99"]
    (->RV -1 :ERR-PARMS-GLOBAL :HELP-GLOBAL nil ["Global option error: Spec failure for global option 'aa'"])

    ; bb non esiste proprio
    ["--aa" "3"  "foo" "--cc" "2" "--dd" "3" "--ee" "99"]
    (->RV -1 :ERR-PARMS-GLOBAL :HELP-GLOBAL nil ["Global option error: Spec failure for global option 'bb': with value '' got java.lang.IllegalArgumentException: Argument must be an integer: "])

    ; dd (local)
    ["--aa" "3" "--bb" "7" "foo" "--cc" "2" "--dd" "4" "--ee" "99"]
    (->RV -1 :ERR-PARMS-SUBCMD :HELP-SUBCMD "foo" ["Option error: Spec failure for option 'dd'"])

    ; ee non 99 (validazione globale subcmd)
    ["--aa" "3" "--bb" "7" "foo" "--cc" "2" "--dd" "5" "--ee" "97"]
    (->RV -1 :ERR-PARMS-SUBCMD :HELP-SUBCMD "foo" ["Option error: Spec failure for subcommand 'foo'"])))


; =================================================================
;
; =================================================================

(def SETS-CFG
  {:app         {:command     "dummy"
                 :description "I am some command"
                 :version     "0.1.2"}
   :global-opts []
   :commands    [{:command     "foo" :short       "f"
                  :description "I am function foo"
                  :opts        [{:option "kw" :as "blabla" :type #{:a :b :zebrafuffa} }
                                ]
                  :runs        cmd_save_opts}]})


(deftest check-sets
  (are [i o]
    (= (run-cmd* SETS-CFG i) o)

    ; all of the should pass
    ["foo" "--kw" "a"]
    (->RV 0 :OK nil nil [])

    ["foo" "--kw" "B"]
    (->RV 0 :OK nil nil [])

    ["foo" "--kw" "zebrafufa"]
    {:help   :HELP-SUBCMD
     :retval -1
     :status :ERR-PARMS-SUBCMD
     :stderr ["Option error: Error while parsing option \"--kw zebrafufa\": clojure.lang.ExceptionInfo: Value 'zebrafufa' not allowed. Did you mean ':zebrafuffa'? {}"]
     :subcmd "foo"}





    ))
