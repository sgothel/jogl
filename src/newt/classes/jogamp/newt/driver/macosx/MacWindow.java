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

import com.jogamp.newt.event.KeyEvent;

public class MacWindow extends WindowImpl implements SurfaceChangeable {
    
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

    // Key constants handled differently on Mac OS X than other platforms
    private static final int NSUpArrowFunctionKey        = 0xF700;
    private static final int NSDownArrowFunctionKey      = 0xF701;
    private static final int NSLeftArrowFunctionKey      = 0xF702;
    private static final int NSRightArrowFunctionKey     = 0xF703;
    private static final int NSF1FunctionKey             = 0xF704;
    private static final int NSF2FunctionKey             = 0xF705;
    private static final int NSF3FunctionKey             = 0xF706;
    private static final int NSF4FunctionKey             = 0xF707;
    private static final int NSF5FunctionKey             = 0xF708;
    private static final int NSF6FunctionKey             = 0xF709;
    private static final int NSF7FunctionKey             = 0xF70A;
    private static final int NSF8FunctionKey             = 0xF70B;
    private static final int NSF9FunctionKey             = 0xF70C;
    private static final int NSF10FunctionKey            = 0xF70D;
    private static final int NSF11FunctionKey            = 0xF70E;
    private static final int NSF12FunctionKey            = 0xF70F;
    private static final int NSF13FunctionKey            = 0xF710;
    private static final int NSF14FunctionKey            = 0xF711;
    private static final int NSF15FunctionKey            = 0xF712;
    private static final int NSF16FunctionKey            = 0xF713;
    private static final int NSF17FunctionKey            = 0xF714;
    private static final int NSF18FunctionKey            = 0xF715;
    private static final int NSF19FunctionKey            = 0xF716;
    private static final int NSF20FunctionKey            = 0xF717;
    private static final int NSF21FunctionKey            = 0xF718;
    private static final int NSF22FunctionKey            = 0xF719;
    private static final int NSF23FunctionKey            = 0xF71A;
    private static final int NSF24FunctionKey            = 0xF71B;
    private static final int NSF25FunctionKey            = 0xF71C;
    private static final int NSF26FunctionKey            = 0xF71D;
    private static final int NSF27FunctionKey            = 0xF71E;
    private static final int NSF28FunctionKey            = 0xF71F;
    private static final int NSF29FunctionKey            = 0xF720;
    private static final int NSF30FunctionKey            = 0xF721;
    private static final int NSF31FunctionKey            = 0xF722;
    private static final int NSF32FunctionKey            = 0xF723;
    private static final int NSF33FunctionKey            = 0xF724;
    private static final int NSF34FunctionKey            = 0xF725;
    private static final int NSF35FunctionKey            = 0xF726;
    private static final int NSInsertFunctionKey         = 0xF727;
    private static final int NSDeleteFunctionKey         = 0xF728;
    private static final int NSHomeFunctionKey           = 0xF729;
    private static final int NSBeginFunctionKey          = 0xF72A;
    private static final int NSEndFunctionKey            = 0xF72B;
    private static final int NSPageUpFunctionKey         = 0xF72C;
    private static final int NSPageDownFunctionKey       = 0xF72D;
    private static final int NSPrintScreenFunctionKey    = 0xF72E;
    private static final int NSScrollLockFunctionKey     = 0xF72F;
    private static final int NSPauseFunctionKey          = 0xF730;
    private static final int NSSysReqFunctionKey         = 0xF731;
    private static final int NSBreakFunctionKey          = 0xF732;
    private static final int NSResetFunctionKey          = 0xF733;
    private static final int NSStopFunctionKey           = 0xF734;
    private static final int NSMenuFunctionKey           = 0xF735;
    private static final int NSUserFunctionKey           = 0xF736;
    private static final int NSSystemFunctionKey         = 0xF737;
    private static final int NSPrintFunctionKey          = 0xF738;
    private static final int NSClearLineFunctionKey      = 0xF739;
    private static final int NSClearDisplayFunctionKey   = 0xF73A;
    private static final int NSInsertLineFunctionKey     = 0xF73B;
    private static final int NSDeleteLineFunctionKey     = 0xF73C;
    private static final int NSInsertCharFunctionKey     = 0xF73D;
    private static final int NSDeleteCharFunctionKey     = 0xF73E;
    private static final int NSPrevFunctionKey           = 0xF73F;
    private static final int NSNextFunctionKey           = 0xF740;
    private static final int NSSelectFunctionKey         = 0xF741;
    private static final int NSExecuteFunctionKey        = 0xF742;
    private static final int NSUndoFunctionKey           = 0xF743;
    private static final int NSRedoFunctionKey           = 0xF744;
    private static final int NSFindFunctionKey           = 0xF745;
    private static final int NSHelpFunctionKey           = 0xF746;
    private static final int NSModeSwitchFunctionKey     = 0xF747;

