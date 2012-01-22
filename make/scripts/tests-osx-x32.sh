#! /bin/bash

spath=`dirname $0`

. $spath/tests.sh /usr/bin/java -d32 ../build-macosx $*


