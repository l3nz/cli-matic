# cli-matic
Compact [sub]command line parsing library, for Clojure

## Using

The library can be easily referenced through Github:

	{:deps
	 {cli-matic
	  {:git/url "https://github.com/l3nz/cli-matic.git"
	   :sha "1afc35f1bac6c84363fcb3160e8ad3e0ed64f78e"}}}

At the moment there are no artifacts released.

### Transitive dependencies

cli-matic currently depends on:

* org.clojure/clojure {:mvn/version "1.9.0"}
* org.clojure/spec.alpha {:mvn/version "0.1.143"}
* org.clojure/tools.cli {:mvn/version "0.3.5"} 


## Rationale

Say you want to create a simple script, in Clojure, where you want
to run a very simple calculator that either sums A to B or subtracts B from A:


	$ clj -m calc add -a 40 -b 2
	42
	$ clj -m calc sub -a 10 -b 2
	8
	$ clj -m calc --base 16 add -a 30 -b 2
	20

We would also want it to display its help:

	$clj -m calc -?
	Calc version 0.2
	Usage: calc [parms] cmd

	Parms:
	    -b, --base n 	      The number base for output (default 10)

	Cmd:
		add:          Adds A and B together
		sub:          Subtracts B from A

And help for sub-commands:

	$clj -m calc add -?
	Calc version 0.2
	Usage: calc [parms] add [-a n] [-b n]
	Adds A and B together
	
	Parms:
	    -a          Parameter A
	    -b   		Parameter B


But while we are coding this, we do not realy want to waste time writing any parsing logic.
What we care about are functions "add-numbers" and "sub-numbers"; the rest sould just be declared externally.

From your point of view of an application programmer, you'd like to have a function:

	(defn add-number
		"Sums A and B together, and prints it in base `base`"
		[{:keys [a b base]}]
		(Integer/toString (+ a b) base))

And nothing more; the fact that both parameters exist, are of the right type, have the right defaults etc, 
should not be a concern.

(def CLI 
	{:description "Calc version 0.2"
	 :global-parms {["-b" "--base n" :default 10 ]}
	 :commands {
	 	[{:description "Adds A and B together"
	 	  :short-form "a"
	 	  :long-form "add"
	 	  :fn    add-number
	 	  :spec  spec-add
	 	  :parms [
	 	  {:short "-a" :type :integer }
	 	  {:short "-b" :type :integer }]
	 	}]

	}
}
)



## License

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.

You must not remove this notice, or any other, from this software.

