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
fi

if [ -z "$1" ] ; then
    echo JOGL BUILD DIR missing
    print_usage
    exit
fi

. $scriptdir/setenv-jogl.sh $1 JOGL_ALLALL
shift

MOSX=0
uname -a | grep -i Darwin && MOSX=1

# D_ARGS="-Dgluegen.debug.ProcAddressHelper=true -Dgluegen.debug.NativeLibrary=true -Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all"
# D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all -Djogl.debug.GLSLState"
# D_ARGS="-Dnativewindow.debug.X11Util=true -Djogl.debug.GLDrawableFactory=true"
# D_ARGS="-Dnativewindow.debug.X11Util=true"
# D_ARGS="-Dnewt.debug=all"

echo java $X_ARGS $D_ARGS $* 2>&1 | tee java-run.log
java $X_ARGS $D_ARGS $* 2>&1 | tee -a java-run.log
