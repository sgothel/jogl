/**
 * Copyright 2012-2023 JogAmp Community. All rights reserved.
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

package jogamp.opengl.egl;

import java.nio.IntBuffer;
import java.util.Iterator;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ToolkitLock;
import com.jogamp.opengl.GLException;

import jogamp.opengl.Debug;
import jogamp.opengl.GLVersionNumber;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.LongObjectHashMap;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.egl.EGL;
import com.jogamp.opengl.egl.EGLExt;

/**
 * This implementation provides recursive calls to
 * {@link EGL#eglInitialize(long, IntBuffer, IntBuffer)} and {@link EGL#eglTerminate(long)},
 * where <code>eglInitialize(..)</code> is issued only for the 1st call per <code>eglDisplay</code>
 * and <code>eglTerminate(..)</code> is issued only for the last call.
 * <p>
 * This class is required, due to implementation bugs within EGL where {@link EGL#eglTerminate(long)}
 * does not mark the resource for deletion when still in use, bug releases them immediately.
 * </p>
 */
public class EGLDisplayUtil {
    private static final boolean DEBUG = Debug.debug("EGLDisplayUtil");
    private static boolean useSingletonEGLDisplay = false;
    private static EGLDisplayRef singletonEGLDisplay = null;

    private static class EGLDisplayRef {
        final long eglDisplay;
        final Throwable createdStack;
        int initRefCount;

        /**
         * Returns an already opened {@link EGLDisplayRef} or opens a new {@link EGLDisplayRef}.
         * <p>
         * Opened {@link EGLDisplayRef}s are mapped against their <code>eglDisplay</code> handle.
         * </p>
         * <p>
         * Method utilizes {@link EGLDisplayRef}'s reference counter, i.e. increases it.
         * </p>
         * <p>
         * An {@link EGLDisplayRef} is <i>opened</i> via {@link EGL#eglInitialize(long, IntBuffer, IntBuffer)}.
         * </p>
         */
        static EGLDisplayRef getOrCreateOpened(final long eglDisplay, final IntBuffer major, final IntBuffer minor) {
            final EGLDisplayRef o = (EGLDisplayRef) openEGLDisplays.get(eglDisplay);
            if( null == o ) {
                final boolean ok = EGL.eglInitialize(eglDisplay, major, minor);
                if( DEBUG ) {
                    System.err.println("EGLDisplayUtil.EGL.eglInitialize 0x"+Long.toHexString(eglDisplay)+" -> "+ok);
                }
                if( ok ) {
                    final EGLDisplayRef n = new EGLDisplayRef(eglDisplay);
                    openEGLDisplays.put(eglDisplay, n);
                    n.initRefCount++;
                    if( DEBUG ) {
                        System.err.println("EGLDisplayUtil.EGL.eglInitialize "+n);
                    }
                    if( null == singletonEGLDisplay ) {
                        singletonEGLDisplay = n;
                    }
                    return n;
                } else {
                    return null;
                }
            } else {
                o.initRefCount++;
                return o;
            }
        }

        /**
         * Closes an already opened {@link EGLDisplayRef}.
         * <p>
         * Method decreases a reference counter and closes the {@link EGLDisplayRef} if it reaches zero.
         * </p>
         * <p>
         * An {@link EGLDisplayRef} is <i>closed</i> via {@link EGL#eglTerminate(long)}.
         * </p>
         */
        static EGLDisplayRef closeOpened(final long eglDisplay, final boolean[] res) {
            final EGLDisplayRef o = (EGLDisplayRef) openEGLDisplays.get(eglDisplay);
            res[0] = true;
            if( null != o ) {
                if( 0 < o.initRefCount ) { // no negative refCount
                    o.initRefCount--;
                    if( 0 == o.initRefCount ) {
                        final boolean ok = EGL.eglTerminate(eglDisplay);
                        if( DEBUG ) {
                            System.err.println("EGLDisplayUtil.EGL.eglTerminate 0x"+Long.toHexString(eglDisplay)+" -> "+ok);
                            System.err.println("EGLDisplayUtil.EGL.eglTerminate "+o);
                        }
                        res[0] = ok;
                        if( o == singletonEGLDisplay ) {
                            singletonEGLDisplay = null;
                        }
                    }
                }
                if( 0 >= o.initRefCount ) {
                    openEGLDisplays.remove(eglDisplay);
                }
            }
            return o;
        }

        private EGLDisplayRef(final long eglDisplay) {
            this.eglDisplay = eglDisplay;
            this.initRefCount = 0;
            this.createdStack = DEBUG ? new Throwable() : null;
        }

