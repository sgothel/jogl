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

cd $wsdir

rm -rf orig
mkdir orig

for i in *.jnlp ; do
    mv $i orig
    sed -e 's/<security>//g' -e 's/<\/security>//g' -e 's/<all-permissions\/>//g' orig/$i > $i
done

