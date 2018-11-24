(ns cli-matic.platform
  "
  ## Platform-specific functions for Node.

  At the moment it's all empty.

  BTW, in this NS, we avoid using Spec / Orchestra.

  ")

(defn read-env
  "Reads an environment variable.
  If undefined, returns nil."
  [var]
  nil)

(defn exit-script
  "Terminates execution with a return value."
  [retval]
  nil)

(defn add-shutdown-hook
  "Add a shutdown hook.

  Does not work (?) on CLJS.
  "
  [fnToCallOnShutdown]

  nil)

(defn slurp-file
  "
  No slurping in Node-land.

  See https://github.com/pkpkpk/cljs-node-io

  "
  [f]
  nil
  )


;
; Conversions
;

(defn parseInt
  "Converts a string to an integer. "
  [s]
  (js/parseInt s))

(defn parseFloat
  "Converts a string to a float."
  [s]
  (js/parseFloat s))

(defn asDate
  "Converts a string in format yyyy-mm-dd to a
  Date object; if conversion
  fails, returns nil."
  [s]
  nil)

(defn parseEdn
  "
      See https://stackoverflow.com/questions/44661385/how-do-i-read-an-edn-file-from-clojurescript-running-on-nodejs
      "
  [edn-in]
  nil)


;
; Exceptions
;

(defmacro try-catch-all
  "
  See the .clj docs.
  "
  [f onErr]
  `(try (~@f)
        (catch :default t#
          ((~@onErr) t#))))
