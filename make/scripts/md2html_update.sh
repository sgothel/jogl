#!/bin/sh

set -x

sdir=`dirname $(readlink -f $0)`
rdir=$sdir/../..

pandoc_md2html_local.sh $rdir/doc/OpenGL_API_Divergence.md             > $rdir/doc/OpenGL_API_Divergence.html
pandoc_md2html_local.sh $rdir/doc/Windows_Custom_OpenGL.md             > $rdir/doc/Windows_Custom_OpenGL.html
