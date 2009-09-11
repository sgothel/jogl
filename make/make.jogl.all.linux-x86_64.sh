#! /bin/sh

if [ -e ../../setenv-build-jogl-x86_64.sh ] ; then
    . ../../setenv-build-jogl-x86_64.sh
fi

# -Djogl.cg=1 -Dx11.cg.lib=../../lib-linux-x86_64
#    -Dc.compiler.debug=true 
#    -Dbuild.noarchives=true

#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxAMD64=true \
#    -DisX11=true \
#    -Dbuild.noarchives=true \

ant  \
    -Dbuild.noarchives=true \
    -Djogl.cg=1 -Dx11.cg.lib=../../lib-linux-x86_64 \
    -Dc.compiler.debug=true \
    -Drootrel.build=build-x86_64 \
    -DuseKD=true \
    -DuseOpenMAX=true \
    $* 2>&1 | tee make.jogl.all.linux-x86_64.log
