export HOST_UID=sven
export HOST_IP=192.168.0.52
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
export TARGET_IP=beagle01
export TARGET_ROOT=projects-cross

export BUILD_DIR=../build-linux-armv7
export ANT_PATH=/usr/share/ant

#TSTCLASS=com.jogamp.nativewindow.NativeWindowVersion
#TSTCLASS=com.jogamp.opengl.JoglVersion
#TSTCLASS=com.jogamp.newt.NewtVersion
#TSTCLASS=com.jogamp.newt.opengl.GLWindow
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen01GLPBufferNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLSimple01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState02NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestRulerNEWT01
TSTCLASS=com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo01
#TSTCLASS=com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo02

LOGFILE=`basename $0 .sh`.log

#  -Djogamp.debug.NativeLibrary.Lookup=true \
#  -Djogamp.debug.ProcAddressHelper=true \

CP_BASE=../../gluegen/make/lib/junit.jar:$ANT_PATH/lib/ant.jar:$ANT_PATH/lib/ant-junit.jar:../../gluegen/make/$BUILD_DIR/gluegen.jar
CP_JOGL_ALL=$BUILD_DIR/jar/nativewindow.all.jar:$BUILD_DIR/jar/jogl.all.jar:$BUILD_DIR/jar/newt.all.jar:$BUILD_DIR/jar/jogl.test.jar
#CP_JOGL_EGLES=$BUILD_DIR/jar/nativewindow.all-noawt.jar:$BUILD_DIR/jar/jogl.core.jar:$BUILD_DIR/jar/jogl.util.jar:$BUILD_DIR/jar/jogl.gles1.jar:$BUILD_DIR/jar/jogl.gles1.dbg.jar:$BUILD_DIR/jar/jogl.gles2.jar:$BUILD_DIR/jar/jogl.gles2.dbg.jar:$BUILD_DIR/jar/jogl.egl.jar:$BUILD_DIR/jar/jogl.util.fixedfuncemu.jar:$BUILD_DIR/jar/jogl.glu.tess.jar:$BUILD_DIR/jar/jogl.glu.mipmap.jar:$BUILD_DIR/jar/newt.all-noawt.jar:$BUILD_DIR/jar/jogl.test.jar
CP_JOGL_EGLES=$BUILD_DIR/jar/nativewindow.all-noawt.jar:$BUILD_DIR/jar/jogl.core.jar:$BUILD_DIR/jar/jogl.util.jar:$BUILD_DIR/jar/jogl.gles2.jar:$BUILD_DIR/jar/jogl.gles2.dbg.jar:$BUILD_DIR/jar/jogl.egl.jar:$BUILD_DIR/jar/jogl.util.fixedfuncemu.jar:$BUILD_DIR/jar/jogl.glu.tess.jar:$BUILD_DIR/jar/jogl.glu.mipmap.jar:$BUILD_DIR/jar/newt.all-noawt.jar:$BUILD_DIR/jar/jogl.test.jar

ssh $TARGET_UID@$TARGET_IP "\
rsync -aAv --delete --delete-after --exclude 'build-x86*/' $HOST_UID@$HOST_IP::$HOST_RSYNC_ROOT/gluegen $HOST_UID@$HOST_IP::$HOST_RSYNC_ROOT/jogl $TARGET_ROOT ; \
cd $TARGET_ROOT/jogl/make ;
export DISPLAY=:0.0 ;
LD_LIBRARY_PATH=../../gluegen/make/$BUILD_DIR/obj:$BUILD_DIR/lib \
java \
  -Djava.library.path=../../gluegen/make/$BUILD_DIR/obj:$BUILD_DIR/lib \
  -Djava.class.path=$CP_BASE:$CP_JOGL_EGLES \
  -Djogl.debug.GLProfile=true \
  -Djogl.debug.EGL=true \
  -Djogamp.debug.JNILibLoader=true \
  -Djogamp.debug.NativeLibrary=true \
  -Dnewt.debug.Screen=true \
  -Dnativewindow.x11.mt-bug=true \
  $TSTCLASS $* \
 2>&1 | tee $LOGFILE \
"

scp $TARGET_UID@$TARGET_IP:$TARGET_ROOT/jogl/make/$LOGFILE .
