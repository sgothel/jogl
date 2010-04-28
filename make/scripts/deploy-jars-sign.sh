#! /bin/sh

wsdir=$1
shift 

keystore=$1
shift 

storepass=$1
shift 

signarg=$1
shift 

if [ -z "$wsdir" -o -z "$keystore" -o -z "$storepass" ] ; then
    echo "usage $0 webstartdir pkcs12-keystore storepass [signarg]"
    exit 1
fi

if [ ! -e $wsdir ] ; then
    echo $wsdir does not exist
    exit 1
fi

if [ ! -e $keystore ] ; then
    echo $keystore does not exist
    exit 1
fi

THISDIR=`pwd`

cd $wsdir

rm -rf demo-jars
mkdir -p demo-jars
mv jogl.test.jar jogl-demos*jar demo-jars/

for i in *.jar ; do
    echo jarsigner -storetype pkcs12 -keystore $keystore $i \"$signarg\"
    jarsigner -storetype pkcs12 -keystore $THISDIR/$keystore -storepass $storepass $i "$signarg"
done

mv demo-jars/* .
rm -rf demo-jars

cd $THISDIR

