#! /bin/bash

export HOST_UID=jogamp
# jogamp02 - 10.1.0.122
#export HOST_IP=10.1.0.122
export HOST_IP=10.1.0.52
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
#export TARGET_IP=panda02
export TARGET_IP=jautab01
export TARGET_ADB_PORT=5555
export TARGET_ROOT=/data/projects

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

LOGFILE=`basename $0 .sh`.log

RSYNC_EXCLUDES="--delete-excluded \
                --exclude 'build-x86*/' --exclude 'build-linux*/' --exclude 'build-win*/' --exclude 'build-mac*/' \
                --exclude 'classes/' --exclude 'src/' --exclude '.git/' --exclude '*-java-src.zip' \
                --exclude 'gensrc/' --exclude 'doc/' --exclude 'jnlp-files' --exclude 'archive/' \
                --exclude 'android-sdk/' --exclude 'resources/' --exclude 'scripts/' \
                --exclude 'stub_includes/' --exclude 'nbproject/' --exclude '*.log' --exclude '*.zip' --exclude '*.7z' \
                --exclude 'make/lib/external/'"

echo "#! /system/bin/sh" > $BUILD_DIR/jogl-targetcommand.sh

echo "\
rsync -av --delete --delete-after $RSYNC_EXCLUDES \
   $HOST_UID@$HOST_IP::$HOST_RSYNC_ROOT/gluegen \
   $HOST_UID@$HOST_IP::$HOST_RSYNC_ROOT/jogl \
   $TARGET_ROOT ; \
cd $TARGET_ROOT/jogl/make ;
export LD_LIBRARY_PATH=/system/lib:$TARGET_ROOT/gluegen/make/$BUILD_DIR/obj:$TARGET_ROOT/jogl/make/$BUILD_DIR/lib ; \
# export BOOTCLASSPATH=/system/framework/core.jar:/system/framework/bouncycastle.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar ;
setprop log.redirect-stdio true ; setprop log.redirect-stderr true ; \
am start -a android.intent.action.MAIN -n jogamp.android.launcher/jogamp.android.launcher.MainLauncher -d launch://jogamp.org/$TSTCLASS/?pkg=com.jogamp.opengl.test\&newt.debug=all\&jogl.debug=all\&nativewindow.debug=all \
# \
#dalvikvm \
#  -Xjnigreflimit:2000 \
#  -cp ../../gluegen/make/$BUILD_DIR/jogamp.android-launcher.apk:../../gluegen/make/lib/ant-junit-all.apk:../../gluegen/make/$BUILD_DIR/gluegen-rt-android-armeabi.apk:$BUILD_DIR/jar/jogl.all-android-armeabi.apk:$BUILD_DIR/jar/jogl.test.apk \
#  -Dgluegen.root=../../gluegen \
#  -Drootrel.build=build-android-armv6 \
#  com.android.internal.util.WithFramework \
#  $TSTCLASS \
" >> $BUILD_DIR/jogl-targetcommand.sh


chmod ugo+x $BUILD_DIR/jogl-targetcommand.sh
adb connect $TARGET_IP:$TARGET_ADB_PORT
adb -s $TARGET_IP:$TARGET_ADB_PORT push $BUILD_DIR/jogl-targetcommand.sh $TARGET_ROOT/jogl-targetcommand.sh
adb -s $TARGET_IP:$TARGET_ADB_PORT shell su -c $TARGET_ROOT/jogl-targetcommand.sh 2>&1 | tee $LOGFILE


