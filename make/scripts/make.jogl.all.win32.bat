set THISDIR="C:\JOGL"

set J2RE_HOME=c:\jre1.8.0_66_x32
set JAVA_HOME=c:\jdk1.8.0_66_x32
set ANT_PATH=C:\apache-ant-1.9.4

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw\bin;%PATH%

REM set LIB_GEN=%THISDIR%\lib
REM set CLASSPATH=.;%THISDIR%\build-win32\classes
REM    -Dc.compiler.debug=true 
REM    -Dsetup.addNativeOpenMAX=true 
REM    -Dsetup.addNativeKD=true

set SOURCE_LEVEL=1.6
set TARGET_LEVEL=1.6
set TARGET_RT_JAR=c:\jre1.6.0_30\lib\rt.jar

REM set JOGAMP_JAR_CODEBASE=Codebase: *.jogamp.org
set JOGAMP_JAR_CODEBASE=Codebase: *.goethel.localnet

ant -Drootrel.build=build-win32 %1 %2 %3 %4 %5 %6 %7 %8 %9 > make.jogl.all.win32.log 2>&1
