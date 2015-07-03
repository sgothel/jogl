#! /bin/bash

export DYLD_LIBRARY_PATH=/usr/local/libav:$DYLD_LIBRARY_PATH

JAVA_HOME=`/usr/libexec/java_home -version 1.6`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

spath=`dirname $0`

. $spath/tests.sh  /usr/bin/java -d64 ../build-macosx $*

