# Scripting with Bababshka

This is JMV/Clojure

		./toycalc.clj  4.49s user 0.15s system 295% cpu 1.569 total

This is bb

		./toycalc.bb  0.43s user 0.04s system 96% cpu 0.484 total

'nuff said, right?

## Differences 

- You need to have a `bb.edn` file with the libraries you need. Look at the one here for a minimal working solution.
- You do not need an external namespace, but you can simply `(require '[cli-matic.core :refer [run-cmd]])` 
- You don't need a main function, but you just call `(run-cmd *command-line-args* CONFIGURATION)`

Good luck!
