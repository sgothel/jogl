#! /bin/sh

. ../../setenv-build-jogl.sh

ant $* 2>&1 | tee make.jogl.all.log
