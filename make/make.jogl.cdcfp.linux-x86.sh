#! /bin/sh

. ../../setenv-build-jogl.sh

#    -Dc.compiler.debug=true 
#    -Djavacdebug="false"
#    -Djavacdebuglevel=""

ant -v \
    -Djavacdebug="false" \
    -Djavacdebuglevel="none" \
    -Djogl.cdcfp=true \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxX86=true \
    -DisX11=true \
    $* 2>&1 | tee make.jogl.cdcfp.linux-x86.log
