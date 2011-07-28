#! /bin/sh

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxX86=true \
#    -DisX11=true \

ant \
    -Drootrel.build=build-linux-armv7 \
    -DuseKD=true \
    -DuseOpenMAX=true \
    -DuseBroadcomEGL=true \
    $* 2>&1 | tee make.jogl.all.linux-armv7.log
