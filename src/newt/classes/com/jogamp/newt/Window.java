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
import com.jogamp.newt.util.*;
import com.jogamp.newt.impl.Debug;

import com.jogamp.common.util.*;
import javax.media.nativewindow.*;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.impl.RecursiveToolkitLock;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.lang.reflect.Method;

public abstract class Window implements NativeWindow, NEWTEventConsumer
{
    public static final boolean DEBUG_MOUSE_EVENT = Debug.debug("Window.MouseEvent");
    public static final boolean DEBUG_KEY_EVENT = Debug.debug("Window.KeyEvent");
    public static final boolean DEBUG_WINDOW_EVENT = Debug.debug("Window.WindowEvent");
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");
    public static final boolean DEBUG_TEST_REPARENT_INCOMPATIBLE = Debug.isPropertyDefined("newt.test.reparent.incompatible", true);
    
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

    protected Screen screen;
    protected boolean screenReferenced = false;

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

    private final boolean createNative() {
        if( null==screen || 0!=windowHandle || !visible ) {
            return 0 != windowHandle ;
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.createNative() START ("+getThreadName()+", "+this+")");
        }
        if(validateParentWindowHandle()) {
            if(!screenReferenced) {
                screenReferenced = true;
                screen.addReference();
            }
            createNativeImpl();
            setVisibleImpl(true);
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.createNative() END ("+getThreadName()+", "+this+")");
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
            boolean locked=false;
            try {
                if( NativeWindow.LOCK_SURFACE_NOT_READY < nativeWindow.lockSurface() ) {
                    locked=true;
                    handle = nativeWindow.getWindowHandle();
                    if(0==handle) {
                        throw new NativeWindowException("Parent native window handle is NULL, after succesful locking: "+nativeWindow);
                    }
                }
            } catch (NativeWindowException nwe) {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window.getNativeWindowHandle: not successful yet: "+nwe);
                }
            } finally {
                if(locked) {
                    nativeWindow.unlockSurface();
                }
            }
            if(DEBUG_IMPLEMENTATION) {
                System.err.println("Window.getNativeWindowHandle: locked "+locked+", "+nativeWindow);
            }
        }
        return handle;
    }

    public void runOnEDTIfAvail(boolean wait, final Runnable task) {
        Screen screen = getInnerWindow().getScreen();
        if(null==screen) {
            throw new RuntimeException("Null screen of inner class: "+this);
        }
        Display d  = screen.getDisplay();
        d.runOnEDTIfAvail(wait, task);
    }

    /**
     * Create native windowHandle, ie creates a new native invisible window.
     */
    protected abstract void createNativeImpl();

    protected abstract void closeNativeImpl();

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
                    "\n, WrappedWindow "+getWrappedWindow()+
                    "\n, ChildWindows "+childWindows.size());

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
        if(0 != windowHandle) {
            setTitleImpl(title);
        }
    }
    protected void setTitleImpl(String title) {}

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
        enqueueRequestFocus(false); // FIXME: or shall we wait ?
    }
    protected void requestFocusImpl() {}

    class RequestFocusAction implements Runnable {
        public void run() {
            Window.this.requestFocusImpl();
        }
    }
    RequestFocusAction requestFocusAction = new RequestFocusAction();

    public void enqueueRequestFocus(boolean wait) {
        runOnEDTIfAvail(wait, requestFocusAction);
    }

    /** 
     * May set to a {@link FocusRunnable}, {@link FocusRunnable#run()} before Newt requests the native focus.
     * This allows notifying a covered window toolkit like AWT that the focus is requested,
     * hence focus traversal can be made transparent.
     */
    public void setFocusAction(FocusRunnable focusAction) {
        this.focusAction = focusAction;
    }
    protected boolean focusAction() {
        if(null!=focusAction) {
            return focusAction.run();
        }
        return false;
    }
    protected FocusRunnable focusAction = null;

    public static interface FocusRunnable {
        /**
         * @return false if NEWT shall proceed requesting the focus,
         * true if NEWT shall not request the focus.
         */
        public boolean run();
    }

    //
    // NativeWindow impl
    //

    /** Recursive and blocking lockSurface() implementation */
    public int lockSurface() {
        // We leave the ToolkitLock lock to the specializtion's discretion, 
        // ie the implicit JAWTWindow in case of AWTWindow

        windowLock.lock();

        // if(windowLock.getRecursionCount() == 0) { // allow recursion to lock again, always
            if(!isNativeValid()) {
                windowLock.unlock();
                return LOCK_SURFACE_NOT_READY;
            }
        // }
        return LOCK_SUCCESS;
    }

    /** Recursive and unblocking unlockSurface() implementation */
    public void unlockSurface() throws NativeWindowException {
        windowLock.unlock();
        // We leave the ToolkitLock unlock to the specializtion's discretion, 
        // ie the implicit JAWTWindow in case of AWTWindow
    }

    public boolean isSurfaceLocked() {
        return windowLock.isLocked();
    }

    public Thread getSurfaceLockOwner() {
        return windowLock.getOwner();
    }

    public Exception getLockedStack() {
        return windowLock.getLockedStack();
    }

    /** 
     * <p>
     * destroys the window and children and releases
     * windowing related resources.<br></p>
     * <p>
     * all other resources and states are kept intact,
     * ie listeners, parent handles, size, position and Screen reference.<br></p>
     *
     * @see #destroy(boolean)
     * @see #invalidate()
     */
    public final void destroy() {
        destroy(false);
    }

    /** 
     * Destroys the Window and it's children.
     * @param unrecoverable If true, all resources, ie listeners, parent handles, 
     * size, position and reference to it's Screen will be destroyed as well. 
     * Otherwise you can recreate the window, via <code>setVisible(true)</code>.
     * @see #destroy()
     * @see #invalidate(boolean)
     * @see #setVisible(boolean)
     */
    public void destroy(boolean unrecoverable) {
        if(isValid()) {
            if(DEBUG_IMPLEMENTATION) {
                String msg = new String("Window.destroy(unrecoverable: "+unrecoverable+") START "+getThreadName()+", "+this);
                //System.err.println(msg);
                Exception ee = new Exception(msg);
                ee.printStackTrace();
            }
            runOnEDTIfAvail(true, new DestroyAction(unrecoverable));
        }
    }

    class DestroyAction implements Runnable {
        boolean unrecoverable;
        public DestroyAction(boolean unrecoverable) {
            this.unrecoverable = unrecoverable;
        }
        public void run() {
            windowLock();
            try {
                // Childs first ..
                synchronized(childWindowsLock) {
                  for(Iterator i = childWindows.iterator(); i.hasNext(); ) {
                    NativeWindow nw = (NativeWindow) i.next();
                    System.err.println("Window.destroy(unrecoverable: "+unrecoverable+") CHILD BEGIN");
                    if(nw instanceof Window) {
                        ((Window)nw).sendWindowEvent(WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY);
                        if(unrecoverable) {
                                ((Window)nw).destroy(unrecoverable);
                        }
                    } else {
                        nw.destroy();
                    }
                    System.err.println("Window.destroy(unrecoverable: "+unrecoverable+") CHILD END");
                  }
                }

                // Now us ..
                if(unrecoverable) {
                    synchronized(childWindowsLock) {
                        childWindows = new ArrayList();
                    }
                    synchronized(surfaceUpdatedListenersLock) {
                        surfaceUpdatedListeners = new ArrayList();
                    }
                    windowListeners = new ArrayList();
                    mouseListeners = new ArrayList();
                    keyListeners = new ArrayList();
                }
                if( null != screen && 0 != windowHandle ) {
                    closeNativeImpl();
                }
                invalidate(unrecoverable);
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window.destroy(unrecoverable: "+unrecoverable+") END "+getThreadName()+", "+Window.this);
                }
            } finally {
                windowUnlock();
            }
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
     * @param unrecoverable If true, all states, size, position, parent handles,
     * reference to it's Screen are reset.
     * Otherwise you can recreate the window, via <code>setVisible(true)</code>.
     * @see #invalidate()
     * @see #destroy()
     * @see #destroy(boolean)
     */
    protected void invalidate(boolean unrecoverable) {
        windowLock();
        try{
            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                String msg = new String("!!! Window Invalidate(unrecoverable: "+unrecoverable+") "+getThreadName());
                System.err.println(msg);
                // Exception e = new Exception(msg);
                // e.printStackTrace();
            }
            windowHandle = 0;
            visible = false;
            fullscreen = false;

            if(unrecoverable) {
                System.err.println("Window.invalidate: 1 "+screen);
                if(null!=screen) {
                    screenReferenced = false;
                    screen.removeReference();
                }
                screen = null;
                System.err.println("Window.invalidate: 2 "+screen);
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

    /** @return true if the native window handle is valid and ready to operate, ie
     *  if the native window has been created, otherwise false.
     *
     * @see #setVisible(boolean)
     * @see #destroy(boolean)
     */
    public boolean isNativeValid() {
        return null != screen && 0 != windowHandle ;
    }

    /** @return True if native window is valid, can be created or recovered.
    *   Otherwise false, ie this window is unrecoverable due to a <code>destroy(true)</code> call.
     *
     * @see #destroy(boolean)
     * @see #setVisible(boolean)
     */
    public boolean isValid() {
        return null != screen ;
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

    class ReparentAction implements Runnable {
        /** No native reparenting action */
        static final int ACTION_NONE               = 0;

        /** Change Window tree only */
        static final int ACTION_SOFT_REPARENTING   = 1;

        /** Native reparenting incl. Window tree */
        static final int ACTION_NATIVE_REPARENTING = 2;

        /** Native window creation after tree change - instead of reparenting. */
        static final int ACTION_NATIVE_CREATION    = 3;

        NativeWindow newParent;
        public ReparentAction(NativeWindow newParent) {
            this.newParent = newParent;
        }
        public void run() {
            windowLock();
            try{
                Window newParentWindow = null;
                if(newParent instanceof Window) {
                    newParentWindow = (Window) newParent;
                }

                int reparentAction = -1; // ensure it's set
                long newParentHandle = 0 ;

                if(null!=newParent) {
                    // Case: Child Window
                    newParentHandle = getNativeWindowHandle(newParent);
                    if(0 == newParentHandle) {
                        // Case: Parent's native window not realized yet
                        if(null==newParentWindow) {
                            throw new NativeWindowException("Parent not NEWT Window and not realized yet: "+newParent);
                        }
                        // Destroy this window (handle screen + native) and use parent's Screen.
                        // It may be created properly when the parent is made visible.
                        destroy(false);
                        screen = newParentWindow.getScreen();
                        reparentAction = ACTION_SOFT_REPARENTING;
                    } else if(newParent != parentNativeWindow) {
                        // Case: Parent's native window realized and changed
                        if( !isNativeValid() ) {
                            // May create a new compatible Screen/Display and
                            // mark it for creation.
                            if(null!=newParentWindow) {
                                screen = newParentWindow.getScreen();
                            } else {
                                Screen newScreen = NewtFactory.createCompatibleScreen(newParent, screen);
                                if( screen != newScreen ) {
                                    // auto destroy on-the-fly created Screen/Display
                                    newScreen.setDestroyWhenUnused(true);
                                    screen = newScreen;
                                }
                            }
                            reparentAction = ACTION_NATIVE_CREATION;
                        } else if ( DEBUG_TEST_REPARENT_INCOMPATIBLE || !NewtFactory.isScreenCompatible(newParent, screen) ) {
                            // Destroy this window (handle screen + native) and 
                            // may create a new compatible Screen/Display and
                            // mark it for creation.
                            destroy(false);
                            if(null!=newParentWindow) {
                                screen = newParentWindow.getScreen();
                            } else {
                                screen = NewtFactory.createCompatibleScreen(newParent, screen);
                                screen.setDestroyWhenUnused(true);
                            }
                            reparentAction = ACTION_NATIVE_CREATION;
                        } else {
                            // Mark it for native reparenting
                            reparentAction = ACTION_NATIVE_REPARENTING;
                        }
                    } else {
                        // Case: Parent's native window realized and not changed
                        reparentAction = ACTION_NONE;
                    }
                } else {
                    // Case: Top Window
                    if( 0 == parentWindowHandle ) {
                        // Already Top Window
                        reparentAction = ACTION_NONE;
                    } else {
                        // Mark it for native reparenting
                        reparentAction = ACTION_NATIVE_REPARENTING;
                    }
                }

                if ( ACTION_NONE > reparentAction ) {
                    throw new NativeWindowException("Internal Error: reparentAction not set");
                }

                if( ACTION_NONE == reparentAction ) {
                    if(DEBUG_IMPLEMENTATION) {
                        System.err.println("reparent: NO CHANGE ("+getThreadName()+") windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle)+" -> "+toHexString(newParentHandle)+", visible "+visible+", parentNativeWindow "+(null!=parentNativeWindow));
                    }
                    return;
                }

                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("reparent: START ("+getThreadName()+") windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle)+" -> "+toHexString(newParentHandle)+", reparentAction "+reparentAction+", visible "+visible+", parentNativeWindow "+(null!=parentNativeWindow));
                }

                // rearrange window tree
                if(null!=parentNativeWindow && parentNativeWindow instanceof Window) {
                    ((Window)parentNativeWindow).getInnerWindow().removeChild(Window.this);
                }
                parentNativeWindow = newParent;
                if(parentNativeWindow instanceof Window) {
                    ((Window)parentNativeWindow).getInnerWindow().addChild(Window.this);
                }

                if( ACTION_SOFT_REPARENTING == reparentAction ) {
                    return;
                }

                if( ACTION_NATIVE_REPARENTING == reparentAction ) {
                    Display display = screen.getDisplay();

                    parentWindowHandle = newParentHandle;
                    if(0!=parentWindowHandle) {
                        // reset position to 0/0 within parent space
                        // FIXME .. cache position ?
                        x = 0;
                        y = 0;
                    }
                    getScreen().getDisplay().dispatchMessages(); // status up2date
                    boolean wasVisible = isVisible();
                    if(wasVisible) {
                        Window.this.visible = false;
                        setVisibleImpl(false);
                        display.dispatchMessages(); // status up2date
                    }
                    boolean ok = reparentWindowImpl();
                    display.dispatchMessages(); // status up2date
                    if ( !ok ) {
                        // native reparent failed -> try creation
                        if(DEBUG_IMPLEMENTATION) {
                            System.err.println("reparent: native reparenting failed ("+getThreadName()+") windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle)+" -> "+toHexString(newParentHandle)+" - Trying recreation");
                        }
                        destroy(false);
                        Window.this.visible = wasVisible;
                        reparentAction = ACTION_NATIVE_CREATION ;
                    } else {
                        if(wasVisible) {
                            Window.this.visible = true;
                            setVisibleImpl(true);
                            requestFocusImpl();
                            display.dispatchMessages(); // status up2date
                        }
                    }
                }
                
                // not-else: re-entrance via reparentAction value change possible
                if( ACTION_NATIVE_CREATION == reparentAction ) {
                    if(isVisible()) {
                        setVisible(true); // native creation
                        screen.getDisplay().dispatchMessages(); // status up2date
                    }
                }

                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("reparentWindow: END ("+getThreadName()+") windowHandle "+toHexString(windowHandle)+", visible: "+visible+", parentWindowHandle "+toHexString(parentWindowHandle)+", parentNativeWindow "+(null!=parentNativeWindow));
                }
            } finally {
                windowUnlock();
            }
        }
    }

    /**
     * Change this window's parent window.<br>
     * <P>
     * In case the old parent is not null and a Window, 
     * this window is removed from it's list of children.<br>
     * In case the new parent is not null and a Window, 
     * this window is added to it's list of children.<br></P>
     *
     * @param newParent The new parent NativeWindow. If null, this Window becomes a top level window.
     */
    public void reparentWindow(NativeWindow newParent) {
        if(isValid()) {
            runOnEDTIfAvail(true, new ReparentAction(newParent)); 
            if( isVisible() ) {
                sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout to listener
                windowRepaint(0, 0, getWidth(), getHeight());
            }
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
                if( isValid() ) {
                    if(!visible && childWindows.size()>0) {
                      synchronized(childWindowsLock) {
                        for(Iterator i = childWindows.iterator(); i.hasNext(); ) {
                            NativeWindow nw = (NativeWindow) i.next();
                            if(nw instanceof Window) {
                                ((Window)nw).setVisible(false);
                            }
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
                    if(0!=windowHandle && visible && childWindows.size()>0) {
                      synchronized(childWindowsLock) {
                        for(Iterator i = childWindows.iterator(); i.hasNext(); ) {
                            NativeWindow nw = (NativeWindow) i.next();
                            if(nw instanceof Window) {
                                ((Window)nw).setVisible(true);
                            }
                        }
                      }
                    }
                }

                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window setVisible: END ("+getThreadName()+") "+x+"/"+y+" "+width+"x"+height+", fs "+fullscreen+", windowHandle "+toHexString(windowHandle)+", visible: "+Window.this.visible);
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
            String msg = new String("Window setVisible: START ("+getThreadName()+") "+x+"/"+y+" "+width+"x"+height+", fs "+fullscreen+", windowHandle "+toHexString(windowHandle)+", visible: "+this.visible+" -> "+visible+", parentWindowHandle "+toHexString(parentWindowHandle)+", parentNativeWindow "+(null!=parentNativeWindow));
            //System.err.println(msg);
            Exception ee = new Exception(msg);
            ee.printStackTrace();
        }
        if(isValid()) {
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
                System.err.println(msg);
                // Exception e = new Exception(msg);
                // e.printStackTrace();
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
                        // this width/height will be set by windowChanged, called by the native implementation
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
                    if(0!=parentWindowHandle) {
                        x=0;
                        y=0;
                    } else {
                        x = nfs_x;
                        y = nfs_y;
                    }
                    w = nfs_width;
                    h = nfs_height;
                }
                if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                    System.err.println("X11Window fs: "+fullscreen+" "+x+"/"+y+" "+w+"x"+h+", "+isUndecorated());
                }
                this.fullscreen = fullscreen;
                setFullscreenImpl(fullscreen, x, y, w, h);
            }
        } finally {
            windowUnlock();
        }
        if( isVisible() ) {
            windowRepaint(0, 0, getWidth(), getHeight());
        }
        return this.fullscreen;
    }
    protected abstract void setFullscreenImpl(boolean fullscreen, int x, int y, int widht, int height);

    //
    // Child Window Management
    // 

    private ArrayList childWindows = new ArrayList();
    private Object childWindowsLock = new Object();

    protected void removeChild(NativeWindow win) {
        synchronized(childWindowsLock) {
            childWindows.remove(win);
        }
    }

    protected void addChild(NativeWindow win) {
        if (win == null) {
            return;
        }
        synchronized(childWindowsLock) {
            childWindows.add(win);
        }
    }

    // 
    // Generic Event Support
    //
    private void doEvent(boolean enqueue, boolean wait, com.jogamp.newt.event.NEWTEvent event) {
        boolean done = false;

        if(!enqueue) {
            done = consumeEvent(event);
            wait = done; // don't wait if event can't be consumed now
        }

        if(!done) {
            enqueueEvent(wait, event);
        }
    }

    public void enqueueEvent(boolean wait, com.jogamp.newt.event.NEWTEvent event) {
        if(getInnerWindow().isValid()) {
            getInnerWindow().getScreen().getDisplay().enqueueEvent(wait, event);
        }
    }

    public boolean consumeEvent(NEWTEvent e) {
        switch(e.getEventType()) {
            case WindowEvent.EVENT_WINDOW_REPAINT:
                if( windowIsLocked() ) {
                  // make sure only one repaint event is queued
                  if(!repaintQueued) {
                      repaintQueued=true;
                      return false;
                  }
                  return true;
                }
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window.windowRepaint: "+e);
                    // Exception ee = new Exception("Window.windowRepaint: "+e);
                    // ee.printStackTrace();
                }
                repaintQueued=false; // no repaint event queued
                break;
            default:
                break;
        }
        if(e instanceof WindowEvent) {
            getInnerWindow().consumeWindowEvent((WindowEvent)e);
        } else if(e instanceof KeyEvent) {
            getInnerWindow().consumeKeyEvent((KeyEvent)e);
        } else if(e instanceof MouseEvent) {
            getInnerWindow().consumeMouseEvent((MouseEvent)e);
        } else {
            throw new NativeWindowException("Unexpected NEWTEvent type " + e);
        }
        return true;
    }
    protected boolean repaintQueued = false;

    //
    // SurfaceUpdatedListener Support
    //

    private ArrayList surfaceUpdatedListeners = new ArrayList();
    private Object surfaceUpdatedListenersLock = new Object();

    /** 
     * Appends the given {@link com.jogamp.newt.event.SurfaceUpdatedListener} to the end of 
     * the list.
     */
    public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        getInnerWindow().addSurfaceUpdatedListener(-1, l);
    }

    /** 
     * Inserts the given {@link com.jogamp.newt.event.SurfaceUpdatedListener} at the 
     * specified position in the list.<br>

     * @param index Position where the listener will be inserted. 
     *              Should be within (0 <= index && index <= size()).
     *              An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    public void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) 
        throws IndexOutOfBoundsException
    {
        if(l == null) {
            return;
        }
        synchronized(surfaceUpdatedListenersLock) {
            if(0>index) { 
                index = surfaceUpdatedListeners.size(); 
            }
            surfaceUpdatedListeners.add(index, l);
        }
    }

    public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        if (l == null) {
            return;
        }
        synchronized(surfaceUpdatedListenersLock) {
            surfaceUpdatedListeners.remove(l);
        }
    }

    public void removeAllSurfaceUpdatedListener() {
        synchronized(surfaceUpdatedListenersLock) {
            surfaceUpdatedListeners = new ArrayList();
        }
    }

    public SurfaceUpdatedListener getSurfaceUpdatedListener(int index) {
        synchronized(surfaceUpdatedListenersLock) {
            if(0>index) { 
                index = surfaceUpdatedListeners.size()-1; 
            }
            return (SurfaceUpdatedListener) surfaceUpdatedListeners.get(index);
        }
    }

    public SurfaceUpdatedListener[] getSurfaceUpdatedListeners() {
        synchronized(surfaceUpdatedListenersLock) {
            return (SurfaceUpdatedListener[]) surfaceUpdatedListeners.toArray();
        }
    }

    public void surfaceUpdated(Object updater, NativeWindow window, long when) { 
        synchronized(surfaceUpdatedListenersLock) {
          for(Iterator i = surfaceUpdatedListeners.iterator(); i.hasNext(); ) {
            SurfaceUpdatedListener l = (SurfaceUpdatedListener) i.next();
            l.surfaceUpdated(updater, window, when);
          }
        }
    }

    //
    // MouseListener/Event Support
    //
    private ArrayList mouseListeners = new ArrayList();
    private int  mouseButtonPressed = 0; // current pressed mouse button number
    private long lastMousePressed = 0; // last time when a mouse button was pressed
    private int  lastMouseClickCount = 0; // last mouse button click count
    public  static final int ClickTimeout = 300;

    public void sendMouseEvent(int eventType, int modifiers,
                               int x, int y, int button, int rotation) {
        doMouseEvent(false, false, eventType, modifiers, x, y, button, rotation);
    }
    public void enqueueMouseEvent(boolean wait, int eventType, int modifiers,
                                  int x, int y, int button, int rotation) {
        doMouseEvent(true, wait, eventType, modifiers, x, y, button, rotation);
    }
    private void doMouseEvent(boolean enqueue, boolean wait, int eventType, int modifiers,
                              int x, int y, int button, int rotation) {
        if(x<0||y<0||x>=width||y>=height) {
            return; // .. invalid ..
        }
        if(DEBUG_MOUSE_EVENT) {
            System.err.println("doMouseEvent: enqueue"+enqueue+", wait "+wait+", "+MouseEvent.getEventTypeString(eventType)+
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
        doEvent(enqueue, wait, e);
        if(null!=eClicked) {
            if(DEBUG_MOUSE_EVENT) {
                System.err.println("doMouseEvent: synthesized MOUSE_CLICKED event");
            }
            doEvent(enqueue, wait, eClicked);
        }
    }


    /** 
     * Appends the given {@link com.jogamp.newt.event.MouseListener} to the end of 
     * the list.
     */
    public void addMouseListener(MouseListener l) {
        getInnerWindow().addMouseListener(-1, l);
    }

    /** 
     * Inserts the given {@link com.jogamp.newt.event.MouseListener} at the 
     * specified position in the list.<br>

     * @param index Position where the listener will be inserted. 
     *              Should be within (0 <= index && index <= size()).
     *              An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    public void addMouseListener(int index, MouseListener l) {
        if(l == null) {
            return;
        }
        ArrayList clonedListeners = (ArrayList) mouseListeners.clone();
        if(0>index) { 
            index = clonedListeners.size(); 
        }
        clonedListeners.add(index, l);
        mouseListeners = clonedListeners;
    }

    public void removeMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        ArrayList clonedListeners = (ArrayList) mouseListeners.clone();
        clonedListeners.remove(l);
        mouseListeners = clonedListeners;
    }

    public MouseListener getMouseListener(int index) {
        ArrayList clonedListeners = (ArrayList) mouseListeners.clone();
        if(0>index) { 
            index = clonedListeners.size()-1; 
        }
        return (MouseListener) clonedListeners.get(index);
    }

    public MouseListener[] getMouseListeners() {
        return (MouseListener[]) mouseListeners.toArray();
    }

    protected void consumeMouseEvent(MouseEvent e) {
        if(DEBUG_MOUSE_EVENT) {
            System.err.println("consumeMouseEvent: event:         "+e);
        }

        for(Iterator i = mouseListeners.iterator(); i.hasNext(); ) {
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

    public void sendKeyEvent(int eventType, int modifiers, int keyCode, char keyChar) {
        consumeKeyEvent(new KeyEvent(eventType, this, System.currentTimeMillis(), modifiers, keyCode, keyChar) );
    }

    public void enqueueKeyEvent(boolean wait, int eventType, int modifiers, int keyCode, char keyChar) {
        enqueueEvent(wait, new KeyEvent(eventType, this, System.currentTimeMillis(), modifiers, keyCode, keyChar) );
    }

    /** 
     * Appends the given {@link com.jogamp.newt.event.KeyListener} to the end of 
     * the list.
     */
    public void addKeyListener(KeyListener l) {
        getInnerWindow().addKeyListener(-1, l);
    }

    /** 
     * Inserts the given {@link com.jogamp.newt.event.KeyListener} at the 
     * specified position in the list.<br>

     * @param index Position where the listener will be inserted. 
     *              Should be within (0 <= index && index <= size()).
     *              An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    public void addKeyListener(int index, KeyListener l) {
        if(l == null) {
            return;
        }
        ArrayList clonedListeners = (ArrayList) keyListeners.clone();
        if(0>index) { 
            index = clonedListeners.size();
        }
        clonedListeners.add(index, l);
        keyListeners = clonedListeners;
    }

    public void removeKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        ArrayList clonedListeners = (ArrayList) keyListeners.clone();
        clonedListeners.remove(l);
        keyListeners = clonedListeners;
    }

    public KeyListener getKeyListener(int index) {
        ArrayList clonedListeners = (ArrayList) keyListeners.clone();
        if(0>index) { 
            index = clonedListeners.size()-1;
        }
        return (KeyListener) clonedListeners.get(index);
    }

    public KeyListener[] getKeyListeners() {
        return (KeyListener[]) keyListeners.toArray();
    }

    private ArrayList keyListeners = new ArrayList();

    protected void consumeKeyEvent(KeyEvent e) {
        if(DEBUG_KEY_EVENT) {
            System.err.println("consumeKeyEvent: "+e);
        }
        for(Iterator i = keyListeners.iterator(); i.hasNext(); ) {
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
    public void sendWindowEvent(int eventType) {
        consumeWindowEvent( new WindowEvent(eventType, this, System.currentTimeMillis()) );
    }

    public void enqueueWindowEvent(boolean wait, int eventType) {
        enqueueEvent( wait, new WindowEvent(eventType, this, System.currentTimeMillis()) );
    }

    private ArrayList windowListeners = new ArrayList();

    /** 
     * Appends the given {@link com.jogamp.newt.event.WindowListener} to the end of 
     * the list.
     */
    public void addWindowListener(WindowListener l) {
        getInnerWindow().addWindowListener(-1, l);
    }

    /** 
     * Inserts the given {@link com.jogamp.newt.event.WindowListener} at the 
     * specified position in the list.<br>

     * @param index Position where the listener will be inserted. 
     *              Should be within (0 <= index && index <= size()).
     *              An index value of -1 is interpreted as the end of the list, size().
     * @param l The listener object to be inserted
     * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
     */
    public void addWindowListener(int index, WindowListener l) 
        throws IndexOutOfBoundsException
    {
        if(l == null) {
            return;
        }
        ArrayList clonedListeners = (ArrayList) windowListeners.clone();
        if(0>index) { 
            index = clonedListeners.size(); 
        }
        clonedListeners.add(index, l);
        windowListeners = clonedListeners;
    }

    public void removeWindowListener(WindowListener l) {
        if (l == null) {
            return;
        }
        ArrayList clonedListeners = (ArrayList) windowListeners.clone();
        clonedListeners.remove(l);
        windowListeners = clonedListeners;
    }

    public WindowListener getWindowListener(int index) {
        ArrayList clonedListeners = (ArrayList) windowListeners.clone();
        if(0>index) { 
            index = clonedListeners.size()-1; 
        }
        return (WindowListener) clonedListeners.get(index);
    }

    public WindowListener[] getWindowListeners() {
        return (WindowListener[]) windowListeners.toArray();
    }

    protected void consumeWindowEvent(WindowEvent e) {
        if(DEBUG_WINDOW_EVENT) {
            System.err.println("consumeWindowEvent: "+e);
        }
        for(Iterator i = windowListeners.iterator(); i.hasNext(); ) {
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
                case WindowEvent.EVENT_WINDOW_REPAINT:
                    l.windowRepaint((WindowUpdateEvent)e);
                    break;
                default:
                    throw 
                        new NativeWindowException("Unexpected window event type "
                                                  + e.getEventType());
            }
        }
    }

    /**
     * @param focusGained
     */
    protected void focusChanged(boolean focusGained) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.focusChanged: "+focusGained);
        }
        if (focusGained) {
            sendWindowEvent(WindowEvent.EVENT_WINDOW_GAINED_FOCUS);
        } else {
            sendWindowEvent(WindowEvent.EVENT_WINDOW_LOST_FOCUS);
        }
    }

    protected void visibleChanged(boolean visible) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.visibleChanged ("+getThreadName()+"): "+this.visible+" -> "+visible+" - windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle));
            // Exception e = new Exception("Window.visibleChanged ("+getThreadName()+"): "+this.visible+" -> "+visible+" - windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle));
            // e.printStackTrace();
        }
        this.visible = visible ;
    }

    protected void sizeChanged(int newWidth, int newHeight) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.sizeChanged: "+width+"x"+height+" -> "+newWidth+"x"+newHeight);
        }
        if(width != newWidth || height != newHeight) {
            width = newWidth;
            height = newHeight;
            if(!fullscreen) {
                nfs_width=width;
                nfs_height=height;
            }
            sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
        }
    }

    protected void positionChanged(int newX, int newY) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.positionChanged: "+x+"/"+y+" -> "+newX+"/"+newY);
        }
        if( 0==parentWindowHandle && ( x != newX || y != newY ) ) {
            x = newX;
            y = newY;
            if(!fullscreen) {
                nfs_x=x;
                nfs_y=y;
            }
            sendWindowEvent(WindowEvent.EVENT_WINDOW_MOVED);
        }
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
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.windowDestroyNotify START "+getThreadName());
        }

        enqueueWindowEvent(false, WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY);

        if(handleDestroyNotify && isValid()) {
            destroy();
        }

        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.windowDestroyeNotify END "+getThreadName());
        }
    }

    protected void windowDestroyed() {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.windowDestroyed "+getThreadName());
        }
        invalidate();
    }

    public boolean getPropagateRepaint() {
        return propagateRepaint;
    }
    public void setPropagateRepaint(boolean v) {
        propagateRepaint = v;
    }
    protected boolean propagateRepaint = true;

    public void windowRepaint(int x, int y, int width, int height) {
        if(!propagateRepaint) { 
            return; 
        }
        if(0>width) {
            width=this.width;
        }
        if(0>height) {
            height=this.height;
        }

        NEWTEvent e = new WindowUpdateEvent(WindowEvent.EVENT_WINDOW_REPAINT, this, System.currentTimeMillis(), 
                                            new Rectangle(x, y, width, height));
        doEvent(false, false, e);
    }

    protected boolean reparentWindowImpl() {
        // default implementation, no native reparenting support
        return false;
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

    protected RecursiveToolkitLock windowLock = new RecursiveToolkitLock();

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
    protected RecursiveToolkitLock getWindowLock() {
        return getInnerWindow().windowLock;
    }
    protected final void shouldNotCallThis() {
        throw new NativeWindowException("Should not call this");
    }

    public static String getThreadName() {
        return Display.getThreadName();
    }

    public static String toHexString(int hex) {
        return Display.toHexString(hex);
    }

    public static String toHexString(long hex) {
        return Display.toHexString(hex);
    }
}

