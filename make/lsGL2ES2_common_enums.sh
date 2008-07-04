#! /bin/sh

idir=../build/gensrc/classes/javax/media/opengl

sort $idir/GL.java $idir/GL2ES2.java | uniq -d | grep GL_ | awk ' { print $5 } '
#sort $idir/GL.java | grep GL_ | awk ' { print $5 } '
