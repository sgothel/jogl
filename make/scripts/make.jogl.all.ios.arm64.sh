#! /bin/sh

if [ -e /usr/local/etc/profile.ant ] ; then
    . /usr/local/etc/profile.ant
fi

#    -Dc.compiler.debug=true 

# Force OSX SDK 10.6, if desired
# export SDKROOT=macosx10.6

export SDKROOT=iphoneos13.2
xcrun --show-sdk-path

JAVA_HOME=`/usr/libexec/java_home -version 11`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH
which java
java -version 

export SOURCE_LEVEL=1.8
export TARGET_LEVEL=1.8
export TARGET_RT_JAR=/usr/local/jre1.8.0_212/lib/rt.jar

export GLUEGEN_CPPTASKS_FILE=make/lib/gluegen-cpptasks-ios-aarch64.xml

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-ios-arm64 \
    $* 2>&1 | tee make.jogl.all.ios-arm64.log
