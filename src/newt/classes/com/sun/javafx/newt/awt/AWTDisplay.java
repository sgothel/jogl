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

package com.sun.javafx.newt.awt;

import java.awt.event.*;
import com.sun.javafx.newt.Display;
import com.sun.javafx.newt.Window;
import javax.media.nativewindow.*;
import javax.media.nativewindow.awt.*;
import java.util.*;

public class AWTDisplay extends Display {
    public AWTDisplay() {
    }

    protected void createNative() {
        aDevice = (AWTGraphicsDevice) AWTGraphicsDevice.createDevice(null); // default 
    }

    protected void setAWTGraphicsDevice(AWTGraphicsDevice d) {
        aDevice = d;
    }

    protected void closeNative() { }

    public void dispatchMessages() {
        AWTEventWrapper w;
        do {
            synchronized(this) {
                if (!events.isEmpty()) {
                    w = (AWTEventWrapper) events.removeFirst();
                } else {
                    w = null;
                }
            }
            if (w != null) {
                switch (w.getType()) {
                    case com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_RESIZED:
                    case com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_MOVED:
                    case com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY:
                        w.getWindow().sendWindowEvent(w.getType());
                        break;

                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_CLICKED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_ENTERED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_EXITED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_PRESSED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_RELEASED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_MOVED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_DRAGGED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_WHEEL_MOVED:
                        {
                            MouseEvent e = (MouseEvent) w.getEvent();
                            int rotation = 0;
                            if (e instanceof MouseWheelEvent) {
                                rotation = ((MouseWheelEvent)e).getWheelRotation();
                            }
                            w.getWindow().sendMouseEvent(w.getType(), convertModifiers(e),
                                                         e.getX(), e.getY(), convertButton(e),
                                                         rotation);
                        }
                        break;

                    case com.sun.javafx.newt.KeyEvent.EVENT_KEY_PRESSED:
                    case com.sun.javafx.newt.KeyEvent.EVENT_KEY_RELEASED:
                    case com.sun.javafx.newt.KeyEvent.EVENT_KEY_TYPED:
                        {
                            KeyEvent e = (KeyEvent) w.getEvent();
                            w.getWindow().sendKeyEvent(w.getType(), convertModifiers(e),
                                                       e.getKeyCode(), e.getKeyChar());
                        }
                        break;

                    default:
                        throw new NativeWindowException("Unknown event type " + w.getType());
                }
                if(Window.DEBUG_MOUSE_EVENT) {
                    System.out.println("dispatchMessages: "+w.getWindow()+" in event:"+w.getEvent());
                }
            }
        } while (w != null);
    }

    protected void enqueueEvent(AWTWindow w, int type, InputEvent e) {
        AWTEventWrapper wrapper = new AWTEventWrapper(w, type, e);
        synchronized(this) {
            events.add(wrapper);
        }
    }

    private LinkedList/*<AWTEventWrapper>*/ events = new LinkedList();

    static class AWTEventWrapper {
        AWTWindow window;
        int type;
        InputEvent e;

        AWTEventWrapper(AWTWindow w, int type, InputEvent e) {
            this.window = w;
            this.type = type;
            this.e = e;
        }

        public AWTWindow getWindow() {
            return window;
        }

        public int getType() {
            return type;
        }

        public InputEvent getEvent() {
            return e;
        }
    }

    private static int convertModifiers(InputEvent e) {
        int newtMods = 0;
        int mods = e.getModifiers();
        if ((mods & InputEvent.SHIFT_MASK) != 0)     newtMods |= com.sun.javafx.newt.InputEvent.SHIFT_MASK;
        if ((mods & InputEvent.CTRL_MASK) != 0)      newtMods |= com.sun.javafx.newt.InputEvent.CTRL_MASK;
        if ((mods & InputEvent.META_MASK) != 0)      newtMods |= com.sun.javafx.newt.InputEvent.META_MASK;
        if ((mods & InputEvent.ALT_MASK) != 0)       newtMods |= com.sun.javafx.newt.InputEvent.ALT_MASK;
        if ((mods & InputEvent.ALT_GRAPH_MASK) != 0) newtMods |= com.sun.javafx.newt.InputEvent.ALT_GRAPH_MASK;
        return newtMods;
    }

    private static int convertButton(MouseEvent e) {
        switch (e.getButton()) {
            case MouseEvent.BUTTON1: return com.sun.javafx.newt.MouseEvent.BUTTON1;
            case MouseEvent.BUTTON2: return com.sun.javafx.newt.MouseEvent.BUTTON2;
            case MouseEvent.BUTTON3: return com.sun.javafx.newt.MouseEvent.BUTTON3;
        }
        return 0;
    }

}
