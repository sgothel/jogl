#! /bin/sh

if [ -e /opt-share/etc/profile.ant ] ; then
    . /opt-share/etc/profile.ant
fi

# Force OSX SDK 10.6, if desired
# export SDKROOT=macosx10.6

#JAVA_HOME=`/usr/libexec/java_home`
JAVA_HOME=`/usr/libexec/java_home -version 11`
#JAVA_HOME=`/usr/libexec/java_home -version 1.8`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH


#    -Dc.compiler.debug=true \
#    -Djavacdebug="true" \
#    -Djavacdebuglevel="source,lines,vars" \
#

export SOURCE_LEVEL=1.8
export TARGET_LEVEL=1.8
export TARGET_RT_JAR=/opt-share/jre1.8.0_212/lib/rt.jar

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

# BUILD_ARCHIVE=true \
ant \
    -Drootrel.build=build-macosx \
    -Djunit.run.arg0="--illegal-access=warn" \
    $* 2>&1 | tee make.jogl.all.macosx.log
