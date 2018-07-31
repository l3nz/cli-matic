(ns cli-matic.presets-test
  (:require [clojure.test :refer :all]
            [cli-matic.core :refer :all]))

(defn cmd_foo [v]
  (prn "Foo:" v)
  0)

(defn mkDummyCfg
  [myOption]
  {:app         {:command   "dummy"
                 :description "I am some command"
                 :version     "0.1.2"}
   :global-opts []
   :commands [{:command    "foo"
               :description "I am function foo"
               :opts  [myOption]
               :runs  cmd_foo}]})

; :subcommand     "foo"
; :subcommand-def

(defn parse-cmds-simpler [args cfg]
  (dissoc
   (parse-cmds args cfg)
   :subcommand
   :subcommand-def))

(defn str-val
  "Rewrites a value for float comparison to a string"
  [o]
  (let [v (get-in o [:commandline :val])
        vs (str v)]
    (assoc-in o [:commandline :val] vs)))

; :int values

(deftest test-ints

  (testing "int value"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :int}))
            o)

      ; integers
      ["foo" "--val" "7"]
      {:commandline  {:_arguments []
                      :val        7}
       :error-text   ""
       :parse-errors :NONE}))

  ;
  (testing "int-0 value"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :int-0}))
            o)

      ; integers
      ["foo" "--val" "7"]
      {:commandline  {:_arguments []
                      :val        7}
       :error-text   ""
       :parse-errors :NONE}

      ; integers
      ["foo"]
      {:commandline  {:_arguments []
                      :val        0}
       :error-text   ""
       :parse-errors :NONE})))

;; float values (float and float-0)
;; to compare them, we rewrite them to strings
(deftest test-float

  (testing "float value"
    (are [i o]
         (= (str-val (parse-cmds-simpler
                      i
                      (mkDummyCfg {:option "val" :as "x" :type :float})))

            (str-val o))

    ; integers as floats
      ["foo" "--val" "7"]
      {:commandline  {:_arguments []
                      :val        7.0}
       :error-text   ""
       :parse-errors :NONE}

    ; floats as floats
      ["foo" "--val" "3.14"]
      {:commandline  {:_arguments []
                      :val        3.14}
       :error-text   ""
       :parse-errors :NONE}))

  (testing "float0 value"
    (are [i o]
         (= (str-val (parse-cmds-simpler
                      i
                      (mkDummyCfg {:option "val" :as "x" :type :float-0})))

            (str-val o))

    ; integers as floats
      ["foo" "--val" "7"]
      {:commandline  {:_arguments []
                      :val        7.0}
       :error-text   ""
       :parse-errors :NONE}

    ; floats as floats
      ["foo" "--val" "3.14"]
      {:commandline  {:_arguments []
                      :val        3.14}
       :error-text   ""
       :parse-errors :NONE}

    ; missing as zero
      ["foo"]
      {:commandline  {:_arguments []
                      :val        0.0}
       :error-text   ""
       :parse-errors :NONE})))

; :string
(deftest test-string
  (testing "just strings"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :string})) o)

         ;
      ["foo" "--val" "abcd"]
      {:commandline  {:_arguments []
                      :val        "abcd"}
       :error-text   ""
       :parse-errors :NONE}))

  (testing
   (are [i o]
        (= (parse-cmds-simpler
            i
            (mkDummyCfg {:option "val" :short "v" :as "x" :type :string})) o)

        ;
     ["foo" "-v" "abcd" "aaarg"]
     {:commandline  {:_arguments ["aaarg"]
                     :val        "abcd"}
      :error-text   ""
      :parse-errors :NONE}))

  (testing "multiple strings"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :string :multiple true})) o)
         ;
      ["foo" "--val" "abcd"]
      {:commandline  {:_arguments []
                      :val        ["abcd"]}
       :error-text   ""
       :parse-errors :NONE}

      ["foo" "--val" "abcd" "--val" "defg"]
      {:commandline  {:_arguments []
                      :val        ["abcd" "defg"]}
       :error-text   ""
       :parse-errors :NONE}))

  (testing "multiple strings but no multiple option"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :string :multiple false})) o)

         ;
      ["foo" "--val" "abcd"]
      {:commandline  {:_arguments []
                      :val        "abcd"}
       :error-text   ""
       :parse-errors :NONE}

      ["foo" "--val" "abcd" "--val" "defg"]
      {:commandline  {:_arguments []
                      :val        "defg"}
       :error-text   ""
       :parse-errors :NONE})))

