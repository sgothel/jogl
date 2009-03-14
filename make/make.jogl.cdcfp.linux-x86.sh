#! /bin/sh

. ../../setenv-build-jogl.sh

#    -Dc.compiler.debug=true 
#
#    -Djavacdebug="false" \
#    -Djavacdebuglevel="none" \
#
#    -Djava.generate.skip=true \

ant -v \
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
