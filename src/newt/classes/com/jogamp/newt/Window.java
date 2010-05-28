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

package com.jogamp.newt;

import com.jogamp.newt.event.*;
import com.jogamp.newt.impl.Debug;
import com.jogamp.newt.util.EDTUtil;

import com.jogamp.common.util.*;
import javax.media.nativewindow.*;
import com.jogamp.nativewindow.impl.RecursiveToolkitLock;

import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Method;

public abstract class Window implements NativeWindow
{
    public static final boolean DEBUG_MOUSE_EVENT = Debug.debug("Window.MouseEvent");
    public static final boolean DEBUG_KEY_EVENT = Debug.debug("Window.KeyEvent");
    public static final boolean DEBUG_WINDOW_EVENT = Debug.debug("Window.WindowEvent");
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");
    
    // Workaround for initialization order problems on Mac OS X
    // between native Newt and (apparently) Fmod -- if Fmod is
    // initialized first then the connection to the window server
    // breaks, leading to errors from deep within the AppKit
    static void init(String type) {
        if (NativeWindowFactory.TYPE_MACOSX.equals(type)) {
            try {
                getWindowClass(type);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Class getWindowClass(String type) 
        throws ClassNotFoundException 
    {
        Class windowClass = NewtFactory.getCustomClass(type, "Window");
        if(null==windowClass) {
            if (NativeWindowFactory.TYPE_EGL.equals(type)) {
                windowClass = Class.forName("com.jogamp.newt.impl.opengl.kd.KDWindow");
            } else if (NativeWindowFactory.TYPE_WINDOWS.equals(type)) {
                windowClass = Class.forName("com.jogamp.newt.impl.windows.WindowsWindow");
            } else if (NativeWindowFactory.TYPE_MACOSX.equals(type)) {
                windowClass = Class.forName("com.jogamp.newt.impl.macosx.MacWindow");
            } else if (NativeWindowFactory.TYPE_X11.equals(type)) {
                windowClass = Class.forName("com.jogamp.newt.impl.x11.X11Window");
            } else if (NativeWindowFactory.TYPE_AWT.equals(type)) {
                windowClass = Class.forName("com.jogamp.newt.impl.awt.AWTWindow");
            } else {
                throw new NativeWindowException("Unknown window type \"" + type + "\"");
            }
        }
        return windowClass;
    }

    protected static Window create(String type, NativeWindow parentNativeWindow, long parentWindowHandle, Screen screen, Capabilities caps, boolean undecorated) {
        try {
            Class windowClass;
            if(caps.isOnscreen()) {
                windowClass = getWindowClass(type);
            } else {
                windowClass = OffscreenWindow.class;
            }
            Window window = (Window) windowClass.newInstance();
            window.invalidate(true);
            window.parentNativeWindow = parentNativeWindow;
            window.parentWindowHandle = parentWindowHandle;
            window.screen = screen;
            window.caps = (Capabilities)caps.clone();
            window.setUndecorated(undecorated||0!=parentWindowHandle);
            return window;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new NativeWindowException(t);
        }
    }

    protected static Window create(String type, Object[] cstrArguments, Screen screen, Capabilities caps, boolean undecorated) {
        try {
            Class windowClass = getWindowClass(type);
            Class[] cstrArgumentTypes = getCustomConstructorArgumentTypes(windowClass);
            if(null==cstrArgumentTypes) {
                throw new NativeWindowException("WindowClass "+windowClass+" doesn't support custom arguments in constructor");
            }
            int argsChecked = verifyConstructorArgumentTypes(cstrArgumentTypes, cstrArguments);
            if ( argsChecked < cstrArguments.length ) {
                throw new NativeWindowException("WindowClass "+windowClass+" constructor mismatch at argument #"+argsChecked+"; Constructor: "+getTypeStrList(cstrArgumentTypes)+", arguments: "+getArgsStrList(cstrArguments));
            }
            Window window = (Window) ReflectionUtil.createInstance( windowClass, cstrArgumentTypes, cstrArguments ) ;
            window.invalidate(true);
            window.screen = screen;
            window.caps = (Capabilities)caps.clone();
            window.setUndecorated(undecorated);
            return window;
        } catch (Throwable t) {
            throw new NativeWindowException(t);
        }
    }

    public static String toHexString(int hex) {
        return "0x" + Integer.toHexString(hex);
    }

    public static String toHexString(long hex) {
        return "0x" + Long.toHexString(hex);
    }

    protected Screen screen;

    protected NativeWindow parentNativeWindow;
    protected long parentWindowHandle;

    protected Capabilities caps;
    protected AbstractGraphicsConfiguration config;
    protected long   windowHandle;
    protected boolean fullscreen, visible;
    protected int width, height, x, y;

    // non fullscreen dimensions ..
    protected int nfs_width, nfs_height, nfs_x, nfs_y;

    protected String title = "Newt Window";
    protected boolean undecorated = false;

    private boolean createNative() {
        if( null==screen || 0!=windowHandle || !visible ) {
            return 0 != windowHandle ;
        }
        EDTUtil edtUtil = screen.getDisplay().getEDTUtil();
        if( null != edtUtil && edtUtil.isRunning() && !edtUtil.isCurrentThreadEDT() ) {
            throw new NativeWindowException("EDT enabled but not on EDT");
        }

        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.createNative() START ("+Thread.currentThread()+", "+this+")");
        }
        if(validateParentWindowHandle()) {
            createNativeImpl();
            setVisibleImpl(true);
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.createNative() END ("+Thread.currentThread()+", "+this+")");
        }
        return 0 != windowHandle ;
    }

    private boolean validateParentWindowHandle() {
        if(null!=parentNativeWindow) {
            parentWindowHandle = getNativeWindowHandle(parentNativeWindow);
            return 0 != parentWindowHandle ;
        }
        return true;
    }

    private static long getNativeWindowHandle(NativeWindow nativeWindow) {
        long handle = 0;
        if(null!=nativeWindow) {
            boolean ok = true;
            try {
                nativeWindow.lockSurface();
            } catch (NativeWindowException nwe) {
                // parent native window not ready .. just skip action for now
                ok = false;
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window.getNativeWindowHandle: not successful yet: "+nwe);
                }
            }
            if(ok) {
                handle = nativeWindow.getWindowHandle();
                nativeWindow.unlockSurface();
                if(0==handle) {
                    throw new NativeWindowException("Parent native window handle is NULL, after succesful locking: "+nativeWindow);
                }
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window.getNativeWindowHandle: "+nativeWindow);
                }
            }
        }
        return handle;
    }

