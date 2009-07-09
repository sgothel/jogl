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

ant -v \
    -Dbuild.noarchives=true \
    -Drootrel.build=build-cdcfp-x86 \
    -Djogl.cdcfp=true \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxX86=true \
    -DisX11=true \
    -DuseKD=true \
    -DuseOpenMAX=true \
    $* 2>&1 | tee make.jogl.cdcfp.linux-x86.log
