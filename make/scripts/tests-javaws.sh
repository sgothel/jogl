javaws -uninstall

JNLP=Gears.jnlp
# JNLP=TextCube.jnlp
# JNLP=JRefractNoOGL.jnlp

javaws -J-verbose:class -J-verbose:jni -J-Dsun.java2d.noddraw=true -J-Dsun.awt.noerasebackground=true -J-Dsun.java2d.opengl=false -J-Djnlp.jogl.debug=all http://risa/deployment/test/jau01s/jogl-demos/$JNLP 2>&1 | tee tests-javaws.log

