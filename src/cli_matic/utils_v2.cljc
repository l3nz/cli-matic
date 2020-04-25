(ns cli-matic.utils-v2
  "
  #  Utils to work with nested configuration trees (cfg v2)

  - Convert commands v1 to v2 (fully nested).
  - Accessors for data in a nested structure





  "
  (:require
   [cli-matic.specs :as S]
   [clojure.spec.alpha :as s]))

(defn convert-config-v1->v2
  "Converts a command version 1 to v2.

  A command v1 has always an outer `::S/climatic-cfg`
  while a v2 is fully nested.

  Note that even in the case where there is only one
  command to be run, we still retain the same invocation
  format as it originally was - no surprises.

  "
  [cmd_v1]

  {:command (get-in cmd_v1 [:app :command])
   :description (get-in cmd_v1 [:app :description])
   :version (get-in cmd_v1 [:app :version])
   :opts (get-in cmd_v1 [:global-opts])
   :subcommands (get-in cmd_v1 [:commands])})

(s/fdef convert-config-v1->v2
  :args (s/cat :cmdv1 ::S/climatic-cfg)
  :ret ::S/climatic-cfg-v2)

(defn isRightCmd
  "Check if this is the right command or not,
  by name or alias."
  [name cfg]
  (= (:command cfg) name))

(defn walk
  [cfg path]
  "
  Walks a path through a configuration object,
  and returns a list of all elements,
  in order from root to leaf, as an
  executable path.

  Does not assert that the last element is a leaf.

  The path:
  - A subcommand path.
  -  If empty, no subcommands and no globals
  -  Each member is the canonical name of a subcommand.
  -  Each member but the last is of type ::branch-subcommand

  It is possible to convert an executable-path back
  into a path with [[as-canonical-path]].

  "
  (cond

    ; No path: return a vector with the only option
    (empty? path)
    [cfg]

    ; Follow through the path
    :else

    (loop [c [cfg]
           p path
           e []]

      (let [pe (first p)
            rp (rest p)
            my-cmd (first (filter (partial isRightCmd pe) c))
            elems (conj e my-cmd)]

        (cond
          ; not found?
          (empty? my-cmd)
          (throw (ex-info
                  (str "Unknown item: " pe " - in " path)
                  {}))

          ; no remaining items
          (empty? rp)
          elems

          ; go back to work
          :else
          (recur (:subcommands my-cmd)
                 rp
                 elems))))))

(s/fdef walk
  :args (s/cat
         :cfg ::S/climatic-cfg-v2
         :path ::S/subcommand-path)
  :ret ::S/subcommand-executable-path)

(defn as-canonical-path
  "
  Gets the canonical path from an executable-sequence
  of subcommands, as obtained from [[walk]].
  "
  [subcommands]
  (mapv :command subcommands))

(defn is-runnable?
  "Checks if the last element if the
  executable path is actually runnable, that is,
  a command."
  [xp]
  (let [e (last xp)]
    (ifn? (:runs e))))

(s/fdef is-runnable?
  :args (s/cat :xp ::S/subcommand-executable-path)
  :ret boolean?)
