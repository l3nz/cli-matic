#!/bin/sh
#_(
#_This runs clj so we use the deps.edn but specify which module 
#_we want to run. 
exec clj -J-Xms256m -J-Xmx256m -J-client  -J-Dclojure.spec.skip-macros=true -M -i "$0" -m toycalc "$@"
)


(ns toycalc
  (:require [cli-matic.core :refer [run-cmd]]
            [clojure.core.async :as CA]))

;
; This file is used to see what happens with promises,
; as for feature #84.
;

(defn wait3secs []
      (println "Wating 3 seconds....")
      (Thread/sleep 3000)
      (println "Waiting: done."))


(defn check_promise
      "A promise that returns 1"
      [_]
      (let [p (promise)]
           (.start (Thread.
                     (fn []
                        (do
                          (println "On new  thread")
                          (wait3secs)
                          (deliver p 1)
                          ))))
           p))


(defn check_future
      "A future that returns 2"
      [_]
      (future
        (do

          (println "Future: On new thread")
          (wait3secs)
          2)))

(defn check_core_async
      "A core-async channel that returns 3"
      [_]
      (let [c (CA/chan)]
           (CA/go
             (do
               (println "Core.async")
               (wait3secs)
               (CA/>! c 3)))

           c))


(def CONFIGURATION
  {:app      {:command     "jvm-promise"
              :description "Tests JVM promises"
              :version     "0.0.1"}
   :commands [{:command     "promise" :short "p"
               :description ["Checks Clojure  promises"]
               :opts        []
               :runs        check_promise}
              {:command     "future" :short "f"
               :description ["Checks Clojure future"]
               :opts        []
               :runs        check_future}
              {:command     "async" :short "a"
               :description ["Reads core.async channel"]
               :opts        []
               :runs        check_core_async}

              ]})

(defn -main
      "This is our entry point.
      Just pass parameters and configuration.
      Commands (functions) will be invoked as appropriate."
      [& args]
      (run-cmd args CONFIGURATION))
