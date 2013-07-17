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

function gluegen_gl2es1() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-if-gl2_es1.cfg \
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
stub_includes/opengl/gles1.c \

copy_temp
}

function gluegen_es1() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-es1.cfg \
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
stub_includes/opengl/gles1.c \

copy_temp
}

function gluegen_gl2es2() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-if-gl2_es2.cfg \
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

function gluegen_gl3es3() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-if-gl3_es3.cfg \
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
stub_includes/opengl/gles3.c \

copy_temp

}
function gluegen_es3() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-es3.cfg \
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
stub_includes/opengl/gles3.c \

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
-C./config/jogl/gl-if-gl2gl3-subset.cfg \
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

copy_temp

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

function gluegen_glx() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/glx-x11.cfg \
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
stub_includes/x11/window-system1.c \

copy_temp

}

function gluegen_glxext() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/glxext.cfg \
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
stub_includes/x11/glxext.c \

copy_temp

}

function gluegen_wgl() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/wgl-win32.cfg \
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
stub_includes/win32/window-system1.c \

copy_temp

}

function gluegen_wglext() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/wglext.cfg \
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
stub_includes/win32/wglext.c \

copy_temp

}

function gluegen_gl4bc() {
java \
-classpath \
../../gluegen/$rootrel/gluegen.jar:../$rootrel/jogl/gluegen-gl.jar \
com.jogamp.gluegen.GlueGen \
--debug \
--dumpCPP \
-O../$rootrel/jogl \
-Ecom.jogamp.gluegen.opengl.GLEmitter \
-C./config/jogl/gl-gl4bc.cfg \
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
stub_includes/opengl/gl4bc.c \

copy_temp

}

function gluegen_all() {
#   gluegen_if_gl
#   gluegen_gl2es1
#   gluegen_es1
   gluegen_gl2es2
#   gluegen_es2
#   gluegen_gl3es3
#   gluegen_es3
#   gluegen_gl2
#   gluegen_gl2gl3
#   gluegen_gl4bc
#   gluegen_glx
#   gluegen_glxext
#   gluegen_wgl
#   gluegen_wglext
}

gluegen_all 2>&1 | tee $(basename $0 .sh).log

