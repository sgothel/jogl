set THISDIR="C:\JOGL"

set J2RE_HOME=c:\jre1.6.0_19
set JAVA_HOME=c:\jdk1.6.0_19
set ANT_PATH=C:\apache-ant-1.8.0

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw\bin;%PATH%

set LIB_GEN=%THISDIR%\lib
set CLASSPATH=.;%THISDIR%\build-win32\classes
REM    -Dc.compiler.debug=true 
REM    -DuseOpenMAX=true 
REM    -DuseKD=true
REM    -Djogl.cg=1 -D-Dwindows.cg.lib=C:\Cg-2.2

ant -Dbuild.noarchives=true -Drootrel.build=build-win32 -Djogl.cg=1 -Dwindows.cg.lib=C:\Cg-2.2\lib %1 %2 %3 %4 %5 %6 %7 %8 %9 > make.jogl.all.win32.log 2>&1
