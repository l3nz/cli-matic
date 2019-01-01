#!/usr/bin/env bash
"exec" "plk" "-Sdeps" "{:deps {cli-matic {:mvn/version \"0.3.2\"}}}" "-Ksf" "$0" "$@"

(ns exit-code
  (:require [cli-matic.core :refer [run-cmd]]))

;; To run this, try from the project root:
;;
;; $ ./exit-status.cljs exit --mode NONE
;; echo $?
;; 0
;; $ ./exit-status.cljs exit --mode ONE
;; $ echo $?
;; 1
;; $ ./exit-status.cljs exit --mode XXX
;; ** ERROR: **
;; JVM Exception: #object[Error Error: No matching clause: XXX]
;; $ echo $?
;; 255


(defn exiter
  "Just exits with an exit code. 
  Unknown values cause an exception."
  [{:keys [mode]}]
  (condp = mode
    "NONE" nil
    "ONE"  1
    ))


(def CONFIGURATION
  {:app         {:command     "exit-code"
                 :description "Return the exit code"
                 :version     "0.0.1"}
   :global-opts []
   :commands    [{:command     "exit" :short "x"
                  :description ["Forces an exit code."]
                  :opts        [{:option "mode" :as "NONE|ONE|ERROR" :type :string :default :present}]
                  :runs        exiter}
                 ]})

(defn -main
  [& args]
  (run-cmd args CONFIGURATION))

(set! *main-cli-fn* -main)

