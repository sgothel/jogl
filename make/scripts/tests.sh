#! /bin/bash

if [ -z "$1" -o -z "$2" -o -z "$3" ] ; then
    echo Usage $0 java-exe java-xargs build-dir
    exit 0
fi

javaexe="$1"
shift
javaxargs=$1
shift
bdir="$1"
shift

if [ ! -x "$javaexe" ] ; then
    echo java-exe "$javaexe" is not an executable
    exit 1
fi
if [ ! -d "$bdir" ] ; then
    echo build-dir "$bdir" is not a directory
    exit 1
fi

rm -f java-run.log

spath=`dirname $0`

. $spath/setenv-jogl.sh "$bdir" JOGL_ALL
unset CLASSPATH

MOSX=0
MOSX_MT=0
uname -a | grep -i Darwin && MOSX=1
if [ $MOSX -eq 1 ] ; then
    echo setup OSX environment vars
    #export NSZombieEnabled=YES
    export NSTraceEvents=YES
    #export OBJC_PRINT_EXCEPTIONS=YES
    echo NSZombieEnabled $NSZombieEnabled 2>&1 | tee -a java-run.log
    echo NSTraceEvents $NSTraceEvents  2>&1 | tee -a java-run.log
    echo OBJC_PRINT_EXCEPTIONS $OBJC_PRINT_EXCEPTIONS  2>&1 | tee -a java-run.log
    MOSX_MT=1
fi

# We use TempJarCache and JAR files per default now!
export USE_BUILDDIR=0
#export USE_BUILDDIR=1

if [ $USE_BUILDDIR -eq 1 ] ; then
    export LD_LIBRARY_PATH=$JOGAMP_LD_LIBRARY_PATH
    export DYLD_LIBRARY_PATH=$JOGAMP_DYLD_LIBRARY_PATH
fi

#export LD_LIBRARY_PATH=$spath/../../EXTERNAL/PVRVFrame/OGLES-2.0/Linux_x86_64:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=$spath/../../EXTERNAL/PVRVFrame/OGLES-2.0/Linux_x86_32:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=/usr/local/projects/Xorg.modular/build-x86_64/lib:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=/opt-linux-x86_64/x11lib-1.3:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=/opt-linux-x86_64/mesa-7.8.1/lib64:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=/usr/lib/mesa:/usr/lib32/mesa:$LD_LIBRARY_PATH
#export LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu/mesa:/usr/lib/i386-linux-gnu/mesa:$LD_LIBRARY_PATH
#export LIBGL_DRIVERS_PATH=/usr/lib/x86_64-linux-gnu/dri:/usr/lib/i386-linux-gnu/dri
#export LD_LIBRARY_PATH=`pwd`/../../EXTERNAL/mesa_git/x86_64-linux-gnu:$LD_LIBRARY_PATH
#export LIBGL_DRIVERS_PATH=`pwd`/../../EXTERNAL/mesa_git/x86_64-linux-gnu/dri
#export LD_LIBRARY_PATH=`pwd`/../../EXTERNAL/mesa_901/x86_64-linux-gnu:$LD_LIBRARY_PATH
#export LIBGL_DRIVERS_PATH=`pwd`/../../EXTERNAL/mesa_901/x86_64-linux-gnu/dri
#export LD_LIBRARY_PATH=`pwd`/../../EXTERNAL/mesa_900/x86_64-linux-gnu:$LD_LIBRARY_PATH
#export LIBGL_DRIVERS_PATH=`pwd`/../../EXTERNAL/mesa_900/x86_64-linux-gnu/dri

#export LIBGL_DEBUG=verbose 
#export MESA_DEBUG=true 
#export LIBGL_ALWAYS_SOFTWARE=true
#export INTEL_DEBUG="buf bat"
#export INTEL_STRICT_CONFORMANCE=1 

# export ALSOFT_LOGLEVEL=[0..4]
# export ALSOFT_LOGLEVEL=3
# export ALSOFT_LOGLEVEL=3
#export ALSOFT_TRAP_ERROR=true
#export ALSOFT_TRAP_AL_ERROR=true
#export ALSOFT_TRAP_ALC_ERROR=true
#export ALSOFT_LOGFILE=openal-soft.log

which "$javaexe" 2>&1 | tee -a java-run.log
"$javaexe" -version 2>&1 | tee -a java-run.log
echo LD_LIBRARY_PATH $LD_LIBRARY_PATH 2>&1 | tee -a java-run.log
echo LIBXCB_ALLOW_SLOPPY_LOCK: $LIBXCB_ALLOW_SLOPPY_LOCK 2>&1 | tee -a java-run.log
echo LIBGL_DRIVERS_PATH: $LIBGL_DRIVERS_PATH 2>&1 | tee -a java-run.log
echo LIBGL_DEBUG: $LIBGL_DEBUG 2>&1 | tee -a java-run.log
echo LIBGL_ALWAYS_INDIRECT: $LIBGL_ALWAYS_INDIRECT 2>&1 | tee -a java-run.log
echo LIBGL_ALWAYS_SOFTWARE: $LIBGL_ALWAYS_SOFTWARE 2>&1 | tee -a java-run.log
echo SWT_CLASSPATH: $SWT_CLASSPATH 2>&1 | tee -a java-run.log
echo MacOsX $MOSX

