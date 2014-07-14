set THISDIR="C:\JOGL"

set J2RE_HOME=c:\jre1.7.0_45_x64
set JAVA_HOME=c:\jdk1.7.0_45_x64
set ANT_PATH=C:\apache-ant-1.8.2

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw64\bin;%PATH%

REM set LIB_GEN=%THISDIR%\lib
REM set CLASSPATH=.;%THISDIR%\build-win64\classes
REM    -Dc.compiler.debug=true 
REM    -Dsetup.addNativeOpenMAX=true 
REM    -Dsetup.addNativeKD=true

set SOURCE_LEVEL=1.6
set TARGET_LEVEL=1.6
set TARGET_RT_JAR=c:\jre1.6.0_30\lib\rt.jar

REM set JOGAMP_JAR_CODEBASE=Codebase: *.jogamp.org
set JOGAMP_JAR_CODEBASE=Codebase: *.goethel.localnet

ant -Drootrel.build=build-win64 %1 %2 %3 %4 %5 %6 %7 %8 %9 > make.jogl.all.win64.log 2>&1
