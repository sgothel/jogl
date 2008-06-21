#! /bin/sh

. ../../setenv-build-jogl.sh

ant 2>&1 | tee make.jogl.gl2.x86_32.log
