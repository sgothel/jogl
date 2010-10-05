/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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
import com.jogamp.newt.util.Insets;
import com.jogamp.newt.impl.WindowImpl;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.jogamp.opengl.impl.GLDrawableHelper;

/**
 * An implementation of {@link javax.media.opengl.GLAutoDrawable} interface,
 * using an aggregation of a {@link com.jogamp.newt.Window} implementation.
 * <P>
 * This implementation does not make the OpenGL context current<br>
 * before calling the various input EventListener callbacks, ie {@link com.jogamp.newt.event.MouseListener} etc.<br>
 * This design decision is made in favor of a more performant and simplified
 * implementation. Also the event dispatcher shall be implemented OpenGL agnostic.<br>
 * To be able to use OpenGL commands from within such input {@link com.jogamp.newt.event.NEWTEventListener},<br>
 * you can inject {@link javax.media.opengl.GLRunnable} objects
 * via {@link #invoke(boolean, javax.media.opengl.GLRunnable)} to the OpenGL command stream.<br>
 * <p>
 */
public class GLWindow implements GLAutoDrawable, Window {
    private WindowImpl window;

    /**
     * Constructor. Do not call this directly -- use {@link #create()} instead.
     */
    protected GLWindow(Window window) {
        resetPerfCounter();
        this.window = (WindowImpl) window;
        ((WindowImpl)this.window).setHandleDestroyNotify(false);
        window.addWindowListener(new WindowAdapter() {
                public void windowRepaint(WindowUpdateEvent e) {
                    if( !GLWindow.this.window.isSurfaceLockedByOtherThread() && !GLWindow.this.helper.isExternalAnimatorAnimating() ) {
                        display();
                    }
                }

                public void windowResized(WindowEvent e) {
                    sendReshape = true;
                    if( !GLWindow.this.window.isSurfaceLockedByOtherThread() && !GLWindow.this.helper.isExternalAnimatorAnimating() ) {
                        display();
                    }
                }

                public void windowDestroyNotify(WindowEvent e) {
                    if( !GLWindow.this.window.isSurfaceLockedByOtherThread() && !GLWindow.this.helper.isExternalAnimatorAnimating() ) {
                        destroy();
                    } else {
                        sendDestroy = true;
                    }
                }
            });
        this.window.setLifecycleHook(new GLLifecycleHook());
    }

    /**
     * Creates a new GLWindow attaching a new Window referencing a new Screen
     * with the given GLCapabilities.
     * <P>
     * The resulting GLWindow owns the Window, Screen and Device, ie it will be destructed.
     */
    public static GLWindow create(GLCapabilities caps) {
        return new GLWindow(NewtFactory.createWindow(caps));
    }

    /**
     * Creates a new GLWindow attaching a new Window referencing the given Screen
     * with the given GLCapabilities.
     * <P>
     * The resulting GLWindow owns the Window, ie it will be destructed.
     */
    public static GLWindow create(Screen screen, GLCapabilities caps) {
        return new GLWindow(NewtFactory.createWindow(screen, caps));
    }

    /** 
     * Creates a new GLWindow attaching the given window.
     * <P>
     * The resulting GLWindow does not own the given Window, ie it will not be destructed. 
     */
    public static GLWindow create(Window window) {
        return new GLWindow(window);
    }

    /** 
     * Creates a new GLWindow attaching a new child Window 
     * of the given <code>parentNativeWindow</code> with the given GLCapabilities.
     * <P>
     * The Display/Screen will be compatible with the <code>parentNativeWindow</code>,
     * or even identical in case it's a Newt Window.
     * <P>
     * The resulting GLWindow owns the Window, ie it will be destructed. 
     */
    public static GLWindow create(NativeWindow parentNativeWindow, GLCapabilities caps) {
        return new GLWindow(NewtFactory.createWindow(parentNativeWindow, caps));
    }

    //----------------------------------------------------------------------
    // Window Access
    //

    public final Capabilities getChosenCapabilities() {
        if (drawable == null) {
            return window.getChosenCapabilities();
        }

        return drawable.getChosenGLCapabilities();
    }

