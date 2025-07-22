#! /bin/bash

TDIR=`pwd`

function dump_version() {
    echo -n "$1: "
    javap -v $1 | grep 'major version'
}

function dump_versions() {
    cd $1
    #dump_version jogamp.common.Debug
    javap -v `find . -name '*.class'` | grep -e '^Classfile' -e 'major version'
    #for i in `find . -name '*.class'` ; do 
    #  dump_version `echo $i | sed -e 's/^\.\///g' -e 's/\//./g' -e 's/\.class//g'`
    #done
    cd $TDIR
}

function do_it() {
    if [ -e $1/nativewindow/classes -a -e $1/jogl/classes -a -e $1/newt/classes -a -e $1/test/build/classes ] ; then
        # regular build
        dump_versions $1/nativewindow/classes
        dump_versions $1/jogl/classes
        dump_versions $1/newt/classes
        dump_versions $1/test/build/classes
    elif [ -e $1/com -a -e $1/gluegen -a -e $1/jogamp ] ; then
        # inflated fat platform jar
        dump_versions $1/com
        dump_versions $1/gluegen
        dump_versions $1/jogamp
    else
        echo "No class files as expected"
        exit 1
    fi
}

do_it $1 2>&1 | tee check-java-major-version.log
echo 
echo VERSIONS found:
echo
grep 'major version' check-java-major-version.log | sort -u

