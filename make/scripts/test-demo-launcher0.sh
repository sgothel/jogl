#! /bin/bash

if [ -z "$1" -o -z "$2" ] ; then
    echo Usage $0 java-exe build-dir
    exit 0
fi

#set -x 

javaexe="$1"
shift
bdir="$1"
shift

if [ ! -x "$javaexe" ] ; then
    echo java-exe "$javaexe" is not an executable
    exit 1
fi
if [ ! -d "$bdir" ] ; then
    echo build-dir "$bdir" is not a directory
    exit 1
fi

rm -f java-demo-run.log

bdirb=`basename "$bdir"`

#export LIBGL_DEBUG=verbose 
#export MESA_DEBUG=true 
#export LIBGL_ALWAYS_SOFTWARE=true
#export INTEL_DEBUG="buf bat"
#export INTEL_STRICT_CONFORMANCE=1 

X_ARGS="-Djava.awt.headless=true $X_ARGS"

#D_ARGS="-Djogl.debug.GLProfile -Djogl.debug.GLContext"
#D_ARGS="-Djogl.debug.GLProfile"
#D_ARGS="-Djogl.debug.DebugGL"
#D_ARGS="-Djogl.debug.TraceGL"
#D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL"
#D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL -Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all"
#D_ARGS="-Djogl.debug.GLProfile -Djogl.debug.GLContext -Djogl.quirks.force=GL3CompatNonCompliant,NoSurfacelessCtx -Djogl.disable.opengles" 
#D_ARGS="-Djogl.quirks.force=NoDoubleBufferedPBuffer" 
#D_ARGS="-Dnativewindow.debug.GraphicsConfiguration"
#D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=600000"

#D_ARGS="-Djogamp.debug=all"
#D_ARGS="-Dnativewindow.debug=all"
#D_ARGS="-Djogl.debug=all"
#D_ARGS="-Djogl.debug=all -Dnewt.debug=all -Djogl.debug.DebugGL"
#D_ARGS="-Dnewt.debug=all"
D_ARGS="-Dnewt.debug.Display.PointerIcon -Dnewt.disable.PointerIcon"

#D_ARGS="-Dnewt.disable.LinuxKeyEventTracker -Dnewt.disable.LinuxMouseTracker"
#D_ARGS="-Dnewt.disable.LinuxKeyEventTracker"
#D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all -Dnewt.disable.LinuxKeyEventTracker -Dnewt.disable.LinuxMouseTracker"
#D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all -Dnewt.disable.LinuxKeyEventTracker"
#D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all -Dnewt.disable.LinuxMouseTracker"
#D_ARGS="-Djogl.debug=all -Dnewt.debug=all"
#D_ARGS="-Djogl.debug=all -Dnativewindow.debug=all"
#D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all"
#D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all -Djogl.disable.opengldesktop"
#D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all -Djogl.disable.opengldesktop -Djogl.quirks.force=NoSurfacelessCtx"

USE_CLASSPATH="../../gluegen/$bdirb/gluegen-rt.jar:../../joal/$bdirb/jar/joal.jar:../$bdirb/jar/jogl-all-noawt.jar:../$bdirb/jar/atomic/oculusvr.jar:../$bdirb/jar/jogl-demos-mobile.jar"
JAR_CLASSPATH="../../gluegen/$bdirb/gluegen-rt.jar ../../joal/$bdirb/jar/joal.jar ../$bdirb/jar/jogl-all-noawt.jar ../$bdirb/jar/atomic/oculusvr.jar ../$bdirb/jar/jogl-demos-mobile.jar"

function inflate0() {
    echo building temp_classpath
    rm -rf temp_classpath
    mkdir temp_classpath
    for i in $JAR_CLASSPATH ; do
        echo Inflating $i
        unzip -q -o $i -d temp_classpath
    done
    for i in ../../gluegen/$bdirb/*natives*.jar ../../joal/$bdirb/jar/*natives*.jar ../$bdirb/jar/*natives*.jar ; do
        echo Inflating $i
        unzip -q -o $i -d temp_classpath
    done
}


function jrun0() {
    which "$javaexe" 2>&1 | tee -a java-run.log
    "$javaexe" -version 2>&1 | tee -a java-run.log
    echo LD_LIBRARY_PATH $LD_LIBRARY_PATH 2>&1 | tee -a java-run.log
    echo LIBXCB_ALLOW_SLOPPY_LOCK: $LIBXCB_ALLOW_SLOPPY_LOCK 2>&1 | tee -a java-run.log
    echo LIBGL_DRIVERS_PATH: $LIBGL_DRIVERS_PATH 2>&1 | tee -a java-run.log
    echo LIBGL_DEBUG: $LIBGL_DEBUG 2>&1 | tee -a java-run.log
    echo LIBGL_ALWAYS_INDIRECT: $LIBGL_ALWAYS_INDIRECT 2>&1 | tee -a java-run.log
    echo LIBGL_ALWAYS_SOFTWARE: $LIBGL_ALWAYS_SOFTWARE 2>&1 | tee -a java-run.log
    echo SWT_CLASSPATH: $SWT_CLASSPATH 2>&1 | tee -a java-run.log
    echo MacOsX $MOSX 2>&1 | tee -a java-run.log
    echo DISPLAY $DISPLAY 2>&1 | tee -a java-run.log
    echo WAYLAND_DISPLAY $WAYLAND_DISPLAY 2>&1 | tee -a java-run.log

    if [ -z "$JAR_CLASSPATH" ] ; then
        echo "$javaexe" $javaxargs $X_ARGS -cp $USE_CLASSPATH $D_ARGS $C_ARG $*
        #gdb --args "$javaexe" $javaxargs $X_ARGS -cp $USE_CLASSPATH $D_ARGS $C_ARG $*
        "$javaexe" $X_ARGS -cp $USE_CLASSPATH $D_ARGS $C_ARG $*
    else
        echo "$javaexe" $javaxargs $X_ARGS -cp temp_classpath $D_ARGS $C_ARG $*
        "$javaexe" $X_ARGS -cp temp_classpath $D_ARGS $C_ARG $*
    fi
}

function jrun() {
    if [ ! -z "$JAR_CLASSPATH" ] ; then
        inflate0 2>&1 | tee -a java-demo-run.log
        nlibdir=`find temp_classpath  -name libgluegen_rt.so -exec dirname \{\} \;`
        echo Native Libraries in $nlibdir 2>&1 | tee -a java-demo-run.log
        export LD_LIBRARY_PATH=$nlibdir
        export DYLD_LIBRARY_PATH=$nlibdir
    fi
    jrun0 $* 2>&1 | tee -a java-demo-run.log
}

jrun com.jogamp.opengl.demos.Launcher0 $*

