#! /bin/sh

. /devtools/etc/profile.ant

#    -Dc.compiler.debug=true 
#    -Dbuild.noarchives=true

ant \
    -Dbuild.noarchives=true \
    -Djogl.cg=1 \
    -Drootrel.build=build-macosx \
    $* 2>&1 | tee make.jogl.all.macosx.log
