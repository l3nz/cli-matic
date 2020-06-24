(ns cli-matic.platform-macros
  "## Macros shared between CLJ and CLJS.
  "
  #?(:cljs (:require-macros [cli-matic.platform-macros])));trick so macros to be the target of :refer

#?(:clj

   (defmacro try-catch-all
     "
This creates a try-catch block that either traps
Throwable on the JVM or :default on Node.

Use:

`(try-catch-all (/ 1 0) (fn [x] 0))`

So both expressions must be surrounded by round parentheses.



"

     [f onErr]
     `(try (~@f)
           (catch Throwable t#
             ((~@onErr) t#)))))

#?(:cljs
   (defmacro try-catch-all
     "
See the .clj docs.
"
     [f onErr]
     `(try (~@f)
           (catch :default t#
             (do
               ((~@onErr) t#))))))

