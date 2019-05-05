(ns cli-matic.core
  "
  ### ATTENTION

  CLI-matic has one main entry-point: [[run-cmd]].

  As an end-user, you need nothing else,  but the documentation
  that explains how parameters are to be run.

  See `examples/` to get started quickly.

  *Developers*

  Most of the logic will be run in [[run-cmd*]] to make testing easier,
  as [[run-cmd]] calls `System/exit`.

  "
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.spec.alpha :as s]
            [cli-matic.specs :as S]
            [cli-matic.help-gen :as H]
            [cli-matic.platform :as P]
            #?(:clj [cli-matic.platform-macros :refer [try-catch-all]]
               :cljs [cli-matic.platform-macros :refer-macros [try-catch-all]])
            [cli-matic.utils :as U]
            [cli-matic.optionals :as OPT]
            [expound.alpha :as expound]))

(defn mkError
  "Builds an error condition."
  [config subcommand error text]
  {:subcommand     subcommand
   :subcommand-def (if (or (= error :ERR-UNKNOWN-SUBCMD)
                           (= error :ERR-NO-SUBCMD)
                           (= error :ERR-PARMS-GLOBAL)
                           (= error :HELP-GLOBAL))
                     nil
                     (U/get-subcommand config subcommand))
   :commandline    {}
   :parse-errors   error
   :error-text     (U/asString text)})

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

    (try-catch-all
     (let [v-parsed (parseFn stringValue)]
       [label nil v-parsed])

     (fn [t]
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
  "Parses a command line with environment defaults.

   We want environment defaults to be PARSED, so they will go through
   the same validation/check cycle as other elements.
   So - if any of them - we first run parsing disabling defaults,
   then go check if they are available in parsed elements;
   if they are not, we inject them as options to the left of argv
   and parse again.

   (As a side effect, if you have a wrong value for your option, and a
   default, the default will be used - YMMV).

  "

  [cmt-options argv in-order? fn-env]
  (let [cli-cmd-options (U/cm-opts->cli-opts cmt-options)
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
  (let [cmt-options (U/get-options-for config canonical-subcommand)
        parsed-cmd-opts (parse-cmds-with-defaults cmt-options subcommand-parms false P/read-env)
        cmd-args (:arguments parsed-cmd-opts)

        ;; capture positional parms
        ;; if they exist, we inject them at the end of the command line
        ;; and parse again
        positional-parms (U/capture-positional-parms config canonical-subcommand cmd-args)]

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

  expound-string returns 'Success!\n' if all goes well.


  "
  [name type spec value]
  (try-catch-all
   (let [ed (expound/expound-str spec value)]
     (if (not= ed "Success!\n")
        ;(str "Spec failure for '" name "': value '" value "' is invalid.")
       (str "Spec failure for " type " '" name "'\n" ed)

       nil))

   (fn [t]
     (str "Spec failure for " type " '" name "': with value '" value "' got " t))))

(s/fdef
  check-one-spec
  :args (s/cat :name string?
               :type string?
               :spec ::S/spec
               :value ::S/anything)
  :ret  (s/or :nil nil?
              :str string?))

(defn check-specs-on-parameters
  "Given a set of option (so, global options, or a subcommand's options)
  and the fully parsed results, we assert that any defined specs pass.
  "
  [options parsed-results type]
  ;(prn "Validating Specs" options parsed-results)
  (let [cmds-with-specs (filter #(some? (:spec %)) options)
        specs-applied (map #(check-one-spec (:option %)
                                            type
                                            (:spec %)
                                            (get parsed-results (keyword (:option %))))
                           cmds-with-specs)]
    (filter some? specs-applied)))

(s/fdef
  check-specs-on-parameters
  :args (s/cat :options ::S/opts
               :parsed-results map?
               :type string?))

(defn check-specs-on-parsed-args
  "As a last step, before we call the subcommand itself, we assert
  that any spec that was actually defined is passed.

  We just care about the first spec that fails, so we can get a
  lazy list of failures and get the first of them (or nil).
  "

  [parsed-args canonical-subcommand config]

  (let [globals-opts (U/get-options-for config nil)
        subcmd-def  (U/get-subcommand config canonical-subcommand)
        subcmd-opts  (U/get-options-for config canonical-subcommand)

        ; if we have no subcmd spec, we just call (true) instead
        subcmd-spec  (get subcmd-def :spec (constantly true))

        ;
        ;_ (prn "SUB: globals" globals-opts)
        ;_ (prn "SUB: def" subcmd-opts)
        ;_ (prn "Spec for subcmd " subcmd-spec)

        failing-global-spec (first (check-specs-on-parameters globals-opts parsed-args "global option"))
        failing-subcmd-spec (first (check-specs-on-parameters subcmd-opts parsed-args "option"))
        failing-subcmd-general (check-one-spec canonical-subcommand "subcommand" subcmd-spec parsed-args)

        ; 
        ;_ (prn "Failing global" failing-global-spec)
        ;_ (prn "Failing local" failing-subcmd-spec)
        ;_ (prn "Failing total" failing-subcmd-general))
        ]

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
  and if all went well we prepare to run it.

  This function returns a structure ::S/lineParseResult
  that contains information about what went wrong or the command
  to run.
  "
  [argv config]

  (let [gl-options (U/get-options-for config nil)
        ;_ (prn "Cmdline" cmdline)
        parsed-gl-opts (parse-cmds-with-defaults gl-options argv true P/read-env) ;(parse-opts cmdline cli-gl-options :in-order true)
        missing-gl-opts (errors-for-missing-mandatory-args
                         (U/get-options-for config nil)
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

          (nil? ((U/all-subcommands config) subcommand))
          (mkError config subcommand :ERR-UNKNOWN-SUBCMD
                   (H/generate-help-possible-mistypes config subcommand))

          :else
          (let [canonical-subcommand (U/canonicalize-subcommand config subcommand)
                parsed-cmd-opts (parse-cmds-with-positions config canonical-subcommand subcommand-argv);_ (prn "Subcmd cmdline" parsed-cmd-opts)
                ;_ (prn "G" missing-gl-opts)
                ;_ (prn "C" missing-cmd-opts)
                {cmd-errs :errors cmd-opts :options cmd-args :arguments} parsed-cmd-opts;; run checks on parameters
                missing-cmd-opts (errors-for-missing-mandatory-args
                                  (U/get-options-for config canonical-subcommand)
                                  parsed-cmd-opts {})]

            (cond
              ; asking for help?
              (some? (:_help_trigger cmd-opts))
              (mkError config canonical-subcommand :HELP-SUBCMD nil)

              ; any missing required global parm?
              ; we raise only if there are no rrors in global parms
              (and (empty? gl-errs) (pos? (count missing-gl-opts)))
              (mkError config nil :ERR-PARMS-GLOBAL missing-gl-opts)

              ; missing required parms?
              ; we raise this only if there are no errors in cmd parms
              (and (empty? cmd-errs) (pos? (count missing-cmd-opts)))
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
      (throw (ex-info
              (str "In option area: " optName " for options of type " option " some option names are not unique: " dupes)
              {})))))

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

    (let [global-positional-parms (U/list-positional-parms currentCfg nil)]

      (if (pos? (count global-positional-parms))
        (throw (ex-info
                (str "Positional parameters not allowed in global options. " global-positional-parms)
                {}))));; checks subcommands
    (let [all-subcommands (into [nil]
                                (U/all-subcommands currentCfg))]
      (doall (map #(assert-unique-values %
                                         (U/get-options-for currentCfg %)
                                         :option) all-subcommands))
      (doall (map #(assert-unique-values %
                                         (U/get-options-for currentCfg %)
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
  {:retval return-code
   :status type
   :help   stdout
   :subcmd subcmd
   :stderr (U/asStrVec stderr)})

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

   * return an integer (to specify exit code)
   * return nil
   * throw a Throwable object


   If there is a shutdown hook defined, we also add the shutdown hook
   before the command is run. If there is a shutdown hook,
   it is called anyway when the JVM terminates - if you only want this
   called on early shutdowns, it's up to you to keep some state
   in a shared atom and decide whether to do something or not.

  "
  [subcommand-def options]

  (try-catch-all
   (let [_  (P/add-shutdown-hook (:on-shutdown subcommand-def))
         rv ((:runs subcommand-def)  options)]
     (cond
       (nil? rv)    (->RV 0 :OK nil nil nil)
       (int? rv)   (if (zero? rv)
                     (->RV 0 :OK nil nil nil)
                     (->RV rv :ERR nil nil nil))

       :else        (->RV 0 :OK nil nil nil)))

   (fn [t]
     (->RV -1 :EXCEPTION nil nil
           (str "JVM Exception: "
                (with-out-str (println t)))))))

(def setup-defaults
  {:app {:global-help H/generate-global-help
         :subcmd-help H/generate-subcmd-help}})

(defn run-cmd*
  "
  Executes our code.

  It will try and parse the arguments via `clojure.tools.cli` and detect our subcommand.

  If no subcommand was found, it will print the error reminder.

  On exceptions, it will raise an exception message.

  "

  [setup args]
  (let [{:keys [subcommand subcommand-def parse-errors error-text commandline]}
        (parse-cmds args setup)]
    ;; maybe there was an error parsing
    (condp = parse-errors
      :ERR-CFG (->RV -1 :ERR-CFG nil nil  "Error in CLI-matic configuration.")
      :ERR-NO-SUBCMD (->RV -1 :ERR-NO-SUBCMD :HELP-GLOBAL nil "No sub-command specified.")
      :ERR-UNKNOWN-SUBCMD (->RV -1 :ERR-UNKNOWN-SUBCMD :HELP-GLOBAL nil error-text)
      :HELP-GLOBAL (->RV 0 :OK :HELP-GLOBAL nil nil)
      :ERR-PARMS-GLOBAL (->RV -1 :ERR-PARMS-GLOBAL :HELP-GLOBAL nil
                              (str "Global option error: " error-text))
      :HELP-SUBCMD (->RV 0 :OK :HELP-SUBCMD subcommand nil)
      :ERR-PARMS-SUBCMD (->RV -1 :ERR-PARMS-SUBCMD :HELP-SUBCMD subcommand
                              (str "Option error: " error-text))
      :NONE (invoke-subcmd subcommand-def commandline))))

(defn run-cmd
  "This is the actual function that is executed.
  It wraps [[run-cmd*]] and then does the printing
  of any errors, of help pages and  `System.exit`.

  As it invokes `System.exit` you cannot use it from a REPL.
  "
  [args supplied-setup]
  (let [setup (U/deep-merge setup-defaults supplied-setup)
        {:keys [help stderr subcmd retval]}
        (run-cmd* setup (if (nil? args) [] args))]
    (if (not (empty? stderr))
      (println
       (U/asString ["** ERROR: **" stderr "" ""])))
    (cond
      (= :HELP-GLOBAL help)
      (println (U/asString ((get-in setup [:app :global-help]) setup)))
      (= :HELP-SUBCMD help)
      (println (U/asString ((get-in setup [:app :subcmd-help]) setup subcmd))))
    (P/exit-script retval)))

(OPT/orchestra-instrument)
