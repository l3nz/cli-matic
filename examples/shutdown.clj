(ns shutdown
  (:require [cli-matic.core :refer [run-cmd]]))

;; To run this, try from the project root:
;; clj -i examples/shutdown.clj -m shutdown add -a 1 -b 80

(defn add-after-waiting-some-seconds
      "Sums A and B together, and prints it in base `base`"
      [{:keys [a1 a2 base]}]
      (do
        (println "Press Ctrl+C for shutdown. I'm sleeping 5 secs now.")
        (Thread/sleep 5000)
        (println
          (Integer/toString (+ a1 a2) base))))

(defn shutdown-fn
      "This is a shutdown example, called when the JVM dies."
      []
      (prn "Shutdown called. Time to clean up."))


(def CONFIGURATION
  {:app         {:command     "shutdown"
                 :description "An example for the shutdown hook."
                 :version     "0.0.1"}
   :global-opts [{:option  "base"
                  :as      "The number base for output"
                  :type    :int
                  :default 10}]
   :commands    [{:command     "add" :short "a"
                  :description ["Adds two numbers together"
                                (str cli-matic.optionals/with-orchestra?)
                                ""
                                "Looks great, doesn't it?"]
                  :opts        [{:option "a1" :short "a" :env "AA" :as "First addendum" :type :int :default 0}
                                {:option "a2" :short "b" :as "Second addendum" :type :int :default 0}]
                  :runs        add-after-waiting-some-seconds
                  :on-shutdown shutdown-fn}
                 ]})

(defn -main
  "This is our entry point.
  Just pass parameters and configuration.
  Commands (functions) will be invoked as appropriate."
  [& args]
  (run-cmd args CONFIGURATION))
