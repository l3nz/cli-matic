(ns cli-matic.bb-runner
  #?(:bb (:require [clojure.test :refer [run-tests]]
                   [cli-matic.core-test]
                   [cli-matic.help-gen-test]
                   [cli-matic.presets-test]
                   [cli-matic.utils-candidates-test]
                   [cli-matic.utils-convert-config-test]
                   [cli-matic.utils-test]
                   [cli-matic.utils-v2-test])))

#?(:bb (defn run []
         (let [{:keys [fail error]} (run-tests
                                     'cli-matic.core-test
                                     'cli-matic.help-gen-test
                                     'cli-matic.presets-test
                                     'cli-matic.utils-candidates-test
                                     'cli-matic.utils-convert-config-test
                                     'cli-matic.utils-test
                                     'cli-matic.utils-v2-test)]
           (System/exit (+ fail error)))))