    public final Capabilities getRequestedCapabilities() {
        return window.getRequestedCapabilities();
    }

    public final Window getWindow() {
        return window;
    }

    public final NativeWindow getParentNativeWindow() {
        return window.getParentNativeWindow();
    }

    public final Screen getScreen() {
        return window.getScreen();
    }

    public final void setTitle(String title) {
        window.setTitle(title);
    }

    public final String getTitle() {
        return window.getTitle();
    }

    public final void setUndecorated(boolean value) {
        window.setUndecorated(value);
    }

    public final boolean isUndecorated() {
        return window.isUndecorated();
    }

    public final void setFocusAction(FocusRunnable focusAction) {
        window.setFocusAction(focusAction);
    }
    
    public final void requestFocus() {
        window.requestFocus();
    }

    public boolean hasFocus() {
        return window.hasFocus();
    }

    public final Insets getInsets() {
        return window.getInsets();
    }

    public final void setPosition(int x, int y) {
        window.setPosition(x, y);
    }

    public final boolean setFullscreen(boolean fullscreen) {
        return window.setFullscreen(fullscreen);
    }

    public final boolean isFullscreen() {
        return window.isFullscreen();
    }

    public final boolean isVisible() {
        return window.isVisible();
    }

    public final String toString() {
        return "NEWT-GLWindow[ \n\tHelper: " + helper + ", \n\tDrawable: " + drawable + 
               ", \n\tContext: " + context + /** ", \n\tWindow: "+window+", \n\tFactory: "+factory+ */ "]";
    }

    public final int reparentWindow(NativeWindow newParent) {
        return window.reparentWindow(newParent);
    }

    public final int reparentWindow(NativeWindow newParent, boolean forceDestroyCreate) {
        return window.reparentWindow(newParent, forceDestroyCreate);
    }

    public final void removeChild(NativeWindow win) {
        window.removeChild(win);
    }

    public final void addChild(NativeWindow win) {
        window.addChild(win);
    }

    //----------------------------------------------------------------------
    // Window.LifecycleHook Implementation
    //

    public final void destroy(boolean unrecoverable) {
        window.destroy(unrecoverable);
    }

    public final void setVisible(boolean visible) {
        window.setVisible(visible);
    }

    public final void setSize(int width, int height) {
        window.setSize(width, height);
    }

    public final boolean isValid() {
        return window.isValid();
    }

    public final boolean isNativeValid() {
        return window.isNativeValid();
    }

    // Hide methods here ..
    protected class GLLifecycleHook implements WindowImpl.LifecycleHook {

        class DisposeAction implements Runnable {
            public void run() {
                // Lock: Covered by DestroyAction ..
                helper.dispose(GLWindow.this);
            }
        }
        DisposeAction disposeAction = new DisposeAction();

        /** Window.LifecycleHook */
        public synchronized void destroyAction(boolean unrecoverable) {
            if(Window.DEBUG_WINDOW_EVENT || Window.DEBUG_IMPLEMENTATION) {
                String msg = new String("GLWindow.destroy("+unrecoverable+") "+Thread.currentThread()+", start");
                System.err.println(msg);
                //Exception e1 = new Exception(msg);
                //e1.printStackTrace();
            }

            if( window.isNativeValid() && null != drawable && drawable.isRealized() ) {
                if( null != context && context.isCreated() ) {
                    // Catch dispose GLExceptions by GLEventListener, just 'print' them
                    // so we can continue with the destruction.
                    try {
                        helper.invokeGL(drawable, context, disposeAction, null);
                    } catch (GLException gle) {
                        gle.printStackTrace();
                    }
                    context.destroy();
                }
                drawable.setRealized(false);
            }
            context = null;
            drawable = null;

            if(unrecoverable) {
                helper=null;
            }

            if(Window.DEBUG_WINDOW_EVENT || Window.DEBUG_IMPLEMENTATION) {
                System.err.println("GLWindow.destroy("+unrecoverable+") "+Thread.currentThread()+", fin");
            }
        }

