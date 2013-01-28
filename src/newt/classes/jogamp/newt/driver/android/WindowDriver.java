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

import jogamp.common.os.android.StaticContext;
import jogamp.newt.WindowImpl;
import jogamp.newt.driver.android.event.AndroidNewtEventTranslator;

import javax.media.nativewindow.Capabilities;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.Point;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.newt.Screen;
import com.jogamp.newt.ScreenMode;

import jogamp.opengl.egl.EGL;
import jogamp.opengl.egl.EGLGraphicsConfiguration;
import jogamp.opengl.egl.EGLGraphicsConfigurationFactory;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback2;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.view.SurfaceView;
import android.view.KeyEvent;

public class WindowDriver extends jogamp.newt.WindowImpl implements Callback2 {    
    static {
        DisplayDriver.initSingleton();
    }

    /**
     * First stage of selecting an Android PixelFormat, 
     * at construction via {@link SurfaceHolder#setFormat(int)} 
     * before native realization!
     * 
     * @param rCaps requested Capabilities
     * @return An Android PixelFormat number suitable for {@link SurfaceHolder#setFormat(int)}.
     */
    public static final int getSurfaceHolderFormat(CapabilitiesImmutable rCaps) {
        int fmt = PixelFormat.UNKNOWN;
        
        if( !rCaps.isBackgroundOpaque() ) {
            fmt = PixelFormat.TRANSLUCENT;
        } else if( rCaps.getRedBits()<=5 &&
                   rCaps.getGreenBits()<=6 &&
                   rCaps.getBlueBits()<=5 &&
                   rCaps.getAlphaBits()==0 ) {
            fmt = PixelFormat.RGB_565;            
        } else if( rCaps.getAlphaBits()==0 ) {
            fmt = PixelFormat.RGB_888;
        } else {        
            fmt = PixelFormat.RGBA_8888;
        }
        Log.d(MD.TAG, "getSurfaceHolderFormat: requested: "+rCaps);
        Log.d(MD.TAG, "getSurfaceHolderFormat:  returned: "+fmt);
        
        return fmt;
    }
    
    
    public static final int NATIVE_WINDOW_FORMAT_RGBA_8888          = 1;
    public static final int NATIVE_WINDOW_FORMAT_RGBX_8888          = 2;
    public static final int NATIVE_WINDOW_FORMAT_RGB_565            = 4;

    /**
     * Second stage of selecting an Android PixelFormat, 
     * at right after native (surface) realization at {@link Callback2#surfaceChanged(SurfaceHolder, int, int, int)}.
     * Selection happens via {@link #setSurfaceVisualID0(long, int)} before native EGL creation.
     * 
     * @param androidPixelFormat An Android PixelFormat delivered via {@link Callback2#surfaceChanged(SurfaceHolder, int, int, int)} params.
     * @return A native Android PixelFormat number suitable for {@link #setSurfaceVisualID0(long, int)}.
     */
    public static final int getANativeWindowFormat(int androidPixelFormat) {
        final int nativePixelFormat;
        switch(androidPixelFormat) {
            case PixelFormat.RGBA_8888:
            case PixelFormat.RGBA_5551:
            case PixelFormat.RGBA_4444:
                nativePixelFormat = NATIVE_WINDOW_FORMAT_RGBA_8888; 
                break;
            
            case PixelFormat.RGBX_8888:
            case PixelFormat.RGB_888:   
                nativePixelFormat = NATIVE_WINDOW_FORMAT_RGBX_8888; 
                break;
                
            case PixelFormat.RGB_565:   
            case PixelFormat.RGB_332:   
                nativePixelFormat = NATIVE_WINDOW_FORMAT_RGB_565; 
                break;
            default: nativePixelFormat = NATIVE_WINDOW_FORMAT_RGBA_8888;
        }
        Log.d(MD.TAG, "getANativeWindowFormat: android: "+androidPixelFormat+" -> native "+nativePixelFormat);
        return nativePixelFormat;
    }
    