function jrun() {
    awton=$1
    shift
    swton=$1
    shift

    #D_ARGS="-Djogl.debug.DebugGL"
    #D_ARGS="-Djogl.debug.TraceGL"
    #D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL"
    #D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL -Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all"

    #D_ARGS="-Djogamp.debug=all"
    #D_ARGS="-Dnativewindow.debug=all"
    #D_ARGS="-Djogl.debug=all"
    #D_ARGS="-Djogl.debug=all -Dnewt.debug=all -Djogl.debug.DebugGL"
    #D_ARGS="-Dnewt.debug=all"
    #D_ARGS="-Djogl.debug=all -Dnewt.debug=all"
    #D_ARGS="-Djogl.debug=all -Dnativewindow.debug=all"
    #D_ARGS="-Djogamp.debug=all -Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all"

    #D_ARGS="-Djogamp.debug.NativeLibrary=true -Djogamp.debug.JNILibLoader=true"
    #D_ARGS="-Djogl.debug.GLContext -Djogamp.debug.NativeLibrary -Djogamp.debug.JNILibLoader -Djogl.debug.DebugGL -Djogl.debug.GLDebugMessageHandler"
    #D_ARGS="-Djogamp.debug.ProcAddressHelper -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup -Djogamp.debug.JNILibLoader -Djogl.debug.GLContext"
    #D_ARGS="-Djogamp.debug.ProcAddressHelper -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup -Djogamp.debug.JNILibLoader -Djogamp.debug.TempJarCache -Djogamp.debug.JarUtil"
    #D_ARGS="-Djogamp.debug.ProcAddressHelper -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup -Djogamp.debug.JNILibLoader -Djogamp.debug.TempJarCache -Djogamp.debug.JarUtil -Djogl.glu.nojava=true"

    #D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all"
    #D_ARGS="-Dnativewindow.debug=all -Dnewt.debug.Window"
    #D_ARGS="-Djogl.debug=all -Dnativewindow.debug=all -Dnewt.debug=all -Djogamp.debug.Lock"

    #D_ARGS="-Djogamp.debug.ReflectionUtil -Djogamp.debug.ReflectionUtil.forNameStats"
    #D_ARGS="-Djogamp.debug.ReflectionUtil.forNameStats"

    #D_ARGS="-Dnativewindow.debug.X11Util.ATI_HAS_NO_XCLOSEDISPLAY_BUG"
    #D_ARGS="-Dnativewindow.debug.X11Util.ATI_HAS_NO_MULTITHREADING_BUG"
    #D_ARGS="-Djogl.disable.opengldesktop"
    #D_ARGS="-Djogl.disable.opengles"
    #D_ARGS="-Djogl.disable.openglcore"
    #D_ARGS="-Djogl.debug=all -Djogl.disable.openglarbcontext"
    #D_ARGS="-Djogl.debug.DebugGL -Dnewt.debug.Window"
    #D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.FBObject"
    #D_ARGS="-Djogl.debug.FBObject -Djogl.debug.TraceGL -Djogl.debug.GLBufferStateTracker"
    #D_ARGS="-Djogl.debug.FBObject"
    #D_ARGS="-Djogl.debug.FBObject.Swap -Djogl.debug.GLJPanel.Frames"
    #D_ARGS="-Djogl.debug.GLBufferStateTracker -Djogl.debug.GLBufferObjectTracker -Djogamp.debug.Lock -Djogamp.common.utils.locks.Lock.timeout=600000 -Dnewt.debug.EDT"
    #D_ARGS="-Djogl.debug.GLBufferStateTracker -Djogl.debug.GLBufferObjectTracker"
    #D_ARGS="-Djogl.debug.GLBufferObjectTracker"
    #D_ARGS="-Djogl.debug.GLBufferObjectTracker -Djogl.debug.GLArrayData -Djogl.debug.TraceGL -Djogl.debug.DebugGL"
    #D_ARGS="-Djogl.debug.GLSLCode"
    #D_ARGS="-Djogl.debug.GLSLCode -Djogl.debug.TraceGL"
    #D_ARGS="-Djogl.debug.GLSLCode -Djogl.debug.DebugGL"
    #D_ARGS="-Djogl.debug.GLContext -Dnativewindow.debug.JAWT -Dnewt.debug.Window"
    #D_ARGS="-Dnativewindow.debug.JAWT -Djogl.debug.GLCanvas"
    #D_ARGS="-Dnativewindow.debug.JAWT -Djogamp.debug.TaskBase.TraceSource"
    #D_ARGS="-Dnativewindow.debug.JAWT"
    #D_ARGS="-Djogl.debug.GLContext.TraceSwitch"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLContext.TraceSwitch"
    #D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL -Djogl.debug.FixedFuncPipeline -Djogl.debug.GLSLState -Djogl.debug.GLSLCode"
    #D_ARGS="-Djogl.debug.FixedFuncPipeline -Djogl.debug.GLSLCode"
    #D_ARGS="-Djogl.debug.FixedFuncPipeline -Djogl.debug.GLSLState"
    #D_ARGS="-Djogl.debug.FixedFuncPipeline"
    #D_ARGS="-Djogl.debug.ImmModeSink.Buffer -Djogl.debug.ImmModeSink.Draw"
    #D_ARGS="-Djogl.debug.FixedFuncPipeline -Djogl.debug.GLSLState -Djogl.debug.ImmModeSink.Buffer -Djogl.debug.ImmModeSink.Draw"
    #D_ARGS="-Djogl.debug.TraceGL -Djogl.debug.DebugGL -Djogl.debug.GLSLCode"
    #D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.FBObject -Djogl.debug.GLContext -Djogl.debug.GLDrawable -Djogl.debug.GLCanvas -Dnewt.debug.Window"
    #D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.GLContext -Djogl.debug.GLContext.TraceSwitch -Djogl.debug.GLDrawable"
    #D_ARGS="-Dnativewindow.debug.GraphicsConfiguration -Djogl.debug.GLDrawable -Djogl.debug.GLContext -Djogl.debug.FBObject"
    #D_ARGS="-Djogl.debug.GLContext"
    #D_ARGS="-Djogl.debug.GLContext -Dnativewindow.debug.GraphicsConfiguration"
    #D_ARGS="-Djogl.debug.GLContext -Dnativewindow.debug.X11Util.XSync"
    #D_ARGS="-Dnativewindow.debug.GraphicsConfiguration"
    #D_ARGS="-Djogl.debug.GLDrawable -Djogl.debug.GLContext -Djogl.debug.GLProfile"
    #D_ARGS="-Djogamp.debug.ProcAddressHelper -Djogamp.debug.NativeLibrary -Djogl.debug.GLContext -Djogl.debug.GLProfile"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLProfile"
    #D_ARGS="-Djogl.debug=all"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLDrawable"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLDrawable -Dnativewindow.debug.GraphicsConfiguration"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLDrawable -Djogl.debug.GLProfile -Djogamp.common.utils.locks.Lock.timeout=600000 -Djogamp.debug.Lock"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLDrawable -Dnativewindow.debug.ProxySurface -Djogl.debug.GLProfile -Djogl.disable.opengldesktop"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLDrawable -Dnativewindow.debug.ProxySurface -Djogl.debug.GLProfile"
    #D_ARGS="-Djogl.disable.surfacelesscontext -Djogl.debug.GLContext -Djogl.debug.GLDrawable -Djogl.debug.GLJPanel -Djogl.debug.DebugGL"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLJPanel -Djogl.debug.DebugGL"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLJPanel"
    #D_ARGS="-Djogl.debug.GLDrawable -Djogl.debug.GLContext -Djogl.debug.GLJPanel"
    #D_ARGS="-Djogl.debug.GLJPanel"
    #D_ARGS="-Djogl.debug.GLContext.NoProfileAliasing"
    #D_ARGS="-Djogl.debug.GLDrawable -Dnativewindow.debug.X11Util -Dnativewindow.debug.NativeWindow -Dnewt.debug.Display -Dnewt.debug.Screen -Dnewt.debug.Window"
    #D_ARGS="-Djogl.debug.Animator"
    #D_ARGS="-Djogl.debug.Animator -Djogl.debug.GLDrawable -Dnativewindow.debug.NativeWindow"
    #D_ARGS="-Djogl.debug=all -Dnewt.debug=all"
    #D_ARGS="-Djogl.debug.GLDrawable -Djogl.debug.GLContext -Djogl.debug.GLCanvas"
    #D_ARGS="-Djogl.debug.GLDrawable"
    #D_ARGS="-Djogl.debug.GLEventListenerState"
    #D_ARGS="-Djogl.debug.GLDebugMessageHandler"
    #D_ARGS="-Djogl.debug.GLDebugMessageHandler -Djogl.debug.DebugGL"
    #D_ARGS="-Djogl.debug.GLDebugMessageHandler -Djogl.debug.DebugGL -Djogl.debug.GLContext"
    #D_ARGS="-Djogl.debug.GLDebugMessageHandler -Djogl.debug.TraceGL -Djogl.debug.DebugGL -Djogl.debug.GLSLCode -Djogl.debug.GLSLState"
    #D_ARGS="-Djogl.debug.GLDebugMessageHandler -Djogl.debug.DebugGL -Djogl.debug.TraceGL"
    #D_ARGS="-Djogl.1thread=false -Djogl.debug.Threading"
    #D_ARGS="-Djogl.1thread=true -Djogl.debug.Threading"
    #D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL -Djogl.debug.GLContext.TraceSwitch -Djogl.debug=all"
    #D_ARGS="-Djogl.debug.GLArrayData"
    #D_ARGS="-Dnewt.debug.Screen -Dnewt.debug.Window"
    #D_ARGS="-Dnewt.debug.Window"
    #D_ARGS="-Dnewt.debug.Screen"
    #D_ARGS="-Dnewt.window.icons=null,null"
    #D_ARGS="-Dnewt.window.icons=../src/test/com/jogamp/opengl/test/junit/jogl/util/texture/test-ntscI_4-01-160x90.png,../src/test/com/jogamp/opengl/test/junit/jogl/util/texture/cross-grey-alpha-16x16.png -Djogamp.debug.IOUtil"
    #D_ARGS="-Dnewt.test.Screen.disableRandR13"
    #D_ARGS="-Dnewt.test.Screen.disableScreenMode -Dnewt.debug.Screen"
    #D_ARGS="-Dnewt.debug.Screen -Djogl.debug.Animator"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLProfile"
    #D_ARGS="-Djogl.debug.GLProfile"
    #D_ARGS="-Djogl.debug.EGLDisplayUtil -Djogl.debug.GLContext"
    #D_ARGS="-Dnewt.debug.MainThread"
    #D_ARGS="-Dnativewindow.debug.GraphicsConfiguration -Dnativewindow.debug.NativeWindow"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.Animator -Djogl.debug.GLDrawable -Djogl.debug.GLContext -Djogl.debug.GLContext.TraceSwitch"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.ExtensionAvailabilityCache"
    #D_ARGS="-Djogl.debug.EGLDrawableFactory.QueryNativeTK -Djogl.debug.GLDrawable"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLProfile -Djogl.debug.GLDrawable -Djogl.debug.EGLDisplayUtil -Dnativewindow.debug.GraphicsDevice"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLProfile -Djogl.debug.GLDrawable -Djogl.debug.EGLDisplayUtil -Djogl.debug.EGLDrawableFactory.QueryNativeTK"
    #D_ARGS="-Djogl.debug.EGLDisplayUtil -Dnativewindow.debug.GraphicsConfiguration -Djogl.debug.GLDrawable"
    #D_ARGS="-Djogl.debug.EGLDisplayUtil -Dnativewindow.debug.X11Util"
    #D_ARGS="-Dnativewindow.debug.X11Util -Dnativewindow.debug.X11Util.TraceDisplayLifecycle -Djogl.debug.EGLDisplayUtil -Djogl.debug.GLDrawable"
    #D_ARGS="-Djogl.debug.EGLDisplayUtil -Djogl.debug.GLDrawable"
    #D_ARGS="-Dnativewindow.debug.NativeWindow -Dnewt.debug.Window -Dnewt.debug.Screen -Dnewt.debug.Display"
    #D_ARGS="-Djogl.debug.GLCanvas -Dnewt.debug.Window -Dnewt.debug.Display -Dnewt.debug.EDT -Djogl.debug.Animator"
    #D_ARGS="-Dnewt.debug.Display -Dnewt.debug.EDT -Dnewt.debug.Window"
    #D_ARGS="-Dnewt.debug.EDT -Dnewt.debug.Window -Djogl.debug.GLContext"
    #D_ARGS="-Dnativewindow.debug.X11Util.XErrorStackDump -Dnativewindow.debug.X11Util.TraceDisplayLifecycle -Dnativewindow.debug.X11Util"
    #D_ARGS="-Dnativewindow.debug.X11Util -Djogl.debug.GLContext -Djogl.debug.GLDrawable -Dnewt.debug=all"
    #D_ARGS="-Dnativewindow.debug.X11Util -Dnativewindow.debug.X11Util.XSync"
    #D_ARGS="-Dnativewindow.debug.X11Util.XSync -Dnativewindow.debug.X11Util.TraceDisplayLifecycle"
    #D_ARGS="-Dnativewindow.debug.X11Util.XSync -Dnewt.debug.Window"
    #D_ARGS="-Dnativewindow.debug.NativeWindow -Dnativewindow.debug.X11Util"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=3000 -Djogamp.debug.Lock"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=3000 -Djogamp.debug.Lock -Djogl.debug.GLContext.TraceSwitch"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=3000 -Djogamp.debug.Lock -Dnativewindow.debug.ToolkitLock.TraceLock"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=600000 -Djogamp.debug.Lock -Dnewt.debug=all"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=600000 -Djogamp.debug.Lock -Djogamp.debug.Lock.TraceLock -Dnativewindow.debug.ToolkitLock.TraceLock"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=600000 -Djogamp.debug.Lock -Dnativewindow.debug.X11Util"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=600000 -Dnativewindow.debug.X11Util"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=600000"
    #D_ARGS="-Dnewt.debug.EDT -Djogamp.common.utils.locks.Lock.timeout=600000 -Djogl.debug.Animator -Dnewt.debug.Display -Dnewt.debug.Screen"
    #D_ARGS="-Dnewt.debug.Window -Djogamp.common.utils.locks.Lock.timeout=600000"
    #D_ARGS="-Dnewt.debug=all -Djogamp.common.utils.locks.Lock.timeout=600000"
    #D_ARGS="-Djogl.debug.Animator -Dnewt.debug=all"
    #D_ARGS="-Dnewt.debug.EDT -Dnewt.debug.Display -Dnativewindow.debug.X11Util -Djogl.debug.GLDrawable -Djogl.debug.GLCanvas"
    #D_ARGS="-Dnativewindow.debug.GraphicsConfiguration -Djogl.debug.CapabilitiesChooser -Djogl.debug.GLDrawable -Djogl.debug.GLProfile"
    #D_ARGS="-Dnewt.debug.Screen -Dnewt.debug.EDT -Djogamp.debug.Lock"
    #D_ARGS="-Dnewt.debug.EDT"
    #D_ARGS="-Dnewt.debug.Window -Dnewt.debug.Display -Dnewt.debug.EDT -Djogl.debug.GLContext"
    #D_ARGS="-Dnewt.debug.Window -Djogl.debug.Animator -Dnewt.debug.Screen"
    #D_ARGS="-Dnativewindow.debug.JAWT -Dnewt.debug.Window -Djogl.debug.GLJPanel -Djogl.debug.GLCanvas -Djogamp.debug.TempJarCache"
    #D_ARGS="-Dnewt.debug.Window.KeyEvent"
    #D_ARGS="-Dnewt.debug.Window.MouseEvent"
    #D_ARGS="-Dnewt.debug.Window.MouseEvent -Dnewt.debug.Window.KeyEvent"
    #D_ARGS="-Dnewt.debug.Window -Dnativewindow.debug=all"
    #D_ARGS="-Dnewt.debug.Window -Dnativewindow.debug.JAWT -Djogl.debug.Animator"
    #D_ARGS="-Dnewt.debug.Window -Djogl.debug.GLDrawable"
    #D_ARGS="-Dnewt.debug.Window -Dnewt.debug.Window.KeyEvent"
    #D_ARGS="-Dnewt.debug.Window -Dnewt.debug.Window.MouseEvent -Dnewt.debug.Window.KeyEvent"
    D_ARGS="-Dnewt.debug.Window"
    #D_ARGS="-Dnewt.debug.Window.visibility.failure.freeze"
    #D_ARGS="-Xprof"
    #D_ARGS="-Dnativewindow.debug=all -Djogl.debug=all -Dnewt.debug=all"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.Java2D -Djogl.debug.GLJPanel"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.Java2D -Djogl.debug.GLJPanel -Djogl.debug.Animator"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.Java2D -Djogl.debug.GLJPanel -Djogl.gljpanel.noglsl"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.Java2D -Djogl.debug.GLJPanel -Djogl.debug.FBObject.MaxTextureSize=512"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.Java2D -Djogl.debug.GLJPanel -Djogl.gljpanel.noglsl -Djogl.debug.FBObject.MaxTextureSize=512"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.Java2D -Djogl.debug.GLJPanel -Dnativewindow.awt.nohidpi"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.Java2D -Djogl.debug.GLJPanel -Djogl.debug.GLJPanel.Viewport"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.Java2D -Djogl.debug.GLJPanel -Djogl.debug.FBObject"
    #D_ARGS="-Djogl.debug.GLJPanel -Djogl.debug.FBObject -Djogl.fbo.force.nocolorrenderbuffer"
    #D_ARGS="-Djogl.debug.GLJPanel -Djogl.debug.FBObject"
    #D_ARGS="-Djogl.fbo.force.none"
    #D_ARGS="-Djogl.debug.GLJPanel -Djogl.debug.DebugGL"
    #D_ARGS="-Djogl.gljpanel.noverticalflip"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.Animator"
    #D_ARGS="-Dnativewindow.debug.X11Util.XSync -Dnativewindow.debug.ToolkitLock.TraceLock"
    #D_ARGS="-Dnativewindow.debug.NativeWindow"
    #D_ARGS="-Dnativewindow.osx.calayer.bugfree"
    #D_ARGS="-Dnativewindow.debug.ToolkitLock"
    #D_ARGS="-Djogl.debug.graph.curve"
    #D_ARGS="-Djogl.debug.graph.curve -Djogl.debug.GLSLCode -Djogl.debug.DebugGL -Djogl.debug.TraceGL"
    #D_ARGS="-Djogl.debug.graph.curve"
    #D_ARGS="-Djogl.debug.graph.curve -Djogl.debug.GLSLCode"
    #D_ARGS="-Djogl.debug.DebugGL"
    #D_ARGS="-Djogl.debug.graph.curve -Djogl.debug.graph.curve.Instance -Djogl.debug.GLSLCode"
    #D_ARGS="-Djogl.debug.GLSLCode -Djogl.debug.graph.curve.triangulation.LINE_AA -Djogl.debug.graph.curve.Triangulation -Djogl.debug.graph.font.Renderer"
    #D_ARGS="-Djogl.debug.graph.font.Renderer"
    #D_ARGS="-Djogl.debug.GLSLCode -Djogl.debug.graph.curve.vbaa.resizeLowerBoundary=100"
    #D_ARGS="-Djogl.debug.GLSLCode"
    #D_ARGS="-Djogl.debug.graph.curve.instance"
    #D_ARGS="-Djogl.debug.graph.curve -Djogl.debug.GLSLCode -Djogl.debug.TraceGL"
    #D_ARGS="-Djogl.debug.graph.curve -Djogl.debug.GLSLState"
    #D_ARGS="-Djogamp.debug.IOUtil"
    #D_ARGS="-Djogamp.debug.TempJarCache -Djogamp.debug.JarUtil -Djogamp.debug.IOUtil"
    #D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.TempJarCache -Djogamp.debug.JarUtil -Djogamp.debug.IOUtil"
    #D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.TempJarCache -Djogamp.debug.JarUtil"
    #D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.TempJarCache -Djogamp.debug.JarUtil -Djogamp.gluegen.UseTempJarCache=false"
    #D_ARGS="-Djogamp.debug.JNILibLoader.Perf"
    #D_ARGS="-Dnewt.test.EDTMainThread -Dnewt.debug.MainThread"
    #C_ARG="com.jogamp.newt.util.MainThread"
    #D_ARGS="-Dnewt.debug.MainThread"
    #D_ARGS="-Dnewt.debug=all -Djogamp.debug.Lock.TraceLock -Djogamp.common.utils.locks.Lock.timeout=600000"
    #D_ARGS="-Dnewt.debug=all -Djogamp.debug.Lock -Djogamp.debug.Lock.TraceLock"
    #D_ARGS="-Djogl.debug.GLContext -Dnewt.debug=all -Djogamp.debug.Lock -Djogamp.common.utils.locks.Lock.timeout=10000"
    #D_ARGS="-Djogl.debug.GLContext -Dnewt.debug=all"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.GLJPanel -Djogl.debug.TileRenderer -Djogl.debug.TileRenderer.PNG"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.GLJPanel -Djogl.debug.TileRenderer"
    #D_ARGS="-Djogl.debug.PNG -Dnewt.debug.Display.PointerIcon"
    #D_ARGS="-Djogl.debug.JPEGImage -Djogamp.debug.Bitstream"
    #D_ARGS="-Djogl.debug.GLDrawable -Dnativewindow.debug.GraphicsConfiguration -Djogl.debug.CapabilitiesChooser"
    #X_ARGS="-Dsun.java2d.noddraw=True -Dsun.java2d.opengl=True -Dsun.java2d.xrender=false"
    #X_ARGS="-Dsun.java2d.noddraw=True -Dsun.java2d.opengl=false -Dsun.java2d.xrender=false"
    #X_ARGS="-verbose:jni"
    #X_ARGS="-Xcheck:jni"
    #X_ARGS="-Xcheck:jni -verbose:jni"
    #X_ARGS="-Xrs"
    #X_ARGS="-Dsun.awt.disableMixing=true"
    #D_ARGS="-Djogamp.debug.NativeLibrary=true -Djogamp.debug.JNILibLoader=true -Djogl.debug.GLMediaPlayer"
    #D_ARGS="-Djogl.debug.GLMediaPlayer"
    #D_ARGS="-Djogamp.debug.IOUtil -Djogl.debug.GLSLCode -Djogl.debug.GLMediaPlayer"
    #D_ARGS="-Djogl.debug.AudioSink -Djoal.openal.lib=system"
    #D_ARGS="-Djogl.debug.AudioSink -Djogl.debug.AudioSink.trace"
    #D_ARGS="-Djogl.debug.AudioSink -Djogl.debug.GLMediaPlayer"
    #D_ARGS="-Djogl.debug.GLMediaPlayer -Djogl.debug.GLMediaPlayer.Native"
    #D_ARGS="-Djogl.debug.GLMediaPlayer -Djogl.debug.GLSLCode"
    #D_ARGS="-Djogl.debug.GLMediaPlayer.StreamWorker.delay=25 -Djogl.debug.GLMediaPlayer"
    #D_ARGS="-Djogl.debug.GLMediaPlayer.Native"
    #D_ARGS="-Djogl.debug.StereoDevice -Djogl.debug.StereoDevice.DumpData"
    #D_ARGS="-Djogl.debug.StereoDevice"
    #D_ARGS="-Dnewt.debug.Screen -Djogl.debug.StereoDevice"
    #D_ARGS="-Dnewt.debug.Screen -Dnewt.test.Screen.disableRandR13"
    #D_ARGS="-Dnewt.debug.Screen"

    if [ $awton -eq 1 ] ; then
        export USE_CLASSPATH=$JOGAMP_ALL_AWT_CLASSPATH
        echo USE_CLASSPATH $USE_CLASSPATH
        X_ARGS="-Djava.awt.headless=false $X_ARGS"
    elif [ $swton -eq 1 ] ; then
        export USE_CLASSPATH=$JOGAMP_ALL_AWT_CLASSPATH
        echo USE_CLASSPATH $USE_CLASSPATH
        X_ARGS="-Djava.awt.headless=true $X_ARGS"
    elif [ $awton -eq -1 ] ; then
        export USE_CLASSPATH=$JOGAMP_ATOMICS_NOAWT_CLASSPATH
    else
        #export USE_CLASSPATH=$JOGAMP_ALL_AWT_CLASSPATH
        export USE_CLASSPATH=$JOGAMP_ALL_NOAWT_CLASSPATH
        #export USE_CLASSPATH=$JOGAMP_MOBILE_CLASSPATH
        #export USE_CLASSPATH=.:$GLUEGEN_JAR:$JOGL_BUILDDIR/jar/atomic/jogl.jar:$JOGL_BUILDDIR/jar/atomic/jogl-gldesktop.jar:$JOGL_BUILDDIR/jar/atomic/jogl-os-x11.jar:$JOGL_BUILDDIR/jar/atomic/jogl-util.jar:$JOGL_BUILDDIR/jar/atomic/nativewindow.jar:$JOGL_BUILDDIR/jar/atomic/nativewindow-os-x11.jar:$JOGL_BUILDDIR/jar/atomic/newt.jar:$JOGL_BUILDDIR/jar/atomic/newt-driver-x11.jar:$JOGL_BUILDDIR/jar/atomic/newt-ogl.jar:$JOGL_BUILDDIR/jar/jogl-test.jar:$JUNIT_JAR:$ANT_JARS
        X_ARGS="-Djava.awt.headless=true $X_ARGS"
    fi

    if [ $USE_BUILDDIR -eq 1 ] ; then
        export USE_CLASSPATH=.:$GLUEGEN_BUILDDIR/classes:$GLUEGEN_BUILDDIR/test/build/classes:$JOAL_BUILDDIR/classes:$JOGL_BUILDDIR/nativewindow/classes:$JOGL_BUILDDIR/jogl/classes:$JOGL_BUILDDIR/newt/classes:$JOGL_BUILDDIR/oculusvr/classes:$JOGL_BUILDDIR/test/build/classes:$JUNIT_JAR:$ANT_JARS
    fi

    if [ $swton -eq 1 ] ; then
        export USE_CLASSPATH=$USE_CLASSPATH:$JOGL_SWT_CLASSPATH
    fi
    if [ ! -z "$CUSTOM_CLASSPATH" ] ; then
        export USE_CLASSPATH=$CUSTOM_CLASSPATH:$USE_CLASSPATH
    fi
    #Test NEWT Broadcom ..
    #export USE_CLASSPATH=$JOGL_BUILDDIR/jar/atomic/newt.driver.broadcomegl.jar::$USE_CLASSPATH
    #X_ARGS="-Dnativewindow.ws.name=jogamp.newt.driver.broadcom.egl $X_ARGS"
    echo USE_BUILDDIR $USE_BUILDDIR
    echo USE_CLASSPATH $USE_CLASSPATH
    if [ $MOSX_MT -eq 1 ] ; then
        if [ $awton -eq 0 -a $swton -eq 0 ] ; then
            # No AWT, No SWT -> Preserve Main-Thread
            X_ARGS="-XstartOnFirstThread $X_ARGS"
            C_ARG="com.jogamp.newt.util.MainThread"
        elif [ $awton -eq 0 -a $swton -eq 1 ] ; then
            # SWT Only: Launch on Main-Thread
            X_ARGS="-XstartOnFirstThread $X_ARGS"
        # elif [ $awton -eq 1 -a $swton -eq 1 ] ; then
            # AWT and SWT -> Use AWT Main-Thread
        fi
    fi
    echo
    echo "Test Start: $*"
    echo
    # export __GL_THREADED_OPTIMIZATIONS=1
    echo __GL_THREADED_OPTIMIZATIONS $__GL_THREADED_OPTIMIZATIONS
    echo
    echo "$javaexe" $javaxargs $X_ARGS -cp $USE_CLASSPATH $D_ARGS $C_ARG $*
    #gdb --args "$javaexe" $javaxargs $X_ARGS -cp $USE_CLASSPATH $D_ARGS $C_ARG $*
    "$javaexe" $javaxargs $X_ARGS -cp $USE_CLASSPATH $D_ARGS $C_ARG $*
    #strace $javaexe $javaxargs $X_ARGS -cp $USE_CLASSPATH $D_ARGS $C_ARG $*
    echo
    echo "Test End: $*"
    echo
}

