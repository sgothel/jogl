#! /bin/bash

scriptdir=`dirname $0`

function print_usage() {
    echo "Usage: $0 [-libdir pre-lib-dir] jogl-build-dir ..."
}

if [ "$1" = "-libdir" ] ; then
    shift
    if [ -z "$1" ] ; then
        echo libdir argument missing
        print_usage
        exit
    fi
    PRELIB=$1
    shift
    LD_LIBRARY_PATH=$PRELIB:$LD_LIBRARY_PATH
    export LD_LIBRARY_PATH
    # Mesa/Gallium  EGL driver
    EGL_DRIVER=$PRELIB/egl_glx.so
    export EGL_DRIVER
    # unset DRI/ATI ..
    unset LIBGL_DRIVERS_PATH
fi

if [ -z "$1" ] ; then
    echo JOGL BUILD DIR missing
    print_usage
    exit
fi

. $scriptdir/setenv-jogl.sh $1 JOGL_ALL
shift

MOSX=0
uname -a | grep -i Darwin && MOSX=1

# D_ARGS="-Djogamp.debug.ProcAddressHelper=true -Djogamp.debug.NativeLibrary=true -Djogl.debug=all"
# D_ARGS="-Djogamp.debug.ProcAddressHelper=true -Djogamp.debug.NativeLibrary=true -Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all"
# D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all -Djogl.debug.GLSLState"
# D_ARGS="-Dnativewindow.debug.X11Util=true -Djogl.debug.GLDrawableFactory=true"
# D_ARGS="-Dnativewindow.debug.X11Util=true"
# D_ARGS="-Dnewt.debug=all -Dnativewindow.debug=all"
# D_ARGS="-Djogl.debug=all -Dnewt.debug=all -Dnativewindow.debug=all"
# D_ARGS="-Dnewt.debug=all -Dnativewindow.debug=all -Djogamp.common.utils.locks.Lock.timeout=600000 -Djogamp.debug.Lock -Djogamp.debug.Lock.TraceLock"
# D_ARGS="-Dnewt.debug=all -Dnativewindow.debug=all -Djogamp.common.utils.locks.Lock.timeout=600000"
# D_ARGS="-Dnewt.debug=all"
# D_ARGS="-Dnewt.debug.Window -Dnewt.debug.Display -Dnewt.debug.EDT"
# D_ARGS="-Dnewt.debug.EDT -Dnewt.debug.Window"
# D_ARGS="-Dsun.awt.disableMixing=true -Dnewt.debug.EDT"
D_ARGS="-Dnewt.debug.EDT -Dnativewindow.debug.ToolkitLock.TraceLock"
# D_ARGS="-Djogamp.debug.Lock.TraceLock"
# D_ARGS="-Dnewt.debug.Display"
# D_ARGS="-Djogl.debug.Animator -Dnewt.debug.Window -Dnewt.debug.Display"
# D_ARGS="-Dnewt.debug.Window -Dnewt.debug.Display -Dnewt.test.Window.reparent.incompatible=true"
# D_ARGS="-Dnewt.debug.Window -Dnewt.debug.TestEDTMainThread"
# D_ARGS="-Dnewt.debug.TestEDTMainThread"
# D_ARGS="-Djogl.debug=all -Djogl.debug.DynamicLookup=true -Djogamp.debug.NativeLibrary=true"
# D_ARGS="-Djogl.debug=all"
# D_ARGS="-Djogamp.debug.JNILibLoader=true -Djogamp.debug.NativeLibrary=true -Djogamp.debug.NativeLibrary.Lookup=true -Djogl.debug.GLProfile=true"

rm -f java-run.log

# export LIBGL_DRIVERS_PATH=/usr/lib/fglrx/dri:/usr/lib32/fglrx/dri
# export LIBGL_DEBUG=verbose
which java 2>&1 | tee -a java-run.log
java -version 2>&1 | tee -a java-run.log
echo LIBXCB_ALLOW_SLOPPY_LOCK: $LIBXCB_ALLOW_SLOPPY_LOCK 2>&1 | tee -a java-run.log
echo LIBGL_DRIVERS_PATH: $LIBGL_DRIVERS_PATH 2>&1 | tee -a java-run.log
echo LIBGL_DEBUG: $LIBGL_DEBUG 2>&1 | tee -a java-run.log
echo java $X_ARGS $D_ARGS $* 2>&1 | tee -a java-run.log
java $X_ARGS $D_ARGS $* 2>&1 | tee -a java-run.log

