#! /bin/sh

BUILDDIR=$1
shift
if [ -z "$BUILDDIR" ] ; then
    echo "usage $0 <BUILDDIR>"
    exit 1
fi

idir=$BUILDDIR/jogl/gensrc/classes/javax/media/opengl

SOURCE="$idir/GL.java $idir/GL2ES1.java $idir/GL2ES2.java $idir/GLES1.java $idir/GLES2.java $idir/GL2GL3.java $idir/GL2.java $idir/GL3.java"

echo GL GL2ES1 GL2ES2 GLES1 GLES2 GL2GL3 GL2 GL3 defines
sort $SOURCE | uniq -d | grep GL_ | grep -v "Part of <code>"

echo GL GL2ES1 GL2ES2 GLES1 GLES2 GL2GL3 GL2 GL3 functions
sort $SOURCE | uniq -d | grep "public [a-z0-9_]* gl"
