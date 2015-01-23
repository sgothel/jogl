#! /bin/sh

function print_usage() {
    echo "Usage: $0 jogl-build-dir [JOGL_PROFILE]"
}

if [ -z "$1" ] ; then
    echo JOGL BUILD DIR missing
    print_usage
    exit
fi

if [ -e /opt-share/etc/profile.ant ] ; then
    . /opt-share/etc/profile.ant
fi

JOGL_BUILDDIR="$1"
shift

if [ -z "$1" ] ; then
    JOGL_PROFILE=JOGL_ALL
else
    JOGL_PROFILE=$1
    shift
fi

THISDIR=`pwd`

if [ -e "$JOGL_BUILDDIR" ] ; then
    JOGL_DIR="$JOGL_BUILDDIR/.."
    JOGL_BUILDDIR_BASE=`basename "$JOGL_BUILDDIR"`
else
    echo JOGL_BUILDDIR "$JOGL_BUILDDIR" not exist or not given
    print_usage
    exit
fi

gpf=`find ../../gluegen/make -name jogamp-env.xml`
if [ -z "$gpf" ] ; then
    gpf=`find .. -name jogamp-env.xml`
fi
if [ -z "$gpf" ] ; then
    echo GLUEGEN_BUILDDIR not found
    print_usage
    exit
fi

GLUEGEN_DIR=`dirname $gpf`/..
GLUEGEN_BUILDDIR="$GLUEGEN_DIR"/"$JOGL_BUILDDIR_BASE"
if [ ! -e "$GLUEGEN_BUILDDIR" ] ; then
    echo GLUEGEN_BUILDDIR "$GLUEGEN_BUILDDIR" does not exist
    print_usage
    exit
fi
GLUEGEN_JAR="$GLUEGEN_BUILDDIR"/gluegen-rt.jar
GLUEGEN_ALT_JAR="$GLUEGEN_BUILDDIR"/gluegen-rt-alt.jar
GLUEGEN_TESTUTIL_JAR="$GLUEGEN_BUILDDIR"/gluegen-test-util.jar
GLUEGEN_OS="$GLUEGEN_BUILDDIR"/obj
JUNIT_JAR="$GLUEGEN_DIR"/make/lib/junit.jar
SEMVER_JAR="$GLUEGEN_DIR"/make/lib/semantic-versioning/semver.jar

joalpf=`find ../../joal -name joal.iml`
if [ -z "$joalpf" ] ; then
    joalpf=`find .. -name joal.iml`
fi
if [ -z "$joalpf" ] ; then
    echo JOAL_BUILDDIR not found
    print_usage
    exit
fi

JOAL_DIR=`dirname $joalpf`
JOAL_BUILDDIR="$JOAL_DIR"/"$JOGL_BUILDDIR_BASE"
if [ ! -e "$JOAL_BUILDDIR" ] ; then
    echo JOAL_BUILDDIR "$JOAL_BUILDDIR" does not exist
    print_usage
    exit
fi
JOAL_JAR="$JOAL_BUILDDIR"/jar/joal.jar

if [ -z "$ANT_PATH" ] ; then
    ANT_PATH=$(dirname $(dirname $(which ant)))
    if [ -e $ANT_PATH/lib/ant.jar ] ; then
        export ANT_PATH
        echo autosetting ANT_PATH to $ANT_PATH
    fi
fi
if [ -z "$ANT_PATH" ] ; then
    echo ANT_PATH does not exist, set it
    print_usage
    exit
fi
ANT_JARS=$ANT_PATH/lib/ant.jar:$ANT_PATH/lib/ant-junit.jar

echo GLUEGEN BUILDDIR: "$GLUEGEN_BUILDDIR"
echo JOAL BUILDDIR: "$JOAL_BUILDDIR"
echo JOGL DIR: "$JOGL_DIR"
echo JOGL BUILDDIR: "$JOGL_BUILDDIR"
echo JOGL BUILDDIR BASE: "$JOGL_BUILDDIR_BASE"
echo JOGL PROFILE: "$JOGL_PROFILE"

J2RE_HOME=$(dirname $(dirname $(which java)))
JAVA_HOME=$(dirname $(dirname $(which javac)))
CP_SEP=:

. "$JOGL_DIR"/etc/profile.jogl $JOGL_PROFILE "$JOGL_BUILDDIR"

LIB=$THISDIR/lib

