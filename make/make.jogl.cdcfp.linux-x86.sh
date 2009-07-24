#! /bin/sh

if [ -e ../../setenv-build-jogl-x86.sh ] ; then
    . ../../setenv-build-jogl-x86.sh
fi

#    -Dc.compiler.debug=true 
#
#    -Djavacdebug="false" \
#    -Djavacdebuglevel="none" \
#
#    -Djava.generate.skip=true \
#    -Dbuild.noarchives=true

BUILD_SUBDIR=build-cdcfp-x86

ant -v \
    -Dbuild.noarchives=true \
    -Drootrel.build=$BUILD_SUBDIR \
    -Dsetup.cdcfp=true \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxX86=true \
    -DisX11=true \
    -DuseOpenMAX=true \
    $* 2>&1 | tee make.jogl.cdcfp.linux-x86.log

rm -rf ../$BUILD_SUBDIR/lib
mkdir -p ../$BUILD_SUBDIR/lib
for i in `find ../$BUILD_SUBDIR/ -name \*so` ; do
    cp -v $i ../$BUILD_SUBDIR/lib/$(basename $i .so).so
done
for i in `find ../../gluegen/$BUILD_SUBDIR/ -name \*so` ; do
    cp -v $i ../$BUILD_SUBDIR/lib/$(basename $i .so).so
done

