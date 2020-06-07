(ns cli-matic.utils
  "
  ### Utilities used in the project

  * the *general* section contains low-level
    stuff that could be anywhere
  * the *cli-matic* section contain low-level functions
    used by the parser and the help generators.


  "
  (:require [clojure.string :as str]
            [cli-matic.presets :as PRESETS]
            [cli-matic.specs :as S]
            [clojure.spec.alpha :as s]
            [cli-matic.platform :as P]))


; ================================================
;  GENERAL TOOLS
; ================================================


(defn asString
  "Turns a collection of strings into one string,
  or the string itself.

  If the collection includes multiple sub-arrays,
  those are flattened into lines as well.
  "
  [s]
  (if (string? s)
    s
    (str/join "\n" (flatten s))))

(defn printString
  "Prints a string (either single, or a nested vector of it)."
  [s]
  (-> s
      asString
      println))

(defn printErr
  "Like [[printString]] but writes on stderr"
  [s]
  (-> s
      asString
      P/printError))

(defn asStrVec
  "Whatever we get in, we want a vector of strings out."
  [s]
  (cond
    (nil? s) []
    (string? s) [s]
    :else  s))

(defn indent-string
  "Indents a single string by one space."
  [s]
  (str " " s))

(s/fdef indent-string
  :args (s/cat :s string?)
  :ret string?)

(defn indent
  "Indents a single string, or each string
  in a collection of strings."
  [s]
  (if (string? s)
    (indent-string s)
    (map indent-string (flatten s))))

(defn pad
  "Pads 's[, s1]' to so many characters"
  [s s1 len]
  (subs (str s
             (when s1
               (str ", " s1))
             "                   ")
        0 len))

(defn deep-merge
  "
  Merges a number of maps, considering values in inner maps.

  See https://gist.github.com/danielpcox/c70a8aa2c36766200a95#gistcomment-2308595
  "

  [& maps]
  (apply merge-with (fn [& args]
                      (if (every? map? args)
                        (apply deep-merge args)
                        (last args)))
         maps))


; ==================================================================
; CLI-matic specific stuff
; ==================================================================


(defn assoc-new-multivalue
  "Associates a new multiple value to the
  current parameter map.
  If the current value is not a vector, creates
  a new vector with the new value."
  [parameter-map option v]
  (let [curr-val (get parameter-map option [])
        new-val (if (vector? curr-val)
                  (conj curr-val v)
                  [v])]
    (assoc parameter-map option new-val)))

(defn mk-short-opt
  "Converts short climatic option to short tools.cli option"
  [short]
  (if (string? short)
    (str "-" short)
    nil))

(defn mk-long-opt
  "Converts long climatic option to long tools.cli option"
  [option placeholder type]
  (str "--"
       (when (= :with-flag type) "[no-]")
       option
       (when (not= "" placeholder) " ")
       placeholder))

(defn mk-env-name
  "Writes a description with the env name by the end."
  [description env for-parsing?]
  (if (and (not for-parsing?)
           (some? env))
    (str description " [$" env "]")
    description))

(defn get-cli-option
  [type]
  (cond

    ; is this a set option?
    (set? type)
    {:parse-fn (partial PRESETS/asSet type)
     :placeholder (PRESETS/set-help-values type)}

    ; normal preset
    ; we check that we know what to do with it
    :else
    (let [preset (get PRESETS/known-presets type :unknown)]
      (if (= preset :unknown)
        ; throw exception
        (throw (ex-info
                (str "Unknown  preset: " type " - Aborting")
                {}))

        ; return  preset found
        preset))))

(defn mk-cli-option
  "Builds a tools.cli option out of our own format.

  If for-parsing is true, the option will be used for parsing;
  if false, for generating help messages.

  "
  [{:keys [option short as type default multiple env]}]

  (let [as_description (asString as)
        preset (get-cli-option type)
        placeholder (str (:placeholder preset)
                         (if (= :present default) "*" ""))
        positional-opts [(mk-short-opt short)
                         (mk-long-opt option placeholder type)
                         (mk-env-name as_description env false)]

        ;; step 1 - remove :placeholder
        opts-1 (dissoc preset :placeholder)

        ;; step 2 - add default if present and is not ":present"
        opts-2 (if (and (some? default)
                        (not= :present default))
                 (assoc opts-1 :default default)
                 opts-1)

        ;; step 3 - if multivalue, add correct assoc-fns
        opts-3 (if multiple
                 (assoc opts-2 :assoc-fn assoc-new-multivalue)
                 opts-2)]
    (apply
     conj positional-opts
     (flatten (seq opts-3)))))

