(ns cli-matic.core
  (:require [cli-matic.specs :as S]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]
            [clojure.string :as str]
            [cli-matic.presets :as PRESETS]
            [cli-matic.platform :as P]))

;; ================ ATTENTION ====================
;; Cli-matic has one main entry-point: run!
;; Actually, most of the logic will be run in run*
;; to make testing easier.
;;
;; -----------------------------------------------


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

;; Rewrite options from our format
;; {:opt "x" :as "Port number" :type :int}
;; to tools.cli:
;; ["-x" nil "Port number"
;;  :parse-fn #(Integer/parseInt %)]
;; as specified in
;;  https://github.com/clojure/tools.cli/blob/master/src/main/clojure/clojure/tools/cli.clj#L488
;;

(defn mk-env-name
  "Writes a description with the env name by the end."
  [description env for-parsing?]
  (if (and (not for-parsing?)
           (some? env))
    (str description " [$" env "]")
    description))

(defn mk-cli-option
  "Builds a tools.cli option out of our own format.

  If for-parsing is true, the option will be used for parsing;
  if false, for generating help messages.

  "
  [{:keys [option short as type default multiple env] :as cm-option}]

  (let [preset (get PRESETS/known-presets type :unknown)
        placeholder (str (:placeholder preset)
                         (if (= :present default) "*" ""))
        positional-opts [(if (string? short)
                           (str "-" short)
                           nil)
                         (str "--" option " " placeholder)
                         (mk-env-name as env false)]

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

(defn get-subcommand
  "Given args and the canonical name of a subcommand,
  returns the map describing it.
  "
  [climatic-args subcmd]
  (let [subcommands (:commands climatic-args)]
    (first (filter #(= (:command %) subcmd) subcommands))))

(s/fdef get-subcommand
        :args (s/cat :args ::S/climatic-cfg :subcmd string?)
        :ret ::S/a-command)

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
             (fn [{:keys [command short]}]
               [command command])
             subcommands))

      (into {}
            (map
             (fn [{:keys [command short]}]
               [short command])
             subcommands)))
     nil)))

(s/fdef all-subcommands-aliases
        :args (s/cat :args ::S/climatic-cfg)
        :ret (s/map-of string? string?))

(defn all-subcommands
  "Returns all subcommands, as strings.
   We get all versions of all subcommands.
  "
  [climatic-args]
  (set (keys (all-subcommands-aliases climatic-args))))

(s/fdef all-subcommands
        :args (s/cat :args ::S/climatic-cfg)
        :ret set?)

(defn canonicalize-subcommand
  "Returns the 'canonical' name of a subcommand,
  i.e. the one that appears in :command, even
  if we pass an alias or short version."
  [commands subcmd]
  (get (all-subcommands-aliases commands) subcmd))

(s/fdef canonicalize-subcommand
        :args (s/cat :args ::S/climatic-cfg :sub string?)
        :ret string?)

(defn get-options-for
  "Gets specific :options for a subcommand or,
  if nil, for global."
  [climatic-args subcmd]
  (if (nil? subcmd)
    (:global-opts climatic-args)
    (:opts (get-subcommand climatic-args subcmd))))

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

(defn rewrite-opts
  "
  Out of a cli-matic arg list, generates a set of
  options for tools.cli.
  It also adds in the -? and --help options
  to trigger display of helpness.
  "
  [climatic-args subcmd]
  (cm-opts->cli-opts (get-options-for climatic-args subcmd)))

(s/fdef rewrite-opts
        :args (s/cat :args some?
                     :mode (s/or :common nil?
                                 :a-subcommand string?))
        :ret some?)

;; -------------------------------------------------------------
;; POSITIONAL PARAMETERS
; Positional parameters:
; 1- are only valid in subcommands
; 2- appear on help
; 3- capture from the "leftovers" vector :_arguments
;; --------------------------------------------------------------


(defn list-positional-parms
  "Extracts all positional parameters from the configuration."
  [cfg subcmd]
  ;;(prn "CFG" cfg "Sub" subcmd)
  (let [opts (get-options-for cfg subcmd)
        rv (filterv #(integer? (:short %)) opts)]
    ;;(prn "Subcmd" subcmd "OPTS" opts "RV" rv )
    rv))

(s/fdef
 list-positional-parms
 :args (s/cat :cfg ::S/climatic-cfg :cmd (s/or :cmd ::S/command :global nil?))
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
  [cfg subcmd remaining-args]
  (let [pp (list-positional-parms cfg subcmd)]
    (into {}
          (map (partial a-positional-parm remaining-args) pp))))

(s/fdef
 capture-positional-parms
 :args (s/cat :cfg ::S/climatic-cfg :cmd ::S/command :args sequential?)
 :ret ::S/mapOfCliParams)

(defn arg-list-with-positional-entries
  "Creates the `[arguments...]`"
  [cfg cmd]
  (let [pos-args (sort-by :short (list-positional-parms cfg cmd))]
    (if (empty? pos-args)
      "[arguments...]"
      (str
       (apply str (map :option pos-args))
       " ..."))))

;; ------------------------------------------------
;; Stuff to generate help pages
;; ------------------------------------------------

(defn asString
  "Turns a collection of strings into one string,
  or the string itself."
  [s]
  (if (string? s)
    s
    (str/join "\n" s)))

(defn indent-string
  "Indents a single string."
  [s]
  (str " " s))

(defn indent
  "Indents a single string, or each string
  in a collection of strings."
  [s]
  (if (string? s)
    (indent-string s)
    (map indent-string (flatten s))))

(defn generate-section
  "Generates a section (as a collection of strings,
  possibly nested, but we'll flatten it out).
  If a section has no content, we return [].
  "
  [title lines]
  (if (empty? lines)
    []

    [(str title ":")
     (indent lines)
     ""]))

(defn generate-sections
  "Generates all sections.
  All those positional parameters are not that nice.
  "
  [name version usage commands opts-title opts]

  (vec
   (flatten
    [(generate-section "NAME" name)
     (generate-section "USAGE" usage)
     (generate-section "VERSION" version)
     (generate-section "COMMANDS" commands)
     (generate-section opts-title opts)])))

(defn get-options-summary
  "To get the summary of options, we pass options to
  tools.cli parse-opts and an empty set of arguments.
  Parsing will fail but we get the :summary.
  We then split it into a collection of lines."
  [cfg subcmd]
  (let [cli-cfg (rewrite-opts cfg subcmd)
        options-str (:summary
                     (parse-opts [] cli-cfg))]
    (str/split-lines options-str)))

(defn get-first-rest-description-rows
  "get title and description of description rows"
  [row-or-rows]
  (cond
    (string? row-or-rows)
    [row-or-rows []]

    (zero? (count row-or-rows))
    ["?" []]

    :else
    [(first row-or-rows) (rest row-or-rows)]))

(defn pad
  "Pads s[, s1] to so many characters"
  [s s1 len]
  (subs (str s
             (when s1
               (str ", " s1))
             "                   ")
        0 len))

(defn generate-a-command
  "Maybe we should use a way to format commands

   E.g.
   (pp/cl-format true \"~{ ~vA  ~vA  ~vA ~}\" v)


   (clojure.pprint/cl-format true \"~3a ~a\" \"pippo\" \"pluto\")
   "

  [{:keys [command short description]}]

  (let [[des0 _] (get-first-rest-description-rows description)]
    (str "  "
         (pad command short 20)
         " "
         des0)))

(defn generate-global-command-list
  "Creates a list of commands and descriptions.
   Commands are of kind ::S/commands
  "
  [commands]
  (map generate-a-command commands))

(s/fdef
 generate-global-command-list
 :args (s/cat :commands ::S/commands)
 :ret  (s/coll-of string?))

(defn generate-global-help
  "This is where we generate global help, so
  global attributes and subcommands."

  [cfg]

  (let [name (get-in cfg [:app :command])
        version (get-in cfg [:app :version])
        descr (get-in cfg [:app :description])
        [desc0 descr-extra] (get-first-rest-description-rows descr)]

    (generate-sections
     [(str name " - " desc0) descr-extra]
     version
     (str name " [global-options] command [command options] [arguments...]")
     (generate-global-command-list (:commands cfg))
     "GLOBAL OPTIONS"
     (get-options-summary cfg nil))))

(s/fdef
 generate-global-help
 :args (s/cat :cfg ::S/climatic-cfg)
 :ret (s/coll-of string?))

(defn generate-subcmd-help
  "This is where we generate help for a specific subcommand."
  [cfg cmd]

  (let [glname (get-in cfg [:app :command])
        cmd-cfg (get-subcommand cfg cmd)
        name (:command cmd-cfg)
        shortname (:short cmd-cfg)
        name-short (if shortname
                     (str "[" name "|" shortname "]")
                     name)
        descr (:description cmd-cfg)
        [desc0 descr-extra] (get-first-rest-description-rows descr)
        arglist (arg-list-with-positional-entries cfg cmd)]

    (generate-sections
     [(str glname " " name " - " desc0) descr-extra]
     nil
     (str glname " " name-short " [command options] " arglist)
     nil
     "OPTIONS"
     (get-options-summary cfg cmd))))

(s/fdef
 generate-subcmd-help
 :args (s/cat :cfg ::S/climatic-cfg :cmd ::S/command)
 :ret (s/coll-of string?))

;; -----------------------------------------------------
;; Here we parse our command line.
;; -----------------------------------------------------

(defn mkError
  "Builds an error condition."
  [config subcommand error text]
  {:subcommand     subcommand
   :subcommand-def (if (or (= error :ERR-UNKNOWN-SUBCMD)
                           (= error :ERR-NO-SUBCMD)
                           (= error :ERR-PARMS-GLOBAL)
                           (= error :HELP-GLOBAL))
                     nil
                     (get-subcommand config subcommand))
   :commandline    {}
   :parse-errors   error
   :error-text     (asString text)})

;; TODO s/fdef
(s/fdef
 mkError
 :args (s/cat :config ::S/climatic-cfg
              :subcmd ::S/subcommand
              :err    (s/or :err ::S/climatic-errors
                            :help ::S/help)
              :text   any?)
 :ret ::S/lineParseResult)

(defn parse-single-arg
  "Parses and validates a single command.
  Returns its value, and an error message,
  in a vector [:keyword err val].
  Parsing is OK if error message is nil.

  Sequence is:
  - parsing (eg `2` -> 2), on exception parse error
  - validation via spec
  - validation via function, on exception validation error

  "
  [optionDef stringValue]
  (let [label (get optionDef :option)
        parseFn (get optionDef :parse-fn identity)
        valdationSpec (get optionDef :xx identity)
        validationFn (get optionDef :validate-fn (constantly true))]

    (try
      (let [v-parsed (parseFn stringValue)]
        [label nil v-parsed])

      (catch Throwable t
        [label (str "Cannot parse " label) nil]))))

(s/fdef
 parse-single-arg
 :args (s/cat :opt ::S/climatic-option :val string?)
 :ret (s/cat :lbl keyword?
             :err (s/or :s string? :n nil?)
             :val any?))

(defn errors-for-missing-mandatory-args
  "Gets us a sequence of errors if mandatory options are missing.
  Options read by cli module are merged with other options, e.g.
  positional parameters.
  "
  [climatic-options parsed-opts other-options]
  (let [mandatory-options (filter
                           #(= :present (:default %))
                           climatic-options)
        curr-options (:options parsed-opts)
        all-curr-options (into other-options curr-options)]

    (reduce
     (fn [a v]
       (let [optname  (:option v)
             val (get all-curr-options (keyword optname) :MISSING)]

         (if (= :MISSING val)
           (conj a (str "Missing option: " optname))
           a)))
     []
     mandatory-options)))

(s/fdef errors-for-missing-mandatory-args
        :args (s/cat :options ::S/opts
                     :parsed-opts map?
                     :other-options map?)
        :ret (s/coll-of string?))

;; RUN ANALYSIS

(defn mk-fake-args
  "Builds the set of fake arguments that we append to our
  subcommands' own CLI items
  when we have positional parameters.
  If value is nil, option is not added.

  We receive a map of options and output a vector of strings.
  "
  [parms]
  (vec
   (flatten
    (map (fn [[k v]]
           (if (nil? v)
             []
             [(str "--" (name k)) (str v)]))
         parms))))

(s/fdef
 mk-fake-args
 :args (s/cat :parms ::S/mapOfCliParams)
 :ret (s/coll-of string?))

(defn parse-cmds-with-defaults
  "Parses a command line with environemt defaults.
   We want environment defaults to be PARSED, so they will go through
   the same validation/check cycle as other elements.
   So - if any of them - we first run parsing disabling defaults,
   then go check if they are available in parsed elements;
   if they are not, we inject them as options to the left of argv
   and parse again.
   (as a side effect, if you have a wrong value for your option, and a
   default, the default will be used - YMMV).

  "

  [cmt-options argv in-order? fn-env]
  (let [cli-cmd-options (cm-opts->cli-opts cmt-options)
        env-options (filter :env cmt-options)
        argv+ (if (not (empty? env-options))
                ;; I have env variables

                (let [parse1 (parse-opts argv cli-cmd-options
                                         :in-order in-order?
                                         ::no-defaults true)
                      set-keys-in (set (keys (:options parse1)))
                      missing-cmt-options (filter (complement set-keys-in) env-options)
                      map-missing-keys (into {}
                                             (map
                                              (fn [{:keys [option env]}]
                                                [option (fn-env env)])
                                              missing-cmt-options))]
                  (into (mk-fake-args map-missing-keys) argv))

                ;; no env variables - we are good
                argv)]

    (parse-opts argv+ cli-cmd-options :in-order in-order?)))

(s/fdef
 parse-cmds-with-defaults
 :args (s/cat :opts ::S/opts
              :argv (s/coll-of string?)
              :in-order boolean?
              :fn-env any?)
 :ret  ::S/parsedCliOpts)

(defn parse-cmds-with-positions
  "To process positional parameters, first we run some parsing; if
  all goes well, we capture the values of positional
  arguments and run parsing again with a command line that has those
  items as if they were expressed.

  This means that type casting and validation just happen in one place
  (CLI parsing) and  we don't have to do them separately.

  This function is used both for global and subcmd parsing,
  but when doing global parsing, positional parameters are
  not allowed, so they never come in.
  "
  [config canonical-subcommand subcommand-parms]
  (let [cmt-options (get-options-for config canonical-subcommand)
        parsed-cmd-opts (parse-cmds-with-defaults cmt-options subcommand-parms false P/read-env)
        cmd-args (:arguments parsed-cmd-opts)

        ;; capture positional parms
        ;; if they exist, we inject them at the end of the command line
        ;; and parse again
        positional-parms (capture-positional-parms config canonical-subcommand cmd-args)]

    (cond
      (pos? (count positional-parms))
      (let [addl-args (mk-fake-args positional-parms)
            newcmdline (into subcommand-parms addl-args)]
        (parse-cmds-with-defaults cmt-options newcmdline false P/read-env))

      :else
      parsed-cmd-opts)))

(defn check-one-spec
  "Checks one spec.
  If spec passes, returns nil; if not, returns the failure.
  If there is an error raised, creates a fake spec result.

  explain-data return nil if everything okay.
  "
  [name spec value]
  (try
    (let [ed (s/explain-data spec value)]
      (if (some? ed)
        (str "Spec failure for '" name "': value '" value "' is invalid.")
        nil))

    (catch Throwable t
      (str "Spec failure for '" name "': with value '" value "' got " t))))

(s/fdef
 check-one-spec
 :args (s/cat :name string?
              :spec ::S/spec
              :value ::S/anything)
 :ret  (s/or :nil nil?
             :str string?))

(defn check-specs-on-parameters
  "Given a set of option (so, global options, or a subcommand's options)
  and the fully parsed results, we assert that any defined specs pass.
  "
  [options parsed-results]
  (prn "Validating Specs" options parsed-results)
  (let [cmds-with-specs (filter #(some? (:spec %)) options)
        specs-applied (map #(check-one-spec (:option %)
                                            (:spec %)
                                            (get parsed-results (keyword (:option %))))
                           cmds-with-specs)]
    (filter some? specs-applied)))

(s/fdef
 check-specs-on-parameters
 :args (s/cat :options ::S/opts
              :parsed-results map?))

(defn check-specs-on-parsed-args
  "As a last step, before we call the subcommand itself, we assert
  that any spec that was actually defined is passed.

  We just care about the first spec that fails, so we can get a
  lazy list of failures and get the first of them (or nil).
  "

  [parsed-args canonical-subcommand config]

  (let [globals-opts (get-options-for config nil)
        subcmd-def  (get-subcommand config canonical-subcommand)
        subcmd-opts  (get-options-for config canonical-subcommand)

        ; if we have no subcmd spec, we just call (true) instead
        subcmd-spec  (get subcmd-def :spec (constantly true))

        _ (prn "SUB: globals" globals-opts)
        _ (prn "SUB: def" subcmd-opts)
        _ (prn "Spec for subcmd " subcmd-spec)

        failing-global-spec (first (check-specs-on-parameters globals-opts parsed-args))
        failing-subcmd-spec (first (check-specs-on-parameters subcmd-opts parsed-args))
        failing-subcmd-general (check-one-spec canonical-subcommand subcmd-spec parsed-args) _ (prn "Failing global" failing-global-spec)
        _ (prn "Failing local" failing-subcmd-spec)
        _ (prn "Failing total" failing-subcmd-general)]

    (cond
      (some? failing-global-spec)
      (mkError config nil :ERR-PARMS-GLOBAL failing-global-spec)

      (some? failing-subcmd-spec)
      (mkError config canonical-subcommand :ERR-PARMS-SUBCMD failing-subcmd-spec)

      (some? failing-subcmd-general)
      (mkError config canonical-subcommand :ERR-PARMS-SUBCMD failing-subcmd-general)

      :else
      ; all went well.... phew!
      {:subcommand     canonical-subcommand
       :subcommand-def subcmd-def
       :commandline    parsed-args
       :parse-errors    :NONE
       :error-text     ""})))

(s/fdef
 check-specs-on-parsed-args
 :args (s/cat :parsed-args map?
              :canonical-subcommand string?
              :config ::S/climatic-cfg)
 :ret ::S/lineParseResult)

(defn parse-cmds
  "This is where magic happens.
  We first parse global options, then stop,
  get the subcommand, parse specific options for the subcommand
  and if all went well we prepare run it.

  This function returns a structure ::S/lineParseResult
  that contains information about what went wrong or the command
  to run.
  "
  [argv config]

  (let [gl-options (get-options-for config nil)
        ;_ (prn "Cmdline" cmdline)
        parsed-gl-opts (parse-cmds-with-defaults gl-options argv true P/read-env) ;(parse-opts cmdline cli-gl-options :in-order true)
        missing-gl-opts (errors-for-missing-mandatory-args
                         (get-options-for config nil)
                         parsed-gl-opts {})
        ;_ (prn "Common cmdline" parsed-common-cmdline)
        {gl-errs :errors gl-opts :options gl-args :arguments} parsed-gl-opts]

    (cond
      ; any parse errors?
      (some? gl-errs)
      (mkError config nil :ERR-PARMS-GLOBAL gl-errs)

      ; did we ask for help?
      (some? (:_help_trigger gl-opts))
      (mkError config nil :HELP-GLOBAL nil)

      :else
      (let [subcommand (first gl-args)
            subcommand-argv (vec (rest gl-args))]

        (cond
          (nil? subcommand)
          (mkError config nil :ERR-NO-SUBCMD "")

          (nil? ((all-subcommands config) subcommand))
          (mkError config subcommand :ERR-UNKNOWN-SUBCMD "")

          :else
          (let [canonical-subcommand (canonicalize-subcommand config subcommand)
                parsed-cmd-opts (parse-cmds-with-positions config canonical-subcommand subcommand-argv);_ (prn "Subcmd cmdline" parsed-cmd-opts)
                ;_ (prn "G" missing-gl-opts)
                ;_ (prn "C" missing-cmd-opts)
                {cmd-errs :errors cmd-opts :options cmd-args :arguments} parsed-cmd-opts;; run checks on parameters
                missing-cmd-opts (errors-for-missing-mandatory-args
                                  (get-options-for config canonical-subcommand)
                                  parsed-cmd-opts {})]

            (cond
              ; asking for help?
              (some? (:_help_trigger cmd-opts))
              (mkError config canonical-subcommand :HELP-SUBCMD nil)

              ; any missing required global parm?
              (pos? (count missing-gl-opts))
              (mkError config nil :ERR-PARMS-GLOBAL missing-gl-opts)

              ; missing required parms?
              (pos? (count missing-cmd-opts))
              (mkError config canonical-subcommand :ERR-PARMS-SUBCMD missing-cmd-opts)

              ; no errors?
              ; as a last step, we assert that specs are okay
              (nil? cmd-errs)
              (let [parsed-opts (-> {}
                                    (into gl-opts)
                                    (into cmd-opts)
                                    (into {:_arguments cmd-args}))]
                (check-specs-on-parsed-args parsed-opts canonical-subcommand config))

              ;; sth went wrong
              :else
              (mkError config canonical-subcommand :ERR-PARMS-SUBCMD cmd-errs))))))))

(s/fdef
 parse-cmds
 :args (s/cat :args (s/coll-of string?)
              :opts ::S/climatic-cfg)
 :ret ::S/lineParseResult)

(defn assert-unique-values
  "Check that all values are unique.
  name is the area of the configuration
  vec-opts are the options to check
  option is the keyword to check.
  "
  [name vec-opts option]

  (let [optName (if (nil? name) "global" name)
        allOptions (filter some? (map option vec-opts))
        dupes (filterv (fn [[k v]]  (> v 1)) (frequencies allOptions))]
    (cond
      (not (empty? dupes))
      (throw (IllegalAccessException.
              (str "In option area: " optName " for options of type " option " some option names are not unique: " dupes))))))

(s/fdef
 assert-unique-values
 :args (s/cat :name (s/or :some-subcmd ::S/existing-string
                          :global  nil?)
              :vec-opts any? ; ::S/commands
              :option keyword?))

;;
;; Asserts sanity of initial configuration.
;; If this goes wrong, throws an error.
;;
(defn assert-cfg-sanity
  "Checks configuration and throws if anything wrong.

  1. are :option values unique?
  2. are :short values unique?
  3. do we have any positional arguments in global config?

  First we make a list of `nil` plus all subcmds.

  "
  [currentCfg]

  (do
    ;; checks positional parameters

    (let [global-positional-parms (list-positional-parms currentCfg nil)]

      (if (pos? (count global-positional-parms))
        (throw (IllegalAccessException.
                (str "Positional parameters not allowed in global options. " global-positional-parms)))));; checks subcommands
    (let [all-subcommands (into [nil]
                                (all-subcommands currentCfg))]
      (doall (map #(assert-unique-values %
                                         (get-options-for currentCfg %)
                                         :option) all-subcommands))
      (doall (map #(assert-unique-values %
                                         (get-options-for currentCfg %)
                                         :short) all-subcommands))))
  ;; just say nil
  nil)

(s/fdef assert-cfg-sanity
        :args (s/cat :opts ::S/climatic-cfg))

;
; builds a return value
;

(defn ->RV
  "This is a Return Value, i.e. what happens after the
  parsing is done and possibly the subcommand was invoked."
  [return-code type stdout subcmd stderr]
  (let [fnStrVec (fn [s]
                   (cond
                     (nil? s) []
                     (string? s) [s]
                     :else  s))]

    {:retval return-code
     :status type
     :help   stdout
     :subcmd subcmd
     :stderr (fnStrVec stderr)}))

(s/fdef
 ->RV
 :args (s/cat :rv int? :status some? :help any? :subcmd any? :stderr any?)
 :rets ::S/RV)

;
;
;
;

(defn invoke-subcmd
  "Invokes a subcommand, and produces a Return Value.

   The subcommand may:
    - return an integer (to specify exit code)
    - return nil
    - throw a Throwable object

  "
  [subcommand-def options]

  (try
    (let [rv ((:runs subcommand-def)  options)]
      (cond
        (nil? rv)    (->RV 0 :OK nil nil nil)
        (int? rv)   (if (zero? rv)
                      (->RV 0 :OK nil nil nil)
                      (->RV rv :ERR nil nil nil))

        :else        (->RV 0 :OK nil nil nil)))

    (catch Throwable t
      (->RV -1 :EXCEPTION nil nil
            (str "JVM Exception: "
                 (with-out-str (println t)))))))

;; Executes our code.
;; It will try and parse the arguments via clojure.tools.cli
;; and detect our subcommand.

;; If no subcommand was found, it will print the error reminder.
;; On exceptions, it will raise an exception message.
(defn run-cmd*
  [setup args]
  (let [parsed-opts (parse-cmds args setup)]
    ;; maybe there was an error parsing
    (condp = (:parse-errors parsed-opts)
      :ERR-CFG (->RV -1 :ERR-CFG nil nil  "Error in cli-matic configuration")
      :ERR-NO-SUBCMD (->RV -1 :ERR-NO-SUBCMD :HELP-GLOBAL nil "No sub-command specified")
      :ERR-UNKNOWN-SUBCMD (->RV -1 :ERR-UNKNOWN-SUBCMD :HELP-GLOBAL nil "Unknown sub-command")
      :HELP-GLOBAL (->RV 0 :OK :HELP-GLOBAL nil nil)
      :ERR-PARMS-GLOBAL (->RV -1 :ERR-PARMS-GLOBAL :HELP-GLOBAL nil
                              (str "Global option error: " (:error-text parsed-opts)))
      :HELP-SUBCMD (->RV 0 :OK :HELP-SUBCMD (:subcommand parsed-opts) nil)
      :ERR-PARMS-SUBCMD (->RV -1 :ERR-PARMS-SUBCMD :HELP-SUBCMD (:subcommand parsed-opts)
                              (str "Option error: " (:error-text parsed-opts)))
      :NONE (invoke-subcmd (:subcommand-def parsed-opts) (:commandline parsed-opts)))))

(defn run-cmd
  "This is the actual function that is executed.
  It wraps run-cmd* and then does the printing
  of any errors, of help pages and  System.exit.
  As it invokes Sys.exit you cannot use it from a REPL.
  "
  [args setup]
  (let [{:keys [help stderr subcmd retval]}
        (run-cmd* setup (if (nil? args) [] args))]
    (if (not (empty? stderr))
      (println
       (asString
        (flatten
         ["** ERROR: **" stderr "" ""]))))
    (cond
      (= :HELP-GLOBAL help)
      (println (asString (generate-global-help setup)))
      (= :HELP-SUBCMD help)
      (println (asString (generate-subcmd-help setup subcmd))))
    (P/exit-script retval)))

(st/instrument)
