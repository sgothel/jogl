#! /bin/sh

SDIR=`dirname $0` 

$SDIR/make.jogl.all.linux-armv6-cross.sh \
&& $SDIR/make.jogl.all.linux-armv6hf-cross.sh \
&& $SDIR/make.jogl.all.linux-x86_64.sh \
&& $SDIR/make.jogl.all.linux-x86.sh \
&& $SDIR/make.jogl.all.android-armv6-cross.sh \
