
set BLD_SUB=build-win32
set J2RE_HOME=c:\jre1.7.0_25_x32
set JAVA_HOME=c:\jdk1.7.0_25_x32
set ANT_PATH=C:\apache-ant-1.8.2

set PROJECT_ROOT=D:\projects\jogamp\jogl
set BLD_DIR=..\%BLD_SUB%

REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\angle\win32\20120127;%PATH%
set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\angle\win32\20121010-chrome;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\PVRVFrame\OGLES-2.0\Windows_x86_32;%PATH%

set BLD_DIR=..\%BLD_SUB%
REM set LIB_DIR=..\..\gluegen\%BLD_SUB%\obj;%BLD_DIR%\lib
REM set FFMPEG_LIB=%PROJECT_ROOT%\make\lib\ffmpeg\x32
REM set LIB_DIR=%FFMPEG_LIB%
set LIB_DIR=

set CP_ALL=.;%BLD_DIR%\jar\jogl-all.jar;%BLD_DIR%\jar\jogl-test.jar;..\..\gluegen\%BLD_SUB%\gluegen-rt.jar;..\..\gluegen\make\lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;%BLD_DIR%\..\make\lib\swt\win32-win32-x86\swt-debug.jar

echo CP_ALL %CP_ALL%

scripts\tests-win.bat
