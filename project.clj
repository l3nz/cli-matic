(defproject cli-matic "0.3.6"
  :description "Compact [sub]command line parsing library, for Clojure"
  :url "https://github.com/l3nz/cli-matic"
  :license {:name "Eclipse Public License, v2"
            :url  "http://www.eclipse.org/legal/epl-v20.html"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.10.439" :scope "provided"]
                 [org.clojure/spec.alpha "0.1.143" :scope "provided"]
                 [org.clojure/tools.cli "0.4.1"]
                 [orchestra "2017.11.12-1" :scope "provided"]
                 [cheshire "5.8.0" :scope "provided"]
                 [io.forward/yaml "1.0.9" :scope "provided"]
                 [l3nz/planck "0.0.0" :scope "provided"]
                 [expound "0.7.1"]]
  :scm {:name "git"
        ;; :tag "..."
        :url  "https://github.com/l3nz/cli-matic"}
  :plugins [[lein-eftest "0.5.1"]
            [jonase/eastwood "0.2.5"]
            [lein-kibit "0.1.6"]
            [lein-cljfmt "0.5.7"]
            [lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.11"]]
  :cljsbuild
  {:test-commands {"unit-tests" ["node" "target/unit-tests.js"]}
   :builds
                  {:tests
                   {:source-paths   ["src" "test"]
                    :notify-command ["node" "target/unit-tests.js"]
                    :compiler       {:output-to     "target/unit-tests.js"
                                     :optimizations :none
                                     :target        :nodejs
                                     :main          cli-matic.cljs-runner}}
                   :production
                   {:source-paths ["src"]
                    :compiler     {:output-to     "target/production.js"
                                   :optimizations :advanced}}}}
  :deploy-repositories [["clojars" {:sign-releases false :url "https://clojars.org/repo"}]
                        ["snapshots" {:sign-releases false :url "https://clojars.org/repo"}]])

