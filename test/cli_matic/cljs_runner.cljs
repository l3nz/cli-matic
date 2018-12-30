(ns cli-matic.cljs-runner
  (:require [cljs.test :refer [run-tests]]
            [cli-matic.core-test]
            [cli-matic.help-gen-test]
            [cli-matic.presets-test]
            [cli-matic.utils-test]))

  ; Turn on console printing. Node can't print to *out* without.
(enable-console-print!)

  ; This must be a root level call for Node to pick it up.
(run-tests 'cli-matic.core-test
           'cli-matic.help-gen-test
           'cli-matic.presets-test
           'cli-matic.utils-test)

