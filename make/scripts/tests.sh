#! /bin/bash

bdir=../build-x86_64

rm -f java-run.log

spath=`dirname $0`

. $spath/setenv-jogl.sh $bdir JOGL_ALL

which java 2>&1 | tee -a java-run.log
java -version 2>&1 | tee -a java-run.log
echo LIBXCB_ALLOW_SLOPPY_LOCK: $LIBXCB_ALLOW_SLOPPY_LOCK 2>&1 | tee -a java-run.log
echo LIBGL_DRIVERS_PATH: $LIBGL_DRIVERS_PATH 2>&1 | tee -a java-run.log
echo LIBGL_DEBUG: $LIBGL_DEBUG 2>&1 | tee -a java-run.log
echo java $X_ARGS $D_ARGS $* 2>&1 | tee -a java-run.log

function jrun() {
    awtarg=$1
    shift

    #D_ARGS="-Djogl.debug.ExtensionAvailabilityCache -Djogl.debug=all -Dnativewindow.debug=all -Djogamp.debug.ProcAddressHelper=true -Djogamp.debug.NativeLibrary=true -Djogamp.debug.NativeLibrary.Lookup=true"
    #D_ARGS="-Djogl.debug=all -Dnativewindow.debug=all"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.ExtensionAvailabilityCache"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLProfile -Djogl.debug.GLDrawable"
    #D_ARGS="-Djogl.debug.GLProfile"
    #D_ARGS="-Dnewt.debug.Window -Djogamp.common.utils.locks.Lock.timeout=600000 -Djogl.debug.Animator"
    # D_ARGS="-Dnewt.debug.EDT -Dnativewindow.debug.ToolkitLock.TraceLock -Dnativewindow.debug.NativeWindow"
    #D_ARGS="-Dnewt.debug.Window -Dnewt.debug.Display -Dnewt.debug.EDT"
    # D_ARGS="-Dnewt.debug.EDT -Dnativewindow.debug.ToolkitLock.TraceLock -Dnativewindow.debug.X11Util.TraceDisplayLifecycle=true"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=600000 -Djogamp.debug.Lock -Djogamp.debug.Lock.TraceLock"
    # D_ARGS="-Dnewt.debug.Window -Dnewt.debug.EDT -Dnewt.debug.Display "
    #D_ARGS="-Dnewt.debug.EDT -Djogamp.common.utils.locks.Lock.timeout=600000 -Djogl.debug.Animator -Dnewt.debug.Display -Dnewt.debug.Screen"
    #D_ARGS="-Dnewt.debug.EDT -Dnewt.debug.Display -Dnativewindow.debug.X11Util -Djogl.debug.GLDrawable -Djogl.debug.GLCanvas"
    #D_ARGS="-Djogl.debug.GLContext -Dnewt.debug=all"
    #D_ARGS="-Dnewt.debug.Screen -Dnewt.debug.EDT -Djogamp.debug.Lock"
    #D_ARGS="-Dnewt.debug.EDT"
    #D_ARGS="-Djogl.debug=all -Dnativewindow.debug=all -Dnewt.debug=all"
    #D_ARGS="-Djogl.debug=all -Dnewt.debug=all"
    #D_ARGS="-Dnewt.debug=all"
    #D_ARGS="-Dnativewindow.debug=all"
    #D_ARGS="-Djogl.debug.GraphicsConfiguration"
    #D_ARGS="-Djogl.debug.GLCanvas"
    #X_ARGS="-Dsun.java2d.noddraw=true -Dsun.java2d.opengl=true"
    #X_ARGS="-verbose:jni"
    echo
    echo "Test Start: $*"
    echo
    java $awtarg $X_ARGS $D_ARGS $*
    echo
    echo "Test End: $*"
    echo
}

function testnoawt() {
    jrun -Djava.awt.headless=true $* 2>&1 | tee -a java-run.log
}

function testawt() {
    jrun -Djava.awt.headless=false $* 2>&1 | tee -a java-run.log
}

#
# newt (testnoawt and testawt)
#
#testnoawt com.jogamp.nativewindow.NativeWindowVersion $*
#testnoawt com.jogamp.opengl.JoglVersion $*
#testnoawt com.jogamp.newt.NewtVersion $*
#testnoawt com.jogamp.newt.opengl.GLWindow $*
testnoawt com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen01GLPBufferNEWT $*
testnoawt com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen02BitmapNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile01NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile01NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextListNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestTransformFeedbackVaryingsBug407NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLSimple01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestRemoteWindow01NEWT -time 1000000
#testnoawt com.jogamp.opengl.test.junit.newt.TestRemoteGLWindows01NEWT -time 1000000
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.newt.TestGearsNEWT $*
#testawt com.jogamp.opengl.test.junit.newt.TestDisplayLifecycle01NEWT
#testawt com.jogamp.opengl.test.junit.newt.TestDisplayLifecycle02NEWT
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01NEWT
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting02NEWT
#testawt com.jogamp.opengl.test.junit.newt.TestScreenMode00NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestScreenMode01NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestScreenMode02NEWT
#testawt com.jogamp.opengl.test.junit.newt.TestGLWindows01NEWT -time 1000000
#testawt -Djava.awt.headless=true com.jogamp.opengl.test.junit.newt.TestGLWindows01NEWT
#testawt com.jogamp.opengl.test.junit.newt.TestGLWindows02NEWTAnimated


#
# awt (testawt)
#
#testawt javax.media.opengl.awt.GLCanvas $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT01GLn $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextListAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestSwingAWT01GLn
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT03GLCanvasRecreate01 $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT02WindowClosing
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.TestGearsAWT
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.TestGearsGLJPanelAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.texture.TestTexture01AWT

#
# newt.awt (testawt)
#
#testawt com.jogamp.opengl.test.junit.jogl.newt.TestSwingAWTRobotUsageBeforeJOGLInitBug411
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.newt.TestGearsNewtAWTWrapper
#testawt com.jogamp.opengl.test.junit.newt.TestEventSourceNotAWTBug
#testawt com.jogamp.opengl.test.junit.newt.TestFocus01SwingAWTRobot
#testawt com.jogamp.opengl.test.junit.newt.TestFocus02SwingAWTRobot
#testawt com.jogamp.opengl.test.junit.newt.TestListenerCom01AWT
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01aAWT
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01bAWT
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01cAWT
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01cSwingAWT
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting02AWT
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting03AWT
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting03AWT -time 100000
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting03bAWT -time 100000
#testawt com.jogamp.opengl.test.junit.newt.TestCloseNewtAWT
#testawt com.jogamp.opengl.test.junit.jogl.caps.TestMultisampleAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.caps.TestMultisampleNEWT $*

#testawt com.jogamp.opengl.test.junit.newt.TestGLWindows02NEWTAnimated $*
#testawt com.jogamp.opengl.test.junit.jogl.newt.TestSwingAWTRobotUsageBeforeJOGLInitBug411 $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01NEWT $*

#testawt $*

#testawt com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile01NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT03GLCanvasRecreate01 -time 2000
#testawt com.jogamp.opengl.test.junit.jogl.newt.TestSwingAWTRobotUsageBeforeJOGLInitBug411

$spath/count-edt-start.sh java-run.log

