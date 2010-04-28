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

mkdir -p DLLS
mv *natives*.jar DLLS/

for i in *.jar ; do
    echo pack200 -E9 $i.pack.gz $i
    pack200 -E9 $i.pack.gz $i
done

mv DLLS/* .

rm -rf DLLS

cd $THISDIR

