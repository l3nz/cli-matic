(ns cmds
  (:require
   [clojure.pprint :as pp]
   [cli-matic.core :refer [run-cmd]]))

;; To run this, try (from the parent directory):
;; $ clj -m cmds get repo
;;   {:resource "repo", :_arguments ["repo"]}
;;   got REPO

;; $ clj -m cmds list cluster sf
;;   {:resource "cluster", :location "sf", :_arguments ["cluster" "sf"]}
;;   CLUSTER

;; Note that in production, you'd probably want a separate dispatch
;; function that did things like normalizing case and converting to
;; keywords.

;; Single dispatch arg
(defmulti get-resource :resource)

(defmethod get-resource "repo"
  [args]
  (pp/pprint args)
  (println "got REPO"))

(defmethod get-resource "cluster"
  [args]
  (pp/pprint args)
  (println "got CLUSTER"))

;; Multiple dispatch args
(defmulti list-resource (juxt :resource :location))

(defmethod list-resource ["repo" "sf"]
  [args]
  (pp/pprint args)
  (println "REPO"))

(defmethod list-resource ["cluster" "sf"]
  [args]
  (pp/pprint args)
  (println "CLUSTER"))


(def CONFIGURATION
  {:app         {:command     "cmds"
                 :description "An example of nested commands"
                 :version     "0.0.1"}
   :global-opts []
   :commands    [{:command     "get"  :short "g"
                  :description "Get a resource"
                  :opts        [{:option "resource" :short 0 :as "Resource" :type :string :default "cluster"}]
                  :runs        get-resource}
                 {:command     "list" :short "l"
                  :description ["List a resource"]
                  :opts        [{:option "resource" :short 0 :as "Resource" :type :string :default "repo"}
                                {:option "location" :short 1 :as "Some location" :type :string :default "sf"}]
                  :runs        list-resource}
                 ]})

(defn -main
  "This is our entry point.
  Just pass parameters and configuration.
  Commands (functions) will be invoked as appropriate."
  [& args]
  (run-cmd args CONFIGURATION))
