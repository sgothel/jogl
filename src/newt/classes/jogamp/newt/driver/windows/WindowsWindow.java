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
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;

public class WindowsWindow extends WindowImpl {

    private long hmon;
    private long hdc;
    private long windowHandleClose;

    static {
        WindowsDisplay.initSingleton();
    }

    public WindowsWindow() {
    }

    @Override
    protected int lockSurfaceImpl() {
        if (0 != hdc) {
            throw new InternalError("surface not released");
        }
        hdc = GDI.GetDC(getWindowHandle());
        hmon = MonitorFromWindow0(getWindowHandle());
        return ( 0 != hdc ) ? LOCK_SUCCESS : LOCK_SURFACE_NOT_READY;
    }

    @Override
    protected void unlockSurfaceImpl() {
        if (0 == hdc) {
            throw new InternalError("surface not acquired");
        }
        GDI.ReleaseDC(getWindowHandle(), hdc);
        hdc=0;
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
                    Exception e = new Exception("Info: Window Device Changed "+Thread.currentThread().getName()+
                                                ", HMON "+toHexString(hmon)+" -> "+toHexString(_hmon));
                    e.printStackTrace();
                }
                hmon = _hmon;
                return true;
            }
        }
        return false;
    }

    protected void createNativeImpl() {
        final WindowsScreen  screen = (WindowsScreen) getScreen();
        final WindowsDisplay display = (WindowsDisplay) screen.getDisplay();
        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(display.getGraphicsDevice()).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, screen.getGraphicsScreen());
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);
        final int flags = getReconfigureFlags(0, true) & 
                          ( FLAG_IS_ALWAYSONTOP | FLAG_IS_UNDECORATED ) ;
        setWindowHandle(CreateWindow0(display.getHInstance(), display.getWindowClassName(), display.getWindowClassName(),
                                      getParentWindowHandle(), x, y, width, height, autoPosition, flags)); 
        if (getWindowHandle() == 0) {
            throw new NativeWindowException("Error creating window");
        }
        windowHandleClose = getWindowHandle();
        addMouseListener(new MouseTracker());
        
        if(DEBUG_IMPLEMENTATION) {
            Exception e = new Exception("Info: Window new window handle "+Thread.currentThread().getName()+
                                        " (Parent HWND "+toHexString(getParentWindowHandle())+
                                        ") : HWND "+toHexString(getWindowHandle())+", "+Thread.currentThread());
            e.printStackTrace();
        }
    }
    
    class MouseTracker extends MouseAdapter {
        public void mouseEntered(MouseEvent e) {
            WindowsWindow.trackPointerLeave0(WindowsWindow.this.getWindowHandle());
        }
    }

    protected void closeNativeImpl() {
        if (hdc != 0) {
            if(windowHandleClose != 0) {
                try {
                    GDI.ReleaseDC(windowHandleClose, hdc);
                } catch (Throwable t) {
                    if(DEBUG_IMPLEMENTATION) { 
                        Exception e = new Exception("Warning: closeNativeImpl failed - "+Thread.currentThread().getName(), t);
                        e.printStackTrace();
                    }
                }
            }
            hdc = 0;
        }
        if(windowHandleClose != 0) {
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
        final Boolean[] res = new Boolean[] { Boolean.FALSE };
        
        this.runOnEDTIfAvail(true, new Runnable() {
            public void run() {
                res[0] = Boolean.valueOf(setPointerVisible0(getWindowHandle(), pointerVisible));
            }
        });
        return res[0].booleanValue();
    }

    @Override
    protected boolean confinePointerImpl(final boolean confine) {
        final Boolean[] res = new Boolean[] { Boolean.FALSE };
        
        this.runOnEDTIfAvail(true, new Runnable() {
            public void run() {
                final Point p0 = getLocationOnScreenImpl(0, 0);
                res[0] = Boolean.valueOf(confinePointer0(getWindowHandle(), confine, 
                        p0.getX(), p0.getY(), p0.getX()+width, p0.getY()+height));
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
    
    private final int validateKeyCode(int eventType, int keyCode) {
        switch(eventType) {
            case KeyEvent.EVENT_KEY_PRESSED:
                lastPressedKeyCode = keyCode;
                break;
            case KeyEvent.EVENT_KEY_TYPED:
                if(-1==keyCode) {
                    keyCode = lastPressedKeyCode;
                }
                lastPressedKeyCode = -1;
                break;
        }
        return keyCode;
    }
    private int lastPressedKeyCode = 0;
    
    @Override
    public void sendKeyEvent(int eventType, int modifiers, int keyCode, char keyChar) {
        // Note that we have to regenerate the keyCode for EVENT_KEY_TYPED on this platform
        keyCode = validateKeyCode(eventType, keyCode);
        super.sendKeyEvent(eventType, modifiers, keyCode, keyChar);        
    }
    
    @Override
    public void enqueueKeyEvent(boolean wait, int eventType, int modifiers, int keyCode, char keyChar) {
        // Note that we have to regenerate the keyCode for EVENT_KEY_TYPED on this platform
        keyCode = validateKeyCode(eventType, keyCode);
        super.enqueueKeyEvent(wait, eventType, modifiers, keyCode, keyChar);
    }
    
    //----------------------------------------------------------------------
    // Internals only
    //
    protected static native boolean initIDs0();
    protected static native long getNewtWndProc0();

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
