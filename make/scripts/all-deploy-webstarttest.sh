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
    echo    sh $0 file:////usr/local/projects/JOGL/webstart ../build-x86_64 ../../webstart
    echo    sh $0 http://domain.org/jogl/webstart ../build-win32 ../../webstart
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
#
# repack it .. so the signed jars can be pack200'ed
# sh scripts/deploy-jars-repack200.sh $wsdir
#
# sign it
# sh scripts/deploy-jars-sign.sh $wsdir KEY_STORE_FILE STORE_PASSWORD SOME_ARGUMENT
#
# pack200
# sh scripts/deploy-jars-pack200.sh $wsdir
#
sh scripts/deploy-jnlp-webstarttest.sh $url $joglbuilddir $wsdir
#
# In case you don't sign it ..
#
# sh scripts/deploy-jnlp-webstarttest-filter.sh $wsdir
#
# Add to HOME/.java.policy
#
# grant codeBase "file:////usr/local/projects/JOGL/webstart/-" {
#      permission java.security.AllPermission;
# };

