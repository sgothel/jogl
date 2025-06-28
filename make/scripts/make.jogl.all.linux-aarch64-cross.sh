#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh
fi

# aarch64-linux-gnueabi == aarch64 triplet
PATH=`pwd`/../../gluegen/make/lib/toolchain/aarch64-linux-gnueabi/bin:$PATH
export PATH

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxARM64=true \
#    -DisX11=true \

export TARGET_PLATFORM_SYSROOT=`gcc --print-sysroot`
export TARGET_PLATFORM_USRROOT=/opt-linux-arm64
export TARGET_PLATFORM_USRLIBS=$TARGET_PLATFORM_USRROOT/usr/lib
export TARGET_JAVA_LIBS=$TARGET_PLATFORM_USRROOT/jre/lib/aarch64

export GLUEGEN_CPPTASKS_FILE="../../gluegen/make/lib/gluegen-cpptasks-linux-aarch64.xml"

#export JUNIT_DISABLED="true"
#export JUNIT_RUN_ARG0="-Dnewt.test.Screen.disableScreenMode"

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-linux-aarch64 \
    $* 2>&1 | tee make.jogl.all.linux-aarch64-cross.log

