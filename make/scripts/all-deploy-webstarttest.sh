#! /bin/sh

if [ ! -e scripts -o ! -e ../make ] ; then
    echo start this script from JOGL/jogl/make
	exit 1
fi

url=$1
shift

joglbuilddir=$1
shift 

wsdir=$1
shift 

if [ -z "$url" -o -z "$joglbuilddir" -o -z "$wsdir" ] ; then
    echo Usage $0 codebase-url jogl-builddir webstartdir
    echo Examples
    echo    sh scripts/all-deploy-webstarttest.sh file:////usr/local/projects/JOGL/webstart ../build-x86_64 ../../webstart
    echo    sh scripts/all-deploy-webstarttest.sh http://domain.org/jogl/webstart ../build-win32 ../../webstart
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

sh scripts/deploy-jars-webstarttest.sh $joglbuilddir $wsdir
# sh scripts/deploy-jars-webstarttest_pack200.sh $wsdir
sh scripts/deploy-jnlp-webstarttest.sh $url $joglbuilddir $wsdir
sh scripts/deploy-jnlp-webstarttest-filter.sh $wsdir
