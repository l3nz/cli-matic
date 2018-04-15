(ns cli-matic.core-test
  (:require [clojure.test :refer :all]
            [cli-matic.core :refer :all]))


(defn cmd_pippo [])




(deftest simple-subcommand
  (testing "A simple subcommand"
    (is (= (parse-cmds
             "-a 1 -b 2 pippo -c 3 -d 4 pluto"

             [["-a" "--aa" "aaaa"]
               ["-b" "-bb" "bbbb"]
              ["-c" "--cc" "cccc"]
              ["-d" "--dd" "dddd"]]



             )
           {:options {"a" 1 "b" 2 "c" 3 "d" 4}
            :arguments ["pluto"]
            :subcommand cmd_pippo
            :errors     :NONE
            }



           ))




    )


  )





(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