    /**
     * Final stage of Android PixelFormat operation, 
     * match the requested Capabilities w/ Android PixelFormat number. 
     * This is done at native realization @ {@link Callback2#surfaceChanged(SurfaceHolder, int, int, int)}.
     * 
     * @param matchFormatPrecise
     * @param format
     * @param rCaps requested Capabilities
     * @return The fixed Capabilities
     */    
    public static final CapabilitiesImmutable fixCaps(boolean matchFormatPrecise, int format, CapabilitiesImmutable rCaps) {
        PixelFormat pf = new PixelFormat(); 
        PixelFormat.getPixelFormatInfo(format, pf);
        final CapabilitiesImmutable res;        
        int r, g, b, a;
        
        switch(format) {
            case PixelFormat.RGBA_8888: r=8; g=8; b=8; a=8; break; // NATIVE_WINDOW_FORMAT_RGBA_8888
            case PixelFormat.RGBX_8888: r=8; g=8; b=8; a=0; break; // NATIVE_WINDOW_FORMAT_RGBX_8888
            case PixelFormat.RGB_888:   r=8; g=8; b=8; a=0; break;
            case PixelFormat.RGB_565:   r=5; g=6; b=5; a=0; break; // NATIVE_WINDOW_FORMAT_RGB_565
            case PixelFormat.RGBA_5551: r=5; g=5; b=5; a=1; break;
            case PixelFormat.RGBA_4444: r=4; g=4; b=4; a=4; break;
            case PixelFormat.RGB_332:   r=3; g=3; b=2; a=0; break;
            default: throw new InternalError("Unhandled pixelformat: "+format);
        }
        final boolean change = matchFormatPrecise ||
                               rCaps.getRedBits()   > r &&
                               rCaps.getGreenBits() > g &&
                               rCaps.getBlueBits()  > b &&
                               rCaps.getAlphaBits() > a ;
        
        if(change) {
            Capabilities nCaps = (Capabilities) rCaps.cloneMutable();
            nCaps.setRedBits(r);
            nCaps.setGreenBits(g);
            nCaps.setBlueBits(b);
            nCaps.setAlphaBits(a);
            res = nCaps;        
        } else {
            res = rCaps;
        }
        Log.d(MD.TAG, "fixCaps:    format: "+format);
        Log.d(MD.TAG, "fixCaps: requested: "+rCaps);
        Log.d(MD.TAG, "fixCaps:    chosen: "+res);
        
        return res;
    }
    
    public static final boolean isAndroidFormatTransparent(int aFormat) {
        switch (aFormat) {
            case PixelFormat.TRANSLUCENT:
            case PixelFormat.TRANSPARENT:
                return true;
        }
        return false;
    }
    
    public static Class<?>[] getCustomConstructorArgumentTypes() {
        return new Class<?>[] { Context.class } ;
    }
    
    public WindowDriver() {
        reset();
    }

    private final void reset() {
        added2StaticViewGroup = false;
        androidView = null;
        nativeFormat = VisualIDHolder.VID_UNDEFINED;
        androidFormat = VisualIDHolder.VID_UNDEFINED;
        capsByFormat = null;
        surface = null;
        surfaceHandle = 0;
        eglSurface = 0;
        definePosition(0, 0); // default to 0/0
        defineSize(0, 0); // default size -> TBD !
        
        setBrokenFocusChange(true);
    }
    
    private final void setupInputListener(final boolean enable) {
        Log.d(MD.TAG, "setupInputListener(enable "+enable+") - "+Thread.currentThread().getName());
        
        final AndroidNewtEventTranslator eventTranslator = 
                enable ? new AndroidNewtEventTranslator(this, androidView.getContext(), androidView.getHandler()) : null; 
        androidView.setOnTouchListener(eventTranslator);
        androidView.setOnKeyListener(eventTranslator);
        androidView.setOnFocusChangeListener(eventTranslator);
        if(AndroidVersion.SDK_INT >= 12) { // API Level 12
            Log.d(MD.TAG, "setupInputListener - enable GenericMotionListener - "+Thread.currentThread().getName());
            androidView.setOnGenericMotionListener(eventTranslator);
        }
        if( enable ) {
            androidView.post(new Runnable() {
                public void run() {
                    androidView.setClickable(false);
                    androidView.setFocusable(true);
                    androidView.setFocusableInTouchMode(true);
                } } );
        }
    }
    
