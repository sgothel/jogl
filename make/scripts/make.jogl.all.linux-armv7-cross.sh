#! /bin/sh

PATH=`pwd`/../../gluegen/make/lib/linux-x86_64/arm-linux-gnueabi/bin:$PATH
export PATH

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxARMv7=true \
#    -DisX11=false \

export HOST_UID=sven
export HOST_IP=192.168.0.52
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
export TARGET_IP=beagle01
export TARGET_ROOT=/home/jogamp/projects-cross
export TARGET_ANT_HOME=/usr/share/ant

ant \
    -Drootrel.build=build-linux-armv7 \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxARMv7=true \
    -DisX11=true \
    -DisCrosscompilation=true \
    \
    -DuseKD=true \
    -DuseOpenMAX=true \
    -DuseBroadcomEGL=true \
    $* 2>&1 | tee make.jogl.all.linux-armv7-cross.log




