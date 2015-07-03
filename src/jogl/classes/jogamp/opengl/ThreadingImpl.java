/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */

package jogamp.opengl;

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.Threading.Mode;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.ReflectionUtil;

/** Implementation of the {@link com.jogamp.opengl.Threading} class. */

public class ThreadingImpl {
    protected static final boolean DEBUG = Debug.debug("Threading");

    private static boolean singleThreaded;
    private static Mode mode;
    private static boolean hasAWT;
    // We need to know whether we're running on X11 platforms to change
    // our behavior when the Java2D/JOGL bridge is active
    private static boolean _isX11;

    private static final ToolkitThreadingPlugin threadingPlugin;

    static {
        threadingPlugin =
            AccessController.doPrivileged(new PrivilegedAction<ToolkitThreadingPlugin>() {
                    @Override
                    public ToolkitThreadingPlugin run() {
                        final String singleThreadProp;
                        {
                            final String w = PropertyAccess.getProperty("jogl.1thread", true);
                            singleThreadProp = null != w ? w.toLowerCase() : null;
                        }
                        final ClassLoader cl = ThreadingImpl.class.getClassLoader();
                        // Default to using the AWT thread on all platforms except
                        // Windows. On OS X there is instability apparently due to
                        // using the JAWT on non-AWT threads. On X11 platforms there
                        // are potential deadlocks which can be caused if the AWT
                        // EventQueue thread hands work off to the GLWorkerThread
                        // while holding the AWT lock. The optimization of
                        // makeCurrent / release calls isn't worth these stability
                        // problems.
                        hasAWT = GLProfile.isAWTAvailable();

                        _isX11 = NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false);

                        if (singleThreadProp != null) {
                            if (singleThreadProp.equals("true") ||
                                singleThreadProp.equals("auto")) {
                                mode  = ( hasAWT ? Mode.ST_AWT : Mode.MT );
                            } else if (singleThreadProp.equals("worker")) {
                                mode = Mode.ST_WORKER;
                            } else if (hasAWT && singleThreadProp.equals("awt")) {
                                mode = Mode.ST_AWT;
                            } else if (singleThreadProp.equals("false")) {
                                mode = Mode.MT;
                            } else {
                                throw new RuntimeException("Unsupported value for property jogl.1thread: "+singleThreadProp+", should be [true/auto, worker, awt or false]");
                            }
                        } else {
                            mode  = ( hasAWT ? Mode.ST_AWT : Mode.MT );
                        }
                        singleThreaded = Mode.MT != mode;

                        ToolkitThreadingPlugin threadingPlugin=null;
                        if(hasAWT) {
                            // try to fetch the AWTThreadingPlugin
                            Exception error=null;
                            try {
                                threadingPlugin = (ToolkitThreadingPlugin) ReflectionUtil.createInstance("jogamp.opengl.awt.AWTThreadingPlugin", cl);
                            } catch (final JogampRuntimeException jre) { error = jre; }
                            if( Mode.ST_AWT == mode && null==threadingPlugin ) {
                                throw new GLException("Mode is AWT, but class 'jogamp.opengl.awt.AWTThreadingPlugin' is not available", error);
                            }
                        }
                        if(DEBUG) {
                            System.err.println("Threading: jogl.1thread "+singleThreadProp+", singleThreaded "+singleThreaded+", hasAWT "+hasAWT+", mode "+mode+", plugin "+threadingPlugin);
                        }
                        return threadingPlugin;
                    }
                });
    }

    /** No reason to ever instantiate this class */
    private ThreadingImpl() {}

    public static boolean isX11() { return _isX11; }
    public static Mode getMode() { return mode; }

    /** If an implementation of the com.jogamp.opengl APIs offers a
        multithreading option but the default behavior is single-threading,
        this API provides a mechanism for end users to disable single-threading
        in this implementation.  Users are strongly discouraged from
        calling this method unless they are aware of all of the
        consequences and are prepared to enforce some amount of
        threading restrictions in their applications. Disabling
        single-threading, for example, may have unintended consequences
        on GLAutoDrawable implementations such as GLCanvas, GLJPanel and
        GLPbuffer. Currently there is no supported way to re-enable it
        once disabled, partly to discourage careless use of this
        method. This method should be called as early as possible in an
        application. */
    public static final void disableSingleThreading() {
        if( Mode.MT != mode ) {
            singleThreaded = false;
            if (Debug.verbose()) {
                System.err.println("Application forced disabling of single-threading of com.jogamp.opengl implementation");
            }
        }
    }

    /** Indicates whether OpenGL work is being automatically forced to a
        single thread in this implementation. */
    public static final boolean isSingleThreaded() {
        return singleThreaded;
    }

    /**
     * Indicates whether the current thread is capable of
     * performing OpenGL-related work.
     * <p>
     * Method always returns <code>true</code>
     * if {@link #getMode()} == {@link Mode#MT} or {@link #isSingleThreaded()} == <code>false</code>.
     * </p>
     */
    public static final boolean isOpenGLThread() throws GLException {
        if( Mode.MT == mode || !singleThreaded ) {
            return true;
        } else if( null != threadingPlugin ) {
            return threadingPlugin.isOpenGLThread();
        } else {
            switch (mode) {
                case ST_AWT:
                    throw new InternalError();
                case ST_WORKER:
                    return GLWorkerThread.isWorkerThread();
                default:
                    throw new InternalError("Illegal single-threading mode " + mode);
            }
        }
    }

    public static final boolean isToolkitThread() throws GLException {
        if(null!=threadingPlugin) {
            return threadingPlugin.isToolkitThread();
        }
        return false;
    }

    /** Executes the passed Runnable on the single thread used for all
        OpenGL work in this com.jogamp.opengl API implementation. It is
        not specified exactly which thread is used for this
        purpose. This method should only be called if the single-thread
        model is in use and if the current thread is not the OpenGL
        thread (i.e., if <code>isOpenGLThread()</code> returns
        false). It is up to the end user to check to see whether the
        current thread is the OpenGL thread and either execute the
        Runnable directly or perform the work inside it. */
    public static final void invokeOnOpenGLThread(final boolean wait, final Runnable r) throws GLException {
        if(null!=threadingPlugin) {
            threadingPlugin.invokeOnOpenGLThread(wait, r);
            return;
        }

        switch (mode) {
            case ST_WORKER:
                invokeOnWorkerThread(wait, r);
                break;

            case MT:
                r.run();
                break;

            default:
                throw new InternalError("Illegal single-threading mode " + mode);
        }
    }

    public static final void invokeOnWorkerThread(final boolean wait, final Runnable r) throws GLException {
        GLWorkerThread.start(); // singleton start via volatile-dbl-checked-locking
        try {
            GLWorkerThread.invoke(wait, r);
        } catch (final InvocationTargetException e) {
            throw new GLException(e.getTargetException());
        } catch (final InterruptedException e) {
            throw new GLException(e);
        }
    }
}
