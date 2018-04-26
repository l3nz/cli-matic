# cli-matic

Compact [sub]command line parsing library, for Clojure.



## Using

The library is available on Clojars:

	[cli-matic "0.1.0"]

Or the library can be easily referenced through Github:

	{:deps
	 {cli-matic
	  {:git/url "https://github.com/l3nz/cli-matic.git"
	   :sha "545ceac178771f9fd5560c948a301f836d3be19e"}}}


### Transitive dependencies

cli-matic currently depends on:

* org.clojure/clojure {:mvn/version "1.9.0"}
* org.clojure/spec.alpha {:mvn/version "0.1.143"}
* org.clojure/tools.cli {:mvn/version "0.3.5"} 


## Rationale

Say you want to create a simple script, in Clojure, where you want
to run a very simple calculator that either sums A to B or subtracts B from A:


	$ clj -m calc add --a 40 --b 2
	42
	$ clj -m calc sub --a 10 --b 2
	8
	$ clj -m calc --base 16 add --a 30 --b 2
	20

We would also want it to display its help:

	$clj -m calc -?
	NAME:
	 toycalc - A command-line toy calculator

	USAGE:
	 toycalc [global-options] command [command options] [arguments...]

	VERSION:
	 0.0.1

	COMMANDS:
	 add    Adds two numbers together
	 sub    Subtracts parameter B from A

	GLOBAL OPTIONS:
	       --base N  10  The number base for output


And help for sub-commands:

	$clj -m calc add -?
	NAME:
	 toycalc add - Adds two numbers together

	USAGE:
	 toycalc add [command options] [arguments...]

	OPTIONS:
	       --a N     Addendum 1
	       --b N  0  Addendum 2

But while we are coding this, we do not realy want to waste time writing any parsing logic.
What we care about are functions "add-numbers" and "sub-numbers"; the rest sould just be declared externally.

From your point of view of an application programmer, you'd like to have a function:

	(defn add-number
		"Sums A and B together, and prints it in base `base`"
		[{:keys [a b base]}]
		(Integer/toString (+ a b) base))

And nothing more; the fact that both parameters exist, are of the right type, have the right defaults, print
the correct help screen, etc., should ideally not be a concern.


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

The following pre-sets (":type") are available:

* `:int` - an integer number
* `:int-0` - an integre number, with defaults to zero
* `:string` - a string
* `:yyyy-mm-dd` - a Date object, expressed as "yyyy-mm-dd" in the local time zone

For all options, you can then add:

* `:default` the default value, as expected after conversion

[to be done]

* boolean types
* having a library of ready-made types that cover most cases
* using spec for checking values
* `:multiple` if true, the values for all options with the same name are stored in an array
* `:env` if set, the default is taken from the current value of an envirnoment variable


### Return values

The function called can return an integer; if it does, it is used as an exit code
for the shell process.

Errors and exceptions return an exit code of -1; while normal executions (including invocations 
of help) return 0.









## License

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.

You must not remove this notice, or any other, from this software.

