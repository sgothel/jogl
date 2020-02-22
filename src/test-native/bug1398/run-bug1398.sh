#! /bin/bash

# JVM_JLI_LIB=/Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/MacOS/libjli.dylib
# JVM_JLI_LIB=/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/MacOS/libjli.dylib
JVM_JLI_LIB=/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/MacOS/libjli.dylib

#JOGAMP_VERSION=v2.3.2
#JOGAMP_VERSION=v2.2.4

# This one is expected to work ..
function run_test_sdk1011() 
{
if [ -z "$JOGAMP_VERSION" ] ; then
    ./Bug1398LauncherSDK1011 -jvmlibjli $JVM_JLI_LIB \
        -classpath ".:/Users/jogamp/projects/JogAmp/gluegen/build/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/jogl/build/jar/jogl-all.jar" \
        -libpath "/Users/jogamp/projects/JogAmp/gluegen/build/obj:/Users/jogamp/projects/JogAmp/jogl/build/lib"
else
    ./Bug1398LauncherSDK1011 -jvmlibjli $JVM_JLI_LIB \
        -classpath ".:/Users/jogamp/projects/JogAmp/builds/$JOGAMP_VERSION/jogamp-all-platforms/jar/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/builds/v2.3.2/jogamp-all-platforms/jar/jogl-all.jar" \
        -libpath "/Users/jogamp/projects/JogAmp/builds/$JOGAMP_VERSION/jogamp-all-platforms/lib/macosx-universal"
fi
}

# This one is expected to crash @ -[NSOpenGLContext setView:]
function run_test_sdk1015() 
{
if [ -z "$JOGAMP_VERSION" ] ; then
    ./Bug1398LauncherSDK1015 -jvmlibjli $JVM_JLI_LIB \
        -classpath ".:/Users/jogamp/projects/JogAmp/gluegen/build/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/jogl/build/jar/jogl-all.jar" \
        -libpath "/Users/jogamp/projects/JogAmp/gluegen/build/obj:/Users/jogamp/projects/JogAmp/jogl/build/lib"
else

    ./Bug1398LauncherSDK1015 -jvmlibjli $JVM_JLI_LIB \
        -classpath ".:/Users/jogamp/projects/JogAmp/builds/$JOGAMP_VERSION/jogamp-all-platforms/jar/gluegen-rt.jar:/Users/jogamp/projects/JogAmp/builds/v2.3.2/jogamp-all-platforms/jar/jogl-all.jar" \
        -libpath "/Users/jogamp/projects/JogAmp/builds/$JOGAMP_VERSION/jogamp-all-platforms/lib/macosx-universal"

fi
}

run_test_sdk1011 2>&1 | tee run-bug1398-sdk1011.log

run_test_sdk1015 2>&1 | tee run-bug1398-sdk1015.log