    private final void setupAndroidView(Context ctx) {
        androidView = new MSurfaceView(ctx);
                
        final SurfaceHolder sh = androidView.getHolder();
        sh.addCallback(WindowDriver.this); 
        sh.setFormat(getSurfaceHolderFormat(getRequestedCapabilities()));        
    }
    
    public final SurfaceView getAndroidView() { return androidView; }
    
    @Override
    protected final void instantiationFinished() {
        Log.d(MD.TAG, "instantiationFinished() - "+Thread.currentThread().getName());
                
        final Context ctx = StaticContext.getContext();        
        if(null == ctx) {
            throw new NativeWindowException("No static [Application] Context has been set. Call StaticContext.setContext(Context) first.");
        }
        
        if( null != Looper.myLooper() ) {
            setupAndroidView(ctx);
        }
    }
    
    @Override
    protected final boolean canCreateNativeImpl() {
        Log.d(MD.TAG, "canCreateNativeImpl.0: surfaceHandle ready "+(0!=surfaceHandle)+" - on thread "+Thread.currentThread().getName());
        if(WindowImpl.DEBUG_IMPLEMENTATION) {
            Thread.dumpStack();
        }
        
        if( isFullscreen() ) {
            final Screen screen = getScreen();
            final ScreenMode sm = screen.getCurrentScreenMode();            
            definePosition(screen.getX(), screen.getY());
            defineSize(sm.getRotatedWidth(), sm.getRotatedHeight());
        }
        
        final boolean b;
        
        if( 0 == surfaceHandle ) {
            // Static ViewGroup, i.e. self contained main code
            final ViewGroup viewGroup = StaticContext.getContentViewGroup();
            Log.d(MD.TAG, "canCreateNativeImpl: viewGroup "+viewGroup);
            if( null != viewGroup && !added2StaticViewGroup ) {
                added2StaticViewGroup = true;
                viewGroup.post(new Runnable() {
                    public void run() {
                        if(null == androidView) {
                            setupAndroidView( StaticContext.getContext() );
                        }
                        viewGroup.addView(androidView, new android.widget.FrameLayout.LayoutParams(getWidth(), getHeight(), Gravity.BOTTOM|Gravity.RIGHT));
                        Log.d(MD.TAG, "canCreateNativeImpl: added to static ViewGroup - on thread "+Thread.currentThread().getName());                        
                    } });
                for(long sleep = TIMEOUT_NATIVEWINDOW; 0<sleep && 0 == surfaceHandle; sleep-=10 ) {
                    try { Thread.sleep(10); } catch (InterruptedException ie) {}
                }
                b = 0 != surfaceHandle;
                Log.d(MD.TAG, "canCreateNativeImpl: surfaceHandle ready(2) "+b+" - on thread "+Thread.currentThread().getName());                
            } else {
                // No surfaceHandle defined, No static ViewGroup to add ourselves
                b = false;
            }
        } else {
            // surfaceHandle already defined
            b = true; 
        }
        return b;
    }

