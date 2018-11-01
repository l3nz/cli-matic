(ns cli-matic.optionals
  (:require [clojure.string :as str]))

;; This namespace contains optional libraries.


;; CHESHIRE

;; ---------------
;; Cheshire is an optional dependency, so we check for it at compile time.
;; Taken from core.clj in https://github.com/dakrone/clj-http
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

;; ---------------
;; YAML is an optional dependency, so we check for it at compile time.
;; Taken from core.clj in https://github.com/dakrone/clj-http
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
