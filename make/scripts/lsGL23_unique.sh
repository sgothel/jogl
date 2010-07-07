#! /bin/sh

BUILDDIR=$1
shift
if [ -z "$BUILDDIR" ] ; then
    echo "usage $0 <BUILDDIR>"
    exit 1
fi

idir=$BUILDDIR/jogl/gensrc/classes/javax/media/opengl

SOURCE="$idir/GL.java $idir/GLES2.java $idir/GL2ES2.java $idir/GL3.java $idir/GL2.java $idir/GL2GL3.java"

echo GL2GL3 to GL2 GL3 Gl2ES2 GLES2 GL defines
sort $SOURCE | uniq -u | grep GL_ | grep -v "Part of <code>" | awk ' { print $5 } '

echo GL2GL3 to GL2 GL3 Gl2ES2 GLES2 GL functions
sort $SOURCE | uniq -u | grep "public [a-z0-9_]* gl"
