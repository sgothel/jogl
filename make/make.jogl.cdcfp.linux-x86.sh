#! /bin/sh

. ../../setenv-build-jogl.sh

#    -Dc.compiler.debug=true 
#    -Djavacdebug="false"
#    -Djavacdebuglevel=""
#    -Djava.generate.skip=true

ant -v \
    -Djavacdebug="false" \
    -Djavacdebuglevel="none" \
    -Djogl.cdcfp=true \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxX86=true \
    -DisX11=true \
    -DuseKD=true \
    $* 2>&1 | tee make.jogl.cdcfp.linux-x86.log
