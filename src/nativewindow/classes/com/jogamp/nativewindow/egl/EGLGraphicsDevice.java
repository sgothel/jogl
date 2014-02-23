/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.jogamp.nativewindow.egl;

import javax.media.nativewindow.*;

/** Encapsulates a graphics device on EGL platforms.
 */
public class EGLGraphicsDevice extends DefaultGraphicsDevice implements Cloneable {
    final long[] nativeDisplayID = new long[1];
    /* final */ EGLDisplayLifecycleCallback eglLifecycleCallback;

    /**
     * Hack to allow inject a EGL termination call.
     * <p>
     * FIXME: This shall be removed when relocated EGL to the nativewindow package,
     * since then it can be utilized directly.
     * </p>
     */
    public interface EGLDisplayLifecycleCallback {
        /**
         * Implementation should issue an <code>EGL.eglGetDisplay(nativeDisplayID)</code>
         * inclusive <code>EGL.eglInitialize(eglDisplayHandle, ..)</code> call.
         * @param nativeDisplayID in/out array of size 1, passing the requested nativeVisualID, may return a different revised nativeVisualID handle
         * @return the initialized EGL display ID, or <code>0</code> if not successful
         */
        public long eglGetAndInitDisplay(long[] nativeDisplayID);

        /**
         * Implementation should issue an <code>EGL.eglTerminate(eglDisplayHandle)</code> call.
         * @param eglDisplayHandle
         */
        void eglTerminate(long eglDisplayHandle);
    }

    /**
     * Note that this is not an open connection, ie no native display handle exist.
     * This constructor exist to setup a default device connection/unit.<br>
     */
    public EGLGraphicsDevice() {
        super(NativeWindowFactory.TYPE_EGL, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
        this.nativeDisplayID[0] = 0 ; // EGL.EGL_DEFAULT_DISPLAY
        this.eglLifecycleCallback = null;
    }

    public EGLGraphicsDevice(long nativeDisplayID, long eglDisplay, String connection, int unitID, EGLDisplayLifecycleCallback eglLifecycleCallback) {
        super(NativeWindowFactory.TYPE_EGL, connection, unitID, eglDisplay);
        this.nativeDisplayID[0] = nativeDisplayID;
        this.eglLifecycleCallback = eglLifecycleCallback;
    }

    public long getNativeDisplayID() { return nativeDisplayID[0]; }

    @Override
    public Object clone() {
      return super.clone();
    }

    /**
     * Opens the EGL device if handle is null and it's {@link EGLDisplayLifecycleCallback} is valid.
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public boolean open() {
        if(null != eglLifecycleCallback && 0 == handle) {
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName() + " - EGLGraphicsDevice.open(): "+this);
            }
            handle = eglLifecycleCallback.eglGetAndInitDisplay(nativeDisplayID);
            if(0 == handle) {
                throw new NativeWindowException("EGLGraphicsDevice.open() failed: "+this);
            }
            return true;
        }
        return false;
    }

    /**
     * Closes the EGL device if handle is not null and it's {@link EGLDisplayLifecycleCallback} is valid.
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public boolean close() {
        if(null != eglLifecycleCallback && 0 != handle) {
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName() + " - EGLGraphicsDevice.close(): "+this);
            }
            eglLifecycleCallback.eglTerminate(handle);
        }
        return super.close();
    }

    @Override
    public boolean isHandleOwner() {
        return null != eglLifecycleCallback;
    }
    @Override
    public void clearHandleOwner() {
        eglLifecycleCallback = null;
    }
    @Override
    protected Object getHandleOwnership() {
        return eglLifecycleCallback;
    }
    @Override
    protected Object setHandleOwnership(Object newOwnership) {
        final EGLDisplayLifecycleCallback oldOwnership = eglLifecycleCallback;
        eglLifecycleCallback = (EGLDisplayLifecycleCallback) newOwnership;
        return oldOwnership;
    }
}

