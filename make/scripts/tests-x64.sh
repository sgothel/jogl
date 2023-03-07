#! /bin/bash

#set -x 

SDIR=`dirname $0` 

#export LD_LIBRARY_PATH=/opt-linux-x86_64/ffmpeg-4.3/lib:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=/opt-linux-x86_64/ffmpeg-5.1/lib:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=/opt-linux-x86_64/ffmpeg-6.0/lib:$LD_LIBRARY_PATH

#J2RE_HOME=/opt-linux-x86_64/jre1.7.0_45
#JAVA_HOME=/opt-linux-x86_64/jdk1.7.0_45
#J2RE_HOME=/opt-linux-x86_64/jre7
#JAVA_HOME=/opt-linux-x86_64/j2se7
#export J2RE_HOME JAVA_HOME

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh
fi

export SWT_CLASSPATH=`pwd`/lib/swt/gtk-linux-x86_64/swt.jar
#export SWT_CLASSPATH=/usr/local/projects/JOGL/SWT/swt-4.3.0/gtk-linux-x86_64/swt-debug.jar

#JAVA_CMD=/opt-linux-x86_64/zulu19.32.13-ca-jdk19.0.2-linux_x64/bin/java
JAVA_CMD=`which java`

. $SDIR/tests.sh  $JAVA_CMD -DummyArg ../build-x86_64 $*

