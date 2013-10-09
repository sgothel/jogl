#! /bin/sh

if [ -e /opt-share/etc/profile.ant ] ; then
    . /opt-share/etc/profile.ant
fi

JAVA_HOME=`/usr/libexec/java_home -version 1.7`
#JAVA_HOME=`/usr/libexec/java_home -version 1.6`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH


#    -Dc.compiler.debug=true \
#    -Djavacdebug="true" \
#    -Djavacdebuglevel="source,lines,vars" \
#

export SOURCE_LEVEL=1.6
export TARGET_LEVEL=1.6
export TARGET_RT_JAR=/opt-share/jre1.6.0_30/lib/rt.jar

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-macosx \
    $* 2>&1 | tee make.jogl.all.macosx.log
