#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh
fi

# arm-linux-gnueabi == armel triplet
PATH=`pwd`/../../gluegen/make/lib/linux/arm-linux-gnueabi/bin:$PATH
export PATH

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxARMv7=true \
#    -DisX11=false \

export NODE_LABEL=.

export HOST_UID=jogamp
export HOST_IP=jogamp02
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
export TARGET_IP=panda01
#export TARGET_IP=jautab02
export TARGET_ROOT=/home/jogamp/projects-cross
export TARGET_ANT_HOME=/usr/share/ant

export TARGET_PLATFORM_ROOT=/opt-linux-armv7-armel
export TARGET_PLATFORM_LIBS=$TARGET_PLATFORM_ROOT/usr/lib
export TARGET_JAVA_LIBS=/opt-linux-armv7-armel/jre/lib/arm

export GLUEGEN_CPPTASKS_FILE="../../gluegen/make/lib/gluegen-cpptasks-linux-armv7.xml"

#export JUNIT_DISABLED="true"
export JUNIT_RUN_ARG0="-Dnewt.test.Screen.disableScreenMode"

ant \
    -Drootrel.build=build-linux-armv7 \
    \
    -Dsetup.addNativeKD=true \
    -Dsetup.addNativeOpenMAX=true \
    -Dsetup.addNativeBroadcomEGL=true \
    $* 2>&1 | tee make.jogl.all.linux-armv7-cross.log




