set THISDIR="C:\JOGL"

set J2RE_HOME=c:\jre1.6.0_22_x64
set JAVA_HOME=c:\jdk1.6.0_22_x64
set ANT_PATH=C:\apache-ant-1.8.0

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw64\bin;c:\mingw\bin;%PATH%

set LIB_GEN=%THISDIR%\lib
set CLASSPATH=.;%THISDIR%\build-win64\classes
REM    -Dc.compiler.debug=true 
REM    -DuseOpenMAX=true 
REM    -DuseKD=true
REM    -Djogl.cg=1
REM    -Dbuild.noarchives=true

ant -Dc.compiler.debug=true -Dbuild.noarchives=true -Drootrel.build=build-win64 -Djogl.cg=1 %1 %2 %3 %4 %5 %6 %7 %8 %9 > make.jogl.all.win64.log 2>&1
