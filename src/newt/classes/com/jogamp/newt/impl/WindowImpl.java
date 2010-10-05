/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
   Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.newt.impl;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Display;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.*;
import com.jogamp.newt.util.*;

import com.jogamp.common.util.*;
import javax.media.nativewindow.*;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.impl.RecursiveToolkitLock;
import com.jogamp.newt.impl.OffscreenWindow;

import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Method;

public abstract class WindowImpl implements Window, NEWTEventConsumer
{
    public static final boolean DEBUG_TEST_REPARENT_INCOMPATIBLE = Debug.isPropertyDefined("newt.test.Window.reparent.incompatible", true);
    
    // Workaround for initialization order problems on Mac OS X
    // between native Newt and (apparently) Fmod -- if Fmod is
    // initialized first then the connection to the window server
    // breaks, leading to errors from deep within the AppKit
    public static void init(String type) {
        if (NativeWindowFactory.TYPE_MACOSX.equals(type)) {
            try {
                getWindowClass(type);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //
    // Construction Methods
    //

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

    public static WindowImpl create(String type, NativeWindow parentWindow, long parentWindowHandle, Screen screen, Capabilities caps) {
        try {
            Class windowClass;
            if(caps.isOnscreen()) {
                windowClass = getWindowClass(type);
            } else {
                windowClass = OffscreenWindow.class;
            }
            WindowImpl window = (WindowImpl) windowClass.newInstance();
            window.invalidate(true);
            window.parentWindow = parentWindow;
            window.parentWindowHandle = parentWindowHandle;
            window.screen = (ScreenImpl) screen;
            window.caps = (Capabilities)caps.clone();
            window.setUndecorated(0!=parentWindowHandle);
            return window;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new NativeWindowException(t);
        }
    }

    public static WindowImpl create(String type, Object[] cstrArguments, Screen screen, Capabilities caps) {
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
            WindowImpl window = (WindowImpl) ReflectionUtil.createInstance( windowClass, cstrArgumentTypes, cstrArguments ) ;
            window.invalidate(true);
            window.screen = (ScreenImpl) screen;
            window.caps = (Capabilities)caps.clone();
            return window;
        } catch (Throwable t) {
            throw new NativeWindowException(t);
        }
    }

    public static interface LifecycleHook {
        /** 
         * Invoked after Window setVisible, 
         * allows allocating resources depending on the native Window.
         * Called from EDT.
         */
        void setVisibleAction(boolean visible, boolean nativeWindowCreated);

        /** 
         * Invoked before Window destroy action, 
         * allows releasing of resources depending on the native Window.
         * Called from EDT.
         */
        void destroyAction(boolean unrecoverable);

        /** Only informal, when starting reparenting */
        void reparentActionPre();

        /** Only informal, when finishing reparenting */
        void reparentActionPost(int reparentActionType);
    }

    private LifecycleHook lifecycleHook = null;
    private RecursiveToolkitLock windowLock = new RecursiveToolkitLock();
    private long   windowHandle;
    private ScreenImpl screen;
    private boolean screenReferenced = false;
    private NativeWindow parentWindow;
    private long parentWindowHandle;

    protected AbstractGraphicsConfiguration config;
    protected Capabilities caps;
    protected boolean fullscreen, visible, hasFocus;
    protected int width, height, x, y;

    // non fullscreen dimensions ..
    protected int nfs_width, nfs_height, nfs_x, nfs_y;

    protected String title = "Newt Window";
    protected boolean undecorated = false;

    private final void destroyScreen() {
        screenReferenced = false;
        if(null!=screen) {
            screen.removeReference();
            screen = null;
        }
    }
    private final void setScreen(ScreenImpl newScreen) {
        if(screenReferenced) {
            screenReferenced = false;
            screen.removeReference();
        }
        screen = newScreen;
    }

    private boolean createNative() {
        if( null==screen || 0!=windowHandle || !visible ) {
            return 0 != windowHandle ;
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.createNative() START ("+getThreadName()+", "+this+")");
        }
        if( null != parentWindow && 
            NativeWindow.LOCK_SURFACE_NOT_READY >= parentWindow.lockSurface() ) {
                throw new NativeWindowException("Parent surface lock: not ready: "+parentWindow);
        }
        try {
            if(validateParentWindowHandle()) {
                if(!screenReferenced) {
                    screenReferenced = true;
                    screen.addReference();
                }
                createNativeImpl();
                setVisibleImpl(true);
            }
        } finally {
            if(null!=parentWindow) {
                parentWindow.unlockSurface();
            }
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.createNative() END ("+getThreadName()+", "+this+")");
        }
        return 0 != windowHandle ;
    }

    private boolean validateParentWindowHandle() {
        if(null!=parentWindow) {
            parentWindowHandle = getNativeWindowHandle(parentWindow);
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


    //----------------------------------------------------------------------
    // NativeWindow: Native implementation
    //

    protected int lockSurfaceImpl() { return LOCK_SUCCESS; }

    protected void unlockSurfaceImpl() { }

    //----------------------------------------------------------------------
    // Window: Native implementation
    //

    protected abstract void createNativeImpl();

    protected abstract void closeNativeImpl();

    protected abstract void requestFocusImpl(boolean reparented);

    protected abstract void setVisibleImpl(boolean visible);

    protected abstract void setSizeImpl(int width, int height);

    protected abstract void setPositionImpl(int x, int y);

    protected abstract void reconfigureWindowImpl(int x, int y, int width, int height);

    protected void setTitleImpl(String title) {}

    //----------------------------------------------------------------------
    // NativeWindow
    //

    public final int lockSurface() {
        // We leave the ToolkitLock lock to the specializtion's discretion,
        // ie the implicit JAWTWindow in case of AWTWindow

        // may throw RuntimeException if timed out while waiting for lock
        windowLock.lock();

        int res = lockSurfaceImpl();
        if(!isNativeValid()) {
            windowLock.unlock();
            res = LOCK_SURFACE_NOT_READY;
        }
        return res;
    }

    public final void unlockSurface() {
        // may throw RuntimeException if not locked
        windowLock.validateLocked();

        unlockSurfaceImpl();

        windowLock.unlock();
        // We leave the ToolkitLock unlock to the specializtion's discretion,
        // ie the implicit JAWTWindow in case of AWTWindow
    }

    public final boolean isSurfaceLockedByOtherThread() {
        return windowLock.isLockedByOtherThread();
    }

    public final boolean isSurfaceLocked() {
        return windowLock.isLocked();
    }

    public final Thread getSurfaceLockOwner() {
        return windowLock.getOwner();
    }

    public final Exception getSurfaceLockStack() {
        return windowLock.getLockedStack();
    }

    public final long getDisplayHandle() {
        return getScreen().getDisplay().getHandle();
    }

    public final int  getScreenIndex() {
        return getScreen().getIndex();
    }

    public AbstractGraphicsConfiguration getGraphicsConfiguration() {
        return config;
    }

    public final long getWindowHandle() {
        return windowHandle;
    }

    public long getSurfaceHandle() {
        return windowHandle; // default: return window handle
    }

    public boolean surfaceSwap() {
        return false;
    }

    //----------------------------------------------------------------------
    // Window
    //

    public final boolean isNativeValid() {
        return null != getScreen() && 0 != getWindowHandle() ;
    }

    public final boolean isValid() {
        return null != getScreen() ;
    }

    public final NativeWindow getParentNativeWindow() {
        return parentWindow;
    }

    public final Screen getScreen() {
        return screen;
    }

    public void setVisible(boolean visible) {
        if(DEBUG_IMPLEMENTATION) {
            String msg = new String("Window setVisible: START ("+getThreadName()+") "+x+"/"+y+" "+width+"x"+height+", fs "+fullscreen+", windowHandle "+toHexString(windowHandle)+", visible: "+this.visible+" -> "+visible+", parentWindowHandle "+toHexString(this.parentWindowHandle)+", parentWindow "+(null!=this.parentWindow)/*+", "+this*/);
            System.err.println(msg);
            //Exception ee = new Exception(msg);
            //ee.printStackTrace();
        }
        if(isValid()) {
            VisibleAction va = new VisibleAction(visible);
            runOnEDTIfAvail(true, va);
            if( va.getChanged() ) {
                sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout and repaint to listener
            }

        }
    }

    class VisibleAction implements Runnable {
        boolean visible;
        boolean nativeWindowCreated;
        boolean madeVisible;

        public VisibleAction(boolean visible) {
            this.visible = visible;
            this.nativeWindowCreated = false;
            this.madeVisible = false;
        }

        public final boolean getNativeWindowCreated() { return nativeWindowCreated; }
        public final boolean getBecameVisible() { return madeVisible; }
        public final boolean getChanged() { return nativeWindowCreated || madeVisible; }

        public void run() {
            windowLock.lock();
            try {
                if( isValid() ) {
                    if(!visible && childWindows.size()>0) {
                      synchronized(childWindowsLock) {
                        for(Iterator i = childWindows.iterator(); i.hasNext(); ) {
                            NativeWindow nw = (NativeWindow) i.next();
                            if(nw instanceof WindowImpl) {
                                ((WindowImpl)nw).setVisible(false);
                            }
                        }
                      }
                    }
                    if(0==windowHandle && visible) {
                        WindowImpl.this.visible = visible;
                        if( 0<width*height ) {
                            nativeWindowCreated = createNative();
                        }
                    } else if(WindowImpl.this.visible != visible) {
                        WindowImpl.this.visible = visible;
                        if(0 != windowHandle) {
                            setVisibleImpl(visible);
                            madeVisible = visible;
                        }
                    }

                    if(null!=lifecycleHook) {
                        lifecycleHook.setVisibleAction(visible, nativeWindowCreated);
                    }

                    if(0!=windowHandle && visible && childWindows.size()>0) {
                      synchronized(childWindowsLock) {
                        for(Iterator i = childWindows.iterator(); i.hasNext(); ) {
                            NativeWindow nw = (NativeWindow) i.next();
                            if(nw instanceof WindowImpl) {
                                ((WindowImpl)nw).setVisible(true);
                            }
                        }
                      }
                    }
                }

                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window setVisible: END ("+getThreadName()+") "+x+"/"+y+" "+width+"x"+height+", fs "+fullscreen+", windowHandle "+toHexString(windowHandle)+", visible: "+WindowImpl.this.visible+", nativeWindowCreated: "+nativeWindowCreated+", madeVisible: "+madeVisible);
                }

            } finally {
                windowLock.unlock();
            }
            getScreen().getDisplay().dispatchMessages(); // status up2date
        }
    }

    public void setSize(int width, int height) {
        int visibleAction = 0; // 1 invisible, 2 visible
        windowLock.lock();
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
            windowLock.unlock();
        }
        if(visibleAction>0) {
            setVisible( ( 1 == visibleAction ) ? false : true );
        }
    }

    public final void destroy() {
        destroy(false);
    }

    public void destroy(boolean unrecoverable) {
        if( isValid() ) {
            if(DEBUG_IMPLEMENTATION) {
                String msg = new String("Window.destroy(unrecoverable: "+unrecoverable+") START "+getThreadName()/*+", "+this*/);
                System.err.println(msg);
                //Exception ee = new Exception(msg);
                //ee.printStackTrace();
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
            windowLock.lock();
            try {
                if( !isValid() ) {
                    return; // nop
                }

                // Childs first ..
                synchronized(childWindowsLock) {
                  // avoid ConcurrentModificationException: parent -> child -> parent.removeChild(this)
                  ArrayList clonedChildWindows = (ArrayList) childWindows.clone(); 
                  while( clonedChildWindows.size() > 0 ) {
                    NativeWindow nw = (NativeWindow) clonedChildWindows.remove(0);
                    if(nw instanceof WindowImpl) {
                        ((WindowImpl)nw).sendWindowEvent(WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY);
                        if(unrecoverable) {
                            ((WindowImpl)nw).destroy(unrecoverable);
                        }
                    } else {
                        nw.destroy();
                    }
                  }
                }

                if(null!=lifecycleHook) {
                    lifecycleHook.destroyAction(unrecoverable);
                }

                // Now us ..
                if(unrecoverable) {
                    if(null!=parentWindow && parentWindow instanceof Window) {
                        ((Window)parentWindow).removeChild(WindowImpl.this);
                    }
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
                    System.err.println("Window.destroy(unrecoverable: "+unrecoverable+") END "+getThreadName()/*+", "+WindowImpl.this*/);
                }
            } finally {
                windowLock.unlock();
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
    public final void invalidate() {
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
        windowLock.lock();
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
            hasFocus = false;

            if(unrecoverable) {
                destroyScreen();
                parentWindowHandle = 0;
                parentWindow = null;
                caps = null;
                lifecycleHook = null;

                // Default position and dimension will be re-set immediately by user
                width  = 128;
                height = 128;
                x=0;
                y=0;
            }
        } finally {
            windowLock.unlock();
        }
    }

    class ReparentActionImpl implements Runnable, ReparentAction {
        NativeWindow newParentWindow;
        boolean forceDestroyCreate;
        int reparentAction;

        public ReparentActionImpl(NativeWindow newParentWindow, boolean forceDestroyCreate) {
            this.newParentWindow = newParentWindow;
            this.forceDestroyCreate = forceDestroyCreate;
            this.reparentAction = -1; // ensure it's set
        }

        public int getStrategy() {
            return reparentAction;
        }

        public void run() {
            boolean wasVisible;
            boolean displayChanged = false;

            windowLock.lock();
            try {
                wasVisible = isVisible();

                Window newParentWindowNEWT = null;
                if(newParentWindow instanceof Window) {
                    newParentWindowNEWT = (Window) newParentWindow;
                }

                long newParentWindowHandle = 0 ;

                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window.reparent: START ("+getThreadName()+") windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle)+", visible "+wasVisible+", old parentWindow: "+Display.hashCode(parentWindow)+", new parentWindow: "+Display.hashCode(newParentWindow)+", forceDestroyCreate "+forceDestroyCreate+", DEBUG_TEST_REPARENT_INCOMPATIBLE "+DEBUG_TEST_REPARENT_INCOMPATIBLE);
                }

                if(null!=newParentWindow) {
                    // Case: Child Window
                    newParentWindowHandle = getNativeWindowHandle(newParentWindow);
                    if(0 == newParentWindowHandle) {
                        // Case: Parent's native window not realized yet
                        if(null==newParentWindowNEWT) {
                            throw new NativeWindowException("Reparenting with non NEWT Window type only available after it's realized: "+newParentWindow);
                        }
                        // Destroy this window (handle screen + native) and use parent's Screen.
                        // It may be created properly when the parent is made visible.
                        destroy(false);
                        setScreen( (ScreenImpl) newParentWindowNEWT.getScreen() );
                        displayChanged = true;
                        reparentAction = ACTION_NATIVE_CREATION_PENDING;
                    } else if(newParentWindow != getParentNativeWindow()) {
                        // Case: Parent's native window realized and changed
                        if( !isNativeValid() ) {
                            // May create a new compatible Screen/Display and
                            // mark it for creation.
                            if(null!=newParentWindowNEWT) {
                                setScreen( (ScreenImpl) newParentWindowNEWT.getScreen() );
                            } else {
                                Screen newScreen = NewtFactory.createCompatibleScreen(newParentWindow, getScreen());
                                if( getScreen() != newScreen ) {
                                    // auto destroy on-the-fly created Screen/Display
                                    newScreen.setDestroyWhenUnused(true);
                                    setScreen( (ScreenImpl) newScreen );
                                    displayChanged = true;
                                }
                            }
                            reparentAction = ACTION_NATIVE_CREATION;
                        } else if ( DEBUG_TEST_REPARENT_INCOMPATIBLE || forceDestroyCreate ||
                                    !NewtFactory.isScreenCompatible(newParentWindow, getScreen()) ) {
                            // Destroy this window (handle screen + native) and
                            // may create a new compatible Screen/Display and
                            // mark it for creation.
                            destroy(false);
                            if(null!=newParentWindowNEWT) {
                                setScreen( (ScreenImpl) newParentWindowNEWT.getScreen() );
                            } else {
                                setScreen( (ScreenImpl) NewtFactory.createCompatibleScreen(newParentWindow, getScreen()) );
                                screen.setDestroyWhenUnused(true);
                            }
                            displayChanged = true;
                            reparentAction = ACTION_NATIVE_CREATION;
                        } else {
                            // Mark it for native reparenting
                            reparentAction = ACTION_NATIVE_REPARENTING;
                        }
                    } else {
                        // Case: Parent's native window realized and not changed
                        reparentAction = ACTION_UNCHANGED;
                    }
                } else {
                    // Case: Top Window
                    if( 0 == getParentWindowHandle() ) {
                        // Already Top Window
                        reparentAction = ACTION_UNCHANGED;
                    } else if ( DEBUG_TEST_REPARENT_INCOMPATIBLE || forceDestroyCreate ) {
                        // Destroy this window (handle screen + native) and
                        // keep Screen/Display and
                        // mark it for creation.
                        destroy(false);
                        reparentAction = ACTION_NATIVE_CREATION;
                    } else {
                        // Mark it for native reparenting
                        reparentAction = ACTION_NATIVE_REPARENTING;
                    }
                }
                parentWindowHandle = newParentWindowHandle;

                if ( ACTION_UNCHANGED > reparentAction ) {
                    throw new NativeWindowException("Internal Error: reparentAction not set");
                }

                if( ACTION_UNCHANGED == reparentAction ) {
                    if(DEBUG_IMPLEMENTATION) {
                        System.err.println("Window.reparent: NO CHANGE ("+getThreadName()+") windowHandle "+toHexString(windowHandle)+" new parentWindowHandle "+toHexString(newParentWindowHandle)+", visible "+wasVisible);
                    }
                    return;
                }

                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window.reparent: ACTION ("+getThreadName()+") windowHandle "+toHexString(windowHandle)+" new parentWindowHandle "+toHexString(newParentWindowHandle)+", reparentAction "+reparentAction+", visible "+wasVisible);
                }

                // rearrange window tree
                if(null!=parentWindow && parentWindow instanceof Window) {
                    ((Window)parentWindow).removeChild(WindowImpl.this);
                }
                parentWindow = newParentWindow;
                if(parentWindow instanceof Window) {
                    ((Window)parentWindow).addChild(WindowImpl.this);
                }

                if( ACTION_NATIVE_CREATION_PENDING == reparentAction ) {
                    return;
                }

                if( ACTION_NATIVE_REPARENTING == reparentAction ) {
                    if(0!=parentWindowHandle) {
                        // reset position to 0/0 within parent space
                        // FIXME .. cache position ?
                        x = 0;
                        y = 0;
                    }
                    DisplayImpl display = (DisplayImpl) screen.getDisplay();
                    display.dispatchMessages(); // status up2date
                    if(wasVisible) {
                        visible = false;
                        setVisibleImpl(false);
                        display.dispatchMessages(); // status up2date
                    }

                    // Lock parentWindow only during reparenting (attempt)
                    NativeWindow parentWindowLocked = null;
                    if( null != parentWindow ) {
                        parentWindowLocked = parentWindow;
                        if(NativeWindow.LOCK_SURFACE_NOT_READY >= parentWindowLocked.lockSurface() ) {
                            throw new NativeWindowException("Parent surface lock: not ready: "+parentWindow);
                        }
                    }
                    boolean ok = false;
                    try {
                        ok = reparentWindowImpl();
                    } finally {
                        if(null!=parentWindowLocked) {
                            parentWindowLocked.unlockSurface();
                        }
                    }

                    if(ok) {
                        display.dispatchMessages(); // status up2date
                        if(wasVisible) {
                            visible = true;
                            setVisibleImpl(true);
                            display.dispatchMessages(); // status up2date
                        }
                    } else {
                        // native reparent failed -> try creation
                        if(DEBUG_IMPLEMENTATION) {
                            System.err.println("Window.reparent: native reparenting failed ("+getThreadName()+") windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle)+" -> "+toHexString(newParentWindowHandle)+" - Trying recreation");
                        }
                        destroy(false);
                        reparentAction = ACTION_NATIVE_CREATION ;
                    }
                }

                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window.reparentWindow: END ("+getThreadName()+") windowHandle "+toHexString(windowHandle)+", visible: "+visible+", parentWindowHandle "+toHexString(parentWindowHandle)+", parentWindow "+ Display.hashCode(parentWindow));
                }
            } finally {
                windowLock.unlock();
            }

            if( ACTION_NATIVE_CREATION == reparentAction && wasVisible ) {
                // This may run on the the Display/Screen connection,
                // hence a new EDT task
                runOnEDTIfAvail(true, reparentActionRecreate);
            }
        }
    }

    class ReparentActionRecreate implements Runnable {
        public void run() {
            windowLock.lock();
            try {
                visible = true;
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Window.reparentWindow: ReparentActionRecreate ("+getThreadName()+") windowHandle "+toHexString(windowHandle)+", visible: "+visible+", parentWindowHandle "+toHexString(parentWindowHandle)+", parentWindow "+Display.hashCode(parentWindow));
                }
                setVisible(true); // native creation
            } finally {
                windowLock.unlock();
            }
        }
    }
    private ReparentActionRecreate reparentActionRecreate = new ReparentActionRecreate();

