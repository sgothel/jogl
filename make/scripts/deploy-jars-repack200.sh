#! /bin/sh

wsdir=$1
shift 

if [ -z "$wsdir" ] ; then
    echo usage $0 webstartdir
    exit 1
fi

if [ ! -e $wsdir ] ; then
    echo $wsdir does not exist
    exit 1
fi

THISDIR=`pwd`

cd $wsdir

rm -rf orig-jars
mkdir -p orig-jars

for i in *.jar ; do
    cp -a $i orig-jars
    echo pack200 --repack $i
    pack200 --repack $i
done

cd $THISDIR

