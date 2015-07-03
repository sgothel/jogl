#! /bin/bash

export JAVA7_EXE=`/usr/libexec/java_home -version 1.7.0_12`/bin/java

SDIR=`dirname $0` 

. $SDIR/make.jogl.all.macosx.sh -f build-test.xml junit.run.settings junit.run.local.java7

