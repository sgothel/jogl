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

import java.nio.ByteBuffer;

import jogamp.nativewindow.windows.GDI;
import jogamp.nativewindow.windows.GDIUtil;
import jogamp.newt.PointerIconImpl;
import jogamp.newt.WindowImpl;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseEvent.PointerType;

public class WindowDriver extends WindowImpl {

    static {
        DisplayDriver.initSingleton();
    }

    private long hmon;
    private long hdc;
    private long hdc_old;
    private long windowHandleClose;

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
            final long _hmon = MonitorFromWindow0(getWindowHandle());
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

    @Override
    protected void createNativeImpl() {
        final ScreenDriver  screen = (ScreenDriver) getScreen();
        final DisplayDriver display = (DisplayDriver) screen.getDisplay();
        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(display.getGraphicsDevice(), capsRequested).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, screen.getGraphicsScreen(), VisualIDHolder.VID_UNDEFINED);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);
        final VersionNumber winVer = Platform.getOSVersionNumber();
        int flags = getReconfigureMask(0, true) & STATE_MASK_CREATENATIVE;
        int maxCount = 0;
        if( 0 != ( STATE_MASK_MAXIMIZED_HORZ & flags ) ) {
            flags |= CHANGE_MASK_MAXIMIZED_HORZ;
            maxCount++;
        }
        if( 0 != ( STATE_MASK_MAXIMIZED_VERT & flags ) ) {
            flags |= CHANGE_MASK_MAXIMIZED_VERT;
            maxCount++;
        }
        final long _windowHandle = CreateWindow0(DisplayDriver.getHInstance(), display.getWindowClassName(), display.getWindowClassName(),
                                                 winVer.getMajor(), winVer.getMinor(),
                                                 getParentWindowHandle(),
                                                 getX(), getY(), getWidth(), getHeight(), flags);
        if ( 0 == _windowHandle ) {
            throw new NativeWindowException("Error creating window");
        }
        if( !cfg.getChosenCapabilities().isBackgroundOpaque() ) {
            GDIUtil.DwmSetupTranslucency(_windowHandle, true);
        }
        InitWindow0(_windowHandle, flags);
        setWindowHandle(_windowHandle);
        windowHandleClose = _windowHandle;

        if( 0 == ( STATE_MASK_CHILDWIN & flags ) && 1 == maxCount ) {
            reconfigureWindowImpl(getX(), getY(), getWidth(), getHeight(), flags);
        }

        if(DEBUG_IMPLEMENTATION) {
            final Exception e = new Exception("Info: Window new window handle "+Thread.currentThread().getName()+
                                        " (Parent HWND "+toHexString(getParentWindowHandle())+
                                        ") : HWND "+toHexString(_windowHandle)+", "+Thread.currentThread());
            e.printStackTrace();
        }
    }

    @Override
    protected void closeNativeImpl() {
        if( 0 != windowHandleClose ) {
            if ( 0 != hdc ) {
                try {
                    GDI.ReleaseDC(windowHandleClose, hdc);
                } catch (final Throwable t) {
                    if(DEBUG_IMPLEMENTATION) {
                        final Exception e = new Exception("Warning: closeNativeImpl failed - "+Thread.currentThread().getName(), t);
                        e.printStackTrace();
                    }
                }
            }
            try {
                GDI.DestroyWindow(windowHandleClose);
            } catch (final Throwable t) {
                if(DEBUG_IMPLEMENTATION) {
                    final Exception e = new Exception("Warning: closeNativeImpl failed - "+Thread.currentThread().getName(), t);
                    e.printStackTrace();
                }
            }
        }
        windowHandleClose = 0;
        hdc = 0;
        hdc_old = 0;
    }

    @Override
    protected final int getSupportedReconfigMaskImpl() {
        return minimumReconfigStateMask |
               STATE_MASK_CHILDWIN |
               STATE_MASK_UNDECORATED |
               STATE_MASK_ALWAYSONTOP |
               STATE_MASK_ALWAYSONBOTTOM |
               // STATE_MASK_STICKY |
               STATE_MASK_RESIZABLE |
               STATE_MASK_MAXIMIZED_VERT |
               STATE_MASK_MAXIMIZED_HORZ |
               STATE_MASK_POINTERVISIBLE |
               STATE_MASK_POINTERCONFINED;
    }

    @Override
    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, final int flags) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("WindowsWindow reconfig.0: "+x+"/"+y+" "+width+"x"+height+
                               ", "+getReconfigStateMaskString(flags));
        }
        final InsetsImmutable insets = getInsets();

        if( 0 == ( STATE_MASK_CHILDWIN & flags ) &&
            0 != ( ( CHANGE_MASK_MAXIMIZED_HORZ | CHANGE_MASK_MAXIMIZED_VERT ) & flags ) ) {
            final int[] posSize = { x, y, width, height };
            if( ( 0 != ( STATE_MASK_MAXIMIZED_HORZ & flags ) ) == ( 0 != ( STATE_MASK_MAXIMIZED_VERT & flags ) ) ) {
                resetMaximizedManual(posSize); // reset before native maximize/reset
            } else {
                reconfigMaximizedManual(flags, posSize, insets);
            }
            x = posSize[0];
            y = posSize[1];
            width = posSize[2];
            height = posSize[3];
        }

        final boolean changeDecoration = 0 != ( CHANGE_MASK_DECORATION & flags);
        final boolean isTranslucent = !getChosenCapabilities().isBackgroundOpaque();
        if( changeDecoration && isTranslucent ) {
            GDIUtil.DwmSetupTranslucency(getWindowHandle(), false);
        }
        reconfigureWindow0( getParentWindowHandle(), getWindowHandle(), x, y, width, height, flags);
        if( changeDecoration && isTranslucent ) {
            GDIUtil.DwmSetupTranslucency(getWindowHandle(), true);
        }

        if( 0 != ( CHANGE_MASK_VISIBILITY & flags) ) {
            visibleChanged(false, 0 != ( STATE_MASK_VISIBLE & flags));
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("WindowsWindow reconfig.X: "+getX()+"/"+getY()+" "+getWidth()+"x"+getHeight()+", "+getStateMaskString());
        }
        return true;
    }

    @Override
    protected void requestFocusImpl(final boolean force) {
        requestFocus0(getWindowHandle(), force);
    }

    @Override
    protected void setTitleImpl(final String title) {
        setTitle0(getWindowHandle(), title);
    }

    @Override
    protected void setPointerIconImpl(final PointerIconImpl pi) {
        setPointerIcon0(getWindowHandle(), null != pi ? pi.validatedHandle() : 0);
    }

    @Override
    protected boolean setPointerVisibleImpl(final boolean pointerVisible) {
        final boolean[] res = new boolean[] { false };

        this.runOnEDTIfAvail(true, new Runnable() {
            @Override
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
            @Override
            public void run() {
                final Point p0 = convertToPixelUnits( getLocationOnScreenImpl(0, 0) );
                res[0] = Boolean.valueOf(confinePointer0(getWindowHandle(), confine,
                                                         p0.getX(), p0.getY(),
                                                         p0.getX()+getSurfaceWidth(), p0.getY()+getSurfaceHeight()));
            }
        });
        return res[0].booleanValue();
    }

    @Override
    protected void warpPointerImpl(final int x, final int y) {
        this.runOnEDTIfAvail(true, new Runnable() {
            @Override
            public void run() {
                final Point sPos = convertToPixelUnits( getLocationOnScreenImpl(x, y) );
                warpPointer0(getWindowHandle(), sPos.getX(), sPos.getY());
            }
        });
        return;
    }

    @Override
    protected Point getLocationOnScreenImpl(final int x, final int y) {
        return GDIUtil.GetRelativeLocation( getWindowHandle(), 0 /*root win*/, x, y);
    }

    //
    // PointerEvent Handling
    //
    /**
     * Send multiple-pointer {@link MouseEvent.PointerType#TouchScreen} event to be directly consumed
     * <p>
     * Assumes non normal pointer names and rotation/scroll will be determined by a gesture handler.
     * </p>
     * <p>
     * See {@link #doPointerEvent(boolean, boolean, PointerType[], short, int, int, boolean, int[], int[], int[], float[], float, float[], float)}
     * for details.
     * </p>
     */
    public final void sendTouchScreenEvent(final short eventType, final int modifiers,
                                           final int pActionIdx, final int[] pNames,
                                           final int[] pX, final int[] pY, final float[] pPressure, final float maxPressure) {
        final int pCount = pNames.length;
        final MouseEvent.PointerType[] pTypes = new MouseEvent.PointerType[pCount];
        for(int i=pCount-1; i>=0; i--) { pTypes[i] = PointerType.TouchScreen; }
        doPointerEvent(false /*enqueue*/, false /*wait*/,
                       pTypes, eventType, modifiers, pActionIdx, false /*normalPNames*/, pNames,
                       pX, pY, pPressure, maxPressure, new float[] { 0f, 0f, 0f} /*rotationXYZ*/, 1f/*rotationScale*/);
    }

    //
    // KeyEvent Handling
    //
    private short repeatedKey = KeyEvent.VK_UNDEFINED;

    private final boolean handlePressTypedAutoRepeat(final boolean isModifierKey, int modifiers, final short keyCode, final short keySym, final char keyChar) {
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
    public final void sendKeyEvent(final short eventType, final int modifiers, final short keyCode, final short keySym, final char keyChar) {
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
    public final void enqueueKeyEvent(final boolean wait, final short eventType, final int modifiers, final short keyCode, final short keySym, final char keyChar) {
        throw new InternalError("XXX: Adapt Java Code to Native Code Changes");
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    protected static native long getNewtWndProc0();
    protected static native boolean initIDs0(long hInstance);

    private native long CreateWindow0(long hInstance, String wndClassName, String wndName, int winMajor, int winMinor,
                                      long parentWindowHandle, int x, int y, int width, int height, int flags);
    private native void InitWindow0(long windowHandle, int flags);
    private native long MonitorFromWindow0(long windowHandle);
    private native void reconfigureWindow0(long parentWindowHandle, long windowHandle,
                                           int x, int y, int width, int height, int flags);
    private static native void setTitle0(long windowHandle, String title);
    private native void requestFocus0(long windowHandle, boolean force);

    private static native boolean setPointerVisible0(long windowHandle, boolean visible);
    private static native boolean confinePointer0(long windowHandle, boolean grab, int l, int t, int r, int b);
    private static native void warpPointer0(long windowHandle, int x, int y);
    private static native ByteBuffer newDirectByteBuffer(long addr, long capacity);

    private static native void setPointerIcon0(long windowHandle, long iconHandle);
}
