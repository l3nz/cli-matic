# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## 0.2.5 - 2018-11-16
### Changed
* Fixes #26 - CLI-matic will now suggest subcommands if you mistype them (tks jwhitlark)

## 0.2.4 - 2018-11-16
### Changed
* Internal refactoring - using new namespaces
* Created a dummy `platform.cljs` and `optionals.cljs` file so analysis does not break in CLJS.


## 0.2.0 - 2018-11-15
## Changed
* Fixes #48 / #22 - Orchestra is not a mandatory dependecy anymore.


## 0.1.19 - 2018-11-05
## Changed
* Fixes #46 - Adding JVM shutdown hook


## 0.1.18 - 2018-11-04
### Changed
* Fixes #45 - Overridable help text generation (tks ty-i3) 


## 0.1.17 - 2018-11-01
### Changed
* Fixes #41 - Use expound for sane error messages with spec
* Fix #43 - Lots of useless output in 0.1.16
* Fixes #38 - Use tools.cli v 0.4.1


## 0.1.16 - 2018-10-29
### Changed
- Added Spec support for all options, and for the subcommand at once.
- Added keywords as a parse type (tks jwhitlark)


## 0.1.15 - 2018-07-31
### Changed
- Added EDN support (tks jwhitlark)
- Added optional YAML parsing, but using it breaks Graal (tks jwhitlark)
