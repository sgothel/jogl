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

import java.io.PrintStream;
import java.util.List;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.nativewindow.NativeWindowVersion;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import jogamp.newt.WindowImpl;

import javax.media.nativewindow.*;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.Insets;
import javax.media.opengl.*;

import jogamp.opengl.FPSCounterImpl;
import jogamp.opengl.GLDrawableHelper;
import com.jogamp.opengl.JoglVersion;

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
public class GLWindow implements GLAutoDrawable, Window, NEWTEventConsumer, FPSCounter {
    private WindowImpl window;

    /**
     * Constructor. Do not call this directly -- use {@link #create()} instead.
     */
    protected GLWindow(Window window) {
        resetFPSCounter();
        this.window = (WindowImpl) window;
        ((WindowImpl)this.window).setHandleDestroyNotify(false);
        window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowRepaint(WindowUpdateEvent e) {
                    if( !GLWindow.this.window.isWindowLockedByOtherThread() && !GLWindow.this.helper.isExternalAnimatorAnimating() ) {
                        display();
                    }
                }

                @Override
                public void windowResized(WindowEvent e) {
                    sendReshape = true;
                    if( !GLWindow.this.window.isWindowLockedByOtherThread() && !GLWindow.this.helper.isExternalAnimatorAnimating() ) {
                        display();
                    }
                }

                @Override
                public void windowDestroyNotify(WindowEvent e) {
                    if( DISPOSE_ON_CLOSE == GLWindow.this.getDefaultCloseOperation() ) {
                        // Is an animator thread perform rendering?
                        if (GLWindow.this.helper.isExternalAnimatorRunning()) {
                            // Pause animations before initiating safe destroy.
                            GLAnimatorControl ctrl = GLWindow.this.helper.getAnimator();
                            boolean isPaused = ctrl.pause();
                            destroy();
                            if(isPaused) {
                                ctrl.resume();
                            }
                        } else if (GLWindow.this.window.isWindowLockedByOtherThread()) {
                            // Window is locked by another thread
                            // Flag that destroy should be performed on the next
                            // attempt to display.
                            sendDestroy = true;
                        } else {
                            // Without an external thread animating or locking the
                            // surface, we are safe.
                            destroy ();
                        }
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
    public static GLWindow create(GLCapabilitiesImmutable caps) {
        return new GLWindow(NewtFactory.createWindow(caps));
    }

    /**
     * Creates a new GLWindow attaching a new Window referencing the given Screen
     * with the given GLCapabilities.
     * <P>
     * The resulting GLWindow owns the Window, ie it will be destructed.
     */
    public static GLWindow create(Screen screen, GLCapabilitiesImmutable caps) {
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
    public static GLWindow create(NativeWindow parentNativeWindow, GLCapabilitiesImmutable caps) {
        return new GLWindow(NewtFactory.createWindow(parentNativeWindow, caps));
    }

    //----------------------------------------------------------------------
    // WindowClosingProtocol implementation
    //
    public int getDefaultCloseOperation() {
        return window.getDefaultCloseOperation();
    }

    public int setDefaultCloseOperation(int op) {
        return window.setDefaultCloseOperation(op);
    }

    //----------------------------------------------------------------------
    // Window Access
    //

    public CapabilitiesChooser setCapabilitiesChooser(CapabilitiesChooser chooser) {
        return window.setCapabilitiesChooser(chooser);
    }

    public final CapabilitiesImmutable getChosenCapabilities() {
        if (drawable == null) {
            return window.getChosenCapabilities();
        }

        return drawable.getChosenGLCapabilities();
    }

    public final CapabilitiesImmutable getRequestedCapabilities() {
        return window.getRequestedCapabilities();
    }

    public final Window getWindow() {
        return window;
    }

    public final NativeWindow getParent() {
        return window.getParent();
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

    @Override
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

    public final void destroy() {
        window.destroy();
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

    public Point getLocationOnScreen(Point storage) {
        return window.getLocationOnScreen(storage);
    }

    // Hide methods here ..
    protected class GLLifecycleHook implements WindowImpl.LifecycleHook {

        private class DisposeAction implements Runnable {
            public final void run() {
                // Lock: Covered by DestroyAction ..
                helper.dispose(GLWindow.this);
            }
        }
        DisposeAction disposeAction = new DisposeAction();

        public synchronized void destroyActionPreLock() {
            // nop
        }

        public synchronized void destroyActionInLock() {
            if(Window.DEBUG_WINDOW_EVENT || Window.DEBUG_IMPLEMENTATION) {
                String msg = "GLWindow.destroy() "+Thread.currentThread()+", start";
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
            
            GLAnimatorControl ctrl = GLWindow.this.getAnimator();
            if ( null!=ctrl ) {
                ctrl.remove(GLWindow.this);
            }            
            // helper=null; // pending events ..
            
            if(Window.DEBUG_WINDOW_EVENT || Window.DEBUG_IMPLEMENTATION) {
                System.err.println("GLWindow.destroy() "+Thread.currentThread()+", fin");
            }
        }

        public synchronized void resetCounter() {
            if(Window.DEBUG_WINDOW_EVENT || Window.DEBUG_IMPLEMENTATION) {
                System.err.println("GLWindow.resetCounter() "+Thread.currentThread());
            }
            GLWindow.this.resetFPSCounter();
        }

        public synchronized void setVisibleActionPost(boolean visible, boolean nativeWindowCreated) {
            if(Window.DEBUG_WINDOW_EVENT || Window.DEBUG_IMPLEMENTATION) {
                String msg = "GLWindow.setVisibleActionPost("+visible+", "+nativeWindowCreated+") "+Thread.currentThread()+", start";
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
                GLCapabilitiesImmutable glCaps = (GLCapabilitiesImmutable) nw.getGraphicsConfiguration().getNativeGraphicsConfiguration().getChosenCapabilities();
                if(null==factory) {
                    factory = GLDrawableFactory.getFactory(glCaps.getGLProfile());
                }
                if(null==drawable) {
                    drawable = factory.createGLDrawable(nw);
                }
                drawable.setRealized(true);
                context = drawable.createContext(sharedContext);
                context.setContextCreationFlags(additionalCtxCreationFlags);                
            }
            if(Window.DEBUG_WINDOW_EVENT || Window.DEBUG_IMPLEMENTATION) {
                String msg = "GLWindow.setVisibleActionPost("+visible+", "+nativeWindowCreated+") "+Thread.currentThread()+", fin";
                System.err.println(msg);
                //Exception e1 = new Exception(msg);
                //e1.printStackTrace();
            }
        }
        
        private GLAnimatorControl savedAnimator = null;
        
        public synchronized boolean pauseRenderingAction() {
            boolean animatorPaused = false;
            savedAnimator = GLWindow.this.getAnimator();
            if ( null != savedAnimator ) {
                animatorPaused = savedAnimator.pause();
            }
            return animatorPaused;
        }

        public synchronized void resumeRenderingAction() {
            if ( null != savedAnimator && savedAnimator.isPaused() ) {
                savedAnimator.resume();
            }
        }
    }

    //----------------------------------------------------------------------
    // OpenGL-related methods and state
    //

    private GLContext sharedContext = null;
    private int additionalCtxCreationFlags = 0;
    private GLDrawableFactory factory;
    private GLDrawable drawable;
    private GLContext context;
    private GLDrawableHelper helper = new GLDrawableHelper();
    // To make reshape events be sent immediately before a display event
    private boolean sendReshape=false;
    private boolean sendDestroy=false;
    private FPSCounterImpl fpsCounter = new FPSCounterImpl();    

    public GLDrawableFactory getFactory() {
        return factory;
    }

    /**
     * Specifies an {@link javax.media.opengl.GLContext OpenGL context} to share with.<br>
     * At native creation, {@link #setVisible(boolean) setVisible(true)},
     * a {@link javax.media.opengl.GLDrawable drawable} and {@link javax.media.opengl.GLContext context} is created besides the native Window itself,<br>
     * hence you shall set the shared context before.
     *
     * @param sharedContext The OpenGL context shared by this GLWindow's one
     */
    public void setSharedContext(GLContext sharedContext) {
        this.sharedContext = sharedContext;
    }

    public void setContext(GLContext newCtx) {
        context = newCtx;
        if(null != context) {
            context.setContextCreationFlags(additionalCtxCreationFlags);
        }        
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
        if(null!=helper) {
            helper.addGLEventListener(listener);
        }
    }

    public void addGLEventListener(int index, GLEventListener listener) {
        if(null!=helper) {
            helper.addGLEventListener(index, listener);
        }
    }

    public void removeGLEventListener(GLEventListener listener) {
        if(null!=helper) {
            helper.removeGLEventListener(listener);
        }
    }

    public void setAnimator(GLAnimatorControl animatorControl) {
        if(null!=helper) {
            helper.setAnimator(animatorControl);
        }
    }

    public GLAnimatorControl getAnimator() {
        if(null!=helper) {
            return helper.getAnimator();
        }
        return null;
    }

    public void invoke(boolean wait, GLRunnable glRunnable) {
        if(null!=helper) {
            helper.invoke(this, wait, glRunnable);
        }
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

        if( null == context && isVisible() && 0<getWidth()*getHeight() ) {
            // retry native window and drawable/context creation 
            setVisible(true);
        }

        if(forceReshape) {
            sendReshape = true;
        }
        
        if( isVisible() && null != context ) {
            if( NativeSurface.LOCK_SURFACE_NOT_READY < lockSurface() ) {
                try {
                    helper.invokeGL(drawable, context, displayAction, initAction);
                } finally {
                    unlockSurface();
                }
            }
        }
    }
    
    /** This implementation uses a static value */
    public void setAutoSwapBufferMode(boolean onOrOff) {
        if(null!=helper) {
            helper.setAutoSwapBufferMode(onOrOff);
        }
    }

    /** This implementation uses a static value */
    public boolean getAutoSwapBufferMode() {
        if(null!=helper) {
            return helper.getAutoSwapBufferMode();
        }
        return false;
    }
    
    public void swapBuffers() {
        if(drawable!=null && context != null) {
            drawable.swapBuffers();
        }
    }

    public void setContextCreationFlags(int flags) {
        additionalCtxCreationFlags = flags;
    }
      
    public int getContextCreationFlags() {
        return additionalCtxCreationFlags;                
    }
        
    private class InitAction implements Runnable {
        public final void run() {
            // Lock: Locked Surface/Window by MakeCurrent/Release
            helper.init(GLWindow.this);
            resetFPSCounter();
        }
    }
    private InitAction initAction = new InitAction();

    private class DisplayAction implements Runnable {
        public final void run() {
            // Lock: Locked Surface/Window by display _and_ MakeCurrent/Release
            if (sendReshape) {
                helper.reshape(GLWindow.this, 0, 0, getWidth(), getHeight());
                sendReshape = false;
            }

            helper.display(GLWindow.this);

            fpsCounter.tickFPS();
        }
    }
    private DisplayAction displayAction = new DisplayAction();

    public final void setUpdateFPSFrames(int frames, PrintStream out) {
        fpsCounter.setUpdateFPSFrames(frames, out);
    }
    
    public final void resetFPSCounter() {
        fpsCounter.resetFPSCounter();
    }

    public final int getUpdateFPSFrames() {
        return fpsCounter.getUpdateFPSFrames();
    }
    
    public final long getFPSStartTime()   {
        return fpsCounter.getFPSStartTime();
    }

    public final long getLastFPSUpdateTime() {
        return fpsCounter.getLastFPSUpdateTime();
    }

    public final long getLastFPSPeriod() {
        return fpsCounter.getLastFPSPeriod();
    }
    
    public final float getLastFPS() {
        return fpsCounter.getLastFPS();
    }
    
    public final int getTotalFPSFrames() {
        return fpsCounter.getTotalFPSFrames();
    }

    public final long getTotalFPSDuration() {
        return fpsCounter.getTotalFPSDuration();
    }
    
    public final float getTotalFPS() {
        return fpsCounter.getTotalFPS();
    }        

    private class SwapBuffersAction implements Runnable {
        public final void run() {
            drawable.swapBuffers();
        }
    }
    private SwapBuffersAction swapBuffersAction = new SwapBuffersAction();

    //----------------------------------------------------------------------
    // GLDrawable methods
    //

    public final NativeSurface getNativeSurface() {
        return null!=drawable ? drawable.getNativeSurface() : null;
    }

    public final long getHandle() {
        return null!=drawable ? drawable.getHandle() : 0;
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

    public final GLCapabilitiesImmutable getChosenGLCapabilities() {
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
    // NEWTEventConsumer 
    //
    public boolean consumeEvent(NEWTEvent event) {
        return window.consumeEvent(event);
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

    public final boolean surfaceSwap() {
        return window.surfaceSwap();
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

    public final void surfaceUpdated(Object updater, NativeSurface ns, long when) {
        window.surfaceUpdated(updater, ns, when);
    }

    /**
     * A most simple JOGL AWT test entry
     */
    public static void main(String args[]) {
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(GlueGenVersion.getInstance());
        System.err.println(NativeWindowVersion.getInstance());
        System.err.println(JoglVersion.getInstance());
        System.err.println(NewtVersion.getInstance());

        GLProfile glp = GLProfile.getDefault();
        GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
        List/*<GLCapabilitiesImmutable>*/ availCaps = factory.getAvailableCapabilities(null);
        for(int i=0; i<availCaps.size(); i++) {
            System.err.println(availCaps.get(i));
        }
        GLCapabilitiesImmutable caps = new GLCapabilities( glp );

        GLWindow glWindow = GLWindow.create(caps);
        glWindow.setSize(128, 128);

        glWindow.addGLEventListener(new GLEventListener() {
            public void init(GLAutoDrawable drawable) {
                GL gl = drawable.getGL();
                System.err.println(JoglVersion.getGLInfo(gl, null));
            }

            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            }

            public void display(GLAutoDrawable drawable) {
            }

            public void dispose(GLAutoDrawable drawable) {
            }
        });

        glWindow.setVisible(true);
        glWindow.destroy();
    }

}
