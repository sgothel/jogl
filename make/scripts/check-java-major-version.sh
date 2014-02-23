#! /bin/bash

TDIR=`pwd`

function dump_version() {
    echo -n "$1: "
    javap -v $1 | grep 'major version'
}

function dump_versions() {
    cd $1
    #dump_version jogamp.common.Debug
    for i in `find . -name '*.class'` ; do 
      dump_version `echo $i | sed -e 's/\//./g' -e 's/\.class//g'`
    done
    cd $TDIR
}

function do_it() {
    dump_versions $1/nativewindow/classes
    dump_versions $1/jogl/classes
    dump_versions $1/newt/classes
    dump_versions $1/test/build/classes
}

do_it $1 2>&1 | tee check-java-major-version.log
echo 
echo VERSIONS found:
echo
grep 'major version' check-java-major-version.log | sort -u

