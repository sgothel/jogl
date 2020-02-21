#! /bin/bash

JVM_JLI_LIB=/Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/MacOS/libjli.dylib
# JVM_JLI_LIB=/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/MacOS/libjli.dylib

JOGAMP_VERSION=v2.3.2
#JOGAMP_VERSION=v2.2.4

if [ -z "$JOGAMP_VERSION" ] ; then
    ./Bug1398macOSContextOpsOnMainThread -jvmlibjli $JVM_JLI_LIB \
        -classpath ".:/Users/jogamp/projects/JogAmp/gluegen/build/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/jogl/build/jar/jogl-all.jar" \
        -libpath "/Users/jogamp/projects/JogAmp/gluegen/build/obj:/Users/jogamp/projects/JogAmp/jogl/build/lib"
else

    ./Bug1398macOSContextOpsOnMainThread -jvmlibjli $JVM_JLI_LIB \
        -classpath ".:/Users/jogamp/projects/JogAmp/builds/$JOGAMP_VERSION/jogamp-all-platforms/jar/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/builds/v2.3.2/jogamp-all-platforms/jar/jogl-all.jar" \
        -libpath "/Users/jogamp/projects/JogAmp/builds/$JOGAMP_VERSION/jogamp-all-platforms/lib/macosx-universal"

fi