    private volatile long surfaceHandle;
    private long sscSurfaceHandle;

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
        return lockSurface0(getWindowHandle()) ? LOCK_SUCCESS : LOCK_SURFACE_NOT_READY;
    }

    @Override
    protected void unlockSurfaceImpl() {
        unlockSurface0(getWindowHandle());
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
        requestFocus0(getWindowHandle(), force);
    }
    
    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {
        final PointImmutable pS = position2TopLevel(new Point(x, y));
        final boolean offscreenInstance = 0 != sscSurfaceHandle || isOffscreenInstance(this, this.getParent());
        
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("MacWindow reconfig: "+x+"/"+y+" -> "+pS+" - "+width+"x"+height+
                               ", offscreenInstance "+offscreenInstance+
                               ", "+getReconfigureFlagsAsString(null, flags));
        }
        
        if( getWindowHandle() == 0 ) {
            if( 0 != ( FLAG_IS_VISIBLE & flags) ) {
                createWindow(offscreenInstance, false, pS, width, height, 0 != ( FLAG_IS_FULLSCREEN & flags));
                // no native event ..
                visibleChanged(true, true); 
            } /* else { ?? } */
        } else {
            if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) && 0 == ( FLAG_IS_VISIBLE & flags) ) {
                if ( !offscreenInstance ) {
                    orderOut0(getWindowHandle());
                }
                // no native event ..
                visibleChanged(true, false); 
            }
            if( 0 != ( FLAG_CHANGE_DECORATION & flags) ||
                0 != ( FLAG_CHANGE_PARENTING & flags) ||
                0 != ( FLAG_CHANGE_FULLSCREEN & flags) ) {
                createWindow(offscreenInstance, true, pS, width, height, 0 != ( FLAG_IS_FULLSCREEN & flags));
                if(isVisible()) { flags |= FLAG_CHANGE_VISIBILITY; } 
            }
            if(x>=0 && y>=0) {
                setFrameTopLeftPoint0(getParentWindowHandle(), getWindowHandle(), pS.getX(), pS.getY());               
                // no native event (fullscreen, some reparenting)
                positionChanged(true, getLocationOnScreenImpl(0, 0)); // incl. validation
            }
            if(width>0 && height>0) {
                setContentSize0(getWindowHandle(), width, height);
                // no native event (fullscreen, some reparenting)
                sizeChanged(true, width, height, false); // incl. validation (incl. repositioning)
            }
            if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) && 0 != ( FLAG_IS_VISIBLE & flags) ) {
                if( !offscreenInstance ) {                
                    orderFront0(getWindowHandle());
                }
                // no native event ..
                visibleChanged(true, true);
            } 
            if( !offscreenInstance ) {                
                setAlwaysOnTop0(getWindowHandle(), 0 != ( FLAG_IS_ALWAYSONTOP & flags));
            }
        }
        return true;
    }

    protected Point getLocationOnScreenImpl(int x, int y) {
        return (Point) getLocationOnScreen0(getWindowHandle(), x, y);
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
        return setPointerVisible0(getWindowHandle(), pointerVisible);
    }

    @Override
    protected boolean confinePointerImpl(final boolean confine) {
        return confinePointer0(getWindowHandle(), confine);
    }
    
    @Override
    protected void warpPointerImpl(final int x, final int y) {
        warpPointer0(getWindowHandle(), x, y);
    }
    
    @Override
    public void sendKeyEvent(int eventType, int modifiers, int keyCode, char keyChar) {
        // Note that we send the key char for the key code on this
        // platform -- we do not get any useful key codes out of the system
        final int keyCode2 = convertKeyChar(keyChar);
        if(DEBUG_IMPLEMENTATION) System.err.println("MacWindow.sendKeyEvent "+Thread.currentThread().getName()+" char: "+keyChar+", code 0x"+Integer.toHexString(keyCode)+" -> 0x"+Integer.toHexString(keyCode2));
        // only deliver keyChar on key Typed events, harmonizing platform behavior
        keyChar = KeyEvent.EVENT_KEY_TYPED == eventType ? keyChar : (char)-1;
        super.sendKeyEvent(eventType, modifiers, keyCode2, keyChar);        
    }
    
    @Override
    public void enqueueKeyEvent(boolean wait, int eventType, int modifiers, int keyCode, char keyChar) {
        // Note that we send the key char for the key code on this
        // platform -- we do not get any useful key codes out of the system
        final int keyCode2 = convertKeyChar(keyChar);
        if(DEBUG_IMPLEMENTATION) System.err.println("MacWindow.enqueueKeyEvent "+Thread.currentThread().getName()+" char: "+keyChar+", code 0x"+Integer.toHexString(keyCode)+" -> 0x"+Integer.toHexString(keyCode2));
        // only deliver keyChar on key Typed events, harmonizing platform behavior
        keyChar = KeyEvent.EVENT_KEY_TYPED == eventType ? keyChar : (char)-1;
        super.enqueueKeyEvent(wait, eventType, modifiers, keyCode2, keyChar);
    }

    //----------------------------------------------------------------------
    // Internals only
    //    
    
    private char convertKeyChar(char keyChar) {
        if (keyChar == '\r') {
            // Turn these into \n
            return '\n';
        }

        if (keyChar >= NSUpArrowFunctionKey && keyChar <= NSModeSwitchFunctionKey) {
            switch (keyChar) {
                case NSUpArrowFunctionKey:     return KeyEvent.VK_UP;
                case NSDownArrowFunctionKey:   return KeyEvent.VK_DOWN;
                case NSLeftArrowFunctionKey:   return KeyEvent.VK_LEFT;
                case NSRightArrowFunctionKey:  return KeyEvent.VK_RIGHT;
                case NSF1FunctionKey:          return KeyEvent.VK_F1;
                case NSF2FunctionKey:          return KeyEvent.VK_F2;
                case NSF3FunctionKey:          return KeyEvent.VK_F3;
                case NSF4FunctionKey:          return KeyEvent.VK_F4;
                case NSF5FunctionKey:          return KeyEvent.VK_F5;
                case NSF6FunctionKey:          return KeyEvent.VK_F6;
                case NSF7FunctionKey:          return KeyEvent.VK_F7;
                case NSF8FunctionKey:          return KeyEvent.VK_F8;
                case NSF9FunctionKey:          return KeyEvent.VK_F9;
                case NSF10FunctionKey:         return KeyEvent.VK_F10;
                case NSF11FunctionKey:         return KeyEvent.VK_F11;
                case NSF12FunctionKey:         return KeyEvent.VK_F12;
                case NSF13FunctionKey:         return KeyEvent.VK_F13;
                case NSF14FunctionKey:         return KeyEvent.VK_F14;
                case NSF15FunctionKey:         return KeyEvent.VK_F15;
                case NSF16FunctionKey:         return KeyEvent.VK_F16;
                case NSF17FunctionKey:         return KeyEvent.VK_F17;
                case NSF18FunctionKey:         return KeyEvent.VK_F18;
                case NSF19FunctionKey:         return KeyEvent.VK_F19;
                case NSF20FunctionKey:         return KeyEvent.VK_F20;
                case NSF21FunctionKey:         return KeyEvent.VK_F21;
                case NSF22FunctionKey:         return KeyEvent.VK_F22;
                case NSF23FunctionKey:         return KeyEvent.VK_F23;
                case NSF24FunctionKey:         return KeyEvent.VK_F24;
                case NSInsertFunctionKey:      return KeyEvent.VK_INSERT;
                case NSDeleteFunctionKey:      return KeyEvent.VK_DELETE;
                case NSHomeFunctionKey:        return KeyEvent.VK_HOME;
                case NSBeginFunctionKey:       return KeyEvent.VK_BEGIN;
                case NSEndFunctionKey:         return KeyEvent.VK_END;
                case NSPageUpFunctionKey:      return KeyEvent.VK_PAGE_UP;
                case NSPageDownFunctionKey:    return KeyEvent.VK_PAGE_DOWN;
                case NSPrintScreenFunctionKey: return KeyEvent.VK_PRINTSCREEN;
                case NSScrollLockFunctionKey:  return KeyEvent.VK_SCROLL_LOCK;
                case NSPauseFunctionKey:       return KeyEvent.VK_PAUSE;
                // Not handled:
                // NSSysReqFunctionKey
                // NSBreakFunctionKey
                // NSResetFunctionKey
                case NSStopFunctionKey:        return KeyEvent.VK_STOP;
                // Not handled:
                // NSMenuFunctionKey
                // NSUserFunctionKey
                // NSSystemFunctionKey
                // NSPrintFunctionKey
                // NSClearLineFunctionKey
                // NSClearDisplayFunctionKey
                // NSInsertLineFunctionKey
                // NSDeleteLineFunctionKey
                // NSInsertCharFunctionKey
                // NSDeleteCharFunctionKey
                // NSPrevFunctionKey
                // NSNextFunctionKey
                // NSSelectFunctionKey
                // NSExecuteFunctionKey
                // NSUndoFunctionKey
                // NSRedoFunctionKey
                // NSFindFunctionKey
                // NSHelpFunctionKey
                // NSModeSwitchFunctionKey
                default: break;
            }
        }

        // NSEvent's charactersIgnoringModifiers doesn't ignore the shift key
        if (keyChar >= 'a' && keyChar <= 'z') {
            return Character.toUpperCase(keyChar);
        }

        return keyChar;
    }

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
}
