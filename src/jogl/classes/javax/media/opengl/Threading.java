/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2012 JogAmp Community. All rights reserved.
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
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.opengl;

import jogamp.opengl.ThreadingImpl;

/** This API provides access to the threading model for the implementation of
    the classes in this package.

    <P>

    OpenGL is specified as a thread-safe API, but in practice there
    are multithreading-related issues on most, if not all, of the
    platforms which support it. For example, some OpenGL
    implementations do not behave well when one context is made
    current first on one thread, released, and then made current on a
    second thread, although this is legal according to the OpenGL
    specification. On other platforms there are other problems.

    <P>

    Due to these limitations, and due to the inherent multithreading
    in the Java platform (in particular, in the Abstract Window
    Toolkit), it is often necessary to limit the multithreading
    occurring in the typical application using the OpenGL API.

    <P>

    In the current reference implementation, for instance, multithreading
    has been limited by
    forcing all OpenGL-related work for GLAutoDrawables on to a single
    thread. In other words, if an application uses only the
    GLAutoDrawable and GLEventListener callback mechanism, it is
    guaranteed to have the most correct single-threaded behavior on
    all platforms.

    <P>

    Applications using the GLContext makeCurrent/release API directly
    will inherently break this single-threaded model, as these methods
    require that the OpenGL context be made current on the current
    thread immediately. For applications wishing to integrate better
    with an implementation that uses the single-threaded model, this
    class provides public access to the mechanism used by the implementation.

    <P>

    Users can execute Runnables on the
    internal thread used for performing OpenGL work, and query whether
    the current thread is already this thread. Using these mechanisms
    the user can move work from the current thread on to the internal
    OpenGL thread if desired.

    <P>

    This class also provides mechanisms for querying whether this
    internal serialization of OpenGL work is in effect, and a
    programmatic way of disabling it.  In the current reference
    implementation it is enabled by default, although it could be
    disabled in the future if OpenGL drivers become more robust on
    all platforms.

    <P>

    In addition to specifying programmatically whether the single
    thread for OpenGL work is enabled, users may switch it on and off
    using the system property <code>jogl.1thread</code>. Valid values
    for this system property are:

    <PRE>
    -Djogl.1thread=false     Disable single-threading of OpenGL work, hence use multithreading.
    -Djogl.1thread=true      Enable single-threading of OpenGL work (default -- on a newly-created worker thread)
    -Djogl.1thread=auto      Select default single-threading behavior (currently on)
    -Djogl.1thread=awt       Enable single-threading of OpenGL work on AWT event dispatch thread (current default on all
                             platforms, and also the default behavior older releases)
    -Djogl.1thread=worker    Enable single-threading of OpenGL work on newly-created worker thread (not suitable for Mac
                             OS X or X11 platforms, and risky on Windows in applet environments)
    </PRE>
*/

public class Threading {
    public static enum Mode {
        /**
         * Full multithreaded OpenGL,
         * i.e. any {@link Threading#invoke(boolean, Runnable, Object) invoke}
         * {@link Threading#invokeOnOpenGLThread(boolean, Runnable) commands}
         * will be issued on the current thread immediately.
         */
        MT(0),

        /** Single-Threaded OpenGL on AWT EDT */
        ST_AWT(1),

        /** Single-Threaded OpenGL on dedicated worker thread. */
        ST_WORKER(2);

        public final int id;

        Mode(final int id){
            this.id = id;
        }
    }

    /** No reason to ever instantiate this class */
    private Threading() {}

    /** Returns the threading mode */
    public static Mode getMode() {
        return ThreadingImpl.getMode();
    }

    /** If an implementation of the com.jogamp.opengl APIs offers a
        multithreading option but the default behavior is single-threading,
        this API provides a mechanism for end users to disable single-threading
        in this implementation.  Users are strongly discouraged from
        calling this method unless they are aware of all of the
        consequences and are prepared to enforce some amount of
        threading restrictions in their applications. Disabling
        single-threading, for example, may have unintended consequences
        on GLAutoDrawable implementations such as GLCanvas and GLJPanel.
        Currently there is no supported way to re-enable it
        once disabled, partly to discourage careless use of this
        method. This method should be called as early as possible in an
        application. */
    public static final void disableSingleThreading() {
        ThreadingImpl.disableSingleThreading();
    }

    /** Indicates whether OpenGL work is being automatically forced to a
        single thread in this implementation. */
    public static final boolean isSingleThreaded() {
        return ThreadingImpl.isSingleThreaded();
    }

    /** Indicates whether the current thread is the designated toolkit thread,
        if such semantics exists. */
    public static final boolean isToolkitThread() throws GLException {
        return ThreadingImpl.isToolkitThread();
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
        return ThreadingImpl.isOpenGLThread();
    }

    /** Executes the passed Runnable on the single thread used for all
        OpenGL work in this com.jogamp.opengl API implementation. It is
        not specified exactly which thread is used for this
        purpose. This method should only be called if the single-thread
        model is in use and if the current thread is not the OpenGL
        thread (i.e., if <code>isOpenGLThread()</code> returns
        false). It is up to the end user to check to see whether the
        current thread is the OpenGL thread and either execute the
        Runnable directly or perform the work inside it.
     **/
    public static final void invokeOnOpenGLThread(final boolean wait, final Runnable r) throws GLException {
        ThreadingImpl.invokeOnOpenGLThread(wait, r);
    }

    /**
     * If not {@link #isOpenGLThread()}
     * <b>and</b> the <code>lock</code> is not being hold by this thread,
     * invoke Runnable <code>r</code> on the OpenGL thread via {@link #invokeOnOpenGLThread(boolean, Runnable)}.
     * <p>
     * Otherwise invoke Runnable <code>r</code> on the current thread.
     * </p>
     *
     * @param wait set to true for waiting until Runnable <code>r</code> is finished, otherwise false.
     * @param r the Runnable to be executed
     * @param lock optional lock object to be tested
     * @throws GLException
     */
    public static final void invoke(final boolean wait, final Runnable r, final Object lock) throws GLException {
        if ( !isOpenGLThread() &&
             ( null == lock || !Thread.holdsLock(lock) ) ) {
            invokeOnOpenGLThread(wait, r);
        } else {
            r.run();
        }
    }
}
