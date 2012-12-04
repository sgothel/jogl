#! /bin/bash

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86.sh
fi

export SWT_CLASSPATH=`pwd`/lib/swt/gtk-linux-x86/swt-debug.jar

. $SDIR/tests.sh  `which java` -d32 ../build-x86 $*