function testnoawtatomics() {
    jrun -1 0 $* 2>&1 | tee -a java-run.log
}

function testnoawt() {
    jrun  0 0 $* 2>&1 | tee -a java-run.log
}

function testawt() {
    MOSX_MT=0
    jrun  1 0 $* 2>&1 | tee -a java-run.log
}

function testswt() {
    jrun  0 1 $* 2>&1 | tee -a java-run.log
}

function testawtswt() {
    jrun  1 1 $* 2>&1 | tee -a java-run.log
}

#
# Version
#
#testawt jogamp.newt.awt.opengl.VersionApplet $*
#testawt com.jogamp.opengl.awt.GLCanvas $*
#testnoawt com.jogamp.nativewindow.NativeWindowVersion $*
#testnoawt com.jogamp.opengl.JoglVersion $*
#testnoawt com.jogamp.newt.NewtVersion $*
#testnoawt com.jogamp.oculusvr.OVRVersion $*

#testnoawt com.jogamp.newt.opengl.GLWindow $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLVersionParsing00NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestMainVersionGLWindowNEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestMainVersionGLCanvasAWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile00NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile02NEWTNoARBCtx $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile03NEWTOffscreen $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile04NEWTOffscreenNoARBCtx $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestVersionSemanticsNOUI $*

