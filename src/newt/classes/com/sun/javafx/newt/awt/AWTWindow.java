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
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.awt.event.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import com.sun.javafx.newt.Window;
import javax.media.nativewindow.*;
import javax.media.nativewindow.awt.*;

/** An implementation of the Newt Window class built using the
    AWT. This is provided for convenience of porting to platforms
    supporting Java SE. */

public class AWTWindow extends Window {

    public AWTWindow() {
        super();
        title = "AWT NewtWindow";
    }

    private Frame frame;
    private AWTCanvas canvas;
    private LinkedList/*<AWTEventWrapper>*/ events = new LinkedList();
    // non fullscreen dimensions ..
    private int nfs_width, nfs_height, nfs_x, nfs_y;

    public void setTitle(final String title) {
        super.setTitle(title);
        runOnEDT(true, new Runnable() {
                public void run() {
                    if (frame != null) {
                        frame.setTitle(title);
                    }
                }
            });
    }

    protected void createNative(final Capabilities caps) {

        runOnEDT(true, new Runnable() {
                public void run() {
                    frame = new Frame(getTitle());
                    frame.setUndecorated(isUndecorated());
                    frame.setLayout(new BorderLayout());
                    canvas = new AWTCanvas(caps);
                    Listener listener = new Listener();
                    canvas.addMouseListener(listener);
                    canvas.addMouseMotionListener(listener);
                    canvas.addKeyListener(listener);
                    canvas.addComponentListener(listener);
                    frame.add(canvas, BorderLayout.CENTER);
                    frame.setSize(width, height);
                    frame.setLocation(x, y);
                    frame.addComponentListener(new MoveListener());
                    frame.addWindowListener(new WindowEventListener());
                }
            });
    }

    protected void closeNative() {
        runOnEDT(true, new Runnable() {
                public void run() {
                    frame.dispose();
                    frame = null;
                }
            });
    }

    public boolean hasDeviceChanged() {
        boolean res = canvas.hasDeviceChanged();
        if(res) {
            config = canvas.getAWTGraphicsConfiguration();
            if (config == null) {
                throw new NativeWindowException("Error Device change null GraphicsConfiguration: "+this);
            }
            updateDeviceData();
        }
        return res;
    }

    public void setVisible(final boolean visible) {
        runOnEDT(true, new Runnable() {
                public void run() {
                    frame.setVisible(visible);
                }
            });

        config = canvas.getAWTGraphicsConfiguration();

        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }

