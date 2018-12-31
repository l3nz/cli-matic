# Using CLI-matic with Planck

CLI-matic 0.3.0 works with Planck! Planck is a stand-alone ClojureScript REPL for macOS and Linux based on JavaScriptCore that includes a small emulation library for common shell functions (eg reading files, variables, forking processes) so it is very easy to use for scripting. http://planck-repl.org/

## Getting started

To start toying, you need to have Planck installed; just clone this repo, go to ''examples-cljs-planck'' and run the files in there.

Some commands you may want to try:

	./toycalc.cljs
	./toycalc.cljs add -?
	./toycalc.cljs --base 16 add -a 10 -b 20
	AA=10 ./toycalc.cljs --base 16 add -b 20

	./toycalc-spec.cljs
	./toycalc-spec.cljs add -?
	./toycalc-spec.cljs add -a 1 -b 2
	./toycalc-spec.cljs add -a 1 -b 1

Note how they are self-contained; just one file for each scripts. Also note how you waste no time on boilerplate functions, and you get help and parameter validation for free.

## Scripting

Scripting with Planck is very similar to Clojure; there are a few things that you should remember:

* If you want single-file scripts, remember to start with the preamble: 
		`#!/usr/bin/env bash`
		`"exec" "plk" "-Sdeps" "{:deps {cli-matic {:mvn/version \"0.3.1\"}}}" "-Ksf" "$0" "$@"`
* After declaring main, add: `(set! *main-cli-fn* -main)` so Planck knows where to start the script.

On first run, with compilation, you will get a number of warnings; some are caused by CLI-matic and
we plan to fix them, and some  are caused by dependencies.

	WARNING: toycalc is a single segment namespace at line 4
	WARNING: Use of undeclared Var clojure.tools.cli/Exception at line 126 clojure/tools/cli.cljc
	WARNING: Use of undeclared Var clojure.tools.cli/Exception at line 126 clojure/tools/cli.cljc
	WARNING: Use of undeclared Var clojure.tools.cli/*err* at line 228 clojure/tools/cli.cljc

In spite of this, *the library is currently usable*!


When debugging, it is sometimes useful to:

* Pin to a local JAR file: `"{:deps {cli-matic {:local/root \"../target/cli-matic-0.2.16-SNAPSHOT.jar\"}}}"`
* Remove the `.planck-cache/` folder before each run
* Add `"-v"` for verbose compilation

## What currently works

* Help generation
* Simple parameters
* clojure.spec parameter validation
* Slurping of files and HTTP resources
* Environment variables

## What does not work

* Expound does not work with ClojureScript 1.10.439 in defining custom specs.
* No automatic reading of JSON [#60] / EDN / YAML parameters (at the moment, but it's easily doable)
* Scripot return value is not set #63
* Dates - what to do with dates?


