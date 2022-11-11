SHELL = /bin/sh

run:
	mvn exec:java -Dexec

help:
	mvn exec:java  -Dexec.args="database --help"

test:
	chr.sh rell run-test -r sample/test -tm SimpleTests
     #chr.sh rell run-test -r sample -tm test.SimpleTests
