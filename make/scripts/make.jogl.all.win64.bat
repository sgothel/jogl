set THISDIR="C:\JogAmp"

set J2RE_HOME=c:\jdk-21
set JAVA_HOME=c:\jdk-21
set ANT_PATH=C:\apache-ant-1.10.5
set GIT_PATH=C:\cygwin64\bin
set SEVENZIP=C:\Program Files\7-Zip

set CMAKE_PATH=C:\cmake-3.25.1-windows-x86_64
set CMAKE_C_COMPILER=c:\mingw64\bin\gcc

REM set LIBGL_DEBUG=verbose
REM set MESA_DEBUG=true
set LIBGL_ALWAYS_SOFTWARE=true
REM set INTEL_DEBUG="buf bat"
REM set INTEL_STRICT_CONFORMANCE=1

REM set TEMP=C:\Documents and Settings\jogamp\temp
REM set TMP=C:\Documents and Settings\jogamp\temp
REM set TEMP=C:\Users\jogamp\temp\no-exec
REM set TMP=C:\Users\jogamp\temp\no-exec

set MESA3D_LIB=C:\Mesa3D\x64

set PATH=%J2RE_HOME%\bin;%JAVA_HOME%\bin;%ANT_PATH%\bin;%MESA3D_LIB%;c:\mingw64\bin;%CMAKE_PATH%\bin;%GIT_PATH%;%SEVENZIP%;%PATH%
REM set PATH=%J2RE_HOME%\bin;%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw64\bin;%CMAKE_PATH%\bin;%GIT_PATH%;%SEVENZIP%;%PATH%
echo PATH %PATH%

REM set LIB_GEN=%THISDIR%\lib
REM set CLASSPATH=.;%THISDIR%\build-win64\classes
REM    -Dc.compiler.debug=true 
REM    -Dsetup.addNativeOpenMAX=true 
REM    -Dsetup.addNativeKD=true

REM set JOGAMP_JAR_CODEBASE=Codebase: *.jogamp.org
set JOGAMP_JAR_CODEBASE=Codebase: *.goethel.localnet

ant -Drootrel.build=build-win64 %1 %2 %3 %4 %5 %6 %7 %8 %9 > make.jogl.all.win64.log 2>&1