        @Override
        public String toString() {
            return "EGLDisplayRef[0x"+Long.toHexString(eglDisplay)+": refCnt "+initRefCount+"]";
        }
    }
    private static final LongObjectHashMap openEGLDisplays;

    static {
        openEGLDisplays = new LongObjectHashMap();
        openEGLDisplays.setKeyNotFoundValue(null);
    }

    /**
     * @return number of unclosed EGL Displays.<br>
     */
    public static int shutdown(final boolean verbose) {
        if(DEBUG || verbose || openEGLDisplays.size() > 0 ) {
            System.err.println("EGLDisplayUtil.EGLDisplays: Shutdown (open: "+openEGLDisplays.size()+")");
            if(DEBUG) {
                ExceptionUtils.dumpStack(System.err);
            }
            if( openEGLDisplays.size() > 0) {
                dumpOpenDisplayConnections();
            }
        }
        return openEGLDisplays.size();
    }

    public static void dumpOpenDisplayConnections() {
        System.err.println("EGLDisplayUtil: Open EGL Display Connections: "+openEGLDisplays.size());
        int i=0;
        for(final Iterator<LongObjectHashMap.Entry> iter = openEGLDisplays.iterator(); iter.hasNext(); i++) {
            final LongObjectHashMap.Entry e = iter.next();
            final EGLDisplayRef v = (EGLDisplayRef) e.value;
            System.err.println("EGLDisplayUtil: Open["+i+"]: 0x"+Long.toHexString(e.key)+": "+v);
            if(null != v.createdStack) {
                v.createdStack.printStackTrace();
            }
        }
    }

    /* pp */ static synchronized void setSingletonEGLDisplayOnly(final boolean v) { useSingletonEGLDisplay = v; }

    /**
     * @param useCustom see {@link NativeWindowFactory#getNativeWindowType(boolean)}
     * @return the EGL platform type, e.g. {@link EGLExt#EGL_PLATFORM_X11_KHR} or {@link EGLExt#EGL_PLATFORM_GBM_KHR}
     * @see NativeWindowFactory#getNativeWindowType(boolean)
     * @see #getEGLPlatformType(String)
     */
    public static int getEGLPlatformType(final boolean useCustom) {
        return getEGLPlatformType( NativeWindowFactory.getNativeWindowType(useCustom) );
    }

    /**
     * @param nativeWindowType return value of {@link NativeWindowFactory#getNativeWindowType(boolean)}
     * @return the EGL platform type, e.g. {@link EGLExt#EGL_PLATFORM_X11_KHR} or {@link EGLExt#EGL_PLATFORM_GBM_KHR}
     * @see NativeWindowFactory#getNativeWindowType(boolean)
     * @see #getEGLPlatformType(boolean)
     */
    public static int getEGLPlatformType(final String nativeWindowType) {
        final int eglPlatform;
        switch( nativeWindowType ) {
            case NativeWindowFactory.TYPE_X11:
                eglPlatform = EGLExt.EGL_PLATFORM_X11_KHR;
                break;
            case NativeWindowFactory.TYPE_ANDROID:
                eglPlatform = EGLExt.EGL_PLATFORM_ANDROID_KHR;
                break;
            case NativeWindowFactory.TYPE_DRM_GBM:
                eglPlatform = EGLExt.EGL_PLATFORM_GBM_KHR; // same EGLExt.EGL_PLATFORM_GBM_MESA;
                break;
            case NativeWindowFactory.TYPE_WAYLAND:
                // TODO
                eglPlatform = EGLExt.EGL_PLATFORM_WAYLAND_KHR;
                break;
            default:
                eglPlatform = 0;
        }
        return eglPlatform;
    }

    private static boolean eglGetPlatformDisplayProbed = false;
    private static boolean eglGetPlatformDisplayAvail = false;
    private static VersionNumber eglGetPlatformDisplayMinVersion = new VersionNumber(1, 5, 0);

