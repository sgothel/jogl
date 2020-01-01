#! /bin/sh

SDIR=`dirname $0` 

$SDIR/make.jogl.all.macosx.sh && \
$SDIR/make.jogl.all.ios.amd64.sh && \
$SDIR/make.jogl.all.ios.arm64.sh

# $SDIR/make.jogl.all.macosx.sh
# $SDIR/make.jogl.all.ios.amd64.sh
# $SDIR/make.jogl.all.ios.arm64.sh
# $SDIR/make.jogl.all.win32.bat
# $SDIR/make.jogl.all.win64.bat
# $SDIR/make.jogl.all.linux-ppc64le.sh
# $SDIR/make.jogl.all.linux-armv6hf.sh
# $SDIR/make.jogl.all.linux-aarch64.sh
