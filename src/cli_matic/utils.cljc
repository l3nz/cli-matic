(ns cli-matic.utils
  "
  ### Utilities used in the project


  "
  (:require [clojure.string :as str])
  )



(defn asString
  "Turns a collection of strings into one string,
  or the string itself."
  [s]
  (if (string? s)
    s
    (str/join "\n" s)))

(defn indent-string
  "Indents a single string by one space."
  [s]
  (str " " s))

(defn indent
  "Indents a single string, or each string
  in a collection of strings."
  [s]
  (if (string? s)
    (indent-string s)
    (map indent-string (flatten s))))

(defn pad
  "Pads 's[, s1]' to so many characters"
  [s s1 len]
  (subs (str s
             (when s1
               (str ", " s1))
             "                   ")
        0 len))


(defn deep-merge
  "
  Merges a number of maps, considering values in inner maps.

  See https://gist.github.com/danielpcox/c70a8aa2c36766200a95#gistcomment-2308595
  "

  [& maps]
  (apply merge-with (fn [& args]
                      (if (every? map? args)
                        (apply deep-merge args)
                        (last args)))
         maps))



