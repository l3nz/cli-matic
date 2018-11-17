(ns cli-matic.help-gen-test
  (:require [clojure.test :refer [is are deftest testing]]
            [cli-matic.help-gen :refer [generate-possible-mistypes]]))

(deftest generate-possible-mistypes-test

  (are [w c a o]
    (= o (generate-possible-mistypes w c a))

    ;
    "purchasse"
    ["purchase" "purchasing" "add" "remove"]
    [nil "PP" "A" "R"]
    ["purchase" "purchasing"]
    ))



