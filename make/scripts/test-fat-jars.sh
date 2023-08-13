#!/bin/bash

#type=archive/rc
#version=v2.5.0-rc-20230813
#folder=${type}/${version}
version=jogamp-next
folder=${version}

MOSX=0
uname -a | grep -i Darwin && MOSX=1

MODULE_ARGS="--add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens java.desktop/sun.java2d=ALL-UNNAMED"
# D_ARGS="-Djogl.debug.GLMediaPlayer"

USE_CLASSPATH=jogamp-fat.jar:jogl-demos.jar

TEST_CLASS=com.jogamp.opengl.demos.graph.ui.UISceneDemo20
# TEST_CLASS=com.jogamp.opengl.demos.graph.ui.UISceneDemo03b
# TEST_CLASS=com.jogamp.opengl.demos.es2.GearsES2
# TEST_CLASS=com.jogamp.opengl.demos.es2.LandscapeES2

fetchjars() {
    curl --silent --output jogamp-fat.jar  https://jogamp.org/deployment/${folder}/fat/jogamp-fat.jar
    curl --silent --output jogl-demos.jar https://jogamp.org/deployment/${folder}/fat/jogl-demos.jar
    curl --silent --output jogl-fonts-p0.jar https://jogamp.org/deployment/${folder}/fat/jogl-fonts-p0.jar

    echo "Fetched from ${folder} to ${version}"
}

doit() {
    if [ $MOSX -eq 1 ] ; then
        # MacOS: Include FFmpeg via Homebrew ...
        if [ -e /opt/homebrew/Cellar/ffmpeg/6.0/lib ] ; then
            export DYLD_LIBRARY_PATH=/opt/homebrew/Cellar/ffmpeg/6.0/lib:$DYLD_LIBRARY_PATH
        elif [ -e /usr/local/Cellar/ffmpeg/6.0/lib ] ; then
            export DYLD_LIBRARY_PATH=/usr/local/Cellar/ffmpeg/6.0/lib:$DYLD_LIBRARY_PATH
        else
            echo "No homebrew FFmpeg for MacOS found"
        fi

        # MacOS: Select JVM path to allow DYLD_LIBRARY_PATH (FIXME?)
        JAVA_HOME=`/usr/libexec/java_home -version 17`
        PATH=$JAVA_HOME/bin:$PATH
        export JAVA_HOME PATH
    fi

    if [ ! -e ${version} ] ; then
        mkdir ${version}
        cd ${version}
        fetchjars
    else
        cd ${version}
    fi

    echo "Using ${version}"

    echo "Java exe post setup"
    which java

    echo
    echo
    echo "JOGL Default OpenGL-Info via NEWT"
    java $MODULE_ARGS $D_ARGS -jar jogamp-fat.jar

    echo
    echo
    echo "JOGL Demp ..."
    java $MODULE_ARGS -cp $USE_CLASSPATH $D_ARGS $TEST_CLASS $*
}

doit $* 2>&1 | tee ${version}.log
