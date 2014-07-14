
set BLD_SUB=build-win64
set J2RE_HOME=c:\jre1.7.0_45_x64
set JAVA_HOME=c:\jdk1.7.0_45_x64
set ANT_PATH=C:\apache-ant-1.8.2

set PROJECT_ROOT=D:\projects\jogamp\jogl
set BLD_DIR=..\%BLD_SUB%

REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc53_lavf53_lavu51-ffmpeg\x64
REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc55_lavf55_lavu52-ffmpeg\x64
set FFMPEG_LIB=C:\ffmpeg_libav\lavc55_lavf55_lavu52-ffmpeg-2013-10-09\x64
REM set FFMPEG_LIB=C:\ffmpeg_libav\lavc54_lavf54_lavu52_lavr01-libav\x64

REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw\bin;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\PVRVFrame\OGLES-2.0\Windows_x86_64;%PATH%
set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%FFMPEG_LIB%;%PATH%

set CP_ALL=.;%BLD_DIR%\jar\jogl-all.jar;%BLD_DIR%\jar\atomic\oculusvr.jar;%BLD_DIR%\jar\jogl-test.jar;..\..\joal\%BLD_SUB%\joal.jar;..\..\gluegen\%BLD_SUB%\gluegen-rt.jar;..\..\gluegen\make\lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;%BLD_DIR%\..\make\lib\swt\win32-win32-x86_64\swt-debug.jar

echo CP_ALL %CP_ALL%

