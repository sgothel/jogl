/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

package jogamp.newt.driver.windows;

import jogamp.nativewindow.windows.GDI;
import jogamp.nativewindow.windows.GDIUtil;
import jogamp.newt.WindowImpl;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;

import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;

public class WindowDriver extends WindowImpl {

    private long hmon;
    private long hdc;
    private long hdc_old;
    private long windowHandleClose;

    static {
        DisplayDriver.initSingleton();
    }

    public WindowDriver() {
    }

    @Override
    protected int lockSurfaceImpl() {
        if (0 != hdc) {
            throw new InternalError("surface not released");
        }
        final long hWnd = getWindowHandle();
        hdc = GDI.GetDC(hWnd);
        
        // return ( 0 == hdc ) ? LOCK_SURFACE_NOT_READY : ( hdc_old != hdc ) ? LOCK_SURFACE_CHANGED : LOCK_SUCCESS ;
        if( 0 == hdc ) { 
            return LOCK_SURFACE_NOT_READY;
        }        
        hmon = MonitorFromWindow0(hWnd);
        
        // Let's not trigger on HDC change, GLDrawableImpl.'s destroy/create is a nop here anyways.
        // FIXME: Validate against EGL surface creation: ANGLE uses HWND -> fine!
        return LOCK_SUCCESS; 
        
        /**
        if( hdc_old == hdc ) {
            return LOCK_SUCCESS;
        }
        if(DEBUG_IMPLEMENTATION) {            
            System.err.println("WindowsWindow: surface change "+toHexString(hdc_old)+" -> "+toHexString(hdc));
            // Thread.dumpStack();
        }
        return LOCK_SURFACE_CHANGED; */        
    }

    @Override
    protected void unlockSurfaceImpl() {
        if (0 != hdc) {
            GDI.ReleaseDC(getWindowHandle(), hdc);
            hdc_old = hdc;
            hdc=0;
        }
    }

    @Override
    public final long getSurfaceHandle() {
        return hdc;
    }