#
# Stereo
#
#testnoawt com.jogamp.opengl.test.junit.jogl.stereo.StereoDemo01 $*
#
#
# HiDPI
#
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2SimpleNEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.es2.awt.TestGearsES2GLJPanelAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.es2.awt.TestGearsES2AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NewtCanvasAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.awt.TestGearsAWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestRulerNEWT01 $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.GPUUISceneNewtDemo $*
#testawt com.jogamp.opengl.test.junit.graph.demos.GPUUISceneNewtCanvasAWTDemo $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.ManualHiDPIBufferedImage01AWT $*

#
# demos (any TK, more user driven tests)
#
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es1.newt.TestGearsES1NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es1.newt.TestOlympicES1NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es1.newt.TestRedSquareES1NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.es2.awt.TestGearsES2AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.es2.awt.TestGearsES2GLJPanelAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.ManualHiDPIBufferedImage01AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.es2.awt.TestGearsES2GLJPanelsAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NewtCanvasAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestLandscapeES2NewtCanvasAWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestLandscapeES2NEWT $*
#testawtswt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NewtCanvasSWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestElektronenMultipliziererNEWT $*
#testnoawtatomics com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestRedSquareES2NEWT $*
#testswt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NewtCanvasSWT $*
#testswt com.jogamp.opengl.test.junit.jogl.demos.es2.swt.TestGearsES2SWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.awt.TestGearsAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.awt.TestGearsAWTAnalyzeBug455 $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.awt.TestGearsGLJPanelAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.awt.TestGearsGLJPanelAWTBug450 $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.newt.TestGearsNewtAWTWrapper $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.gl2.newt.TestGearsNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.gl2.newt.TestTeapotNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.gl3.newt.TestGeomShader01TextureGL3NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.gl4.newt.TestTessellationShader01GL4NEWT $*

