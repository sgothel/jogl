#! /bin/sh

if [ -e /devtools/etc/profile.ant ] ; then
    . /devtools/etc/profile.ant
fi


#    -Dc.compiler.debug=true 
#    -Dbuild.noarchives=true

ant \
    -Dbuild.noarchives=true \
    -Djogl.cg=1 \
    -Drootrel.build=build-macosx \
    $* 2>&1 | tee make.jogl.all.macosx.log
