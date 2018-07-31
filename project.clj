(defproject cli-matic "0.1.15"
  :description "Compact [sub]command line parsing library, for Clojure"
  :url "https://github.com/l3nz/cli-matic"
  :license {:name "Eclipse Public License, v2"
            :url "http://www.eclipse.org/legal/epl-v20.html"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/spec.alpha "0.1.143" :scope "provided"]
                 [org.clojure/tools.cli "0.3.7"]
                 [orchestra "2017.11.12-1"]
                 [cheshire "5.8.0" :scope "provided"]
                 [io.forward/yaml "1.0.9" :scope "provided"]]
  :scm {:name "git"
        ;; :tag "..."
        :url "https://github.com/l3nz/cli-matic" }
  :plugins [[lein-eftest "0.5.1"]
            [jonase/eastwood "0.2.5"]
            [lein-kibit "0.1.6"]
            [lein-cljfmt "0.5.7"]]
  :deploy-repositories [["clojars"  {:sign-releases false :url "https://clojars.org/repo"}]
                        ["snapshots" {:sign-releases false :url "https://clojars.org/repo"}]])

