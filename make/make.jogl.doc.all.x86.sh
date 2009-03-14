#! /bin/sh

. ../../setenv-build-jogl-x86.sh

ant -v  \
    -Drootrel.build=build-x86\
    -DuseKD=true \
    javadoc.spec javadoc javadoc.dev.all $* 2>&1 | tee make.jogl.doc.all.x86.log
