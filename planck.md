# Using CLI-matic with Planck

CLI-matic works with Planck! Planck is a stand-alone ClojureScript REPL for macOS and Linux based on JavaScriptCore that includes a small emulation library for common shell functions (eg reading files, variables, forking processes) so it is very easy to use for scripting. http://planck-repl.org/

## Getting started

Scripting with Planck is very similar to Clojure; there are a few things that you should remember:

* If you want single-file scripts, remember to start with the preamble: 
		#!/usr/bin/env bash
		"exec" "plk" "-Sdeps" "{:deps {cli-matic {:mvn/version \"0.2.10\"}}}" "-Ksf" "$0" "$@"
* After declaring main, add: `(set! *main-cli-fn* -main)` so Planck knows where to start the script.

On first run, with compilation, you will get a number of warnings; some are caused by CLI-matic and
we plan to fix them, and some  are caused by dependencies.

	WARNING: toycalc is a single segment namespace at line 4
	WARNING: Use of undeclared Var clojure.tools.cli/Exception at line 126 clojure/tools/cli.cljc
	WARNING: Use of undeclared Var clojure.tools.cli/Exception at line 126 clojure/tools/cli.cljc
	WARNING: Use of undeclared Var clojure.tools.cli/*err* at line 228 clojure/tools/cli.cljc
	WARNING: Use of undeclared Var cli-matic.presets/slurp at line 24 cli_matic/presets.cljc
	WARNING: Use of undeclared Var cli-matic.core/Throwable at line 74 cli_matic/core.cljc
	WARNING: Use of undeclared Var cli-matic.core/Throwable at line 233 cli_matic/core.cljc
	WARNING: Use of undeclared Var cli-matic.core/IllegalAccessException at line 422 cli_matic/core.cljc
	WARNING: Use of undeclared Var cli-matic.core/IllegalAccessException at line 422 cli_matic/core.cljc
	WARNING: Use of undeclared Var cli-matic.core/IllegalAccessException at line 454 cli_matic/core.cljc
	WARNING: Use of undeclared Var cli-matic.core/IllegalAccessException at line 454 cli_matic/core.cljc
	WARNING: Use of undeclared Var cli-matic.core/Throwable at line 524 cli_matic/core.cljc

In spite of this, the library is currently usable!

## What works

* Help generation
* Simple parameters


## What does not work

* Expound does not work with ClojureScript 1.10.439
* Specs do not seem to work much, even without Expound
* :spec validation does not work, fails with `Right hand side of instanceof is not an object` - see 'toycalc-spec.cljs'.
* No slurping of files
* No JSON / EDN / YAML (at the moment)
* No environment variables




