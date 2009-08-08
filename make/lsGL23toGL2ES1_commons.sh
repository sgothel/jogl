#! /bin/sh

BUILDDIR=$1
shift
if [ -z "$BUILDDIR" ] ; then
    echo "usage $0 <BUILDDIR>"
    exit 1
fi

idir=$BUILDDIR/jogl/gensrc/classes/javax/media/opengl

echo GL2GL3 to GL2ES1 enums
# sort $idir/GL2.java $idir/GL3.java $idir/GL2ES1.java $idir/GL2GL3.java | uniq -d | grep GL_ | awk ' { print $5 } '
sort $idir/GL2.java $idir/GL3.java $idir/GL2ES1.java $idir/GL2GL3.java | uniq -d | grep GL_ | grep -v "Part of <code>"

echo GL2GL3 to GL2ES1 functions
# sort $idir/GL2.java $idir/GL3.java $idir/GL2ES1.java $idir/GL2GL3.java | uniq -d | grep "public [a-z0-9_]* gl"
sort $idir/GL2.java $idir/GL3.java $idir/GL2ES1.java $idir/GL2GL3.java | uniq -d | grep "public [a-z0-9_]* gl"
