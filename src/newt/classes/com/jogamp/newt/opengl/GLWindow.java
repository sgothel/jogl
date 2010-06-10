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

package com.jogamp.newt.opengl;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.nativewindow.impl.RecursiveToolkitLock;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.jogamp.opengl.impl.GLDrawableHelper;
import java.util.*;

/**
 * An implementation of {@link Window} which is customized for OpenGL
 * use, and which implements the {@link javax.media.opengl.GLAutoDrawable} interface.
 * <P>
 * This implementation does not make the OpenGL context current<br>
 * before calling the various input EventListener callbacks (MouseListener, KeyListener,
 * etc.).<br>
 * This design decision is made to favor a more performant and simplified
 * implementation, as well as the event dispatcher shall be allowed
 * not having a notion about OpenGL.
 * <p>
 */
public class GLWindow extends Window implements GLAutoDrawable {
    private Window window;
    private boolean runPumpMessages;

    /**
     * Constructor. Do not call this directly -- use {@link #create()} instead.
     */
    protected GLWindow(Window window) {
        this.window = window;
        this.window.setHandleDestroyNotify(false);
        this.runPumpMessages = ( null == getScreen().getDisplay().getEDTUtil() ) ;
        window.addWindowListener(new WindowAdapter() {
                public void windowResized(WindowEvent e) {
                    sendReshape = true;
                }

                public void windowDestroyNotify(WindowEvent e) {
                    sendDestroy = true;
                }
            });
    }

    /** Creates a new GLWindow attaching the given window - not owning the Window. */
    public static GLWindow create(Window window) {
        return create(null, window, null, false);
    }

    /** Creates a new GLWindow attaching a new native child Window of the given <code>parentNativeWindow</code>
        with the given GLCapabilities - owning the Window */
    public static GLWindow create(NativeWindow parentNativeWindow, GLCapabilities caps) {
        return create(parentNativeWindow, null, caps, false);
    }

    /** Creates a new GLWindow attaching a new decorated Window on the local display, screen 0, with a
        dummy visual ID and given GLCapabilities - owning the window */
    public static GLWindow create(GLCapabilities caps) {
        return create(null, null, caps, false);
    }

    /** Creates a new GLWindow attaching a new Window on the local display, screen 0, with a
        dummy visual ID and given GLCapabilities - owning the window */
    public static GLWindow create(GLCapabilities caps, boolean undecorated) {
        return create(null, null, caps, undecorated);
    }

    /** Either or: window (prio), or caps and undecorated (2nd choice) */
    private static GLWindow create(NativeWindow parentNativeWindow, Window window, 
                                   GLCapabilities caps,
                                   boolean undecorated) {
        if (window == null) {
            if (caps == null) {
                caps = new GLCapabilities(null); // default ..
            }
            window = NewtFactory.createWindow(parentNativeWindow, caps, undecorated);
        }

        return new GLWindow(window);
    }
    
    public boolean isNativeWindowValid() {
        return (null!=window)?window.isNativeWindowValid():false;
    }

    public boolean isDestroyed() {
        return (null!=window)?window.isDestroyed():true;
    }

    public final Window getInnerWindow() {
        return window.getInnerWindow();
    }

    public final Object getWrappedWindow() {
        return window.getWrappedWindow();
    }

    /** 
     * EXPERIMENTAL<br> 
     * Enable or disables running the {@link Display#pumpMessages} in the {@link #display()} call.<br>
     * The default behavior is to run {@link Display#pumpMessages}.<P>
     *
     * The idea was that in a single threaded environment with one {@link Display} and many {@link Window}'s,
     * a performance benefit was expected while disabling the implicit {@link Display#pumpMessages} and
     * do it once via {@link GLWindow#runCurrentThreadPumpMessage()} <br>
     * This could not have been verified. No measurable difference could have been recognized.<P>
     *
     * Best performance has been achieved with one GLWindow per thread.<br> 
     *
     * Enabling local pump messages while using the EDT, 
     * {@link com.jogamp.newt.NewtFactory#setUseEDT(boolean)},
     * will result in an exception.
     *
     * @deprecated EXPERIMENTAL, semantic is about to be removed after further verification.
     */
    public void setRunPumpMessages(boolean onoff) {
        if( onoff && null!=getScreen().getDisplay().getEDTUtil() ) {
            throw new GLException("GLWindow.setRunPumpMessages(true) - Can't do with EDT on");
        }
        runPumpMessages = onoff;
    }

    protected void createNativeImpl() {
        shouldNotCallThis();
    }

    protected void closeNative() {
        shouldNotCallThis();
    }

