#! /bin/sh

if [ -e /opt-share/etc/profile.ant ] ; then
    . /opt-share/etc/profile.ant
fi


#    -Dc.compiler.debug=true \
#    -Djavacdebug="true" \
#    -Djavacdebuglevel="source,lines,vars" \
#

JAVA_HOME=`/usr/libexec/java_home -version 1.7`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-macosx-java7 \
    $* 2>&1 | tee make.jogl.all.macosx-java7.log
