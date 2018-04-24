(ns cli-matic.presets)



;
; Known presets
;

(defn parseInt [s]
  (Integer/parseInt s))


(defn asDate [s]
  (try
    (.parse
      (java.text.SimpleDateFormat. "yyyy-MM-dd") s)
    (catch Throwable t
      nil)))



;
; Remember to add these to
; ::S/type

(def known-presets
  {:int    {:parse-fn    parseInt
            :placeholder "N"}

   :int-0  {:parse-fn    parseInt
            :placeholder "N"
            :default     0}

   :string {:placeholder "S"}

   :yyyy-mm-dd
           {:placeholder "YYYY-MM-DD"
            :parse-fn    asDate
            :validate    [some? "Must be a date in format YYYY-MM-DD"]}

   })

