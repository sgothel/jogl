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
    if [ -e /usr/local/android-ndk-r7 ] ; then
        NDK_ROOT=/usr/local/android-ndk-r7
    elif [ -e /opt-linux-x86/android-ndk-r7 ] ; then
        NDK_ROOT=/opt-linux-x86/android-ndk-r7
    elif [ -e /opt/android-ndk-r7 ] ; then
        NDK_ROOT=/opt/android-ndk-r7
    else 
        echo NDK_ROOT is not specified and does not exist in default locations
        exit 1
    fi
elif [ ! -e $NDK_ROOT ] ; then
    echo NDK_ROOT $NDK_ROOT does not exist
    exit 1
fi
export NDK_ROOT

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

export GCC_VERSION=4.4.3
HOST_ARCH=linux-x86
export TARGET_ARCH=arm-linux-androideabi
export TARGET_TRIPLE=arm-linux-androideabi

export NDK_TOOLCHAIN_ROOT=$NDK_ROOT/toolchains/${TARGET_ARCH}-${GCC_VERSION}/prebuilt/${HOST_ARCH}
export TARGET_PLATFORM_ROOT=${NDK_ROOT}/platforms/android-${ANDROID_VERSION}/arch-arm

# Need to add toolchain bins to the PATH. 
export PATH="$NDK_TOOLCHAIN_ROOT/$TARGET_ARCH/bin:$ANDROID_SDK_HOME/platform-tools:$PATH"

which gcc 2>&1 | tee make.jogl.all.android-armv7-cross.log

ant \
    -Dgluegen-cpptasks.file=`pwd`/../../gluegen/make/lib/gluegen-cpptasks-android-armv7.xml \
    -Drootrel.build=build-android-armv7 \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisAndroid=true \
    -DisAndroidARMv7=true \
    -DjvmDataModel.arg="-Djnlp.no.jvm.data.model.set=true" \
    -DisCrosscompilation=true \
    -Dandroid.abi=armeabi-v7a \
    \
    $* 2>&1 | tee -a make.jogl.all.android-armv7-cross.log


