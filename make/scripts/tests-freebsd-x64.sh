#! /bin/bash

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh
fi

#J2RE_HOME=/usr/local/openjdk21
#JAVA_HOME=/usr/local/openjdk21
#export JAVA_HOME PATH

JAVA_CMD=`which java`

. $SDIR/tests.sh  $JAVA_CMD -DummyArg ../build-freebsd-x86_64 $*