    @Override
    public boolean hasDeviceChanged() {
        if(0!=getWindowHandle()) {
            long _hmon = MonitorFromWindow0(getWindowHandle());
            if (hmon != _hmon) {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Info: Window Device Changed "+Thread.currentThread().getName()+
                                        ", HMON "+toHexString(hmon)+" -> "+toHexString(_hmon));
                    // Thread.dumpStack();
                }
                hmon = _hmon;
                return true;
            }
        }
        return false;
    }

    protected void createNativeImpl() {
        final ScreenDriver  screen = (ScreenDriver) getScreen();
        final DisplayDriver display = (DisplayDriver) screen.getDisplay();
        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(display.getGraphicsDevice(), capsRequested).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, screen.getGraphicsScreen(), VisualIDHolder.VID_UNDEFINED);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);
        final int flags = getReconfigureFlags(0, true) & 
                          ( FLAG_IS_ALWAYSONTOP | FLAG_IS_UNDECORATED ) ;
        final long _windowHandle = CreateWindow0(DisplayDriver.getHInstance(), display.getWindowClassName(), display.getWindowClassName(),
                                                 getParentWindowHandle(), getX(), getY(), getWidth(), getHeight(), autoPosition(), flags); 
        if ( 0 == _windowHandle ) {
            throw new NativeWindowException("Error creating window");
        }
        setWindowHandle(_windowHandle);
        windowHandleClose = _windowHandle;
        addMouseListener(mouseTracker);
        
        if(DEBUG_IMPLEMENTATION) {
            Exception e = new Exception("Info: Window new window handle "+Thread.currentThread().getName()+
                                        " (Parent HWND "+toHexString(getParentWindowHandle())+
                                        ") : HWND "+toHexString(_windowHandle)+", "+Thread.currentThread());
            e.printStackTrace();
        }
    }    
    private MouseAdapter mouseTracker = new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            WindowDriver.trackPointerLeave0(WindowDriver.this.getWindowHandle());
        }
    };

    protected void closeNativeImpl() {
        if(windowHandleClose != 0) {
            if (hdc != 0) {
                try {
                    GDI.ReleaseDC(windowHandleClose, hdc);
                } catch (Throwable t) {
                    if(DEBUG_IMPLEMENTATION) { 
                        Exception e = new Exception("Warning: closeNativeImpl failed - "+Thread.currentThread().getName(), t);
                        e.printStackTrace();
                    }
                }
            }
            try {
                GDI.DestroyWindow(windowHandleClose);
            } catch (Throwable t) {
                if(DEBUG_IMPLEMENTATION) {
                    Exception e = new Exception("Warning: closeNativeImpl failed - "+Thread.currentThread().getName(), t);
                    e.printStackTrace();
                }
            } finally {
                windowHandleClose = 0;
            }
        }
        hdc = 0;
        hdc_old = 0;
    }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("WindowsWindow reconfig: "+x+"/"+y+" "+width+"x"+height+", "+
                               getReconfigureFlagsAsString(null, flags));
        }
        
        if(0 == ( FLAG_IS_UNDECORATED & flags)) {
            final InsetsImmutable i = getInsets();
            
            // client position -> top-level window position
            x -= i.getLeftWidth() ;
            y -= i.getTopHeight() ;
            
            if(0<width && 0<height) {
                // client size -> top-level window size
                width += i.getTotalWidth();
                height += i.getTotalHeight();
            }
        }
        reconfigureWindow0( getParentWindowHandle(), getWindowHandle(), x, y, width, height, flags);
        
        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            visibleChanged(false, 0 != ( FLAG_IS_VISIBLE & flags));            
        }
        return true;
    }

    protected void requestFocusImpl(boolean force) {
        requestFocus0(getWindowHandle(), force);
    }

    @Override
    protected void setTitleImpl(final String title) {
        setTitle0(getWindowHandle(), title);
    }

    @Override
    protected boolean setPointerVisibleImpl(final boolean pointerVisible) {
        final boolean[] res = new boolean[] { false };
        
        this.runOnEDTIfAvail(true, new Runnable() {
            public void run() {
                res[0] = setPointerVisible0(getWindowHandle(), pointerVisible);
            }
        });
        return res[0];
    }

    @Override
    protected boolean confinePointerImpl(final boolean confine) {
        final Boolean[] res = new Boolean[] { Boolean.FALSE };
        
        this.runOnEDTIfAvail(true, new Runnable() {
            public void run() {
                final Point p0 = getLocationOnScreenImpl(0, 0);
                res[0] = Boolean.valueOf(confinePointer0(getWindowHandle(), confine, 
                        p0.getX(), p0.getY(), p0.getX()+getWidth(), p0.getY()+getHeight()));
            }
        });
        return res[0].booleanValue();
    }
    
    @Override
    protected void warpPointerImpl(final int x, final int y) {        
        this.runOnEDTIfAvail(true, new Runnable() {
            public void run() {
                final Point sPos = getLocationOnScreenImpl(x, y);
                warpPointer0(getWindowHandle(), sPos.getX(), sPos.getY());
            }
        });
        return;
    }
        
    protected Point getLocationOnScreenImpl(int x, int y) {
        return GDIUtil.GetRelativeLocation( getWindowHandle(), 0 /*root win*/, x, y);
    }

    protected void updateInsetsImpl(Insets insets) {
        // nop - using event driven insetsChange(..)         
    }
    
    private short repeatedKey = KeyEvent.VK_UNDEFINED;
    
    private final boolean handlePressTypedAutoRepeat(boolean isModifierKey, int modifiers, short keyCode, short keySym, char keyChar) {
        if( setKeyPressed(keyCode, true) ) {
            // AR: Key was already pressed: Either [enter | within] AR mode
            final boolean withinAR = repeatedKey == keyCode;
            repeatedKey = keyCode;
            if( !isModifierKey ) {
                // AR: Key was already pressed: Either [enter | within] AR mode
                modifiers |= InputEvent.AUTOREPEAT_MASK;
                if( withinAR ) {
                    // AR: Within AR mode
                    super.sendKeyEvent(KeyEvent.EVENT_KEY_PRESSED, modifiers, keyCode, keySym, keyChar);
                } // else { AR: Enter AR mode - skip already send PRESSED ; or ALT }
                super.sendKeyEvent(KeyEvent.EVENT_KEY_RELEASED, modifiers, keyCode, keySym, keyChar);
            }
            return true;
        } 
        return false;
    }
    
    @Override
    public final void sendKeyEvent(short eventType, int modifiers, short keyCode, short keySym, char keyChar) {
        final boolean isModifierKey = KeyEvent.isModifierKey(keySym);
        // System.err.println("*** sendKeyEvent: event "+KeyEvent.getEventTypeString(eventType)+", keyCode "+toHexString(keyCode)+", keyChar <"+keyChar+">, mods "+toHexString(modifiers)+
        //                   ", isKeyCodeTracked "+isKeyCodeTracked(keyCode)+", was: pressed "+isKeyPressed(keyCode)+", printableKey "+KeyEvent.isPrintableKey(keyCode, false)+" [modifierKey "+isModifierKey+"] - "+System.currentTimeMillis());
        
        // Reorder: WINDOWS delivery order is PRESSED (t0), TYPED (t0) and RELEASED (t1) -> NEWT order: PRESSED (t0) and RELEASED (t1)
        // Auto-Repeat: WINDOWS delivers only PRESSED (t0) and TYPED (t0).        
        switch(eventType) {
            case KeyEvent.EVENT_KEY_RELEASED:
                if( isKeyCodeTracked(keyCode) ) {
                    if( repeatedKey == keyCode && !isModifierKey ) {
                        // AR out - send out missing PRESSED
                        super.sendKeyEvent(KeyEvent.EVENT_KEY_PRESSED, modifiers | InputEvent.AUTOREPEAT_MASK, keyCode, keySym, keyChar);
                    }
                    setKeyPressed(keyCode, false);
                    repeatedKey = KeyEvent.VK_UNDEFINED;
                }
                super.sendKeyEvent(KeyEvent.EVENT_KEY_RELEASED, modifiers, keyCode, keySym, keyChar);
                break;
            case KeyEvent.EVENT_KEY_PRESSED:
                if( !handlePressTypedAutoRepeat(isModifierKey, modifiers, keyCode, keySym, keyChar) ) {
                    super.sendKeyEvent(KeyEvent.EVENT_KEY_PRESSED, modifiers, keyCode, keySym, keyChar);
                }
                break;
        }
    }
    
    @Override
    public final void enqueueKeyEvent(boolean wait, short eventType, int modifiers, short keyCode, short keySym, char keyChar) {
        throw new InternalError("XXX: Adapt Java Code to Native Code Changes");
    }
    
    //----------------------------------------------------------------------
    // Internals only
    //
    protected static native long getNewtWndProc0();
    protected static native boolean initIDs0(long hInstance);

    private native long CreateWindow0(long hInstance, String wndClassName, String wndName,
                                      long parentWindowHandle,
                                      int x, int y, int width, int height, boolean autoPosition, int flags);
    private native long MonitorFromWindow0(long windowHandle);
    private native void reconfigureWindow0(long parentWindowHandle, long windowHandle,
                                           int x, int y, int width, int height, int flags);
    private static native void setTitle0(long windowHandle, String title);
    private native void requestFocus0(long windowHandle, boolean force);

    private static native boolean setPointerVisible0(long windowHandle, boolean visible);
    private static native boolean confinePointer0(long windowHandle, boolean grab, int l, int t, int r, int b);
    private static native void warpPointer0(long windowHandle, int x, int y);    
    private static native void trackPointerLeave0(long windowHandle);    
}