    private static synchronized long eglGetDisplay(final long nativeDisplay_id)  {
        if( useSingletonEGLDisplay && null != singletonEGLDisplay ) {
            if(DEBUG) {
                System.err.println("EGLDisplayUtil.eglGetDisplay.s: eglDisplay("+EGLContext.toHexString(nativeDisplay_id)+"): "+
                                   EGLContext.toHexString(singletonEGLDisplay.eglDisplay)+
                                   ", "+((EGL.EGL_NO_DISPLAY != singletonEGLDisplay.eglDisplay)?"OK":"Failed")+", singletonEGLDisplay "+singletonEGLDisplay+" (use "+useSingletonEGLDisplay+")");
            }
            return singletonEGLDisplay.eglDisplay;
        }

        if( !eglGetPlatformDisplayProbed ) {
            boolean viaVersion = false;
            boolean viaExtension = false;
            // A display of EGL_NO_DISPLAY is supported only if the EGL version is 1.5 or greater.
            final GLVersionNumber eglVersion = GLVersionNumber.create( EGL.eglQueryString(EGL.EGL_NO_DISPLAY, EGL.EGL_VERSION) );
            final int eglVersionErr = EGL.eglGetError();
            eglGetPlatformDisplayAvail = EGL.EGL_SUCCESS == eglVersionErr &&
                                         eglVersion.isValid() &&
                                         eglVersion.compareTo(eglGetPlatformDisplayMinVersion) >= 0;
            viaVersion = eglGetPlatformDisplayAvail;
            final int eglExtsErr;
            if( !eglGetPlatformDisplayAvail ) {
                final String eglExts = EGL.eglQueryString(EGL.EGL_NO_DISPLAY, EGL.EGL_EXTENSIONS);
                eglExtsErr = EGL.eglGetError();
                if( EGL.EGL_SUCCESS == eglExtsErr && null != eglExts && eglExts.length() > 0 ) {
                    if( eglExts.indexOf("EGL_EXT_platform_base") >= 0 ) {
                        eglGetPlatformDisplayAvail = true;
                        viaExtension = true;
                    }
                }
            } else {
                eglExtsErr = EGL.EGL_SUCCESS;
            }
            eglGetPlatformDisplayProbed = true;
            if(DEBUG) {
                System.err.println("EGLDisplayUtil.eglGetDisplay.p: eglGetPlatformDisplay available: "+eglGetPlatformDisplayAvail+
                                   ", eglClientVersion '"+eglVersion+"' via[Version "+viaVersion+", err 0x"+Integer.toHexString(eglVersionErr)+
                                                                              " / Extension "+viaExtension+", err 0x"+Integer.toHexString(eglExtsErr)+"]");
            }
        }
        final int eglPlatform = getEGLPlatformType(true);
        final long eglDisplay;
        if( eglGetPlatformDisplayAvail && 0 != eglPlatform ) {
            eglDisplay = EGL.eglGetPlatformDisplay(eglPlatform, nativeDisplay_id, null);
        } else {
            eglDisplay = EGL.eglGetDisplay(nativeDisplay_id);
        }

        if(DEBUG) {
            System.err.println("EGLDisplayUtil.eglGetDisplay.X: eglDisplay("+EGLContext.toHexString(nativeDisplay_id)+") @ "+
                               eglPlatform+"/"+NativeWindowFactory.getNativeWindowType(true)+": "+
                               EGLContext.toHexString(eglDisplay)+
                               ", "+((EGL.EGL_NO_DISPLAY != eglDisplay)?"OK":"Failed")+", singletonEGLDisplay "+singletonEGLDisplay+" (use "+useSingletonEGLDisplay+")");
        }
        return eglDisplay;
    }

    /**
     * @param eglDisplay
     * @param major
     * @param minor
     * @return true if the eglDisplay is valid and it's reference counter becomes one and {@link EGL#eglInitialize(long, IntBuffer, IntBuffer)} was successful, otherwise false
     *
     * @see EGL#eglInitialize(long, IntBuffer, IntBuffer)
     */
    private static synchronized boolean eglInitialize(final long eglDisplay, final int[] major, final int[] minor)  {
        if( EGL.EGL_NO_DISPLAY == eglDisplay) {
            return false;
        }
        final EGLDisplayRef d = EGLDisplayRef.getOrCreateOpened(eglDisplay, _eglMajorVersion, _eglMinorVersion);
        final int _major = _eglMajorVersion.get(0);
        final int _minor = _eglMinorVersion.get(0);
        if( null != major && null != minor ) {
            if( null != d ) {
                major[0] = _major;
                minor[0] = _minor;
            } else {
                major[0] = 0;
                minor[0] = 0;
            }
        }
        if(DEBUG) {
            System.err.println("EGLDisplayUtil.eglInitialize("+EGLContext.toHexString(eglDisplay)+" ...): "+d+" = "+(null != d)+", eglVersion "+_major+"."+_minor+", singletonEGLDisplay "+singletonEGLDisplay+" (use "+useSingletonEGLDisplay+")");
            // Thread.dumpStack();
        }
        return null != d;
    }
    private static final IntBuffer _eglMajorVersion = Buffers.newDirectIntBuffer(1);
    private static final IntBuffer _eglMinorVersion = Buffers.newDirectIntBuffer(1);

