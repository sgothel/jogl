#! /bin/bash

SDIR=`dirname $0` 

#export LD_LIBRARY_PATH=/home/sven/libav-0.8/lib:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=/home/sven/ffmpeg-0.10/lib:$LD_LIBRARY_PATH

#export LD_LIBRARY_PATH=/home/sven/libav-9.x/lib:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=/home/sven/ffmpeg-1.2/lib:$LD_LIBRARY_PATH

#export LD_LIBRARY_PATH=/home/sven/libav-10.x/lib:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=/home/sven/ffmpeg-2.x/lib:$LD_LIBRARY_PATH

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh
fi

. $SDIR/tests.sh  `which java` -d64 ../build-x86_64 $*

