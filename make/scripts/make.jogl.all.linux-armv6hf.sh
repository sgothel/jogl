#! /bin/sh

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxARMv6=true \
#    -DisX11=false \

export TARGET_PLATFORM_USRROOT=
export TARGET_PLATFORM_USRLIBS=$TARGET_PLATFORM_USRROOT/usr/lib/arm-linux-gnueabihf
export TARGET_JAVA_LIBS=$TARGET_PLATFORM_USRROOT/usr/lib/jvm/java-8-openjdk-armhf/jre/lib/arm

export GLUEGEN_CPPTASKS_FILE="../../gluegen/make/lib/gluegen-cpptasks-linux-armv6hf-ontarget.xml"

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-linux-armv6hf \
    -Dsetup.addNativeKD=true \
    -Dsetup.addNativeOpenMAX=true \
    -Dsetup.addNativeBroadcom=true \
    -Djunit.run.arg0="-Dnewt.test.Screen.disableScreenMode" \
    $* 2>&1 | tee make.jogl.all.linux-armv6hf.log

