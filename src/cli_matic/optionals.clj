(ns cli-matic.optionals
  "### This namespace contains optional dependencies.

  CLI-matic is supposed to work whether they are present or not.

  * JSON (Cheshire)
  * YAML (io.forward/yaml)

  Detection is taken from `core.clj` in https://github.com/dakrone/clj-http

  "
  (:require [clojure.string :as str]))

;; CHESHIRE

(def with-cheshire?
  (try
    (require 'cheshire.core)
    true
    (catch Throwable _ false)))

(defn ^:dynamic json-decode-cheshire
  "Resolve and apply cheshire's json decoding dynamically."
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
