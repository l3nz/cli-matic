(ns cli-matic.optionals
  #?(:clj  "### This namespace contains optional dependencies for CLJ.

           CLI-matic is supposed to work whether they are present or not.

           * JSON (Cheshire)
           * YAML (io.forward/yaml)
           * Orchestra

           Detection is taken from `core.clj` in https://github.com/dakrone/clj-http

           "
     :cljs "### This namespace contains optional dependencies for CLJS.

           JSON is always available in CLJS.

           ")
  #?(:bb  (:require [clojure.string :as str]
                    [spartan.spec :as s])
     :clj (:require [clojure.string :as str]
                    [clojure.spec.alpha :as s])))

(defn- provided? [& args]
  (try
    (apply require args)
    true
    (catch Throwable _ false)))

;; CORE-ASYNC

(def with-core-async? (provided? 'clojure.core.async
                                 'clojure.core.async.impl.protocols))

(defn ^:dynamic read-value-from-core-async-channel
  "Reads a value from a core.async channel, blocking."
  [& args]
  {:pre [with-core-async?]}
  (apply (resolve 'clojure.core.async/<!!)
         args))

(defn is-core-async-channel?
  "Is this entity a core.async channel?"
  [c]
  (and with-core-async?
       (some? c)
       (let [var (resolve 'clojure.core.async.impl.protocols/ReadPort)]
         (satisfies? #?(:bb var :cljs var :clj @var) c))))

#?(:clj (do
          ;; CHESHIRE

          (def with-cheshire? (provided? 'cheshire.core))

          (defn ^:dynamic json-decode-cheshire
            "Resolve and apply Cheshire's json decoding dynamically."
            [& args]
            {:pre [with-cheshire?]}
            (apply (resolve 'cheshire.core/decode) args))

          ;; YAML

          (def with-yaml? (provided? 'yaml.core))

          ;; ORCHESTRA

          (def with-orchestra? (provided? 'orchestra.spec.test))

          ;; EXPOUND

          (def with-expound? (provided? 'expound.alpha))))

(defn ^:dynamic json-decode
  #?(:clj  "Decodes a JSON string, without keywordizing."
     :cljs "
           We should use Transit if present, but this should be enough to get us started.

           https://cljs.github.io/api/cljs.core/js-GTclj
           ")
  [json]
  #?(:clj  (json-decode-cheshire json)
     :cljs (let [a (.parse js/JSON json)]
             (js->clj a))))

(defn ^:dynamic yaml-decode
  #?(:clj  "Resolve and apply io.forward/yaml's yaml decoding dynamically."
     :cljs "")
  [& args]
  #?(:clj {:pre [with-yaml?]})
  ;FIXME yaml with built-in https://github.com/clj-commons/clj-yaml
  #?(:bb   nil
     :clj  ((resolve 'yaml.core/parse-string)
            (if (string? args) args (str/join args))
            :keywords identity
            :constructor (resolve 'yaml.reader/passthrough-constructor))
     :cljs (throw (ex-info "No YAML decoding in CLJS." {}))))

(def explain-success (str "Success!" \newline))

(defn- spec-explain-str [spec value]
  (if (s/valid? spec value)
    explain-success
    #?(:bb      (with-out-str
                  (s/explain spec value))
       :default (s/explain-str spec value))))

;TODO not sure if explain-str should be in optionals or platform ns
(defn ^:dynamic explain-str
  [spec value]
  #?(:clj     (if with-expound?
                ((resolve 'expound.alpha/explain-str) spec value)
                (spec-explain-str spec value))
     :default (spec-explain-str spec value)))

(defn ^:dynamic orchestra-instrument
  #?(:clj  "If Orchestra is present, runs instrumentation.
           If absent, do nothing.

           While we are at it, we set up Expound to
           print meaningful errors.

           Expound is a mandatory dependency,  so
           we take for granted it's there.


           The `*explain-out*` binding will NOT be there in a compiled
           uberjar, unless we call `with-bindings`,
           so we just go check. It will succeed from the REPL.

           Ref: https://github.com/clojure/clojure/blob/clojure-1.9.0/src/clj/clojure/main.clj#L85

           "
     :cljs "")
  []
  #?(:bb  nil
     :clj (when (and with-orchestra? with-expound?)
            ;; orchestra.spec.test/instrument
            ((resolve 'orchestra.spec.test/instrument))

            ;; as we have expound, we'd better use it.
            (try
              (set! s/*explain-out* (resolve 'expound.alpha/printer))
              (catch Exception _ nil)))))
