#!/usr/bin/env bash
"exec" "plk" "-Sdeps" "{:deps {cli-matic {:mvn/version \"0.3.3\"}}}" "-Ksf" "$0" "$@"


(ns sets
  (:require [cli-matic.core :refer [run-cmd]]))

;; To run this, try from the project root:
;; ./sets.cljs p --set-kws one --set-str Black
;; ./sets.cljs p --set-kws one --set-str Blacka

(defn print_out
  "Print options"
  [opts]
  (println opts))


(def CONFIGURATION
  {:app         {:command     "sets"
                 :description "Shows sets"
                 :version     "0.0.1"}
   :global-opts [{:option  "mode"
                  :as      "Connection model"
                  :type    #{"HTTP" "FTP"}
                  :default "HTTP"}]
   :commands    [{:command     "prn" :short "p"
                  :description "Prints parameters"
                  :opts        [{:option "set-kws" :as "Set of KWs" :type #{:one :two :three} :default :one}
                                {:option "set-str" :as "Set of str" :type #{"BAAH" "bah" "black" "Sheep"} :default :present}]
                  :runs        print_out}
                 ]})

(defn -main [& args]
  (run-cmd args CONFIGURATION))

(set! *main-cli-fn* -main)
