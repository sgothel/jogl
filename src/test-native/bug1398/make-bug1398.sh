#! /bin/bash

set -x

JOGAMP_VERSION=v2.3.2
#JOGAMP_VERSION=v2.2.4

CLASSPATH=".:/Users/jogamp/projects/JogAmp/builds/$JOGAMP_VERSION/jogamp-all-platforms/jar/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/builds/$JOGAMP_VERSION/jogamp-all-platforms/jar/jogl-all.jar"

ok=0

xcrun clang -x objective-c -framework Cocoa \
    -o Bug1398macOSContextOpsOnMainThread Bug1398macOSContextOpsOnMainThread.c \
    && ok=1

if [ $ok -eq 1 ] ; then
	javac -source 1.8 -target 1.8 -classpath $CLASSPATH RedSquareES2.java Bug1398macOSContextOpsOnMainThread.java
fi