        /** Window.LifecycleHook */
        public synchronized void setVisibleAction(boolean visible, boolean nativeWindowCreated) {
            if(Window.DEBUG_WINDOW_EVENT || Window.DEBUG_IMPLEMENTATION) {
                String msg = new String("GLWindow.setVisibleAction("+visible+", "+nativeWindowCreated+") "+Thread.currentThread()+", start");
                System.err.println(msg);
                // Exception e1 = new Exception(msg);
                // e1.printStackTrace();
            }

            /* if (nativeWindowCreated && null != context) {
                throw new GLException("InternalError: Native Windows has been just created, but context wasn't destroyed (is not null)");
            } */
            if (null == context && visible && 0 != window.getWindowHandle() && 0<getWidth()*getHeight()) {
                NativeWindow nw;
                if (window.getWrappedWindow() != null) {
                    nw = NativeWindowFactory.getNativeWindow(window.getWrappedWindow(), window.getGraphicsConfiguration());
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
                resetPerfCounter();
            } else if(!visible) {
                resetPerfCounter();
            }
            if(Window.DEBUG_WINDOW_EVENT || Window.DEBUG_IMPLEMENTATION) {
                String msg = new String("GLWindow.setVisibleAction("+visible+", "+nativeWindowCreated+") "+Thread.currentThread()+", fin");
                System.err.println(msg);
                //Exception e1 = new Exception(msg);
                //e1.printStackTrace();
            }
        }

        boolean animatorPaused = false;

        public synchronized void reparentActionPre() {
            GLAnimatorControl ctrl = GLWindow.this.getAnimator();
            if ( null!=ctrl && ctrl.isAnimating() && ctrl.getThread() != Thread.currentThread() ) {
                animatorPaused = true;
                ctrl.pause();
            }
        }

        public synchronized void reparentActionPost(int reparentActionType) {
            resetPerfCounter();
            GLAnimatorControl ctrl = GLWindow.this.getAnimator();
            if ( null!=ctrl && animatorPaused ) {
                animatorPaused = false;
                ctrl.resume();
            }
        }
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
    private long startTime, curTime, lastCheck;
    private int  totalFrames, lastFrames;

    /** Reset all performance counter (startTime, currentTime, frame number) */
    public void resetPerfCounter() {
        startTime = System.currentTimeMillis(); // overwrite startTime to real init one
        curTime   = startTime;
        lastCheck  = startTime;
        totalFrames = 0; lastFrames = 0;
    }

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

    public void addGLEventListener(int index, GLEventListener listener) {
        helper.addGLEventListener(index, listener);
    }

    public void removeGLEventListener(GLEventListener listener) {
        helper.removeGLEventListener(listener);
    }

    public void setAnimator(GLAnimatorControl animatorControl) {
        helper.setAnimator(animatorControl);
    }

    public GLAnimatorControl getAnimator() {
        return helper.getAnimator();
    }

    public boolean getPerfLogEnabled() { return perfLog; }

    public void enablePerfLog(boolean v) {
        perfLog = v;
    }

    public void invoke(boolean wait, GLRunnable glRunnable) {
        helper.invoke(this, wait, glRunnable);
    }

    public void display() {
        display(false);
    }

    public void display(boolean forceReshape) {
        if( null == window ) { return; }

        if(sendDestroy || ( null!=window && window.hasDeviceChanged() && GLAutoDrawable.SCREEN_CHANGE_ACTION_ENABLED ) ) {
            sendDestroy=false;
            destroy();
            return;
        }

        if( null == context && window.isVisible() ) {
            // retry native window and drawable/context creation 
            setVisible(true);
        }

        if( isVisible() && isNativeValid() && null != context ) {
            if(forceReshape) {
                sendReshape = true;
            }
            lockSurface();
            try{
                helper.invokeGL(drawable, context, displayAction, initAction);
            } finally {
                unlockSurface();
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
            resetPerfCounter();
        }
    }
    private InitAction initAction = new InitAction();

    class DisplayAction implements Runnable {
        public void run() {
            // Lock: Locked Surface/Window by display _and_ MakeCurrent/Release
            if (sendReshape) {
                helper.reshape(GLWindow.this, 0, 0, getWidth(), getHeight());
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
                    System.err.println(dt0/1000 +"s: "+ lastFrames + "f, " + (lastFrames*1000)/dt0 + " fps, "+dt0/lastFrames+" ms/f; "+
                                       "total: "+ dt1/1000+"s, "+(totalFrames*1000)/dt1 + " fps, "+dt1/totalFrames+" ms/f");
                    lastCheck=curTime;
                    lastFrames=0;
                }
            }
        }
    }
    private DisplayAction displayAction = new DisplayAction();

    /** 
     * @return Time of the first display call in milliseconds.
     *         This value is reset if becoming visible again or reparenting.
     *         In case an animator is used, 
     *         the corresponding {@link javax.media.opengl.GLAnimatorControl} value is returned.
     *
     * @see javax.media.opengl.GLAnimatorControl#getStartTime()
     */
    public final long getStartTime()   { 
        GLAnimatorControl animator = getAnimator();
        if ( null == animator || null == animator.getThread() ) {
            // no animator, or not started -> use local time
            return startTime; 
        } else {
            return animator.getStartTime();
        }
    }

    /** 
     * @return Time of the last display call in milliseconds.
     *         This value is reset if becoming visible again or reparenting.
     *         In case an animator is used, 
     *         the corresponding {@link javax.media.opengl.GLAnimatorControl} value is returned.
     *
     * @see javax.media.opengl.GLAnimatorControl#getCurrentTime()
     */
    public final long getCurrentTime() {
        GLAnimatorControl animator = getAnimator();
        if ( null == animator || null == animator.getThread() ) {
            // no animator, or not started -> use local time
            return curTime;
        } else {
            return animator.getCurrentTime();
        }
    }

    /** 
     * @return Duration <code>getCurrentTime() - getStartTime()</code>.
     *
     * @see #getStartTime()
     * @see #getCurrentTime()
     */
    public final long getDuration() { 
        return getCurrentTime()-getStartTime(); 
    }

    /** 
     * @return Number of frames displayed since the first display call, ie <code>getStartTime()</code>.
     *         This value is reset if becoming visible again or reparenting.
     *         In case an animator is used, 
     *         the corresponding {@link javax.media.opengl.GLAnimatorControl} value is returned.
     *
     * @see javax.media.opengl.GLAnimatorControl#getTotalFrames()
     */
    public final int  getTotalFrames() { 
        GLAnimatorControl animator = getAnimator();
        if ( null == animator || null == animator.getThread() ) {
            // no animator, or not started -> use local value
            return totalFrames; 
        } else {
            return animator.getTotalFrames();
        }
    }

    class SwapBuffersAction implements Runnable {
        public void run() {
            drawable.swapBuffers();
        }
    }
    private SwapBuffersAction swapBuffersAction = new SwapBuffersAction();

    //----------------------------------------------------------------------
    // GLDrawable methods
    //

    public final NativeWindow getNativeWindow() {
        return null!=drawable ? drawable.getNativeWindow() : null;
    }

    public final long getHandle() {
        return null!=drawable ? drawable.getHandle() : 0;
    }

    public final void destroy() {
        window.destroy();
    }

    public final int getX() {
        return window.getX();
    }

    public final int getY() {
        return window.getY();
    }

    public final int getWidth() {
        return window.getWidth();
    }

    public final int getHeight() {
        return window.getHeight();
    }

    //----------------------------------------------------------------------
    // GLDrawable methods that are not really needed
    //

    public final GLContext createContext(GLContext shareWith) {
        return drawable.createContext(shareWith);
    }

    public final void setRealized(boolean realized) {
    }

    public final boolean isRealized() {
        return ( null != drawable ) ? drawable.isRealized() : false;
    }

    public final GLCapabilities getChosenGLCapabilities() {
        if (drawable == null) {
            throw new GLException("No drawable yet");
        }

        return drawable.getChosenGLCapabilities();
    }

    public final GLProfile getGLProfile() {
        if (drawable == null) {
            throw new GLException("No drawable yet");
        }

        return drawable.getGLProfile();
    }

    //----------------------------------------------------------------------
    // Window completion
    //
    public final void windowRepaint(int x, int y, int width, int height) {
        window.windowRepaint(x, y, width, height);
    }

    public final void enqueueEvent(boolean wait, com.jogamp.newt.event.NEWTEvent event) {
        window.enqueueEvent(wait, event);
    }

    public final void runOnEDTIfAvail(boolean wait, final Runnable task) {
        window.runOnEDTIfAvail(wait, task);
    }

    public final SurfaceUpdatedListener getSurfaceUpdatedListener(int index) {
        return window.getSurfaceUpdatedListener(index);
    }

    public final SurfaceUpdatedListener[] getSurfaceUpdatedListeners() {
        return window.getSurfaceUpdatedListeners();
    }

    public final void removeAllSurfaceUpdatedListener() {
        window.removeAllSurfaceUpdatedListener();
    }

    public final void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        window.removeSurfaceUpdatedListener(l);
    }

