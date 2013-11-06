/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ToolkitLock;
import javax.media.opengl.GLException;

import jogamp.opengl.Debug;

import com.jogamp.common.util.LongObjectHashMap;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;

/**
 * This implementation provides recursive calls to
 * {@link EGL#eglInitialize(long, IntBuffer, IntBuffer)} and {@link EGL#eglTerminate(long)},
 * where <code>eglInitialize(..)</code> is issued only for the 1st call per <code>eglDisplay</code>
 * and <code>eglTerminate(..)</code> is issued only for the last call.
 * <p>
 * This class is required, due to implementation bugs within EGL where {@link EGL#eglTerminate(long)}
 * does not mark the resource for deletion when still in use, bug releases them immediatly.
 * </p>
 */
public class EGLDisplayUtil {
    protected static final boolean DEBUG = Debug.debug("EGLDisplayUtil");

    private static class DpyCounter {
        final long eglDisplay;
        final Throwable createdStack;
        int refCount;

        private DpyCounter(long eglDisplay) {
            this.eglDisplay = eglDisplay;
            this.refCount = 0;
            this.createdStack = DEBUG ? new Throwable() : null;
        }

        @Override
        public String toString() {
            return "EGLDisplay[0x"+Long.toHexString(eglDisplay)+": refCnt "+refCount+"]";
        }
    }
    static final LongObjectHashMap eglDisplayCounter;

    static {
        eglDisplayCounter = new LongObjectHashMap();
        eglDisplayCounter.setKeyNotFoundValue(null);
    }

    /**
     * @return number of unclosed EGL Displays.<br>
     */
    public static int shutdown(boolean verbose) {
        if(DEBUG || verbose || eglDisplayCounter.size() > 0 ) {
            System.err.println("EGLDisplayUtil.EGLDisplays: Shutdown (open: "+eglDisplayCounter.size()+")");
            if(DEBUG) {
                Thread.dumpStack();
            }
            if( eglDisplayCounter.size() > 0) {
                dumpOpenDisplayConnections();
            }
        }
        return eglDisplayCounter.size();
    }

    public static void dumpOpenDisplayConnections() {
        System.err.println("EGLDisplayUtil: Open EGL Display Connections: "+eglDisplayCounter.size());
        int i=0;
        for(Iterator<LongObjectHashMap.Entry> iter = eglDisplayCounter.iterator(); iter.hasNext(); i++) {
            final LongObjectHashMap.Entry e = iter.next();
            final DpyCounter v = (DpyCounter) e.value;
            System.err.println("EGLDisplayUtil: Open["+i+"]: 0x"+Long.toHexString(e.key)+": "+v);
            if(null != v.createdStack) {
                v.createdStack.printStackTrace();
            }
        }
    }

