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

package jogamp.newt.driver.x11;

import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;
import jogamp.newt.DisplayImpl;
import jogamp.newt.DisplayImpl.DisplayRunnable;
import jogamp.newt.WindowImpl;
import javax.media.nativewindow.*;
import javax.media.nativewindow.VisualIDHolder.VIDType;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;

import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;

public class WindowDriver extends WindowImpl {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";
    private static final int X11_WHEEL_ONE_UP_BUTTON   = 4;
    private static final int X11_WHEEL_ONE_DOWN_BUTTON = 5;
    private static final int X11_WHEEL_TWO_UP_BUTTON   = 6;
    private static final int X11_WHEEL_TWO_DOWN_BUTTON = 7;
    
    static {
        DisplayDriver.initSingleton();
    }

    public WindowDriver() {
    }

    protected void createNativeImpl() {
        final ScreenDriver screen = (ScreenDriver) getScreen();
        final DisplayDriver display = (DisplayDriver) screen.getDisplay();
        final AbstractGraphicsDevice edtDevice = display.getGraphicsDevice();
        
        // Decoupled X11 Device/Screen allowing X11 display lock-free off-thread rendering 
        final long renderDeviceHandle = X11Util.openDisplay(edtDevice.getConnection());
        if( 0 == renderDeviceHandle ) {
            throw new RuntimeException("Error creating display(GfxCfg/Render): "+edtDevice.getConnection());
        }
        renderDevice = new X11GraphicsDevice(renderDeviceHandle, AbstractGraphicsDevice.DEFAULT_UNIT, true /* owner */);
        final AbstractGraphicsScreen renderScreen = new X11GraphicsScreen(renderDevice, screen.getIndex());
        
        final GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(display.getGraphicsDevice(), capsRequested);
        final AbstractGraphicsConfiguration cfg = factory.chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, renderScreen, VisualIDHolder.VID_UNDEFINED);
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("X11Window.createNativeImpl() factory: "+factory+", chosen config: "+cfg);
        }        
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        final int visualID = cfg.getVisualID(VIDType.NATIVE);
        if(VisualIDHolder.VID_UNDEFINED == visualID) {
            throw new NativeWindowException("Chosen Configuration w/o native visual ID: "+cfg);
        }
        setGraphicsConfiguration(cfg);
        final int flags = getReconfigureFlags(0, true) & 
                          ( FLAG_IS_ALWAYSONTOP | FLAG_IS_UNDECORATED ) ;
        edtDevice.lock();
        try {        
            setWindowHandle(CreateWindow0(getParentWindowHandle(),
                                   edtDevice.getHandle(), screen.getIndex(), visualID, 
                                   display.getJavaObjectAtom(), display.getWindowDeleteAtom(), 
                                   getX(), getY(), getWidth(), getHeight(), autoPosition(), flags));
        } finally {
            edtDevice.unlock();
        }
        windowHandleClose = getWindowHandle();
        if (0 == windowHandleClose) {
            throw new NativeWindowException("Error creating window");
        }
    }

    protected void closeNativeImpl() {
        if(0!=windowHandleClose && null!=getScreen() ) {
            DisplayDriver display = (DisplayDriver) getScreen().getDisplay();
            final AbstractGraphicsDevice edtDevice = display.getGraphicsDevice();
            edtDevice.lock();
            try {
                CloseWindow0(edtDevice.getHandle(), windowHandleClose, 
                             display.getJavaObjectAtom(), display.getWindowDeleteAtom() /* , display.getKbdHandle() */); // XKB disabled for now
            } catch (Throwable t) {
                if(DEBUG_IMPLEMENTATION) { 
                    Exception e = new Exception("Warning: closeNativeImpl failed - "+Thread.currentThread().getName(), t);
                    e.printStackTrace();
                }
            } finally {
                edtDevice.unlock();
                windowHandleClose = 0;
            }
        }
        if(null != renderDevice) {
            renderDevice.close(); // closes X11 display
            renderDevice = null;
        }
    }

    protected boolean reconfigureWindowImpl(final int x, final int y, final int width, final int height, final int flags) { 
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("X11Window reconfig: "+x+"/"+y+" "+width+"x"+height+", "+ getReconfigureFlagsAsString(null, flags));
        }
        final int _x, _y;
        if(0 == ( FLAG_IS_UNDECORATED & flags)) {
            final InsetsImmutable i = getInsets();         
            
            // client position -> top-level window position
            _x = x - i.getLeftWidth() ;
            _y = y - i.getTopHeight() ;
        } else {
            _x = x;
            _y = y;
        }
        final DisplayDriver display = (DisplayDriver) getScreen().getDisplay();
        runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
            public Object run(long dpy) {
                reconfigureWindow0( dpy, getScreenIndex(), 
                                    getParentWindowHandle(), getWindowHandle(), display.getWindowDeleteAtom(),
                                    _x, _y, width, height, flags);
                return null;
            }
        });
        return true;
    }

    protected void reparentNotify(long newParentWindowHandle) {
        if(DEBUG_IMPLEMENTATION) {
            final long p0 = getParentWindowHandle();
            System.err.println("Window.reparentNotify ("+getThreadName()+"): "+toHexString(p0)+" -> "+toHexString(newParentWindowHandle));
        }
    }
    
    protected void requestFocusImpl(final boolean force) {        
        runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
            public Object run(long dpy) {
                requestFocus0(dpy, getWindowHandle(), force);
                return null;
            }
        });
    }

    @Override
    protected void setTitleImpl(final String title) {
        runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
            public Object run(long dpy) {
                setTitle0(dpy, getWindowHandle(), title);
                return null;
            }
        });
    }
    
    @Override
    protected boolean setPointerVisibleImpl(final boolean pointerVisible) {
        return runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Boolean>() {
            public Boolean run(long dpy) {
                return Boolean.valueOf(setPointerVisible0(dpy, getWindowHandle(), pointerVisible));
            }
        }).booleanValue();
    }

    @Override
    protected boolean confinePointerImpl(final boolean confine) {
        return runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Boolean>() {
            public Boolean run(long dpy) {
                return Boolean.valueOf(confinePointer0(dpy, getWindowHandle(), confine));
            }
        }).booleanValue();
    }
    
    @Override
    protected void warpPointerImpl(final int x, final int y) {
        runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
            public Object run(long dpy) {
                warpPointer0(dpy, getWindowHandle(), x, y);
                return null;
            }
        });
    }
    
    protected Point getLocationOnScreenImpl(final int x, final int y) {
        return runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Point>() {
            public Point run(long dpy) {
                return X11Lib.GetRelativeLocation(dpy, getScreenIndex(), getWindowHandle(), 0 /*root win*/, x, y);
            }
        } );
    }

    protected void updateInsetsImpl(Insets insets) {
        // nop - using event driven insetsChange(..)         
    }
    
    @Override
    protected final void doMouseEvent(boolean enqueue, boolean wait, short eventType, int modifiers,
                                int x, int y, short button, float[] rotationXYZ, float rotationScale) {
        switch(eventType) {
            case MouseEvent.EVENT_MOUSE_PRESSED:
                switch(button) {
                    case X11_WHEEL_ONE_UP_BUTTON:
                    case X11_WHEEL_ONE_DOWN_BUTTON:
                    case X11_WHEEL_TWO_UP_BUTTON:
                    case X11_WHEEL_TWO_DOWN_BUTTON:
                        // ignore wheel pressed !
                        return;
                }
                break;
            case MouseEvent.EVENT_MOUSE_RELEASED:
                switch(button) {
                    case X11_WHEEL_ONE_UP_BUTTON: // vertical scroll up
                        eventType = MouseEvent.EVENT_MOUSE_WHEEL_MOVED;
                        button = 1;
                        rotationXYZ[1] = 1;
                        break;
                    case X11_WHEEL_ONE_DOWN_BUTTON: // vertical scroll down
                        eventType = MouseEvent.EVENT_MOUSE_WHEEL_MOVED;
                        button = 1;
                        rotationXYZ[1] = -1;
                        break;
                    case X11_WHEEL_TWO_UP_BUTTON: // horizontal scroll left
                        eventType = MouseEvent.EVENT_MOUSE_WHEEL_MOVED;
                        button = 1;
                        rotationXYZ[0] = 1;
                        modifiers |= InputEvent.SHIFT_MASK;
                        break;
                    case X11_WHEEL_TWO_DOWN_BUTTON: // horizontal scroll right
                        eventType = MouseEvent.EVENT_MOUSE_WHEEL_MOVED;
                        button = 1;
                        rotationXYZ[0] = -1;
                        modifiers |= InputEvent.SHIFT_MASK;
                        break;
                }                
                break;
        }
        super.doMouseEvent(enqueue, wait, eventType, modifiers, x, y, button, rotationXYZ, rotationScale);
    }
    
    protected final void sendKeyEvent(short eventType, int modifiers, short keyCode, short keySym, char keyChar0, String keyString) {
        // handleKeyEvent(true, false, eventType, modifiers, keyCode, keyChar);
        final boolean isModifierKey = KeyEvent.isModifierKey(keyCode);
        final boolean isAutoRepeat = 0 != ( KeyEvent.AUTOREPEAT_MASK & modifiers );
        final char keyChar =  ( null != keyString ) ? keyString.charAt(0) : keyChar0;        
        // System.err.println("*** sendKeyEvent: event "+KeyEvent.getEventTypeString(eventType)+", keyCode "+toHexString(keyCode)+", keyChar <"+keyChar0+">/<"+keyChar+">, keyString "+keyString+", mods "+toHexString(modifiers)+
        //                    ", isKeyCodeTracked "+isKeyCodeTracked(keyCode)+", was: pressed "+isKeyPressed(keyCode)+", repeat "+isAutoRepeat+", [modifierKey "+isModifierKey+"] - "+System.currentTimeMillis());
        
        if( !isAutoRepeat || !isModifierKey ) { // ! (  isModifierKey && isAutoRepeat )
            switch(eventType) {
                case KeyEvent.EVENT_KEY_PRESSED:
                    super.sendKeyEvent(KeyEvent.EVENT_KEY_PRESSED, modifiers, keyCode, keySym, keyChar);
                    break;
                    
                case KeyEvent.EVENT_KEY_RELEASED:
                    super.sendKeyEvent(KeyEvent.EVENT_KEY_RELEASED, modifiers, keyCode, keySym, keyChar);
                    break;
            }
        }
    }
    
    @Override
    public final void sendKeyEvent(short eventType, int modifiers, short keyCode, short keySym, char keyChar) {
        throw new InternalError("XXX: Adapt Java Code to Native Code Changes");
    }
    @Override
    public final void enqueueKeyEvent(boolean wait, short eventType, int modifiers, short keyCode, short keySym, char keyChar) {
        throw new InternalError("XXX: Adapt Java Code to Native Code Changes");
    }
    
    //----------------------------------------------------------------------
    // Internals only
    //
    private static final String getCurrentThreadName() { return Thread.currentThread().getName(); } // Callback for JNI
    private static final void dumpStack() { Thread.dumpStack(); } // Callback for JNI
    
    private final <T> T runWithLockedDisplayDevice(DisplayRunnable<T> action) {
        return ((DisplayDriver) getScreen().getDisplay()).runWithLockedDisplayDevice(action);
    }

    protected static native boolean initIDs0();
    
    private native long CreateWindow0(long parentWindowHandle, long display, int screen_index, 
                                      int visualID, long javaObjectAtom, long windowDeleteAtom, 
                                      int x, int y, int width, int height, boolean autoPosition, int flags); 
    private native void CloseWindow0(long display, long windowHandle, long javaObjectAtom, long windowDeleteAtom /*, long kbdHandle*/ ); // XKB disabled for now
    private native void reconfigureWindow0(long display, int screen_index, long parentWindowHandle, long windowHandle,
                                           long windowDeleteAtom, int x, int y, int width, int height, int flags);    
    private native void requestFocus0(long display, long windowHandle, boolean force);
    
    private static native void setTitle0(long display, long windowHandle, String title);
    private static native long getParentWindow0(long display, long windowHandle);
    private static native boolean setPointerVisible0(long display, long windowHandle, boolean visible);
    private static native boolean confinePointer0(long display, long windowHandle, boolean grab);
    private static native void warpPointer0(long display, long windowHandle, int x, int y);
    
    private long   windowHandleClose;
    private X11GraphicsDevice renderDevice;
}