    public void runOnEDTIfAvail(boolean wait, final Runnable task) {
        Screen screen = getInnerWindow().getScreen();
        if(null==screen) {
            throw new RuntimeException("Null screen of inner class: "+this);
        }
        EDTUtil edtUtil = screen.getDisplay().getEDTUtil();
        if(null!=edtUtil) {
            edtUtil.invoke(wait, task);
        } else {
            task.run();
        }
    }

    /**
     * Create native windowHandle, ie creates a new native invisible window.
     */
    protected abstract void createNativeImpl();

    protected abstract void closeNative();

    public Capabilities getRequestedCapabilities() {
        return (Capabilities)caps.clone();
    }

    public NativeWindow getParentNativeWindow() {
        return parentNativeWindow;
    }

    public Screen getScreen() {
        return screen;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getName()+"[Config "+config+
                    "\n, "+screen+
                    "\n, ParentWindow "+parentNativeWindow+
                    "\n, ParentWindowHandle "+toHexString(parentWindowHandle)+
                    "\n, WindowHandle "+toHexString(getWindowHandle())+
                    "\n, SurfaceHandle "+toHexString(getSurfaceHandle())+
                    "\n, Pos "+getX()+"/"+getY()+", size "+getWidth()+"x"+getHeight()+
                    "\n, Visible "+isVisible()+
                    "\n, Undecorated "+undecorated+
                    "\n, Fullscreen "+fullscreen+
                    "\n, WrappedWindow "+getWrappedWindow());

