#! /bin/bash

#export DYLD_LIBRARY_PATH=$HOME/ffmpeg-0.8_0.9/lib:$DYLD_LIBRARY_PATH
#export DYLD_LIBRARY_PATH=$HOME/ffmpeg-2.2.3/lib:$DYLD_LIBRARY_PATH
export DYLD_LIBRARY_PATH=/usr/local/Cellar/ffmpeg/2.8/lib:$DYLD_LIBRARY_PATH

JAVA_HOME=`/usr/libexec/java_home -version 1.8`
#JAVA_HOME=`/usr/libexec/java_home -version 1.7`
#JAVA_HOME=`/usr/libexec/java_home -version 1.7.0_25`
#JAVA_HOME=`/usr/libexec/java_home -version 1.6.0`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

spath=`dirname $0`

. $spath/tests.sh  $JAVA_HOME/bin/java -d64 ../build-macosx $*

