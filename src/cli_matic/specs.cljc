(ns cli-matic.specs
  (:require [clojure.spec.alpha :as s]))

(defn has-elements? [s]
  (pos? (count s)))

(s/def ::anything (s/or :nil nil?
                        :some some?))

(s/def ::existing-string (s/and string? has-elements?))

; In many places you can have "foo” or ["foo" "bar"]
(s/def ::string-or-strings (s/or :a-string ::existing-string
                                 :coll-str (s/coll-of string?)))

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

(s/def ::subcmd (s/coll-of ::existing-string))

(s/def ::stderr (s/coll-of string?))

(s/def ::RV
  (s/keys :req-un [::retval ::status ::help ::subcmd ::stderr]))

;; Cli-matic option definition
(s/def ::option ::existing-string) ;; ex-string

(s/def ::positional-arg (s/and integer?
                               (s/or :z zero? :p pos?)))

(s/def ::short (s/or :str ::existing-string
                     :pos ::positional-arg))

(s/def ::as ::string-or-strings)

(s/def ::set-of-strings
  (s/and set?
         (s/coll-of ::existing-string :min-count 1)))

(s/def ::set-of-kws
  (s/and set?
         (s/coll-of keyword? :min-count 1)))

(s/def ::set-of-vals
  (s/or :set-str ::set-of-strings
        :set-kw  ::set-of-kws))

(s/def ::type
  (s/or :plain-kw #{:int :int-0
                    :string :keyword
                    :float :float-0
                    :yyyy-mm-dd
                    :slurp :slurplines
                    :edn :ednfile
                    :json :jsonfile
                    :with-flag :flag
                    :yaml :yamlfile}
        :set-vals ::set-of-vals))

(s/def ::default some?)

(s/def ::env ::existing-string)

(s/def ::spec some?) ; \TODO how do we know it's a valid spec?

(s/def ::climatic-option
  (s/keys :req-un [::option  ::as  ::type]
          :opt-un [::short ::default ::env ::spec]))

;; CLI-matic configuration
(s/def ::description ::string-or-strings)

(s/def ::examples ::string-or-strings)

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

(s/def ::climatic-cfg-classic
  (s/keys :req-un [::app ::global-opts ::commands]))

(s/def ::global-help ifn?)
(s/def ::subcmd-help ifn?)



;; Return value of parsing with tools.cli


(s/def ::parsedCliOpts map?)

(s/def ::mapOfCliParams (s/map-of string? (s/or :empty nil? :str string?)))

;
; Configuration for nested sub-commands
; (see bug #69 and more)
;


(s/def ::any-subcommand (s/keys :req-un [::command ::opts]
                                :opt-un [::short ::description ::spec
                                         ::version
                                         ::examples
                                         ::on-shutdown ::global-help ::subcmd-help]))

; root has a version
(s/def ::root-subcommand
  (s/keys :req-un [::version]))

(defn no-positional-opts
  "Makes sure that this subcommand does not have
  any positional argument in its opts"

  [any-subcmd]
  (let [o (:opts any-subcmd)]
    (empty?
     (filter int? o))))

(s/def ::branch-subcommand
  (s/and
   ::any-subcommand
   (s/keys :req-un [::subcommands])
   ; no positional arguments
   no-positional-opts))

(s/def ::leaf-subcommand
  (s/and
   ::any-subcommand
   (s/keys :req-un [::runs])))

(s/def ::a-subcommand (s/or :branch ::branch-subcommand
                            :leaf ::leaf-subcommand))

(s/def ::subcommands (s/coll-of
                      ::a-subcommand))

(s/def ::climatic-cfg
  ::a-subcommand)

; A subcommand path.
; If empty, no subcommands and no globals
; Each member is the canonical name of a subcommand.
; Each member but the last is of type ::branch-subcommand


(s/def ::subcommand-path
  (s/coll-of ::existing-string))

; A path exploded into the actual subcommad definitions
(s/def ::subcommand-executable-path
  (s/coll-of ::any-subcommand
             :min-count 1))



;; Parsing of options.


(s/def ::subcommand (s/or :empty nil?
                          :some ::existing-string))

(s/def ::subcommand-def (s/or :empty nil?
                              :some ::a-command))

(s/def ::commandline map?) ;; contains :_arguments as vec

(s/def ::parse-errors (s/or :oth #{:NONE :HELP-GLOBAL :HELP-SUBCMD}
                            :err ::climatic-errors))

(s/def ::error-text string?)

(s/def ::lineParseResult (s/keys :req-un [::subcommand ::subcommand-path ::subcommand-def ::commandline ::parse-errors ::error-text]))
