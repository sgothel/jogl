#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh
fi

LOGF=junit.jogl.all.linux-x86_64-console.log
rm -f $LOGF

export SOURCE_LEVEL=1.8
export TARGET_LEVEL=1.8
export TARGET_RT_JAR=/opt-share/jre1.8.0_212/lib/rt.jar

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

#TARGET=junit.run
TARGET=junit.run.console

# BUILD_ARCHIVE=true \
ant  \
    -Dsetup.noAWT=true \
    -Dsetup.noSWT=true \
    -Drootrel.build=build-x86_64 \
    $TARGET 2>&1 | tee -a $LOGF