    public final int reparentWindow(NativeWindow newParent) {
        return reparentWindow(newParent, false);
    }

    public int reparentWindow(NativeWindow newParent, boolean forceDestroyCreate) {
        int reparentActionStrategy = ReparentAction.ACTION_INVALID;
        if(isValid()) {
            if(null!=lifecycleHook) {
                lifecycleHook.reparentActionPre();
            }
            try {
                ReparentActionImpl reparentAction = new ReparentActionImpl(newParent, forceDestroyCreate);
                runOnEDTIfAvail(true, reparentAction);
                reparentActionStrategy = reparentAction.getStrategy();
                if( isVisible() ) {
                    sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout and repaint to listener
                }
            } finally {
                if(null!=lifecycleHook) {
                    lifecycleHook.reparentActionPost(reparentActionStrategy);
                }
            }
        }
        return reparentActionStrategy;
    }

    public final Capabilities getChosenCapabilities() {
        return config.getNativeGraphicsConfiguration().getChosenCapabilities();
    }

    public final Capabilities getRequestedCapabilities() {
        return (Capabilities)caps.clone();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }
        this.title = title;
        if(0 != getWindowHandle()) {
            setTitleImpl(title);
        }
    }

    public void setUndecorated(boolean value) {
        if(this.undecorated != value) {
            undecorated = value;
            if( 0 != windowHandle ) {
                reconfigureWindowImpl(x, y, width, height);
                requestFocus();
            }
        }
    }

    public boolean isUndecorated(boolean fullscreen) {
        return 0 != getParentWindowHandle() || undecorated || fullscreen ;
    }

    public boolean isUndecorated() {
        return 0 != parentWindowHandle || undecorated || fullscreen ;
    }

    public void requestFocus() {
        enqueueRequestFocus(false); // FIXME: or shall we wait ?
    }

    public boolean hasFocus() {
        return hasFocus;
    }

    public Insets getInsets() {
        return new Insets(0,0,0,0);
    }

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final boolean isVisible() {
        return visible;
    }

    public final boolean isFullscreen() {
        return fullscreen;
    }


    //----------------------------------------------------------------------
    // Window
    //

    /**
     * If the implementation is capable of detecting a device change
     * return true and clear the status/reason of the change.
     */
    public boolean hasDeviceChanged() {
        return false;
    }

    public LifecycleHook getLifecycleHook() {
        return lifecycleHook;
    }

    public LifecycleHook setLifecycleHook(LifecycleHook hook) {
        LifecycleHook old = lifecycleHook;
        lifecycleHook = hook;
        return old;
    }

    /** If this Window actually wraps one from another toolkit such as
        the AWT, this will return a non-null value. */
    public Object getWrappedWindow() {
        return null;
    }

    /**
     * If set to true, the default value, this NEWT Window implementation will
     * handle the destruction (ie {@link #destroy()} call) within {@link #windowDestroyNotify()} implementation.<br>
     * If set to false, it's up to the caller/owner to handle destruction within {@link #windowDestroyNotify()}.
     */
    public void setHandleDestroyNotify(boolean b) {
        handleDestroyNotify = b;
    }

    //----------------------------------------------------------------------
    // WindowImpl
    //

    protected final long getParentWindowHandle() {
        return parentWindowHandle;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getName()+"[Config "+config+
                    "\n, "+screen+
                    "\n, ParentWindow "+parentWindow+
                    "\n, ParentWindowHandle "+toHexString(parentWindowHandle)+
                    "\n, WindowHandle "+toHexString(getWindowHandle())+
                    "\n, SurfaceHandle "+toHexString(getSurfaceHandle())+ " (lockedExt "+isSurfaceLockedByOtherThread()+")"+
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

    protected final void setWindowHandle(long handle) {
        windowHandle = handle;
    }

    public void runOnEDTIfAvail(boolean wait, final Runnable task) {
        Screen screen = getScreen();
        if(null==screen) {
            throw new RuntimeException("Null screen of inner class: "+this);
        }
        DisplayImpl d = (DisplayImpl) screen.getDisplay();
        d.runOnEDTIfAvail(wait, task);
    }

    class RequestFocusAction implements Runnable {
        public void run() {
            WindowImpl.this.requestFocusImpl(false);
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
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.focusAction() START - "+getThreadName()+", focusAction: "+focusAction+" - windowHandle "+toHexString(getWindowHandle()));
        }
        boolean res;
        if(null!=focusAction) {
            res = focusAction.run();
        } else {
            res = false;
        }
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.focusAction() END - "+getThreadName()+", focusAction: "+focusAction+" - windowHandle "+toHexString(getWindowHandle())+", res: "+res);
        }
        return res;
    }
    protected FocusRunnable focusAction = null;

    private boolean handleDestroyNotify = true;

    public void setPosition(int x, int y) {
        windowLock.lock();
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
            windowLock.unlock();
        }
    }

    public boolean setFullscreen(boolean fullscreen) {
        windowLock.lock();
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
                    System.err.println("X11Window fs: "+fullscreen+" "+x+"/"+y+" "+w+"x"+h+", "+isUndecorated()+", "+screen);
                }
                this.fullscreen = fullscreen;
                reconfigureWindowImpl(x, y, w, h);
                requestFocus();
            }
        } finally {
            windowLock.unlock();
        }
        if( isVisible() ) {
            sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout and repaint to listener
        }
        return this.fullscreen;
    }

    //----------------------------------------------------------------------
    // Child Window Management
    // 

    private ArrayList childWindows = new ArrayList();
    private Object childWindowsLock = new Object();

    public final void removeChild(NativeWindow win) {
        synchronized(childWindowsLock) {
            childWindows.remove(win);
        }
    }

    public final void addChild(NativeWindow win) {
        if (win == null) {
            return;
        }
        synchronized(childWindowsLock) {
            childWindows.add(win);
        }
    }

    //----------------------------------------------------------------------
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
        if(isValid()) {
            ((DisplayImpl)getScreen().getDisplay()).enqueueEvent(wait, event);
        }
    }

    public boolean consumeEvent(NEWTEvent e) {
        switch(e.getEventType()) {
            // special repaint treatment
            case WindowEvent.EVENT_WINDOW_REPAINT:
                // queue repaint event in case surface is locked, ie in operation
                if( isSurfaceLocked() ) {
                    // make sure only one repaint event is queued
                    if(!repaintQueued) {
                        repaintQueued=true;
                        if(DEBUG_IMPLEMENTATION) {
                            System.err.println("Window.consumeEvent: queued "+e);
                            // Exception ee = new Exception("Window.windowRepaint: "+e);
                            // ee.printStackTrace();
                        }
                        return false;
                    }
                    return true;
                }
                repaintQueued=false; // no repaint event queued
                break;

            // common treatment
            case WindowEvent.EVENT_WINDOW_RESIZED:
                // queue event in case surface is locked, ie in operation
                if( isSurfaceLocked() ) {
                    if(DEBUG_IMPLEMENTATION) {
                        System.err.println("Window.consumeEvent: queued "+e);
                        // Exception ee = new Exception("Window.windowRepaint: "+e);
                        // ee.printStackTrace();
                    }
                    return false;
                }
                break;
            default:
                break;
        }
        if(e instanceof WindowEvent) {
            consumeWindowEvent((WindowEvent)e);
        } else if(e instanceof KeyEvent) {
            consumeKeyEvent((KeyEvent)e);
        } else if(e instanceof MouseEvent) {
            consumeMouseEvent((MouseEvent)e);
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

    public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        addSurfaceUpdatedListener(-1, l);
    }

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


    public void addMouseListener(MouseListener l) {
        addMouseListener(-1, l);
    }

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

    public void addKeyListener(KeyListener l) {
        addKeyListener(-1, l);
    }

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

    public void addWindowListener(WindowListener l) {
        addWindowListener(-1, l);
    }

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

    public final void removeWindowListener(WindowListener l) {
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
            System.err.println("Window.focusChanged: ("+getThreadName()+"): "+focusGained+" - windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle));
        }
        hasFocus = focusGained;
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
            System.err.println("Window.sizeChanged: ("+getThreadName()+"): "+width+"x"+height+" -> "+newWidth+"x"+newHeight+" - windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle));
        }
        if(width != newWidth || height != newHeight) {
            width = newWidth;
            height = newHeight;
            if(!fullscreen) {
                nfs_width=width;
                nfs_height=height;
            }
            if(isNativeValid()) {
                sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
            }
        }
    }

    protected void positionChanged(int newX, int newY) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.positionChanged: ("+getThreadName()+"): "+x+"/"+y+" -> "+newX+"/"+newY+" - windowHandle "+toHexString(windowHandle)+" parentWindowHandle "+toHexString(parentWindowHandle));
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

    public void windowRepaint(int x, int y, int width, int height) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("Window.windowRepaint "+getThreadName()+" - "+x+"/"+y+" "+width+"x"+height);
        }
        if(0>width) {
            width=this.width;
        }
        if(0>height) {
            height=this.height;
        }

        NEWTEvent e = new WindowUpdateEvent(WindowEvent.EVENT_WINDOW_REPAINT, this, System.currentTimeMillis(), 
                                            new Rectangle(x, y, width, height));
        if(isNativeValid()) {
            doEvent(false, false, e);
        }
    }

    protected boolean reparentWindowImpl() {
        // default implementation, no native reparenting support
        return false;
    }

    protected int getWindowLockRecursionCount() {
        return windowLock.getRecursionCount();
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