    @Override
    protected final void createNativeImpl() {
        Log.d(MD.TAG, "createNativeImpl 0 - surfaceHandle 0x"+Long.toHexString(surfaceHandle)+
                    ", format [a "+androidFormat+", n "+nativeFormat+"], "+getX()+"/"+getY()+" "+getWidth()+"x"+getHeight()+" - on thread "+Thread.currentThread().getName());

        if(0!=getParentWindowHandle()) {
            throw new NativeWindowException("Window parenting not supported (yet)");
        }
        if(0==surfaceHandle) {
            throw new InternalError("surfaceHandle null");
        }
       
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) getScreen().getDisplay().getGraphicsDevice();
        final EGLGraphicsConfiguration eglConfig = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(
                capsByFormat, (GLCapabilitiesImmutable) getRequestedCapabilities(), 
                (GLCapabilitiesChooser)capabilitiesChooser, getScreen().getGraphicsScreen(), nativeFormat, 
                isAndroidFormatTransparent(androidFormat));
        if (eglConfig == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        final int nativeVisualID = eglConfig.getVisualID(VisualIDHolder.VIDType.NATIVE);
        Log.d(MD.TAG, "nativeVisualID 0x"+Integer.toHexString(nativeVisualID));
        Log.d(MD.TAG, "requestedCaps: "+eglConfig.getRequestedCapabilities());
        Log.d(MD.TAG, "chosenCaps   : "+eglConfig.getChosenCapabilities());
        if(VisualIDHolder.VID_UNDEFINED != nativeVisualID) {
            setSurfaceVisualID0(surfaceHandle, nativeVisualID);
        }
        
        eglSurface = EGL.eglCreateWindowSurface(eglDevice.getHandle(), eglConfig.getNativeConfig(), surfaceHandle, null);
        if (EGL.EGL_NO_SURFACE==eglSurface) {
            throw new NativeWindowException("Creation of window surface failed: "+eglConfig+", surfaceHandle 0x"+Long.toHexString(surfaceHandle)+", error "+toHexString(EGL.eglGetError()));
        }
                
        // propagate data ..
        setGraphicsConfiguration(eglConfig);
        setWindowHandle(surfaceHandle);
        visibleChanged(false, true);
        focusChanged(false, true);
        
        setupInputListener(true);
        
        Log.d(MD.TAG, "createNativeImpl X: eglSurfaceHandle 0x"+Long.toHexString(eglSurface));
    }

    @Override
    protected final void closeNativeImpl() {
        Log.d(MD.TAG, "closeNativeImpl 0 - surfaceHandle 0x"+Long.toHexString(surfaceHandle)+
                    ", eglSurfaceHandle 0x"+Long.toHexString(eglSurface)+
                    ", format [a "+androidFormat+", n "+nativeFormat+"], "+getX()+"/"+getY()+" "+getWidth()+"x"+getHeight()+" - on thread "+Thread.currentThread().getName());
        if(WindowImpl.DEBUG_IMPLEMENTATION) {
            Thread.dumpStack();
        }
        
        setupInputListener(false);
        
        if(0 != eglSurface) {
            final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) getScreen().getDisplay().getGraphicsDevice();
            if (!EGL.eglDestroySurface(eglDevice.getHandle(), eglSurface)) {
                throw new GLException("Error destroying window surface (eglDestroySurface)");
            }
            eglSurface = 0;        
        }        
        release0(surfaceHandle);

        if( null != androidView ) {
            if( added2StaticViewGroup ) {
                added2StaticViewGroup = false;
                final ViewGroup viewGroup = StaticContext.getContentViewGroup();
                if( null != viewGroup) {
                    viewGroup.post(new Runnable() {
                        public void run() {
                            viewGroup.removeView(androidView);
                            Log.d(MD.TAG, "closeNativeImpl: removed from static ViewGroup - on thread "+Thread.currentThread().getName());
                        } });
                }
            }
        }
        
