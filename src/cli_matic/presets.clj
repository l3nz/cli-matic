(ns cli-matic.presets)



;
; Known presets
;

(defn parseInt [s]
  (Integer/parseInt s))


(def known-presets
  {:int {:parse-fn parseInt
         :placeholder "N"}

   :string {:placeholder "S"}


   })

