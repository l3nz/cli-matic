# CLI-matic

Compact [sub]command line parsing library, for Clojure. Perfect for scripting (who said
Clojure is not good for scripting?). 


## Using

The library is available on Clojars:

[![Clojars Project](https://img.shields.io/clojars/v/cli-matic.svg)](https://clojars.org/cli-matic)
[![](https://cljdoc.xyz/badge/cli-matic)](https://cljdoc.xyz/jump/release/cli-matic)


Or the library can be easily referenced through Github:

	{:deps
	 {cli-matic
	  {:git/url "https://github.com/l3nz/cli-matic.git"
	   :sha "b27bc676a879542b4e83f1bef3b9776e600018e3"}}}

## Features

Especially when scripting, you should write interesting code, not boilerplate.

* Create **all-in-one scripts with subcommands and help**, in a way more compact than the excellent `tools.cli`.
* **Avoid common pre-processing.** Parsing dates, integers, reading small files, downloading a JSON URL.... it should just happen. The more you declare, the less time you waste.
* **Validate with Spec.** Modern Clojure uses Spec, so validation should be spec-based as well. Validation should happen at the parameter level, and across all parameters at once. Again, the more you have in declarative code, the less room for mistakes. 
* **Read environment variables.** Passing environment variables is a handy way to inject passwords, etc. This should just happen and be declarative.
* **Capture unnamed parameters** as if they were named parameters, with casting, validation, etc.

While targeted at scripting, CLI-matic of course works with any program receiving CLI arguments.


## Rationale

Say we want to create a short script, in Clojure, where we want
to run a very simple calculator that either sums A to B or subtracts B from A:


	$ clj -m calc add -a 40 -b 2
	42
	$ clj -m calc sub -a 10 -b 2
	8
	$ clj -m calc --base 16 add -a 30 -b 2
	20

We also want it to display its help:

	$clj -m calc -?
	NAME:
	 toycalc - A command-line toy calculator

	USAGE:
	 toycalc [global-options] command [command options] [arguments...]

	VERSION:
	 0.0.1

	COMMANDS:
	   add, a   Adds two numbers together
	   sub, s   Subtracts parameter B from A

	GLOBAL OPTIONS:
	       --base N  10  The number base for output
	   -?, --help


And help for sub-commands:

	$clj -m calc add -?
	NAME:
	 toycalc add - Adds two numbers together

	USAGE:
	 toycalc [add|a] [command options] [arguments...]

	OPTIONS:
	   -a, --a1 N  0  Addendum 1
	   -b, --a2 N  0  Addendum 2
	   -?, --help

But while we are coding this, we do not realy want to waste time writing any parsing logic.
What we care about implementing are the functions `add-numbers` and `sub-numbers` where we do actual work; the rest should be declared externally and/or "just happen".

From the point of view of us programmers, we'd like to have a couple of functions like:

	(defn add-number
		"Sums A and B together, and prints it in base `base`"
		[{:keys [a b base]}]
		(Integer/toString (+ a b) base))

And nothing more; **the fact that both parameters exist, are of the right type, have the right defaults, print
the correct help screen, etc., should ideally not be a concern.**


	(def CONFIGURATION
	  {:app         {:command     "toycalc"
	                 :description "A command-line toy calculator"
	                 :version     "0.0.1"}

	   :global-opts [{:option  "base"
	                  :as      "The number base for output"
	                  :type    :int
	                  :default 10}]

	   :commands    [{:command     "add"
	                  :description "Adds two numbers together"
	                  :opts        [{:option "a" :as "Addendum 1" :type :int}
	                                {:option "b" :as "Addendum 2" :type :int :default 0}]
	                  :runs        add_numbers}

	                 {:command     "sub"
	                  :description "Subtracts parameter B from A"
	                  :opts        [{:option "a" :as "Parameter A" :type :int :default 0}
	                                {:option "b" :as "Parameter B" :type :int :default 0}]
	                  :runs        subtract_numbers}
	                 ]})



### Current pre-sets

The following pre-sets (`:type`) are available:

* `:int` - an integer number
* `:int-0` - an integer number, with defaults to zero
* `:float` - a float number
* `:float-0` - a float number, with defaults to zero
* `:string` - a string
* `:json` - a JSON literal value, that will be decoded an returned as a Clojure structure.
* `:yyyy-mm-dd` - a Date object, expressed as "yyyy-mm-dd" in the local time zone
* `:slurp` - Receives a file name - reads is as text and returns it as a single string. Handles URIs correctly.
* `:slurplines` - Receives a file name - reads is as text and returns it as a seq of strings. Handles URIs correctly.
* `:jsonfile` - a file (or URL) containing JSON, that will be decoded and returned as a Clojure structure.


For all options, you can then add:

* `:default` the default value, as expected after conversion. If no default, the value will be 
  passed only if present. If you set `:default :present` this means that CLI-matic will abort
  if that option is not present (and it appears with a trailing asterisk in the help)
* `:multiple` if true, the values for all options with the same name are stored in an array
* `:short`: a shortened name for the command (if a string), or a positional argument if integer (see below)
* `:env` if set, the default is taken from the current value of an envirnonment variable. For capture to happen, either the option must be mising, or its value must be invalid.

[to be done]

* boolean types
* having a library of ready-made types that cover most cases
* using spec for checking values


### Return values

The function called can return an integer; if it does, it is used as an exit code
for the shell process.

Errors and exceptions return an exit code of -1; while normal executions (including invocations 
of help) return 0.

### Positional arguments

If there are values that are not options in your command line, CLI-matic will usually return them in an array of unparsed entries, as strings.
But  - if you use the positional syntax for `short`:

	{:option "a1" :short 0 :as "First addendum" :type :int :default 23}

You 'bind' the 	option 'a1' to the first unparsed element; this means that
you can apply all presets/defaults/validation rules as if it was a named option.

So you could call your script as:

	clj -m calc add --a2 3 5

And CLI-matic would set 'a2' to 3 and have "5" as an unparsed argument; and then bind it to "a1", so it will be cast to an integer. You function will be called with:

	{:a1 5, :a2 3}

That is what you wanted from the start.

At the same time, the named option remains, so you can use either version. Bound entries are not removed from the unparsed command line entries.


### Transitive dependencies

CLI-matic currently depends on:

* org.clojure/clojure 
* org.clojure/spec.alpha 
* org.clojure/tools.cli 
* orchestra 

To use Json decoding, you need Cheshire to be on the classpath; otherwise it will break.
If you do not need JSON parsing, you can do without.

## License

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.

You must not remove this notice, or any other, from this software.

