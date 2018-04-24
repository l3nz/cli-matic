(ns cli-matic.specs
  (:require [clojure.spec.alpha :as s]))



(s/def ::existing-string string?)

(s/def ::climatic-errors #{:ERR-CFG
                           :ERR-NO-SUBCMD
                           :ERR-UNKNOWN-SUBCMD
                           :ERR-PARMS-GLOBAL
                           :ERR-PARMS-SUBCMD})


;
; The way your process runs; will generate
; a return val and some lines for stdout/stderr.
;


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


;
; Cli-matic option definition
;

(s/def ::option ::existing-string) ; ex-string
(s/def ::shortened ::existing-string)
(s/def ::as ::existing-string)
(s/def ::type #{:int :string})
(s/def ::default some?)

(s/def ::climatic-option
  (s/keys :req-un [::option  ::as  ::type]
          :opt-un [::shortened ::default]))

;
; Climatic configuration
;

(s/def ::description ::existing-string)
(s/def ::version ::existing-string)
(s/def ::command ::existing-string)

(s/def ::opts  (s/coll-of ::climatic-option))
(s/def ::runs ifn?)

(s/def ::app (s/keys :req-un [::command ::description ::version]))
(s/def ::global-opts ::opts)

(s/def ::a-command (s/keys :req-un [::command ::opts ::runs]))
(s/def ::commands (s/coll-of ::a-command))

(s/def ::climatic-cfg (s/keys :req-un [::app ::global-opts ::commands]))



;
; Parsing of options.
;

(s/def ::subcommand (s/or :empty nil?
                          :some ::existing-string))
(s/def ::subcommand-def (s/or :empty nil?
                              :some ::a-command))
(s/def ::commandline map?) ;; contains :_arguments as vec
(s/def ::parse-errors (s/or :oth #{:NONE :HELP-COMMON :HELP-SUBCMD}
                            :err ::climatic-errors))


(s/def ::error-text string?)


(s/def ::lineParseResult (s/keys :req-un [::subcommand ::subcommand-def ::commandline ::parse-errors ::error-text]))




