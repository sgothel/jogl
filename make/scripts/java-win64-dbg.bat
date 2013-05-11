
set BLD_SUB=build-win64
set J2RE_HOME=c:\jre1.6.0_35_x64
set JAVA_HOME=c:\jdk1.6.0_35_x64
set ANT_PATH=C:\apache-ant-1.8.2

set PROJECT_ROOT=D:\projects\jogamp\jogl
set BLD_DIR=..\%BLD_SUB%

set FFMPEG_LIB=%PROJECT_ROOT%\make\lib\ffmpeg\x64

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw\bin;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PROJECT_ROOT%\make\lib\external\PVRVFrame\OGLES-2.0\Windows_x86_64;%PATH%
REM set LIB_DIR=%BLD_DIR%\lib;..\..\gluegen\%BLD_SUB%\obj
set LIB_DIR=%FFMPEG_LIB%

set CP_ALL=.;%BLD_DIR%\jar\jogl-all.jar;%BLD_DIR%\jar\jogl-test.jar;..\..\gluegen\%BLD_SUB%\gluegen-rt.jar;..\..\gluegen\make\lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;%BLD_DIR%\..\make\lib\swt\win32-win32-x86_64\swt-debug.jar

echo CP_ALL %CP_ALL%

REM set D_ARGS="-Djogl.debug.GLContext" "-Djogl.debug.FBObject"
REM set D_ARGS="-Djogl.debug.GLDrawable" "-Djogl.debug.EGLDrawableFactory.DontQuery"
REM set D_ARGS="-Djogl.debug.GLDrawable" "-Djogl.debug.EGLDrawableFactory.QueryNativeTK"
REM set D_ARGS="-Djogl.debug=all" "-Dnewt.debug=all" "-Dnativewindow.debug=all"
REM set D_ARGS="-Djogl.debug.GLDrawable" "-Djogl.debug.GLContext" "-Djogl.debug.GLCanvas"
REM set D_ARGS="-Djogl.debug=all" "-Dnewt.debug=all" "-Dnativewindow.debug=all" "-Djogamp.debug=all" "-Djogl.debug.EGLDrawableFactory.DontQuery"
REM set D_ARGS="-Dnativewindow.debug.GraphicsConfiguration -Djogl.debug.CapabilitiesChooser -Djogl.debug.GLProfile"
REM set D_ARGS="-Djogamp.debug.IOUtil" "-Djogl.debug.GLSLCode" "-Djogl.debug.GLMediaPlayer"
REM set D_ARGS="-Djogamp.debug.NativeLibrary=true" "-Djogamp.debug.NativeLibrary.Lookup=true" "-Djogl.debug.GLSLCode"
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
REM set D_ARGS="-Dnewt.debug.Window"
REM set D_ARGS="-Dnewt.debug.Window.KeyEvent"
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
REM set D_ARGS="-Dnewt.debug.Window" "-Dnewt.debug.Display" "-Dnewt.test.Window.reparent.incompatible=true"

REM set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.java2d.opengl=true" "-Dsun.awt.noerasebackground=true"
REM set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.java2d.d3d=false" "-Dsun.java2d.ddoffscreen=false" "-Dsun.java2d.gdiblit=false" "-Dsun.java2d.opengl=false" "-Dsun.awt.noerasebackground=true" "-Xms512m" "-Xmx1024m"
REM set X_ARGS="-Dsun.java2d.noddraw=true" "-Dsun.java2d.d3d=false" "-Dsun.java2d.ddoffscreen=false" "-Dsun.java2d.gdiblit=false" "-Dsun.java2d.opengl=true" "-Dsun.awt.noerasebackground=true" "-Xms512m" "-Xmx1024m"

%J2RE_HOME%\bin\java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %D_ARGS% %X_ARGS% %* > java-win64-dbg.log 2>&1
tail java-win64-dbg.log
REM %J2RE_HOME%\bin\java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %D_ARGS% %X_ARGS% %*
