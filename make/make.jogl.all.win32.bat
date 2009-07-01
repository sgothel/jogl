set THISDIR="C:\SUN\JOGL2"
set J2RE_HOME=c:\jre6
set JAVA_HOME=c:\jdk6
set PATH=%JAVA_HOME%\bin;%PATH%
set LIB_GEN=%THISDIR%\lib
set CLASSPATH=.;%THISDIR%\build-win32\classes
REM    -Dc.compiler.debug=true 
REM    -DuseOpenMAX=true 
REM    -DuseKD=true
REM    -Djogl.cg=1 -D-Dwindows.cg.lib=C:\Cg-2.2

ant -Drootrel.build=build-win32 -Djogl.cg=1 -Dwindows.cg.lib=C:\Cg-2.2\lib > make.jogl.all.win32.log 2>&1
