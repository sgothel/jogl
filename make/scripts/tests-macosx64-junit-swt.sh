#! /bin/bash

SDIR=`dirname $0` 

. $SDIR/make.jogl.all.macosx.sh -f build-test.xml junit.run.settings junit.run.swt.awt