        updateDeviceData();
    }

    private void updateDeviceData() {
        // propagate new info ..
        ((AWTScreen)getScreen()).setAWTGraphicsScreen((AWTGraphicsScreen)config.getScreen());
        ((AWTDisplay)getScreen().getDisplay()).setAWTGraphicsDevice((AWTGraphicsDevice)config.getScreen().getDevice());

        DisplayMode mode = ((AWTGraphicsDevice)config.getScreen().getDevice()).getGraphicsDevice().getDisplayMode();
        int w = mode.getWidth();
        int h = mode.getHeight();
        ((AWTScreen)screen).setScreenSize(w, h);
    }

    public void setSize(final int width, final int height) {
        this.width = width;
        this.height = height;
        if(!fullscreen) {
            nfs_width=width;
            nfs_height=height;
        }
        /** An AWT event on setSize() would bring us in a deadlock situation, hence invokeLater() */
        runOnEDT(false, new Runnable() {
                public void run() {
                    frame.setSize(width, height);
                }
            });
    }

    public void setPosition(final int x, final int y) {
        this.x = x;
        this.y = y;
        if(!fullscreen) {
            nfs_x=x;
            nfs_y=y;
        }
        runOnEDT(true, new Runnable() {
                public void run() {
                    frame.setLocation(x, y);
                }
            });
    }

    public boolean setFullscreen(final boolean fullscreen) {
        if(this.fullscreen!=fullscreen) {
            final int x,y,w,h;
            this.fullscreen=fullscreen;
            if(fullscreen) {
                x = 0; y = 0;
                w = screen.getWidth();
                h = screen.getHeight();
            } else {
                x = nfs_x;
                y = nfs_y;
                w = nfs_width;
                h = nfs_height;
            }
            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                System.err.println("AWTWindow fs: "+fullscreen+" "+x+"/"+y+" "+w+"x"+h);
            }
            /** An AWT event on setSize() would bring us in a deadlock situation, hence invokeLater() */
            runOnEDT(false, new Runnable() {
                    public void run() {
                        if(!frame.isDisplayable()) {
                            frame.setUndecorated(fullscreen);
                        } else {
                            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                                System.err.println("AWTWindow can't undecorate already created frame");
                            }
                        }
                        frame.setLocation(x, y);
                        frame.setSize(w, h);
                    }
                });
        }
        return true;
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
                switch (w.getType()) {
                    case com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_RESIZED:
                    case com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_MOVED:
                    case com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY:
                        if ((eventMask & com.sun.javafx.newt.EventListener.WINDOW) != 0) {
                            sendWindowEvent(w.getType());
                        }
                        break;

                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_CLICKED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_ENTERED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_EXITED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_PRESSED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_RELEASED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_MOVED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_DRAGGED:
                    case com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_WHEEL_MOVED:
                        if ((eventMask & com.sun.javafx.newt.EventListener.MOUSE) != 0) {
                            MouseEvent e = (MouseEvent) w.getEvent();
                            int rotation = 0;
                            if (e instanceof MouseWheelEvent) {
                                rotation = ((MouseWheelEvent)e).getWheelRotation();
                            }
                            sendMouseEvent(w.getType(), convertModifiers(e),
                                           e.getX(), e.getY(), convertButton(e),
                                           rotation);
                        }
                        break;

                    case com.sun.javafx.newt.KeyEvent.EVENT_KEY_PRESSED:
                    case com.sun.javafx.newt.KeyEvent.EVENT_KEY_RELEASED:
                    case com.sun.javafx.newt.KeyEvent.EVENT_KEY_TYPED:
                        if ((eventMask & com.sun.javafx.newt.EventListener.KEY) != 0) {
                            KeyEvent e = (KeyEvent) w.getEvent();
                            sendKeyEvent(w.getType(), convertModifiers(e),
                                         e.getKeyCode(), e.getKeyChar());
                        }
                        break;

                    default:
                        throw new NativeWindowException("Unknown event type " + w.getType());
                }
                if(Window.DEBUG_MOUSE_EVENT) {
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

    private static void runOnEDT(boolean wait, Runnable r) {
        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            try {
                if(wait) {
                    EventQueue.invokeAndWait(r);
                } else {
                    EventQueue.invokeLater(r);
                }
            } catch (Exception e) {
                throw new NativeWindowException(e);
            }
        }
    }

    private void enqueueEvent(int type, InputEvent e) {
        AWTEventWrapper wrapper = new AWTEventWrapper(type, e);
        synchronized(this) {
            events.add(wrapper);
        }
    }

    private static final int WINDOW_EVENT = 1;
    private static final int KEY_EVENT = 2;
    private static final int MOUSE_EVENT = 3;

    static class AWTEventWrapper {
        int type;
        InputEvent e;

        AWTEventWrapper(int type, InputEvent e) {
            this.type = type;
            this.e = e;
        }

        public int getType() {
            return type;
        }

        public InputEvent getEvent() {
            return e;
        }
    }

    class MoveListener implements ComponentListener {
        public void componentResized(ComponentEvent e) {
        }

        public void componentMoved(ComponentEvent e) {
            x = frame.getX();
            y = frame.getY();
            enqueueEvent(com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_MOVED, null);
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }

    }

    class Listener implements ComponentListener, MouseListener, MouseMotionListener, KeyListener {
        public void componentResized(ComponentEvent e) {
            width = canvas.getWidth();
            height = canvas.getHeight();
            enqueueEvent(com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_RESIZED, null);
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
            // We ignore these as we synthesize them ourselves out of
            // mouse pressed and released events
        }

        public void mouseEntered(MouseEvent e) {
            enqueueEvent(com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_ENTERED, e);
        }

        public void mouseExited(MouseEvent e) {
            enqueueEvent(com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_EXITED, e);
        }

        public void mousePressed(MouseEvent e) {
            enqueueEvent(com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_PRESSED, e);
        }

        public void mouseReleased(MouseEvent e) {
            enqueueEvent(com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_RELEASED, e);
        }

        public void mouseMoved(MouseEvent e) {
            enqueueEvent(com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_MOVED, e);
        }

        public void mouseDragged(MouseEvent e) {
            enqueueEvent(com.sun.javafx.newt.MouseEvent.EVENT_MOUSE_DRAGGED, e);
        }

        public void keyPressed(KeyEvent e) {
            enqueueEvent(com.sun.javafx.newt.KeyEvent.EVENT_KEY_PRESSED, e);
        }

        public void keyReleased(KeyEvent e) {
            enqueueEvent(com.sun.javafx.newt.KeyEvent.EVENT_KEY_RELEASED, e);
        }

        public void keyTyped(KeyEvent e)  {
            enqueueEvent(com.sun.javafx.newt.KeyEvent.EVENT_KEY_TYPED, e);
        }
    }

    class WindowEventListener implements WindowListener {
        public void windowActivated(WindowEvent e) {
        }
        public void windowClosed(WindowEvent e) {
        }
        public void windowClosing(WindowEvent e) {
            enqueueEvent(com.sun.javafx.newt.WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY, null);
        }
        public void windowDeactivated(WindowEvent e) {
        }
        public void windowDeiconified(WindowEvent e) {
        }
        public void windowIconified(WindowEvent e) {
        }
        public void windowOpened(WindowEvent e) {
        }
    }
}
