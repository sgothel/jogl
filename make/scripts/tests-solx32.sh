#! /bin/bash

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86.sh
fi

. $SDIR/tests.sh  `which java` -d32 ../build-solaris-x86 $*

