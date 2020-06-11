(ns cli-matic.utils-v2
  "
  #  Utils to work with nested configuration trees (cfg v2)

  - Convert commands v1 to v2 (fully nested).
  - Accessors for data in a nested structure

  "
  (:require
   [cli-matic.utils :as U]
   [cli-matic.specs :as S]
   [clojure.spec.alpha :as s]
   #?(:clj  [cli-matic.platform-macros :refer [try-catch-all]]
      :cljs [cli-matic.platform-macros :refer-macros [try-catch-all]])
   [clojure.string :as str]))

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
  :args (s/cat :cmdv1 ::S/climatic-cfg-classic)
  :ret ::S/climatic-cfg)


      ;
;
;


(def SETUP-DEFAULTS-v1
  {:app {; see help-gen/
         :global-help nil
         :subcmd-help nil}
   :global-opts []})

(defn add-setup-defaults-v1
  "Adds all elements that need to be in the setup spec
  but we allow the caller not specify explicitly."
  [supplied-setup]
  (U/deep-merge SETUP-DEFAULTS-v1 supplied-setup))

(defn cfg-v2
  " Converts a config object into v2, if not already v2.

  "
  [cfg]

  (cond
    ; in v2, we have no :app
    (nil? (:app cfg))
    cfg

    ; else, we need to covert it
    :else
    (convert-config-v1->v2 (add-setup-defaults-v1 cfg))))

(s/fdef cfg-v2
  :args (s/cat :cfg (s/or :v1 ::S/climatic-cfg-classic
                          :v2 ::S/climatic-cfg))
  :ret ::S/climatic-cfg)

(defn isRightCmd?
  "Check if this is the right command or not,
  by name or alias."
  [command-or-short-name cfg]
  (or (= (:command cfg) command-or-short-name)
      (= (:short cfg) command-or-short-name)))

(defn walk
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
  [cfg path]
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
            my-cmd (first (filter (partial isRightCmd? pe) c))
            elems (conj e my-cmd)]

        (cond
          ; not found?
          (empty? my-cmd)
          (throw (ex-info
                  (str "Unknown subcommand: " pe " - in path " path)
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
         :cfg ::S/climatic-cfg
         :path ::S/subcommand-path)
  :ret ::S/subcommand-executable-path)

(defn can-walk?
  "Check that you can walk up to a point.
  It basically traps the exception."
  [cfg path]

  (try-catch-all
   (do
     (walk cfg path)
     true)

   (fn [_]
     false)))

(s/fdef can-walk?
  :args (s/cat
         :cfg ::S/climatic-cfg
         :path ::S/subcommand-path)
  :ret boolean?)

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

(defn canonical-path-to-string
  [path]
  (str/join " " path))

(s/fdef canonical-path-to-string
  :args (s/cat
         :path ::S/subcommand-path)
  :ret string?)

(defn get-subcommand
  "Given a configuration and a path through it,
  reeturns the last subcommand."
  [cfg path]
  (last (walk cfg path)))

(defn get-options-for
  "Given a configuration and a path through it,
  returns :opts for the last subcommmand."
  [cfg path]
  (:opts (get-subcommand cfg path)))

(defn rewrite-opts
  "
  Out of a cli-matic arg list, generates a set of
  options for tools.cli.
  It also adds in the -? and --help options
  to trigger display of helpness.
  "
  [climatic-args subcmd]
  (U/cm-opts->cli-opts
   (get-options-for climatic-args subcmd)))

(s/fdef rewrite-opts
  :args (s/cat :args ::S/climatic-cfg
               :path ::S/subcommand-path)
  :ret some?)

(defn list-positional-parms
  "Extracts all positional parameters from the configuration."
  [cfg subcmd]
  ;;(prn "CFG" cfg "Sub" subcmd)
  (let [opts (get-options-for cfg subcmd)]
    (U/positional-parms-from-opts opts)))

(s/fdef
  list-positional-parms
  :args (s/cat :cfg ::S/climatic-cfg
               :cmd ::S/subcommand-path)
  :ret (s/coll-of ::S/climatic-option))

(defn get-most-specific-value
  "Given a configuration and a path through it, gets the most
  specific value for an option.

  For example, the help generator might be defined on each subcommand,
  or on the root node, or nowhere. We always take the most specific
  value.

  If the value is defined nowhere, we return a default.

  "
  ([cfg path a-key default]
   (let [path-as-objects (walk cfg path)
         values (map a-key path-as-objects)
         non-nil-values (filter some? values)]

     (if (empty? non-nil-values)
       default
       (last non-nil-values))))

  ([cfg path a-key]
   (get-most-specific-value cfg path a-key nil)))


