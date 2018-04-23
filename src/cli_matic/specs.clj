(ns cli-matic.specs
  (:require [clojure.spec.alpha :as s]))



(s/def ::existing-string string?)

;
; The way your process runs; will generate
; a return val and some lines for stdout/stderr.
;


(s/def ::retval int?)
(s/def ::status #{:OK :ARGS :EXCEPTION})
(s/def ::stdout (s/coll-of string?))
(s/def ::stderr (s/coll-of string?))


(s/def ::RV
  (s/keys :req-un [::retval ::status ::stdout ::stderr]))


;
; Cli-matic option definition
;

(s/def ::option ::existing-string) ; ex-string
(s/def ::shortened ::existing-string)
(s/def ::as ::existing-string)
(s/def ::type #{:int :string})

(s/def ::climatic-option
  (s/keys :req-un [::option  ::as  ::type]
          :opt-un [::shortened]))

;
; Climatic configuration
;

(s/def ::descr ::existing-string)
(s/def ::opts  (s/coll-of ::climatic-option))
(s/def ::runs ifn?)

(s/def ::climatic-general (s/keys :req-un [::descr ::opts]))
(s/def ::climatic-subcommand (s/keys :req-un [::descr ::opts ::runs]))

(s/def ::climatic-cfg (s/map-of keyword? (s/or :cgen ::climatic-general
                                               :subc ::climatic-subcommand)))



;
; Parsing of options.
;

(s/def ::subcommand ::existing-string)
(s/def ::subcommand-def ::climatic-subcommand)
(s/def ::commandline map?) ;; contains :_arguments as vec
(s/def ::parse-errors #{:NONE :CLI-MATIC-DEF :HELP-MAIN :ERR-COMMON :HELP-SUBCMD :ERR-SUBCMD})
(s/def ::error-text string?)


(s/def ::lineParseResult (s/keys :req-un [::subcommand ::subcommand-def ::commandline ::parse-errors ::error-text]))