    public static long eglGetDisplay(long nativeDisplay_id)  {
        final long eglDisplay = EGL.eglGetDisplay(nativeDisplay_id);
        if(DEBUG) {
            System.err.println("EGLDisplayUtil.eglGetDisplay(): eglDisplay("+EGLContext.toHexString(nativeDisplay_id)+"): "+
                               EGLContext.toHexString(eglDisplay)+
                               ", "+((EGL.EGL_NO_DISPLAY != eglDisplay)?"OK":"Failed"));
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
    public static synchronized boolean eglInitialize(long eglDisplay, IntBuffer major, IntBuffer minor)  {
        if( EGL.EGL_NO_DISPLAY == eglDisplay) {
            return false;
        }
        final int refCnt;
        final DpyCounter d;
        {
            DpyCounter _d = (DpyCounter) eglDisplayCounter.get(eglDisplay);
            if(null == _d) {
                _d = new DpyCounter(eglDisplay);
                refCnt = 1; // 1st init
            } else {
                refCnt = _d.refCount + 1;
            }
            d = _d;
        }
        final boolean res;
        if(1==refCnt) { // only initialize once
            res = EGL.eglInitialize(eglDisplay, major, minor);
        } else {
            res = true;
        }
        if(res) { // update refCount and map if successfully initialized, only
            d.refCount = refCnt;
            if(1 == refCnt) {
                eglDisplayCounter.put(eglDisplay, d);
            }
        }
        if(DEBUG) {
            System.err.println("EGLDisplayUtil.eglInitialize("+EGLContext.toHexString(eglDisplay)+" ...): #"+refCnt+", "+d+" = "+res);
            // Thread.dumpStack();
        }
        return res;
    }

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
    public static synchronized int eglGetDisplayAndInitialize(long nativeDisplayID, long[] eglDisplay, int[] eglErr, IntBuffer major, IntBuffer minor) {
        eglDisplay[0] = EGL.EGL_NO_DISPLAY;
        final long _eglDisplay = EGLDisplayUtil.eglGetDisplay( nativeDisplayID );
        if ( EGL.EGL_NO_DISPLAY == _eglDisplay ) {
            eglErr[0] = EGL.eglGetError();
            return EGL.EGL_BAD_DISPLAY;
        }
        if ( !EGLDisplayUtil.eglInitialize( _eglDisplay, major, minor) ) {
            eglErr[0] = EGL.eglGetError();
            return EGL.EGL_NOT_INITIALIZED;
        }
        eglDisplay[0] = _eglDisplay;
        return EGL.EGL_SUCCESS;
    }

    /**
     * @param nativeDisplayID in/out array of size 1, passing the requested nativeVisualID, may return a different revised nativeVisualID handle
     * @return the initialized EGL display ID
     * @throws GLException if not successful
     */
    public static synchronized long eglGetDisplayAndInitialize(long[] nativeDisplayID) {
        final long[] eglDisplay = new long[1];
        final int[] eglError = new int[1];
        int eglRes = EGLDisplayUtil.eglGetDisplayAndInitialize(nativeDisplayID[0], eglDisplay, eglError, null, null);
        if( EGL.EGL_SUCCESS == eglRes ) {
            return eglDisplay[0];
        }
        if( EGL.EGL_DEFAULT_DISPLAY != nativeDisplayID[0] ) { // fallback to DEGAULT_DISPLAY
            if(DEBUG) {
                System.err.println("EGLDisplayUtil.eglGetAndInitDisplay failed with native "+EGLContext.toHexString(nativeDisplayID[0])+", error "+EGLContext.toHexString(eglRes)+"/"+EGLContext.toHexString(eglError[0])+" - fallback!");
            }
            eglRes = EGLDisplayUtil.eglGetDisplayAndInitialize(EGL.EGL_DEFAULT_DISPLAY, eglDisplay, eglError, null, null);
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
    public static synchronized boolean eglTerminate(long eglDisplay)  {
        if( EGL.EGL_NO_DISPLAY == eglDisplay) {
            return false;
        }
        final boolean res;
        final int refCnt;
        final DpyCounter d;
        {
            DpyCounter _d = (DpyCounter) eglDisplayCounter.get(eglDisplay);
            if(null == _d) {
                _d = null;
                refCnt = -1; // n/a
            } else {
                refCnt = _d.refCount - 1; // 1 - 1 = 0 -> final terminate
            }
            d = _d;
        }
        if( 0 == refCnt ) { // no terminate if still in use or already terminated
            res = EGL.eglTerminate(eglDisplay);
            eglDisplayCounter.remove(eglDisplay);
        } else {
            if(0 < refCnt) { // no negative refCount
                d.refCount = refCnt;
            }
            res = true;
        }
        if(DEBUG) {
            System.err.println("EGLDisplayUtil.eglTerminate("+EGLContext.toHexString(eglDisplay)+" ...): #"+refCnt+" = "+res);
            // Thread.dumpStack();
        }
        return res;
    }

    public static final EGLGraphicsDevice.EGLDisplayLifecycleCallback eglLifecycleCallback = new EGLGraphicsDevice.EGLDisplayLifecycleCallback() {
        @Override
        public long eglGetAndInitDisplay(long[] nativeDisplayID) {
            return eglGetDisplayAndInitialize(nativeDisplayID);
        }
        @Override
        public void eglTerminate(long eglDisplayHandle) {
            EGLDisplayUtil.eglTerminate(eglDisplayHandle);
        }
    };

    /**
     * Using the default {@link ToolkitLock}, via {@link NativeWindowFactory#getDefaultToolkitLock(String, long)}.
     * @param nativeDisplayID
     * @param connection
     * @param unitID
     * @return an initialized EGLGraphicsDevice
     * @throws GLException if {@link EGL#eglGetDisplay(long)} or {@link EGL#eglInitialize(long, int[], int, int[], int)} fails
     * @see EGLGraphicsDevice#EGLGraphicsDevice(long, long, String, int, com.jogamp.nativewindow.egl.EGLGraphicsDevice.EGLDisplayLifecycleCallback)
     */
    public static EGLGraphicsDevice eglCreateEGLGraphicsDevice(long nativeDisplayID, String connection, int unitID)  {
        final EGLGraphicsDevice eglDisplay = new EGLGraphicsDevice(nativeDisplayID, EGL.EGL_NO_DISPLAY, connection, unitID, eglLifecycleCallback);
        eglDisplay.open();
        return eglDisplay;
    }

    /**
     * @param surface
     * @return an initialized EGLGraphicsDevice
     * @throws GLException if {@link EGL#eglGetDisplay(long)} or {@link EGL#eglInitialize(long, int[], int, int[], int)} fails incl fallback
     */
    public static EGLGraphicsDevice eglCreateEGLGraphicsDevice(NativeSurface surface)  {
        final long nativeDisplayID;
        if( NativeWindowFactory.TYPE_WINDOWS == NativeWindowFactory.getNativeWindowType(false) ) {
            nativeDisplayID = surface.getSurfaceHandle(); // don't even ask ..
        } else {
            nativeDisplayID = surface.getDisplayHandle(); // 0 == EGL.EGL_DEFAULT_DISPLAY
        }
        final AbstractGraphicsDevice adevice = surface.getGraphicsConfiguration().getScreen().getDevice();
        final EGLGraphicsDevice eglDevice = new EGLGraphicsDevice(nativeDisplayID, EGL.EGL_NO_DISPLAY, adevice.getConnection(), adevice.getUnitID(), eglLifecycleCallback);
        eglDevice.open();
        return eglDevice;
    }
}
