
set BLD_SUB=build-win32
set J2RE_HOME=c:\jre1.7.0_40_x32
set JAVA_HOME=c:\jdk1.7.0_40_x32
set ANT_PATH=C:\apache-ant-1.8.2

set PROJECT_ROOT=D:\projects\jogamp\jogl
set BLD_DIR=..\%BLD_SUB%

REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc53_lavf53_lavu51-ffmpeg\x32
set FFMPEG_LIB=C:\ffmpeg_libav\lavc55_lavf55_lavu52-ffmpeg\x32
REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc54_lavf54_lavu52_lavr01-libav\x32

REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\angle\win32\20120127;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\angle\win32\20121010-chrome;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\PVRVFrame\OGLES-2.0\Windows_x86_32;%PATH%
set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%FFMPEG_LIB%;%PATH%

REM set LIB_DIR=%BLD_DIR%\lib;..\..\gluegen\%BLD_SUB%\obj
set LIB_DIR=

set CP_ALL=.;%BLD_DIR%\jar\jogl-all.jar;%BLD_DIR%\jar\jogl-test.jar;..\..\gluegen\%BLD_SUB%\gluegen-rt.jar;..\..\gluegen\make\lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;%BLD_DIR%\..\make\lib\swt\win32-win32-x86\swt-debug.jar

echo CP_ALL %CP_ALL%

REM set D_ARGS="-Djogl.debug.GLDrawable" "-Djogl.debug.GLContext" "-Djogl.debug.FBObject" "-Djogl.enable.ANGLE"
REM set D_ARGS="-Djogl.debug.GLDrawable" "-Djogl.debug.EGLDrawableFactory.DontQuery"
REM set D_ARGS="-Djogl.debug.GLDrawable" "-Djogl.debug.EGLDrawableFactory.QueryNativeTK"
REM set D_ARGS="-Djogl.debug=all" "-Dnewt.debug=all" "-Dnativewindow.debug=all"
REM set D_ARGS="-Djogamp.debug.IOUtil" "-Djogl.debug.GLSLCode" "-Djogl.debug.GLMediaPlayer"
REM set D_ARGS="-Djogl.debug.ExtensionAvailabilityCache" "-Djogl.debug=all" "-Dnewt.debug=all" "-Dnativewindow.debug=all" "-Djogamp.debug.ProcAddressHelper=true" "-Djogamp.debug.NativeLibrary=true" "-Djogamp.debug.NativeLibrary.Lookup=true"
REM set D_ARGS="-Djogl.debug=all" "-Dnewt.debug=all" "-Dnativewindow.debug=all" "-Djogamp.debug.NativeLibrary=true"
REM set D_ARGS="-Djogamp.debug.JNILibLoader=true" "-Djogamp.debug.NativeLibrary=true" "-Djogamp.debug.NativeLibrary.Lookup=true" "-Djogl.debug.GLProfile=true"
REM set D_ARGS="-Dnewt.debug.Window" "-Dnativewindow.debug.TraceLock"
REM set D_ARGS="-Dnativewindow.debug.TraceLock"
REM set D_ARGS="-Dnewt.debug.Window" "-Dnewt.debug.Display"
REM set D_ARGS="-Djogl.debug=all"
REM set D_ARGS="-Djogl.debug.DebugGL" "-Djogl.debug.TraceGL"
REM set D_ARGS="-Djogl.debug.DebugGL" "-Djogl.debug.TraceGL" "-Djogl.debug=all"
REM set D_ARGS="-Dnewt.debug.Window" "-Dnewt.debug.Display" "-Dnewt.test.Window.reparent.incompatible=true"
REM set D_ARGS="-Dnewt.debug.Window.MouseEvent"
REM set D_ARGS="-Dnewt.debug.Window.KeyEvent"
set D_ARGS="-Djogl.debug.GLMediaPlayer" "-Djogl.debug.GLMediaPlayer.Native"
REM set D_ARGS="-Xcheck:jni" "-Xint" "-verbose:jni"

set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true"

scripts\tests-win.bat %*
