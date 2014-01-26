
set BLD_SUB=build-win64
set J2RE_HOME=c:\jre1.7.0_45_x64
set JAVA_HOME=c:\jdk1.7.0_45_x64
set ANT_PATH=C:\apache-ant-1.8.2

REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc53_lavf53_lavu51-ffmpeg\x64
REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc55_lavf55_lavu52-ffmpeg\x64
set FFMPEG_LIB=C:\ffmpeg_libav\lavc55_lavf55_lavu52-ffmpeg-2013-10-09\x64
REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc54_lavf54_lavu52_lavr01-libav\x64

REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\PVRVFrame\OGLES-2.0\Windows_x86_64;%PATH%
set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%FFMPEG_LIB%;%PATH%

set BLD_DIR=..\%BLD_SUB%
REM set LIB_DIR=%BLD_DIR%\lib;..\..\gluegen\%BLD_SUB%\obj
set LIB_DIR=

set CP_ALL=.;%BLD_DIR%\jar\jogl-all.jar;%BLD_DIR%\jar\jogl-test.jar;..\..\joal\%BLD_SUB%\joal.jar;..\..\gluegen\%BLD_SUB%\gluegen-rt.jar;..\..\gluegen\make\lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;%BLD_DIR%\..\make\lib\swt\win32-win32-x86_64\swt-debug.jar
echo CP_ALL %CP_ALL%

set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true"

scripts\tests-win.bat %*

