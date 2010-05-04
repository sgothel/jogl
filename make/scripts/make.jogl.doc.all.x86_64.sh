#! /bin/sh

if [ -e ../../setenv-build-jogl-x86_64.sh ] ; then
    . ../../setenv-build-jogl-x86_64.sh
fi


ant -v  \
    -Dbuild.noarchives=true \
    -Drootrel.build=build-x86_64 \
    javadoc.spec javadoc javadoc.dev $* 2>&1 | tee make.jogl.doc.all.x86_64.log