JOGAMP_ALL_AWT_CLASSPATH=.:"$GLUEGEN_JAR":"$JOAL_JAR":"$JOGL_ALL_AWT_CLASSPATH":"$SWT_CLASSPATH":"$JUNIT_JAR":"$ANT_JARS":"$SEMVER_JAR":"$GLUEGEN_TESTUTIL_JAR"
JOGAMP_ALL_NOAWT_CLASSPATH=.:"$GLUEGEN_JAR":"$JOAL_JAR":"$JOGL_ALL_NOAWT_CLASSPATH":"$SWT_CLASSPATH":"$JUNIT_JAR":"$ANT_JARS":"$SEMVER_JAR":"$GLUEGEN_TESTUTIL_JAR"
JOGAMP_MOBILE_CLASSPATH=.:"$GLUEGEN_JAR":"$JOAL_JAR":"$JOGL_MOBILE_CLASSPATH":"$SWT_CLASSPATH":"$JUNIT_JAR":"$ANT_JARS":"$SEMVER_JAR":"$GLUEGEN_TESTUTIL_JAR"
JOGAMP_ATOMICS_NOAWT_CLASSPATH=.:"$GLUEGEN_JAR":"$JOAL_JAR":"$JOGL_ATOMICS_NOAWT_CLASSPATH":"$SWT_CLASSPATH":"$JUNIT_JAR":"$ANT_JARS":"$SEMVER_JAR":"$GLUEGEN_TESTUTIL_JAR"
JOGAMP_ALL_NOAWT_ALT_CLASSPATH=.:"$GLUEGEN_ALT_JAR":"$JOAL_JAR":"$JOGL_ALL_NOAWT_CLASSPATH":"$SWT_CLASSPATH":"$JUNIT_JAR":"$ANT_JARS":"$SEMVER_JAR":"$GLUEGEN_TESTUTIL_JAR"
JOGAMP_MOBILE_ALT_CLASSPATH=.:"$GLUEGEN_ALT_JAR":"$JOAL_JAR":"$JOGL_MOBILE_CLASSPATH":"$SWT_CLASSPATH":"$JUNIT_JAR":"$ANT_JARS":"$SEMVER_JAR":"$GLUEGEN_TESTUTIL_JAR"
export JOGAMP_ALL_AWT_CLASSPATH JOGAMP_ALL_NOAWT_CLASSPATH JOGAMP_MOBILE_CLASSPATH JOGAMP_ATOMICS_NOAWT_CLASSPATH JOGAMP_ALL_NOAWT_ALT_CLASSPATH JOGAMP_MOBILE_ALT_CLASSPATH

JOGAMP_CLASSPATH=.:"$GLUEGEN_JAR":"$JOAL_JAR":"$JOGL_CLASSPATH":"$SWT_CLASSPATH":"$JUNIT_JAR":"$ANT_JARS":"$SEMVER_JAR":"$GLUEGEN_TESTUTIL_JAR"
export JOGAMP_CLASSPATH

export JOGAMP_LD_LIBRARY_PATH="$LD_LIBRARY_PATH":"$GLUEGEN_OS":"$JOGL_LIB_DIR"
export JOGAMP_DYLD_LIBRARY_PATH="$DYLD_LIBRARY_PATH":"$GLUEGEN_OS":"$JOGL_LIB_DIR"

echo JOGAMP_ALL_AWT_CLASSPATH: "$JOGAMP_ALL_AWT_CLASSPATH"
echo JOGAMP_ALL_NOAWT_CLASSPATH: "$JOGAMP_ALL_NOAWT_CLASSPATH"
echo JOGAMP_MOBILE_CLASSPATH: "$JOGAMP_MOBILE_CLASSPATH"
echo JOGAMP_CLASSPATH: "$JOGAMP_CLASSPATH"
echo JOGAMP_LD_LIBRARY_PATH: "$JOGAMP_LD_LIBRARY_PATH"
echo JOGAMP_DYLD_LIBRARY_PATH: "$JOGAMP_DYLD_LIBRARY_PATH"
echo
echo MacOSX REMEMBER to add the JVM arguments "-XstartOnFirstThread -Djava.awt.headless=true" for running demos without AWT, e.g. NEWT
echo MacOSX REMEMBER to add the JVM arguments "-XstartOnFirstThread -Djava.awt.headless=true com.jogamp.newt.util.MainThread" for running demos with NEWT

PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
export PATH