; :yyyy-mm-dd

(deftest test-dates
  (testing "YYYY-MM-DD suck"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :yyyy-mm-dd})) o)

      ; this works (CEST)
      ["foo" "--val" "2018-01-01"]
      {:commandline    {:_arguments []
                        :val        #inst "2017-12-31T23:00:00.000-00:00"}
       :error-text     ""
       :parse-errors   :NONE}

      ; this does not
      ["foo" "--val" "pippo"]
      {:commandline    {:_arguments []
                        :val        nil}
       :error-text     ""
       :parse-errors   :NONE})))

; Slurping di file di testo

(deftest test-slurping
  (testing "Slurping all-in-one"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :slurp})) o)

      ; one file
      ["foo" "--val" "resources/three_lines.txt"]
      {:commandline    {:_arguments []
                        :val        "L1\nLine2\nline 3\n"}
       :error-text     ""
       :parse-errors   :NONE}))

  (testing "Slurping multiline"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :slurplines})) o)

      ; one file
      ["foo" "--val" "resources/three_lines.txt"]
      {:commandline    {:_arguments []
                        :val        ["L1" "Line2" "line 3"]}
       :error-text     ""
       :parse-errors   :NONE})))

; JSON

(deftest test-json
  (testing "JSON valore singolo"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :json})) o)

      ; one file
      ["foo" "--val" "{\"a\":1, \"b\":2}"]
      {:commandline    {:_arguments []
                        :val        {"a" 1
                                     "b" 2}}
       :error-text     ""
       :parse-errors   :NONE}))

  (testing "Slurping multiline JSON"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :jsonfile})) o)

      ; one file
      ["foo" "--val" "resources/json_simple.json"]
      {:commandline    {:_arguments []
                        :val        {"list"   [1 2 "hi"]
                                     "intval" 100
                                     "strval" "good"}}
       :error-text     ""
       :parse-errors   :NONE})))

; EDN

(deftest test-edn
  (testing "EDN valore singolo"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :edn})) o)

      ; one file
      ["foo" "--val" "{:a 1, :b 2}"]
      {:commandline    {:_arguments []
                        :val        {:a 1
                                     :b 2}}
       :error-text     ""
       :parse-errors   :NONE}))

  (testing "Slurping multiline EDN"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :ednfile})) o)

      ; one file
      ["foo" "--val" "resources/edn_simple.edn"]
      {:commandline    {:_arguments []
                        :val        {:list   [1 2 "hi"]
                                     :intval 100
                                     :strval "good"}}
       :error-text     ""
       :parse-errors   :NONE}))
 
; YAML

(deftest test-yaml
  (testing "YAML valore singolo"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :yaml})) o)

                                        ; one file
      ["foo" "--val" "a: 1\nb: 2"]
      ;;["foo" "--val" "{\"a\":1, \"b\":2}"]
      {:commandline    {:_arguments []
                        :val        {"a" 1
                                     "b" 2}}
       :error-text     ""
       :parse-errors   :NONE}))

  (testing "Slurping multiline YAML"
    (are [i o]
         (= (parse-cmds-simpler
             i
             (mkDummyCfg {:option "val" :as "x" :type :yamlfile})) o)

      ; one file
      ["foo" "--val" "resources/yaml_simple.yaml"]
      {:commandline    {:_arguments []
                        :val        {"list"   [1 2 "hi"]
                                     "intval" 100
                                     "strval" "good"}}
       :error-text     ""
       :parse-errors   :NONE}))
  (testing "Complex multiline YAML"
    (are [i o]
         (= (-> (parse-cmds-simpler
                i
                (mkDummyCfg {:option "val" :as "x" :type :yamlfile}))
                (get-in [:commandline :val])
                (select-keys ["invoice" "date"])
                ) o)

      ; one file
      ["foo" "--val" "resources/yaml_full.yaml"]
      {"invoice" 34843
       "date" #inst "2001-01-23"}
      #_ {:commandline    {:_arguments []
                        :val        {"list"   [1 2 "hi"]
                                     "intval" 100
                                     "strval" "good"}}
       :error-text     ""
       :parse-errors   :NONE}))
  )
