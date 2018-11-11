(ns cli-matic.utils)

(defn- deep-merge-with
  [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))
 
(defn- levenshtein-distance
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
  [a b]
  (/ (levenshtein-distance a b) (max (count a) (count b))))

(def max-str-distance 1/3)

(defn suggestions
  [cmds cmd]
  (->> cmds
      (filter #(<= (str-distance % cmd) max-str-distance)))
  )

#_(defn candidate-message
  "Returns message for telling a user candidate commands. Returns nil if
  candidates is empty or nil."
  [candidates]
  (if (seq candidates)
    (->> candidates
         (map (partial str "        "))
         (cons (if (= (count candidates) 1)
                 "The most similar command is"
                 "The most similar commands are"))
         (join \newline))))
