#! /bin/bash

rootrel=build-x86_64

builddir=../$rootrel
buildtmp=../build-temp

function copy_temp() {
    mkdir -p $buildtmp/gensrc/classes
    cp -a $builddir/jogl/gensrc/classes/* $buildtmp/gensrc/classes/
}

function gluegen_if_gl() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-if-gl.cfg \
-Istub_includes/jni \
-Istub_includes/jni/macosx \
-Istub_includes/jni/win32 \
-Istub_includes/jni/x11 \
-Istub_includes/khr \
-Istub_includes/khr/KD \
-Istub_includes/khr/KHR \
-Istub_includes/macosx \
-Istub_includes/macosx/AppKit \
-Istub_includes/macosx/OpenGL \
-Istub_includes/macosx/QuartzCore \
-Istub_includes/opengl \
-Istub_includes/opengl/GL \
-Istub_includes/opengl/GL3 \
-Istub_includes/opengl/GLES \
-Istub_includes/opengl/GLES2 \
-Istub_includes/win32 \
-Istub_includes/x11 \
-Istub_includes/x11/X11 \
-Istub_includes/x11/X11/extensions \
-I../../gluegen/make/stub_includes/gluegen \
stub_includes/opengl/gles2.c \

copy_temp

}

function gluegen_es2() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-es2.cfg \
-Istub_includes/jni \
-Istub_includes/jni/macosx \
-Istub_includes/jni/win32 \
-Istub_includes/jni/x11 \
-Istub_includes/khr \
-Istub_includes/khr/KD \
-Istub_includes/khr/KHR \
-Istub_includes/macosx \
-Istub_includes/macosx/AppKit \
-Istub_includes/macosx/OpenGL \
-Istub_includes/macosx/QuartzCore \
-Istub_includes/opengl \
-Istub_includes/opengl/GL \
-Istub_includes/opengl/GL3 \
-Istub_includes/opengl/GLES \
-Istub_includes/opengl/GLES2 \
-Istub_includes/win32 \
-Istub_includes/x11 \
-Istub_includes/x11/X11 \
-Istub_includes/x11/X11/extensions \
-I../../gluegen/make/stub_includes/gluegen \
stub_includes/opengl/gles2.c \

copy_temp

}

function gluegen_gl2() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-if-gl2.cfg \
-Istub_includes/jni \
-Istub_includes/jni/macosx \
-Istub_includes/jni/win32 \
-Istub_includes/jni/x11 \
-Istub_includes/khr \
-Istub_includes/khr/KD \
-Istub_includes/khr/KHR \
-Istub_includes/macosx \
-Istub_includes/macosx/AppKit \
-Istub_includes/macosx/OpenGL \
-Istub_includes/macosx/QuartzCore \
-Istub_includes/opengl \
-Istub_includes/opengl/GL \
-Istub_includes/opengl/GL3 \
-Istub_includes/opengl/GLES \
-Istub_includes/opengl/GLES2 \
-Istub_includes/win32 \
-Istub_includes/x11 \
-Istub_includes/x11/X11 \
-Istub_includes/x11/X11/extensions \
-I../../gluegen/make/stub_includes/gluegen \
stub_includes/opengl/gl2.c \

copy_temp

}

function gluegen_gl2gl3() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-if-gl3-subset.cfg \
-Istub_includes/jni \
-Istub_includes/jni/macosx \
-Istub_includes/jni/win32 \
-Istub_includes/jni/x11 \
-Istub_includes/khr \
-Istub_includes/khr/KD \
-Istub_includes/khr/KHR \
-Istub_includes/macosx \
-Istub_includes/macosx/AppKit \
-Istub_includes/macosx/OpenGL \
-Istub_includes/macosx/QuartzCore \
-Istub_includes/opengl \
-Istub_includes/opengl/GL \
-Istub_includes/opengl/GL3 \
-Istub_includes/opengl/GLES \
-Istub_includes/opengl/GLES2 \
-Istub_includes/win32 \
-Istub_includes/x11 \
-Istub_includes/x11/X11 \
-Istub_includes/x11/X11/extensions \
-I../../gluegen/make/stub_includes/gluegen \
stub_includes/opengl/gl3.c \

java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-if-gl2_gl3.cfg \
-Istub_includes/jni \
-Istub_includes/jni/macosx \
-Istub_includes/jni/win32 \
-Istub_includes/jni/x11 \
-Istub_includes/khr \
-Istub_includes/khr/KD \
-Istub_includes/khr/KHR \
-Istub_includes/macosx \
-Istub_includes/macosx/AppKit \
-Istub_includes/macosx/OpenGL \
-Istub_includes/macosx/QuartzCore \
-Istub_includes/opengl \
-Istub_includes/opengl/GL \
-Istub_includes/opengl/GL3 \
-Istub_includes/opengl/GLES \
-Istub_includes/opengl/GLES2 \
-Istub_includes/win32 \
-Istub_includes/x11 \
-Istub_includes/x11/X11 \
-Istub_includes/x11/X11/extensions \
-I../../gluegen/make/stub_includes/gluegen \
stub_includes/opengl/gl2.c \

copy_temp

}
function gluegen_all() {
#    gluegen_if_gl
#   gluegen_es2
#   gluegen_gl2
   gluegen_gl2gl3
}

gluegen_all 2>&1 | tee $(basename $0 .sh).log

