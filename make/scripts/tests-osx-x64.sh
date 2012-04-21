#! /bin/bash

export DYLD_LIBRARY_PATH=/usr/local/lib:$DYLD_LIBRARY_PATH

spath=`dirname $0`

. $spath/tests.sh  /usr/bin/java -d64 ../build-macosx $*


