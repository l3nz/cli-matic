(ns cli-matic.specs
  (:require [clojure.spec.alpha :as s]))


(s/def ::retval int?)
(s/def ::status #{:OK :ARGS :EXCEPTION})
(s/def ::stdout (s/coll-of string?))
(s/def ::stderr (s/coll-of string?))


(s/def ::RV
  (s/keys :req-un [::retval ::status ::stdout ::stderr]))






(s/def ::options map?)
(s/def ::arguments (s/coll-of string?))
(s/def ::subcommand ifn?)
(s/def ::errors  #{:NONE :HELP-MAIN :HELP-CMD :ERR-CMD})