#
# av demos
#
#testnoawt jogamp.opengl.openal.av.ALDummyUsage $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieCube $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieSimple $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.av.CrossFadePlayer $*

#
# performance tests
#
#testnoawt com.jogamp.opengl.test.junit.jogl.perf.TestPerf001RawInit00NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.perf.TestPerf001GLJPanelInit01AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.perf.TestPerf001GLJPanelInit02AWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.perf.TestPerf001GLWindowInit03NEWT $*

#
# tile rendring / printing w/ & w/o AWT
#
#testnoawt com.jogamp.opengl.test.junit.jogl.tile.TestTiledRendering1GL2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.tile.TestTiledRendering2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.tile.TestRandomTiledRendering2GL2NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.tile.TestRandomTiledRendering3GL2AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.tile.TestTiledPrintingGearsAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.tile.TestTiledPrintingGearsSwingAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.tile.TestTiledPrintingGearsSwingAWT2 $*
#testawt com.jogamp.opengl.test.junit.jogl.tile.TestTiledPrintingGearsNewtAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.tile.TestTiledPrintingNIOImageSwingAWT $*

#
# CORE [NEWT + AWT] (testnoawt and testawt)
#
#testnoawt com.jogamp.opengl.test.junit.jogl.math.TestFloatUtil01NOUI $*
#testnoawt com.jogamp.opengl.test.junit.jogl.math.TestFloatUtil02MatrixMatrixMultNOUI $*
#testnoawt com.jogamp.opengl.test.junit.jogl.math.TestFloatUtil03InversionNOUI $*
#testnoawt com.jogamp.opengl.test.junit.jogl.math.TestPMVMatrix01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.math.TestPMVMatrix02NOUI $*
#testnoawt com.jogamp.opengl.test.junit.jogl.math.TestPMVMatrix03NOUI $*
#testnoawt com.jogamp.opengl.test.junit.jogl.math.TestGluUnprojectFloatNOUI $*
#testnoawt com.jogamp.opengl.test.junit.jogl.math.TestGluUnprojectDoubleNOUI $*
#testnoawt com.jogamp.opengl.test.junit.jogl.math.TestBinary16NOUI $*
#testnoawt com.jogamp.opengl.test.junit.jogl.math.TestQuaternion01NOUI $*

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestShutdownCompleteNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestInitConcurrent01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestInitConcurrent02NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLContextSurfaceLockNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLDebug00NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLDebug01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGPUMemSec01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLException01NEWT $*

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestMapBufferRead01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestMapBufferRead02NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestRedSquareES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT $*

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestBug669RecursiveGLContext01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestBug669RecursiveGLContext02NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestBug692GL3VAONEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestCPUSourcingAPINEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLExtensionQueryOffscreen $*

#testawt   com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextListAWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextListNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextListNEWT2 $*
#testawt   com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextNewtAWTBug523 $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES1NEWT $*
#testawt   com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2AWT3 $*
#testawt   com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2AWT3b $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2NEWT0 $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2NEWT1 $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2NEWT2 $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2NEWT3 $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2NEWT4 $*
#testswt   com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2SWT3 $*
#testawt   com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextWithJTabbedPaneAWT $*
#testawt   com.jogamp.opengl.test.junit.jogl.acore.TestSharedExternalContextAWT $*

#testawt com.jogamp.opengl.test.junit.jogl.acore.TestFBOAutoDrawableDeadlockAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestPBufferDeadlockAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestShutdownCompleteAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.x11.TestGLXCallsOnAWT $*

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestFBOOffThreadSharedContextMix2DemosES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestFBOOnThreadSharedContext1DemoES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestFBOAutoDrawableFactoryNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestFBOMix2DemosES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestFBOMRTNEWT01 $*

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLPointsNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLMesaBug651NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLMesaBug658NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestX11DefaultDisplay $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestOffscreenLayer01GLCanvasAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestOffscreenLayer02NewtCanvasAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestAddRemove01GLCanvasSwingAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestAddRemove02GLWindowNewtCanvasAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestAddRemove03GLWindowNEWT $*

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableDelegateNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableDelegateOnOffscrnCapsNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableFactoryGL2OffscrnCapsNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableFactoryGLnBitmapCapsNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableFactoryES2OffscrnCapsNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableFactoryGLProfileDeviceNEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableGLCanvasOnOffscrnCapsAWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableGLWindowOnOffscrnCapsNEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableNewtCanvasAWTOnOffscrnCapsAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestGLOffscreenAutoDrawableBug1044AWT $*

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.ect.TestExclusiveContext01VSyncAnimNEWT $*
#testawt   com.jogamp.opengl.test.junit.jogl.acore.ect.TestExclusiveContext01VSyncAnimAWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.ect.TestExclusiveContext02FPSAnimNEWT $*
#testawt   com.jogamp.opengl.test.junit.jogl.acore.ect.TestExclusiveContext02FPSAnimAWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.ect.TestExclusiveContext11VSyncAnimNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.ect.TestExclusiveContext12FPSAnimNEWT $*

