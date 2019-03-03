#!/bin/sh
#_(
#_This runs clj so we use the deps.edn but specify which module 
#_we want to run. 
exec clj -J-Xms256m -J-Xmx256m -J-client  -J-Dclojure.spec.skip-macros=true -i "$0" -m noparms "$@"
)
(ns noparms
  (:require [cli-matic.core :refer [run-cmd]]))

;; To run this, try from the project root:
;; clj -i examples/noparms.clj -m noparms hi

(defn say_hi
  "Just say hi"
  [parms]
  (println "Hi man!"))


(def CONFIGURATION
  {:app         {:command     "sayhi"
                 :description "Greeter"
                 :version     "0.0.1"}
   :global-opts []
   :commands    [{:command     "hi" 
                  :description "Greets you"
                  :opts        []
                  :runs        say_hi}
                ]})

(defn -main  [& args]
  (run-cmd args CONFIGURATION))
