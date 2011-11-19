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

import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.SurfaceChangeable;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.PointImmutable;

import jogamp.newt.WindowImpl;
import jogamp.newt.driver.DriverClearFocus;

import com.jogamp.newt.event.KeyEvent;

public class MacWindow extends WindowImpl implements SurfaceChangeable, DriverClearFocus {
    
    static {
        MacDisplay.initSingleton();
    }

    public MacWindow() {
    }
    
    @Override
    protected void createNativeImpl() {
        config = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice()).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, getScreen().getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        reconfigureWindowImpl(x, y, width, height, getReconfigureFlags(FLAG_CHANGE_VISIBILITY, true));        
        if (0 == getWindowHandle()) {
            throw new NativeWindowException("Error creating window");
        }
    }

    @Override
    protected void closeNativeImpl() {
        try {
            if(DEBUG_IMPLEMENTATION) { System.err.println("MacWindow.CloseAction "+Thread.currentThread().getName()); }
            if (getWindowHandle() != 0) {
                close0(getWindowHandle());
            }
        } catch (Throwable t) {
            if(DEBUG_IMPLEMENTATION) { 
                Exception e = new Exception("Warning: closeNative failed - "+Thread.currentThread().getName(), t);
                e.printStackTrace();
            }
        } finally {
            setWindowHandle(0);
        }
    }
    
    @Override
    protected int lockSurfaceImpl() {
        if(!isOffscreenInstance) {
            return lockSurface0(getWindowHandle()) ? LOCK_SUCCESS : LOCK_SURFACE_NOT_READY;
        }
        return LOCK_SUCCESS;
    }

    @Override
    protected void unlockSurfaceImpl() {
        if(!isOffscreenInstance) {
            unlockSurface0(getWindowHandle());
        }
    }
    
    @Override
    public final long getSurfaceHandle() {
        return 0 != sscSurfaceHandle ? sscSurfaceHandle : surfaceHandle;
    }

    public void setSurfaceHandle(long surfaceHandle) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow.setSurfaceHandle(): 0x"+Long.toHexString(surfaceHandle));
        }
        sscSurfaceHandle = surfaceHandle;
        if (isNativeValid()) {
            if (0 != sscSurfaceHandle) {
                orderOut0( 0!=getParentWindowHandle() ? getParentWindowHandle() : getWindowHandle() );
            } /** this is done by recreation! 
              else if (isVisible()){
                orderFront0( 0!=getParentWindowHandle() ? getParentWindowHandle() : getWindowHandle() );
            } */
        }        
    }

    public void surfaceSizeChanged(int width, int height) {
        sizeChanged(false, width, height, false);
    }
    
    @Override
    protected void setTitleImpl(final String title) {
        setTitle0(getWindowHandle(), title);
    }

    protected void requestFocusImpl(boolean force) {
        if(!isOffscreenInstance) {
            requestFocus0(getWindowHandle(), force);
        } else {
            focusChanged(false, true);
        }
    }
        
    public final void clearFocus() {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow: clearFocus() - requestFocusParent, isOffscreenInstance "+isOffscreenInstance);
        }
        if(!isOffscreenInstance) {
            requestFocusParent0(getWindowHandle());
        } else {
            focusChanged(false, false);
        }
    }
    
    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {
        final PointImmutable pS = position2TopLevel(new Point(x, y));
        isOffscreenInstance = 0 != sscSurfaceHandle || isOffscreenInstance(this, this.getParent());
        
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow reconfig: "+x+"/"+y+" -> "+pS+" - "+width+"x"+height+
                               ", offscreenInstance "+isOffscreenInstance+
                               ", "+getReconfigureFlagsAsString(null, flags));
        }
        
        if( getWindowHandle() == 0 ) {
            if( 0 != ( FLAG_IS_VISIBLE & flags) ) {
                createWindow(isOffscreenInstance, false, pS, width, height, 0 != ( FLAG_IS_FULLSCREEN & flags));
                // no native event ..
                visibleChanged(true, true); 
            } /* else { ?? } */
        } else {
            if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) && 0 == ( FLAG_IS_VISIBLE & flags) ) {
                if ( !isOffscreenInstance ) {
                    orderOut0(getWindowHandle());
                }
                // no native event ..
                visibleChanged(true, false); 
            }
            if( 0 != ( FLAG_CHANGE_DECORATION & flags) ||
                0 != ( FLAG_CHANGE_PARENTING & flags) ||
                0 != ( FLAG_CHANGE_FULLSCREEN & flags) ) {
                createWindow(isOffscreenInstance, true, pS, width, height, 0 != ( FLAG_IS_FULLSCREEN & flags));
                if(isVisible()) { flags |= FLAG_CHANGE_VISIBILITY; } 
            }
            if(x>=0 && y>=0) {
                if( !isOffscreenInstance ) {                
                    setFrameTopLeftPoint0(getParentWindowHandle(), getWindowHandle(), pS.getX(), pS.getY());
                } // else no offscreen position
                // no native event (fullscreen, some reparenting)
                positionChanged(true, getLocationOnScreenImpl(0, 0)); // incl. validation
            }
            if(width>0 && height>0) {
                if( !isOffscreenInstance ) {                
                    setContentSize0(getWindowHandle(), width, height);
                } // else offscreen size is realized via recreation
                // no native event (fullscreen, some reparenting)
                sizeChanged(true, width, height, false); // incl. validation (incl. repositioning)
            }
            if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) && 0 != ( FLAG_IS_VISIBLE & flags) ) {
                if( !isOffscreenInstance ) {                
                    orderFront0(getWindowHandle());
                }
                // no native event ..
                visibleChanged(true, true);
            } 
            if( !isOffscreenInstance ) {                
                setAlwaysOnTop0(getWindowHandle(), 0 != ( FLAG_IS_ALWAYSONTOP & flags));
            }
        }
        return true;
    }

    protected Point getLocationOnScreenImpl(int x, int y) {
        return position2TopLevel(new Point(x, y)); // allows offscreen ..        
        // return (Point) getLocationOnScreen0(getWindowHandle(), x, y);
    }
    
    protected void updateInsetsImpl(Insets insets) {
        // nop - using event driven insetsChange(..)
    }
        
    @Override
    protected void sizeChanged(boolean defer, int newWidth, int newHeight, boolean force) {
        if(width != newWidth || height != newHeight) {
            final Point p0S = position2TopLevel(new Point(x, y));            
            setFrameTopLeftPoint0(getParentWindowHandle(), getWindowHandle(), p0S.getX(), p0S.getY());               
        }
        super.sizeChanged(defer, newWidth, newHeight, force);
    }
    
    @Override
    protected void positionChanged(boolean defer, int newX, int newY) {
        positionChanged(defer, new Point(newX, newY));
    }
    
    @Override
    protected boolean setPointerVisibleImpl(final boolean pointerVisible) {
        if( !isOffscreenInstance ) {                
            return setPointerVisible0(getWindowHandle(), pointerVisible);
        } // else may need offscreen solution ? FIXME
        return false;
    }

    @Override
    protected boolean confinePointerImpl(final boolean confine) {
        if( !isOffscreenInstance ) {                
            return confinePointer0(getWindowHandle(), confine);
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
    public void sendKeyEvent(int eventType, int modifiers, int keyCode, char keyChar) {
        // Note that we send the key char for the key code on this
        // platform -- we do not get any useful key codes out of the system
        final int keyCode2 = MacKeyUtil.validateKeyCode(keyCode, keyChar);
        if(DEBUG_IMPLEMENTATION) System.err.println("MacWindow.sendKeyEvent "+Thread.currentThread().getName()+" char: 0x"+Integer.toHexString(keyChar)+", code 0x"+Integer.toHexString(keyCode)+" -> 0x"+Integer.toHexString(keyCode2));
        // only deliver keyChar on key Typed events, harmonizing platform behavior
        keyChar = KeyEvent.EVENT_KEY_TYPED == eventType ? keyChar : (char)-1;
        super.sendKeyEvent(eventType, modifiers, keyCode2, keyChar);        
    }
    
    @Override
    public void enqueueKeyEvent(boolean wait, int eventType, int modifiers, int keyCode, char keyChar) {
        // Note that we send the key char for the key code on this
        // platform -- we do not get any useful key codes out of the system
        final int keyCode2 = MacKeyUtil.validateKeyCode(keyCode, keyChar);
        if(DEBUG_IMPLEMENTATION) System.err.println("MacWindow.enqueueKeyEvent "+Thread.currentThread().getName()+" char: 0x"+Integer.toHexString(keyChar)+", code 0x"+Integer.toHexString(keyCode)+" -> 0x"+Integer.toHexString(keyCode2));
        // only deliver keyChar on key Typed events, harmonizing platform behavior
        keyChar = KeyEvent.EVENT_KEY_TYPED == eventType ? keyChar : (char)-1;
        super.enqueueKeyEvent(wait, eventType, modifiers, keyCode2, keyChar);
    }

    //----------------------------------------------------------------------
    // Internals only
    //    
    
    private void createWindow(final boolean offscreenInstance, final boolean recreate, 
                              final PointImmutable pS, final int width, final int height, 
                              final boolean fullscreen) {

        if(0!=getWindowHandle() && !recreate) {
            return;
        }

        try {
            if(0!=getWindowHandle()) {
                // save the view .. close the window
                surfaceHandle = changeContentView0(getParentWindowHandle(), getWindowHandle(), 0);
                if(recreate && 0==surfaceHandle) {
                    throw new NativeWindowException("Internal Error - recreate, window but no view");
                }
                orderOut0(getWindowHandle());
                close0(getWindowHandle());
                setWindowHandle(0);
            } else {
                surfaceHandle = 0;
            }
            setWindowHandle(createWindow0(getParentWindowHandle(),
                                 pS.getX(), pS.getY(), width, height,
                                 (config.getChosenCapabilities().isBackgroundOpaque() && !offscreenInstance),
                                 fullscreen,
                                 ((isUndecorated() || offscreenInstance) ?
                                   NSBorderlessWindowMask :
                                   NSTitledWindowMask|NSClosableWindowMask|NSMiniaturizableWindowMask|NSResizableWindowMask),
                                 NSBackingStoreBuffered, 
                                 getScreen().getIndex(), surfaceHandle));
            if (getWindowHandle() == 0) {
                throw new NativeWindowException("Could create native window "+Thread.currentThread().getName()+" "+this);
            }
            surfaceHandle = contentView0(getWindowHandle());
            if( offscreenInstance ) {
                orderOut0(0!=getParentWindowHandle() ? getParentWindowHandle() : getWindowHandle());
            } else {
                setTitle0(getWindowHandle(), getTitle());
                // need to revalidate real position
                positionChanged(true, getLocationOnScreenImpl(0, 0)); // incl. validation
            }
        } catch (Exception ie) {
            ie.printStackTrace();
        }
    }
    
    private void positionChanged(boolean defer, Point absPos) {
        if(getWindowHandle()!=0) {
            position2ClientSpace(absPos);
            super.positionChanged(defer, absPos.getX(), absPos.getY());
        }
    }
    
    private Point position2ClientSpace(Point absPos) {
        final NativeWindow parent = getParent();
        if(null != parent) {
            return absPos.translate( parent.getLocationOnScreen(null).scale(-1, -1) );
        }
        return absPos;
    }
    
    private Point position2TopLevel(Point clientPos) {        
        if(0<=clientPos.getX() && 0<=clientPos.getY()) {
            final InsetsImmutable _insets = getInsets(); // zero if undecorated
            // client position -> top-level window position
            clientPos.setX(clientPos.getX() - _insets.getLeftWidth()) ;
            clientPos.setY(clientPos.getY() - _insets.getTopHeight()) ;
        }
        // min val is 0
        clientPos.setX(Math.max(clientPos.getX(),  0));
        clientPos.setY(Math.max(clientPos.getY(),  0));
        // On MacOSX the absolute position is required to position 
        // a window - even a child window!
        final NativeWindow parent = getParent();
        if( null != parent && 0 != parent.getWindowHandle() ) {
            clientPos.translate(parent.getLocationOnScreen(null));
        }
        return clientPos;
    }
    
    protected static native boolean initIDs0();
    private native long createWindow0(long parentWindowHandle, int x, int y, int w, int h,
                                     boolean opaque, boolean fullscreen, int windowStyle,
                                     int backingStoreType,
                                     int screen_idx, long view);
    private native boolean lockSurface0(long window);
    private native void unlockSurface0(long window);
    private native void requestFocus0(long window, boolean force);
    private native void requestFocusParent0(long window);
    /** in case of a child window, it actually only issues orderBack(..) */
    private native void orderOut0(long window);
    private native void orderFront0(long window);
    private native void close0(long window);
    private native void setTitle0(long window, String title);
    private native long contentView0(long window);
    private native long changeContentView0(long parentWindowOrViewHandle, long window, long view);
    private native void setContentSize0(long window, int w, int h);
    private native void setFrameTopLeftPoint0(long parentWindowHandle, long window, int x, int y);
    private native void setAlwaysOnTop0(long window, boolean atop);
    private static native Object getLocationOnScreen0(long windowHandle, int src_x, int src_y);
    private static native boolean setPointerVisible0(long windowHandle, boolean visible);
    private static native boolean confinePointer0(long windowHandle, boolean confine);
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

    private volatile long surfaceHandle;
    private long sscSurfaceHandle;
    private boolean isOffscreenInstance;
    
}
