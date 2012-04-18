#! /bin/sh

# arm-linux-gnueabi == armel triplet
PATH=`pwd`/../../gluegen/make/lib/linux/arm-linux-gnueabi/bin:$PATH
export PATH

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxARMv7=true \
#    -DisX11=false \

export TARGET_PLATFORM_ROOT=/
export TARGET_PLATFORM_LIBS=/usr/lib/arm-linux-gnueabi
export TARGET_JAVA_LIBS=/usr/lib/jvm/default-java/jre/lib/arm

export GLUEGEN_CPPTASKS_FILE="../../gluegen/make/lib/gluegen-cpptasks-linux-armv4.xml"

ant \
    -Drootrel.build=build-linux-armv7 \
    -Dsetup.addNativeKD=true \
    -Dsetup.addNativeOpenMAX=true \
    -Dsetup.addNativeBroadcomEGL=true \
    -Djunit.run.arg0="-Dnewt.test.Screen.disableScreenMode" \
    $* 2>&1 | tee make.jogl.all.linux-armv7.log