REM set D_ARGS="-Djogamp.debug=all"
set D_ARGS="-Djogl.debug=all" "-Dnativewindow.debug=all"
REM set D_ARGS="-Djogl.debug=all" "-Dnewt.debug=all" "-Dnativewindow.debug=all"
REM set D_ARGS="-Djogl.debug=all" "-Dnewt.debug=all" "-Dnativewindow.debug=all" "-Djogamp.debug=all" "-Djogl.debug.EGLDrawableFactory.DontQuery"
REM set D_ARGS="-Dnativewindow.debug.GDIUtil" "-Dnativewindow.debug.RegisteredClass"
REM set D_ARGS="-Djogl.debug.GLContext" "-Djogl.debug.FBObject"
REM set D_ARGS="-Djogl.debug.GLDrawable" "-Djogl.debug.EGLDrawableFactory.DontQuery"
REM set D_ARGS="-Djogl.debug.GLDrawable" "-Djogl.debug.EGLDrawableFactory.QueryNativeTK"
REM set D_ARGS="-Djogl.debug.GLDrawable" "-Djogl.debug.GLContext" "-Djogl.debug.GLCanvas"
REM set D_ARGS="-Dnativewindow.debug.GraphicsConfiguration -Djogl.debug.CapabilitiesChooser -Djogl.debug.GLProfile"
REM set D_ARGS="-Djogamp.debug.IOUtil"
REM set D_ARGS="-Djogl.debug.GLSLCode" "-Djogl.debug.GLMediaPlayer"
REM set D_ARGS="-Djogl.debug.GLMediaPlayer"
REM set D_ARGS="-Djogl.debug.GLMediaPlayer" "-Djogl.debug.AudioSink"
REM set D_ARGS="-Djogl.debug.GLMediaPlayer" "-Djogl.debug.GLMediaPlayer.Native"
REM set D_ARGS="-Djogl.debug.GLMediaPlayer.StreamWorker.delay=25" "-Djogl.debug.GLMediaPlayer"
REM set D_ARGS="-Djogl.debug.GLMediaPlayer.Native"
REM set D_ARGS="-Djogl.debug.AudioSink"
REM set D_ARGS="-Djogamp.debug.NativeLibrary=true" "-Djogamp.debug.NativeLibrary.Lookup=true" "-Djogl.debug.GLSLCode"
REM set D_ARGS="-Djogl.debug=all" "-Dnativewindow.debug=all" "-Djogamp.debug.ProcAddressHelper" "-Djogamp.debug.NativeLibrary" "-Djogamp.debug.NativeLibrary.Lookup" "-Djogamp.debug.JNILibLoader" "-Djogamp.debug.TempJarCache" "-Djogamp.debug.JarUtil"
REM set D_ARGS="-Djogl.debug.ExtensionAvailabilityCache" "-Djogl.debug=all" "-Dnewt.debug=all" "-Dnativewindow.debug=all" "-Djogamp.debug.ProcAddressHelper=true" "-Djogamp.debug.NativeLibrary=true" "-Djogamp.debug.NativeLibrary.Lookup=true"
REM set D_ARGS="-Djogl.debug=all" "-Dnewt.debug=all" "-Dnativewindow.debug=all" "-Djogamp.debug.NativeLibrary=true"
REM set D_ARGS="-Djogl.debug.GLContext" "-Djogl.debug.ExtensionAvailabilityCache" "-Djogamp.debug.ProcAddressHelper=true"
REM set D_ARGS="-Dnativewindow.debug.GraphicsConfiguration"
REM set D_ARGS="-Djogl.debug.GLDrawable" "-Dnativewindow.debug.GraphicsConfiguration" "-Djogl.debug.CapabilitiesChooser"
REM set D_ARGS="-Djogamp.debug.JNILibLoader=true" "-Djogamp.debug.NativeLibrary=true" "-Djogamp.debug.NativeLibrary.Lookup=true" "-Djogl.debug.GLProfile=true"
REM set D_ARGS="-Djogl.debug=all" "-Dnewt.debug=all" "-Dnativewindow.debug=all" "-Djogamp.debug.Lock" "-Djogamp.debug.Lock.TraceLock"
REM set D_ARGS="-Djogl.debug=all" "-Dnativewindow.debug=all"
REM set D_ARGS="-Djogl.debug=all"
REM set D_ARGS="-Djogl.debug.GLCanvas" "-Djogl.debug.Animator" "-Djogl.debug.GLContext" "-Djogl.debug.GLContext.TraceSwitch" "-Djogl.debug.DebugGL" "-Djogl.debug.TraceGL"
REM set D_ARGS="-Djogl.debug.GLCanvas" "-Djogl.debug.Animator" "-Djogl.debug.GLContext" "-Djogl.debug.GLContext.TraceSwitch" "-Djogl.windows.useWGLVersionOf5WGLGDIFuncSet"
REM set D_ARGS="-Djogl.debug.GLCanvas" "-Djogl.debug.Animator" "-Djogl.debug.GLContext" "-Djogl.debug.GLContext.TraceSwitch"
REM set D_ARGS="-Djogl.debug.GLCanvas" "-Djogl.debug.GLJPanel" "-Djogl.debug.TileRenderer" "-Djogl.debug.TileRenderer.PNG"
REM set D_ARGS="-Djogl.debug.GLCanvas" "-Djogl.debug.GLJPanel" "-Djogl.debug.TileRenderer"
REM set D_ARGS="-Djogl.gljpanel.noverticalflip"
REM set D_ARGS="-Dnewt.debug=all"
REM set D_ARGS="-Dnewt.debug.Window"
REM set D_ARGS="-Dnewt.debug.Window.KeyEvent"
REM set D_ARGS="-Dnewt.debug.Window" "-Dnewt.debug.Window.KeyEvent" "-Dnewt.debug.EDT"
REM set D_ARGS="-Dnewt.debug.Window.MouseEvent"
REM set D_ARGS="-Dnewt.debug.Window.MouseEvent" "-Dnewt.debug.Window.KeyEvent"
REM set D_ARGS="-Dnewt.debug.Window" "-Dnewt.debug.Display"
REM set D_ARGS="-Djogl.debug.GLDebugMessageHandler" "-Djogl.debug.DebugGL" "-Djogl.debug.TraceGL"
REM set D_ARGS="-Djogl.debug.DebugGL" "-Djogl.debug.GLDebugMessageHandler" "-Djogl.debug.GLSLCode"
REM set D_ARGS="-Djogl.debug.GLContext" "-Dnewt.debug=all"
REM set D_ARGS="-Dnewt.debug.Window" "-Dnativewindow.debug.TraceLock"
REM set D_ARGS="-Dnativewindow.debug.TraceLock"
REM set D_ARGS="-Dnewt.debug.Display" "-Dnewt.debug.EDT" "-Dnewt.debug.Window"
REM set D_ARGS="-Dnewt.debug.Window" "-Dnewt.debug.Display" "-Dnewt.debug.EDT" "-Djogl.debug.GLContext"
REM set D_ARGS="-Dnewt.debug.Screen" "-Dnewt.debug.EDT" "-Dnativewindow.debug=all"
REM set D_ARGS="-Dnewt.debug.Screen"
REM set D_ARGS="-Dnewt.debug.Screen" "-Dnewt.debug.Window"
REM set D_ARGS="-Dnewt.debug.Window" "-Dnewt.debug.Display" "-Dnewt.test.Window.reparent.incompatible=true"

REM set D_ARGS="-Djogamp.debug.ReflectionUtil" "-Djogamp.debug.ReflectionUtil.forNameStats"
REM set D_ARGS="-Djogamp.debug.ReflectionUtil.forNameStats"

REM set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.java2d.opengl=true" "-Dsun.awt.noerasebackground=true"
REM set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.java2d.d3d=false" "-Dsun.java2d.ddoffscreen=false" "-Dsun.java2d.gdiblit=false" "-Dsun.java2d.opengl=false" "-Dsun.awt.noerasebackground=true" "-Xms512m" "-Xmx1024m"
REM set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.java2d.d3d=false" "-Dsun.java2d.ddoffscreen=false" "-Dsun.java2d.gdiblit=false" "-Dsun.java2d.opengl=true" "-Dsun.awt.noerasebackground=true" "-Xms512m" "-Xmx1024m"

scripts\tests-win.bat %*

