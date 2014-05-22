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

package jogamp.newt.driver.macosx;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.MutableSurface;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.PointImmutable;

import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.newt.PointerIconImpl;
import jogamp.newt.WindowImpl;
import jogamp.newt.driver.DriverClearFocus;
import jogamp.newt.driver.DriverUpdatePosition;

import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;

public class WindowDriver extends WindowImpl implements MutableSurface, DriverClearFocus, DriverUpdatePosition {

    static {
        DisplayDriver.initSingleton();
    }

    public WindowDriver() {
    }

    @Override
    protected void createNativeImpl() {
        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice(), capsRequested).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, getScreen().getGraphicsScreen(), VisualIDHolder.VID_UNDEFINED);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);
        reconfigureWindowImpl(getX(), getY(), getWindowWidth(), getWindowHeight(), getReconfigureFlags(FLAG_CHANGE_VISIBILITY, true));
        if (0 == getWindowHandle()) {
            throw new NativeWindowException("Error creating window");
        }
    }

    @Override
    protected void closeNativeImpl() {
        try {
            if(DEBUG_IMPLEMENTATION) { System.err.println("MacWindow.CloseAction "+Thread.currentThread().getName()); }
            final long handle = getWindowHandle();
            visibleChanged(true, false);
            setWindowHandle(0);
            surfaceHandle = 0;
            sscSurfaceHandle = 0;
            isOffscreenInstance = false;
            if (0 != handle) {
                OSXUtil.RunOnMainThread(false, new Runnable() {
                   @Override
                   public void run() {
                       close0( handle );
                   } } );
            }
        } catch (Throwable t) {
            if(DEBUG_IMPLEMENTATION) {
                Exception e = new Exception("Warning: closeNative failed - "+Thread.currentThread().getName(), t);
                e.printStackTrace();
            }
        }
    }

    @Override
    protected int lockSurfaceImpl() {
        /**
         * if( isOffscreenInstance ) {
         *    return LOCK_SUCCESS;
         * }
         */
        final long w = getWindowHandle();
        final long v = surfaceHandle;
        if( 0 != v && 0 != w ) {
            return lockSurface0(w, v) ? LOCK_SUCCESS : LOCK_SURFACE_NOT_READY;
        }
        return LOCK_SURFACE_NOT_READY;
    }

    @Override
    protected void unlockSurfaceImpl() {
        /**
         * if( isOffscreenInstance ) {
         *    return;
         * }
         */
        final long w = getWindowHandle();
        final long v = surfaceHandle;
        if(0 != w && 0 != v) {
            if( !unlockSurface0(w, v) ) {
                throw new NativeWindowException("Failed to unlock surface, probably not locked!");
            }
        }
    }

    @Override
    public final long getSurfaceHandle() {
        return 0 != sscSurfaceHandle ? sscSurfaceHandle : surfaceHandle;
    }

    @Override
    public void setSurfaceHandle(long surfaceHandle) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow.setSurfaceHandle(): 0x"+Long.toHexString(surfaceHandle));
        }
        sscSurfaceHandle = surfaceHandle;
        if (isNativeValid()) {
            if (0 != sscSurfaceHandle) {
                OSXUtil.RunOnMainThread(false, new Runnable() {
                    @Override
                    public void run() {
                        orderOut0( 0 != getParentWindowHandle() ? getParentWindowHandle() : getWindowHandle() );
                    } } );
            } /** this is done by recreation!
              else if (isVisible()){
                OSXUtil.RunOnMainThread(false, new Runnable() {
                    public void run() {
                        orderFront0( 0!=getParentWindowHandle() ? getParentWindowHandle() : getWindowHandle() );
                    } } );
            } */
        }
    }

    @Override
    protected void setTitleImpl(final String title) {
        OSXUtil.RunOnMainThread(false, new Runnable() {
            @Override
            public void run() {
                setTitle0(getWindowHandle(), title);
            } } );
    }

    @Override
    protected void requestFocusImpl(final boolean force) {
        final boolean _isFullscreen = isFullscreen();
        final boolean _isOffscreenInstance = isOffscreenInstance;
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow: requestFocusImpl(), isOffscreenInstance "+_isOffscreenInstance+", isFullscreen "+_isFullscreen);
        }
        if(!_isOffscreenInstance) {
            OSXUtil.RunOnMainThread(false, new Runnable() {
                @Override
                public void run() {
                    requestFocus0(getWindowHandle(), force);
                    if(_isFullscreen) {
                        // 'NewtMacWindow::windowDidBecomeKey()' is not always called in fullscreen-mode!
                        focusChanged(false, true);
                    }
                } } );
        } else {
            focusChanged(false, true);
        }
    }

    @Override
    public final void clearFocus() {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow: clearFocus(), isOffscreenInstance "+isOffscreenInstance);
        }
        if(!isOffscreenInstance) {
            OSXUtil.RunOnMainThread(false, new Runnable() {
                @Override
                public void run() {
                    resignFocus0(getWindowHandle());
                } } );
        } else {
            focusChanged(false, false);
        }
    }

    private boolean useParent(NativeWindow parent) { return null != parent && 0 != parent.getWindowHandle(); }

    @Override
    protected final int getPixelScaleX() {
        return 1; // FIXME HiDPI: Use pixelScale
    }

    @Override
    protected final int getPixelScaleY() {
        return 1; // FIXME HiDPI: Use pixelScale
    }

    @Override
    public void updatePosition(int x, int y) {
        final long handle = getWindowHandle();
        if( 0 != handle && !isOffscreenInstance ) {
            final NativeWindow parent = getParent();
            final boolean useParent = useParent(parent);
            final int pX=parent.getX(), pY=parent.getY();
            final Point p0S = getLocationOnScreenImpl(x, y, parent, useParent);
            if(DEBUG_IMPLEMENTATION) {
                System.err.println("MacWindow: updatePosition() parent["+useParent+" "+pX+"/"+pY+"] "+x+"/"+y+" ->  "+x+"/"+y+" rel-client-pos, "+p0S+" screen-client-pos");
            }
            OSXUtil.RunOnMainThread(false, new Runnable() {
                @Override
                public void run() {
                    setWindowClientTopLeftPoint0(handle, p0S.getX(), p0S.getY(), isVisible());
                } } );
            // no native event (fullscreen, some reparenting)
            positionChanged(true, x, y);
        }
    }

    @Override
    protected void sizeChanged(boolean defer, int newWidth, int newHeight, boolean force) {
        final long handle = getWindowHandle();
        if( 0 != handle && !isOffscreenInstance ) {
            final NativeWindow parent = getParent();
            final boolean useParent = useParent(parent);
            if( useParent && ( getWindowWidth() != newWidth || getWindowHeight() != newHeight ) ) {
                final int x=getX(), y=getY();
                final Point p0S = getLocationOnScreenImpl(x, y, parent, useParent);
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("MacWindow: sizeChanged() parent["+useParent+" "+x+"/"+y+"] "+getX()+"/"+getY()+" "+newWidth+"x"+newHeight+" ->  "+p0S+" screen-client-pos");
                }
                OSXUtil.RunOnMainThread(false, new Runnable() {
                    @Override
                    public void run() {
                        setWindowClientTopLeftPoint0(getWindowHandle(), p0S.getX(), p0S.getY(), isVisible());
                    } } );
            }
        }
        super.sizeChanged(defer, newWidth, newHeight, force);
    }

    @Override
    protected boolean reconfigureWindowImpl(final int x, final int y, final int width, final int height, int flags) {
        final boolean _isOffscreenInstance = isOffscreenInstance(this, this.getParent());
        isOffscreenInstance = 0 != sscSurfaceHandle || _isOffscreenInstance;
        final PointImmutable pClientLevelOnSreen;
        if( isOffscreenInstance ) {
            pClientLevelOnSreen = new Point(0, 0);
        } else  {
            final NativeWindow parent = getParent();
            final boolean useParent = useParent(parent);
            if( useParent ) {
                pClientLevelOnSreen = getLocationOnScreenImpl(x, y, parent, useParent);
            } else {
                pClientLevelOnSreen = new Point(x, y);
            }
        }

        if(DEBUG_IMPLEMENTATION) {
            final AbstractGraphicsConfiguration cWinCfg = this.getGraphicsConfiguration();
            final NativeWindow pWin = getParent();
            final AbstractGraphicsConfiguration pWinCfg = null != pWin ? pWin.getGraphicsConfiguration() : null;
            System.err.println("MacWindow reconfig.0: "+x+"/"+y+" -> clientPos "+pClientLevelOnSreen+" - "+width+"x"+height+
                               ",\n\t parent type "+(null != pWin ? pWin.getClass().getName() : null)+
                               ",\n\t   this-chosenCaps "+(null != cWinCfg ? cWinCfg.getChosenCapabilities() : null)+
                               ",\n\t parent-chosenCaps "+(null != pWinCfg ? pWinCfg.getChosenCapabilities() : null)+
                               ", isOffscreenInstance(sscSurfaceHandle "+toHexString(sscSurfaceHandle)+
                               ", ioi: "+_isOffscreenInstance+
                               ") -> "+isOffscreenInstance+
                               "\n\t, "+getReconfigureFlagsAsString(null, flags));
            // Thread.dumpStack();
        }

        final boolean setVisible = 0 != ( FLAG_IS_VISIBLE & flags);

        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) && !setVisible ) {
            if ( !isOffscreenInstance ) {
                OSXUtil.RunOnMainThread(false, new Runnable() {
                    @Override
                    public void run() {
                        orderOut0(getWindowHandle());
                        visibleChanged(true, false);
                    } } );
            } else {
                visibleChanged(true, false);
            }
        }
        if( 0 == getWindowHandle() && setVisible ||
            0 != ( FLAG_CHANGE_DECORATION & flags) ||
            0 != ( FLAG_CHANGE_PARENTING & flags) ||
            0 != ( FLAG_CHANGE_FULLSCREEN & flags) ) {
            if(isOffscreenInstance) {
                createWindow(true, 0 != getWindowHandle(), pClientLevelOnSreen, 64, 64, false, setVisible, false);
            } else {
                createWindow(false, 0 != getWindowHandle(), pClientLevelOnSreen, width, height,
                                    0 != ( FLAG_IS_FULLSCREEN & flags), setVisible, 0 != ( FLAG_IS_ALWAYSONTOP & flags));
            }
        } else {
            if( width>0 && height>0 ) {
                if( !isOffscreenInstance ) {
                    OSXUtil.RunOnMainThread(false, new Runnable() {
                        @Override
                        public void run() {
                            setWindowClientTopLeftPointAndSize0(getWindowHandle(), pClientLevelOnSreen.getX(), pClientLevelOnSreen.getY(), width, height, setVisible);
                        } } );
                } // else offscreen size is realized via recreation
                // no native event (fullscreen, some reparenting)
                positionChanged(true,  x, y);
                sizeChanged(true, width, height, false);
            }
            if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) && setVisible ) {
                if( !isOffscreenInstance ) {
                    OSXUtil.RunOnMainThread(false, new Runnable() {
                        @Override
                        public void run() {
                            orderFront0(getWindowHandle());
                            visibleChanged(true, true);
                        } } );
                } else {
                    visibleChanged(true, true);
                }
            }
            if( !isOffscreenInstance ) {
                setAlwaysOnTop0(getWindowHandle(), 0 != ( FLAG_IS_ALWAYSONTOP & flags));
            }
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow reconfig.X: clientPos "+pClientLevelOnSreen+", "+width+"x"+height+" -> clientPos "+getLocationOnScreenImpl(0, 0)+", insets: "+getInsets());
        }
        return true;
    }

    @Override
    protected Point getLocationOnScreenImpl(int x, int y) {
        final NativeWindow parent = getParent();
        final boolean useParent = useParent(parent);
        return getLocationOnScreenImpl(x, y, parent, useParent);
    }

    private Point getLocationOnScreenImpl(final int x, final int y, final NativeWindow parent, final boolean useParent) {
        if( !useParent && !isOffscreenInstance && 0 != surfaceHandle) {
            return OSXUtil.GetLocationOnScreen(surfaceHandle, true, x, y);
        }

        final Point p = new Point(x, y);
        if( useParent ) {
            p.translate( parent.getLocationOnScreen(null) );
        }
        return p;
    }

    @Override
    protected void updateInsetsImpl(Insets insets) {
        // nop - using event driven insetsChange(..)
    }

    /** Callback for native screen position change event of the client area. */
    protected void screenPositionChanged(boolean defer, int newX, int newY) {
        // passed coordinates are in screen position of the client area
        if(getWindowHandle()!=0) {
            final NativeWindow parent = getParent();
            if( null == parent || isOffscreenInstance ) {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("MacWindow.positionChanged.0 (Screen Pos - TOP): ("+getThreadName()+"): (defer: "+defer+") "+getX()+"/"+getY()+" -> "+newX+"/"+newY);
                }
                positionChanged(defer, newX, newY);
            } else {
                // screen position -> rel child window position
                Point absPos = new Point(newX, newY);
                Point parentOnScreen = parent.getLocationOnScreen(null);
                absPos.translate( parentOnScreen.scale(-1, -1) );
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("MacWindow.positionChanged.1 (Screen Pos - CHILD): ("+getThreadName()+"): (defer: "+defer+") "+getX()+"/"+getY()+" -> absPos "+newX+"/"+newY+", parentOnScreen "+parentOnScreen+" -> "+absPos);
                }
                positionChanged(defer, absPos.getX(), absPos.getY());
            }
        } else if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow.positionChanged.2 (Screen Pos - IGN): ("+getThreadName()+"): (defer: "+defer+") "+getX()+"/"+getY()+" -> "+newX+"/"+newY);
        }
    }

    @Override
    protected void setPointerIconImpl(final PointerIconImpl pi) {
        if( !isOffscreenInstance ) {
            final long piHandle = null != pi ? pi.validatedHandle() : 0;
            OSXUtil.RunOnMainThread(true, new Runnable() { // waitUntildone due to PointerIconImpl's Lifecycle !
                @Override
                public void run() {
                    setPointerIcon0(getWindowHandle(), piHandle);
                } } );
        }
    }

    @Override
    protected boolean setPointerVisibleImpl(final boolean pointerVisible) {
        if( !isOffscreenInstance ) {
            OSXUtil.RunOnMainThread(false, new Runnable() {
                @Override
                public void run() {
                    setPointerVisible0(getWindowHandle(), hasFocus(), pointerVisible);
                } } );
            return true;
        }
        return false;
    }

    @Override
    protected boolean confinePointerImpl(final boolean confine) {
        if( !isOffscreenInstance ) {
            confinePointer0(getWindowHandle(), confine);
            return true;
        } // else may need offscreen solution ? FIXME
        return false;
    }

    @Override
    protected void warpPointerImpl(final int x, final int y) {
        if( !isOffscreenInstance ) {
            warpPointer0(getWindowHandle(), x, y);
        } // else may need offscreen solution ? FIXME
    }

    @Override
    public final void sendKeyEvent(short eventType, int modifiers, short keyCode, short keySym, char keyChar) {
        throw new InternalError("XXX: Adapt Java Code to Native Code Changes");
    }

    @Override
    public final void enqueueKeyEvent(boolean wait, short eventType, int modifiers, short _keyCode, short _keySym, char keyChar) {
        throw new InternalError("XXX: Adapt Java Code to Native Code Changes");
    }

    protected final void enqueueKeyEvent(boolean wait, short eventType, int modifiers, short _keyCode, char keyChar, char keySymChar) {
        // Note that we send the key char for the key code on this
        // platform -- we do not get any useful key codes out of the system
        final short keyCode = MacKeyUtil.validateKeyCode(_keyCode, keyChar);
        final short keySym;
        {
            short _keySym = KeyEvent.NULL_CHAR != keySymChar ? KeyEvent.utf16ToVKey(keySymChar) : KeyEvent.VK_UNDEFINED;
            keySym = KeyEvent.VK_UNDEFINED != _keySym ? _keySym : keyCode;
        }
        /**
        {
            final boolean isModifierKeyCode = KeyEvent.isModifierKey(keyCode);
            System.err.println("*** handleKeyEvent: event "+KeyEvent.getEventTypeString(eventType)+
                               ", keyCode 0x"+Integer.toHexString(_keyCode)+" -> 0x"+Integer.toHexString(keyCode)+
                               ", keySymChar '"+keySymChar+"', 0x"+Integer.toHexString(keySymChar)+" -> 0x"+Integer.toHexString(keySym)+
                               ", mods "+toHexString(modifiers)+
                               ", was: pressed "+isKeyPressed(keyCode)+", isModifierKeyCode "+isModifierKeyCode+
                               ", nativeValid "+isNativeValid()+", isOffscreen "+isOffscreenInstance);
        } */

        // OSX delivery order is PRESSED (t0), RELEASED (t1) and TYPED (t2) -> NEWT order: PRESSED (t0) and RELEASED (t1)
        // Auto-Repeat: OSX delivers only PRESSED, inject auto-repeat RELEASE key _before_ PRESSED
        switch(eventType) {
            case KeyEvent.EVENT_KEY_RELEASED:
                if( isKeyCodeTracked(keyCode) ) {
                    setKeyPressed(keyCode, false);
                }
                break;
            case KeyEvent.EVENT_KEY_PRESSED:
                if( isKeyCodeTracked(keyCode) ) {
                    if( setKeyPressed(keyCode, true) ) {
                        // key was already pressed
                        modifiers |= InputEvent.AUTOREPEAT_MASK;
                        super.enqueueKeyEvent(wait, KeyEvent.EVENT_KEY_RELEASED, modifiers, keyCode, keySym, keyChar); // RELEASED
                    }
                }
                break;
        }
        super.enqueueKeyEvent(wait, eventType, modifiers, keyCode, keySym, keyChar);
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    private void createWindow(final boolean offscreenInstance, final boolean recreate,
                              final PointImmutable pS, final int width, final int height,
                              final boolean fullscreen, final boolean visible, final boolean alwaysOnTop) {

        final long parentWinHandle = getParentWindowHandle();
        final long preWinHandle = getWindowHandle();

        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow.createWindow on thread "+Thread.currentThread().getName()+
                               ": offscreen "+offscreenInstance+", recreate "+recreate+
                               ", pS "+pS+", "+width+"x"+height+", fullscreen "+fullscreen+", visible "+visible+
                               ", alwaysOnTop "+alwaysOnTop+", preWinHandle "+toHexString(preWinHandle)+", parentWin "+toHexString(parentWinHandle)+
                               ", surfaceHandle "+toHexString(surfaceHandle));
            // Thread.dumpStack();
        }

        try {
            if( 0 != preWinHandle ) {
                setWindowHandle(0);
                if( 0 == surfaceHandle ) {
                    throw new NativeWindowException("Internal Error - create w/ window, but no Newt NSView");
                }
                OSXUtil.RunOnMainThread(false, new Runnable() {
                    @Override
                    public void run() {
                        changeContentView0(parentWinHandle, preWinHandle, 0);
                        close0( preWinHandle );
                    } } );
            } else {
                if( 0 != surfaceHandle ) {
                    throw new NativeWindowException("Internal Error - create w/o window, but has Newt NSView");
                }
                surfaceHandle = createView0(pS.getX(), pS.getY(), width, height, fullscreen);
                if( 0 == surfaceHandle ) {
                    throw new NativeWindowException("Could not create native view "+Thread.currentThread().getName()+" "+this);
                }
            }

            final long newWin = createWindow0( pS.getX(), pS.getY(), width, height, fullscreen,
                                               ( isUndecorated() || offscreenInstance ) ? NSBorderlessWindowMask :
                                               NSTitledWindowMask|NSClosableWindowMask|NSMiniaturizableWindowMask|NSResizableWindowMask,
                                               NSBackingStoreBuffered, surfaceHandle);
            if ( newWin == 0 ) {
                throw new NativeWindowException("Could not create native window "+Thread.currentThread().getName()+" "+this);
            }
            setWindowHandle( newWin );

            final boolean isOpaque = getGraphicsConfiguration().getChosenCapabilities().isBackgroundOpaque() && !offscreenInstance;
            // Blocking initialization on main-thread!
            OSXUtil.RunOnMainThread(true, new Runnable() {
                @Override
                public void run() {
                    initWindow0( parentWinHandle, newWin, pS.getX(), pS.getY(), width, height,
                                 isOpaque, visible && !offscreenInstance, surfaceHandle);
                    if( offscreenInstance ) {
                        orderOut0(0!=parentWinHandle ? parentWinHandle : newWin);
                    } else {
                        setTitle0(newWin, getTitle());
                        setAlwaysOnTop0(getWindowHandle(), alwaysOnTop);
                    }
                    visibleChanged(true, visible);
                } } );
        } catch (Exception ie) {
            ie.printStackTrace();
        }
    }

    protected static native boolean initIDs0();
    private native long createView0(int x, int y, int w, int h, boolean fullscreen);
    private native long createWindow0(int x, int y, int w, int h, boolean fullscreen, int windowStyle, int backingStoreType, long view);
    /** Must be called on Main-Thread */
    private native void initWindow0(long parentWindow, long window, int x, int y, int w, int h, boolean opaque, boolean visible, long view);
    private native boolean lockSurface0(long window, long view);
    private native boolean unlockSurface0(long window, long view);
    /** Must be called on Main-Thread */
    private native void requestFocus0(long window, boolean force);
    /** Must be called on Main-Thread */
    private native void resignFocus0(long window);
    /** Must be called on Main-Thread. In case this is a child window and parent is still visible, orderBack(..) is issued instead of orderOut(). */
    private native void orderOut0(long window);
    /** Must be called on Main-Thread */
    private native void orderFront0(long window);
    /** Must be called on Main-Thread */
    private native void close0(long window);
    /** Must be called on Main-Thread */
    private native void setTitle0(long window, String title);
    private native long contentView0(long window);
    /** Must be called on Main-Thread */
    private native void changeContentView0(long parentWindowOrView, long window, long view);
    /** Must be called on Main-Thread */
    private native void setWindowClientTopLeftPointAndSize0(long window, int x, int y, int w, int h, boolean display);
    /** Must be called on Main-Thread */
    private native void setWindowClientTopLeftPoint0(long window, int x, int y, boolean display);
    /** Must be called on Main-Thread */
    private native void setAlwaysOnTop0(long window, boolean atop);
    private static native Object getLocationOnScreen0(long windowHandle, int src_x, int src_y);
    private static native void setPointerIcon0(long windowHandle, long handle);
    private static native void setPointerVisible0(long windowHandle, boolean hasFocus, boolean visible);
    private static native void confinePointer0(long windowHandle, boolean confine);
    private static native void warpPointer0(long windowHandle, int x, int y);

    // Window styles
    private static final int NSBorderlessWindowMask     = 0;
    private static final int NSTitledWindowMask         = 1 << 0;
    private static final int NSClosableWindowMask       = 1 << 1;
    private static final int NSMiniaturizableWindowMask = 1 << 2;
    private static final int NSResizableWindowMask      = 1 << 3;

    // Window backing store types
    private static final int NSBackingStoreRetained     = 0;
    private static final int NSBackingStoreNonretained  = 1;
    private static final int NSBackingStoreBuffered     = 2;

    private volatile long surfaceHandle = 0;
    private long sscSurfaceHandle = 0;
    private boolean isOffscreenInstance = false;

}
