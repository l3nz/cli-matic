(ns cli-matic.utils-test
   (:require [clojure.test :refer :all]
             [cli-matic.utils :refer :all]))

(deftest arg-str-distance
  (testing "Distance between two strings."
    (is (= 1/3 (str-distance "foo" "ffo"))))
  (testing "Suggestions"
    (is (= ["foo"]
           (suggestions ["foo" "bar" "baz"] "ffo")))))
