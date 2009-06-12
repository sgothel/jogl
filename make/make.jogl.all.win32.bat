set THISDIR="C:\SUN\JOGL2"
set J2RE_HOME=c:\jre6
set JAVA_HOME=c:\jdk6
set PATH=%JAVA_HOME%\bin;%PATH%
set LIB_GEN=%THISDIR%\lib
set CLASSPATH=.;%THISDIR%\build-win32\classes
REM    -Dc.compiler.debug=true 
REM    -DuseOpenMAX=true 
REM    -DuseKD=true

ant -Drootrel.build=build-win32 -v > make.jogl.all.win32.log 2>&1
