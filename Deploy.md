# How to deploy

Before commit:

	lein clean && lein eftest

Then let's see if the linter notices something.

	lein clj-kondo

Then, run the  new version with all script examples:

	./scriptTester.sh

and make sure it's all green.

Then, let's format sources:

	lein cljfmt check
	lein cljfmt fix

And commit with a message "Fixing #XX - Description"

Do not forget to edit the Changelog.

Also, make sure you check test coverage - can it be improved?

	lein cloverage

Also it's a good time to check for outdated dependencies:

	lein ancient check


## Pushing to Clojars

First advance the version in project.clj. Make sure that CHANGELOG.md is up to date.

Then create a lightweight tag and push it

	git tag v0.1.18
 	git push origin v0.1.18


Then check your Clojars credentials and push:

	lein deploy clojars
	Username: l3nz
	

To see my account, `creds.sh clojars`.

## Pushing documentation

Change the documentation linkin `README.md`. Then click on it.


# ClojureScript

Unit tests:

	lein cljsbuild test unit-tests




