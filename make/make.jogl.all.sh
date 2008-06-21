#! /bin/sh

. ../../setenv-build-jogl.sh

ant -v $* 2>&1 | tee make.jogl.all.log
