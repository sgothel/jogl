#! /bin/sh

jogamproot=$1
shift

builddir=$1
shift

if [ -z "$jogamproot" -o -z "$builddir" ] ; then
    echo Usage $0 jogamp_root builddir
    echo e.g. $0 JOGL build-win32
fi


rm -rf $jogamproot/gluegen/$builddir
rm -rf $jogamproot/jogl/$builddir
