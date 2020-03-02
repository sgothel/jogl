#! /bin/bash

set -x

JOGAMP_VERSION=v2.3.2
#JOGAMP_VERSION=v2.2.4

unset SDKROOT

CLASSPATH=".:/Users/jogamp/projects/JogAmp/builds/$JOGAMP_VERSION/jogamp-all-platforms/jar/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/builds/$JOGAMP_VERSION/jogamp-all-platforms/jar/jogl-all.jar"

#CLASSPATH=".:/Users/jogamp/projects/JogAmp/gluegen/build/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/jogl/build/jar/jogl-all.jar"

ok=0

# Default macosx10.15 SDK on XCode 11
xcrun --sdk macosx10.15 clang -x objective-c -framework Cocoa \
    -o Bug1398LauncherSDK1015 Bug1398Launcher.c \
    && ok=1

# Non-Default macosx10.11 SDK (JogAmp builds)
xcrun --sdk macosx10.11 clang -x objective-c -framework Cocoa \
    -o Bug1398LauncherSDK1011 Bug1398Launcher.c \
    && ok=1

if [ $ok -eq 1 ] ; then
	javac -source 1.8 -target 1.8 -classpath $CLASSPATH RedSquareES2.java Bug1398MainClass.java
fi

