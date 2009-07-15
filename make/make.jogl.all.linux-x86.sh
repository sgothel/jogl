#! /bin/sh

if [ -e ../../setenv-build-jogl-x86.sh ] ; then
    . ../../setenv-build-jogl-x86.sh
fi


# -Djogl.cg=1 -Dx11.cg.lib=../../lib-linux-x86_64
#    -Dc.compiler.debug=true 
#    -DuseOpenMAX=true \
#    -Dbuild.noarchives=true

ant \
    -Djogl.cg=1 -Dx11.cg.lib=../../lib-linux-x86 \
    -Drootrel.build=build-x86 \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxX86=true \
    -DisX11=true \
    -DuseKD=true \
    -DuseOpenMAX=true \
    $* 2>&1 | tee make.jogl.all.linux-x86.log
