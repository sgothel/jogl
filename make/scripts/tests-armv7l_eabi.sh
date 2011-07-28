#! /bin/bash

spath=`dirname $0`

. $spath/tests.sh  `which java` ../build-armv7l_eabi $*


