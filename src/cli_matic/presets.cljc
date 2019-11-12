(ns cli-matic.presets
  "
  ### Known presets


  "
  (:require [clojure.string :as str]
            [cli-matic.optionals :as OPT]
            [cli-matic.platform :as P]
            [clojure.spec.alpha :as s]
            [cli-matic.specs :as S]
            [cli-matic.utils-candidates :as BU]))

;; Known presets

(defn asSingleString
  "Turns a filename into a single string.

  If argument is a String, it tries to resolve it first as a URI, then
  as a local file name.  URIs with a 'file' protocol are converted to
  local file names.

  "
  [filename]
  (cond
    (nil? filename)   ""
    (empty? filename) ""
    :else (P/slurp-file filename)))

(defn asLinesString
  "Reads a text file and returns it as a collection of lines."
  [filename]
  (str/split-lines (asSingleString filename)))

(defn asDecodedEdnValue
  "Decodes the value as an EDN structure."
  [s]
  ;;(edn/read-string (if (string? s) s (str/join s)))
  (P/parseEdn s))

(defn asDecodedEdnFile
  "Decodes the contents of a file as a JSON object."
  [filename]
  (P/parseEdn (asSingleString filename)))

(defn asDecodedJsonValue
  "Decodes the value as a JSON object."
  [s]
  (OPT/json-decode s))

(defn asDecodedJsonFile
  "Decodes the contents of a file as a JSON object."
  [filename]
  (OPT/json-decode (asSingleString filename)))

(defn asDecodedYamlValue
  "Decodes the value as a YAML object."
  [s]
  (OPT/yaml-decode s))

(defn asDecodedYamlFile
  "Decodes the contents of a file as a JSON object."
  [filename]
  (OPT/yaml-decode (asSingleString filename)))

(defn- replace-double-colon
  [s]
  (if (str/starts-with? s "::")
    (str/replace s "::" ":user/")
    s))

(defn asKeyword
  [s]
  (-> s replace-double-colon P/parseEdn keyword))


; =================================================================
; Stuff for sets
; =================================================================


(defn set-help-values
  "Given a set, return allowed options as string"
  [st]
  (let [opts (map name st)]
    (str/join  "|" (sort opts))))

(s/fdef
  set-help-values
  :args (s/cat :set ::S/set-of-vals))

(defn set-normalized-entry
  "A normalized set entry is a lowercase string
  without trailing `:`."
  [v]
  (let [ne (str/lower-case (name v))]
    (cond
      (str/starts-with? ne ":")  (subs ne 1)
      :else ne)))

(defn set-normalize-map
  "Builds a normalized map that
  has as key the normalized value, and value the
  original one."
  [st]
  (let [vals (map (fn [k]
                    [(set-normalized-entry k) k])
                  st)]
    (into {} vals)))

(defn set-find-value
  "Finds a string value in a set of options.
  To do this, we first create a map of
  {normalized original}
  Returns valFound or nil.
  "
  [st v]
  (let [mOpts (set-normalize-map st)
        opt (set-normalized-entry v)]
    (get mOpts opt)))

(s/fdef
  set-find-value
  :args (s/cat :set ::S/set-of-vals :v ::S/existing-string))

(defn set-find-didyoumean
  "Finds candidates after normalization.
  Return original candidates."
  [st v]
  (let [optMap (set-normalize-map st)
        ov  (set-normalized-entry v)
        cands (BU/candidate-suggestions (keys optMap) ov 0.33)]

    (mapv #(get optMap %) cands)))

(s/fdef
  set-find-value
  :args (s/cat :set ::S/set-of-vals :v ::S/existing-string))

(defn set-find-didyoumean-str
  "Returns ' Did you mean A or B?' or '' if no candidates. "
  [st v]
  (let [cs  (set-find-didyoumean st v)]
    (if
     (pos? (count cs))
      (str " Did you mean '"
           (str/join "' or '" cs)
           "'?")
      "")))

(s/fdef
  set-find-value
  :args (s/cat :set ::S/set-of-vals :v ::S/existing-string))

(defn asSet
  "Sets of options are dark black magic.
  They are also darn useful.

  A set can be composed of all-keywords or all-strings.

  Values are matched ignoring case, and the correct one
  is returned in the case and type it appears in the set.
  The default value, if present, is returned as-is, even if
  not a member of the set.

  Keywords are accepted with our without trailing `:`.

  On missing values, the closest matches are searched and printed.

  For example, on a set #{:a :b :c}, `A` `a` `:A` and `:a` all match `:a`.

  Defaults for a set are all values, with no colons, separated by a pipe.

  "
  [st v]

  (let [found (set-find-value st v)]
    (cond
      (nil? found)
      (let [message (str "Value '" v "' not allowed."
                         (set-find-didyoumean-str st v))]
        (throw (ex-info message {})))

      :else
      found)))


;; Remember to add these to
;; ::S/type


(def known-presets
  {:int    {:parse-fn    P/parseInt
            :placeholder "N"}
   :int-0  {:parse-fn    P/parseInt
            :placeholder "N"
            :default     0}

   :float  {:parse-fn    P/parseFloat
            :placeholder "N.N"}

   :float-0  {:parse-fn    P/parseFloat
              :placeholder "N.N"
              :default     0.0}

   :string {:placeholder "S"}

   :keyword {:placeholder "S"
             :parse-fn asKeyword}

   :with-flag {}

   :slurp  {:parse-fn    asSingleString
            :placeholder "f"}
   :slurplines {:parse-fn    asLinesString
                :placeholder "f"}
   :edn        {:parse-fn asDecodedEdnValue
                :placeholder "edn"}
   :ednfile    {:parse-fn asDecodedEdnFile
                :placeholder "f"}
   :json       {:parse-fn asDecodedJsonValue
                :placeholder "json"}
   :jsonfile   {:parse-fn asDecodedJsonFile
                :placeholder "f"}
   :yaml       {:parse-fn asDecodedYamlValue
                :placeholder "yaml"}
   :yamlfile   {:parse-fn asDecodedYamlFile
                :placeholder "f"}

   ; dates
   :yyyy-mm-dd {:placeholder "YYYY-MM-DD"     :parse-fn    P/asDate}
    ;;:validate    [#(true)
    ;;              "Must be a date in format YYYY-MM-DD"]
   })

(OPT/orchestra-instrument)