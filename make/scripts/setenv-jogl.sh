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

JOGL_BUILDDIR=$1
shift

if [ -z "$1" ] ; then
    JOGL_PROFILE=JOGL_ALL
else
    JOGL_PROFILE=$1
    shift
fi

THISDIR=`pwd`

if [ -e "$JOGL_BUILDDIR" ] ; then
    JOGL_DIR=$JOGL_BUILDDIR/..
    JOGL_BUILDDIR_BASE=`basename $JOGL_BUILDDIR`
else
    echo JOGL_BUILDDIR $JOGL_BUILDDIR not exist or not given
    print_usage
    exit
fi

gpf=`find ../../gluegen/make -name dynlink-unix.cfg`
if [ -z "$gpf" ] ; then
    gpf=`find .. -name dynlink-unix.cfg`
fi
if [ -z "$gpf" ] ; then
    echo GLUEGEN_BUILDDIR not found
    print_usage
    exit
fi

GLUEGEN_DIR=`dirname $gpf`/..
GLUEGEN_BUILDDIR=$GLUEGEN_DIR/$JOGL_BUILDDIR_BASE
if [ ! -e "$GLUEGEN_BUILDDIR" ] ; then
    echo GLUEGEN_BUILDDIR $GLUEGEN_BUILDDIR does not exist
    print_usage
    exit
fi
GLUEGEN_JAR=$GLUEGEN_BUILDDIR/gluegen-rt.jar
GLUEGEN_OS=$GLUEGEN_BUILDDIR/obj
JUNIT_JAR=$GLUEGEN_DIR/make/lib/junit.jar

if [ -z "$ANT_PATH" ] ; then
    ANT_PATH=$(dirname `which ant`)/..
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

echo GLUEGEN BUILDDIR: $GLUEGEN_BUILDDIR
echo JOGL DIR: $JOGL_DIR
echo JOGL BUILDDIR: $JOGL_BUILDDIR
echo JOGL BUILDDIR BASE: $JOGL_BUILDDIR_BASE
echo JOGL PROFILE: $JOGL_PROFILE

J2RE_HOME=$(which java)
JAVA_HOME=$(which javac)
CP_SEP=:

. $JOGL_DIR/etc/profile.jogl $JOGL_PROFILE $JOGL_BUILDDIR 

LIB=$THISDIR/lib

JOGAMP_ALL_AWT_CLASSPATH=.:$GLUEGEN_JAR:$JOGL_ALL_AWT_CLASSPATH:$SWT_CLASSPATH:$JUNIT_JAR:$ANT_JARS
JOGAMP_ALL_NOAWT_CLASSPATH=.:$GLUEGEN_JAR:$JOGL_ALL_NOAWT_CLASSPATH:$SWT_CLASSPATH:$JUNIT_JAR:$ANT_JARS
JOGAMP_MOBILE_CLASSPATH=.:$GLUEGEN_JAR:$JOGL_MOBILE_CLASSPATH:$SWT_CLASSPATH:$JUNIT_JAR:$ANT_JARS
export JOGAMP_ALL_AWT_CLASSPATH JOGAMP_ALL_NOAWT_CLASSPATH JOGAMP_MOBILE_CLASSPATH

CLASSPATH=.:$GLUEGEN_JAR:$JOGL_CLASSPATH:$SWT_CLASSPATH:$JUNIT_JAR:$ANT_JARS
export CLASSPATH

# We use TempJarCache per default now!
#export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$GLUEGEN_OS:$JOGL_LIB_DIR
#export DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:$GLUEGEN_OS:$JOGL_LIB_DIR

echo JOGAMP_ALL_AWT_CLASSPATH: $CLASSPATH
echo JOGAMP_ALL_NOAWT_CLASSPATH: $CLASSPATH
echo JOGAMP_MOBILE_CLASSPATH: $CLASSPATH
echo CLASSPATH: $CLASSPATH
echo
echo MacOSX REMEMBER to add the JVM arguments "-XstartOnFirstThread -Djava.awt.headless=true" for running demos without AWT, e.g. NEWT
echo MacOSX REMEMBER to add the JVM arguments "-XstartOnFirstThread -Djava.awt.headless=true com.jogamp.newt.util.MainThread" for running demos with NEWT

PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
export PATH

