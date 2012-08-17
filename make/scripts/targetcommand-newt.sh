#! /bin/sh

THISDIR=`pwd`

ROOT_REL=build-linux-armv6hf

export LD_LIBRARY_PATH=$THISDIR/PVRTrace/:$LD_LIBRARY_PATH

#XTRA_FLAGS="-Dnewt.test.Screen.disableScreenMode -Djogl.debug.DebugGL -Djogl.debug.TraceGL -Djogl.debug.GLContext.TraceSwitch "
#XTRA_FLAGS="-Dnewt.test.Screen.disableScreenMode -Djogl.debug.DebugGL -Djogl.debug.TraceGL"
#XTRA_FLAGS="-Dnewt.test.Screen.disableScreenMode -Djogl.debug.DebugGL"
#XTRA_FLAGS="-Dnewt.test.Screen.disableScreenMode -Djogl.debug.EGL -Dnativewindow.debug.GraphicsConfiguration -Djogl.debug.GLDrawable"
#XTRA_FLAGS="-Dnewt.debug.Screen"
#XTRA_FLAGS="-Dnativewindow.debug.GraphicsConfiguration -Dnativewindow.debug.NativeWindow"
#XTRA_FLAGS="-Dnewt.debug.Window -Djogl.debug.EGL -Djogl.debug.GLContext -Djogl.debug.GLDrawable"
#XTRA_FLAGS="-Djogl.debug.GLContext -Djogl.debug.GLProfile -Djogl.debug.GLDrawable"
#XTRA_FLAGS="-Djogl.debug.EGL"
#XTRA_FLAGS="-Djogl.debug.GraphicsConfiguration"
#XTRA_FLAGS="-Djogl.debug.GLContext -Djogl.debug.GLDrawable"
#XTRA_FLAGS="-Djogl.debug.TraceGL"
#XTRA_FLAGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL"

# OK (Panda, Omap4)
#
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.util.TestGLReadBufferUtilTextureIOWrite01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.util.TestGLReadBufferUtilTextureIOWrite02NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.graph.TestRegionRendererNEWT01 # (Tegra regressions)
#TSTCLASS=com.jogamp.opengl.test.junit.graph.TestTextRendererNEWT01  # (Tegra regressions)
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestGLDebug00NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestGLDebug01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestGPUMemSec01NEWT
TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestGLDrawable01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestInitConcurrentNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestFBODrawableNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.caps.TestMultisampleES1NEWT

# Some Regressions (Panda, Omap4)
#
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextListNEWT2
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextListNEWT

#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestMainVersionGLWindowNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestMapBufferRead01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestNVSwapGroupNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES1NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2NEWT2
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestShutdownCompleteNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestShutdownSharedNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.caps.TestTranslucencyNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.es1.newt.TestGearsES1NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.es1.newt.TestRedSquareES1NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestElektronenMultipliziererNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestRedSquareES2NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieCube
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.demos.gl2.newt.TestGearsNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestFBOMRTNEWT01
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.acore.TestFBOMix2DemosES2NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState02NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLSimple01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestRulerNEWT01
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.glsl.TestTransformFeedbackVaryingsBug407NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen01GLPBufferNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen02BitmapNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestDisplayLifecycle01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestDisplayLifecycle02NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestGLWindows00NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestGLWindows01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestGLWindows02NEWTAnimated
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestRemoteGLWindows01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestRemoteWindow01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestScreenMode00NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestScreenMode00bNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestScreenMode01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestScreenMode01bNEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestScreenMode02NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestWindowClosingProtocol02NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.TestWindows01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.parenting.TestParenting01NEWT
#TSTCLASS=com.jogamp.opengl.test.junit.newt.parenting.TestParenting02NEWT
 
 mkdir -p $THISDIR/projects-cross 

 rsync -av --delete --delete-after --delete-excluded \
       --exclude 'build-x86*/' --exclude 'build-linux-x*/' --exclude 'build-android*/' --exclude 'build-win*/' --exclude 'build-mac*/' \
       --exclude 'classes/' --exclude 'src/' --exclude '.git/' --exclude '*-java-src.zip' \
       --exclude 'make/lib/external/' \
       jogamp@jogamp02::PROJECTS/JOGL/gluegen jogamp@jogamp02::PROJECTS/JOGL/jogl $THISDIR/projects-cross 

 cd $THISDIR/projects-cross/jogl/make 

 cp -a $THISDIR/pvrtrace.cfg .
 
function junit_run() {
     java \
     -cp ../../gluegen/make/lib/junit.jar:/usr/share/ant/lib/ant.jar:/usr/share/ant/lib/ant-junit.jar:../../gluegen/$ROOT_REL/gluegen-rt.jar:../$ROOT_REL/jar/jogl-all-noawt.jar:../$ROOT_REL/jar/jogl-test.jar\
     -Djava.awt.headless=true\
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
     -cp ../../gluegen/make/lib/junit.jar:/usr/share/ant/lib/ant.jar:/usr/share/ant/lib/ant-junit.jar:../../gluegen/$ROOT_REL/gluegen-rt.jar:../$ROOT_REL/jar/jogl-all-noawt.jar:../$ROOT_REL/jar/jogl-test.jar\
     -Djava.awt.headless=true\
     $XTRA_FLAGS \
     $TSTCLASS \
     $*
}
 
# junit_run 2>&1 | tee $THISDIR/targetcommand.log

main_run $* 2>&1 | tee $THISDIR/targetcommand.log

cp -a trace-*.pvrt $THISDIR/
 