        surface = null;
        surfaceHandle = 0;
    }

    @Override
    public final long getSurfaceHandle() {
        return eglSurface;
    }
    
    /**
     * <p>
     * Accessible protected method!
     * </p>
     * 
     * {@inheritDoc}
     */
    @Override
    public final void focusChanged(boolean defer, boolean focusGained) {
        super.focusChanged(defer, focusGained);
    }
    
    @Override
    protected final void requestFocusImpl(boolean reparented) { 
        if(null != androidView) {
            Log.d(MD.TAG, "requestFocusImpl: reparented "+reparented);
            androidView.post(new Runnable() {
                public void run() {
                    androidView.requestFocus();
                    androidView.bringToFront();
                }
            });
        }
    }

    @Override
    protected final boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {        
        boolean res = true;
        
        if( 0 != ( FLAG_CHANGE_FULLSCREEN & flags) ) {
            Log.d(MD.TAG, "reconfigureWindowImpl.setFullscreen post creation (setContentView()) n/a");
            return false;
        }
        if(getWidth() != width || getHeight() != height) {
            if(0!=getWindowHandle()) {
                Log.d(MD.TAG, "reconfigureWindowImpl.setSize n/a");
                res = false;
            } else {
                defineSize(width, height);
            }
        }
        if(getX() != x || getY() != y) {
            if(0!=getWindowHandle()) {
                Log.d(MD.TAG, "reconfigureWindowImpl.setPos n/a");
                res = false;
            } else {
                definePosition(x, y);
            }
        }
        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            visibleChanged(false, 0 != ( FLAG_IS_VISIBLE & flags));            
        }
        return res;
    }

    @Override
    protected final Point getLocationOnScreenImpl(int x, int y) {
        return new Point(x,y);
    }

    @Override
    protected final void updateInsetsImpl(Insets insets) {
        // nop ..        
    }
    
    //----------------------------------------------------------------------
    // Virtual On-Screen Keyboard / SoftInput 
    //
    
    private class KeyboardVisibleReceiver extends ResultReceiver {
        public KeyboardVisibleReceiver() {
            super(null);
        }
        
        @Override 
        public void onReceiveResult(int r, Bundle data) {
            boolean v = false;
        
            switch(r) {
                case InputMethodManager.RESULT_UNCHANGED_SHOWN:
                case InputMethodManager.RESULT_SHOWN:
                    v = true;
                    break;
                case InputMethodManager.RESULT_HIDDEN:
                case InputMethodManager.RESULT_UNCHANGED_HIDDEN:
                    v = false;
                    break;            
            }
            Log.d(MD.TAG, "keyboardVisible: "+v);
            keyboardVisibilityChanged(v);
        }
    }
    private KeyboardVisibleReceiver keyboardVisibleReceiver = new KeyboardVisibleReceiver();
    
    @Override
    protected final boolean setKeyboardVisibleImpl(boolean visible) {
        if(null != androidView) {
            final InputMethodManager imm = (InputMethodManager) getAndroidView().getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            final IBinder winid = getAndroidView().getWindowToken();
            if(visible) {
                // Show soft-keyboard:
                imm.showSoftInput(androidView, 0, keyboardVisibleReceiver);
            } else {
                // hide keyboard :
                imm.hideSoftInputFromWindow(winid, 0, keyboardVisibleReceiver);
            }
            return visible;
        } else {
            return false; // nop
        }
    }
    
    //----------------------------------------------------------------------
    // Surface Callbacks 
    //
    
    @Override
    public final void surfaceCreated(SurfaceHolder holder) {    
        Log.d(MD.TAG, "surfaceCreated: "+getX()+"/"+getY()+" "+getWidth()+"x"+getHeight()+" - on thread "+Thread.currentThread().getName());
    }

    @Override
    public final void surfaceChanged(SurfaceHolder aHolder, int aFormat, int aWidth, int aHeight) {
        Log.d(MD.TAG, "surfaceChanged: f "+nativeFormat+" -> "+aFormat+", "+aWidth+"x"+aHeight+", current surfaceHandle: 0x"+Long.toHexString(surfaceHandle)+" - on thread "+Thread.currentThread().getName());
        if(WindowImpl.DEBUG_IMPLEMENTATION) {
            Thread.dumpStack();
        }
        if(0!=surfaceHandle && androidFormat != aFormat ) {
            // re-create
            Log.d(MD.TAG, "surfaceChanged (destroy old)");
            if(!windowDestroyNotify(true)) {
                destroy();
            }
            surfaceHandle = 0;
            surface=null;
        }
        if(getScreen().isNativeValid()) {
            getScreen().getCurrentScreenMode(); // if ScreenMode changed .. trigger ScreenMode event
        }

        if(0>getX() || 0>getY()) {
            positionChanged(false, 0, 0);
        }
        
        if(0 == surfaceHandle) {
            androidFormat = aFormat;
            surface = aHolder.getSurface();
            surfaceHandle = getSurfaceHandle0(surface);
            acquire0(surfaceHandle);
            final int aNativeWindowFormat = getANativeWindowFormat(androidFormat);
            setSurfaceVisualID0(surfaceHandle, aNativeWindowFormat);            
            nativeFormat = getSurfaceVisualID0(surfaceHandle);
            Log.d(MD.TAG, "surfaceChanged: androidFormat "+androidFormat+" -- (set-native "+aNativeWindowFormat+") --> nativeFormat "+nativeFormat);

            final int nWidth = getWidth0(surfaceHandle);
            final int nHeight = getHeight0(surfaceHandle);
            capsByFormat = (GLCapabilitiesImmutable) fixCaps(true /* matchFormatPrecise */, nativeFormat, getRequestedCapabilities());
            sizeChanged(false, nWidth, nHeight, false);
    
            Log.d(MD.TAG, "surfaceRealized: isValid: "+surface.isValid()+
                          ", new surfaceHandle 0x"+Long.toHexString(surfaceHandle)+
                          ", format [a "+androidFormat+"/n "+nativeFormat+"], "+
                          getX()+"/"+getY()+" "+nWidth+"x"+nHeight+", visible: "+isVisible());
    
            if(isVisible()) {
               setVisible(false, true);
            }
        }
        sizeChanged(false, aWidth, aHeight, false);
        windowRepaint(0, 0, aWidth, aHeight);
        Log.d(MD.TAG, "surfaceChanged: X");
    }
    
    @Override
    public final void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(MD.TAG, "surfaceDestroyed - on thread "+Thread.currentThread().getName());
        if(WindowImpl.DEBUG_IMPLEMENTATION) {
            Thread.dumpStack();
        }
        windowDestroyNotify(true); // actually too late .. however ..
    }

    @Override
    public final void surfaceRedrawNeeded(SurfaceHolder holder) {
        Log.d(MD.TAG, "surfaceRedrawNeeded  - on thread "+Thread.currentThread().getName());
        windowRepaint(0, 0, getWidth(), getHeight());
    }
        
    private boolean added2StaticViewGroup;
    private MSurfaceView androidView;
    private int nativeFormat; // chosen current native PixelFormat (suitable for EGL)
    private int androidFormat; // chosen current android PixelFormat (-1, -2 ..)
    private GLCapabilitiesImmutable capsByFormat; // fixed requestedCaps by PixelFormat
    private Surface surface;
    private volatile long surfaceHandle;
    private long eglSurface;
    
    public interface SoftKeyboardBackListener {
      void onBackPressed();
    }
    
    public class MSurfaceView extends SurfaceView {
        SoftKeyboardBackListener softKeyboardBackListener;
      
        public MSurfaceView (Context ctx) {
            super(ctx);
            setBackgroundDrawable(null);
            // setBackgroundColor(Color.TRANSPARENT);
        }
        
        public  boolean dispatchKeyEventPreIme(android.view.KeyEvent event) {
          if (softKeyboardBackListener != null && 
                              event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                      KeyEvent.DispatcherState state = getKeyDispatcherState();
                      if (state != null) {
                          if (event.getAction() == KeyEvent.ACTION_DOWN
                                  && event.getRepeatCount() == 0) {
                              state.startTracking(event, this);
                              return true;
                          } else if (event.getAction() == KeyEvent.ACTION_UP
                                  && !event.isCanceled() && state.isTracking(event)) {
                              softKeyboardBackListener.onBackPressed();
                              return true;
                          }
                      }
                  }
          return super.dispatchKeyEventPreIme(event);
        }
        
        public void setSoftKeyboardBackListener(SoftKeyboardBackListener listener) {
          softKeyboardBackListener = listener;
        }
    }
    //----------------------------------------------------------------------
    // Internals only
    //
    protected static native boolean initIDs0();
    protected static native long getSurfaceHandle0(Surface surface);
    /** Return the native window format via <code>ANativeWindow_getFormat(..)</code>. */
    protected static native int getSurfaceVisualID0(long surfaceHandle);
    /** Set the native window format via <code>ANativeWindow_setBuffersGeometry(..)</code>. */
    protected static native void setSurfaceVisualID0(long surfaceHandle, int nativeVisualID);
    protected static native int getWidth0(long surfaceHandle);
    protected static native int getHeight0(long surfaceHandle);
    protected static native void acquire0(long surfaceHandle);
    protected static native void release0(long surfaceHandle);
}
