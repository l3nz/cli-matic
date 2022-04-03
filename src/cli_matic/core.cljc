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
            [cli-matic.utils-v2 :as U2]
            [cli-matic.optionals :as OPT]
            [expound.alpha :as expound]))

(defn mkError
  "Builds an error condition."
  [config subcommand error text]
  (let [subcmd-name (U2/canonical-path-to-string subcommand)]

    {:subcommand     subcmd-name
     :subcommand-path subcommand
     :subcommand-def (if (or (= error :ERR-UNKNOWN-SUBCMD)
                             (= error :ERR-NO-SUBCMD)
                             (= error :ERR-PARMS-GLOBAL)
                             (= error :HELP-GLOBAL))
                       nil
                       (U2/get-subcommand config subcommand))
     :commandline    {}
     :parse-errors   error
     :error-text     (U/asString text)}))

(s/fdef
  mkError
  :args (s/cat :config (s/or :cfg_v1 ::S/climatic-cfg-classic
                             :cfg_v2 ::S/climatic-cfg)
               :subcmd (s/or :s ::S/subcommand-path)
               :err    (s/or :err ::S/climatic-errors
                             :help ::S/help)
               :text   any?)
  :ret ::S/lineParseResult)

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

  [opts argv in-order? fn-env]
  (let [cli-cmd-options (U/cm-opts->cli-opts opts)
        env-options (filter :env opts)
        argv+ (if (seq env-options)
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
  [opts argv fn-env]
  (let [parsed-cmd-opts (parse-cmds-with-defaults opts argv false fn-env)
        cmd-args (:arguments parsed-cmd-opts)

        ;; capture positional parms
        ;; if they exist, we inject them at the end of the command line
        ;; and parse again
        positional-parms (U/capture-positional-parms opts cmd-args)]

    (cond
      ; do we have any positional parms?
      ; parse a new line where they are not positional anymore
      (pos? (count positional-parms))
      (let [addl-args (mk-fake-args positional-parms)
            newcmdline (into argv addl-args)]
        (parse-cmds-with-defaults opts newcmdline false P/read-env))

      :else
      parsed-cmd-opts)))

(s/fdef
  parse-cmds-with-positions
  :args (s/cat :opts ::S/opts
               :argv (s/coll-of string?)
               :fn-env any?)
  :ret  ::S/parsedCliOpts)

(defn check-one-spec
  "Checks one spec.

  - If spec passes, returns nil; if not, returns the failure.
  - If there is an error raised, creates a fake spec result.
  - If spec is nil, we consider it a pass.
  - if the value is nil, we consider it missing and don't need to check.

  `explain-data` return nil if everything okay.

  `expound-string` returns 'Success!\n' if all goes well.


  "
  [name type spec value]

  (cond
    ; no spec - all went well (at least, I have nothing to complain about)
    (nil? spec)
    nil

    ; no value - must be an optional, so I should not validate it
    (nil? value)
    nil

    ; let's check this spec
    :else
    (try-catch-all
     (let [expound-result (expound/expound-str spec value)]
       (if (not= expound-result "Success!\n")
         (str "Spec failure for " type " '" name "'\n" expound-result)
         nil))

     (fn [t]
       (str "Spec failure for " type " '" name "': with value '" value "' got " t)))))

(s/fdef
  check-one-spec
  :args (s/cat :name string?
               :type string?
               :spec (s/or :nome nil? :a-spec ::S/spec)
               :value ::S/anything)
  :ret  (s/or :nil nil?
              :str string?))

(defn check-specs-on-parameters
  "Given a set of option (so, global options, or a subcommand's options)
  and the fully parsed results, we assert that any defined specs pass.

  This we do only if the parameter is not nil, that is, is present or
  has a default value.

  If the parameter should be present but it's not, it's not a spec issue
  but a ':default :present' issue.
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

(defn parse-command-line
  "
  This is where the magic happens.

  Whereas in version 1 we had one 'global' level and one
  sub-command, now we have many. From the point of view of
  sub-commands, all intermediate levels but actual executable
  sub-commands are global levels; each of them may capture
  its own variables and have them validated via specs.

  So we first try walking to the current level through the
  configuration, capture and verify what is needed at this level
  and then terminate only if we are on a leaf (executable subcommand)
  or there is any error.

  Spec checking:

  - If parsing goes well, parameter specs are checked on each parameter
  - If all of them go well, general spec is checked on the current result
    of all parsing; that is, including results form all earlier
    general settings


  TODO:
   - candidates
   - help

  "
  [argv config]

  (loop [reqd-path  [(:command config)]
         unparsed-argv argv
         current-parsed-opts {}]

    (cond
      ; Maybe I cannot walk all the way to the required command
      (not (U2/can-walk? config reqd-path))
      (let [failed-path-str (U2/canonical-path-to-string reqd-path)]
        (mkError config reqd-path :ERR-UNKNOWN-SUBCMD
                 (str "Unknown sub-command: '" failed-path-str "'.")))

      :else
      (let [curr-xpath (U2/walk config reqd-path)
            curr-subcmd (last curr-xpath)
            curr-path (U2/as-canonical-path curr-xpath)
            curr-path-str (U2/canonical-path-to-string curr-path)
            global-node?     (not (U2/is-runnable? curr-xpath))
            options       (:opts curr-subcmd)
            general-spec  (:spec curr-subcmd)

            parsed-opts (if global-node?
                          ; global: no positions
                          (parse-cmds-with-defaults options unparsed-argv true P/read-env)
                          ; leaf: use positions
                          (parse-cmds-with-positions options unparsed-argv P/read-env))

            ; do we miss any mandatory option?
            missing-opts (errors-for-missing-mandatory-args options parsed-opts {})
            ; destructure results
            {parse-errs :errors parsed-opts :options parse-leftover-args :arguments} parsed-opts
            ; if global, the subcommand to process next....
            next-subcommand (first parse-leftover-args)
            ; ...and its parameters
            next-subcommand-argv (vec (rest parse-leftover-args))
            ; the new path that will be run
            next-path  (conj curr-path next-subcommand)
            ; the total set of parsed options: the ones we got now plus any previous ones
            total-parsed-opts (merge current-parsed-opts parsed-opts)

            ; the (first) failing spec for a parameter we just extracted
            failing-param-spec (when (empty? parse-errs)
                                 (first
                                  (check-specs-on-parameters options parsed-opts
                                                             (if global-node?
                                                               "global option"
                                                               "option"))))

            ; whether the general spec for this level fails
            failing-general-spec (when (empty? parse-errs)
                                   (check-one-spec curr-path-str
                                                   "subcommand"
                                                   general-spec
                                                   total-parsed-opts))]

        (cond
            ; any parse errors?
          (some? parse-errs)
          (mkError config curr-path
                   (if global-node? :ERR-PARMS-GLOBAL :ERR-PARMS-SUBCMD)
                   parse-errs)

            ; did we ask for help?
          (some? (:_help_trigger parsed-opts))
          (mkError config curr-path
                   (if global-node? :HELP-GLOBAL :HELP-SUBCMD) nil)

            ; looks like there is no sub-command specified
          (and global-node? (nil? next-subcommand))
          (mkError config curr-path :ERR-NO-SUBCMD "")

            ; missing required parms?
            ; we raise this only if there are no errors in cmd parms
          (and (empty? parse-errs) (pos? (count missing-opts)))
          (mkError config curr-path
                   (if global-node? :ERR-PARMS-GLOBAL :ERR-PARMS-SUBCMD)
                   missing-opts)

            ; failed spec: parameter validation
          (some? failing-param-spec)
          (mkError config curr-path
                   (if global-node? :ERR-PARMS-GLOBAL :ERR-PARMS-SUBCMD)
                   failing-param-spec)

            ; failed spec: the general level
          (some? failing-general-spec)
          (mkError config curr-path
                   (if global-node? :ERR-PARMS-GLOBAL :ERR-PARMS-SUBCMD)
                   failing-general-spec)

            ; If this is a global option and it went well so far,
            ; let's loop back and explore what's in store below
          global-node?
          (recur next-path
                 next-subcommand-argv
                 total-parsed-opts)

            ; no errors and we are a leaf node? really? let's finish this charade.
            ; teach the kids some geometry and theology.
          (nil? parse-errs)
          (let [all-parsed-opts (-> total-parsed-opts
                                    (into {:_arguments parse-leftover-args}))]
            {:subcommand      curr-path-str
             :subcommand-path curr-path
             :subcommand-def  curr-subcmd
             :commandline     all-parsed-opts
             :parse-errors    :NONE
             :error-text      ""})

            ; something went bad.
          :else
          (mkError config reqd-path :ERR-PARMS-SUBCMD parse-errs))))))

(s/fdef
  parse-command-line
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
        dupes (filterv (fn [[_ v]]  (> v 1)) (frequencies allOptions))]
    (cond
      (seq dupes)
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

(defn assert-cfg-sanity [_] :IMPLEMENT_ME_PLZ)


;(defn assert-cfg-sanity
;  "Checks configuration and throws if anything wrong.
;
;  1. are :option values unique?
;  2. are :short values unique?
;  3. do we have any positional arguments in global config?
;
;  First we make a list of `nil` plus all subcmds.
;
;  "
;  [currentCfg]
;
;    ;; checks positional parameters
;
;  (let [global-positional-parms (U/list-positional-parms currentCfg nil)]
;
;    (when (pos? (count global-positional-parms))
;      (throw (ex-info
;              (str "Positional parameters not allowed in global options. " global-positional-parms)
;              {}))));; checks subcommands
;  (let [all-subcommands (into [nil]
;                              (U/all-subcommands currentCfg))]
;    (doall (map #(assert-unique-values %
;                                       (U/OLD__get-options-for currentCfg %)
;                                       :option) all-subcommands))
;    (doall (map #(assert-unique-values %
;                                       (U/OLD__get-options-for currentCfg %)
;                                       :short) all-subcommands)))
;  ;; just say nil
;  nil)
;
;(s/fdef assert-cfg-sanity
;  :args (s/cat :opts ::S/climatic-cfg))

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
   :subcmd (if (nil? subcmd) [] subcmd)   ; the path
   :stderr (U/asStrVec stderr)})

