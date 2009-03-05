#! /bin/sh

. ../../setenv-build-jogl-x86.sh

#    -Dc.compiler.debug=true 

ant -v \
    -Drootrel.build=build-x86 \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxX86=true \
    -DisX11=true \
    $* 2>&1 | tee make.jogl.all.linux-x86.log
