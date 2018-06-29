(ns cli-matic.platform)

;; In this namespace, we store platform-specific
;; functions.
;; In this file we have them for the JVM, so we
;; can have a different one for JS.

;; In this NS, we avoid using Spec / Orchestra.

(defn read-env
  "Reads an environment variable.
  If undefined, returns nil."
  [var]
  (System/getenv var))

(defn exit-script
  "Terminates execution with a return value."
  [retval]
  (System/exit retval))
