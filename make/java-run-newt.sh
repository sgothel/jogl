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

java $X_ARGS -Djava.awt.headless=true com.jogamp.newt.util.MainThread $* 2>&1 | tee java-run-newt.log
