(ns cli-matic.utils-test
  (:require [clojure.test :refer :all])
  (:require [cli-matic.utils :refer :all]))

(deftest asString-test

  (are [i o]  (= (asString i) o)

    ; a string
    "x" "x"

    ; a vector of strings
    [ "a" "b"] "a\nb"


    ; add more cases.....
  ))

(deftest indent-string-test
  (are [i o]  (= (indent-string i) o)
      "a" " a"

              ))




(deftest indent-test
  (are [i o]  (= (indent i) o)

              ; a string

              "a"  " a"

              ; a vector
              [ "a" "b" ] [ " a" " b"]


  ))

(deftest pad-test
  (are [s s1 t o]  (= (pad s s1 t) o)

      ; shorter
      "pippo" nil 3   "pip"

      ; longer
      "pippo" nil 7   "pippo  "

      ; with merged string
      "pippo" "pluto" 10 "pippo, plu"


                  ))

(deftest deep-merge-test

  (is (=
        {:one 4 :two {:three 6}}

        (deep-merge {:one 1 :two {:three 3}}
                    {:one 2 :two {:three 4}}
                    {:one 3 :two {:three 5}}
                    {:one 4 :two {:three 6}})
      ))

  )
