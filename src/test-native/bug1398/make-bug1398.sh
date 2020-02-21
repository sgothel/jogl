#! /bin/bash

set -x

USE_232=1
ok=0

if [ $USE_232 -eq 0 ] ; then
    xcrun clang -x objective-c -framework Cocoa \
        -DCLASSPATH="\".:/Users/jogamp/projects/JogAmp/gluegen/build/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/jogl/build/jar/jogl-all.jar\"" \
        -DLIBPATH="\"/Users/jogamp/projects/JogAmp/gluegen/build/obj:/Users/jogamp/projects/JogAmp/jogl/build/lib\"" \
        -o Bug1398macOSContextOpsOnMainThread Bug1398macOSContextOpsOnMainThread.c \
        && ok=1
else
    xcrun clang -x objective-c -framework Cocoa \
        -DCLASSPATH="\".:/Users/jogamp/projects/JogAmp/builds/v2.3.2/jogamp-all-platforms/jar/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/builds/v2.3.2/jogamp-all-platforms/jar/jogl-all.jar\"" \
        -DLIBPATH="\"/Users/jogamp/projects/JogAmp/builds/v2.3.2/jogamp-all-platforms/lib/macosx-universal\"" \
        -o Bug1398macOSContextOpsOnMainThread Bug1398macOSContextOpsOnMainThread.c \
        && ok=1
fi

if [ $ok -eq 1 ] ; then
	javac -source 1.8 -target 1.8 -classpath ../../../../gluegen/build/gluegen-rt.jar:../../../build/jar/jogl-all.jar RedSquareES2.java Bug1398macOSContextOpsOnMainThread.java
fi

# ./Bug1398macOSContextOpsOnMainThread /Users/jogamp/projects/JogAmp/gluegen/build/obj:/Users/jogamp/projects/JogAmp/jogl/build/lib/lib
