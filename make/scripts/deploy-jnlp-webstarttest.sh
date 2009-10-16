#! /bin/sh

url=$1
shift

joglbuilddir=$1
shift 

wsdir=$1
shift 

if [ -z "$url" -o -z "$joglbuilddir" -o -z "$wsdir" ] ; then
    echo usage $0 codebase-url jogl-builddir webstartdir
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

cp -v $jnlpdir_demos/*.html $wsdir

uri_esc=`echo $url | sed 's/\//\\\\\//g'`
for j in $jnlpdir_gluegen/*.jnlp ; do
    jb=`basename $j`
    echo "processing $j to $wsdir/$jb"
    sed "s/CODEBASE_TAG/$uri_esc/g" \
        $j > $wsdir/$jb
done

for j in $jnlpdir_jogl/*.jnlp ; do
    jb=`basename $j`
    echo "processing $j to $wsdir/$jb"
    sed -e "s/JOGL_CODEBASE_TAG/$uri_esc/g" \
        -e "s/GLUEGEN_CODEBASE_TAG/$uri_esc/g" \
        $j > $wsdir/$jb
done

for j in $jnlpdir_demos/*.jnlp ; do
    jb=`basename $j`
    echo "processing $j to $wsdir/$jb"
    sed -e "s/DEMO_CODEBASE_TAG/$uri_esc/g" \
        -e "s/JOGL_CODEBASE_TAG/$uri_esc/g" \
        $j > $wsdir/$jb
done

