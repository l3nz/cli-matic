(ns cli-matic.platform
  "
  ## Platform-specific functions for the JVM.

  If running on ClojureScript, we can have a different file for JS.

  BTW, in this NS, we avoid using Spec / Orchestra.

  "
  )


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

  (if (ifn? fnToCallOnShutdown)
    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. fnToCallOnShutdown))))
