#! /bin/bash

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86.sh
fi

export SWT_CLASSPATH=`pwd`/lib/swt/gtk-linux-x86/swt.jar

. $SDIR/tests.sh  `which java` -DummyArg ../build-x86 $*


