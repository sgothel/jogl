#! /bin/sh

THISDIR=`pwd`

ROOT_REL=build-linux-armv6hf

#XTRA_FLAGS="-Dnewt.test.Screen.disableScreenMode"
XTRA_FLAGS="-Dnewt.test.Screen.disableScreenMode -Djogl.debug.EGL -Dnativewindow.debug.GraphicsConfiguration -Djogl.debug.GLDrawable"
#XTRA_FLAGS="-Dnewt.debug.Screen"
#XTRA_FLAGS="-Dnativewindow.debug.GraphicsConfiguration -Dnativewindow.debug.NativeWindow"
#XTRA_FLAGS="-Dnewt.debug.Window -Djogl.debug.EGL -Djogl.debug.GLContext -Djogl.debug.GLDrawable"
#XTRA_FLAGS="-Djogl.debug.GLContext -Djogl.debug.GLProfile -Djogl.debug.GLDrawable"
#XTRA_FLAGS="-Djogl.debug.EGL"
#XTRA_FLAGS="-Djogl.debug.GraphicsConfiguration"
#XTRA_FLAGS="-Djogl.debug.GLContext -Djogl.debug.GLDrawable"
#XTRA_FLAGS="-Djogl.debug.TraceGL"
#XTRA_FLAGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL"

#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.es2.awt.TestGearsES2AWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.gl2.awt.TestGearsAWT
TSTCLASS=com.jogamp.opengl.test.junit.jogl.awt.TestAWT01GLn
 
 mkdir -p $THISDIR/projects-cross 

 rsync -av --delete --delete-after --delete-excluded \
       --exclude 'build-x86*/' --exclude 'build-linux-x*/' --exclude 'build-android*/' --exclude 'build-win*/' --exclude 'build-mac*/' \
       --exclude 'classes/' --exclude 'src/' --exclude '.git/' --exclude '*-java-src.zip' \
       --exclude 'make/lib/external/' \
       jogamp@jogamp02::PROJECTS/JOGL/gluegen jogamp@jogamp02::PROJECTS/JOGL/jogl $THISDIR/projects-cross 

 cd $THISDIR/projects-cross/jogl/make 
 
function junit_run() {
     java \
     -cp ../../gluegen/make/lib/junit.jar:/usr/share/ant/lib/ant.jar:/usr/share/ant/lib/ant-junit.jar:../../gluegen/$ROOT_REL/gluegen-rt.jar:../$ROOT_REL/jar/jogl-all.jar:../$ROOT_REL/jar/jogl-test.jar\
     $XTRA_FLAGS \
     org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner \
     $TSTCLASS \
     filtertrace=true \
     haltOnError=false \
     haltOnFailure=false \
     showoutput=true \
     outputtoformatters=true \
     logfailedtests=true \
     logtestlistenerevents=true \
     formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter \
     formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,$THISDIR/targetcommand.xml
}
 
function main_run() {
     java \
     -cp ../../gluegen/make/lib/junit.jar:/usr/share/ant/lib/ant.jar:/usr/share/ant/lib/ant-junit.jar:../../gluegen/$ROOT_REL/gluegen-rt.jar:../$ROOT_REL/jar/jogl-all.jar:../$ROOT_REL/jar/jogl-test.jar\
     $XTRA_FLAGS \
     $TSTCLASS \
     $*
}
 
# junit_run 2>&1 | tee $THISDIR/targetcommand.log

main_run $* 2>&1 | tee $THISDIR/targetcommand.log
 
