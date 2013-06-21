#! /bin/sh

if [ -e /opt-share/etc/profile.ant ] ; then
    . /opt-share/etc/profile.ant
fi


#    -Dc.compiler.debug=true \
#    -Djavacdebug="true" \
#    -Djavacdebuglevel="source,lines,vars" \
#
#    -Dtarget.sourcelevel=1.6 \
#    -Dtarget.targetlevel=1.6 \
#    -Dtarget.rt.jar=/opt-share/jre1.6.0_30/lib/rt.jar \

JAVA_HOME=`/usr/libexec/java_home -version 1.7`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Dtarget.sourcelevel=1.6 \
    -Dtarget.targetlevel=1.6 \
    -Dtarget.rt.jar=/opt-share/jre1.6.0_30/lib/rt.jar \
    -Drootrel.build=build-macosx-java7 \
    $* 2>&1 | tee make.jogl.all.macosx-java7.log
