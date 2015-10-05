#! /bin/bash

SDIR=`dirname $0` 

#export LD_LIBRARY_PATH=$HOME/libav-0.8/lib:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=$HOME/ffmpeg-0.10/lib:$LD_LIBRARY_PATH

#export LD_LIBRARY_PATH=$HOME/libav-9.x/lib:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=$HOME/ffmpeg-1.2/lib:$LD_LIBRARY_PATH

#export LD_LIBRARY_PATH=$HOME/libav-10.1/lib:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=$HOME/ffmpeg-2.2.2/lib:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=$HOME/ffmpeg-2.2.3/lib:$LD_LIBRARY_PATH

#export LD_LIBRARY_PATH=$HOME/libav-11.2/lib:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=$HOME/ffmpeg-2.5/lib:$LD_LIBRARY_PATH

#export LD_LIBRARY_PATH=$HOME/ffmpeg-2.8/lib:$LD_LIBRARY_PATH

#J2RE_HOME=/opt-linux-x86_64/jre1.7.0_45
#JAVA_HOME=/opt-linux-x86_64/jdk1.7.0_45
#J2RE_HOME=/opt-linux-x86_64/jre7
#JAVA_HOME=/opt-linux-x86_64/j2se7
#export J2RE_HOME JAVA_HOME

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh
fi

. $SDIR/tests.sh  `which java` -d64 ../build-x86_64 $*

