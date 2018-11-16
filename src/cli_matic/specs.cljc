(ns cli-matic.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::anything (s/or :nil nil?
                        :some some?))

(s/def ::existing-string string?)

(s/def ::climatic-errors #{:ERR-CFG
                           :ERR-NO-SUBCMD
                           :ERR-UNKNOWN-SUBCMD
                           :ERR-PARMS-GLOBAL
                           :ERR-PARMS-SUBCMD})

;; The way your process runs; will generate
;; a return val and some lines for stdout/stderr.

(s/def ::retval int?)

(s/def ::status (s/or :oth #{:OK :EXCEPTION}
                      :err ::climatic-errors))

(s/def ::help   (s/or :none nil?
                      :some #{:HELP-GLOBAL :HELP-SUBCMD}))

(s/def ::subcmd (s/or :none ::existing-string
                      :nil  nil?))

(s/def ::stderr (s/coll-of string?))

(s/def ::RV
  (s/keys :req-un [::retval ::status ::help ::subcmd ::stderr]))

;; Cli-matic option definition
(s/def ::option ::existing-string) ;; ex-string

(s/def ::positional-arg (s/and integer?
                               (s/or :z zero? :p pos?)))

(s/def ::short (s/or :str ::existing-string
                     :pos ::positional-arg))

(s/def ::as ::existing-string)

(s/def ::type #{:int :int-0
                :string :keyword
                :float :float-0
                :yyyy-mm-dd
                :slurp :slurplines
                :edn :ednfile
                :json :jsonfile
                :yaml :yamlfile})

(s/def ::default some?)

(s/def ::env ::existing-string)

(s/def ::spec some?) ; \TODO how do we know it's a valid spec?

(s/def ::climatic-option
  (s/keys :req-un [::option  ::as  ::type]
          :opt-un [::short ::default ::env ::spec]))

;; CLI-matic configuration
(s/def ::description (s/or :a-string ::existing-string
                           :coll-str (s/coll-of ::existing-string)))

(s/def ::version ::existing-string)

(s/def ::command ::existing-string)

(s/def ::opts  (s/coll-of ::climatic-option))

(s/def ::runs ifn?)

(s/def ::on-shutdown ifn?)

(s/def ::app (s/keys :req-un [::command ::description ::version]))

(s/def ::global-opts ::opts)

(s/def ::a-command (s/keys :req-un [::command ::opts ::runs]
                           :opt-un [::short ::description ::spec ::on-shutdown]))

(s/def ::commands (s/coll-of ::a-command))

(s/def ::climatic-cfg (s/keys :req-un [::app ::global-opts ::commands]))

;; Parsing of options.
(s/def ::subcommand (s/or :empty nil?
                          :some ::existing-string))

(s/def ::subcommand-def (s/or :empty nil?
                              :some ::a-command))

(s/def ::commandline map?) ;; contains :_arguments as vec

(s/def ::parse-errors (s/or :oth #{:NONE :HELP-GLOBAL :HELP-SUBCMD}
                            :err ::climatic-errors))

(s/def ::error-text string?)

(s/def ::lineParseResult (s/keys :req-un [::subcommand ::subcommand-def ::commandline ::parse-errors ::error-text]))

;; Return value of parsing with tools.cli
(s/def ::parsedCliOpts map?)

(s/def ::mapOfCliParams (s/map-of string? (s/or :empty nil? :str string?)))
