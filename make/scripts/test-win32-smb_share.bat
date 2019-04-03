
set SMB_ROOT=\\risa.goethel.localnet\deployment\test\jogamp

set BLD_SUB=build-win32
set J2RE_HOME=c:\jre1.8.0_121_x32
set JAVA_HOME=c:\jdk1.8.0_121_x32
set ANT_PATH=C:\apache-ant-1.9.4

set PROJECT_ROOT=%SMB_ROOT%\jogl
set BLD_DIR=%PROJECT_ROOT%\%BLD_SUB%

REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc53_lavf53_lavu51-ffmpeg\x32
set FFMPEG_LIB=C:\ffmpeg_libav\lavc55_lavf55_lavu52-ffmpeg\x32
REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc54_lavf54_lavu52_lavr01-libav\x32

REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\angle\win32\20120127;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\angle\win32\20121010-chrome;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\PVRVFrame\OGLES-2.0\Windows_x86_32;%PATH%
set PATH=%J2RE_HOME%\bin;%JAVA_HOME%\bin;%ANT_PATH%\bin;%FFMPEG_LIB%;%PATH%

set D_ARGS="-Djogamp.debug=all"

REM set LIB_DIR=..\..\gluegen\%BLD_SUB%\obj;%BLD_DIR%\lib
set LIB_DIR=

set CP_ALL=.;%BLD_DIR%\jar\jogl-all.jar;%BLD_DIR%\jar\jogl-test.jar;%SMB_ROOT%\gluegen\%BLD_SUB%\gluegen-rt.jar;%SMB_ROOT%\gluegen\make\lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;%BLD_DIR%\..\make\lib\swt\win32-win32-x86\swt-debug.jar

echo CP_ALL %CP_ALL%

%J2RE_HOME%\bin\java -classpath %CP_ALL% %D_ARGS% %X_ARGS% com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT -time 3000 > java-win.log 2>&1
tail java-win.log

