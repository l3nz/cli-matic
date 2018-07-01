(ns cli-matic.presets
  (:require [clojure.string :as str]))

;; Known presets
(defn parseInt
  "Converts a string to an integer. "
  [s]
  (Integer/parseInt s))

(defn parseFloat
  "Converts a string to a float."
  [s]
  (Float/parseFloat s))

(defn asDate
  "Converts a string to a Date object; if conversion
  fails, returns nil."
  [s]
  (try
    (.parse
     (java.text.SimpleDateFormat. "yyyy-MM-dd") s)
    (catch Throwable t
      nil)))

(defn asSingleString
  "Turns a filename into a single string.

  If argument is a String, it tries to resolve it first as a URI, then\n as a local file name.  URIs with a 'file' protocol are converted to\n local file names.

  "
  [filename]
  (cond
    (nil? filename)   ""
    (empty? filename) ""
    :else (slurp filename)))

(defn asLinesString
  "Reads a text file and returns it as a collection of lines."
  [filename]
  (str/split-lines (asSingleString filename)))

;; ---------------
;; Cheshire is an optional dependency, so we check for it at compile time.
;; Taken from core.clj in https://github.com/dakrone/clj-http
(def json-enabled?
  (try
    (require 'cheshire.core)
    true
    (catch Throwable _ false)))

(defn ^:dynamic json-decode
  "Resolve and apply cheshire's json decoding dynamically."
  [& args]
  {:pre [json-enabled?]}
  (apply (ns-resolve (symbol "cheshire.core") (symbol "decode")) args))

(defn asDecodedJsonValue
  "Decodes the value as a JSON object."
  [s]
  (json-decode s))

(defn asDecodedJsonFile
  "Decodes the contents of a file as a JSON object."
  [filename]
  (json-decode (asSingleString filename)))

;; Remember to add these to
;; ::S/type
(def known-presets
  {:int    {:parse-fn    parseInt
            :placeholder "N"}
   :int-0  {:parse-fn    parseInt
            :placeholder "N"
            :default     0}

   :float  {:parse-fn    parseFloat
            :placeholder "N.N"}

   :float-0  {:parse-fn    parseFloat
              :placeholder "N.N"
              :default     0.0}

   :string {:placeholder "S"}

   :slurp  {:parse-fn    asSingleString
            :placeholder "f"}
   :slurplines {:parse-fn    asLinesString
                :placeholder "f"}
   :json       {:parse-fn asDecodedJsonValue
                :placeholder "json"}
   :jsonfile   {:parse-fn asDecodedJsonFile
                :placeholder "f"}

   ; dates
   :yyyy-mm-dd {:placeholder "YYYY-MM-DD"     :parse-fn    asDate}
    ;;:validate    [#(true)
    ;;              "Must be a date in format YYYY-MM-DD"]
})
