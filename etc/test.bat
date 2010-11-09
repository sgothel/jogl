
set BLD_DIR=jar
set LIB_DIR=lib

set CP_ALL=.;%BLD_DIR%\gluegen-rt.jar;%BLD_DIR%\nativewindow.all.jar;%BLD_DIR%\jogl.all.jar;%BLD_DIR%\newt.all.jar
echo CP_ALL %CP_ALL%

set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true"

%J2RE_HOME%\bin\java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %X_ARGS% %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win64.log 2>&1
