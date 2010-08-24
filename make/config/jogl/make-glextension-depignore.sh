#!/bin/bash

thisdir=`pwd`
registry=/data/docs/ComputerIT/3D_AV_GFX/gl/registry/specs
classdirpos=9
dirdep=$thisdir/tmp_glext_dep

gl3ignores=$thisdir/gl-if-gl3-ignores.cfg
gl4ignores=$thisdir/gl-if-gl4-ignores.cfg

rm -rf $dirdep
mkdir -p $dirdep

cd $registry

# We allow OpenGL 3.0 within GL2
grep -RIl "^[ \t]*OpenGL 3\.[1234] is required" . > $dirdep/ext-req-3_x.txt
grep -RIl "^[ \t]*OpenGL 3\.[1234] and GLSL .* are required" . >> $dirdep/ext-req-3_x.txt
grep -RIl "^[ \t]*OpenGL 4\.. is required" . > $dirdep/ext-req-4_x.txt
grep -RIl "^[ \t]*OpenGL 4\.. and GLSL .* are required" . >> $dirdep/ext-req-4_x.txt

cd $thisdir

function dump_ignore_ext() {
    infile=$1
    for i in `cat $infile` ; do 
        class=`echo $i | awk -F '/' ' { print \$2 } ' `
        extname="GL_"$class"_"`basename $i .txt`
        echo IgnoreExtension $extname
    done
}

function dump_ignore_header() {
    echo "#"
    echo "# Generated Configuration File"
    echo "# Use make-glextension-depignore.sh to update!"
    echo "#"
    echo
}

function dump_ignore_ext_gl3() {
    echo "#"
    echo "# OpenGL 3.x dependencies"
    echo "#"
    echo "# We allow GL_VERSION_3_0 within GL2"
    echo "IgnoreExtension GL_VERSION_3_1"
    echo "IgnoreExtension GL_VERSION_3_2"
    echo "IgnoreExtension GL_VERSION_3_3"
    echo "IgnoreExtension GL_VERSION_3_4"
    dump_ignore_ext $dirdep/ext-req-3_x.txt
    echo
}

function dump_ignore_ext_gl4() {
    echo "#"
    echo "# OpenGL 4.x dependencies"
    echo "#"
    echo "IgnoreExtension GL_VERSION_4_0"
    echo "IgnoreExtension GL_VERSION_4_1"
    echo "IgnoreExtension GL_VERSION_4_2"
    dump_ignore_ext $dirdep/ext-req-4_x.txt
    echo
}

dump_ignore_header > $gl3ignores
dump_ignore_ext_gl3 >> $gl3ignores

dump_ignore_header > $gl4ignores
dump_ignore_ext_gl4 >> $gl4ignores