    public final void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        window.addSurfaceUpdatedListener(l);
    }

    public final void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) throws IndexOutOfBoundsException {
        window.addSurfaceUpdatedListener(index, l);
    }

    public void sendWindowEvent(int eventType) {
        window.sendWindowEvent(eventType);
    }

    public final WindowListener getWindowListener(int index) {
        return window.getWindowListener(index);
    }

    public final WindowListener[] getWindowListeners() {
        return window.getWindowListeners();
    }

    public final void removeWindowListener(WindowListener l) {
        window.removeWindowListener(l);
    }

    public final void addWindowListener(WindowListener l) {
        window.addWindowListener(l);
    }

    public final void addWindowListener(int index, WindowListener l) throws IndexOutOfBoundsException {
        window.addWindowListener(index, l);
    }

    public final void addKeyListener(KeyListener l) {
        window.addKeyListener(l);
    }

    public final void addKeyListener(int index, KeyListener l) {
        window.addKeyListener(index, l);
    }

    public final void removeKeyListener(KeyListener l) {
        window.removeKeyListener(l);
    }

    public final KeyListener getKeyListener(int index) {
        return window.getKeyListener(index);
    }

    public final KeyListener[] getKeyListeners() {
        return window.getKeyListeners();
    }

    public final void addMouseListener(MouseListener l) {
        window.addMouseListener(l);
    }

    public final void addMouseListener(int index, MouseListener l) {
        window.addMouseListener(index, l);
    }

    public final void removeMouseListener(MouseListener l) {
        window.removeMouseListener(l);
    }

    public final MouseListener getMouseListener(int index) {
        return window.getMouseListener(index);
    }

    public final MouseListener[] getMouseListeners() {
        return window.getMouseListeners();
    }

    //----------------------------------------------------------------------
    // NativeWindow completion
    //

    public final int lockSurface() {
        return window.lockSurface();
    }

    public final void unlockSurface() throws NativeWindowException {
        window.unlockSurface();
    }

    public final boolean isSurfaceLockedByOtherThread() {
        return window.isSurfaceLockedByOtherThread();
    }

    public final boolean isSurfaceLocked() {
        return window.isSurfaceLocked();
    }

    public final Thread getSurfaceLockOwner() {
        return window.getSurfaceLockOwner();

    }

    public final Exception getSurfaceLockStack() {
        return window.getSurfaceLockStack();
    }

    public final boolean surfaceSwap() {
        return window.surfaceSwap();
    }

    public final void invalidate() {
        window.invalidate();
    }
    
    public final long getWindowHandle() {
        return window.getWindowHandle();

    }

    public final long getSurfaceHandle() {
        return window.getSurfaceHandle();

    }

    public final AbstractGraphicsConfiguration getGraphicsConfiguration() {
        return window.getGraphicsConfiguration();
    }

    public final long getDisplayHandle() {
        return window.getDisplayHandle();
    }

    public final int  getScreenIndex() {
        return window.getScreenIndex();
    }

    public final void surfaceUpdated(Object updater, NativeWindow window, long when) {
        window.surfaceUpdated(updater, window, when);
    }
}
