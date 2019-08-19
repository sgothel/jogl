set THISDIR="C:\JOGL"

set J2RE_HOME=c:\jre-11.0.4+11_x64
set JAVA_HOME=c:\jdk-11.0.4+11_x64
set ANT_PATH=C:\apache-ant-1.10.5
set GIT_PATH=C:\cygwin\bin
set SEVENZIP=C:\Program Files\7-Zip

set CMAKE_PATH=C:\cmake-3.15.2-win64-x64
set CMAKE_C_COMPILER=c:\mingw64\bin\gcc

set PATH=%J2RE_HOME%\bin;%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw64\bin;%CMAKE_PATH%\bin;%GIT_PATH%;%SEVENZIP%;%PATH%

REM set LIB_GEN=%THISDIR%\lib
REM set CLASSPATH=.;%THISDIR%\build-win64\classes
REM    -Dc.compiler.debug=true 
REM    -Dsetup.addNativeOpenMAX=true 
REM    -Dsetup.addNativeKD=true

set SOURCE_LEVEL=1.8
set TARGET_LEVEL=1.8
set TARGET_RT_JAR=C:\jre1.8.0_212\lib\rt.jar

REM set JOGAMP_JAR_CODEBASE=Codebase: *.jogamp.org
set JOGAMP_JAR_CODEBASE=Codebase: *.goethel.localnet

ant -Drootrel.build=build-win64 %1 %2 %3 %4 %5 %6 %7 %8 %9 > make.jogl.all.win64.log 2>&1
