#! /bin/sh

jogamproot=$1
shift

builddir=$1
shift

destination=$1
shift


if [ -z "$jogamproot" -o -z "$builddir" -o -z "$destination" ] ; then
    echo Usage $0 jogamp_root builddir destination
    echo e.g. $0 JOGL build-win32 /cygdrive/y/jogl/lib-jogl-win32/
    exit
fi

cp -v \
  $jogamproot/jogl/$builddir/jar/*natives* \
  $jogamproot/gluegen/$builddir/obj/*.dll \
  $jogamproot/jogl/$builddir/nativewindow/obj/*.dll \
  $jogamproot/jogl/$builddir/jogl/obj/*.dll \
  $jogamproot/jogl/$builddir/newt/obj/*.dll \
    $destination

