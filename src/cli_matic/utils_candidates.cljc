(ns cli-matic.utils-candidates
  "
  ### String candidates tools.

This namespace contains utilities to compute string candidates.

  "

  )



;
; String distance
; Patch by https://github.com/l3nz/cli-matic/pull/49/commits/07c392301a9e12c2f8ad76fd8a9115e0632e175a
;
(defn- deep-merge-with
  [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))

(defn- levenshtein-distance
  "Ref https://en.wikipedia.org/wiki/Levenshtein_distance "
  [a b]
  (let [m (count a)
        n (count b)
        init (apply deep-merge-with (fn [a b] b)
                    (concat
                      (for [i (range 0 (+ 1 m))]
                        {i {0 i}})
                      (for [j (range 0 (+ 1 n))]
                        {0 {j j}})))
        table (reduce
                (fn [d [i j]]
                  (deep-merge-with
                    (fn [a b] b)
                    d
                    {i {j (if (= (nth a (- i 1))
                                 (nth b (- j 1)))
                            ((d (- i 1)) (- j 1))
                            (min
                              (+ ((d (- i 1))
                                   j) 1)
                              (+ ((d i)
                                   (- j 1)) 1)
                              (+ ((d (- i 1))
                                   (- j 1)) 1)))
                        }}))
                init
                (for [j (range 1 (+ 1 n))
                      i (range 1 (+ 1 m))] [i j]))]
    ((table m) n)))

(defn str-distance
  "Distance between two strings, as expressed in percentage
  of changes to the length of the longest string.

  "
  [a b]
  (/ (levenshtein-distance a b)
     (max (count a) (count b) 1)))


(defn candidate-suggestions
  "Returns candidate suggestions, in order of
  reliability."

  [candidates cmd max-str-distance]

  (let [valid (filter #(<= (str-distance % cmd) max-str-distance) candidates)]
    (sort-by (partial str-distance cmd) valid)))


