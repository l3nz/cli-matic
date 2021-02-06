(ns cli-matic.platform
  "
  ## Platform-specific functions for the JVM.

  If running on ClojureScript, we can have a different file for JS.

  BTW, in this NS, we avoid using Spec / Orchestra.

  **DO NOT** define macros in this namespace - see [[cli-matic.platform-macros]]

  "
  (:require [clojure.edn :as edn]
            [cli-matic.optionals :as OPT])
  (:import (clojure.lang IPending)
           (java.text SimpleDateFormat)))

(defn read-env
  "Reads an environment variable.
  If undefined, returns nil."
  [var]
  (System/getenv var))

(defn exit-script
  "Terminates execution with a return value."
  [retval]
  (System/exit retval))

(defn add-shutdown-hook
  "Add a shutdown hook. If `nil`, simply ignores it.

  The shutdown hook is run in a new thread.

  "
  [fnToCallOnShutdown]

  (when (ifn? fnToCallOnShutdown)
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. fnToCallOnShutdown))))

(defn slurp-file
  "No slurping in JavaScript. So we have to move this to
  platform."
  [f]
  (slurp f))

(defonce ^:dynamic *stdin*
  clojure.core/*in*)

;
; Conversions
;

(defn parseInt
  "Converts a string to an integer. "
  [s]
  (Integer/parseInt s))

(defn parseFloat
  "Converts a string to a float."
  [s]
  (Float/parseFloat s))

(defn asDate
  "Converts a string in format yyyy-mm-dd to a
  Date object; if conversion
  fails, returns nil."
  [s]
  (try
    (.parse
     (SimpleDateFormat. "yyyy-MM-dd") s)
    (catch Throwable _
      nil)))

(defn parseEdn
  " Decodes EDN through clojure.edn.      "
  [edn-in]
  (edn/read-string edn-in))

(defn- isJvmPromise?
  "Checks whether the value is a JVM promise."
  [x]
  (instance? IPending x))

(defn isDeferredValue?
  "Is this a deferred value for this platform?"
  [v]
  (cond
    (isJvmPromise? v) true
    (future? v) true
    (OPT/is-core-async-channel? v) true
    :else false))

(defn waitForDeferredValue
  "Given that value is a deferred  value for this platform,
   block termination until it's realized.

   On the JVM, we support:

   - promises
   - futures
   - core.async channels (if they exist)

  "

  [v]
  (cond
    (isJvmPromise? v) (deref v)
    (future? v) (deref  v)
    (OPT/is-core-async-channel? v) (OPT/read-value-from-core-async-channel v)
    :else (throw (ex-info
                  (str "Value is not deferred " v)
                  {}))))

(defn printError
  "On ClojureScript, STDERR is not *err* but it's platform dependent.
  On JVM, standard approach."
  [o]
  (binding [*out* *err*]
    (println o)))
