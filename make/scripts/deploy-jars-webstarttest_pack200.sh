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
mv *linux*.jar DLLS/
mv *windows*.jar DLLS/
mv *macosx*.jar DLLS/

mkdir -p JAVAS
mv *.jar JAVAS

cd JAVAS

for i in *.jar ; do
    echo pack200 -E9 $i.pack.gz $i
    pack200 -E9 $i.pack.gz $i
done

cd $wsdir

mv JAVAS/* .
mv DLLS/* .

rm -rf JAVAS DLLS

cd $THISDIR

