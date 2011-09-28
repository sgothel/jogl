/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.android;

import java.nio.IntBuffer;

import jogamp.newt.driver.android.event.AndroidNewtEventFactory;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.egl.EGLGraphicsDevice;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.Point;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.MouseEvent;

import jogamp.opengl.egl.EGL;
import jogamp.opengl.egl.EGLGraphicsConfiguration;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback2;
import android.view.SurfaceView;
import android.view.View;

public class AndroidWindow extends jogamp.newt.WindowImpl implements Callback2 {
    static {
        AndroidDisplay.initSingleton();
    }

    int getPixelFormat() {
        /**
        int bpp = capsRequested.getRedBits()+
                  capsRequested.getGreenBits()+
                  capsRequested.getBlueBits();
        int alpha = capsRequested.getAlphaBits();
        
        if(bpp <= 16) {
            
        }
        switch(aDisplay.getPixelFormat()) {
            case PixelFormat.RGBA_8888: bpp=32; break;
            case PixelFormat.RGBX_8888: bpp=32; break;
            case PixelFormat.RGB_888:   bpp=24; break;
            case PixelFormat.RGB_565:   bpp=16; break;
            case PixelFormat.RGBA_5551: bpp=16; break;
            case PixelFormat.RGBA_4444: bpp=16; break;
            case PixelFormat.RGB_332:   bpp= 8; break;
            default: bpp=32;   
        } */
        return PixelFormat.RGBA_8888;                       
    }
    
    class AndroidEvents implements /* View.OnKeyListener, */ View.OnTouchListener {

        public boolean onTouch(View v, MotionEvent event) {
            MouseEvent[] newtEvents = AndroidNewtEventFactory.createMouseEvents(AndroidWindow.this, event);
            if(null != newtEvents) {
                for(int i=0; i<newtEvents.length; i++) {
                    AndroidWindow.this.enqueueEvent(false, newtEvents[i]);
                }
            }
            return true;
        }

        /** TODO
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return false;
        } */
    }
    public AndroidWindow() {
        nsv = new MSurfaceView(jogamp.common.os.android.StaticContext.getContext());
        AndroidEvents ae = new AndroidEvents();
        nsv.setOnTouchListener(ae);
        nsv.setClickable(false);
        // nsv.setOnKeyListener(ae);
        SurfaceHolder sh = nsv.getHolder();
        sh.setFormat(getPixelFormat());
        sh.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
        sh.addCallback(this); 
    }

    public SurfaceView getView() { return nsv; }
    
    protected boolean canCreateNativeImpl() {
        final boolean b = 0 != surfaceHandle;
        Log.d(MD.TAG, "canCreateNativeImpl: "+b);
        return b;
    }

    protected void createNativeImpl() {
        Log.d(MD.TAG, "createNativeImpl 0 - surfaceHandle 0x"+Long.toHexString(surfaceHandle)+
                    ", "+x+"/"+y+" "+width+"x"+height);
        if(0!=getParentWindowHandle()) {
            throw new NativeWindowException("Window parenting not supported (yet)");
        }
        if(0==surfaceHandle) {
            throw new InternalError("XXX");
        }
       
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) getScreen().getDisplay().getGraphicsDevice();
        final EGLGraphicsConfiguration eglConfig = (EGLGraphicsConfiguration) GraphicsConfigurationFactory.getFactory(eglDevice)
                .chooseGraphicsConfiguration(capsRequested, capsRequested, capabilitiesChooser, getScreen().getGraphicsScreen());
        if (eglConfig == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        // query native VisualID and pass it to Surface
        final long eglConfigHandle = eglConfig.getNativeConfig();
        final IntBuffer nativeVisualID = Buffers.newDirectIntBuffer(1);
        if(!EGL.eglGetConfigAttrib(eglDevice.getHandle(), eglConfigHandle, EGL.EGL_NATIVE_VISUAL_ID, nativeVisualID)) {
            throw new NativeWindowException("eglgetConfigAttrib EGL_NATIVE_VISUAL_ID failed eglDisplay 0x"+Long.toHexString(eglDevice.getHandle())+ 
                                  ", error 0x"+Integer.toHexString(EGL.eglGetError()));
        }        
        Log.d(MD.TAG, "nativeVisualID 0x"+Integer.toHexString(nativeVisualID.get(0)));
        setSurfaceVisualID(surfaceHandle, nativeVisualID.get(0));
        
        eglSurface = EGL.eglCreateWindowSurface(eglDevice.getHandle(), eglConfig.getNativeConfig(), surfaceHandle, null);
        if (EGL.EGL_NO_SURFACE==eglSurface) {
            throw new NativeWindowException("Creation of window surface failed: "+eglConfig+", surfaceHandle 0x"+Long.toHexString(surfaceHandle)+", error "+toHexString(EGL.eglGetError()));
        }
        
        // propagate data ..
        config = eglConfig;
        setWindowHandle(surfaceHandle);
        Log.d(MD.TAG, "createNativeImpl X");
    }

