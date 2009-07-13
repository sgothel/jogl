#! /bin/sh

if [ -e /devtools/etc/profile.ant ] ; then
    . /devtools/etc/profile.ant
fi


#    -Dc.compiler.debug=true 
#    -Dbuild.noarchives=true

ant \
    -Dbuild.noarchives=true \
    -Dsetup.cdcfp=true \
    -Drootrel.build=build-cdcfp-macosx \
    $* 2>&1 | tee make.jogl.cdcfp.macosx.log
