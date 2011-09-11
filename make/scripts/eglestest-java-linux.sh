#
# desktop EGL/ES1/ES2 test script
#
# Ubuntu:
#  sudo apt-get install mesa-utils-extra libegl1-mesa libegl1-mesa-drivers libgles1-mesa libgles2-mesa 
#
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

#  -Djogamp.debug.NativeLibrary=true \
#  -Djogamp.debug.NativeLibrary.Lookup=true \
#  -Djogamp.debug.ProcAddressHelper=true \
#  -Djogamp.debug=all \
#  -Dnativewindow.debug=all \
#  -Djogl.debug=all \
#  -Dnewt.debug=all \

CP_BASE=../../gluegen/make/lib/junit.jar:$ANT_PATH/lib/ant.jar:$ANT_PATH/lib/ant-junit.jar:../../gluegen/make/$BUILD_DIR/gluegen.jar
CP_JOGL_ALL=$BUILD_DIR/jar/jogl.all.jar:$BUILD_DIR/jar/jogl.test.jar
CP_JOGL_MOBILE=$BUILD_DIR/jar/jogl.all-mobile.jar:$BUILD_DIR/jar/jogl.test.jar

export DISPLAY=:0.0 ;
java \
  -Djava.library.path=../../gluegen/make/$BUILD_DIR/obj:$BUILD_DIR/lib \
  -Djava.class.path=$CP_BASE:$CP_JOGL_MOBILE \
  -Djogamp.debug.NativeLibrary=true \
  -Dnativewindow.debug=all \
  -Djogl.debug=all \
  $TSTCLASS $* \
 2>&1 | tee $LOGFILE \

