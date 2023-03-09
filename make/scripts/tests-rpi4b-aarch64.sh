#! /bin/bash

SDIR=`dirname $0` 

#JAVA_CMD=/opt-linux-arm64/zulu19.32.13-ca-jdk19.0.2-linux_aarch64/bin/java
#JAVA_CMD=/usr/lib/jvm/java-11-openjdk-arm64/bin/java
JAVA_CMD=`which java`

D2_ARGS="-Dnewt.ws.mmwidth=150 -Dnewt.ws.mmheight=90"

. $SDIR/tests.sh  $JAVA_CMD -DummyArg ../build-linux-aarch64 $*


