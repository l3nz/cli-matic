(ns cli-matic.presets
  "
  ### Known presets


  "
  (:require [clojure.string :as str]
            [cli-matic.optionals :as OPT]
            [cli-matic.platform :as P]
            ))

;; Known presets

(defn asSingleString
  "Turns a filename into a single string.

  If argument is a String, it tries to resolve it first as a URI, then\n as a local file name.  URIs with a 'file' protocol are converted to\n local file names.

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
  (OPT/json-decode-cheshire s))

(defn asDecodedJsonFile
  "Decodes the contents of a file as a JSON object."
  [filename]
  (OPT/json-decode-cheshire (asSingleString filename)))

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