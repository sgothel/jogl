#! /bin/bash

export HOST_UID=jogamp
# jogamp02 - 10.1.0.122
export HOST_IP=10.1.0.122
#export HOST_IP=10.1.0.52
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
#export TARGET_IP=panda02
export TARGET_IP=jautab03
export TARGET_ADB_PORT=5555
export TARGET_ROOT=jogamp-test

export BUILD_DIR=../build-android-armv6

if [ -e /opt-linux-x86/android-sdk-linux_x86 ] ; then
    export ANDROID_HOME=/opt-linux-x86/android-sdk-linux_x86
    export PATH=$ANDROID_HOME/platform-tools:$PATH
fi 

#TSTCLASS=jogamp.android.launcher.LauncherUtil
#TSTCLASS=com.jogamp.opengl.test.android.LauncherUtil
#TSTCLASS=com.jogamp.android.launcher.NEWTLauncherMain
#TSTCLASS=com.jogamp.nativewindow.NativeWindowVersion
#TSTCLASS=com.jogamp.opengl.JoglVersion
#TSTCLASS=com.jogamp.newt.NewtVersion
#TSTCLASS=com.jogamp.newt.opengl.GLWindow
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen01GLPBufferNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLSimple01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState02NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestRulerNEWT01
#TSTCLASS=com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo01
#TSTCLASS=com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo02
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.gl2es1.gears.newt.TestGearsGL2ES1NEWT
TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT

#D_FLAGS="\&newt.debug=all\&jogl.debug=all\&nativewindow.debug=all"
#D_FLAGS="\&newt.debug=all\&jogl.debug=all"
D_FLAGS="\&newt.debug=all"

#Screen: 1280 x 752
#M_FLAGS="\&arg=-time\&arg=100000\&arg=-width\&arg=1280\&arg=-height\&arg=752"
M_FLAGS="\&arg=-time\&arg=100000\&arg=-fullscreen"

LOGFILE=`basename $0 .sh`.log

#adb -s $TARGET_IP:$TARGET_ADB_PORT uninstall jogamp.android.launcher
#adb -s $TARGET_IP:$TARGET_ADB_PORT uninstall com.jogamp.common
#adb -s $TARGET_IP:$TARGET_ADB_PORT install $BUILD_DIR/jogamp-android-launcher.apk
#adb -s $TARGET_IP:$TARGET_ADB_PORT install $BUILD_DIR/gluegen-rt-android-armeabi.apk

#adb -s $TARGET_IP:$TARGET_ADB_PORT uninstall com.jogamp.opengl
#adb -s $TARGET_IP:$TARGET_ADB_PORT install $BUILD_DIR/jar/jogl-all-android-armeabi.apk

#adb -s $TARGET_IP:$TARGET_ADB_PORT uninstall com.jogamp.opengl.test
#adb -s $TARGET_IP:$TARGET_ADB_PORT install $BUILD_DIR/jar/jogl-test-android.apk

SHELL_CMD="\
cd /sdcard ; \
if [ -e $TARGET_ROOT ] ; then rm -r $TARGET_ROOT ; fi ; \
mkdir $TARGET_ROOT ; cd $TARGET_ROOT ; \
setprop log.redirect-stdio true ; setprop log.redirect-stderr true ; \
am kill-all ; \
am start -W -S -a android.intent.action.MAIN -n jogamp.android.launcher/jogamp.android.launcher.MainLauncher -d launch://jogamp.org/$TSTCLASS/?pkg=com.jogamp.opengl.test$D_FLAGS$M_FLAGS \
"

adb connect $TARGET_IP:$TARGET_ADB_PORT
adb -s $TARGET_IP:$TARGET_ADB_PORT logcat -c
adb -s $TARGET_IP:$TARGET_ADB_PORT shell $SHELL_CMD 2>&1 | tee $LOGFILE
adb -s $TARGET_IP:$TARGET_ADB_PORT logcat -d 2>&1 | tee -a $LOGFILE

