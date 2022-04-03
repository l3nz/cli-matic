#!/bin/sh
#_(
#_This runs clj so we use the deps.edn but specify which module 
#_we want to run. 
exec clj -J-Xms256m -J-Xmx256m -J-client  -J-Dclojure.spec.skip-macros=true -M -i "$0" -m sets "$@"
)

(ns sets
  (:require [cli-matic.core :refer [run-cmd]]))

;; To run this, try from the project root:
;; clj -i examples/sets.clj -m sets --mode http prn --set-kws OnE --set-str baah

(defn print_out
  "Print options"
  [opts]
  (println opts))


(def CONFIGURATION
  {:app         {:command     "sets"
                 :description "A command-line toy calculator"
                 :version     "0.0.1"}
   :global-opts [{:option  "mode"
                  :as      "Connection model"
                  :type    #{"HTTP" "FTP"}
                  :default "HTTP"}]
   :commands    [{:command     "prn" :short "p"
                  :description "Prints parameters"
                  :opts        [{:option "set-kws" :as "Set of KWs" :type #{:one :une :two :three} :default :one}
                                {:option "set-str" :as "Set of str" :type #{"BAAH" "bah" "black" "Sheep"} :default :present}]
                  :runs        print_out}
                 ]})

(defn -main [& args]
  (run-cmd args CONFIGURATION))
