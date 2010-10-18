#! /bin/bash

rm -f java-run.log

spath=`dirname $0`

#com.jogamp.test.junit.newt.TestParenting01AWT

# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.jogl.acore.TestGLProfile01NEWT $*

# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.jogl.demos.gl2.gears.TestGearsAWT $*
# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.jogl.demos.gl2.gears.TestGearsNEWT $*
# $spath/java-run-all.sh ../build-x86_64 -Djava.awt.headless=true com.jogamp.test.junit.jogl.demos.gl2.gears.TestGearsNEWT $*

# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.TestDisplayLifecycle01NEWT $*

# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.TestListenerCom01AWT $*

# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.TestGLWindows01NEWT $*
# $spath/java-run-all.sh ../build-x86_64 -Djava.awt.headless=true com.jogamp.test.junit.newt.TestGLWindows01NEWT $*
# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.TestGLWindows02NEWTAnimated $*

# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.jogl.offscreen.TestOffscreen01NEWT $*

# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.parenting.TestParenting01NEWT $*
# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.parenting.TestParenting02NEWT $*

# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.parenting.TestParenting01aAWT $*
# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.parenting.TestParenting01bAWT $*
# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.parenting.TestParenting01cAWT $*
# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.parenting.TestParenting01cSwingAWT $*
# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.parenting.TestParenting02AWT $*
# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.parenting.TestParenting03AWT $*

# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.jogl.awt.TestSwingAWTRobotUsageBeforeJOGLInitBug411 $*
# $spath/java-run-all.sh ../build-x86_64 -Dnativewindow.TraceLock=true com.jogamp.test.junit.jogl.awt.TestSwingAWTRobotUsageBeforeJOGLInitBug411 $*

# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.TestFocus01SwingAWTRobot $*
# $spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.TestFocus02SwingAWTRobot $*

#$spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.TestEventSourceNotAWTBug $*

#$spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.TestScreenMode01NEWT $*

$spath/java-run-all.sh ../build-x86_64 com.jogamp.test.junit.newt.TestScreenMode02NEWT $*



$spath/count-edt-start.sh java-run.log
