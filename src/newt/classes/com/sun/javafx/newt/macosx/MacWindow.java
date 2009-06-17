/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.javafx.newt.macosx;

import javax.media.nativewindow.*;

import com.sun.javafx.newt.util.MainThread;
import com.sun.javafx.newt.*;
import com.sun.javafx.newt.impl.*;

public class MacWindow extends Window {
    
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
    // non fullscreen dimensions ..
    private int nfs_width, nfs_height, nfs_x, nfs_y;

    static {
        MacDisplay.initSingleton();
    }

    public MacWindow() {
    }
    
    protected void createNative(Capabilities caps) {
        config = GraphicsConfigurationFactory.getFactory(getScreen().getDisplay().getGraphicsDevice()).chooseGraphicsConfiguration(caps, null, getScreen().getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
    }

    class CloseAction implements Runnable {
        public void run() {
            nsViewLock.lock();
            try {
                if(DEBUG_IMPLEMENTATION) System.out.println("MacWindow.CloseAction "+Thread.currentThread().getName());
                if (windowHandle != 0) {
                    close0(windowHandle);
                    windowHandle = 0;
                }
            } finally {
                nsViewLock.unlock();
            }
        }
    }
    private CloseAction closeAction = new CloseAction();

    protected void closeNative() {
        MainThread.invoke(true, closeAction);
    }
    
    public long getWindowHandle() {
        nsViewLock.lock();
        try {
            return windowHandle;
        } finally {
            nsViewLock.unlock();
        }
    }

    public long getSurfaceHandle() {
        nsViewLock.lock();
        try {
            return surfaceHandle;
        } finally {
            nsViewLock.unlock();
        }
    }

    private ToolkitLock nsViewLock = new ToolkitLock() {
            private Thread owner;
            private int recursionCount;
            
            public synchronized void lock() {
                Thread cur = Thread.currentThread();
                if (owner == cur) {
                    ++recursionCount;
                    return;
                }
                while (owner != null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                owner = cur;
            }

            public synchronized void unlock() {
                if (owner != Thread.currentThread()) {
                    throw new RuntimeException("Not owner");
                }
                if (recursionCount > 0) {
                    --recursionCount;
                    return;
                }
                owner = null;
                notifyAll();
            }
        };

    public synchronized int lockSurface() throws NativeWindowException {
        nsViewLock.lock();
        return super.lockSurface();
    }

    public void unlockSurface() {
        super.unlockSurface();
        nsViewLock.unlock();
    }

    class VisibleAction implements Runnable {
        public void run() {
            nsViewLock.lock();
            try {
                if(DEBUG_IMPLEMENTATION) System.out.println("MacWindow.VisibleAction "+visible+" "+Thread.currentThread().getName());
                if (visible) {
                    createWindow(false);
                } else {
                    if (windowHandle != 0) {
                        orderOut(windowHandle);
                    }
                }
            } finally {
                nsViewLock.unlock();
            }
        }
    }
    private VisibleAction visibleAction = new VisibleAction();

    public void setVisible(boolean visible) {
        this.visible = visible;
        MainThread.invoke(true, visibleAction);
    }

    class TitleAction implements Runnable {
        public void run() {
            nsViewLock.lock();
            try {
                if (windowHandle != 0) {
                    setTitle0(windowHandle, title);
                }
            } finally {
                nsViewLock.unlock();
            }
        }
    }
    private TitleAction titleAction = new TitleAction();

    public void setTitle(String title) {
        super.setTitle(title);
        MainThread.invoke(true, titleAction);
    }

    class FocusAction implements Runnable {
        public void run() {
            nsViewLock.lock();
            try {
                if (windowHandle != 0) {
                    makeKey(windowHandle);
                }
            } finally {
                nsViewLock.unlock();
            }
        }
    }
    private FocusAction focusAction = new FocusAction();

    public void requestFocus() {
        super.requestFocus();
        MainThread.invoke(true, focusAction);
    }
    
    class SizeAction implements Runnable {
        public void run() {
            nsViewLock.lock();
            try {
                if (windowHandle != 0) {
                    setContentSize(windowHandle, width, height);
                }
            } finally {
                nsViewLock.unlock();
            }
        }
    }
    private SizeAction sizeAction = new SizeAction();

    public void setSize(int width, int height) {
        this.width=width;
        this.height=height;
        if(!fullscreen) {
            nfs_width=width;
            nfs_height=height;
        }
        MainThread.invoke(true, sizeAction);
    }
    
    class PositionAction implements Runnable {
        public void run() {
            nsViewLock.lock();
            try {
                if (windowHandle != 0) {
                    setFrameTopLeftPoint(windowHandle, x, y);
                }
            } finally {
                nsViewLock.unlock();
            }
        }
    }
    private PositionAction positionAction = new PositionAction();

    public void setPosition(int x, int y) {
        this.x=x;
        this.y=y;
        if(!fullscreen) {
            nfs_x=x;
            nfs_y=y;
        }
        MainThread.invoke(true, positionAction);
    }
    
    class FullscreenAction implements Runnable {
        public void run() {
            nsViewLock.lock();
            try {
                if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                    System.err.println("MacWindow fs: "+fullscreen+" "+x+"/"+y+" "+width+"x"+height);
                }
                createWindow(true);
            } finally {
                nsViewLock.unlock();
            }
        }
    }
    private FullscreenAction fullscreenAction = new FullscreenAction();

    public boolean setFullscreen(boolean fullscreen) {
        if(this.fullscreen!=fullscreen) {
            this.fullscreen=fullscreen;
            if(fullscreen) {
                x = 0; 
                y = 0;
                width = screen.getWidth();
                height = screen.getHeight();
            } else {
                x = nfs_x;
                y = nfs_y;
                width = nfs_width;
                height = nfs_height;
            }
            MainThread.invoke(true, fullscreenAction);
        }
        return fullscreen;
    }
    
    private void sizeChanged(int newWidth, int newHeight) {
        if (DEBUG_IMPLEMENTATION) {
            System.out.println(Thread.currentThread().getName()+" Size changed to " + newWidth + ", " + newHeight);
        }
        width = newWidth;
        height = newHeight;
        if(!fullscreen) {
            nfs_width=width;
            nfs_height=height;
        }
        if (DEBUG_IMPLEMENTATION) {
            System.out.println("  Posted WINDOW_RESIZED event");
        }
        sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
    }

    private void positionChanged(int newX, int newY) {
        if (DEBUG_IMPLEMENTATION) {
            System.out.println(Thread.currentThread().getName()+" Position changed to " + newX + ", " + newY);
        }
        x = newX;
        y = newY;
        if(!fullscreen) {
            nfs_x=x;
            nfs_y=y;
        }
        if (DEBUG_IMPLEMENTATION) {
            System.out.println("  Posted WINDOW_MOVED event");
        }
        sendWindowEvent(WindowEvent.EVENT_WINDOW_MOVED);
    }

    private void focusChanged(boolean focusGained) {
        if (focusGained) {
            sendWindowEvent(WindowEvent.EVENT_WINDOW_GAINED_FOCUS);
        } else {
            sendWindowEvent(WindowEvent.EVENT_WINDOW_LOST_FOCUS);
        }
    }

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

    protected void sendKeyEvent(int eventType, int modifiers, int keyCode, char keyChar) {
        int key = convertKeyChar(keyChar);
        if(DEBUG_IMPLEMENTATION) System.out.println("MacWindow.sendKeyEvent "+Thread.currentThread().getName());
        // Note that we send the key char for the key code on this
        // platform -- we do not get any useful key codes out of the system
        super.sendKeyEvent(eventType, modifiers, key, keyChar);
    }

    private void createWindow(boolean recreate) {
        if(0!=windowHandle && !recreate) {
            return;
        }
        if(0!=windowHandle) {
            // save the view .. close the window
            surfaceHandle = changeContentView(windowHandle, 0);
            if(recreate && 0==surfaceHandle) {
                throw new NativeWindowException("Internal Error - recreate, window but no view");
            }
            close0(windowHandle);
            windowHandle=0;
        } else {
            surfaceHandle = 0;
        }
        windowHandle = createWindow0(getX(), getY(), getWidth(), getHeight(), fullscreen,
                                     (isUndecorated() ?
                                     NSBorderlessWindowMask :
                                     NSTitledWindowMask|NSClosableWindowMask|NSMiniaturizableWindowMask|NSResizableWindowMask),
                                     NSBackingStoreBuffered, 
                                     getScreen().getIndex(), surfaceHandle);
        if (windowHandle == 0) {
            throw new NativeWindowException("Could create native window "+Thread.currentThread().getName()+" "+this);
        }
        surfaceHandle = contentView(windowHandle);
        setTitle0(windowHandle, getTitle());
        makeKeyAndOrderFront(windowHandle);
        sendWindowEvent(WindowEvent.EVENT_WINDOW_MOVED);
        sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
        sendWindowEvent(WindowEvent.EVENT_WINDOW_GAINED_FOCUS);
    }
    
    protected static native boolean initIDs();
    private native long createWindow0(int x, int y, int w, int h,
                                     boolean fullscreen, int windowStyle,
                                     int backingStoreType,
                                     int screen_idx, long view);
    private native void makeKeyAndOrderFront(long window);
    private native void makeKey(long window);
    private native void orderOut(long window);
    private native void close0(long window);
    private native void setTitle0(long window, String title);
    private native long contentView(long window);
    private native long changeContentView(long window, long view);
    private native void setContentSize(long window, int w, int h);
    private native void setFrameTopLeftPoint(long window, int x, int y);
}
