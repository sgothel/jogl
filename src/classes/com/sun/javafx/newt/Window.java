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

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.NativeWindow;
import javax.media.opengl.NativeWindowException;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class Window implements NativeWindow
{
    public static final boolean DEBUG_MOUSE_EVENT = false;
    public static final boolean DEBUG_KEY_EVENT = false;
    public static final boolean DEBUG_WINDOW_EVENT = false;
    public static final boolean DEBUG_IMPLEMENTATION = false;
    
    private static Class getWindowClass(String type) 
        throws ClassNotFoundException 
    {
        Class windowClass = null;
        if (NewtFactory.KD.equals(type)) {
            windowClass = Class.forName("com.sun.javafx.newt.kd.KDWindow");
        } else if (NewtFactory.WINDOWS.equals(type)) {
            windowClass = Class.forName("com.sun.javafx.newt.windows.WindowsWindow");
        } else if (NewtFactory.MACOSX.equals(type)) {
            windowClass = Class.forName("com.sun.javafx.newt.macosx.MacWindow");
        } else if (NewtFactory.X11.equals(type)) {
            windowClass = Class.forName("com.sun.javafx.newt.x11.X11Window");
        } else if (NewtFactory.AWT.equals(type)) {
            windowClass = Class.forName("com.sun.javafx.newt.awt.AWTWindow");
        } else {
            throw new RuntimeException("Unknown window type \"" + type + "\"");
        }
        return windowClass;
    }

    protected static Window create(String type, Screen screen, GLCapabilities caps) {
        return create(type, screen, caps, false);
    }

    protected static Window create(String type, Screen screen, GLCapabilities caps, boolean undecorated) {
        try {
            Class windowClass = getWindowClass(type);
            Window window = (Window) windowClass.newInstance();
            window.invalidate();
            window.screen   = screen;
            window.visualID = 0;
            window.setUndecorated(undecorated);
            window.createNative(caps);
            return window;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    protected static Window wrapHandle(String type, Screen screen, GLCapabilities caps, long visualID, 
                                 long windowHandle, boolean fullscreen, boolean visible, 
                                 int x, int y, int width, int height) 
    {
        try {
            Class windowClass = getWindowClass(type);
            Window window = (Window) windowClass.newInstance();
            window.invalidate();
            window.screen   = screen;
            window.chosenCaps = caps;
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

    public abstract boolean isTerminalObject();

    /**
     * Create native windowHandle, ie creates a new native invisible window
     *
     * Shall use the capabilities to determine the visualID
     * and shall set chosenCaps.
     */
    protected abstract void createNative(GLCapabilities caps);

    protected abstract void closeNative();

    public Screen getScreen() {
        return screen;
    }

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
        if(windowListeners.size()>0) em |= EventListener.WINDOW;
        if(mouseListeners.size()>0) em |= EventListener.MOUSE;
        if(keyListeners.size()>0) em |= EventListener.KEY;
        pumpMessages(em);
    }

    protected abstract void dispatchMessages(int eventMask);

    public String toString() {
    return "NEWT-Window[windowHandle "+getWindowHandle()+
                    ", surfaceHandle "+getSurfaceHandle()+
                    ", pos "+getX()+"/"+getY()+", size "+getWidth()+"x"+getHeight()+
                    ", visible "+isVisible()+
                    ", wrappedWindow "+getWrappedWindow()+
                    ", terminalObject "+isTerminalObject()+
                    ", visualID "+visualID+
                    ", "+chosenCaps+
                    ", screen handle/index "+getScreenHandle()+"/"+getScreenIndex() +
                    ", display handle "+getDisplayHandle()+ "]";
    }

    protected Screen screen;

    /**
     * The GLCapabilities shall be used to determine the visualID
     */
    protected GLCapabilities chosenCaps;
    /**
     * The visualID shall be determined using the GLCapabilities
     */
    protected long   visualID;
    protected long   windowHandle;
    protected boolean locked=false;
    protected boolean fullscreen, visible;
    protected int width, height, x, y;
    protected int     eventMask;

    protected String title = "AWT NewtWindow";
    protected boolean undecorated = false;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUndecorated(boolean value) {
        undecorated = value;
    }

    public boolean isUndecorated() {
        return undecorated;
    }

    

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
        invalidate(false);
    }

    public void invalidate(boolean internal) {
        unlockSurface();
        screen   = null;
        visualID = 0;
        chosenCaps = null;
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
        if(null==screen ||
           null==screen.getDisplay()) {
           return 0;
        }
        return screen.getDisplay().getHandle();
    }

    public long getScreenHandle() {
        return (null!=screen)?screen.getHandle():0;
    }

    public int  getScreenIndex() {
        return (null!=screen)?screen.getIndex():0;
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public long getSurfaceHandle() {
        return windowHandle; // default: return window handle
    }

    public GLCapabilities getChosenCapabilities() {
        if (chosenCaps == null)
          return null;

        // Must return a new copy to avoid mutation by end user
        return (GLCapabilities) chosenCaps.clone();
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

    public Object getWrappedWindow() {
        return null;
    }

    //
    // MouseListener Support
    //

    public synchronized void addMouseListener(MouseListener l) {
        if(l == null) {
            return;
        }
        ArrayList newMouseListeners = (ArrayList) mouseListeners.clone();
        newMouseListeners.add(l);
        mouseListeners = newMouseListeners;
    }

    public synchronized void removeMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        ArrayList newMouseListeners = (ArrayList) mouseListeners.clone();
        newMouseListeners.remove(l);
        mouseListeners = newMouseListeners;
    }

    public synchronized MouseListener[] getMouseListeners() {
        return (MouseListener[]) mouseListeners.toArray();
    }

    private ArrayList mouseListeners = new ArrayList();
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
        } else {
            e = new MouseEvent(true, eventType, this, when, modifiers, x, y, 0, button);
        }

        if(DEBUG_MOUSE_EVENT) {
            System.out.println("sendMouseEvent: event:         "+e);
            if(null!=eClicked) {
                System.out.println("sendMouseEvent: event Clicked: "+eClicked);
            }
        }

        ArrayList listeners = null;
        synchronized(this) {
            listeners = mouseListeners;
        }
        for(Iterator i = listeners.iterator(); i.hasNext(); ) {
            MouseListener l = (MouseListener) i.next();
            switch(e.getEventType()) {
                case MouseEvent.EVENT_MOUSE_CLICKED:
                    l.mouseClicked(e);
                    break;
                case MouseEvent.EVENT_MOUSE_ENTERED:
                    l.mouseEntered(e);
                    break;
                case MouseEvent.EVENT_MOUSE_EXITED:
                    l.mouseExited(e);
                    break;
                case MouseEvent.EVENT_MOUSE_PRESSED:
                    l.mousePressed(e);
                    break;
                case MouseEvent.EVENT_MOUSE_RELEASED:
                    l.mouseReleased(e);
                    if(null!=eClicked) {
                        l.mouseClicked(eClicked);
                    }
                    break;
                case MouseEvent.EVENT_MOUSE_MOVED:
                    l.mouseMoved(e);
                    break;
                case MouseEvent.EVENT_MOUSE_DRAGGED:
                    l.mouseDragged(e);
                    break;
                default:
                    throw new RuntimeException("Unexpected mouse event type " + e.getEventType());
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
        ArrayList newKeyListeners = (ArrayList) keyListeners.clone();
        newKeyListeners.add(l);
        keyListeners = newKeyListeners;
    }

    public synchronized void removeKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        ArrayList newKeyListeners = (ArrayList) keyListeners.clone();
        newKeyListeners.remove(l);
        keyListeners = newKeyListeners;
    }

    public synchronized KeyListener[] getKeyListeners() {
        return (KeyListener[]) keyListeners.toArray();
    }

    private ArrayList keyListeners = new ArrayList();

    protected void sendKeyEvent(int eventType, int modifiers, int keyCode, char keyChar) {
        KeyEvent e = new KeyEvent(true, eventType, this, System.currentTimeMillis(), 
                                      modifiers, keyCode, keyChar);
        if(DEBUG_KEY_EVENT) {
            System.out.println("sendKeyEvent: "+e);
        }
        ArrayList listeners = null;
        synchronized(this) {
            listeners = keyListeners;
        }
        for(Iterator i = listeners.iterator(); i.hasNext(); ) {
            KeyListener l = (KeyListener) i.next();
            switch(eventType) {
                case KeyEvent.EVENT_KEY_PRESSED:
                    l.keyPressed(e);
                    break;
                case KeyEvent.EVENT_KEY_RELEASED:
                    l.keyReleased(e);
                    break;
                case KeyEvent.EVENT_KEY_TYPED:
                    l.keyTyped(e);
                    break;
                default:
                    throw new RuntimeException("Unexpected key event type " + e.getEventType());
            }
        }
    }

    //
    // WindowListener Support
    //

    private ArrayList windowListeners = new ArrayList();

    public synchronized void addWindowListener(WindowListener l) {
        if(l == null) {
            return;
        }
        ArrayList newWindowListeners = (ArrayList) windowListeners.clone();
        newWindowListeners.add(l);
        windowListeners = newWindowListeners;
    }

    public synchronized void removeWindowListener(WindowListener l) {
        if (l == null) {
            return;
        }
        ArrayList newWindowListeners = (ArrayList) windowListeners.clone();
        newWindowListeners.remove(l);
        windowListeners = newWindowListeners;
    }

    public synchronized WindowListener[] getWindowListeners() {
        return (WindowListener[]) windowListeners.toArray();
    }

    protected void sendWindowEvent(int eventType) {
        WindowEvent e = new WindowEvent(true, eventType, this, System.currentTimeMillis());
        if(DEBUG_WINDOW_EVENT) {
            System.out.println("sendWindowEvent: "+e);
        }
        ArrayList listeners = null;
        synchronized(this) {
            listeners = windowListeners;
        }
        for(Iterator i = listeners.iterator(); i.hasNext(); ) {
            WindowListener l = (WindowListener) i.next();
            switch(eventType) {
                case WindowEvent.EVENT_WINDOW_RESIZED:
                    l.windowResized(e);
                    break;
                case WindowEvent.EVENT_WINDOW_MOVED:
                    l.windowMoved(e);
                    break;
                default:
                    throw new RuntimeException("Unexpected window event type " + e.getEventType());
            }
        }
    }
}
