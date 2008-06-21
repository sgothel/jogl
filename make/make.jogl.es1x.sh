#! /bin/sh

. ../../setenv-build-jogl.sh

ant -v -Djogl.es1=1 $* 2>&1 | tee make.jogl.es1x.log