(s/fdef mk-cli-option
  :args (s/cat :opts ::S/climatic-option)
  :ret some?)


(defn all-subcommands-aliases
  "Maps all subcommands and subcommand aliases
  to their canonical name.
  E.g. {'add': 'add', 'a': 'add'}.

  We basically add them all, then remove
  nil keys.

  "

  [climatic-args]
  (let [subcommands (:commands climatic-args)]

    (dissoc
     (merge
        ;; a map of 'cmd' -> 'cmd'
      (into {}
            (map
             (fn [{:keys [command]}]
               [command command])
             subcommands))

      (into {}
            (map
             (fn [{:keys [command short]}]
               [short command])
             subcommands)))
     nil)))

(s/fdef all-subcommands-aliases
  :args (s/cat :args ::S/climatic-cfg-classic)
  :ret (s/map-of string? string?))

(defn all-subcommands
  "Returns all subcommands, as strings.
   We get all versions of all subcommands.
  "
  [climatic-args]
  (set (keys (all-subcommands-aliases climatic-args))))

(s/fdef all-subcommands
  :args (s/cat :args ::S/climatic-cfg-classic)
  :ret set?)

(defn canonicalize-subcommand
  "Returns the 'canonical' name of a subcommand,
  i.e. the one that appears in :command, even
  if we pass an alias or short version."
  [commands subcmd]
  (get (all-subcommands-aliases commands) subcmd))

(s/fdef canonicalize-subcommand
  :args (s/cat :args ::S/climatic-cfg-classic :sub string?)
  :ret string?)


;; Out of a cli-matic arg list,
;; generates a set of commands for tools.cli
(defn cm-opts->cli-opts
  "
  Out of a cli-matic arg list, generates a set of
  options for tools.cli.
  It also adds in the -? and --help options
  to trigger display of helpness.
  "
  [climatic-opts]
  (conj
   (mapv mk-cli-option climatic-opts)
   ["-?" "--help" "" :id :_help_trigger]))



;; -------------------------------------------------------------
;; POSITIONAL PARAMETERS
; Positional parameters:
; 1- are only valid in subcommands
; 2- appear on help
; 3- capture from the "leftovers" vector :_arguments
;; --------------------------------------------------------------


(defn positional-parms-from-opts
  "
  Returns a list of positional parameters from a subcommand option.
  "
  [opts]
  (filterv #(integer? (:short %)) opts))

(s/fdef
  positional-parms-from-opts
  :args (s/cat :opts ::S/opts)
  :ret (s/coll-of ::S/climatic-option))

(defn a-positional-parm
  "Reads one positional parameter from the arguments.
  Returns a vector [parm value]
  The value is NOT solved, so it's always a string."
  [args option]
  (let [pos (:short option)
        lbl (:option option)
        val (get args pos nil)]
    [lbl val]))

(s/fdef
  a-positional-parm
  :args (s/cat :args sequential?
               :opt  ::S/climatic-option)
  :ret vector?)

(defn capture-positional-parms
  "Captures positional parameters in the remaining-args of
  a subcommand."
  [opts remaining-args]
  (let [pp (positional-parms-from-opts opts)]
    (into {}
          (map (partial a-positional-parm remaining-args) pp))))

(s/fdef
  capture-positional-parms
  :args (s/cat :opts ::S/opts
               :args sequential?)
  :ret ::S/mapOfCliParams)

(defn exit!
  "Raises an exception that will print a message
  without the stack trace. Can specify an optional
  exit code; if not specified, zero."
  [message errorcode]
  (throw
   (ex-info (str "**CLI-MATIC**:" message)
            {:cli-matic 1973
             :message   message
             :errorcode errorcode})))

(defn exception-info
  "Checks the exception; if it was created using [[exit!]]
   we get that data out; if not, standard exception.

   Returns [message exit-code].
  "
  [e]
  (let [data (ex-data e)]
    (cond
      (= 1973 (:cli-matic data))
      [(:message data) (:errorcode data)]

      :else
      [(str "Exception: "
            (with-out-str (println e))) -1])))
