# How to deploy

Before commit:

	lein clean && lein eftest

Then, let's format sources:

	lein cljfmt check
	lein cljfmt fix

And commit with a message "Fixing #XX - Description"

## Pushing to Clojars

First advance the version in project.clj.

Then check your Clojars credentials and push:

	lein deploy clojars
	Username: l3nz
	

## Pushing documentation

Change the documentation linkin `README.md`. Then click on it.



