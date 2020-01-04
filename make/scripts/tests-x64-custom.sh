#! /bin/bash

SDIR=`dirname $0` 

#export LD_LIBRARY_PATH=$HOME/libav/lib:$LD_LIBRARY_PATH

#if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh ] ; then
#    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh
#fi

. $SDIR/tests.sh  `which java` -DummyArg ../build-x86_64 $*

