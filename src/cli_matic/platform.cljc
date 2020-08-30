(ns cli-matic.platform
  #?(:clj  "
           ## Platform-specific functions for the JVM.

           If running on ClojureScript, we can have a different file for JS.

           BTW, in this NS, we avoid using Spec / Orchestra.

           **DO NOT** define macros in this namespace - see [[cli-matic.platform-macros]]

           "
     :cljs "
           ## Platform-specific functions for ClojureScript.

           At the moment, we only support Planck.

           BTW, in this NS, we avoid using Spec / Orchestra.

           ")
  #?(:clj  (:require [cljc.java-time.zone-id :as zone-id]
                     [cljc.java-time.local-date :as local-date]
                     [cljc.java-time.local-date-time :as local-date-time]
                     [cljc.java-time.zoned-date-time :as zoned-date-time]
                     [clojure.edn :as edn]
                     [cli-matic.optionals :as OPT])
     :cljs (:require [planck.core :as plk :refer [slurp]]
                     [planck.environ :refer [env]]
                     [cljs.reader :as edn]
                     [clojure.string :as str]))
  #?(:bb  (:import)
     :clj (:import
           (clojure.lang IDeref IPending IFn))))

(defn read-env
  "Reads an environment variable.
  If undefined, returns nil."
  [var]
  #?(:clj  (System/getenv var)
     :cljs (let [kw (keyword (str/lower-case var))]
             (get env kw nil))))

(defn exit-script
  "Terminates execution with a return value."
  [retval]
  #?(:clj  (System/exit retval)
     :cljs (plk/exit retval)))

(defn add-shutdown-hook
  #?(:clj  "Add a shutdown hook. If `nil`, simply ignores it.

            The shutdown hook is run in a new thread.

            "
     :cljs "Add a shutdown hook.

           Does not work (?) on CLJS and we will throw an exception.

           It might be conceivable that in JS-land, we save this locally in this namespace
           and then call it on `exit-script`.

           ")
  [fnToCallOnShutdown]
  ;TODO remove :bb expression once IFn is in Babashka
  #?(:bb   (when (ifn? fnToCallOnShutdown)
             (.addShutdownHook
              (Runtime/getRuntime)
              (Thread. fnToCallOnShutdown)))
     :clj  (when (ifn? fnToCallOnShutdown)
             (.addShutdownHook
              (Runtime/getRuntime)
              (Thread. ^IFn fnToCallOnShutdown)))
     :cljs (when (ifn? fnToCallOnShutdown)
             (throw (ex-info "Shutdown hooks not supported outside the JVM" {})))))

(defn slurp-file
  #?(:clj  "No slurping in JavaScript. So we have to move this to
           platform.

           "
     :cljs "
           Luckily, Planck implements slurp for us.

           No slurping in Node-land.

           See https://github.com/pkpkpk/cljs-node-io

           ")
  [f]
  (slurp f))

;
; Conversions
;

(defn parseInt
  "Converts a string to an integer. "
  [s]
  #?(:clj  (Integer/parseInt s)
     :cljs (js/parseInt s)))

(defn parseFloat
  "Converts a string to a float."
  [s]
  #?(:clj  (Float/parseFloat s)
     :cljs (js/parseFloat s)))

(defn asDate
  "Converts a string in format yyyy-mm-dd to a
  Date object; if conversion
  fails, returns nil."
  [s]
  #?(:clj     (try
                (-> (local-date/parse s)
                    (local-date/at-start-of-day)
                    (local-date-time/at-zone (zone-id/system-default))
                    (zoned-date-time/to-instant)
                    (java.util.Date/from))
                (catch Throwable _
                  nil))
     :default (throw (ex-info "Dates not supported" {:date s}))))

(defn parseEdn
  "Decodes EDN through clojure.edn."
  [edn-in]
  (edn/read-string edn-in))

(defn isDeferredValue?
  "Is this a deferred value for this platform?"
  [v]
  ;TODO remove :bb expression once IPending is in babashka
  #?(:bb      (cond
                (future? v) true
                (OPT/is-core-async-channel? v) true
                :else false)
     :clj     (cond
                (instance? IPending v) true
                (future? v) true
                (OPT/is-core-async-channel? v) true
                :else false)
     :default false))

(defn waitForDeferredValue
  "Given that value is a deferred  value for this platform,
   block termination until it's realized.

   On the JVM, we support:

   - promises
   - futures
   - core.async channels (if they exist)

  "
  [v]
  ;TODO remove :bb expression once IDeref is in babashka
  #?(:bb      (cond
                (OPT/is-core-async-channel? v) (OPT/read-value-from-core-async-channel v)
                :else (throw (ex-info
                              (str "Value is not deferred " v)
                              {})))
     :clj     (cond
                (instance? IDeref v) (deref v)
                (OPT/is-core-async-channel? v) (OPT/read-value-from-core-async-channel v)
                :else (throw (ex-info
                              (str "Value is not deferred " v)
                              {})))
     :default (throw (ex-info
                      (str "Value is not deferred " v)
                      {}))))

(defn printError
  "On ClojureScript, STDERR is not *err* but it's platform dependent.
  On JVM, standard approach."
  [o]
  #?(:clj  (binding [*out* *err*]
             (println o))
     :cljs (binding [*print-fn* *print-err-fn*]
             (println o))))

