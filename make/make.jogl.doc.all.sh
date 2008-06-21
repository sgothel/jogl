#! /bin/sh

. ../../setenv-build-jogl.sh

ant -v javadoc.dev.all $* 2>&1 | tee make.jogl.doc.all.log
