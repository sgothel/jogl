#! /bin/sh

if [ -e /devtools/etc/profile.ant ] ; then
    . /devtools/etc/profile.ant
fi


#    -Dc.compiler.debug=true 
#    -Dbuild.noarchives=true

BUILD_SUBDIR=build-cdcfp-macosx

ant \
    -Dbuild.noarchives=true \
    -Dsetup.cdcfp=true \
    -Drootrel.build=$BUILD_SUBDIR \
    $* 2>&1 | tee make.jogl.cdcfp.macosx.log

rm -rf ../$BUILD_SUBDIR/lib
mkdir -p ../$BUILD_SUBDIR/lib
for i in `find ../$BUILD_SUBDIR/ -name \*jnilib` ; do
    cp -v $i ../$BUILD_SUBDIR/lib/$(basename $i .jnilib).so
done
for i in `find ../../gluegen/$BUILD_SUBDIR/ -name \*jnilib` ; do
    cp -v $i ../$BUILD_SUBDIR/lib/$(basename $i .jnilib).so
done

