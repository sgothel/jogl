#! /bin/bash

export DYLD_LIBRARY_PATH=/usr/local/libav:$DYLD_LIBRARY_PATH

export SWT_CLASSPATH=`pwd`/lib/swt/cocoa-macosx-x86_64/swt.jar

spath=`dirname $0`

. $spath/tests.sh  "`which java`" -d64 ../build-macosx $*


