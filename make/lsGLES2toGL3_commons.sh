#! /bin/sh

BUILDDIR=$1
shift
if [ -z "$BUILDDIR" ] ; then
    echo "usage $0 <BUILDDIR>"
    exit 1
fi

idir=$BUILDDIR/jogl/gensrc/classes/javax/media/opengl

echo GLES2 to GL3 enums
sort $idir/GLES2.java $idir/GL3.java $idir/GL2ES2.java | uniq -d | grep GL_ | awk ' { print $5 } '

echo GLES2 to GL3 functions
sort $idir/GLES2.java $idir/GL3.java $idir/GL2ES2.java | uniq -d | grep "public [a-z0-9_]* gl"
