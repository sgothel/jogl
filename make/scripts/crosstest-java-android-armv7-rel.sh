#! /bin/bash

export HOST_UID=sven
export HOST_IP=192.168.0.52
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
export TARGET_IP=beagle02
export TARGET_ROOT=/projects

export BUILD_DIR=../build-android-armv7

if [ -e /opt-linux-x86/android-sdk-linux_x86 ] ; then
    export ANDROID_SDK_HOME=/opt-linux-x86/android-sdk-linux_x86
    export PATH=$ANDROID_SDK_HOME/platform-tools:$PATH
fi 

#
# orig android:
#   export LD_LIBRARY_PATH /system/lib
#   export BOOTCLASSPATH /system/framework/core.jar:/system/framework/bouncycastle.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar:/system/framework/core-junit.jar
#

#TSTCLASS=com.jogamp.nativewindow.NativeWindowVersion
#TSTCLASS=com.jogamp.opengl.JoglVersion
#TSTCLASS=com.jogamp.newt.NewtVersion
TSTCLASS=com.jogamp.newt.opengl.GLWindow
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen01GLPBufferNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLSimple01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState02NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestRulerNEWT01
#TSTCLASS=com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo01
#TSTCLASS=com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo02
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.gl2es1.gears.newt.TestGearsGL2ES1NEWT


LOGFILE=`basename $0 .sh`.log

#  -Djava.class.path=lib/junit.jar:/usr/share/ant/lib/ant.jar:/usr/share/ant/lib/ant-junit.jar:$BUILD_DIR/gluegen.jar:$BUILD_DIR/test/build/gluegen-test.jar \
#  -Djava.class.path=lib/ant-junit-all.apk:$BUILD_DIR/gluegen-rt.apk \
#  -Djava.library.path=/system/lib:$TARGET_ROOT/gluegen/make/$BUILD_DIR/obj:$BUILD_DIR/test/build/natives \

RSYNC_EXCLUDES="--exclude 'build-x86*/' --exclude 'build-linux*/' --exclude 'build-win*/' --exclude 'build-mac*/' \
                --exclude 'classes/' --exclude 'src/' --exclude '.git/' --exclude 'jogl-java-src.zip' \
                --delete-excluded"

echo "#! /system/bin/sh" > $BUILD_DIR/targetcommand.sh

echo "\
rsync -av --delete --delete-after $RSYNC_EXCLUDES $HOST_UID@$HOST_IP::$HOST_RSYNC_ROOT/gluegen $HOST_UID@$HOST_IP::$HOST_RSYNC_ROOT/jogl $TARGET_ROOT ; \
cd $TARGET_ROOT/jogl/make ;
export LD_LIBRARY_PATH=/system/lib:$TARGET_ROOT/gluegen/make/$BUILD_DIR/obj:$TARGET_ROOT/jogl/make/$BUILD_DIR/lib ; \
export BOOTCLASSPATH=/system/framework/core.jar:/system/framework/bouncycastle.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar ; \
dalvikvm \
  -Xjnigreflimit:2000 \
  -cp ../../gluegen/make/lib/ant-junit-all.apk:../../gluegen/make/$BUILD_DIR/gluegen-rt.apk:$BUILD_DIR/jar/jogl.all-android.apk:$BUILD_DIR/jar/jogl.test.jar \
  -Djogamp.debug.JNILibLoader=true \
  -Djogamp.debug.NativeLibrary=true \
  -Djogamp.debug.NativeLibrary.Lookup=true \
  -Djogl.debug=all \
  com.android.internal.util.WithFramework \
  $TSTCLASS \
" >> $BUILD_DIR/targetcommand.sh

chmod ugo+x $BUILD_DIR/targetcommand.sh
adb push $BUILD_DIR/targetcommand.sh $TARGET_ROOT/targetcommand.sh
adb shell $TARGET_ROOT/targetcommand.sh 2>&1 | tee $LOGFILE

