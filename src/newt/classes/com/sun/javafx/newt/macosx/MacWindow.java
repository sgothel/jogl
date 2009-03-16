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

import javax.media.nwi.NWCapabilities;
import javax.media.nwi.NativeWindowException;

import com.sun.javafx.newt.*;
import com.sun.javafx.newt.impl.*;

public class MacWindow extends Window {
    private static final boolean DEBUG = false;
    
    private static native boolean initIDs();
    
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

    private long nativeWindow;

    static {
        NativeLibLoader.loadNEWT();
        
        if (!initIDs()) {
            throw new NativeWindowException("Failed to initialize jmethodIDs");
        }
    }
    
    public MacWindow() {
    }
    
    protected void createNative(NWCapabilities caps) {
        chosenCaps = (NWCapabilities) caps.clone(); // FIXME: visualID := f1(caps); caps := f2(visualID)
        visualID = 0; // n/a
    }

    protected void closeNative() {
        if (nativeWindow != 0) {
            close0(nativeWindow);
            nativeWindow = 0;
        }
    }
    
    public long getSurfaceHandle() {
        if (nativeWindow == 0) {
            return 0;
        }
        return contentView(nativeWindow);
    }
    
    public void setVisible(boolean visible) {
        if (visible) {
            boolean created = false;
            if (nativeWindow == 0) {
                nativeWindow = createWindow(getX(), getY(), getWidth(), getHeight(),
                                            (isUndecorated() ?
                                             NSBorderlessWindowMask :
                                             NSTitledWindowMask|NSClosableWindowMask|NSMiniaturizableWindowMask|NSResizableWindowMask),
                                            NSBackingStoreBuffered,
                                            true);
                setTitle0(nativeWindow, getTitle());
                created = true;
            }
            makeKeyAndOrderFront(nativeWindow);
            if (created) {
                sendWindowEvent(WindowEvent.EVENT_WINDOW_MOVED);
                sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
            }
        } else {
            if (nativeWindow != 0) {
                orderOut(nativeWindow);
            }
        }

        this.visible = visible;
    }

    public void setTitle(String title) {
        super.setTitle(title);
        if (nativeWindow != 0) {
            setTitle0(nativeWindow, title);
        }
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        if (nativeWindow != 0) {
            setContentSize(nativeWindow, width, height);
        }
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        if (nativeWindow != 0) {
            setFrameTopLeftPoint(nativeWindow, x, y);
        }
    }
    
    public boolean setFullscreen(boolean fullscreen) {
        // FIXME: implement this
        return false;
    }
    
    private void sizeChanged(int newWidth, int newHeight) {
        if (DEBUG) {
            System.out.println("Size changed to " + newWidth + ", " + newHeight);
        }
        if (width != newWidth || height != newHeight) {
            width = newWidth;
            height = newHeight;
            if (DEBUG) {
                System.out.println("  Posted WINDOW_RESIZED event");
            }
            sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
        }
    }

    private void positionChanged(int newX, int newY) {
        if (DEBUG) {
            System.out.println("Position changed to " + newX + ", " + newY);
        }
        if (x != newX || y != newY) {
            x = newX;
            y = newY;
            if (DEBUG) {
                System.out.println("  Posted WINDOW_MOVED event");
            }
            sendWindowEvent(WindowEvent.EVENT_WINDOW_MOVED);
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
        // Note that we send the key char for the key code on this
        // platform -- we do not get any useful key codes out of the system
        super.sendKeyEvent(eventType, modifiers, key, keyChar);
    }

    private native long createWindow(int x, int y, int w, int h,
                                     int windowStyle,
                                     int backingStoreType,
                                     boolean deferCreation);
    private native void makeKeyAndOrderFront(long window);
    private native void orderOut(long window);
    private native void close0(long window);
    private native void setTitle0(long window, String title);
    protected native void dispatchMessages(int eventMask);
    private native long contentView(long window);
    private native void setContentSize(long window, int w, int h);
    private native void setFrameTopLeftPoint(long window, int x, int y);
}