(s/fdef
  ->RV
  :args (s/cat :rv int? :status some? :help any? :subcmd any? :stderr any?)
  :ret ::S/RV)

;
;
;
;


(defn getReturnValue
  "Evaluates the result of the CLI-matic subcommand.
  A result can be nil, or int?, or a  deferred value,
  in which case we wait in a platform-specific way.
  "
  [rv]

  (cond
    ; it's a number - correct status
    (int? rv) rv
    ; no value - say all went well
    (nil? rv) 0
    ; is a JVM promise? deref and repeat
    (P/isDeferredValue? rv) (-> rv
                                P/waitForDeferredValue
                                getReturnValue)
    ; anything else, it's zero for me.
    :else 0))

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
         rv* ((:runs subcommand-def)  options)
         rv (getReturnValue rv*)]

     (cond
       (int? rv)   (if (zero? rv)
                     (->RV 0 :OK nil nil nil)
                     (->RV rv :EXCEPTION nil nil nil))

       :else        (->RV 0 :OK nil nil nil)))

   (fn [t]
     (let [[msg exitcode] (U/exception-info t)]
       (->RV exitcode :EXCEPTION nil nil msg)))))

(defn run-cmd*
  "
  Executes our code.

  It will try and parse the arguments via `clojure.tools.cli` and detect our subcommand.

  If no subcommand was found, it will print the error reminder.

  On exceptions, it will raise an exception message.

  "
  [setup args]
  (let [args-not-null (if (nil? args) [] args)

        {:keys [subcommand-path subcommand-def parse-errors error-text commandline]}
        (parse-command-line args-not-null setup)]

    ;; maybe there was an error parsing
    (condp = parse-errors
      :ERR-CFG (->RV -1 :ERR-CFG nil nil  "Error in CLI-matic configuration.")
      :ERR-NO-SUBCMD (->RV -1 :ERR-NO-SUBCMD :HELP-GLOBAL subcommand-path "No sub-command specified.")
      :ERR-UNKNOWN-SUBCMD (->RV -1 :ERR-UNKNOWN-SUBCMD :HELP-GLOBAL  (butlast subcommand-path)  ; (-> subcommand-path butlast vec)
                                error-text)
      :HELP-GLOBAL (->RV 0 :OK :HELP-GLOBAL subcommand-path nil)
      :ERR-PARMS-GLOBAL (->RV -1 :ERR-PARMS-GLOBAL :HELP-GLOBAL subcommand-path
                              (str "Global option error: " error-text))
      :HELP-SUBCMD (->RV 0 :OK :HELP-SUBCMD subcommand-path nil)
      :ERR-PARMS-SUBCMD (->RV -1 :ERR-PARMS-SUBCMD :HELP-SUBCMD subcommand-path
                              (str "Option error: " error-text))
      :NONE (invoke-subcmd subcommand-def commandline))))

(defn run-cmd
  "This is the actual function that is executed.
  It wraps [[run-cmd*]] and then does the printing
  of any errors, of help pages and  `System.exit`.

  As it invokes `System.exit` you cannot use it from
  a REPL (well, you technically can, but...).
  "
  [args supplied-config]
  (let [config (U2/cfg-v2 supplied-config)
        {:keys [help stderr subcmd retval]} (run-cmd* config args)]

    ; prints the error message, if present
    (when (seq stderr)
      (U/printErr ["** ERROR: **" stderr "" ""]))

    ; prints help
    (cond
      (= :HELP-GLOBAL help)
      (let [helpFn (H/getGlobalHelperFn config subcmd)]
        (U/printErr (helpFn config subcmd)))

      (= :HELP-SUBCMD help)
      (let [helpFn (H/getSubcommandHelperFn config subcmd)]
        (U/printErr (helpFn config subcmd))))

    ; bye bye baby
    (P/exit-script retval)))

(OPT/orchestra-instrument)
