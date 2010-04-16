#! /bin/sh

function print_usage() {
    echo "Usage: $0 jogl-build-dir ..."
}

if [ -z "$1" ] ; then
    echo JOGL BUILD DIR missing
    print_usage
    return
fi

. ./setenv-jogl.sh $1
shift

MOSX=0
uname -a | grep -i Darwin && MOSX=1

if [ $MOSX -eq 1 ] ; then
    X_ARGS="-XstartOnFirstThread"
fi

# D_ARGS="-Dgluegen.debug.ProcAddressHelper=true -Dgluegen.debug.NativeLibrary=true -Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all"
# D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all -Djogl.debug.GLSLState"
# D_ARGS="-Dnativewindow.debug.X11Util=true -Djogl.debug.GLDrawableFactory=true"
# D_ARGS="-Dnativewindow.debug.X11Util=true"

java $X_ARGS -Djava.awt.headless=true $D_ARGS com.jogamp.newt.util.MainThread $* 2>&1 | tee java-run-newt.log
