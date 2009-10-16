#! /bin/sh

joglbuilddir=$1
shift 

wsdir=$1
shift 

if [ -z "$joglbuilddir" -o -z "$wsdir" ] ; then
    echo usage $0 jogl-builddir webstartdir
    exit 1
fi

if [ ! -e $joglbuilddir ] ; then
    echo $joglbuilddir does not exist
    exit 1
fi

if [ ! -e $wsdir ] ; then
    echo $wsdir does not exist
    exit 1
fi

builddirbase=`basename $joglbuilddir`
joglroot=`dirname $joglbuilddir`
gluegenroot=$joglroot/../gluegen
demosroot=$joglroot/../jogl-demos

jnlpdir_gluegen=$gluegenroot/jnlp-files
jnlpdir_jogl=$joglroot/jnlp-files
jnlpdir_demos=$demosroot/jnlp-files

if [ ! -e $jnlpdir_gluegen ] ; then
    echo $jnlpdir_gluegen does not exist
    exit 1
fi

if [ ! -e $jnlpdir_jogl ] ; then
    echo $jnlpdir_jogl does not exist
    exit 1
fi

if [ ! -e $jnlpdir_demos ] ; then
    echo $jnlpdir_demos does not exist
    exit 1
fi

cp -v $gluegenroot/$builddirbase/*.jar $wsdir
cp -v $gluegenroot/$builddirbase/obj/lib*.so $wsdir

cp -v $joglbuilddir/nativewindow/*.jar $wsdir
cp -v $joglbuilddir/jogl/*.jar $wsdir
cp -v $joglbuilddir/newt/*.jar $wsdir

cp -v $joglbuilddir/nativewindow/obj/lib*.so $wsdir
cp -v $joglbuilddir/jogl/obj/lib*.so $wsdir
cp -v $joglbuilddir/newt/obj/lib*.so $wsdir

cp -v $demosroot/$builddirbase/*.jar $wsdir

