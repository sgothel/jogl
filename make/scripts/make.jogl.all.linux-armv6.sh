#! /bin/sh

# arm-linux-gnueabi == armel triplet
PATH=`pwd`/../../gluegen/make/lib/toolchain/armsf-linux-gnueabi/bin:$PATH
export PATH

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxARMv6=true \
#    -DisX11=false \

export TARGET_PLATFORM_ROOT=/
export TARGET_PLATFORM_LIBS=/usr/lib/arm-linux-gnueabi
export TARGET_JAVA_LIBS=/usr/lib/jvm/default-java/jre/lib/arm

export GLUEGEN_CPPTASKS_FILE="../../gluegen/make/lib/gluegen-cpptasks-linux-armv6.xml"

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-linux-armv6 \
    -Dsetup.addNativeKD=true \
    -Dsetup.addNativeOpenMAX=true \
    -Dsetup.addNativeBroadcom=true \
    -Djunit.run.arg0="-Dnewt.test.Screen.disableScreenMode" \
    $* 2>&1 | tee make.jogl.all.linux-armv6.log

