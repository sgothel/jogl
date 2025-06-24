#! /bin/bash

#export DYLD_LIBRARY_PATH=$HOME/ffmpeg-2.2.3/lib:$DYLD_LIBRARY_PATH
#export DYLD_LIBRARY_PATH=$HOME/ffmpeg-2.8/lib:$DYLD_LIBRARY_PATH
export DYLD_LIBRARY_PATH=/usr/local/Cellar/ffmpeg/6.0/lib:$DYLD_LIBRARY_PATH

#JAVA_HOME=`/usr/libexec/java_home`
#JAVA_HOME=`/usr/libexec/java_home -version 1.8`
#JAVA_HOME=`/usr/libexec/java_home -version 11`
JAVA_HOME=`/usr/libexec/java_home -version 21`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

export SWT_CLASSPATH=`pwd`/lib/swt/cocoa-macosx-x86_64/swt.jar

spath=`dirname $0`

. $spath/tests.sh  $JAVA_HOME/bin/java -DummyArg ../build-macosx $*

