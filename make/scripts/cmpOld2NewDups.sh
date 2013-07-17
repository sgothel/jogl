#! /bin/bash

dircmp=cmp-old2new

GLFILES="$dircmp/GL2ES1-new.java \
 $dircmp/GLES1-new.java \
 $dircmp/GL2ES2-new.java \
 $dircmp/GLES2-new.java \
 $dircmp/GL3ES3-new.java \
 $dircmp/GL2GL3-new.java \
 $dircmp/GL2-new.java \
 $dircmp/GL3-new.java \
 $dircmp/GL3bc-new.java \
 $dircmp/GL4ES3-new.java \
 $dircmp/GLES3-new.java \
 $dircmp/GL4-new.java \
 $dircmp/GL4bc-new.java \
 $dircmp/GL-new.java"

GL4FILES="$dircmp/GL2ES2-new.java \
 $dircmp/GL2ES3-new.java \
 $dircmp/GL3ES3-new.java \
 $dircmp/GL3-new.java \
 $dircmp/GL4ES3-new.java \
 $dircmp/GL4-new.java \
 $dircmp/GL-new.java"

GLES3FILES="$dircmp/GL2ES2-new.java \
 $dircmp/GL2ES3-new.java \
 $dircmp/GL3ES3-new.java \
 $dircmp/GL4ES3-new.java \
 $dircmp/GLES3-new.java \
 $dircmp/GL-new.java"

GLES1FILES="$dircmp/GL2ES1-new.java \
 $dircmp/GLES1-new.java \
 $dircmp/GL-new.java"

GLES2FILES="$dircmp/GL2ES2-new.java \
 $dircmp/GLES2-new.java \
 $dircmp/GL-new.java"

GL2ES2FILES="$dircmp/GL2ES2-new.java \
 $dircmp/GLES2-new.java \
 $dircmp/GL2-new.java \
 $dircmp/GL-new.java"

GL3ES3FILES="$dircmp/GL2ES2-new.java \
 $dircmp/GL3ES3-new.java \
 $dircmp/GL4ES3-new.java \
 $dircmp/GLES3-new.java \
 $dircmp/GL3-new.java \
 $dircmp/GL4-new.java \
 $dircmp/GL-new.java"

echo Duplicates GL GL2ES1 GL2ES2 GL2GL3 GL3 GL3bc GL4 GL4bc > $dircmp/GL4Files.dups
cat $GL4FILES | sort | uniq -d >> $dircmp/GL4Files.dups

echo Duplicates GL GL2ES1 GLES1 > $dircmp/GLES1Files.dups
cat $GLES1FILES | sort | uniq -d >> $dircmp/GLES1Files.dups

echo Duplicates GL GL2ES2 GLES2 > $dircmp/GLES2Files.dups
cat $GLES2FILES | sort | uniq -d >> $dircmp/GLES2Files.dups

echo Duplicates GL GL2ES2 GL3ES3 GLES3 > $dircmp/GLES3Files.dups
cat $GLES3FILES | sort | uniq -d >> $dircmp/GLES3Files.dups

echo Duplicates GL GL2 GL2ES2 GLES2 > $dircmp/GL2ES2Files.dups
cat $GL2ES2FILES | sort | uniq -d >> $dircmp/GL2ES2Files.dups

echo Duplicates GL GL3 GL2ES2 GL2ES3 GLES3 > $dircmp/GL3ES3Files.dups
cat $GL3ES3FILES | sort | uniq -d >> $dircmp/GL3ES3Files.dups

##
##

echo Duplicates GL3ES3 GLES3 > $dircmp/GLES3-GL3ES3.dups
cat $dircmp/GLES3-new.java $dircmp/GL3ES3-new.java | sort | uniq -d >> $dircmp/GLES3-GL3ES3.dups

echo Diff GL3ES3 GLES3 > $dircmp/GLES3-GL3ES3.diff
diff -Nurdw $dircmp/GLES3-new.java $dircmp/GL3ES3-new.java >> $dircmp/GLES3-GL3ES3.diff

##
##

echo Duplicates GL2GL3 GLES3 > $dircmp/GLES3-GL2GL3.dups
cat $dircmp/GLES3-new.java $dircmp/GL2GL3-new.java | sort | uniq -d >> $dircmp/GLES3-GL2GL3.dups

echo Diff GL2GL3 GLES3 > $dircmp/GLES3-GL2GL3.diff
diff -Nurdw $dircmp/GLES3-new.java $dircmp/GL2GL3-new.java >> $dircmp/GLES3-GL2GL3.diff

##
##

echo Duplicates GL2ES2 GLES3 > $dircmp/GLES3-GL2ES2.dups
cat $dircmp/GLES3-new.java $dircmp/GL2ES2-new.java | sort | uniq -d >> $dircmp/GLES3-GL2ES2.dups

echo Diff GL2ES2 GLES3 > $dircmp/GLES3-GL2ES2.diff
diff -Nurdw $dircmp/GLES3-new.java $dircmp/GL2ES2-new.java >> $dircmp/GLES3-GL2ES2.diff

##
##

echo Duplicates GL2GL3 GL3ES3 > $dircmp/GL3ES3-GL2GL3.dups
cat $dircmp/GL3ES3-new.java $dircmp/GL2GL3-new.java | sort | uniq -d >> $dircmp/GL3ES3-GL2GL3.dups

echo Diff GL2GL3 GL3ES3 > $dircmp/GL3ES3-GL2GL3.diff
diff -Nurdw $dircmp/GL3ES3-new.java $dircmp/GL2GL3-new.java >> $dircmp/GL3ES3-GL2GL3.diff

##
##

echo Duplicates GL2ES2 GL3ES3 > $dircmp/GL3ES3-GL2ES2.dups
cat $dircmp/GL3ES3-new.java $dircmp/GL2ES2-new.java | sort | uniq -d >> $dircmp/GL3ES3-GL2ES2.dups

echo Diff GL2ES2 GL3ES3 > $dircmp/GL3ES3-GL2ES2.diff
diff -Nurdw $dircmp/GL3ES3-new.java $dircmp/GL2ES2-new.java >> $dircmp/GL3ES3-GL2ES2.diff

##
##

