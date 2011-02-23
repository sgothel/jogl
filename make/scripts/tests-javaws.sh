javaws -uninstall

rm -f ~/.java/deployment/log/*

JNLP=Gears.jnlp
# JNLP=TextCube.jnlp
# JNLP=JRefractNoOGL.jnlp

D_FLAGS="-J-Djnlp.nativewindow.debug=all -J-Djnlp.jogl.debug=all -J-Djnlp.newt.debug=all"

#X_FLAGS="-J-Dsun.java2d.noddraw=true -J-Dsun.awt.noerasebackground=true -J-Dsun.java2d.opengl=false"
#X_FLAGS="-J-verbose:jni"
X_FLAGS=

javaws $X_FLAGS $D_FLAGS http://risa/deployment/test/jau01s/jogl-demos/$JNLP 2>&1 | tee tests-javaws.log

