#! /bin/sh

# arm-linux-gnueabihf == armhf triplet
PATH=`pwd`/../../gluegen/make/lib/linux/arm-linux-gnueabihf/bin:$PATH
export PATH

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxARMv7=true \
#    -DisX11=false \

export TARGET_PLATFORM_ROOT=/
export TARGET_PLATFORM_LIBS=/usr/lib/arm-linux-gnueabihf
export TARGET_JAVA_LIBS=/usr/lib/jvm/java-6-openjdk-armhf/jre/lib/arm

export GLUEGEN_CPPTASKS_FILE="../../gluegen/make/lib/gluegen-cpptasks-linux-armv6hf.xml"

ant \
    -Drootrel.build=build-linux-armv7hf \
    -Dsetup.addNativeKD=true \
    -Dsetup.addNativeOpenMAX=true \
    -Dsetup.addNativeBroadcomEGL=true \
    -Djunit.run.arg0="-Dnewt.test.Screen.disableScreenMode" \
    $* 2>&1 | tee make.jogl.all.linux-armv7hf.log

