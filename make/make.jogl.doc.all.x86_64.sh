#! /bin/sh

. ../../setenv-build-jogl-x86_64.sh

ant -v  \
    -Dbuild.noarchives=true \
    -Drootrel.build=build-x86_64 \
    javadoc.spec javadoc javadoc.dev $* 2>&1 | tee make.jogl.doc.all.x86_64.log
