set THISDIR="C:\JogAmp"

set J2RE_HOME=c:\jre-11.0.4+11_x32
set JAVA_HOME=c:\jdk-11.0.4+11_x32
set ANT_PATH=C:\apache-ant-1.10.5
set GIT_PATH=C:\cygwin\bin
set SEVENZIP=C:\Program Files\7-Zip

set CMAKE_PATH=C:\cmake-3.15.2-win32-x86
set CMAKE_C_COMPILER=c:\mingw32\bin\gcc

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw32\bin;%CMAKE_PATH%\bin;%GIT_PATH%;%SEVENZIP%;%PATH%

REM set LIB_GEN=%THISDIR%\lib
REM set CLASSPATH=.;%THISDIR%\build-win32\classes
REM    -Dc.compiler.debug=true 
REM    -Dsetup.addNativeOpenMAX=true 
REM    -Dsetup.addNativeKD=true

set SOURCE_LEVEL=1.8
set TARGET_LEVEL=1.8
set TARGET_RT_JAR=C:\jre1.8.0_212\lib\rt.jar

REM set JOGAMP_JAR_CODEBASE=Codebase: *.jogamp.org
set JOGAMP_JAR_CODEBASE=Codebase: *.goethel.localnet

ant -Drootrel.build=build-win32 %1 %2 %3 %4 %5 %6 %7 %8 %9 > make.jogl.all.win32.log 2>&1
