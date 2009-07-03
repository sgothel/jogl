#! /bin/sh

. ../../setenv-build-jogl-x86_64.sh

# -Djogl.cg=1 -Dx11.cg.lib=../../lib-linux-x86_64
#    -Dc.compiler.debug=true 
#    -Dbuild.noarchives=true

ant \
    -Dbuild.noarchives=true \
    -Djogl.cg=1 -Dx11.cg.lib=../../lib-linux-x86_64 \
    -Dc.compiler.debug=true \
    -Drootrel.build=build-x86_64 \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxAMD64=true \
    -DisX11=true \
    $* 2>&1 | tee make.jogl.all.linux-x86_64.log
