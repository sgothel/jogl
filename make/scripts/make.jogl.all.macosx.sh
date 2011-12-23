#! /bin/sh

if [ -e /opt-share/etc/profile.ant ] ; then
    . /opt-share/etc/profile.ant
fi


#    -Dc.compiler.debug=true \
#    -Djavacdebug="true" \
#    -Djavacdebuglevel="source,lines,vars" \

ant \
    -Drootrel.build=build-macosx \
    $* 2>&1 | tee make.jogl.all.macosx.log