#testawt com.jogamp.opengl.test.junit.jogl.acore.glels.TestGLContextDrawableSwitch02NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.glels.TestGLContextDrawableSwitch02AWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.glels.TestGLContextDrawableSwitch01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.glels.TestGLContextDrawableSwitch10NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.glels.TestGLContextDrawableSwitch11NewtAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.glels.TestGLContextDrawableSwitch12AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.glels.TestGLContextDrawableSwitch13Newt2AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.glels.TestGLContextDrawableSwitch21Newt2AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.glels.TestBug722GLContextDrawableSwitchNewt2AWT $*

#testnoawt com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen01GLPBufferNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen02BitmapNEWT $*

#testawt com.jogamp.opengl.test.junit.jogl.acore.TestFBOAutoDrawableDeadlockAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug461FBOSupersamplingSwingAWT
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug461PBufferSupersamplingSwingAWT

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestNEWTCloseX11DisplayBug565 $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestAWTCloseX11DisplayBug565 $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestBug1146GLContextDialogToolTipAWT $*

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.anim.TestAnimatorGLWindow01NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.anim.TestAnimatorGLJPanel01AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.anim.Bug898AnimatorFromEDTAWT $*

#testawt com.jogamp.opengl.test.junit.jogl.acore.TestGLReadBuffer01GLJPanelAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestGLReadBuffer01GLCanvasAWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLReadBuffer01GLWindowNEWT $*

#
# NEWT
#

#testnoawt com.jogamp.opengl.test.junit.newt.TestRemoteWindow01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestRemoteGLWindows01NEWT $*

#testnoawt com.jogamp.opengl.test.junit.newt.TestWindows01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestWindows02NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestWindowClosingProtocol02NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestWindowAndPointerIconNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestGLWindows00NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestGLWindows01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestGLWindows02NEWTAnimated $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestGLWindows03NEWTAnimResize $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestGLWindowInvisiblePointer01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestDisplayLifecycle01NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestDisplayLifecycle02NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestBug1211IRQ00NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode00aNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode00bNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode00cNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode01aNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode01bNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode01cNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode01dNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode02aNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode02bNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.ManualScreenMode03aNEWT $*
#testnoawt -Djava.awt.headless=true com.jogamp.opengl.test.junit.newt.TestGLWindows01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestGLWindowWarpPointer01NEWT $*

#
# AWT
#

#
# OSX CALayer Position and Visibility (OSX CALayer, ..)
# <BEGIN>
#

#
# Simple GLCanvas setVisible on/off
#   OK (X11, OSX)
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug664GLCanvasSetVisibleSwingAWT $*

#
# GLCanvas moving between CardLayout's JPanels
#   OK (X11, OSX)
#testawt com.jogamp.opengl.test.junit.jogl.acore.anim.TestAWTCardLayoutAnimatorStartStopBug532 $*

#
# GLCanvas moving between JTabbedPanel's tabs
#   OK (X11, OSX)
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug816JTabbedPanelVisibilityB849B878AWT $*

#
# GLCanvas/AWT Checkbox Visibility
#   OK (X11, OSX)
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug816OSXCALayerPos03aB729AWT $*

#
# GLCanvas/AWT Checkbox Visibility (on parent's Panel)
#   OK (X11, OSX)
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug816OSXCALayerPos03bB849AWT $*
#
# GLCanvas/Swing Checkbox Visibility (on parent's JPanel)
#   OK (X11, OSX)
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug816OSXCALayerPos03cB849AWT $*

#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug816OSXCALayerPos01AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug816OSXCALayerPos02AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug816GLCanvasFrameHoppingB849B889AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug816OSXCALayerPos04aAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug816OSXCALayerPos04bAWT $*

#
# OSX CALayer Position and Visibility (OSX CALayer, ..)
# <END>
#

#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug1245JTabbedPanelCrashAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug675BeansInDesignTimeAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug551AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug572AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug611AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT01GLn $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestSwingAWT01GLn
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT03GLCanvasRecreate01 $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT03GLJPanelRecreate01 $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT02WindowClosing
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestJScrollPaneMixHwLw01AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug642JSplitPaneMixHwLw01AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestIsRealizedConcurrency01AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.text.TestAWTTextRendererUseVertexArrayBug464
#testawt com.jogamp.opengl.test.junit.jogl.glu.TestBug463ScaleImageMemoryAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.glu.TestBug694ScaleImageUnpackBufferSizeAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.glu.TestBug365TextureGenerateMipMaps $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestGLCanvasAWTActionDeadlock00AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestGLCanvasAWTActionDeadlock01AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestGLCanvasAWTActionDeadlock02AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestGLJPanelTextureStateAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestGLJPanelResize01AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug1225EventQueueInterruptedAWT $*

#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.awt.Bug818GLJPanelAndGLCanvasApplet $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.awt.GLJPanelsAndGLCanvasDemoGL2Applet $*

#testawt com.jogamp.opengl.test.bugs.Bug735Inv0AppletAWT $*
#testawt com.jogamp.opengl.test.bugs.Bug735Inv1AppletAWT $*
#testawt com.jogamp.opengl.test.bugs.Bug735Inv2AppletAWT $*
#testawt com.jogamp.opengl.test.bugs.Bug735Inv3AppletAWT $*
#testawt com.jogamp.opengl.test.bugs.Bug735Inv4AWT $*

#
# SWT (testswt)
#
#testswt com.jogamp.opengl.test.junit.jogl.swt.TestSWTEclipseGLCanvas01GLn $*
#testswt com.jogamp.opengl.test.junit.jogl.swt.TestSWTJOGLGLCanvas01GLn $*
#testswt com.jogamp.opengl.test.junit.jogl.swt.TestNewtCanvasSWTGLn $*
#testswt com.jogamp.opengl.test.junit.jogl.swt.TestBug672NewtCanvasSWTSashForm $*
#testswt com.jogamp.opengl.test.junit.jogl.swt.TestBug672NewtCanvasSWTSashFormComposite $*

#
# awtswt (testawtswt) 
#   Per default (unit tests) all test are performed this way
#   with OSX: -XstartOnFirstThread
#
#testawtswt com.jogamp.opengl.test.junit.jogl.swt.TestSWTEclipseGLCanvas01GLn $*
#testawtswt com.jogamp.opengl.test.junit.jogl.swt.TestSWTAccessor03AWTGLn $*
#testawtswt com.jogamp.opengl.test.junit.jogl.swt.TestSWTJOGLGLCanvas01GLn $*
#testawtswt com.jogamp.opengl.test.junit.jogl.swt.TestNewtCanvasSWTGLn $*
#testawtswt com.jogamp.opengl.test.junit.jogl.swt.TestNewtCanvasSWTBug628ResizeDeadlockAWT $*
#testawtswt com.jogamp.opengl.test.junit.jogl.swt.TestSWTBug643AsyncExec $*

