#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogl-x86_64.sh
fi

LOGF=junit.jogl.all.linux-x86_64-headless.log
rm -f $LOGF

export SOURCE_LEVEL=1.8
export TARGET_LEVEL=1.8
export TARGET_RT_JAR=/opt-share/jre1.8.0_212/lib/rt.jar

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

# BUILD_ARCHIVE=true \
ant  \
    -Dsetup.noAWT=true \
    -Dsetup.noSWT=true \
    -Drootrel.build=build-x86_64 \
    -Djunit.run.arg0="--illegal-access=warn" \
    junit.run 2>&1 | tee -a $LOGF

