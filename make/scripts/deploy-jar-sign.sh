#! /bin/sh

jarfile=$1
shift 

keystore=$1
shift 

storepass=$1
shift 

signarg=$1
shift 

if [ -z "$jarfile" -o -z "$keystore" -o -z "$storepass" ] ; then
    echo "usage $0 jarfile pkcs12-keystore storepass [signarg]"
    exit 1
fi

if [ ! -e $jarfile ] ; then
    echo $jarfile does not exist
    exit 1
fi

if [ ! -e $keystore ] ; then
    echo $keystore does not exist
    exit 1
fi

THISDIR=`pwd`

echo jarsigner -storetype pkcs12 -keystore $keystore $jarfile \"$signarg\"
jarsigner -storetype pkcs12 -keystore $THISDIR/$keystore -storepass $storepass $jarfile "$signarg"

cd $THISDIR

