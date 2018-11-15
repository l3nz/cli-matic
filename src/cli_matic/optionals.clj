(ns cli-matic.optionals
  "### This namespace contains optional dependencies.

  CLI-matic is supposed to work whether they are present or not.

  * JSON (Cheshire)
  * YAML (io.forward/yaml)
  * Orchestra

  Detection is taken from `core.clj` in https://github.com/dakrone/clj-http

  "
  (:require [clojure.string :as str]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]))

;; CHESHIRE

(def with-cheshire?
  (try
    (require 'cheshire.core)
    true
    (catch Throwable _ false)))

(defn ^:dynamic json-decode-cheshire
  "Resolve and apply Cheshire's json decoding dynamically."
  [& args]
  {:pre [with-cheshire?]}
  (apply (ns-resolve (symbol "cheshire.core") (symbol "decode")) args))

;; YAML

(def with-yaml?
  (try
    (require 'yaml.core)
    true
    (catch Throwable _ false)))

(defn ^:dynamic yaml-decode
  "Resolve and apply io.forward/yaml's yaml decoding dynamically."
  [& args]
  {:pre [with-yaml?]}
  ((ns-resolve (symbol "yaml.core") (symbol "parse-string"))
   (if (string? args) args (str/join args))
   :keywords identity
   :constructor (ns-resolve (symbol "yaml.reader") (symbol "passthrough-constructor"))))

;; ORCHESTRA

(def with-orchestra?
  (try
    (require 'orchestra.spec.test)
    true
    (catch Throwable _ false)))

(defn ^:dynamic orchestra-instrument
  "If Orchestra is present, runs instrumentation.
  If absent, do nothing.

  While we are at it, we set up Expound to
  print meaningful errors.

  Expound is a mandatory dependency,  so
  we take for granted it's there.
  "
  []
  (if with-orchestra?
    (do
      ;; orchestra.spec.test/instrument
      ((ns-resolve (symbol "orchestra.spec.test")
                   (symbol "instrument")))

      ;; as we have expound, we'd better use it.
      (set! s/*explain-out* expound/printer))))

