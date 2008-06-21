#! /bin/sh

. ../../setenv-build-jogl.sh

ant -v -Djogl.cdcfp=1 -Djogl.es1=1 $* 2>&1 | tee make.jogl.cdc.es1x.log
