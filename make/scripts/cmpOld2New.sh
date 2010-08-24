#!/bin/bash

dirscript=`dirname $0`

dirold=../build-x86_64-old/jogl/gensrc/classes/javax/media/opengl/
dirnew=../build-x86_64/jogl/gensrc/classes/javax/media/opengl/
dircmp=cmp-old2new

rm -rf $dircmp
mkdir -p $dircmp

for i in GL GL2ES1 GL2ES2 GLES1 GLES2 GL2GL3 GL2 GL3 GL3bc GL4 GL4bc ; do
    echo
    echo processing $i
    awk -f $dirscript/strip-c-comments.awk $dirold/$i.java | sort -u > $dircmp/$i-old.java
    echo created $dircmp/$i-old.java
    awk -f $dirscript/strip-c-comments.awk $dirnew/$i.java | sort -u > $dircmp/$i-new.java
    echo created $dircmp/$i-new.java
    diff -Nurdw $dircmp/$i-old.java $dircmp/$i-new.java > $dircmp/$i-diff.txt
    echo created $dircmp/$i-diff.txt
done
    
