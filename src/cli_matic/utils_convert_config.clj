(ns cli-matic.utils-convert-config
  "
  #  Convert old configuration (v1) to the new format (v2), in a printable way.

  This comes in handy when evolving existing scripts.

  ATM it only works for Clojure, and not so well, as symbol
  de-munging is not 100% deterministic (minuses and underscores are mixed).

  This namespace is not referenced from anywhere else in CLI-matic.

  "
  (:require
   [clojure.string :as str]
   [clojure.walk :as w]
   [cli-matic.utils-v2 :as U2]
   [clojure.pprint :as pp]))




;;


(defn unmangle-fn-name
  "Given the name of a class that implements a Clojure function, returns the function's name in Clojure. Note: If the true Clojure function name
    contains any underscores (a rare occurrence), the unmangled name will
    contain hyphens at those locations instead.

   Inspired by https://www.mail-archive.com/clojure@googlegroups.com/msg13018.html

   There is a similar function in 'clojure.repl/demunge' that screws up
   in a very similar way for symbols  that have a '_' or a '-' in their name,

    "
  [a-fn-name]
  (let [cleaned-name (-> a-fn-name
                         (str/replace "_GT_" ">")
                         (str/replace "_LT_" "<")
                         (str/replace "_" "-"))
        [_ nsp fn] (re-find #"^(.+)\$(.+)$" cleaned-name)]

    (str nsp "/" fn)))

(defn fn->className
  "Extracts the class name for a given function."
  [a-fn]
  (-> a-fn
      .getClass
      .getName))

(defn unmangle-fn
  "Given a non-anonymous function, returns its Clojure name as a symbol."
  [a-fn]
  (-> a-fn
      fn->className
      unmangle-fn-name
      symbol))

(defmacro fn-name
  "This works, but only as a macro."
  [f]
  `(-> ~f var meta :name str))

(defn replace-runs-entries
  "Whenever we find an entry [:runs fnxxxx] we  replace the
  function with its symbol."

  [v]
  (cond

    (and (vector? v)
         (= :runs (first v)))
    [:runs (unmangle-fn (second v))]

    :else
    v))

(defn convert-and-print
  "Given a configuration v1, pretty-prints the corresponding
  configuration v2 that you can copy-and-paste it into
  your scripts.
  "
  [cfg-v1]

  (let [cfg-v2 (U2/convert-config-v1->v2 cfg-v1)
        replace-fns-with-symbols (w/prewalk replace-runs-entries cfg-v2)]

    (println ";; CLI-matic config v2")
    (pp/pprint replace-fns-with-symbols)))


