#! /bin/bash

spath=`dirname $0`

. $spath/tests.sh  `which java` -DummyArg ../build-linux-aarch64 $*
#. $spath/tests.sh  /usr/lib/jvm/java-11-openjdk-arm64/bin/java -DummyArg ../build-linux-aarch64 $*


