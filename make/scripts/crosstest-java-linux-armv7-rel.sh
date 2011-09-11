export HOST_UID=sven
export HOST_IP=192.168.0.52
#export HOST_IP=192.168.1.5
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
export TARGET_IP=beagle01
export TARGET_ROOT=projects-cross
#export TARGET_JAVA=/usr/bin/java
export TARGET_JAVA=/opt-linux-armv7-eabi/jre6/bin/java

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
#TSTCLASS=com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo01
#TSTCLASS=com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo02
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT
TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.es1.newt.TestGearsES1NEWT

LOGFILE=`basename $0 .sh`.log

#  -Djogamp.debug.NativeLibrary.Lookup=true \
#  -Djogamp.debug.ProcAddressHelper=true \

RSYNC_EXCLUDES="--exclude 'build-x86*/' --exclude 'build-linux-x*/' --exclude 'build-android*/' --exclude 'build-win*/' --exclude 'build-mac*/' \
                --exclude 'classes/' --exclude 'src/' --exclude '.git/' --exclude 'jogl-java-src.zip' \
                --delete-excluded"

CP_BASE=../../gluegen/make/lib/junit.jar:$ANT_PATH/lib/ant.jar:$ANT_PATH/lib/ant-junit.jar:../../gluegen/make/$BUILD_DIR/gluegen.jar
CP_JOGL_MOBILE=$BUILD_DIR/jar/jogl.all-mobile.jar:$BUILD_DIR/jar/jogl.test.jar

#  -Djogl.debug=all \
#  -Dnewt.debug=all \
#  -Djogl.debug.DebugGL \

ssh $TARGET_UID@$TARGET_IP "\
rsync -aAv --delete --delete-after $RSYNC_EXCLUDES $HOST_UID@$HOST_IP::$HOST_RSYNC_ROOT/gluegen $HOST_UID@$HOST_IP::$HOST_RSYNC_ROOT/jogl $TARGET_ROOT ; \
cd $TARGET_ROOT/jogl/make ;
export DISPLAY=:0.0 ;
LD_LIBRARY_PATH=../../gluegen/make/$BUILD_DIR/obj:$BUILD_DIR/lib \
$TARGET_JAVA \
  -server \
  -Xmx256m \
  -Djava.library.path=../../gluegen/make/$BUILD_DIR/obj:$BUILD_DIR/lib \
  -Djava.class.path=$CP_BASE:$CP_JOGL_MOBILE \
  -Dnativewindow.x11.mt-bug=true \
  $TSTCLASS $* \
 2>&1 | tee $LOGFILE \
"

scp $TARGET_UID@$TARGET_IP:$TARGET_ROOT/jogl/make/$LOGFILE .
