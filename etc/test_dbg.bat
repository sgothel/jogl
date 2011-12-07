
set BLD_DIR=jar

set CP_ALL=.;%BLD_DIR%\gluegen-rt.jar;%BLD_DIR%\jogl.all.jar
echo CP_ALL %CP_ALL%

set D_ARGS="-Djogamp.debug=all" "-Dnativewindow.debug=all" "-Djogl.debug=all" "-Dnewt.debug=all"
set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true"

REM java -classpath %CP_ALL% %X_ARGS% %D_ARGS% com.jogamp.newt.opengl.GLCanvas >> test.log 2>&1
java -classpath %CP_ALL% %X_ARGS% %D_ARGS% com.jogamp.newt.opengl.GLWindow >> test_dbg.log 2>&1

type test_dbg.log
