#!/usr/bin/env bash
"exec" "plk"  "$0" "$@"

(ns read-file
  (:require [cli-matic.core :refer [run-cmd]]))

;; To run this, try from the project root:
;;
;; ./read-file.cljs p --text data/multiline-text.txt --lines data/multiline-text.txt --json data/json-example.json --edn data/edn-example.edn
;;
;; You can also use URLs:
;;
;; ./read-file.cljs p --json https://jsonplaceholder.typicode.com/todos/1

(defn printer
  "Just prints read values."
  [{:keys [text json edn lines]}]
  (prn {:text text 
        :lines lines 
        :json json 
        :edn edn}))


(def CONFIGURATION
  {:app         {:command     "read-file"
                 :description "Reads all parameters as files."
                 :version     "0.0.1"}
   :global-opts []
   :commands    [{:command     "printer" :short "p"
                  :description ["Prints parameters. All files can be local or http/https"]
                  :opts        [{:option "text" :as "A text file as single string" :type :slurp}
                                {:option "lines" :as "A text file as lines" :type :slurplines}
                                {:option "json" :as "A JSON file" :type :jsonfile}
                                {:option "edn"  :as "An EDN file" :type :ednfile}]
                  :runs        printer}
                 ]})

(defn -main
  [& args]
  (run-cmd args CONFIGURATION))

(set! *main-cli-fn* -main)