    /**
     * @param nativeDisplayID
     * @param eglDisplay array of size 1 holding return value if successful, otherwise {@link EGL#EGL_NO_DISPLAY}.
     * @param eglErr array of size 1 holding the EGL error value as retrieved by {@link EGL#eglGetError()} if not successful.
     * @param major
     * @param minor
     * @return {@link EGL#EGL_SUCCESS} if successful, otherwise {@link EGL#EGL_BAD_DISPLAY} if {@link #eglGetDisplay(long)} failed
     *         or {@link EGL#EGL_NOT_INITIALIZED} if {@link #eglInitialize(long, IntBuffer, IntBuffer)} failed.
     *
     * @see #eglGetDisplay(long)
     * @see #eglInitialize(long, IntBuffer, IntBuffer)
     */
    private static synchronized int eglGetDisplayAndInitialize(final long nativeDisplayID, final long[] eglDisplay, final int[] eglErr, final int[] major, final int[] minor) {
        eglDisplay[0] = EGL.EGL_NO_DISPLAY;
        final long _eglDisplay = eglGetDisplay( nativeDisplayID );
        if ( EGL.EGL_NO_DISPLAY == _eglDisplay ) {
            eglErr[0] = EGL.eglGetError();
            return EGL.EGL_BAD_DISPLAY;
        }
        if ( !eglInitialize( _eglDisplay, major, minor) ) {
            eglErr[0] = EGL.eglGetError();
            return EGL.EGL_NOT_INITIALIZED;
        }
        eglDisplay[0] = _eglDisplay;
        return EGL.EGL_SUCCESS;
    }

    /**
     * Attempts to {@link #eglGetDisplayAndInitialize(long, long[], int[], IntBuffer, IntBuffer)} with given <code>nativeDisplayID</code>.
     * If this fails, method retries with <code>nativeDisplayID</code> {@link EGL#EGL_DEFAULT_DISPLAY} - the fallback mechanism.
     * The actual used <code>nativeDisplayID</code> is returned in it's in/out array.
     *
     * @throws GLException if {@link EGL#eglGetDisplay(long)} or {@link EGL#eglInitialize(long, int[], int, int[], int)} fails incl fallback
     * @param nativeDisplayID in/out array of size 1, passing the requested nativeVisualID, may return a different revised nativeVisualID handle
     * @param major
     * @param minor
     * @return the initialized EGL display ID
     * @throws GLException if not successful
     */
    private static synchronized long eglGetDisplayAndInitialize(final long[] nativeDisplayID, final int[] major, final int[] minor) {
        final long[] eglDisplay = new long[1];
        final int[] eglError = new int[1];
        int eglRes = EGLDisplayUtil.eglGetDisplayAndInitialize(nativeDisplayID[0], eglDisplay, eglError, major, minor);
        if( EGL.EGL_SUCCESS == eglRes ) {
            return eglDisplay[0];
        }
        if( EGL.EGL_DEFAULT_DISPLAY != nativeDisplayID[0] ) { // fallback to DEGAULT_DISPLAY
            if(DEBUG) {
                System.err.println("EGLDisplayUtil.eglGetAndInitDisplay failed with native "+EGLContext.toHexString(nativeDisplayID[0])+", error "+EGLContext.toHexString(eglRes)+"/"+EGLContext.toHexString(eglError[0])+" - fallback!");
            }
            eglRes = EGLDisplayUtil.eglGetDisplayAndInitialize(EGL.EGL_DEFAULT_DISPLAY, eglDisplay, eglError, major, minor);
            if( EGL.EGL_SUCCESS == eglRes ) {
                nativeDisplayID[0] = EGL.EGL_DEFAULT_DISPLAY;
                return eglDisplay[0];
            }
        }
        throw new GLException("Failed to created/initialize EGL display incl. fallback default: native "+EGLContext.toHexString(nativeDisplayID[0])+", error "+EGLContext.toHexString(eglRes)+"/"+EGLContext.toHexString(eglError[0]));
    }

