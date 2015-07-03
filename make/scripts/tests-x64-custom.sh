#! /bin/bash

SDIR=`dirname $0` 

#export LD_LIBRARY_PATH=$HOME/libav/lib:$LD_LIBRARY_PATH

#if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh ] ; then
#    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh
#fi

. $SDIR/tests.sh  `which java` -d64 ../build-x86_64 $*

