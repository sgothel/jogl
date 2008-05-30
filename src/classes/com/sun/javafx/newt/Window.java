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

package com.sun.javafx.newt;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class Window {
    /** OpenKODE window type */
    public static final String KD = "KD";

    /** Microsoft Windows window type */
    public static final String WINDOWS = "Windows";

    /** X11 window type */
    public static final String X11 = "X11";

    /** Mac OS X window type */
    public static final String MACOSX = "MacOSX";

    /** Creates a Window of the default type for the current operating system. */
    public static Window create(long visualID) {
      String osName = System.getProperty("os.name");
      String osNameLowerCase = osName.toLowerCase();
      String windowType;
      if (osNameLowerCase.startsWith("wind")) {
          windowType = WINDOWS;
      } else if (osNameLowerCase.startsWith("mac os x")) {
          windowType = MACOSX;
      } else {
          windowType = X11;
      }
      Window window = create(windowType);
      window.initNative(visualID);
      return window;
    }


    public static Window create(String type) {
        try {
            Class windowClass = null;
            if (KD.equals(type)) {
                windowClass = Class.forName("com.sun.javafx.newt.kd.KDWindow");
            } else if (WINDOWS.equals(type)) {
                windowClass = Class.forName("com.sun.javafx.newt.windows.WindowsWindow");
            } else if (X11.equals(type)) {
                windowClass = Class.forName("com.sun.javafx.newt.x11.X11Window");
            } else if (MACOSX.equals(type)) {
                windowClass = Class.forName("com.sun.javafx.newt.macosx.MacOSXWindow");
            } else {
                throw new RuntimeException("Unknown window type \"" + type + "\"");
            }
            return (Window) windowClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void initNative(long visualID);

    public abstract void    setVisible(boolean visible);
    public abstract void    setSize(int width, int height);
    public abstract void    setPosition(int x, int y);
    public abstract boolean isVisible();
    public abstract int     getWidth();
    public abstract int     getHeight();
    public abstract int     getX();
    public abstract int     getY();
    public abstract boolean setFullscreen(boolean fullscreen);
    public abstract boolean isFullscreen();
    public abstract int     getDisplayWidth();
    public abstract int     getDisplayHeight();
    public abstract long    getWindowHandle();
    public abstract void    pumpMessages();

    //
    // MouseListener Support
    //

    public synchronized void addMouseListener(MouseListener l) {
        if(l == null) {
            return;
        }
        mouseListener.add(l);
    }

    public synchronized void removeMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        mouseListener.remove(l);
    }

    public synchronized MouseListener[] getMouseListeners() {
        return (MouseListener[]) mouseListener.toArray();
    }

    private ArrayList mouseListener = new ArrayList();
    private long lastMousePressed = 0;
    private int  lastMouseClickCount = 0;
    public  static final int ClickTimeout = 200;

    private void sendMouseEvent(int eventType, int modifiers, int x, int y, int button) {
        long when = System.currentTimeMillis();
        MouseEvent eClicked = null;
        MouseEvent e = null;

        if(MouseEvent.EVENT_MOUSE_PRESSED==eventType) {
            if(when-lastMousePressed<ClickTimeout) {
                lastMouseClickCount++;
            } else {
                lastMouseClickCount=1;
            }
            lastMousePressed=when;
            e = new MouseEvent(true, eventType, this, when,
                               modifiers, x, y, lastMouseClickCount, button);
        } else if(MouseEvent.EVENT_MOUSE_RELEASED==eventType) {
            e = new MouseEvent(true, eventType, this, when,
                               modifiers, x, y, lastMouseClickCount, button);
            if(when-lastMousePressed<ClickTimeout) {
                eClicked = new MouseEvent(true, MouseEvent.EVENT_MOUSE_CLICKED, this, when,
                                          modifiers, x, y, lastMouseClickCount, button);
            }
            lastMouseClickCount=0;
            lastMousePressed=0;
        } else if(MouseEvent.EVENT_MOUSE_MOVED==eventType &&
                  1==lastMouseClickCount) {
            e = new MouseEvent(true, eventType, this, when,
                               modifiers, x, y, 1, button);
        } else {
            e = new MouseEvent(true, eventType, this, when,
                               modifiers, x, y, 0, button);
        }

        for(Iterator i = mouseListener.iterator(); i.hasNext(); ) {
            switch(eventType) {
                case MouseEvent.EVENT_MOUSE_ENTERED:
                    ((MouseListener)i.next()).mouseEntered(e);
                    break;
                case MouseEvent.EVENT_MOUSE_EXITED:
                    ((MouseListener)i.next()).mouseExited(e);
                    break;
                case MouseEvent.EVENT_MOUSE_PRESSED:
                    ((MouseListener)i.next()).mousePressed(e);
                    break;
                case MouseEvent.EVENT_MOUSE_RELEASED:
                    ((MouseListener)i.next()).mouseReleased(e);
                    if(null!=eClicked) {
                        ((MouseListener)i.next()).mouseClicked(eClicked);
                    }
                    break;
                case MouseEvent.EVENT_MOUSE_MOVED:
                    ((MouseListener)i.next()).mouseMoved(e);
                    break;
                case MouseEvent.EVENT_MOUSE_DRAGGED:
                    ((MouseListener)i.next()).mouseDragged(e);
                    break;
            }
        }
    }

    //
    // KeyListener Support
    //

    public synchronized void addKeyListener(KeyListener l) {
        if(l == null) {
            return;
        }
        keyListener.add(l);
    }

    public synchronized void removeKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        keyListener.remove(l);
    }

    public synchronized KeyListener[] getKeyListeners() {
        return (KeyListener[]) keyListener.toArray();
    }

    private ArrayList keyListener = new ArrayList();

    private void sendKeyEvent(int eventType, int modifiers, int keyCode, char keyChar) {
        KeyEvent e = new KeyEvent(true, eventType, this, System.currentTimeMillis(), 
                                      modifiers, keyCode, keyChar);
        for(Iterator i = keyListener.iterator(); i.hasNext(); ) {
            switch(eventType) {
                case KeyEvent.EVENT_KEY_PRESSED:
                    ((KeyListener)i.next()).keyPressed(e);
                    break;
                case KeyEvent.EVENT_KEY_RELEASED:
                    ((KeyListener)i.next()).keyReleased(e);
                    break;
                case KeyEvent.EVENT_KEY_TYPED:
                    ((KeyListener)i.next()).keyTyped(e);
                    break;
            }
        }
    }
}

