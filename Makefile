.PHONY: compile format-check test package it

compile:
	sbt compile

format-check: compile
	sbt scalafmtCheckAll

test: compile
	sbt test

package: compile
	sbt assembly

it: package
	bash ./it.sh