    @Override
    protected void closeNativeImpl() {
        surface = null;
        surfaceHandle = 0;
        eglSurface = 0;        
    }

    @Override
    public final long getSurfaceHandle() {
        return eglSurface;
    }
    
    protected void requestFocusImpl(boolean reparented) { }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {
        if(0!=getWindowHandle()) {
            if( 0 != ( FLAG_CHANGE_FULLSCREEN & flags) ) {
                if( 0 != ( FLAG_IS_FULLSCREEN & flags) ) {
                    System.err.println("reconfigureWindowImpl.setFullscreen n/a");
                    return false;
                }
            }
        }
        if(width>0 || height>0) {
            if(0!=getWindowHandle()) {
                System.err.println("reconfigureWindowImpl.setSize n/a");
                return false;
            }
        }
        if(x>=0 || y>=0) {
            System.err.println("reconfigureWindowImpl.setPos n/a");
            return false;
        }
        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            visibleChanged(false, 0 != ( FLAG_IS_VISIBLE & flags));            
        }
        return true;
    }

    /***
    Canvas cLock = null;
    
    @Override
    protected int lockSurfaceImpl() {
        if (null != cLock) {
            throw new InternalError("surface already locked");
        }        
        if (0 != surfaceHandle) {
            cLock = nsv.getHolder().lockCanvas();
        }
        return ( null != cLock ) ? LOCK_SUCCESS : LOCK_SURFACE_NOT_READY;
    }

    @Override
    protected void unlockSurfaceImpl() {
        if (null == cLock) {
            throw new InternalError("surface not locked");
        }
        nsv.getHolder().unlockCanvasAndPost(cLock);
        cLock=null;
    } */
    
    protected Point getLocationOnScreenImpl(int x, int y) {
        return new Point(x,y);
    }

    protected void updateInsetsImpl(Insets insets) {
        // nop ..        
    }
    
    //----------------------------------------------------------------------
    // Surface Callbacks 
    //
    
    public void surfaceCreated(SurfaceHolder holder) {    
        surface = holder.getSurface();
        surfaceHandle = getSurfaceHandle(surface);

        Log.d(MD.TAG, "surfaceCreated - 0 - isValid: "+surface.isValid()+
                    ", surfaceHandle 0x"+Long.toHexString(surfaceHandle)+
                    ", "+nsv.getWidth()+"x"+nsv.getHeight());
        
        positionChanged(false, 0, 0);
        sizeChanged(false, nsv.getWidth(), nsv.getHeight(), false);                
        windowRepaint(0, 0, nsv.getWidth(), nsv.getHeight());

        if(isVisible()) {
           setVisible(true); 
        }
        
        Log.d(MD.TAG, "surfaceCreated - X");    
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.d(MD.TAG, "surfaceChanged: f "+Integer.toString(format)+", "+width+"x"+height);
        getScreen().getCurrentScreenMode(); // if ScreenMode changed .. trigger ScreenMode event
        sizeChanged(false, width, height, false);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(MD.TAG, "surfaceDestroyed");
        windowDestroyNotify();
    }

    public void surfaceRedrawNeeded(SurfaceHolder holder) {
        Log.d(MD.TAG, "surfaceRedrawNeeded");
        windowRepaint(0, 0, getWidth(), getHeight());
    }
    
    
    MSurfaceView nsv;
    Surface surface = null;
    volatile long surfaceHandle = 0;
    long eglSurface = 0;
    
    static class MSurfaceView extends SurfaceView {
        public MSurfaceView (Context ctx) {
            super(ctx);
            
        }
    }
    //----------------------------------------------------------------------
    // Internals only
    //
    protected static native boolean initIDs();
    protected static native long getSurfaceHandle(Surface surface);
    protected static native void setSurfaceVisualID(long surfaceHandle, int nativeVisualID);

}
