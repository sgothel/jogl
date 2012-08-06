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

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GLException;

import jogamp.opengl.Debug;

import com.jogamp.common.util.LongIntHashMap;
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
    protected static final boolean DEBUG = Debug.debug("EGL");
    
    static LongIntHashMap eglDisplayCounter;
    
    static {
        eglDisplayCounter = new LongIntHashMap();
        eglDisplayCounter.setKeyNotFoundValue(0);
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
    
    public static synchronized boolean eglInitialize(long eglDisplay, int[] major, int major_offset, int[] minor, int minor_offset)  {
        final boolean res;    
        final int refCnt = eglDisplayCounter.get(eglDisplay) + 1; // 0 + 1 = 1 -> 1st init
        if(1==refCnt) {
            res = EGL.eglInitialize(eglDisplay, major, major_offset, minor, minor_offset);
        } else {
            res = true;
        }
        eglDisplayCounter.put(eglDisplay, refCnt);
        if(DEBUG) {
            System.err.println("EGL.eglInitialize(0x"+Long.toHexString(eglDisplay)+" ...): #"+refCnt+" = "+res);
        }
        return res;
    }
    
    /**
     * 
     * @param eglDisplay
     * @param major
     * @param minor
     * @return true if the eglDisplay is valid and it's reference counter becomes one and {@link EGL#eglTerminate(long)} was successful, otherwise false
     * 
     * @see EGL#eglInitialize(long, int[], int, int[], int)}
     */
    public static synchronized boolean eglInitialize(long eglDisplay, IntBuffer major, IntBuffer minor)  {    
        if( EGL.EGL_NO_DISPLAY == eglDisplay) {
            return false;
        }
        final boolean res;    
        final int refCnt = eglDisplayCounter.get(eglDisplay) + 1; // 0 + 1 = 1 -> 1st init
        if(1==refCnt) { // only initialize once
            res = EGL.eglInitialize(eglDisplay, major, minor);
        } else {
            res = true;
        }
        eglDisplayCounter.put(eglDisplay, refCnt);
        if(DEBUG) {
            System.err.println("EGL.eglInitialize(0x"+Long.toHexString(eglDisplay)+" ...): #"+refCnt+" = "+res);
        }
        return res;
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
        final int refCnt = eglDisplayCounter.get(eglDisplay) - 1; // 1 - 1 = 0 -> final terminate
        if(0==refCnt) { // no terminate if still in use or already terminated
            res = EGL.eglTerminate(eglDisplay);
        } else {
            res = true;
        }
        if(0<=refCnt) { // no negative refCount
            eglDisplayCounter.put(eglDisplay, refCnt);
        }
        if(DEBUG) {
            System.err.println("EGL.eglTerminate(0x"+Long.toHexString(eglDisplay)+" ...): #"+refCnt+" = "+res);
        }
        return res;
    }
    
    public static final EGLGraphicsDevice.EGLDisplayLifecycleCallback eglLifecycleCallback = new EGLGraphicsDevice.EGLDisplayLifecycleCallback() {
        public long eglGetAndInitDisplay(long nativeDisplayID) {
            long eglDisplay = EGLDisplayUtil.eglGetDisplay(nativeDisplayID);
            if (eglDisplay == EGL.EGL_NO_DISPLAY) {
                throw new GLException("Failed to created EGL display: 0x"+Long.toHexString(nativeDisplayID)+", error 0x"+Integer.toHexString(EGL.eglGetError()));
            }
            if (!EGLDisplayUtil.eglInitialize(eglDisplay, null, null)) {
                throw new GLException("eglInitialize failed"+", error 0x"+Integer.toHexString(EGL.eglGetError()));
            }
            return eglDisplay;
        }
        public void eglTerminate(long eglDisplayHandle) {
            EGLDisplayUtil.eglTerminate(eglDisplayHandle);
        }
    };
    
    /**
     * @param nativeDisplayID
     * @param connection
     * @param unitID
     * @return an initialized EGLGraphicsDevice 
     * @throws GLException if {@link EGL#eglGetDisplay(long)} or {@link EGL#eglInitialize(long, int[], int, int[], int)} fails
     * @see EGLGraphicsDevice#EGLGraphicsDevice(long, long, String, int, com.jogamp.nativewindow.egl.EGLGraphicsDevice.EGLDisplayLifecycleCallback) 
     */
    public static EGLGraphicsDevice eglCreateEGLGraphicsDevice(long nativeDisplayID, String connection, int unitID)  {
        final EGLGraphicsDevice eglDisplay = new EGLGraphicsDevice(nativeDisplayID, 0, connection, unitID, eglLifecycleCallback);
        eglDisplay.open();
        return eglDisplay;
    }
    
    /**
     * @param surface
     * @param allowFallBackToDefault
     * @return an initialized EGLGraphicsDevice 
     * @throws GLException if {@link EGL#eglGetDisplay(long)} or {@link EGL#eglInitialize(long, int[], int, int[], int)} fails 
     */
    public static EGLGraphicsDevice eglCreateEGLGraphicsDevice(NativeSurface surface, boolean allowFallBackToDefault)  {
        long nativeDisplayID;
        if( NativeWindowFactory.TYPE_WINDOWS.equals(NativeWindowFactory.getNativeWindowType(false)) ) {
            nativeDisplayID = surface.getSurfaceHandle(); // don't even ask ..
        } else {
            nativeDisplayID = surface.getDisplayHandle(); // 0 == EGL.EGL_DEFAULT_DISPLAY
        }
        long eglDisplay = EGLDisplayUtil.eglGetDisplay(nativeDisplayID);
        if (eglDisplay == EGL.EGL_NO_DISPLAY && nativeDisplayID != EGL.EGL_DEFAULT_DISPLAY && allowFallBackToDefault) {
            if(DEBUG) {
                System.err.println("EGLDisplayUtil.eglGetDisplay(): Fall back to EGL_DEFAULT_DISPLAY");
            }
            nativeDisplayID = EGL.EGL_DEFAULT_DISPLAY;
            eglDisplay = EGLDisplayUtil.eglGetDisplay(nativeDisplayID);
        }
        if (eglDisplay == EGL.EGL_NO_DISPLAY) {
            throw new GLException("Failed to created EGL display: "+surface+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        }
        if (!EGLDisplayUtil.eglInitialize(eglDisplay, null, null)) {
            throw new GLException("eglInitialize failed"+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        }
        final AbstractGraphicsDevice adevice = surface.getGraphicsConfiguration().getScreen().getDevice();
        return new EGLGraphicsDevice(nativeDisplayID, eglDisplay, adevice.getConnection(), adevice.getUnitID(), eglLifecycleCallback);                                            
    }
}
