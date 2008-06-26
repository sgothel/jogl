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

import javax.media.opengl.NativeWindow;
import javax.media.opengl.NativeWindowException;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class Window implements NativeWindow
{
    public static final boolean DEBUG_MOUSE_EVENT = false;
    public static final boolean DEBUG_KEY_EVENT = false;
    public static final boolean DEBUG_IMPLEMENTATION = false;
    
    private static Class getWindowClass(String type) 
        throws ClassNotFoundException 
    {
        Class windowClass = null;
        if (NewtFactory.KD.equals(type)) {
            windowClass = Class.forName("com.sun.javafx.newt.kd.KDWindow");
        } else if (NewtFactory.WINDOWS.equals(type)) {
            windowClass = Class.forName("com.sun.javafx.newt.windows.WindowsWindow");
        } else if (NewtFactory.X11.equals(type)) {
            windowClass = Class.forName("com.sun.javafx.newt.x11.X11Window");
        } else if (NewtFactory.MACOSX.equals(type)) {
            windowClass = Class.forName("com.sun.javafx.newt.macosx.MacOSXWindow");
        } else {
            throw new RuntimeException("Unknown window type \"" + type + "\"");
        }
        return windowClass;
    }

    protected static Window create(String type, Screen screen, long visualID) {
        try {
            Class windowClass = getWindowClass(type);
            Window window = (Window) windowClass.newInstance();
            window.invalidate();
            window.screen   = screen;
            window.visualID = visualID;
            window.createNative();
            return window;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    protected static Window wrapHandle(String type, Screen screen, long visualID, 
                                 long windowHandle, boolean fullscreen, boolean visible, 
                                 int x, int y, int width, int height) 
    {
        try {
            Class windowClass = getWindowClass(type);
            Window window = (Window) windowClass.newInstance();
            window.invalidate();
            window.screen   = screen;
            window.visualID = visualID;
            window.windowHandle = windowHandle;
            window.fullscreen=fullscreen;
            window.visible=visible;
            window.x=x;
            window.y=y;
            window.width=width;
            window.height=height;
            return window;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    /**
     * create native windowHandle, ie creates a new native invisible window
     */
    protected abstract void createNative();

    protected abstract void closeNative();

    public Screen getScreen() {
        return screen;
    }

    public abstract int getDisplayWidth();
    public abstract int getDisplayHeight();

    /**
     * eventMask is a bitfield of EventListener event flags
     */
    public void pumpMessages(int eventMask) {
        if(this.eventMask!=eventMask && eventMask>0) {
            this.eventMask=eventMask;
            eventMask*=-1;
        }
        dispatchMessages(eventMask);
    }

    public void pumpMessages() {
        int em = 0;
        //if(windowistener.size()>0) em |= EventListener.WINDOW;
        if(mouseListener.size()>0) em |= EventListener.MOUSE;
        if(keyListener.size()>0) em |= EventListener.KEY;
        pumpMessages(em);
    }

    public abstract void dispatchMessages(int eventMask);

    public String toString() {
        return "Window[handle "+windowHandle+
                    ", pos "+getX()+"/"+getY()+", size "+getWidth()+"x"+getHeight()+
                    ", visible "+isVisible()+"]";
    }

    protected Screen screen;
    protected long   visualID;
    protected long   windowHandle;
    protected boolean locked=false;
    protected boolean fullscreen, visible;
    protected int width, height, x, y;
    protected int     eventMask;

    //
    // NativeWindow impl
    //

    public int lockSurface() throws NativeWindowException {
        if (locked) {
          throw new NativeWindowException("Surface already locked");
        }
        // locked = true;
        // return LOCK_SUCCESS;
        return LOCK_NOT_SUPPORTED;
    }

    public void unlockSurface() {
        if (locked) {
            locked = false;
        }
    }

    public boolean isSurfaceLocked() {
        return locked;
    }

    public void close() {
        closeNative();
        invalidate();
    }

    public void invalidate() {
        unlockSurface();
        screen   = null;
        visualID = 0;
        windowHandle = 0;
        locked = false;
        fullscreen=false;
        visible=false;
        eventMask = 0;

        // Default position and dimension will be re-set immediately by user
        width  = 100;
        height = 100;
        x=0;
        y=0;
    }

    protected void clearEventMask() {
        eventMask=0;
    }

    public long getDisplayHandle() {
        return screen.getDisplay().getHandle();
    }

    public long getScreenHandle() {
        return screen.getHandle();
    }

    public int  getScreenIndex() {
        return screen.getIndex();
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public long getSurfaceHandle() {
        return windowHandle; // default: return window handle
    }

    public long getVisualID() {
        return visualID;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public abstract void    setVisible(boolean visible);

    public abstract void    setSize(int width, int height);
    public abstract void    setPosition(int x, int y);
    public abstract boolean setFullscreen(boolean fullscreen);

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
    private boolean[] buttonStates = new boolean[3];

    protected void sendMouseEvent(int eventType, int modifiers, int x, int y, int button) {
        if(DEBUG_MOUSE_EVENT) {
            System.out.println("sendMouseEvent: "+MouseEvent.getEventTypeString(eventType)+
                               ", mod "+modifiers+", pos "+x+"/"+y+", button "+button);
        }
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
            buttonStates[button - 1] = true;
            e = new MouseEvent(true, eventType, this, when,
                               modifiers, x, y, lastMouseClickCount, button);
        } else if(MouseEvent.EVENT_MOUSE_RELEASED==eventType) {
            buttonStates[button - 1] = false;
            e = new MouseEvent(true, eventType, this, when,
                               modifiers, x, y, lastMouseClickCount, button);
            if(when-lastMousePressed<ClickTimeout) {
                eClicked = new MouseEvent(true, MouseEvent.EVENT_MOUSE_CLICKED, this, when,
                                          modifiers, x, y, lastMouseClickCount, button);
            } else {
                lastMouseClickCount=0;
                lastMousePressed=0;
            }
        } else if(MouseEvent.EVENT_MOUSE_MOVED==eventType) {
            if (buttonStates[0] || buttonStates[1] || buttonStates[2]) {
                e = new MouseEvent(true, MouseEvent.EVENT_MOUSE_DRAGGED, this, when,
                                   modifiers, x, y, 1, button);
            } else {
                e = new MouseEvent(true, eventType, this, when,
                                   modifiers, x, y, 0, button);
            }
        }

        if(DEBUG_MOUSE_EVENT) {
            System.out.println("sendMouseEvent: event:         "+e);
            if(null!=eClicked) {
                System.out.println("sendMouseEvent: event Clicked: "+eClicked);
            }
        }

        for(Iterator i = mouseListener.iterator(); i.hasNext(); ) {
            switch(e.getEventType()) {
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
                    MouseListener ml = (MouseListener)i.next();
                    ml.mouseReleased(e);
                    if(null!=eClicked) {
                        ml.mouseClicked(eClicked);
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

    protected void sendKeyEvent(int eventType, int modifiers, int keyCode, char keyChar) {
        KeyEvent e = new KeyEvent(true, eventType, this, System.currentTimeMillis(), 
                                      modifiers, keyCode, keyChar);
        if(DEBUG_KEY_EVENT) {
            System.out.println("sendKeyEvent: "+e);
        }
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

