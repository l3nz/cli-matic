(ns cli-matic.utils-v2
  "
  Convert commands v1 to v2 (fully nested).

  "
  (:require
   [cli-matic.specs :as S]
   [clojure.spec.alpha :as s]))

(defn convert
  "Converts a command version 1 to v2.

  A command v1 has always an outer ::

  "

  [cmd_v1]

  {:command (get-in cmd_v1 [:app :command])
   :description (get-in cmd_v1 [:app :description])
   :version (get-in cmd_v1 [:app :version])
   :opts (get-in cmd_v1 [:global-opts])
   :subcommands (get-in cmd_v1 [:commands])})

(s/fdef convert
  :args (s/cat :cmdv1 ::S/climatic-cfg)
  :ret ::S/a-subcommand)

