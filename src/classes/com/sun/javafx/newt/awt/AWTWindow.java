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

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.*;
import java.util.*;

import com.sun.javafx.newt.Window;

/** An implementation of the Newt Window class built using the
    AWT. This is provided for convenience of porting to platforms
    supporting Java SE. */

public class AWTWindow extends Window {
    private Frame frame;
    private Canvas canvas;
    private LinkedList/*<AWTEventWrapper>*/ events = new LinkedList();
    private boolean gotDisplaySize;
    private int displayWidth;
    private int displayHeight;

    public final boolean isTerminalObject() {
        return false;
    }

    protected void createNative() {
        runOnEDT(new Runnable() {
                public void run() {
                    frame = new Frame("AWT NewtWindow");
                    frame.setLayout(new BorderLayout());
                    canvas = new Canvas();
                    Listener listener = new Listener();
                    canvas.addMouseListener(listener);
                    canvas.addMouseMotionListener(listener);
                    canvas.addKeyListener(listener);
                    frame.add(canvas, BorderLayout.CENTER);
                    frame.setSize(width, height);
                    frame.setLocation(x, y);
                }
            });
    }

    protected void closeNative() {
        runOnEDT(new Runnable() {
                public void run() {
                    frame.dispose();
                    frame = null;
                }
            });
    }

    public int getDisplayWidth() {
        getDisplaySize();
        return displayWidth;
    }

    public int getDisplayHeight() {
        getDisplaySize();
        return displayHeight;
    }

    private void getDisplaySize() {
        if (!gotDisplaySize) {
            DisplayMode mode =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
            displayWidth = mode.getWidth();
            displayHeight = mode.getHeight();
            gotDisplaySize = true;
        }
    }

    public void setVisible(final boolean visible) {
        runOnEDT(new Runnable() {
                public void run() {
                    frame.setVisible(visible);
                }
            });
    }

    public void setSize(final int width, final int height) {
        this.width = width;
        this.height = height;
        runOnEDT(new Runnable() {
                public void run() {
                    frame.setSize(width, height);
                }
            });
    }

    public void setPosition(final int x, final int y) {
        runOnEDT(new Runnable() {
                public void run() {
                    frame.setLocation(x, y);
                }
            });
    }

    public boolean setFullscreen(boolean fullscreen) {
        // Ignore for now
        return false;
    }

    public Object getWrappedWindow() {
        return canvas;
    }

    public void dispatchMessages(int eventMask) {
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
                if (w.isMouseEvent()) {
                    if ((eventMask & com.sun.javafx.newt.EventListener.MOUSE) != 0) {
                        MouseEvent e = (MouseEvent) w.getEvent();
                        sendMouseEvent(w.getType(), convertModifiers(e),
                                       e.getX(), e.getY(), convertButton(e));
                    }
                } else {
                    if ((eventMask & com.sun.javafx.newt.EventListener.KEY) != 0) {
                        KeyEvent e = (KeyEvent) w.getEvent();
                        sendKeyEvent(w.getType(), convertModifiers(e),
                                     e.getKeyCode(), e.getKeyChar());
                    }
                }
                if(DEBUG_MOUSE_EVENT) {
                    System.out.println("dispatchMessages: in event:"+w.getEvent());
                }
            }
        } while (w != null);
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

    private static void runOnEDT(Runnable r) {
        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            try {
                EventQueue.invokeAndWait(r);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void enqueueEvent(boolean isMouseEvent, int type, InputEvent e) {
        if(DEBUG_MOUSE_EVENT) {
            System.out.println("enqueueEvent: mouse"+isMouseEvent+", event: "+e);
        }
        AWTEventWrapper wrapper = new AWTEventWrapper(isMouseEvent,type, e);
        synchronized(this) {
            events.add(wrapper);
        }
    }

    static class AWTEventWrapper {
        boolean isMouseEvent;
        int type;
        InputEvent e;

        AWTEventWrapper(boolean isMouseEvent, int type, InputEvent e) {
            this.isMouseEvent = isMouseEvent;
            this.type = type;
            this.e = e;
        }

        public boolean isMouseEvent() {
            return isMouseEvent;
        }

        public int getType() {
            return type;
        }

        public InputEvent getEvent() {
            return e;
        }
    }

    class Listener implements MouseListener, MouseMotionListener, KeyListener {
        public void mouseClicked(MouseEvent e) {
            // We ignore these as we synthesize them ourselves out of
            // mouse pressed and released events
        }

        public void mouseEntered(MouseEvent e) {
            enqueueEvent(true, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_ENTERED, e);
        }

        public void mouseExited(MouseEvent e) {
            enqueueEvent(true, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_EXITED, e);
        }

        public void mousePressed(MouseEvent e) {
            enqueueEvent(true, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_PRESSED, e);
        }

        public void mouseReleased(MouseEvent e) {
            enqueueEvent(true, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_RELEASED, e);
        }

        public void mouseMoved(MouseEvent e) {
            enqueueEvent(true, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_MOVED, e);
        }

        public void mouseDragged(MouseEvent e) {
            enqueueEvent(true, com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_DRAGGED, e);
        }

        public void keyPressed(KeyEvent e) {
            enqueueEvent(false, com.sun.javafx.newt.KeyEvent.EVENT_KEY_PRESSED, e);
        }

        public void keyReleased(KeyEvent e) {
            enqueueEvent(false, com.sun.javafx.newt.KeyEvent.EVENT_KEY_RELEASED, e);
        }

        public void keyTyped(KeyEvent e)  {
            enqueueEvent(false, com.sun.javafx.newt.KeyEvent.EVENT_KEY_TYPED, e);
        }
    }
}
