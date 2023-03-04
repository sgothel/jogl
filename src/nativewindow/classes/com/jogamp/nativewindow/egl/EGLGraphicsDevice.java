/*
 * Copyright (c) 2010-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.DefaultGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;

/**
 * Encapsulates a graphics device on EGL platforms.
 */
public class EGLGraphicsDevice extends DefaultGraphicsDevice implements Cloneable {
    /** EGL default native display ID */
    public static final int EGL_DEFAULT_DISPLAY = 0;
    /** EGL no display handle */
    public static final int EGL_NO_DISPLAY = 0;

    private final long[] nativeDisplayID = new long[1];
    private /* final */ EGLDisplayLifecycleCallback eglLifecycleCallback;
    private VersionNumber eglVersion = VersionNumber.zeroVersion;

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
         * @param major out array for EGL major version
         * @param minor out array for EGL minor version
         * @return the initialized EGL display ID, or <code>0</code> if not successful
         */
        public long eglGetAndInitDisplay(final long[] nativeDisplayID, final int[] major, final int[] minor);

        /**
         * Implementation should issue an <code>EGL.eglTerminate(eglDisplayHandle)</code> call.
         * @param eglDisplayHandle
         */
        void eglTerminate(long eglDisplayHandle);
    }

    /**
     * Returns the native display ID of the given abstract device,
     * i.e. either {@link EGLGraphicsDevice#getNativeDisplayID()} or {@link AbstractGraphicsDevice#getHandle()}.
     */
    public static long getNativeDisplayID(final AbstractGraphicsDevice aDevice) {
        if( aDevice instanceof EGLGraphicsDevice ) {
            return ((EGLGraphicsDevice)aDevice).getNativeDisplayID();
        } else {
            return aDevice.getHandle();
        }
    }

    /**
     * Generic EGLGraphicsDevice constructor.
     * @param nativeDisplayID the existing native display ID
     * @param connection the existing underlying native connection name
     * @param unitID the unit ID
     * @param handle the handle, maybe 0
     * @param eglLifecycleCallback
     */
    private EGLGraphicsDevice(final long nativeDisplayID, final String connection, final int unitID, final long handle,
                              final EGLDisplayLifecycleCallback eglLifecycleCallback)
    {
        super(NativeWindowFactory.TYPE_EGL, connection, unitID, handle);
        this.nativeDisplayID[0] = nativeDisplayID;
        this.eglLifecycleCallback = eglLifecycleCallback;
    }

    /**
     * Constructs a dummy {@link EGLGraphicsDevice} with default connection settings.
     * <p>
     * The constructed instance will never create a valid EGL display handle.
     * </p>
     */
    public EGLGraphicsDevice() {
        this(EGL_DEFAULT_DISPLAY, DefaultGraphicsDevice.getDefaultDisplayConnection(), AbstractGraphicsDevice.DEFAULT_UNIT, EGL_NO_DISPLAY, null);
    }

    /**
     * Constructs a new dummy {@link EGLGraphicsDevice} reusing the given native display connection
     * with a {@link EGL#EGL_NO_DISPLAY} EGL display handle.
     * <p>
     * The constructed instance will never create a valid EGL display handle.
     * </p>
     * @param nativeDisplayID the existing native display ID
     * @param eglDisplay the existing EGL handle to this nativeDisplayID
     * @param connection the existing underlying native connection name
     * @param unitID the unit ID
     */
    public EGLGraphicsDevice(final long nativeDisplayID, final String connection, final int unitID) {
        this(nativeDisplayID, connection, unitID, EGL_NO_DISPLAY, null);
    }

    /**
     * Constructs a new {@link EGLGraphicsDevice} reusing the given native display connection and handle
     * <p>
     * The constructed instance will not take ownership of given EGL display handle.
     * </p>
     * @param nativeDisplayID the existing native display ID
     * @param eglDisplay the existing EGL handle to this nativeDisplayID
     * @param connection the existing underlying native connection name
     * @param unitID the unit ID
     */
    public EGLGraphicsDevice(final long nativeDisplayID, final long eglDisplay, final String connection, final int unitID) {
        this(nativeDisplayID, connection, unitID, eglDisplay, null);
        if (EGL_NO_DISPLAY == this.nativeDisplayID[0]) {
            throw new NativeWindowException("Invalid EGL_NO_DISPLAY display handle");
        }
    }

    /**
     * Constructs a new {@link EGLGraphicsDevice} using {@link AbstractGraphicsDevice}'s native display ID and connection,
     * using the {@link EGLDisplayLifecycleCallback} to handle this instance's native EGL lifecycle.
     * <p>
     * The constructed instance will take ownership of the created EGL display handle.
     * </p>
     * @param aDevice valid {@link AbstractGraphicsDevice}'s native display ID, connection and unitID
     * @param eglLifecycleCallback valid {@link EGLDisplayLifecycleCallback} to handle this instance's native EGL lifecycle.
     */
    public EGLGraphicsDevice(final AbstractGraphicsDevice aDevice, final EGLDisplayLifecycleCallback eglLifecycleCallback) {
        this(getNativeDisplayID(aDevice), aDevice.getConnection(), aDevice.getUnitID(), EGL_NO_DISPLAY, eglLifecycleCallback);
        if (null == eglLifecycleCallback) {
            throw new NativeWindowException("Null EGLDisplayLifecycleCallback");
        }
    }

    /**
     * Constructs a new {@link EGLGraphicsDevice} using {@link AbstractGraphicsDevice}'s native display ID and connection,
     * using the {@link EGLDisplayLifecycleCallback} to handle this instance's native EGL lifecycle.
     * <p>
     * The constructed instance will take ownership of the created EGL display handle.
     * </p>
     * @param nativeDisplayID the existing native display ID
     * @param connection the existing underlying native connection name
     * @param unitID the unit ID
     * @param eglLifecycleCallback valid {@link EGLDisplayLifecycleCallback} to handle this instance's native EGL lifecycle.
     */
    public EGLGraphicsDevice(final long nativeDisplayID, final String connection, final int unitID, final EGLDisplayLifecycleCallback eglLifecycleCallback) {
        this(nativeDisplayID, connection, unitID, EGL_NO_DISPLAY, eglLifecycleCallback);
        if (null == eglLifecycleCallback) {
            throw new NativeWindowException("Null EGLDisplayLifecycleCallback");
        }
    }

    /** EGL server version as returned by {@code eglInitialize(..)}. Only valid after {@link #open()}. */
    public VersionNumber getEGLVersion() { return eglVersion; }

    /**
     * Return the native display ID of this instance.
     * <p>
     * Since EGL 1.4, sharing resources requires e.g. a GLContext to use the same native display ID
     * for the so called shared context list and the to be created context.
     * </p>
     * @see #getNativeDisplayID(AbstractGraphicsDevice)
     */
    public long getNativeDisplayID() { return nativeDisplayID[0]; }

    /**
     * Returns true if given {@link AbstractGraphicsDevice} uses the same {@link #getNativeDisplayID()} of this instance.
     * <p>
     * In case given {@link AbstractGraphicsDevice} is not a {@link EGLGraphicsDevice}, its {@link AbstractGraphicsDevice#getHandle()}
     * is being used as the native display id for a matching {@link EGLGraphicsDevice} instance.
     * </p>
     * @see #getNativeDisplayID(AbstractGraphicsDevice)
     */
    public boolean sameNativeDisplayID(final AbstractGraphicsDevice aDevice) {
        return getNativeDisplayID(aDevice) == getNativeDisplayID();
    }

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
            final int[] major = { 0 };
            final int[] minor = { 0 };
            handle = eglLifecycleCallback.eglGetAndInitDisplay(nativeDisplayID, major, minor);
            if(0 == handle) {
                eglVersion = VersionNumber.zeroVersion;
                throw new NativeWindowException("EGLGraphicsDevice.open() failed: "+this);
            } else {
                eglVersion = new VersionNumber(major[0], minor[0], 0);
                return true;
            }
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
    protected Object setHandleOwnership(final Object newOwnership) {
        final EGLDisplayLifecycleCallback oldOwnership = eglLifecycleCallback;
        eglLifecycleCallback = (EGLDisplayLifecycleCallback) newOwnership;
        return oldOwnership;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[type "+getType()+", v"+eglVersion+", nativeDisplayID 0x"+Long.toHexString(nativeDisplayID[0])+", connection "+getConnection()+", unitID "+getUnitID()+", handle 0x"+Long.toHexString(getHandle())+", owner "+isHandleOwner()+", "+toolkitLock+"]";
    }
}

