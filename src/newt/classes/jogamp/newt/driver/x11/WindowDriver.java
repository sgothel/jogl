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

import java.nio.Buffer;

import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.X11Util;
import jogamp.newt.DisplayImpl;
import jogamp.newt.DisplayImpl.DisplayRunnable;
import jogamp.newt.PointerIconImpl;
import jogamp.newt.WindowImpl;
import jogamp.newt.driver.PNGIcon;

import com.jogamp.nativewindow.*;
import com.jogamp.nativewindow.VisualIDHolder.VIDType;
import com.jogamp.nativewindow.util.Insets;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;

public class WindowDriver extends WindowImpl {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";
    private static final int X11_WHEEL_ONE_UP_BUTTON   = 4;
    private static final int X11_WHEEL_ONE_DOWN_BUTTON = 5;
    private static final int X11_WHEEL_TWO_UP_BUTTON   = 6;
    private static final int X11_WHEEL_TWO_DOWN_BUTTON = 7;

    private static final int defaultIconDataSize;
    private static final Buffer defaultIconData;

    static {
        ScreenDriver.initSingleton();

        int _icon_data_size=0, _icon_elem_bytesize=0;
        Buffer _icon_data=null;
        if( PNGIcon.isAvailable() ) {
            try {
                // NOTE: MUST BE DIRECT BUFFER, since _NET_WM_ICON Atom uses buffer directly!
                final int[] data_size = { 0 }, elem_bytesize = { 0 };
                _icon_data = PNGIcon.arrayToX11BGRAImages(NewtFactory.getWindowIcons(), data_size, elem_bytesize);
                _icon_data_size = data_size[0];
                _icon_elem_bytesize = elem_bytesize[0];
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        defaultIconDataSize = _icon_data_size;
        defaultIconData = _icon_data;
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Def. Icon: data_size "+defaultIconDataSize+" * elem_size "+_icon_elem_bytesize+" = data "+defaultIconData);
        }
    }

    public WindowDriver() {
    }

    @Override
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
            setWindowHandle(CreateWindow(getParentWindowHandle(),
                                   edtDevice.getHandle(), screen.getIndex(), visualID,
                                   display.getJavaObjectAtom(), display.getWindowDeleteAtom(),
                                   getX(), getY(), getWidth(), getHeight(), autoPosition(), flags,
                                   defaultIconDataSize, defaultIconData));
        } finally {
            edtDevice.unlock();
        }
        windowHandleClose = getWindowHandle();
        if (0 == windowHandleClose) {
            throw new NativeWindowException("Error creating window");
        }
    }

    @Override
    protected void closeNativeImpl() {
        if(0!=windowHandleClose && null!=getScreen() ) {
            final DisplayDriver display = (DisplayDriver) getScreen().getDisplay();
            final AbstractGraphicsDevice edtDevice = display.getGraphicsDevice();
            edtDevice.lock();
            try {
                CloseWindow0(edtDevice.getHandle(), windowHandleClose,
                             display.getJavaObjectAtom(), display.getWindowDeleteAtom() /* , display.getKbdHandle() */, // XKB disabled for now
                             display.getRandREventBase(), display.getRandRErrorBase());
            } catch (final Throwable t) {
                if(DEBUG_IMPLEMENTATION) {
                    final Exception e = new Exception("Warning: closeNativeImpl failed - "+Thread.currentThread().getName(), t);
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

    /**
     * <p>
     * X11 Window supports {@link #FLAG_IS_FULLSCREEN_SPAN}
     * </p>
     * {@inheritDoc}
     */
    @Override
    protected boolean isReconfigureFlagSupported(final int changeFlags) {
        return true; // all flags!
    }

    @Override
    protected boolean reconfigureWindowImpl(final int x, final int y, final int width, final int height, int flags) {
        final int _x, _y;
        final InsetsImmutable _insets;
        if( 0 == ( FLAG_IS_UNDECORATED & flags) ) {
            // client position -> top-level window position
            _insets = getInsets();
            _x = x - _insets.getLeftWidth() ;
            _y = y - _insets.getTopHeight() ;
        } else {
            _insets = null;
            _x = x;
            _y = y;
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("X11Window reconfig: "+x+"/"+y+" -> "+_x+"/"+_y+" "+width+"x"+height+", insets "+_insets+", "+ getReconfigureFlagsAsString(null, flags));
        }
        if( 0 != ( FLAG_CHANGE_FULLSCREEN & flags ) ) {
            if( 0 != ( FLAG_IS_FULLSCREEN & flags) && 0 == ( FLAG_IS_ALWAYSONTOP & flags) ) {
                tempFSAlwaysOnTop = true;
                flags |= FLAG_IS_ALWAYSONTOP;
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("X11Window reconfig.2: temporary "+getReconfigureFlagsAsString(null, flags));
                }
            } else {
                tempFSAlwaysOnTop = false;
            }
        }
        final int fflags = flags;
        final DisplayDriver display = (DisplayDriver) getScreen().getDisplay();
        runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
            @Override
            public Object run(final long dpy) {
                reconfigureWindow0( dpy, getScreenIndex(),
                                    getParentWindowHandle(), getWindowHandle(), display.getWindowDeleteAtom(),
                                    _x, _y, width, height, fflags);
                return null;
            }
        });
        return true;
    }
    volatile boolean tempFSAlwaysOnTop = false;

    /**
     * <p>
     * Deal w/ tempAlwaysOnTop.
     * </p>
     * {@inheritDoc}
     */
    @Override
    protected void focusChanged(final boolean defer, final boolean focusGained) {
        if( isNativeValid() && isFullscreen() && tempFSAlwaysOnTop && hasFocus() != focusGained ) {
            final int flags = getReconfigureFlags(FLAG_CHANGE_ALWAYSONTOP, isVisible()) | ( focusGained ? FLAG_IS_ALWAYSONTOP : 0 );
            if(DEBUG_IMPLEMENTATION) {
                System.err.println("X11Window reconfig.3 (focus): temporary "+getReconfigureFlagsAsString(null, flags));
            }
            final DisplayDriver display = (DisplayDriver) getScreen().getDisplay();
            runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
                @Override
                public Object run(final long dpy) {
                    reconfigureWindow0( dpy, getScreenIndex(),
                                        getParentWindowHandle(), getWindowHandle(), display.getWindowDeleteAtom(),
                                        getX(), getY(), getWidth(), getHeight(), flags);
                    return null;
                }
            });
        }
        super.focusChanged(defer, focusGained);
    }

    protected void reparentNotify(final long newParentWindowHandle) {
        if(DEBUG_IMPLEMENTATION) {
            final long p0 = getParentWindowHandle();
            System.err.println("Window.reparentNotify ("+getThreadName()+"): "+toHexString(p0)+" -> "+toHexString(newParentWindowHandle));
        }
    }

    @Override
    protected void requestFocusImpl(final boolean force) {
        runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
            @Override
            public Object run(final long dpy) {
                requestFocus0(dpy, getWindowHandle(), force);
                return null;
            }
        });
    }

    @Override
    protected void setTitleImpl(final String title) {
        runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
            @Override
            public Object run(final long dpy) {
                setTitle0(dpy, getWindowHandle(), title);
                return null;
            }
        });
    }

    @Override
    protected void setPointerIconImpl(final PointerIconImpl pi) {
        runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
            @Override
            public Object run(final long dpy) {
                try {
                    setPointerIcon0(dpy, getWindowHandle(), null != pi ? pi.validatedHandle() : 0);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    @Override
    protected boolean setPointerVisibleImpl(final boolean pointerVisible) {
        return runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Boolean>() {
            @Override
            public Boolean run(final long dpy) {
                final PointerIconImpl pi = (PointerIconImpl)getPointerIcon();
                final boolean res;
                if( pointerVisible && null != pi ) {
                    setPointerIcon0(dpy, getWindowHandle(), pi.validatedHandle());
                    res = true;
                } else {
                    res = setPointerVisible0(dpy, getWindowHandle(), pointerVisible);
                }
                return Boolean.valueOf(res);
            }
        }).booleanValue();
    }

    @Override
    protected boolean confinePointerImpl(final boolean confine) {
        return runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Boolean>() {
            @Override
            public Boolean run(final long dpy) {
                return Boolean.valueOf(confinePointer0(dpy, getWindowHandle(), confine));
            }
        }).booleanValue();
    }

    @Override
    protected void warpPointerImpl(final int x, final int y) {
        runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
            @Override
            public Object run(final long dpy) {
                warpPointer0(dpy, getWindowHandle(), x, y);
                return null;
            }
        });
    }

    @Override
    protected Point getLocationOnScreenImpl(final int x, final int y) {
        return runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Point>() {
            @Override
            public Point run(final long dpy) {
                return X11Lib.GetRelativeLocation(dpy, getScreenIndex(), getWindowHandle(), 0 /*root win*/, x, y);
            }
        } );
    }

    @Override
    protected void updateInsetsImpl(final Insets insets) {
        // nop - using event driven insetsChange(..)
    }

    @Override
    protected final void doMouseEvent(final boolean enqueue, final boolean wait, short eventType, int modifiers,
                                final int x, final int y, short button, final float[] rotationXYZ, final float rotationScale) {
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
                final boolean shiftPressed = 0 != ( modifiers & InputEvent.SHIFT_MASK );
                switch(button) {
                    case X11_WHEEL_ONE_UP_BUTTON: // vertical scroll up
                        eventType = MouseEvent.EVENT_MOUSE_WHEEL_MOVED;
                        button = 1;
                        rotationXYZ[shiftPressed ? 0 : 1] = 1;
                        break;
                    case X11_WHEEL_ONE_DOWN_BUTTON: // vertical scroll down
                        eventType = MouseEvent.EVENT_MOUSE_WHEEL_MOVED;
                        button = 1;
                        rotationXYZ[shiftPressed ? 0 : 1] = -1;
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

    /** Called by native TK */
    protected final void sendKeyEvent(final short eventType, final int modifiers, final short keyCode, final short keySym, final char keyChar0, final String keyString) {
        // handleKeyEvent(true, false, eventType, modifiers, keyCode, keyChar);
        final boolean isModifierKey = KeyEvent.isModifierKey(keyCode);
        final boolean isAutoRepeat = 0 != ( InputEvent.AUTOREPEAT_MASK & modifiers );
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
    public final void sendKeyEvent(final short eventType, final int modifiers, final short keyCode, final short keySym, final char keyChar) {
        throw new InternalError("XXX: Adapt Java Code to Native Code Changes");
    }
    @Override
    public final void enqueueKeyEvent(final boolean wait, final short eventType, final int modifiers, final short keyCode, final short keySym, final char keyChar) {
        throw new InternalError("XXX: Adapt Java Code to Native Code Changes");
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    private static final String getCurrentThreadName() { return Thread.currentThread().getName(); } // Callback for JNI
    private static final void dumpStack() { ExceptionUtils.dumpStack(System.err); } // Callback for JNI

    private final <T> T runWithLockedDisplayDevice(final DisplayRunnable<T> action) {
        return ((DisplayDriver) getScreen().getDisplay()).runWithLockedDisplayDevice(action);
    }

    protected static native boolean initIDs0();

    private long CreateWindow(final long parentWindowHandle, final long display, final int screen_index,
                              final int visualID, final long javaObjectAtom, final long windowDeleteAtom,
                              final int x, final int y, final int width, final int height, final boolean autoPosition, final int flags,
                              final int pixelDataSize, final Buffer pixels) {
        // NOTE: MUST BE DIRECT BUFFER, since _NET_WM_ICON Atom uses buffer directly!
        if( !Buffers.isDirect(pixels) ) {
            throw new IllegalArgumentException("data buffer is not direct "+pixels);
        }
        return CreateWindow0(parentWindowHandle, display, screen_index,
                             visualID, javaObjectAtom, windowDeleteAtom,
                             x, y, width, height, autoPosition, flags,
                             pixelDataSize,
                             pixels, Buffers.getDirectBufferByteOffset(pixels), true /* pixels_is_direct */);
    }
    private native long CreateWindow0(long parentWindowHandle, long display, int screen_index,
                                      int visualID, long javaObjectAtom, long windowDeleteAtom,
                                      int x, int y, int width, int height, boolean autoPosition, int flags,
                                      int pixelDataSize, Object pixels, int pixels_byte_offset, boolean pixels_is_direct);
    private native void CloseWindow0(long display, long windowHandle, long javaObjectAtom, long windowDeleteAtom /*, long kbdHandle*/, // XKB disabled for now
                                     final int randr_event_base, final int randr_error_base);
    private native void reconfigureWindow0(long display, int screen_index, long parentWindowHandle, long windowHandle,
                                           long windowDeleteAtom, int x, int y, int width, int height, int flags);
    private native void requestFocus0(long display, long windowHandle, boolean force);

    private static native void setTitle0(long display, long windowHandle, String title);

    private static native void setPointerIcon0(long display, long windowHandle, long handle);

    private static native long getParentWindow0(long display, long windowHandle);
    private static native boolean setPointerVisible0(long display, long windowHandle, boolean visible);
    private static native boolean confinePointer0(long display, long windowHandle, boolean grab);
    private static native void warpPointer0(long display, long windowHandle, int x, int y);

    private long   windowHandleClose;
    private X11GraphicsDevice renderDevice;
}
