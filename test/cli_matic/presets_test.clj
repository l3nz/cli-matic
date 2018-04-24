(ns cli-matic.presets-test
  (:require [clojure.test :refer :all]
            [cli-matic.core :refer :all]))


(defn cmd_foo [v]
  (prn "Foo:" v)
  0
  )



(defn mkDummyCfg
  [myOption]
  {:app         {:command   "dummy"
                 :description "I am some command"
                 :version     "0.1.2"}
   :global-opts []
   :commands [{:command    "foo"
               :description "I am function foo"
               :opts  [myOption]
               :runs  cmd_foo}
              ]
   })

; :subcommand     "foo"
; :subcommand-def

(defn parse-cmds-simpler [args cfg]
  (dissoc
    (parse-cmds args cfg)
    :subcommand
    :subcommand-def
    ))


; :int




; :int-0




; :string



; :yyyy-mm-dd

(deftest test-dates
  (testing "YYYY-MM-DD suck"
    (are [i o]
      (= (parse-cmds-simpler
           i
           (mkDummyCfg {:option "val" :as "x" :type :yyyy-mm-dd})
           ) o)

      ; this works (CEST)
      ["foo" "--val" "2018-01-01"]
      {:commandline    {:_arguments []
                        :val        #inst "2017-12-31T23:00:00.000-00:00"}
       :error-text     ""
       :parse-errors   :NONE
       }

      ; this does not
      ["foo" "--val" "pippo"]
      {:commandline    {:_arguments []
                        :val        nil}
       :error-text     ""
       :parse-errors   :NONE
       }
      )))





















