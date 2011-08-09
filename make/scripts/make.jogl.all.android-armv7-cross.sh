#! /bin/sh

export HOST_UID=sven
export HOST_IP=192.168.0.52
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
export TARGET_IP=beagle01
export TARGET_ROOT=/projects
export TARGET_ANT_HOME=/usr/share/ant

export ANDROID_VERSION=9

echo ANDROID_SDK_HOME $ANDROID_SDK_HOME
echo NDK_ROOT $NDK_ROOT

if [ -z "$NDK_ROOT" ] ; then
    if [ -e /usr/local/android-ndk-r6 ] ; then
        NDK_ROOT=/usr/local/android-ndk-r6
    elif [ -e /opt-linux-x86/android-ndk-r6 ] ; then
        NDK_ROOT=/opt-linux-x86/android-ndk-r6
    elif [ -e /opt/android-ndk-r6 ] ; then
        NDK_ROOT=/opt/android-ndk-r6
    else 
        echo NDK_ROOT is not specified and does not exist in default locations
        exit 1
    fi
elif [ ! -e $NDK_ROOT ] ; then
    echo NDK_ROOT $NDK_ROOT does not exist
    exit 1
fi
export NDK_ROOT
NDK_TOOLCHAIN=$NDK_ROOT/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/arm-linux-androideabi

if [ -z "$ANDROID_SDK_HOME" ] ; then
    if [ -e /usr/local/android-sdk-linux_x86 ] ; then
        ANDROID_SDK_HOME=/usr/local/android-sdk-linux_x86
    elif [ -e /opt-linux-x86/android-sdk-linux_x86 ] ; then
        ANDROID_SDK_HOME=/opt-linux-x86/android-sdk-linux_x86
    elif [ -e /opt/android-sdk-linux_x86 ] ; then
        ANDROID_SDK_HOME=/opt/android-sdk-linux_x86
    else 
        echo ANDROID_SDK_HOME is not specified and does not exist in default locations
        exit 1
    fi
elif [ ! -e $ANDROID_SDK_HOME ] ; then
    echo ANDROID_SDK_HOME $ANDROID_SDK_HOME does not exist
    exit 1
fi
export ANDROID_SDK_HOME

export PATH="$NDK_TOOLCHAIN/bin:$ANDROID_SDK_HOME/platform-tools:$PATH"

export GCC_VERSION=4.4.3
HOST_ARCH=linux-x86
export TARGET_ARCH=arm-linux-androideabi
# mcpu: cortex-a8', `cortex-a9', `cortex-r4', `cortex-r4f', `cortex-m3', `cortex-m1', `xscale', `iwmmxt', `iwmmxt2', `ep9312'. 
export TARGET_CPU_NAME=armv7-a
TARGET_CPU_TUNE=armv7-a
# mfpu: `vfp', `vfpv3', `vfpv3-d16' and `neon'
TARGET_FPU_NAME=vfpv3
TARGET_FPU_ABI=softfp

export TARGET_TOOL_PATH=${NDK_ROOT}/toolchains/${TARGET_ARCH}-${GCC_VERSION}/prebuilt/${HOST_ARCH}

export TARGET_OS_PATH=${NDK_ROOT}/platforms/android-${ANDROID_VERSION}/arch-arm/usr
export TARGET_PLATFORM_LIBS=${TARGET_OS_PATH}/lib
export HOST_OS_PATH=${NDK_ROOT}/platforms/android-${ANDROID_VERSION}/arch-x86/usr

export NDK_XBIN_PATH=${TARGET_TOOL_PATH}/bin
export NDK_BIN_PATH=${TARGET_TOOL_PATH}/${TARGET_ARCH}/bin

export NDK_GCC=${NDK_XBIN_PATH}/${TARGET_ARCH}-gcc
export NDK_AR=${NDK_XBIN_PATH}/${TARGET_ARCH}-ar
export NDK_STRIP=${NDK_XBIN_PATH}/${TARGET_ARCH}-strip
export NDK_READELF=${NDK_XBIN_PATH}/${TARGET_ARCH}-readelf

export PATH=${NDK_XBIN_PATH}:$PATH

export NDK_CFLAGS="\
-march=${TARGET_CPU_NAME} \
-fpic \
-DANDROID \
"

export NDK_LDFLAGS="\
-Wl,--demangle \
-nostdlib -Bdynamic -Wl,-dynamic-linker,/system/bin/linker -Wl,--gc-sections -Wl,-z,nocopyreloc \
${TARGET_OS_PATH}/lib/libc.so \
${TARGET_OS_PATH}/lib/libstdc++.so \
${TARGET_OS_PATH}/lib/libm.so \
${TARGET_OS_PATH}/lib/crtbegin_dynamic.o \
-Wl,--no-undefined -Wl,-rpath-link=${TARGET_OS_PATH}/lib \
${TARGET_TOOL_PATH}/lib/gcc/${TARGET_ARCH}/${GCC_VERSION}/${TARGET_CPU_NAME}/libgcc.a \
${TARGET_OS_PATH}/lib/crtend_android.o \
"


which gcc 2>&1 | tee make.jogl.all.android-armv7-cross.log

ant \
    -Dgluegen-cpptasks.file=`pwd`/../../gluegen/make/lib/gluegen-cpptasks-android-armv7.xml \
    -Drootrel.build=build-android-armv7 \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisAndroid=true \
    -DisAndroidARMv7=true \
    -DisCrosscompilation=true \
    \
    $* 2>&1 | tee -a make.jogl.all.android-armv7-cross.log


