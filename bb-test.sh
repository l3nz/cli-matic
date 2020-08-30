#!/usr/bin/env bash
export BABASHKA_CLASSPATH="test:resources:$(clojure -A:babashka:babashka-test -Spath)"
bb -e "(require '[cli-matic.bb-runner :refer [run]])\
			(run)"
