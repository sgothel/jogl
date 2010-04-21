#! /bin/sh

if [ ! -e scripts -o ! -e ../make ] ; then
    echo start this script from JOGL/jogl/make
	exit 1
fi

SOURCE=$1
shift

wsdir=$1
shift 

if [ -z "$SOURCE" -o -z "$wsdir" ] ; then
    echo usage $0 source webstartdir
	echo source might be user@192.168.0.1:webstart/
    exit 1
fi

if [ ! -e $wsdir ] ; then
    echo $wsdir does not exist
    exit 1
fi

echo scp -v $SOURCE*natives* $wsdir
scp -v $SOURCE*natives* $wsdir
