#! /bin/sh

if [ -e ../../setenv-build-jogl-x86_64.sh ] ; then
    . ../../setenv-build-jogl-x86_64.sh
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
#    -Dc.compiler.debug=true \
#    -Dbuild.noarchives=true \

#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxAMD64=true \
#    -DisX11=true \
#    -Dbuild.noarchives=true \

#LD_LIBRARY_PATH=/opt-linux-x86_64/mesa-7.8.1/lib64
#export LD_LIBRARY_PATH

ant  \
    $CUSTOMLIBDIR \
    -Dbuild.noarchives=true \
    -Djogl.cg=1 -Dx11.cg.lib=../../lib-linux-x86_64 \
    -Drootrel.build=build-x86_64 \
    -DuseKD=true \
    -DuseOpenMAX=true \
    $* 2>&1 | tee make.jogl.all.linux-x86_64.log
