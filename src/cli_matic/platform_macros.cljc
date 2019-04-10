(ns cli-matic.platform-macros
  "## Macros shared between CLJ and CLJS.

  Unfortunately:

  - Macros must be in a separate file (for CLJS)
  - Macros have a different import syntax (CLJS)
  - Macros defined in .clj  will STILL be loaded by CLJS and will fail big time.

  So we have to use a separate namespace and hide everything behind reader conditionals.

  Usage is:

  #?(:clj [cli-matic.platform-macros :refer [try-catch-all]]\n
     :cljs [cli-matic.platform-macros :refer-macros [try-catch-all]]\n               )

  This was a real PITA, man.

  ")

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

