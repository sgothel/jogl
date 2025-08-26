#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh ] ; then
    . $SDIR/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh
fi

#    -Dc.compiler.debug=true \
#    -Djavacdebuglevel="source,lines,vars" \

#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxAMD64=true \
#    -DisX11=true \
#
#    -Dsetup.addNativeOpenMAX=true \
#    -Dsetup.addNativeKD=true \
#
#    -Doculusvr.enabled=true \
#


LOGF=make.jogl.all.freebsd-x86_64.log
rm -f $LOGF

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

# BUILD_ARCHIVE=true \
ant  \
    -Drootrel.build=build-freebsd-x86_64 \
    $* 2>&1 | tee -a $LOGF

