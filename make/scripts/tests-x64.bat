
set BLD_SUB=build-win64
set J2RE_HOME=c:\jdk-21
set JAVA_HOME=c:\jdk-21
set ANT_PATH=C:\apache-ant-1.10.5

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
set FFMPEG_LIB=C:\ffmpeg-5.1.2-full_build-shared\bin

REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\PVRVFrame\OGLES-2.0\Windows_x86_64;%PATH%
set PATH=%J2RE_HOME%\bin;%JAVA_HOME%\bin;%ANT_PATH%\bin;%MESA3D_LIB%;%FFMPEG_LIB%;%PATH%
REM set PATH=%J2RE_HOME%\bin;%JAVA_HOME%\bin;%ANT_PATH%\bin;C:\temp;%FFMPEG_LIB%;%PATH%
echo PATH %PATH%

set BLD_DIR=..\%BLD_SUB%
REM set LIB_DIR=%cd%\%BLD_DIR%\lib;%cd%\..\..\gluegen\%BLD_SUB%\obj
set LIB_DIR=

set CP_ALL=.;%BLD_DIR%\jar\jogl-all.jar;%BLD_DIR%\jar\atomic\oculusvr.jar;%BLD_DIR%\jar\jogl-test.jar;%BLD_DIR%\jar\jogl-demos.jar;..\..\joal\%BLD_SUB%\jar\joal.jar;..\..\gluegen\%BLD_SUB%\gluegen-rt.jar;..\..\gluegen\%BLD_SUB%\gluegen-test-util.jar;..\..\gluegen\make\lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;%BLD_DIR%\..\make\lib\swt\win32-win32-x86_64\swt.jar
echo CP_ALL %CP_ALL%

REM set MODULE_ARGS=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens java.desktop/sun.java2d=ALL-UNNAMED
set MODULE_ARGS=--add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens java.desktop/sun.awt.windows=ALL-UNNAMED --add-opens java.desktop/sun.java2d=ALL-UNNAMED
REM set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %MODULE_ARGS%
set X_ARGS="-Dsun.java2d.noddraw=true" %MODULE_ARGS%
REM set X_ARGS="-Xcheck:jni" "-verbose:jni" "-Dsun.java2d.noddraw=true" "-Djava.awt.headless=true" %MODULE_ARGS%
REM set X_ARGS="-Xcheck:jni" "-Dsun.java2d.noddraw=true" "-Djava.awt.headless=true" %MODULE_ARGS%


scripts\tests-win.bat %*