#
# newt.awt (testawt)
#
#testawt com.jogamp.opengl.test.junit.jogl.newt.TestSwingAWTRobotUsageBeforeJOGLInitBug411 $*
#testawt com.jogamp.opengl.test.junit.newt.TestEventSourceNotAWTBug
#testawt com.jogamp.opengl.test.junit.newt.event.TestNewtKeyEventOrderAWT $*
#testawt com.jogamp.opengl.test.junit.newt.event.TestNewtKeyEventAutoRepeatAWT $*
#testawt com.jogamp.opengl.test.junit.newt.event.TestNewtKeyPressReleaseUnmaskRepeatAWT $*
#testawt com.jogamp.opengl.test.junit.newt.event.TestNewtKeyCodesAWT $*
#testawt com.jogamp.opengl.test.junit.newt.event.TestNewtKeyCodeModifiersAWT $*
#testawt com.jogamp.opengl.test.junit.newt.event.TestNewtEventModifiersNEWTWindowAWT $*
#testawt com.jogamp.opengl.test.junit.newt.event.TestNewtEventModifiersAWTCanvas $*
#testawt com.jogamp.opengl.test.junit.newt.event.TestNewtEventModifiersNewtCanvasAWT $*
#testawtswt com.jogamp.opengl.test.junit.newt.event.TestNewtEventModifiersNewtCanvasSWTAWT $*
#testawt com.jogamp.opengl.test.junit.newt.event.TestParentingFocus01SwingAWTRobot $*
#testawt com.jogamp.opengl.test.junit.newt.event.TestParentingFocus02SwingAWTRobot $*
#testawt com.jogamp.opengl.test.junit.newt.event.TestParentingFocus03KeyTraversalAWT $*


#testawt com.jogamp.opengl.test.junit.newt.TestListenerCom01AWT
#testawt com.jogamp.opengl.test.junit.jogl.caps.TestMultisampleES1AWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.caps.TestMultisampleES1NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.caps.TestMultisampleES2NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.caps.TestTranslucencyAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.caps.TestTranslucencyNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.parenting.TestTranslucentChildWindowBug632NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.caps.TestBug605FlippedImageNEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.caps.TestBug605FlippedImageAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.glsl.TestShaderCompilationBug459AWT

#testnoawt com.jogamp.opengl.test.junit.newt.DemoCreateAndDisposeOnCloseNEWT $*
#testawt com.jogamp.opengl.test.junit.newt.DemoCreateAndDisposeOnCloseNEWT $*
#testawt com.jogamp.opengl.test.junit.newt.TestCloseNewtAWT
#testawt com.jogamp.opengl.test.junit.newt.TestWindowClosingProtocol01AWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestWindowClosingProtocol02NEWT $*
#testawt com.jogamp.opengl.test.junit.newt.TestWindowClosingProtocol03NewtAWT $*
#testawt com.jogamp.opengl.test.junit.newt.TestMultipleNewtCanvasAWT $*

#testawt $*

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestInitConcurrent01NEWT $*

#
# Bug 1249 - NEWT X11: 
#   - setVisible(false) IconicState not listening to _NET_WM_STATE_HIDDEN; 
#   - setVisible(true) not restoring from _NET_WM_STATE_HIDDEN
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT $*
testnoawt com.jogamp.opengl.test.junit.newt.TestGLWindows00NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestDisplayLifecycle01NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestDisplayLifecycle02NEWT

#
# NEWT Parenting (w/ NEWT, AWT or SWT)
#
#testnoawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting02NEWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01cSwingAWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01aAWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01bAWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01cAWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01dAWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting02AWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting03AWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting04AWT $*
#testawtswt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01aSWT $*
#testawtswt com.jogamp.opengl.test.junit.newt.parenting.TestParenting04SWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestTranslucentParentingAWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.parenting.TestTranslucentChildWindowBug632NEWT $*

#
# Misc Utils
#
#testnoawt com.jogamp.opengl.test.junit.jogl.util.TestImmModeSinkES1NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.TestImmModeSinkES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.TestES1FixedFunctionPipelineNEWT $*

#
# Texture / TextureUtils
#
#testawt com.jogamp.opengl.test.junit.jogl.util.texture.TestTexture01AWT $*
#testawt com.jogamp.opengl.test.junit.jogl.util.texture.TestTexture02AWT $*

#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestImageTypeNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestTextureIONEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestJPEGImage01NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.util.texture.TestJPEGJoglAWTCompareNewtAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.util.texture.TestJPEGJoglAWTBenchmarkNewtAWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestJPEGTextureFromFileNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestPixelFormat00NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestPixelFormatUtil00NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestPixelFormatUtil01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestPNGPixelRect00NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestPNGPixelRect01NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.util.texture.TestPNGTextureFromFileAWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestPNGTextureFromFileNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestTGATextureFromFileNEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.util.texture.TestGLReadBufferUtilTextureIOWrite01AWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestGLReadBufferUtilTextureIOWrite01NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.util.texture.TestGLReadBufferUtilTextureIOWrite02AWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestGLReadBufferUtilTextureIOWrite02NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestTextureSequence01NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.util.texture.TestTextureSequence01AWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.util.texture.TestBug817GLReadBufferUtilGLCTXDefFormatTypeES2NEWT $*


#
# GLSL
#
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestTransformFeedbackVaryingsBug407NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLSimple01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState02NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestRulerNEWT01 $*

#
# Graph
#
#testnoawt com.jogamp.opengl.test.junit.graph.TestRegionRendererNEWT01 $*
#testnoawt com.jogamp.opengl.test.junit.graph.TestTextRendererNEWT00 $*
#testnoawt com.jogamp.opengl.test.junit.graph.TestTextRendererNEWT01 $*
#testnoawt com.jogamp.opengl.test.junit.graph.TestTextRendererNEWT10 $*
#testnoawt com.jogamp.opengl.test.junit.graph.TestFontsNEWT00 $*
#testnoawt com.jogamp.opengl.test.junit.graph.TestTextRendererNEWTBugXXXX $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.ui.UINewtDemo01 $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.GPURegionNewtDemo $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.GPUUISceneNewtDemo $*
#testawt com.jogamp.opengl.test.junit.graph.demos.GPUUISceneNewtCanvasAWTDemo $*

#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieCube $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieSimple $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLReadBuffer01GLWindowNEWT $*

#
# OSX bugs
#

#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParentingFocus02SwingAWTRobot $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParentingFocus03KeyTraversalAWT $*

# test rotation change
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode02aNEWT

#
# regressions
#

#linux:

# ATI/Linux: XCB Unknown request in queue while dequeuing
#            Most likely this is a multi-threaded client and XInitThreads has not been called
#            ../../src/xcb_io.c:178: dequeue_pending_request: Assertion `!xcb_xlib_unknown_req_in_deq' failed
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.ect.TestExclusiveContext01VSyncAnimNEWT $*
# this one works - though !
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.ect.TestExclusiveContext02FPSAnimNEWT $* 

# ATI/Linux XCB Unknown request in queue while dequeuing
#           Most likely this is a multi-threaded client and XInitThreads has not been called
#           ../../src/xcb_io.c:178: dequeue_pending_request: Assertion `!xcb_xlib_unknown_req_in_deq' failed.
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode02aNEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.mm.TestScreenMode02bNEWT $*

# NEW

$spath/count-edt-start.sh java-run.log

