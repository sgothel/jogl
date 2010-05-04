#! /bin/sh

if [ -e ../../setenv-build-jogl-x86.sh ] ; then
    . ../../setenv-build-jogl-x86.sh
fi

if [ -z "$ANT_PATH" ] ; then
    if [ -e /usr/share/ant/bin/ant -a -e /usr/share/ant/lib/ant.jar ] ; then
        ANT_PATH=/usr/share/ant
        export ANT_PATH
        echo autosetting ANT_PATH to $ANT_PATH
    fi
fi
if [ -z "$ANT_PATH" ] ; then
    echo ANT_PATH does not exist, set it
    exit
fi

if [ "$1" = "-libdir" ] ; then
    shift
    if [ -z "$1" ] ; then
        echo libdir argument missing
        print_usage
        exit
    fi
    CUSTOMLIBDIR="-Dcustom.libdir=$1"
    shift
fi


# -Djogl.cg=1 -Dx11.cg.lib=../../lib-linux-x86_64
#    -Dc.compiler.debug=true 
#    -DuseOpenMAX=true \
#    -Dbuild.noarchives=true
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxX86=true \
#    -DisX11=true \
#    -Djogl.cg=1 -Dx11.cg.lib=../../lib-linux-x86 \

ant \
    $CUSTOMLIBDIR \
    -Dgluegen-cpptasks.file=`pwd`/../../gluegen/make/lib/gluegen-cpptasks-linux-32bit.xml \
    -Dbuild.noarchives=true \
    -Djogl.cg=1 -Dx11.cg.lib=../../lib-linux-x86 \
    -Drootrel.build=build-x86 \
    -Dos.arch=x86 \
    -DuseKD=true \
    -DuseOpenMAX=true \
    $* 2>&1 | tee make.jogl.all.linux-x86.log
