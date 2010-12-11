set JRE_PATH=C:\jre1.6.0_22_x64\bin
%JRE_PATH%\javaws -uninstall

set JNLP=Gears.jnlp
REM set JNLP=TextCube.jnlp
REM set JNLP=JRefractNoOGL.jnlp

%JRE_PATH%\javaws -J-verbose:class -J-verbose:jni -J-Dsun.java2d.noddraw=true -J-Dsun.awt.noerasebackground=true -J-Dsun.java2d.opengl=false -J-Djnlp.jogl.debug=all http://risa/deployment/test/jau01s/jogl-demos/%JNLP% > tests-javaws.log 2>&1

