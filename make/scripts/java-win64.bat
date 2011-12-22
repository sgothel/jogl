
set BLD_SUB=build-win64
set J2RE_HOME=c:\jre1.6.0_30_x64
set JAVA_HOME=c:\jdk1.6.0_30_x64
set ANT_PATH=C:\apache-ant-1.8.2

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw\bin;%PATH%

set BLD_DIR=..\%BLD_SUB%
set LIB_DIR=%BLD_DIR%\lib;..\..\gluegen\%BLD_SUB%\obj

set CP_ALL=.;%BLD_DIR%\jar\jogl.all.jar;%BLD_DIR%\jar\jogl.test.jar;..\..\gluegen\%BLD_SUB%\gluegen-rt.jar;..\..\gluegen\make\lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;%BLD_DIR%\..\make\lib\swt\win32-win32-x86_64\swt-debug.jar
echo CP_ALL %CP_ALL%

set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true"

%J2RE_HOME%\bin\java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %X_ARGS% %* > java-win64.log 2>&1
