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

# -Djogl.cg=1
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

LOGF=make.jogl.all.linux-x86_64.log
rm -f $LOGF

# export LIBGL_DRIVERS_PATH=/usr/lib/fglrx/dri:/usr/lib32/fglrx/dri
# export LIBGL_DEBUG=verbose
echo LIBXCB_ALLOW_SLOPPY_LOCK: $LIBXCB_ALLOW_SLOPPY_LOCK 2>&1 | tee -a $LOGF
echo LIBGL_DRIVERS_PATH: $LIBGL_DRIVERS_PATH 2>&1 | tee -a $LOGF
echo LIBGL_DEBUG: $LIBGL_DEBUG 2>&1 | tee -a $LOGF

#    -Dbuild.noarchives=true \
#    -Dc.compiler.debug=true \

ant  \
    $CUSTOMLIBDIR \
    -Djogl.cg=1 \
    -Drootrel.build=build-x86_64 \
    -DuseKD=true \
    -DuseOpenMAX=true \
    $* 2>&1 | tee -a $LOGF

