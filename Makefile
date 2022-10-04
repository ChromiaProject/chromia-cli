SHELL = /bin/sh

run:
	mvn exec:java -Dexec

help:
	mvn exec:java  -Dexec.args="database --help"

test:
	mvn package exec:java  -Dexec.args="rell run-test"