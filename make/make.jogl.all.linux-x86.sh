#! /bin/sh

. ../../setenv-build-jogl.sh

#    -Dc.compiler.debug=true 

ant -v \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxX86=true \
    -DisX11=true \
    $* 2>&1 | tee make.jogl.all.linux-x86.log
