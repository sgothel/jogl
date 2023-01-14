/**
 * Copyright 2019 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.nativewindow.javafx;

import java.lang.reflect.Method;
import java.security.PrivilegedAction;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.nativewindow.macosx.MacOSXGraphicsDevice;
import com.jogamp.nativewindow.windows.WindowsGraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.RunnableTask;
import com.jogamp.common.util.SecurityUtil;
import com.sun.javafx.tk.TKStage;

import javafx.application.Platform;
import javafx.stage.Window;
import jogamp.nativewindow.Debug;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;

public class JFXAccessor {
    private static final boolean DEBUG;

    private static final boolean jfxAvailable;
    private static final Method fxUserThreadGetter;
    private static final Method tkStageGetter;
    private static final Method glassWindowGetter;
    private static final Method nativeWindowGetter;

    private static final String nwt;
    private static final boolean isOSX;
    private static final boolean isIOS;
    private static final boolean isWindows;
    private static final boolean isX11;

    static {
        final boolean[] _DEBUG = new boolean[] { true };

        final Method[] res = SecurityUtil.doPrivileged(new PrivilegedAction<Method[]>() {
            @Override
            public Method[] run() {
                NativeWindowFactory.initSingleton(); // last resort ..
                final Method[] res = new Method[] { null, null, null, null };
                try {
                    int i=0;
                    _DEBUG[0] = Debug.debug("JFX");
                    /**
                     * com.sun.javafx.tk.Toolkit
                     */
                    final Class<?> jfxToolkitClz = ReflectionUtil.getClass("com.sun.javafx.tk.Toolkit", false, JFXAccessor.class.getClassLoader());
                    res[i] = jfxToolkitClz.getDeclaredMethod("getFxUserThread");
                    res[i++].setAccessible(true);

                    /***
                     * class javafx.stage.Window
                     * class javafx.stage.Stage extends javafx.stage.Window
                     * class com.sun.javafx.tk.quantum.WindowStage extends com.sun.javafx.tk.quantum.GlassStage implements com.sun.javafx.tk.TKStage
                     * abstract com.sun.glass.ui.Window
                     *
                     * javafx.stage.Window: com.sun.javafx.tk.TKStage [impl_]getPeer()
                     * com.sun.javafx.tk.quantum.WindowStage: final com.sun.glass.ui.Window getPlatformWindow()
                     * com.sun.glass.ui.Window: public long getNativeWindow()
                     */
                    final Class<?> jfxStageWindowClz = ReflectionUtil.getClass("javafx.stage.Window", false, JFXAccessor.class.getClassLoader());
                    // final Class<?> jfxTkTKStageClz = ReflectionUtil.getClass("com.sun.javafx.tk.TKStage", false, JFXAccessor.class.getClassLoader());
                    final Class<?> jfxTkQuWindowStageClz = ReflectionUtil.getClass("com.sun.javafx.tk.quantum.WindowStage", false, JFXAccessor.class.getClassLoader());
                    final Class<?> jfxGlassUiWindowClz = ReflectionUtil.getClass("com.sun.glass.ui.Window", false, JFXAccessor.class.getClassLoader());

                    try {
                        // jfx 9, 11, 12, ..
                        res[i] = jfxStageWindowClz.getDeclaredMethod("getPeer");
                    } catch (final NoSuchMethodException ex) {
                        // jfx 8
                        res[i] = jfxStageWindowClz.getDeclaredMethod("impl_getPeer");
                    }
                    res[i++].setAccessible(true);

                    res[i] = jfxTkQuWindowStageClz.getDeclaredMethod("getPlatformWindow");
                    res[i++].setAccessible(true);
                    res[i] = jfxGlassUiWindowClz.getDeclaredMethod("getNativeWindow");
                    res[i++].setAccessible(true);
                } catch (final Throwable t) {
                    if(_DEBUG[0]) {
                        ExceptionUtils.dumpThrowable("jfx-init", t);
                    }
                }
                return res;
            }
        });
        {
            int i=0;
            fxUserThreadGetter = res[i++];
            tkStageGetter = res[i++];
            glassWindowGetter = res[i++];
            nativeWindowGetter = res[i++];
        }
        jfxAvailable = null != fxUserThreadGetter && null != tkStageGetter && null != glassWindowGetter && null != nativeWindowGetter;

        nwt = NativeWindowFactory.getNativeWindowType(false);
        isOSX = NativeWindowFactory.TYPE_MACOSX == nwt;
        isIOS = NativeWindowFactory.TYPE_IOS == nwt;
        isWindows = NativeWindowFactory.TYPE_WINDOWS == nwt;
        isX11 = NativeWindowFactory.TYPE_X11 == nwt;

        DEBUG = _DEBUG[0];
        if(DEBUG) {
            System.err.println(Thread.currentThread().getName()+" - Info: JFXAccessor.<init> available "+jfxAvailable+", nwt "+nwt+"( x11 "+isX11+", win "+isWindows+", osx "+isOSX+")");
        }
    }

    //
    // Common any toolkit
    //

    public static boolean isJFXAvailable() { return jfxAvailable; }

    /**
     * Runs given {@code task} on the JFX Thread if it has not stopped and if caller is not already on the JFX Thread,
     * otherwise execute given {@code task} on the current thread.
     * @param wait
     * @param task
     * @see #isJFXThreadOrHasJFXThreadStopped()
     */
    public static void runOnJFXThread(final boolean wait, final Runnable task) {
        final Object rTaskLock = new Object();
        synchronized(rTaskLock) { // lock the task execution
            if( isJFXThreadOrHasJFXThreadStopped() ) {
                task.run();
            } else if( !wait ) {
                Platform.runLater(task);
            } else {
                final RunnableTask rTask = new RunnableTask(task,
                                                rTaskLock,
                                                true /* always catch and report Exceptions, don't disturb EDT */,
                                                null);
                Platform.runLater(rTask);
                try {
                    while( rTask.isInQueue() ) {
                        rTaskLock.wait(); // free lock, allow execution of rTask
                    }
                } catch (final InterruptedException ie) {
                    throw new InterruptedRuntimeException(ie);
                }
                final Throwable throwable = rTask.getThrowable();
                if(null!=throwable) {
                    if(throwable instanceof NativeWindowException) {
                        throw (NativeWindowException)throwable;
                    }
                    throw new RuntimeException(throwable);
                }
            }
        }
    }

    public static Thread getJFXThread() throws NativeWindowException {
        try {
            return (Thread) fxUserThreadGetter.invoke(null);
        } catch (final Throwable e) {
            throw new NativeWindowException("Error getting JFX-Thread", e);
        }
    }
    public static String getJFXThreadName() {
        final Thread t = getJFXThread();
        return null != t ? t.getName() : null;
    }
    /**
     * @return true if the JFX Thread has stopped
     */
    public static boolean hasJFXThreadStopped() {
        final Thread t = getJFXThread();
        return null == t || !t.isAlive();
    }
    /**
     * @return true if caller is on the JFX Thread
     */
    public static boolean isJFXThread() {
        final Thread t = getJFXThread();
        return Thread.currentThread() == t;
    }
    /**
     * @return true if the JFX Thread has stopped or if caller is on the JFX Thread
     */
    public static boolean isJFXThreadOrHasJFXThreadStopped() {
        final Thread t = getJFXThread();
        return null == t || !t.isAlive() || Thread.currentThread() == t;
    }

    /**
     * @param stageWindow the JavaFX top heavyweight window handle
     * @return the AbstractGraphicsDevice w/ the native device handle
     * @throws NativeWindowException if an exception occurs retrieving the window handle or deriving the native device
     * @throws UnsupportedOperationException if the windowing system is not supported
     */
    public static AbstractGraphicsDevice getDevice(final Window stageWindow) throws NativeWindowException, UnsupportedOperationException {
        if( isX11 ) {
          // Decoupled X11 Device/Screen allowing X11 display lock-free off-thread rendering
          final String connection = null;
          final long x11DeviceHandle = X11Util.openDisplay(connection);
          if( 0 == x11DeviceHandle ) {
              throw new NativeWindowException("Error creating display: "+connection);
          }
          return new X11GraphicsDevice(x11DeviceHandle, AbstractGraphicsDevice.DEFAULT_UNIT, true /* owner */);
        }
        if( isWindows ) {
            return new WindowsGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
        }
        if( isOSX ) {
            return new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
        }
        throw new UnsupportedOperationException("n/a for this windowing system: "+nwt);
    }

    /**
     * @param device
     * @param screen -1 is default screen of the given device, e.g. maybe 0 or determined by native API. >= 0 is specific screen
     * @return
     */
    public static AbstractGraphicsScreen getScreen(final AbstractGraphicsDevice device, final int screen) {
        return NativeWindowFactory.createScreen(device, screen);
    }

    public static int getNativeVisualID(final AbstractGraphicsDevice device, final long windowHandle) {
        if( isX11 ) {
            return X11Lib.GetVisualIDFromWindow(device.getHandle(), windowHandle);
        }
        if( isWindows || isOSX ) {
            return VisualIDHolder.VID_UNDEFINED;
        }
        throw new UnsupportedOperationException("n/a for this windowing system: "+nwt);
    }

    /**
     * @param stageWindow the JavaFX top heavyweight window handle
     * @return the native window handle
     * @throws NativeWindowException if an exception occurs retrieving the window handle
     */
    public static long getWindowHandle(final Window stageWindow) throws NativeWindowException {
        final long h[] = { 0 };
        runOnJFXThread(true, new Runnable() {
            @Override
            public void run() {
                try {
                    final TKStage tkStage = (TKStage) tkStageGetter.invoke(stageWindow);
                    if( null != tkStage ) {
                        final Object platformWindow = glassWindowGetter.invoke(tkStage);
                        if( null != platformWindow ) {
                            final Object nativeHandle = nativeWindowGetter.invoke(platformWindow);
                            h[0] = ((Long) nativeHandle).longValue();
                        } else if(DEBUG) {
                            System.err.println(Thread.currentThread().getName()+" - Info: JFXAccessor null GlassWindow");
                        }
                    } else if(DEBUG) {
                        System.err.println(Thread.currentThread().getName()+" - Info: JFXAccessor null TKStage");
                    }
                } catch (final Throwable e) {
                    throw new NativeWindowException("Error getting Window handle", e);
                }
            } });
        return h[0];
    }
}