        sb.append(", SurfaceUpdatedListeners num "+surfaceUpdatedListeners.size()+" [");
        for (Iterator iter = surfaceUpdatedListeners.iterator(); iter.hasNext(); ) {
          sb.append(iter.next()+", ");
        }
        sb.append("], WindowListeners num "+windowListeners.size()+" [");
        for (Iterator iter = windowListeners.iterator(); iter.hasNext(); ) {
          sb.append(iter.next()+", ");
        }
        sb.append("], MouseListeners num "+mouseListeners.size()+" [");
        for (Iterator iter = mouseListeners.iterator(); iter.hasNext(); ) {
          sb.append(iter.next()+", ");
        }
        sb.append("], KeyListeners num "+keyListeners.size()+" [");
        for (Iterator iter = keyListeners.iterator(); iter.hasNext(); ) {
          sb.append(iter.next()+", ");
        }
        sb.append("] ]");
        return sb.toString();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }
        this.title = title;
    }

    public void setUndecorated(boolean value) {
        undecorated = value;
    }

    public boolean isUndecorated(boolean fullscreen) {
        return 0 != parentWindowHandle || undecorated || fullscreen ;
    }

    public boolean isUndecorated() {
        return 0 != parentWindowHandle || undecorated || fullscreen ;
    }

    public void requestFocus() {
    }

    //
    // NativeWindow impl
    //

    /** Recursive and blocking lockSurface() implementation */
    public int lockSurface() {
        // We leave the ToolkitLock lock to the specializtion's discretion, 
        // ie the implicit JAWTWindow in case of AWTWindow
        if(isDestroyed() || !isNativeWindowValid()) {
            return LOCK_SURFACE_NOT_READY;
        }
        surfaceLock.lock();
        screen.getDisplay().lockDisplay();
        return LOCK_SUCCESS;
    }

    /** Recursive and unblocking unlockSurface() implementation */
    public void unlockSurface() throws NativeWindowException {
        surfaceLock.unlock( new Runnable() {
                                public void run() {
                                    screen.getDisplay().unlockDisplay();
                                }
                            } );
        // We leave the ToolkitLock unlock to the specializtion's discretion, 
        // ie the implicit JAWTWindow in case of AWTWindow
    }

    public boolean isSurfaceLocked() {
        return surfaceLock.isLocked();
    }

    public Thread getSurfaceLockOwner() {
        return surfaceLock.getOwner();
    }

    public Exception getLockedStack() {
        return surfaceLock.getLockedStack();
    }

    /** 
     * <p>
     * destroys the window and children and releases
     * windowing related resources.<br></p>
     * <p>
     * all other resources and states are kept intact,
     * ie listeners, parent handles and size, position etc.<br></p>
     *
     * @see #destroy(boolean)
     * @see #invalidate()
     */
    public final void destroy() {
        destroy(false);
    }

    class DestroyAction implements Runnable {
        boolean deep;
        public DestroyAction(boolean deep) {
            this.deep = deep;
        }
        public void run() {
            windowLock();
            try {
                if(DEBUG_WINDOW_EVENT) {
                    System.err.println("Window.destroy(deep: "+deep+") START "+Thread.currentThread()+", "+this);
                }

                // Childs first ..
                ArrayList listeners = null;
                synchronized(childWindows) {
                    listeners = childWindows;
                }
                for(Iterator i = listeners.iterator(); i.hasNext(); ) {
                    NativeWindow nw = (NativeWindow) i.next();
                    if(nw instanceof Window) {
                        ((Window)nw).destroy(deep);
                    } else {
                        nw.destroy();
                    }
                }

                // Now us ..
                if(deep) {
                    synchronized(childWindows) {
                        childWindows = new ArrayList();
                    }
                    synchronized(surfaceUpdatedListeners) {
                        surfaceUpdatedListeners = new ArrayList();
                    }
                    synchronized(windowListeners) {
                        windowListeners = new ArrayList();
                    }
                    synchronized(mouseListeners) {
                        mouseListeners = new ArrayList();
                    }
                    synchronized(keyListeners) {
                        keyListeners = new ArrayList();
                    }
                }
                Display dpy = null;
                if( null != screen && 0 != windowHandle ) {
                    Screen scr = screen;
                    dpy = (null!=screen) ? screen.getDisplay() : null;
                    closeNative();
                }
                invalidate(deep);
                if(deep) {
                    if(null!=screen) {
                        screen.destroy();
                    }
                    if(null!=dpy) {
                        dpy.destroy();
                    }
                }
                if(DEBUG_WINDOW_EVENT) {
                    System.err.println("Window.destroy(deep: "+deep+") END "+Thread.currentThread()+", "+this);
                }
            } finally {
                windowUnlock();
            }
        }
    }

    /** 
     * @param deep If true, all resources, ie listeners, parent handles, size, position 
     * and the referenced NEWT screen and display, will be destroyed as well. Be aware that if you call
     * this method with deep = true, you will not be able to regenerate the Window.
     * @see #destroy()
     * @see #invalidate(boolean)
     */
    public void destroy(boolean deep) {
        if(!isDestroyed()) {
            runOnEDTIfAvail(true, new DestroyAction(deep));
        }
    }

    /** 
     * <p>
     * render all native window information invalid,
     * as if the native window was destroyed.<br></p>
     * <p>
     * all other resources and states are kept intact,
     * ie listeners, parent handles and size, position etc.<br></p>
     *
     * @see #destroy()
     * @see #destroy(boolean)
     * @see #invalidate(boolean)
     */
    public void invalidate() {
        invalidate(false);
    }

    /**
     * @param deep if false only the native window handle is invalidated, otherwise all
     * states (references and properties) are reset. Be aware that if you call
     * this method with deep = true, you will not be able to regenerate the Window.
     * @see #invalidate()
     * @see #destroy()
     * @see #destroy(boolean)
     */
    public void invalidate(boolean deep) {
        windowLock();
        try{
            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                String msg = new String("!!! Window Invalidate(deep: "+deep+") "+Thread.currentThread());
                System.err.println(msg);
                //Exception e = new Exception(msg);
                //e.printStackTrace();
            }
            windowHandle = 0;
            visible=false;
            fullscreen=false;

            if(deep) {
                screen   = null;
                parentWindowHandle = 0;
                parentNativeWindow = null;
                caps = null;

                // Default position and dimension will be re-set immediately by user
                width  = 128;
                height = 128;
                x=0;
                y=0;
            }
        } finally {
            windowUnlock();
        }
    }

    /** @return true if the native window handle is valid and ready to operate */
    public boolean isNativeWindowValid() {
        return 0 != windowHandle ;
    }

    public boolean isDestroyed() {
        return null == screen ;
    }

    public boolean surfaceSwap() { 
        return false;
    }

    public long getDisplayHandle() {
        return screen.getDisplay().getHandle();
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

    public AbstractGraphicsConfiguration getGraphicsConfiguration() {
        return config;
    }

    /**
     * Returns the width of the client area of this window
     * @return width of the client area
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the client area of this window
     * @return height of the client area
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the insets for this native window (the difference between the
     * size of the toplevel window with the decorations and the client area).
     * 
     * @return insets for this platform window
     */
    // this probably belongs to NativeWindow interface
    public Insets getInsets() {
        return new Insets(0,0,0,0);
    }

    /** Returns the most inner Window instance.<br> 
        Currently only {@link com.jogamp.newt.opengl.GLWindow}
        has an aggregation to an inner Window instance.
     */
    public Window getInnerWindow() {
        return this;
    }

    /** If this Window actually wraps one from another toolkit such as
        the AWT, this will return a non-null value. */
    public Object getWrappedWindow() {
        return null;
    }

    //
    // Additional methods
    //

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    private boolean handleDestroyNotify = true;

    /** If the implementation is capable of detecting a device change
        return true and clear the status/reason of the change. */
    public boolean hasDeviceChanged() {
        return false;
    }

    /**
     * If set to true, the default value, this NEWT Window implementation will 
     * handle the destruction (ie {@link #destroy()} call) within {@link #windowDestroyNotify()} implementation.<br>
     * If set to false, it's up to the caller/owner to handle destruction within {@link #windowDestroyNotify()}.
     */
    public void setHandleDestroyNotify(boolean b) {
        handleDestroyNotify = b;
    }

    protected void windowDestroyNotify() {
        if(DEBUG_WINDOW_EVENT) {
            System.err.println("Window.windowDestroyNotify START "+Thread.currentThread());
        }

        sendWindowEvent(WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY);

        if(handleDestroyNotify && !isDestroyed()) {
            destroy();
        }

        if(DEBUG_WINDOW_EVENT) {
            System.err.println("Window.windowDestroyeNotify END "+Thread.currentThread());
        }
    }

    protected void windowDestroyed() {
        if(DEBUG_WINDOW_EVENT) {
            System.err.println("Window.windowDestroyed "+Thread.currentThread());
        }
        invalidate();
    }

    protected boolean reparentWindowImpl() {
        // default implementation, no native reparenting support
        return false;
    }

    /**
     * Change this window's parent window.<br>
     * <P>
     * In case the old parent is not null and a Window, 
     * this window is removed from it's list of children.<br>
     * In case the new parent is not null and a Window, 
     * this window is added to it's list of children.<br></P>
     *
     * @param newParent the new parent NativeWindow. If null, this Window becomes a top level window.
     * @param newScreen if not null and this window handle is not yet set
     *                  this Screen is being used.
     */
    public void reparentWindow(NativeWindow newParent, Screen newScreen) {
        windowLock();
        try{
            if ( 0 == windowHandle && null != newScreen ) {
                screen = newScreen;
            }
            long newParentHandle = 0 ;
            if(null!=newParent) {
                newParentHandle = getNativeWindowHandle(newParent);
                if ( 0 == newParentHandle ) {
                    return; // bail out .. not ready yet
                }
            }

            if(DEBUG_IMPLEMENTATION) {
                System.err.println("reparent: START ("+Thread.currentThread()+") windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle)+" -> "+toHexString(newParentHandle)+", visible "+visible+", parentNativeWindow "+(null!=parentNativeWindow));
            }

            if(null!=parentNativeWindow && parentNativeWindow instanceof Window) {
                ((Window)parentNativeWindow).getInnerWindow().removeChild(this);
            }
            parentNativeWindow = newParent;
            if(parentNativeWindow instanceof Window) {
                ((Window)parentNativeWindow).getInnerWindow().addChild(this);
            }

            if(newParentHandle != parentWindowHandle) {
                parentWindowHandle = newParentHandle;
                if(0!=parentWindowHandle) {
                    // reset position to 0/0 within parent space
                    // FIXME .. cache position ?
                    x = 0;
                    y = 0;
                }
                if(!reparentWindowImpl()) {
                    parentWindowHandle = 0;

                    // do it the hard way .. reconstruction with setVisible(true)
                    if( 0 != windowHandle ) {
                        destroy(false);
                    }
                }
            }

            if(DEBUG_IMPLEMENTATION) {
                System.err.println("reparentWindow: END ("+Thread.currentThread()+") windowHandle "+toHexString(windowHandle)+", visible: "+visible+", parentWindowHandle "+toHexString(parentWindowHandle)+", parentNativeWindow "+(null!=parentNativeWindow));
            }
        } finally {
            windowUnlock();
        }
    }

    class VisibleAction implements Runnable {
        boolean visible;
        public VisibleAction(boolean visible) {
            this.visible = visible;
        }
        public void run() {
            windowLock();
            try{
                if( !isDestroyed() ) {
                    ArrayList listeners = null;
                    synchronized(childWindows) {
                        listeners = childWindows;
                    }
                    if(!visible && listeners.size()>0) {
                        for(Iterator i = listeners.iterator(); i.hasNext(); ) {
                            NativeWindow nw = (NativeWindow) i.next();
                            if(nw instanceof Window) {
                                ((Window)nw).setVisible(false);
                            }
                        }
                    }
                    if(0==windowHandle && visible) { 
                        Window.this.visible = visible;
                        if( 0<width*height ) {
                            createNative();
                        }
                    } else if(Window.this.visible != visible) {
                        Window.this.visible = visible;
                        if(0 != windowHandle) {
                            setVisibleImpl(visible);
                        }
                    }
                    if(0!=windowHandle && visible && listeners.size()>0) {
                        for(Iterator i = listeners.iterator(); i.hasNext(); ) {
                            NativeWindow nw = (NativeWindow) i.next();
                            if(nw instanceof Window) {
                                ((Window)nw).setVisible(true);
                            }
                        }
                    }
                }

                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window setVisible: END ("+Thread.currentThread()+") "+x+"/"+y+" "+width+"x"+height+", fs "+fullscreen+", windowHandle "+toHexString(windowHandle)+", visible: "+Window.this.visible);
                }
            } finally {
                windowUnlock();
            }
        }
    }

    /**
     * <p>
     * <code>setVisible</code> makes the window and children visible if <code>visible</code> is true,
     * otherwise the window and children becomes invisible.<br></p>
     * <p>
     * The <code>setVisible(true)</code> is responsible to actual create the native window.<br></p>
     * <p>
     * Zero size semantics are respected, see {@link #setSize(int,int)}:<br>
     * <pre>
     * if ( 0 == windowHandle && visible ) { 
     *      this.visible = visible;
     *      if( 0<width*height ) {
     *         createNative();
     *      } 
     * } else if ( this.visible != visible ) {
     *      this.visible = visible;
     *      setNativeSizeImpl();
     * }
     * </pre></p>
     * <p>
     * In case this window is a child window and a parent {@link javax.media.nativewindow.NativeWindow} is being used,<br>
     * the parent's {@link javax.media.nativewindow.NativeWindow} handle is retrieved via {@link javax.media.nativewindow.NativeWindow#getWindowHandle()}.<br>
     * If this action fails, ie if the parent {@link javax.media.nativewindow.NativeWindow} is not valid yet,<br>
     * no native window is created yet and <code>setVisible(true)</code> shall be repeated when it is.<br></p>
     */
    public void setVisible(boolean visible) {
        if(DEBUG_IMPLEMENTATION) {
            String msg = new String("Window setVisible: START ("+Thread.currentThread()+") "+x+"/"+y+" "+width+"x"+height+", fs "+fullscreen+", windowHandle "+toHexString(windowHandle)+", visible: "+this.visible+" -> "+visible+", parentWindowHandle "+toHexString(parentWindowHandle)+", parentNativeWindow "+(null!=parentNativeWindow));
            //System.err.println(msg);
            Exception ee = new Exception(msg);
            ee.printStackTrace();
        }
        if(!isDestroyed()) {
            runOnEDTIfAvail(true, new VisibleAction(visible));
        }
    }
    protected abstract void setVisibleImpl(boolean visible);

    /**
     * Sets the size of the client area of the window, excluding decorations
     * Total size of the window will be
     * {@code width+insets.left+insets.right, height+insets.top+insets.bottom}<br>
     * <p>
     * Zero size semantics are respected, see {@link #setVisible(boolean)}:<br>
     * <pre>
     * if ( 0 != windowHandle && 0>=width*height && visible ) {
     *      setVisible(false);
     * } else if ( 0 == windowHandle && 0<width*height && visible ) {
     *      setVisible(true);
     * } else {
     *      // as expected ..
     * }
     * </pre></p>
     * <p>
     * This call is ignored if in fullscreen mode.<br></p>
     *
     * @param width of the client area of the window
     * @param height of the client area of the window
     */
    public void setSize(int width, int height) {
        int visibleAction = 0; // 1 invisible, 2 visible
        windowLock();
        try{
            if(DEBUG_IMPLEMENTATION) {
                String msg = new String("Window setSize: START "+this.width+"x"+this.height+" -> "+width+"x"+height+", fs "+fullscreen+", windowHandle "+toHexString(windowHandle)+", visible "+visible);
                //System.err.println(msg);
                Exception e = new Exception(msg);
                e.printStackTrace();
            }
            if (width != this.width || this.height != height) {
                if(!fullscreen) {
                    nfs_width=width;
                    nfs_height=height;
                    if ( 0 != windowHandle && 0>=width*height && visible ) {
                        visibleAction=1; // invisible
                        this.width = 0;
                        this.height = 0;
                    } else if ( 0 == windowHandle && 0<width*height && visible ) {
                        visibleAction = 2; // visible
                        this.width = width;
                        this.height = height;
                    } else if ( 0 != windowHandle ) {
                        // this width/height will be set by windowChanged, called by X11
                        setSizeImpl(width, height);
                    } else {
                        this.width = width;
                        this.height = height;
                    }
                }
            }
            if(DEBUG_IMPLEMENTATION) {
                System.err.println("Window setSize: END "+this.width+"x"+this.height+", visibleAction "+visibleAction);
            }
        } finally {
            windowUnlock();
        }
        if(visibleAction>0) {
            setVisible( ( 1 == visibleAction ) ? false : true );
        }
    }
    protected abstract void setSizeImpl(int width, int height);

    /**
     * Sets the location of the top left corner of the window, including
     * decorations (so the client area will be placed at
     * {@code x+insets.left,y+insets.top}.<br>
     *
     * This call is ignored if in fullscreen mode.<br>
     *
     * @param x coord of the top left corner
     * @param y coord of the top left corner
     */
    public void setPosition(int x, int y) {
        windowLock();
        try{
            if(DEBUG_IMPLEMENTATION) {
                System.err.println("Window setPosition: "+this.x+"/"+this.y+" -> "+x+"/"+y+", fs "+fullscreen+", windowHandle "+toHexString(windowHandle));
            }
            if ( this.x != x || this.y != y ) {
                if(!fullscreen) {
                    nfs_x=x;
                    nfs_y=y;
                    if(0!=windowHandle) {
                        // this x/y will be set by windowChanged, called by X11
                        setPositionImpl(x, y);
                    } else {
                        this.x = x;
                        this.y = y;
                    }
                }
            }
        } finally {
            windowUnlock();
        }
    }
    protected abstract void setPositionImpl(int x, int y);

    public boolean setFullscreen(boolean fullscreen) {
        windowLock();
        try{
            if(0!=windowHandle && this.fullscreen!=fullscreen) {
                int x,y,w,h;
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
                    System.err.println("X11Window fs: "+fullscreen+" "+x+"/"+y+" "+w+"x"+h+", "+isUndecorated());
                }
                this.fullscreen = setFullscreenImpl(fullscreen, x, y, w, h);
            }
            return this.fullscreen;
        } finally {
            windowUnlock();
        }
    }
    protected abstract boolean setFullscreenImpl(boolean fullscreen, int x, int y, int widht, int height);

    //
    // Child Window Management
    // 

    private ArrayList childWindows = new ArrayList();

    protected void removeChild(NativeWindow win) {
        synchronized(childWindows) {
            ArrayList newChildWindows = (ArrayList) childWindows.clone();
            newChildWindows.remove(win);
            childWindows = newChildWindows;
        }
    }

    protected void addChild(NativeWindow win) {
        if (win == null) {
            return;
        }
        synchronized(childWindows) {
            ArrayList newChildWindows = (ArrayList) childWindows.clone();
            newChildWindows.add(win);
            childWindows = newChildWindows;
        }
    }

    // 
    // Generic Event Support
    //

    public void sendEvent(NEWTEvent e) {
        if(e instanceof WindowEvent) {
            sendWindowEvent((WindowEvent)e);
        } else if(e instanceof KeyEvent) {
            sendKeyEvent((KeyEvent)e);
        } else if(e instanceof MouseEvent) {
            sendMouseEvent((MouseEvent)e);
        } else if(e instanceof PaintEvent) {
            sendPaintEvent((PaintEvent)e);
        }
    }

    //
    // SurfaceUpdatedListener Support
    //

    private ArrayList surfaceUpdatedListeners = new ArrayList();

    public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        if(l == null) {
            return;
        }
        synchronized(surfaceUpdatedListeners) {
            ArrayList newSurfaceUpdatedListeners = (ArrayList) surfaceUpdatedListeners.clone();
            newSurfaceUpdatedListeners.add(l);
            surfaceUpdatedListeners = newSurfaceUpdatedListeners;
        }
    }

    public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        if (l == null) {
            return;
        }
        synchronized(surfaceUpdatedListeners) {
            ArrayList newSurfaceUpdatedListeners = (ArrayList) surfaceUpdatedListeners.clone();
            newSurfaceUpdatedListeners.remove(l);
            surfaceUpdatedListeners = newSurfaceUpdatedListeners;
        }
    }

    public void removeAllSurfaceUpdatedListener() {
        synchronized(surfaceUpdatedListeners) {
            surfaceUpdatedListeners = new ArrayList();
        }
    }

    public SurfaceUpdatedListener[] getSurfaceUpdatedListener() {
        synchronized(surfaceUpdatedListeners) {
            return (SurfaceUpdatedListener[]) surfaceUpdatedListeners.toArray();
        }
    }

    public void surfaceUpdated(Object updater, NativeWindow window, long when) { 
        ArrayList listeners = null;
        synchronized(surfaceUpdatedListeners) {
            listeners = surfaceUpdatedListeners;
        }
        for(Iterator i = listeners.iterator(); i.hasNext(); ) {
            SurfaceUpdatedListener l = (SurfaceUpdatedListener) i.next();
            l.surfaceUpdated(updater, window, when);
        }
    }

    //
    // MouseListener/Event Support
    //

    public void addMouseListener(MouseListener l) {
        if(l == null) {
            return;
        }
        synchronized(mouseListeners) {
            ArrayList newMouseListeners = (ArrayList) mouseListeners.clone();
            newMouseListeners.add(l);
            mouseListeners = newMouseListeners;
        }
    }

    public void removeMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        synchronized(mouseListeners) {
            ArrayList newMouseListeners = (ArrayList) mouseListeners.clone();
            newMouseListeners.remove(l);
            mouseListeners = newMouseListeners;
        }
    }

    public MouseListener[] getMouseListeners() {
        synchronized(mouseListeners) {
            return (MouseListener[]) mouseListeners.toArray();
        }
    }

    private ArrayList mouseListeners = new ArrayList();
    private int  mouseButtonPressed = 0; // current pressed mouse button number
    private long lastMousePressed = 0; // last time when a mouse button was pressed
    private int  lastMouseClickCount = 0; // last mouse button click count
    public  static final int ClickTimeout = 300;

    /** Be aware that this method synthesizes the events: MouseClicked and MouseDragged */
    protected void sendMouseEvent(int eventType, int modifiers,
                                  int x, int y, int button, int rotation) {
        if(x<0||y<0||x>=width||y>=height) {
            return; // .. invalid ..
        }
        if(DEBUG_MOUSE_EVENT) {
            System.err.println("sendMouseEvent: "+MouseEvent.getEventTypeString(eventType)+
                               ", mod "+modifiers+", pos "+x+"/"+y+", button "+button);
        }
        if(button<0||button>MouseEvent.BUTTON_NUMBER) {
            throw new NativeWindowException("Invalid mouse button number" + button);
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
            mouseButtonPressed=button;
            e = new MouseEvent(eventType, this, when,
                               modifiers, x, y, lastMouseClickCount, button, 0);
        } else if(MouseEvent.EVENT_MOUSE_RELEASED==eventType) {
            e = new MouseEvent(eventType, this, when,
                               modifiers, x, y, lastMouseClickCount, button, 0);
            if(when-lastMousePressed<ClickTimeout) {
                eClicked = new MouseEvent(MouseEvent.EVENT_MOUSE_CLICKED, this, when,
                                          modifiers, x, y, lastMouseClickCount, button, 0);
            } else {
                lastMouseClickCount=0;
                lastMousePressed=0;
            }
            mouseButtonPressed=0;
        } else if(MouseEvent.EVENT_MOUSE_MOVED==eventType) {
            if (mouseButtonPressed>0) {
                e = new MouseEvent(MouseEvent.EVENT_MOUSE_DRAGGED, this, when,
                                   modifiers, x, y, 1, mouseButtonPressed, 0);
            } else {
                e = new MouseEvent(eventType, this, when,
                                   modifiers, x, y, 0, button, 0);
            }
        } else if(MouseEvent.EVENT_MOUSE_WHEEL_MOVED==eventType) {
            e = new MouseEvent(eventType, this, when, modifiers, x, y, 0, button, rotation);
        } else {
            e = new MouseEvent(eventType, this, when, modifiers, x, y, 0, button, 0);
        }
        sendMouseEvent(e);
        if(null!=eClicked) {
            if(DEBUG_MOUSE_EVENT) {
                System.err.println("sendMouseEvent: synthesized MOUSE_CLICKED event");
            }
            sendMouseEvent(eClicked);
        }
    }

    protected void sendMouseEvent(MouseEvent e) {
        if(DEBUG_MOUSE_EVENT) {
            System.err.println("sendMouseEvent: event:         "+e);
        }

        ArrayList listeners = null;
        synchronized(mouseListeners) {
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
                    break;
                case MouseEvent.EVENT_MOUSE_MOVED:
                    l.mouseMoved(e);
                    break;
                case MouseEvent.EVENT_MOUSE_DRAGGED:
                    l.mouseDragged(e);
                    break;
                case MouseEvent.EVENT_MOUSE_WHEEL_MOVED:
                    l.mouseWheelMoved(e);
                    break;
                default:
                    throw new NativeWindowException("Unexpected mouse event type " + e.getEventType());
            }
        }
    }

    //
    // KeyListener/Event Support
    //

    public void addKeyListener(KeyListener l) {
        if(l == null) {
            return;
        }
        synchronized(keyListeners) {
            ArrayList newKeyListeners = (ArrayList) keyListeners.clone();
            newKeyListeners.add(l);
            keyListeners = newKeyListeners;
        }
    }

    public void removeKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        synchronized(keyListeners) {
            ArrayList newKeyListeners = (ArrayList) keyListeners.clone();
            newKeyListeners.remove(l);
            keyListeners = newKeyListeners;
        }
    }

    public KeyListener[] getKeyListeners() {
        synchronized(keyListeners) {
            return (KeyListener[]) keyListeners.toArray();
        }
    }

    private ArrayList keyListeners = new ArrayList();

    protected void sendKeyEvent(int eventType, int modifiers, int keyCode, char keyChar) {
        sendKeyEvent(new KeyEvent(eventType, this, System.currentTimeMillis(),
                                  modifiers, keyCode, keyChar) );
    }

    protected void sendKeyEvent(KeyEvent e) {
        if(DEBUG_KEY_EVENT) {
            System.err.println("sendKeyEvent: "+e);
        }
        ArrayList listeners = null;
        synchronized(keyListeners) {
            listeners = keyListeners;
        }
        for(Iterator i = listeners.iterator(); i.hasNext(); ) {
            KeyListener l = (KeyListener) i.next();
            switch(e.getEventType()) {
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
                    throw new NativeWindowException("Unexpected key event type " + e.getEventType());
            }
        }
    }

    //
    // WindowListener/Event Support
    //

    private ArrayList windowListeners = new ArrayList();

    public void addWindowListener(WindowListener l) {
        if(l == null) {
            return;
        }
        synchronized(windowListeners) {
            ArrayList newWindowListeners = (ArrayList) windowListeners.clone();
            newWindowListeners.add(l);
            windowListeners = newWindowListeners;
        }
    }

    public void removeWindowListener(WindowListener l) {
        if (l == null) {
            return;
        }
        synchronized(windowListeners) {
            ArrayList newWindowListeners = (ArrayList) windowListeners.clone();
            newWindowListeners.remove(l);
            windowListeners = newWindowListeners;
        }
    }

    public WindowListener[] getWindowListeners() {
        synchronized(windowListeners) {
            return (WindowListener[]) windowListeners.toArray();
        }
    }

    protected void sendWindowEvent(int eventType) {
        sendWindowEvent( new WindowEvent(eventType, this, System.currentTimeMillis()) );
    }

    protected void sendWindowEvent(WindowEvent e) {
        if(DEBUG_WINDOW_EVENT) {
            System.err.println("sendWindowEvent: "+e);
        }
        ArrayList listeners = null;
        synchronized(windowListeners) {
            listeners = windowListeners;
        }
        for(Iterator i = listeners.iterator(); i.hasNext(); ) {
            WindowListener l = (WindowListener) i.next();
            switch(e.getEventType()) {
                case WindowEvent.EVENT_WINDOW_RESIZED:
                    l.windowResized(e);
                    break;
                case WindowEvent.EVENT_WINDOW_MOVED:
                    l.windowMoved(e);
                    break;
                case WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY:
                    l.windowDestroyNotify(e);
                    break;
                case WindowEvent.EVENT_WINDOW_GAINED_FOCUS:
                    l.windowGainedFocus(e);
                    break;
                case WindowEvent.EVENT_WINDOW_LOST_FOCUS:
                    l.windowLostFocus(e);
                    break;
                default:
                    throw 
                        new NativeWindowException("Unexpected window event type "
                                                  + e.getEventType());
            }
        }
    }


    //
    // PaintListener/Event Support
    //

    private ArrayList paintListeners = new ArrayList();

    public void addPaintListener(PaintListener l) {
        if(l == null) {
            return;
        }
        synchronized(paintListeners) {
            ArrayList newPaintListeners = (ArrayList) paintListeners.clone();
            newPaintListeners.add(l);
            paintListeners = newPaintListeners;
        }
    }

    public void removePaintListener(PaintListener l) {
        if (l == null) {
            return;
        }
        synchronized(paintListeners) {
            ArrayList newPaintListeners = (ArrayList) paintListeners.clone();
            newPaintListeners.remove(l);
            paintListeners = newPaintListeners;
        }
    }

    protected void sendPaintEvent(int eventType, int x, int y, int w, int h) {
        sendPaintEvent( new PaintEvent(eventType, this, System.currentTimeMillis(), x, y, w, h) );
    }

    protected void sendPaintEvent(PaintEvent e) {
        ArrayList listeners = null;
        synchronized(paintListeners) {
            listeners = paintListeners;
        }
        for(Iterator i = listeners.iterator(); i.hasNext(); ) {
            PaintListener l = (PaintListener) i.next();
            l.exposed(e);
        }
    }

    //
    // Reflection helper ..
    //

    private static Class[] getCustomConstructorArgumentTypes(Class windowClass) {
        Class[] argTypes = null;
        try {
            Method m = windowClass.getDeclaredMethod("getCustomConstructorArgumentTypes", new Class[] {});
            argTypes = (Class[]) m.invoke(null, null);
        } catch (Throwable t) {}
        return argTypes;
    }

    private static int verifyConstructorArgumentTypes(Class[] types, Object[] args) {
        if(types.length != args.length) {
            return -1;
        }
        for(int i=0; i<args.length; i++) {
            if(!types[i].isInstance(args[i])) {
                return i;
            }
        }
        return args.length;
    }

    private static String getArgsStrList(Object[] args) {
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<args.length; i++) {
            sb.append(args[i].getClass());
            if(i<args.length) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private static String getTypeStrList(Class[] types) {
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<types.length; i++) {
            sb.append(types[i]);
            if(i<types.length) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private RecursiveToolkitLock surfaceLock = new RecursiveToolkitLock();
    private RecursiveToolkitLock windowLock = new RecursiveToolkitLock();

    private static final boolean TRACE_LOCK = false;

    protected final void windowLock() {
        getInnerWindow().windowLock.lock();
        if(TRACE_LOCK) {
            Exception e = new Exception("WINDOW LOCK SET: R "+getInnerWindow().windowLock.getRecursionCount()+", "+getInnerWindow().windowLock);
            e.printStackTrace();
        }
    }
    protected final void windowUnlock() {
        getInnerWindow().windowLock.unlock();
        if(TRACE_LOCK) {
            Exception e = new Exception("WINDOW LOCK FREE: R "+getInnerWindow().windowLock.getRecursionCount()+", "+getInnerWindow().windowLock);
            e.printStackTrace();
        }
    }
    protected final boolean windowIsLocked() {
        return getInnerWindow().windowLock.isLocked();
    }
    protected final void shouldNotCallThis() {
        throw new NativeWindowException("Should not call this");
    }
}

