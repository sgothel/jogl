
set BLD_SUB=build-win64
set J2RE_HOME=c:\jdk-17
set JAVA_HOME=c:\jdk-17
set ANT_PATH=C:\apache-ant-1.10.5

REM set TEMP=C:\Documents and Settings\jogamp\temp
REM set TMP=C:\Documents and Settings\jogamp\temp
REM set TEMP=C:\Users\jogamp\temp\no-exec
REM set TMP=C:\Users\jogamp\temp\no-exec

REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc53_lavf53_lavu51-ffmpeg\x64
REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc55_lavf55_lavu52-ffmpeg\x64
set FFMPEG_LIB=C:\ffmpeg_libav\lavc55_lavf55_lavu52-ffmpeg-2013-10-09\x64
REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc54_lavf54_lavu52_lavr01-libav\x64

REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\PVRVFrame\OGLES-2.0\Windows_x86_64;%PATH%
set PATH=%J2RE_HOME%\bin;%JAVA_HOME%\bin;%ANT_PATH%\bin;%FFMPEG_LIB%;%PATH%
REM set PATH=%J2RE_HOME%\bin;%JAVA_HOME%\bin;%ANT_PATH%\bin;C:\temp;%FFMPEG_LIB%;%PATH%
echo PATH %PATH%

set BLD_DIR=..\%BLD_SUB%
REM set LIB_DIR=%BLD_DIR%\lib;..\..\gluegen\%BLD_SUB%\obj
set LIB_DIR=

set CP_ALL=.;%BLD_DIR%\jar\jogl-all.jar;%BLD_DIR%\jar\atomic\oculusvr.jar;%BLD_DIR%\jar\jogl-test.jar;..\..\joal\%BLD_SUB%\joal.jar;..\..\gluegen\%BLD_SUB%\gluegen-rt.jar;..\..\gluegen\%BLD_SUB%\gluegen-test-util.jar;..\..\gluegen\make\lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;%BLD_DIR%\..\make\lib\swt\win32-win32-x86_64\swt.jar
echo CP_ALL %CP_ALL%

REM set MODULE_ARGS=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens java.desktop/sun.java2d=ALL-UNNAMED
set MODULE_ARGS=--add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens java.desktop/sun.java2d=ALL-UNNAMED
set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %MODULE_ARGS%

scripts\tests-win.bat %*

