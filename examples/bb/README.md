# Scripting with Ba*bash*ka

Babashka is a native, stand-alone Clojure interpreter built for scripting - https://github.com/babashka/babashka


This is the time it takes to run a simple script from JMV/Clojure:

		./toycalc.clj  4.49s user 0.15s system 295% cpu 1.569 total

This is the same script in Babashka:

		./toycalc.bb  0.43s user 0.04s system 74% cpu 0.484 total

'nuff said, right?

## Differences 

- In the sample script, we import libraries (including *cli-matic*) right from the script itself,
  so you can have single-file scripts.
  As an alternative, for example if you have multiple scripts with the same deps and you don't want to change them all
  when you upgrade a library,
  you can to have a `bb.edn` file with the libraries you need. Look at the `example_bb.edn` 
  file here for a minimal working solution.
- You do not need an external namespace, but you can simply `(require '[cli-matic.core :refer [run-cmd]])` 
- You don't need a main function, but you just call `(run-cmd *command-line-args* CONFIGURATION)`

Good luck!

