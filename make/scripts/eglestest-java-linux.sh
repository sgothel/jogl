export BUILD_DIR=../build-x86_64
export ANT_PATH=/opt-share/apache-ant

#TSTCLASS=com.jogamp.nativewindow.NativeWindowVersion
#TSTCLASS=com.jogamp.opengl.JoglVersion
#TSTCLASS=com.jogamp.newt.NewtVersion
#TSTCLASS=com.jogamp.newt.opengl.GLWindow
TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.gl2es1.gears.newt.TestGearsGL2ES1NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen01GLPBufferNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLSimple01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState02NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestRulerNEWT01

LOGFILE=`basename $0 .sh`.log

#  -Djogamp.debug.NativeLibrary.Lookup=true \
#  -Djogamp.debug.ProcAddressHelper=true \

CP_BASE=../../gluegen/make/lib/junit.jar:$ANT_PATH/lib/ant.jar:$ANT_PATH/lib/ant-junit.jar:../../gluegen/make/$BUILD_DIR/gluegen.jar
CP_JOGL_ALL=$BUILD_DIR/jar/nativewindow.all.jar:$BUILD_DIR/jar/jogl.all.jar:$BUILD_DIR/jar/newt.all.jar:$BUILD_DIR/jar/jogl.test.jar
#CP_JOGL_EGLES=$BUILD_DIR/jar/nativewindow.all-noawt.jar:$BUILD_DIR/jar/jogl.core.jar:$BUILD_DIR/jar/jogl.util.jar:$BUILD_DIR/jar/jogl.gles1.jar:$BUILD_DIR/jar/jogl.gles1.dbg.jar:$BUILD_DIR/jar/jogl.gles2.jar:$BUILD_DIR/jar/jogl.gles2.dbg.jar:$BUILD_DIR/jar/jogl.egl.jar:$BUILD_DIR/jar/jogl.util.fixedfuncemu.jar:$BUILD_DIR/jar/jogl.glu.tess.jar:$BUILD_DIR/jar/jogl.glu.mipmap.jar:$BUILD_DIR/jar/newt.all-noawt.jar:$BUILD_DIR/jar/jogl.test.jar
CP_JOGL_EGLES1=$BUILD_DIR/jar/nativewindow.all-noawt.jar:$BUILD_DIR/jar/jogl.core.jar:$BUILD_DIR/jar/jogl.util.jar:$BUILD_DIR/jar/jogl.gles1.jar:$BUILD_DIR/jar/jogl.gles1.dbg.jar:$BUILD_DIR/jar/jogl.egl.jar:$BUILD_DIR/jar/jogl.util.fixedfuncemu.jar:$BUILD_DIR/jar/jogl.glu.tess.jar:$BUILD_DIR/jar/jogl.glu.mipmap.jar:$BUILD_DIR/jar/newt.all-noawt.jar:$BUILD_DIR/jar/jogl.test.jar
CP_JOGL_EGLES2=$BUILD_DIR/jar/nativewindow.all-noawt.jar:$BUILD_DIR/jar/jogl.core.jar:$BUILD_DIR/jar/jogl.util.jar:$BUILD_DIR/jar/jogl.gles2.jar:$BUILD_DIR/jar/jogl.gles2.dbg.jar:$BUILD_DIR/jar/jogl.egl.jar:$BUILD_DIR/jar/jogl.util.fixedfuncemu.jar:$BUILD_DIR/jar/jogl.glu.tess.jar:$BUILD_DIR/jar/jogl.glu.mipmap.jar:$BUILD_DIR/jar/newt.all-noawt.jar:$BUILD_DIR/jar/jogl.test.jar

LD_OGLES1=/opt-share/egles-emu/PVRVFrame/OGLES-1.1/Linux_x86_64
LD_OGLES2=/opt-share/egles-emu/PVRVFrame/OGLES-2.0/Linux_x86_64

export DISPLAY=:0.0 ;
LD_LIBRARY_PATH=$LD_OGLES1:../../gluegen/make/$BUILD_DIR/obj:$BUILD_DIR/lib \
java \
  -Djava.library.path=../../gluegen/make/$BUILD_DIR/obj:$BUILD_DIR/lib \
  -Djava.class.path=$CP_BASE:$CP_JOGL_EGLES1 \
  -Djogl.debug=all \
  -Dnewt.debug=all \
  -Dnativewindow.debug=all \
  $TSTCLASS \
 2>&1 | tee $LOGFILE \

