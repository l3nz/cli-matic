(ns cli-matic.platform
  "
  ## Platform-specific functions for ClojureScript.

  At the moment, we only support Planck.

  BTW, in this NS, we avoid using Spec / Orchestra.

  "
  (:require [planck.core :as plk]
            [planck.environ :as plkenv]
            [cljs.reader :as csrdr]
            [clojure.string :as str]))

(defn read-env
  "Reads an environment variable.
  If undefined, returns nil."
  [var]
  (let [kw (keyword (str/lower-case var))]
    (get plkenv/env kw nil)))

(defn exit-script
  "Terminates execution with a return value.

  Please note that in Planck, return codes seem to be 8-bit unsigned ints.
  "
  [retval]
  (plk/exit retval))

(defn add-shutdown-hook
  "Add a shutdown hook.

  Does not work (?) on CLJS and we will throw an exception.

  It might be conceivable that in JS-land, we save this locally in this namespace
  and then call it on `exit-script`.

  "
  [fnToCallOnShutdown]

  (when (ifn? fnToCallOnShutdown)
    (throw (ex-info "Shutdown hooks not supported outside the JVM" {}))))

(defn slurp-file
  "
  Luckily, Planck implements slurp for us.

  No slurping in Node-land.

  See https://github.com/pkpkpk/cljs-node-io

  "
  [f]
  (plk/slurp f))

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
  (throw (ex-info "Dates not supported in CLJS." {:date s})))

(defn parseEdn
  "
    This is actually a piece of ClojureScript, though it lives in a different NS.

    See https://cljs.github.io/api/cljs.reader/read-string
  "
  [edn-in]
  (csrdr/read-string edn-in))

