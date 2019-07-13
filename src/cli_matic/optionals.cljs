(ns cli-matic.optionals
  "### This namespace contains optional dependencies for CLJS.

  JSON is always available in CLJS.

  ")

;; CHESHIRE


(defn json-decode
  "
  We should use Transit if present, but this should be enough to get us started.

  https://cljs.github.io/api/cljs.core/js-GTclj
  "
  [json]
  (let [a (.parse js/JSON json)]
    (js->clj a)))

;; YAML


(defn  yaml-decode
  ""
  [& _]
  (throw (ex-info "No YAML decoding in CLJS." {})))

;; ORCHESTRA


(defn orchestra-instrument
  ""
  []
  nil)

