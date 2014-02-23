#! /bin/bash

SDIR=`dirname $0` 

. $SDIR/make.jogl.all.linux-x86_64.sh -f build-test.xml junit.run.settings junit.run.swt.awt


