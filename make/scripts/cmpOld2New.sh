#!/bin/bash

dirscript=`dirname $0`

dirold=../build-old/jogl/gensrc/classes/com/jogamp/opengl/
dirnew=../build/jogl/gensrc/classes/com/jogamp/opengl/
dircmp=cmp-old2new

rm -rf $dircmp
mkdir -p $dircmp

for i in GL GL2ES1 GLES1 GL2ES2 GLES2 GL2ES3 GL2GL3 GL2 GL3ES3 GL3 GL3bc GL4ES3 GLES3 GL4 GL4bc ; do
    echo
    echo processing $i
    awk -f $dirscript/strip-c-comments.awk $dirold/$i.java | sort -u > $dircmp/$i-old.java
    echo created $dircmp/$i-old.java
    awk -f $dirscript/strip-c-comments.awk $dirnew/$i.java | sort -u > $dircmp/$i-new.java
    echo created $dircmp/$i-new.java
    diff -Nurdw $dircmp/$i-old.java $dircmp/$i-new.java > $dircmp/$i-diff.txt
    echo created $dircmp/$i-diff.txt
done
    