    /**
     * @param eglDisplay the EGL display handle
     * @return true if the eglDisplay is valid and it's reference counter becomes zero and {@link EGL#eglTerminate(long)} was successful, otherwise false
     */
    private static synchronized boolean eglTerminate(final long eglDisplay)  {
        if( EGL.EGL_NO_DISPLAY == eglDisplay) {
            return false;
        }
        final boolean[] res = new boolean[1];
        final EGLDisplayRef d = EGLDisplayRef.closeOpened(eglDisplay, res);
        if(DEBUG) {
            System.err.println("EGLDisplayUtil.eglTerminate.X("+EGLContext.toHexString(eglDisplay)+" ...): "+d+" = "+res[0]+", singletonEGLDisplay "+singletonEGLDisplay+" (use "+useSingletonEGLDisplay+")");
            // Thread.dumpStack();
        }
        return res[0];
    }

    private static final EGLGraphicsDevice.EGLDisplayLifecycleCallback eglLifecycleCallback = new EGLGraphicsDevice.EGLDisplayLifecycleCallback() {
        @Override
        public long eglGetAndInitDisplay(final long[] nativeDisplayID, final int[] major, final int[] minor) {
            return eglGetDisplayAndInitialize(nativeDisplayID, major, minor);
        }
        @Override
        public void eglTerminate(final long eglDisplayHandle) {
            EGLDisplayUtil.eglTerminate(eglDisplayHandle);
        }
    };

    /**
     * Returns an uninitialized {@link EGLGraphicsDevice}. User needs to issue {@link EGLGraphicsDevice#open()} before usage.
     * <p>
     * Using {@link #eglGetDisplayAndInitialize(long[])} for the {@link EGLGraphicsDevice#open()} implementation
     * and {@link #eglTerminate(long)} for {@link EGLGraphicsDevice#close()}.
     * </p>
     * <p>
     * Using the default {@link ToolkitLock}, via {@link NativeWindowFactory#getDefaultToolkitLock(String)}.
     * </p>
     * @param nativeDisplayID the existing native display ID
     * @param connection the existing underlying native connection name
     * @param unitID the unit ID
     * @return an uninitialized {@link EGLGraphicsDevice}
     */
    public static EGLGraphicsDevice eglCreateEGLGraphicsDevice(final long nativeDisplayID, final String connection, final int unitID)  {
        return new EGLGraphicsDevice(nativeDisplayID, connection, unitID, eglLifecycleCallback);
    }

    /**
     * Returns an uninitialized {@link EGLGraphicsDevice}. User needs to issue {@link EGLGraphicsDevice#open()} before usage.
     * <p>
     * Using {@link #eglGetDisplayAndInitialize(long[])} for the {@link EGLGraphicsDevice#open()} implementation
     * and {@link #eglTerminate(long)} for {@link EGLGraphicsDevice#close()}.
     * </p>
     * <p>
     * Using the default {@link ToolkitLock}, via {@link NativeWindowFactory#getDefaultToolkitLock(String)}.
     * </p>
     * @param aDevice valid {@link AbstractGraphicsDevice}'s native display ID, connection and unitID
     * @return an uninitialized {@link EGLGraphicsDevice}
     */
    public static EGLGraphicsDevice eglCreateEGLGraphicsDevice(final AbstractGraphicsDevice aDevice)  {
        return new EGLGraphicsDevice(aDevice, eglLifecycleCallback);
    }

    /**
     * Returns an uninitialized {@link EGLGraphicsDevice}. User needs to issue {@link EGLGraphicsDevice#open()} before usage.
     * <p>
     * Using {@link #eglGetDisplayAndInitialize(long[])} for the {@link EGLGraphicsDevice#open()} implementation
     * and {@link #eglTerminate(long)} for {@link EGLGraphicsDevice#close()}.
     * </p>
     * <p>
     * Using the default {@link ToolkitLock}, via {@link NativeWindowFactory#getDefaultToolkitLock(String)}.
     * </p>
     * @param surface
     * @return an uninitialized EGLGraphicsDevice
     */
    public static EGLGraphicsDevice eglCreateEGLGraphicsDevice(final NativeSurface surface)  {
        final long nativeDisplayID;
        if( NativeWindowFactory.TYPE_WINDOWS == NativeWindowFactory.getNativeWindowType(false) ) {
            nativeDisplayID = surface.getSurfaceHandle(); // don't even ask ..
        } else {
            nativeDisplayID = surface.getDisplayHandle(); // 0 == EGL.EGL_DEFAULT_DISPLAY
        }
        final AbstractGraphicsDevice adevice = surface.getGraphicsConfiguration().getScreen().getDevice();
        return new EGLGraphicsDevice(nativeDisplayID, adevice.getConnection(), adevice.getUnitID(), eglLifecycleCallback);
    }
}
