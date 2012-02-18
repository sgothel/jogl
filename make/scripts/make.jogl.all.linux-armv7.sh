#! /bin/sh

PATH=`pwd`/../../gluegen/make/lib/linux/arm-linux-gnueabi/bin:$PATH
export PATH

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxARMv7=true \
#    -DisX11=false \

export TARGET_PLATFORM_LIBS=/usr/lib/arm-linux-gnueabi
export TARGET_JAVA_LIBS=/usr/lib/jvm/default-java/jre/lib/arm

ant \
    -Drootrel.build=build-linux-armv7 \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxARMv7=true \
    -DisX11=true \
    \
    -Dsetup.addNativeKD=true \
    -Dsetup.addNativeOpenMAX=true \
    -Dsetup.addNativeBroadcomEGL=true \
    $* 2>&1 | tee make.jogl.all.linux-armv7-cross.log

