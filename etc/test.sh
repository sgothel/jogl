#! /bin/bash

THISDIR=`pwd`
logfile=`basename $0 .sh`.log

rm -f $logfile

which java 2>&1 | tee -a $logfile
java -version 2>&1 | tee -a $logfile
echo LIBXCB_ALLOW_SLOPPY_LOCK: $LIBXCB_ALLOW_SLOPPY_LOCK 2>&1 | tee -a $logfile
echo LIBGL_DRIVERS_PATH: $LIBGL_DRIVERS_PATH 2>&1 | tee -a $logfile
echo LIBGL_DEBUG: $LIBGL_DEBUG 2>&1 | tee -a $logfile
echo java $X_ARGS $D_ARGS $* 2>&1 | tee -a $logfile

CLASSPATH=jar/gluegen-rt.jar:jar/jogl-all.jar
export CLASSPATH

echo CLASSPATH: $CLASSPATH
echo

# D_ARGS="-Djogamp.debug=all -Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all"
#
# D_ARGS="-Djogamp.debug.TraceLock"
# D_ARGS="-Dnewt.debug.EDT -Dnativewindow.debug.ToolkitLock.TraceLock -Dnativewindow.debug.NativeWindow"
# D_ARGS="-Dnewt.debug.Window -Dnewt.debug.Display -Dnewt.debug.EDT"
# D_ARGS="-Dnewt.debug.EDT -Dnativewindow.debug.ToolkitLock.TraceLock -Dnativewindow.debug.X11Util.TraceDisplayLifecycle=true"
#D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=600000 -Djogamp.debug.Lock -Djogamp.debug.Lock.TraceLock"
# D_ARGS="-Dnewt.debug.Window -Dnewt.debug.EDT -Dnewt.debug.Display "
#D_ARGS="-Dnewt.debug.EDT -Djogamp.common.utils.locks.Lock.timeout=600000 -Djogl.debug.Animator -Dnewt.debug.Display -Dnewt.debug.Screen"
#D_ARGS="-Dnewt.debug.EDT -Dnewt.debug.Display -Dnativewindow.debug.X11Util -Djogl.debug.GLDrawable -Djogl.debug.GLCanvas"
#D_ARGS="-Dnewt.debug.EDT -Djogl.debug.GLContext"
#D_ARGS="-Dnewt.debug.Screen -Dnewt.debug.EDT -Djogamp.debug.Lock"
#D_ARGS="-Dnewt.debug.EDT"
#D_ARGS="-Dnewt.debug.EDT -Djogl.debug=all -Dnativewindow.debug=all"
# D_ARGS="-Djogl.debug=all"
X_ARGS="-Dsun.java2d.noddraw=true -Dsun.java2d.opengl=false"

#java $X_ARGS $ARGS_AWT  $D_ARGS com.jogamp.common.GlueGenVersion 2>&1 | tee -a $logfile
#java $X_ARGS $ARGS_AWT  $D_ARGS com.jogamp.nativewindow.NativeWindowVersion 2>&1 | tee -a $logfile
#java $X_ARGS $ARGS_AWT  $D_ARGS com.jogamp.opengl.JoglVersion 2>&1 | tee -a $logfile
#java $X_ARGS $ARGS_AWT  $D_ARGS com.jogamp.newt.NewtVersion 2>&1 | tee -a $logfile
#java $X_ARGS $ARGS_AWT  $D_ARGS com.jogamp.opengl.awt.GLCanvas 2>&1 | tee -a $logfile
java $X_ARGS $ARGS_NEWT $D_ARGS com.jogamp.newt.opengl.GLWindow 2>&1 | tee -a $logfile