    class DisposeAction implements Runnable {
        public void run() {
            // Lock: Covered by DestroyAction ..
            helper.dispose(GLWindow.this);
        }
    }
    private DisposeAction disposeAction = new DisposeAction();

    class DestroyAction implements Runnable {
        boolean deep;
        public DestroyAction(boolean deep) {
            this.deep = deep;
        }
        public void run() {
            // Lock: Have to cover whole workflow (dispose all, context, drawable and window)
            windowLock();
            try {
                if(null==window || window.isDestroyed()) {
                    return; // nop
                }
                if(Window.DEBUG_WINDOW_EVENT || window.DEBUG_IMPLEMENTATION) {
                    Exception e1 = new Exception("GLWindow.destroy("+deep+") "+Thread.currentThread()+", start: "+GLWindow.this);
                    e1.printStackTrace();
                }

                if ( null != context && context.isCreated() && null != drawable && drawable.isRealized() ) {
                    // Catch dispose GLExceptions by GLEventListener, just 'print' them
                    // so we can continue with the destruction.
                    try {
                        helper.invokeGL(drawable, context, disposeAction, null);
                    } catch (GLException gle) {
                        gle.printStackTrace();
                    }
                }

                if (context != null && null != drawable && drawable.isRealized() ) {
                    context.destroy();
                    context = null;
                }
                if (drawable != null) {
                    drawable.setRealized(false);
                    drawable = null;
                }

                if(null!=window) {
                    window.destroy(deep);
                }

                if(deep) {
                    helper=null;
                }
                if(Window.DEBUG_WINDOW_EVENT || window.DEBUG_IMPLEMENTATION) {
                    System.out.println("GLWindow.destroy("+deep+") "+Thread.currentThread()+", fin: "+GLWindow.this);
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
     */
    public void destroy(boolean deep) {
        if(!isDestroyed()) {
            runOnEDTIfAvail(true, new DestroyAction(deep));
        }
    }

    public boolean getPerfLogEnabled() { return perfLog; }

    public void enablePerfLog(boolean v) {
        perfLog = v;
    }

    protected void setVisibleImpl(boolean visible) {
        shouldNotCallThis();
    }

    public void reparentWindow(NativeWindow newParent, Screen newScreen) {
        window.reparentWindow(newParent, newScreen);
    }

    class VisibleAction implements Runnable {
        boolean visible;
        public VisibleAction(boolean visible) {
            this.visible = visible;
        }
        public void run() {
            // Lock: Have to cover whole workflow (window, may do nativeCreation, drawable and context)
            windowLock();
            try{
                window.setVisible(visible);
                if (null == context && visible && 0 != window.getWindowHandle() && 0<getWidth()*getHeight()) {
                    NativeWindow nw;
                    if (getWrappedWindow() != null) {
                        nw = NativeWindowFactory.getNativeWindow(getWrappedWindow(), window.getGraphicsConfiguration());
                    } else {
                        nw = window;
                    }
                    GLCapabilities glCaps = (GLCapabilities) nw.getGraphicsConfiguration().getNativeGraphicsConfiguration().getChosenCapabilities();
                    if(null==factory) {
                        factory = GLDrawableFactory.getFactory(glCaps.getGLProfile());
                    }
                    if(null==drawable) {
                        drawable = factory.createGLDrawable(nw);
                    }    
                    drawable.setRealized(true);
                    context = drawable.createContext(null);
                    sendReshape = true; // ensure a reshape event is send ..
                }
            } finally {
                windowUnlock();
            }
        }
    }

    public void setVisible(boolean visible) {
        if(!isDestroyed()) {
            runOnEDTIfAvail(true, new VisibleAction(visible));
        }
    }

    public Capabilities getRequestedCapabilities() {
        return window.getRequestedCapabilities();
    }

    public NativeWindow getParentNativeWindow() {
        return window.getParentNativeWindow();
    }

    public Screen getScreen() {
        return window.getScreen();
    }

    public void setTitle(String title) {
        window.setTitle(title);
    }

    public String getTitle() {
        return window.getTitle();
    }

    public void setUndecorated(boolean value) {
        window.setUndecorated(value);
    }

    public boolean isUndecorated() {
        return window.isUndecorated();
    }

    public void requestFocus() {
        window.requestFocus();
    }

    public Insets getInsets() {
        return window.getInsets();
    }

    public void setSize(int width, int height) {
        window.setSize(width, height);
    }
    protected void setSizeImpl(int width, int height) {
        shouldNotCallThis();
    }

    public void setPosition(int x, int y) {
        window.setPosition(x, y);
    }
    protected void setPositionImpl(int x, int y) {
        shouldNotCallThis();
    }

    public boolean setFullscreen(boolean fullscreen) {
        return window.setFullscreen(fullscreen);
    }
    protected boolean setFullscreenImpl(boolean fullscreen, int x, int y, int w, int h) {
        shouldNotCallThis();
        return false;
    }

    public boolean isVisible() {
        return window.isVisible();
    }

    public int getX() {
        return window.getX();
    }

    public int getY() {
        return window.getY();
    }

    public int getWidth() {
        return window.getWidth();
    }

    public int getHeight() {
        return window.getHeight();
    }

    public boolean isFullscreen() {
        return window.isFullscreen();
    }

    public void sendEvent(NEWTEvent e) {
        window.sendEvent(e);
    }

    public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        window.addSurfaceUpdatedListener(l);
    }
    public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        window.removeSurfaceUpdatedListener(l);
    }
    public void removeAllSurfaceUpdatedListener() {
        window.removeAllSurfaceUpdatedListener();
    }
    public SurfaceUpdatedListener[] getSurfaceUpdatedListener() {
        return window.getSurfaceUpdatedListener();
    }
    public void surfaceUpdated(Object updater, NativeWindow window0, long when) { 
        window.surfaceUpdated(updater, window, when);
    }

    public void addMouseListener(MouseListener l) {
        window.addMouseListener(l);
    }

    public void removeMouseListener(MouseListener l) {
        window.removeMouseListener(l);
    }

    public MouseListener[] getMouseListeners() {
        return window.getMouseListeners();
    }

    public void addKeyListener(KeyListener l) {
        window.addKeyListener(l);
    }

    public void removeKeyListener(KeyListener l) {
        window.removeKeyListener(l);
    }

    public KeyListener[] getKeyListeners() {
        return window.getKeyListeners();
    }

    public void addWindowListener(WindowListener l) {
        window.addWindowListener(l);
    }

    public void removeWindowListener(WindowListener l) {
        window.removeWindowListener(l);
    }

    public WindowListener[] getWindowListeners() {
        return window.getWindowListeners();
    }

    public String toString() {
        return "NEWT-GLWindow[ \n\tHelper: "+helper+", \n\tDrawable: "+drawable + /** ", \n\tWindow: "+window+", \n\tFactory: "+factory+ */ "]";
    }

    //----------------------------------------------------------------------
    // OpenGL-related methods and state
    //

    private GLDrawableFactory factory;
    private GLDrawable drawable;
    private GLContext context;
    private GLDrawableHelper helper = new GLDrawableHelper();
    // To make reshape events be sent immediately before a display event
    private boolean sendReshape=false;
    private boolean sendDestroy=false;
    private boolean perfLog = false;

    public GLDrawableFactory getFactory() {
        return factory;
    }

    public void setContext(GLContext newCtx) {
        context = newCtx;
    }

    public GLContext getContext() {
        return context;
    }

    public GL getGL() {
        if (context == null) {
            return null;
        }
        return context.getGL();
    }

    public GL setGL(GL gl) {
        if (context != null) {
            context.setGL(gl);
            return gl;
        }
        return null;
    }

    public void addGLEventListener(GLEventListener listener) {
        helper.addGLEventListener(listener);
    }

    public void removeGLEventListener(GLEventListener listener) {
        helper.removeGLEventListener(listener);
    }

    public void display() {
        display(false);
    }

    public void display(boolean forceReshape) {
        if( null == window ) { return; }

        if( null == context && window.isVisible() ) {
            // retry native window and drawable/context creation 
            setVisible(true);
        }

        if( window.isNativeWindowValid() && null != context ) {
            if(runPumpMessages) {
                window.getScreen().getDisplay().pumpMessages();
            }
            if(sendDestroy || window.hasDeviceChanged() && GLAutoDrawable.SCREEN_CHANGE_ACTION_ENABLED) {
                destroy();
                sendDestroy=false;
            } else if ( window.isVisible() ) {
                if(forceReshape) {
                    sendReshape = true;
                }
                windowLock();
                try{
                    helper.invokeGL(drawable, context, displayAction, initAction);
                } finally {
                    windowUnlock();
                }
            }
        }
    }

    /** This implementation uses a static value */
    public void setAutoSwapBufferMode(boolean onOrOff) {
        helper.setAutoSwapBufferMode(onOrOff);
    }

    /** This implementation uses a static value */
    public boolean getAutoSwapBufferMode() {
        return helper.getAutoSwapBufferMode();
    }

    public void swapBuffers() {
        if(drawable!=null && context != null) {
            // Lock: Locked Surface/Window by MakeCurrent/Release
            if (context != GLContext.getCurrent()) {
                // Assume we should try to make the context current before swapping the buffers
                helper.invokeGL(drawable, context, swapBuffersAction, initAction);
            } else {
                drawable.swapBuffers();
            }
        }
    }

    class InitAction implements Runnable {
        public void run() {
            // Lock: Locked Surface/Window by MakeCurrent/Release
            helper.init(GLWindow.this);
            startTime = System.currentTimeMillis();
            curTime   = startTime;
            if(perfLog) {
                lastCheck  = startTime;
                totalFrames = 0; lastFrames = 0;
            }
        }
    }
    private InitAction initAction = new InitAction();

    class DisplayAction implements Runnable {
        public void run() {
            // Lock: Locked Surface/Window by display _and_ MakeCurrent/Release
            if (sendReshape) {
                int width = getWidth();
                int height = getHeight();
                getGL().glViewport(0, 0, width, height);
                helper.reshape(GLWindow.this, 0, 0, width, height);
                sendReshape = false;
            }

            helper.display(GLWindow.this);

            curTime = System.currentTimeMillis();
            totalFrames++;

            if(perfLog) {
                long dt0, dt1;
                lastFrames++;
                dt0 = curTime-lastCheck;
                if ( dt0 > 5000 ) {
                    dt1 = curTime-startTime;
                    System.out.println(dt0/1000 +"s: "+ lastFrames + "f, " + (lastFrames*1000)/dt0 + " fps, "+dt0/lastFrames+" ms/f; "+
                                       "total: "+ dt1/1000+"s, "+(totalFrames*1000)/dt1 + " fps, "+dt1/totalFrames+" ms/f");
                    lastCheck=curTime;
                    lastFrames=0;
                }
            }
        }
    }
    private DisplayAction displayAction = new DisplayAction();

    public long getStartTime()   { return startTime; }
    public long getCurrentTime() { return curTime; }
    public long getDuration()    { return curTime-startTime; }
    public int  getTotalFrames() { return totalFrames; }

    private long startTime = 0;
    private long curTime = 0;
    private long lastCheck  = 0;
    private int  totalFrames = 0, lastFrames = 0;

    class SwapBuffersAction implements Runnable {
        public void run() {
            drawable.swapBuffers();
        }
    }
    private SwapBuffersAction swapBuffersAction = new SwapBuffersAction();

    //----------------------------------------------------------------------
    // NativeWindow/Window methods
    //

    public int lockSurface() throws NativeWindowException {
        if(null!=drawable) return drawable.getNativeWindow().lockSurface();
        return window.lockSurface();
    }

    public void unlockSurface() {
        if(null!=drawable) drawable.getNativeWindow().unlockSurface();
        else window.unlockSurface();
    }

    public boolean isSurfaceLocked() {
        if(null!=drawable) return drawable.getNativeWindow().isSurfaceLocked();
        return window.isSurfaceLocked();
    }

    public Exception getLockedStack() {
        if(null!=drawable) return drawable.getNativeWindow().getLockedStack();
        return window.getLockedStack();
    }

    public boolean surfaceSwap() { 
        if(null!=drawable) return drawable.getNativeWindow().surfaceSwap();
        return super.surfaceSwap();
    }

    public long getWindowHandle() {
        if(null!=drawable) return drawable.getNativeWindow().getWindowHandle();
        return window.getWindowHandle();
    }

    public long getSurfaceHandle() {
        if(null!=drawable) return drawable.getNativeWindow().getSurfaceHandle();
        return window.getSurfaceHandle();
    }

    public AbstractGraphicsConfiguration getGraphicsConfiguration() {
        if(null!=drawable) return drawable.getNativeWindow().getGraphicsConfiguration();
        return window.getGraphicsConfiguration();
    }

    //----------------------------------------------------------------------
    // GLDrawable methods
    //

    public NativeWindow getNativeWindow() {
        return null!=drawable ? drawable.getNativeWindow() : null;
    }

    public long getHandle() {
        return null!=drawable ? drawable.getHandle() : 0;
    }

    //----------------------------------------------------------------------
    // GLDrawable methods that are not really needed
    //

    public GLContext createContext(GLContext shareWith) {
        return drawable.createContext(shareWith);
    }

    public void setRealized(boolean realized) {
    }

    public boolean isRealized() {
        return ( null != drawable ) ? drawable.isRealized() : false;
    }

    public GLCapabilities getChosenGLCapabilities() {
        if (drawable == null) {
            throw new GLException("No drawable yet");
        }

        return drawable.getChosenGLCapabilities();
    }

    public GLProfile getGLProfile() {
        if (drawable == null) {
            throw new GLException("No drawable yet");
        }

        return drawable.getGLProfile();
    }
}
