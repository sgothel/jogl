
set BLD_DIR=jar
set LIB_DIR=lib

set CP_ALL=.;%BLD_DIR%\gluegen-rt.jar;%BLD_DIR%\nativewindow.all.jar;%BLD_DIR%\jogl.all.jar;%BLD_DIR%\newt.all.jar
echo CP_ALL %CP_ALL%

set D_ARGS="-Djogamp.debug=all" "-Dnativewindow.debug=all" "-Djogl.debug=all" "-Dnewt.debug=all"
set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true"

REM java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %X_ARGS% javax.media.opengl.awt.GLCanvas > java-win64.log 2>&1

REM java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %X_ARGS% com.jogamp.common.GlueGenVersion > test.log 2>&1
REM java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %X_ARGS% com.jogamp.nativewindow.NativeWindowVersion >> test.log 2>&1
REM java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %X_ARGS% com.jogamp.opengl.JoglVersion >> test.log 2>&1
REM java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %X_ARGS% com.jogamp.newt.NewtVersion >> test.log 2>&1
REM java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %X_ARGS% com.jogamp.newt.opengl.GLCanvas >> test.log 2>&1
java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %X_ARGS% %D_ARGS% com.jogamp.newt.opengl.GLWindow >> test_dbg.log 2>&1

type test.log
