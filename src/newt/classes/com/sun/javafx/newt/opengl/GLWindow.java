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

package com.sun.javafx.newt.opengl;

import com.sun.javafx.newt.*;
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
    private static List/*GLWindow*/ glwindows = new ArrayList();

    private boolean ownerOfWinScrDpy;
    private Window window;
    private boolean runPumpMessages;

    /** Constructor. Do not call this directly -- use {@link
        create()} instead. */
    protected GLWindow(Window window, boolean ownerOfWinScrDpy) {
        this.ownerOfWinScrDpy = ownerOfWinScrDpy;
        this.window = window;
        this.window.setAutoDrawableClient(true);
        this.runPumpMessages = ( null == getScreen().getDisplay().getEDT() ) ;
        window.addWindowListener(new WindowListener() {
                public void windowResized(WindowEvent e) {
                    sendReshape = true;
                }

                public void windowMoved(WindowEvent e) {
                }

                public void windowGainedFocus(WindowEvent e) {
                }

                public void windowLostFocus(WindowEvent e) {
                }

                public void windowDestroyNotify(WindowEvent e) {
                    sendDestroy = true;
                }
            });

        List newglw = (List) ((ArrayList) glwindows).clone();
        newglw.add(this);
        glwindows=newglw;
    }

    /** Creates a new GLWindow on the local display, screen 0, with a
        dummy visual ID, and with the default GLCapabilities. */
    public static GLWindow create() {
        return create(null, null, false);
    }

    public static GLWindow create(boolean undecorated) {
        return create(null, null, undecorated);
    }

    /** Creates a new GLWindow referring to the given window. */
    public static GLWindow create(Window window) {
        return create(window, null, false);
    }
    public static GLWindow create(GLCapabilities caps) {
        return create(null, caps, false);
    }

    /** Creates a new GLWindow on the local display, screen 0, with a
        dummy visual ID, and with the given GLCapabilities. */
    public static GLWindow create(GLCapabilities caps, boolean undecorated) {
        return create(null, caps, undecorated);
    }

    /** Either or: window (prio), or caps and undecorated (2nd choice) */
    private static GLWindow create(Window window, 
                                   GLCapabilities caps,
                                   boolean undecorated) {
        Display display;
        boolean ownerOfWinScrDpy=false;
        if (window == null) {
            if (caps == null) {
                caps = new GLCapabilities(null); // default ..
            }
            ownerOfWinScrDpy = true;
            display = NewtFactory.createDisplay(null); // local display
            Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
            window = NewtFactory.createWindow(screen, caps, undecorated);
        }

        return new GLWindow(window, ownerOfWinScrDpy);
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
     * {@link com.sun.javafx.newt.NewtFactory#setUseEDT(boolean)},
     * will result in an exception.
     *
     * @deprecated EXPERIMENTAL, semantic is about to be removed after further verification.
     */
    public void setRunPumpMessages(boolean onoff) {
        if( onoff && null!=getScreen().getDisplay().getEDT() ) {
            throw new GLException("GLWindow.setRunPumpMessages(true) - Can't do with EDT on");
        }
        runPumpMessages = onoff;
    }

    protected void createNative(long parentWindowHandle, Capabilities caps) {
        shouldNotCallThis();
    }

    protected void closeNative() {
        shouldNotCallThis();
    }

    protected void dispose(boolean regenerate, boolean sendEvent) {
        if(Window.DEBUG_WINDOW_EVENT || window.DEBUG_IMPLEMENTATION) {
            Exception e1 = new Exception("GLWindow.dispose("+regenerate+") "+Thread.currentThread()+", 1");
            e1.printStackTrace();
        }

        if(sendEvent) {
            sendDisposeEvent();
        }

        if (context != null) {
            context.destroy();
        }
        if (drawable != null) {
            drawable.setRealized(false);
        }

        if(regenerate) {
            if(null==window) {
                throw new GLException("GLWindow.dispose(true): null window");
            }

            // recreate GLDrawable, to reflect the new graphics configurations
            NativeWindow nw;
            if (window.getWrappedWindow() != null) {
                nw = NativeWindowFactory.getNativeWindow(window.getWrappedWindow(), window.getGraphicsConfiguration());
            } else {
                nw = window;
            }
            drawable = factory.createGLDrawable(nw);
            drawable.setRealized(true);
            context = drawable.createContext(null);
            sendReshape = true; // ensure a reshape event is send ..
        }

        if(Window.DEBUG_WINDOW_EVENT || window.DEBUG_IMPLEMENTATION) {
            System.out.println("GLWindow.dispose("+regenerate+") "+Thread.currentThread()+", fin: "+this);
        }
    }

    public synchronized void destroy() {
        destroy(true);
    }

    /** @param sendDisposeEvent should be false in a [time,reliable] critical shutdown */
    public synchronized void destroy(boolean sendDisposeEvent) {
        if(Window.DEBUG_WINDOW_EVENT || window.DEBUG_IMPLEMENTATION) {
            Exception e1 = new Exception("GLWindow.destroy "+Thread.currentThread()+", 1: "+this);
            e1.printStackTrace();
        }

        List newglw = (List) ((ArrayList) glwindows).clone();
        newglw.remove(this);
        glwindows=newglw;

        dispose(false, sendDisposeEvent);

        if(null!=window) {
            if(ownerOfWinScrDpy) {
                window.destroy(true);
            }
        }

        drawable = null;
        context = null;
        window = null;

        if(Window.DEBUG_WINDOW_EVENT || window.DEBUG_IMPLEMENTATION) {
            System.out.println("GLWindow.destroy "+Thread.currentThread()+", fin: "+this);
        }
    }

    public boolean getPerfLogEnabled() { return perfLog; }

    public void enablePerfLog(boolean v) {
        perfLog = v;
    }

    public void setVisible(boolean visible) {
        if(Window.DEBUG_WINDOW_EVENT || window.DEBUG_IMPLEMENTATION) {
            System.out.println(Thread.currentThread()+" GLWindow.setVisible("+visible+") START ; isVisible "+this.visible+" ; has context "+(null!=context));
        }
        this.visible=visible;
        window.setVisible(visible);
        if (visible && context == null) {
            NativeWindow nw;
            if (window.getWrappedWindow() != null) {
                nw = NativeWindowFactory.getNativeWindow(window.getWrappedWindow(), window.getGraphicsConfiguration());
            } else {
                nw = window;
            }
            GLCapabilities glCaps = (GLCapabilities) nw.getGraphicsConfiguration().getNativeGraphicsConfiguration().getChosenCapabilities();
            factory = GLDrawableFactory.getFactory(glCaps.getGLProfile());
            drawable = factory.createGLDrawable(nw);
            drawable.setRealized(true);
            context = drawable.createContext(null);
            sendReshape = true; // ensure a reshape event is send ..
        }
        if(Window.DEBUG_WINDOW_EVENT || window.DEBUG_IMPLEMENTATION) {
            System.out.println(Thread.currentThread()+" GLWindow.setVisible("+visible+") END  ; has context "+(null!=context));
        }
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

    public void setSize(int width, int height) {
        window.setSize(width, height);
    }

    public void setPosition(int x, int y) {
        window.setPosition(x, y);
    }

    public Insets getInsets() {
        return window.getInsets();
    }

    public boolean setFullscreen(boolean fullscreen) {
        return window.setFullscreen(fullscreen);
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

    public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        window.addSurfaceUpdatedListener(l);
    }
    public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        window.removeSurfaceUpdatedListener(l);
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
        return "NEWT-GLWindow[ \n\tDrawable: "+drawable+", \n\tWindow: "+window+", \n\tHelper: "+helper+", \n\tFactory: "+factory+"]";
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
        if(window!=null && drawable!=null && context != null) {
            if(runPumpMessages) {
                window.getScreen().getDisplay().pumpMessages();
            }
            if(window.hasDeviceChanged() && GLAutoDrawable.SCREEN_CHANGE_ACTION_ENABLED) {
                dispose(true, true);
            }
            if (sendDestroy) {
                destroy();
                sendDestroy=false;
            } else {
                if(forceReshape) {
                    sendReshape = true;
                }
                helper.invokeGL(drawable, context, displayAction, initAction);
            }
        }
    }

    private void sendDisposeEvent() {
        if(drawable!=null && context != null) {
            helper.invokeGL(drawable, context, disposeAction, null);
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

    class DisposeAction implements Runnable {
        public void run() {
            helper.dispose(GLWindow.this);
        }
    }
    private DisposeAction disposeAction = new DisposeAction();

    class DisplayAction implements Runnable {
        public void run() {
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
    // GLDrawable methods
    //

    public NativeWindow getNativeWindow() {
        return null!=drawable ? drawable.getNativeWindow() : null;
    }

    public synchronized int lockSurface() throws NativeWindowException {
        if(null!=drawable) return drawable.getNativeWindow().lockSurface();
        return NativeWindow.LOCK_SURFACE_NOT_READY;
    }

    public synchronized void unlockSurface() {
        if(null!=drawable) drawable.getNativeWindow().unlockSurface();
        else throw new NativeWindowException("NEWT-GLWindow not locked");
    }

    public synchronized boolean isSurfaceLocked() {
        if(null!=drawable) return drawable.getNativeWindow().isSurfaceLocked();
        return false;
    }

    public synchronized Exception getLockedStack() {
        if(null!=drawable) return drawable.getNativeWindow().getLockedStack();
        return null;
    }

    public boolean surfaceSwap() { 
        if(null!=drawable) return drawable.getNativeWindow().surfaceSwap();
        return super.surfaceSwap();
    }

    public long getWindowHandle() {
        if(null!=drawable) return drawable.getNativeWindow().getWindowHandle();
        return super.getWindowHandle();
    }

    public long getSurfaceHandle() {
        if(null!=drawable) return drawable.getNativeWindow().getSurfaceHandle();
        return super.getSurfaceHandle();
    }

    //----------------------------------------------------------------------
    // GLDrawable methods that are not really needed
    //

    public GLContext createContext(GLContext shareWith) {
        return drawable.createContext(shareWith);
    }

    public void setRealized(boolean realized) {
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

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    private void shouldNotCallThis() {
        throw new NativeWindowException("Should not call this");
    }
}
