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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.SurfaceUpdatedListener;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.Rectangle;
import javax.media.opengl.FPSCounter;
import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GL4ES3;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLES2;
import javax.media.opengl.GLES3;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;

import jogamp.newt.WindowImpl;
import jogamp.opengl.GLAutoDrawableBase;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.event.GestureHandler;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.NEWTEventConsumer;
import com.jogamp.newt.event.NEWTEventListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.GLStateKeeper;

/**
 * An implementation of {@link GLAutoDrawable} and {@link Window} interface,
 * using a delegated {@link Window} instance, which may be an aggregation (lifecycle: created and destroyed).
 * <P>
 * This implementation supports {@link GLStateKeeper GL state preservation},
 * hence {@link #isGLStatePreservationSupported()} returns <code>true</code>.
 * </P>
 * <P>
 * This implementation does not make the OpenGL context current<br>
 * before calling the various input EventListener callbacks, ie {@link MouseListener} etc.<br>
 * This design decision is made in favor of a more performant and simplified
 * implementation. Also the event dispatcher shall be implemented OpenGL agnostic.<br>
 * To be able to use OpenGL commands from within such input {@link NEWTEventListener},<br>
 * you can inject {@link GLRunnable} objects
 * via {@link #invoke(boolean, GLRunnable)} to the OpenGL command stream.<br>
 * </p>
 */
public class GLWindow extends GLAutoDrawableBase implements GLAutoDrawable, Window, NEWTEventConsumer, FPSCounter {
    private final WindowImpl window;

    /**
     * Constructor. Do not call this directly -- use {@link #create()} instead.
     */
    protected GLWindow(Window window) {
        super(null, null, false /* always handle device lifecycle ourselves */);
        this.window = (WindowImpl) window;
        this.window.setWindowDestroyNotifyAction( new Runnable() {
            @Override
            public void run() {
                defaultWindowDestroyNotifyOp();
            } } );
        window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowRepaint(WindowUpdateEvent e) {
                    defaultWindowRepaintOp();
                }

                @Override
                public void windowResized(WindowEvent e) {
                    defaultWindowResizedOp(getSurfaceWidth(), getSurfaceHeight());
                }

            });
        this.window.setLifecycleHook(new GLLifecycleHook());
    }

    @Override
    public final Object getUpstreamWidget() {
        return window;
    }

    /**
     * Creates a new GLWindow attaching a new Window referencing a
     * new default Screen and default Display with the given GLCapabilities.
     * <p>
     * The lifecycle of this Window's Screen and Display is handled via {@link Screen#addReference()}
     * and {@link Screen#removeReference()}.
     * </p>
     * The default Display will be reused if already instantiated.
     */
    public static GLWindow create(GLCapabilitiesImmutable caps) {
        return new GLWindow(NewtFactory.createWindow(caps));
    }

    /**
     * Creates a new GLWindow attaching a new Window referencing the given Screen
     * with the given GLCapabilities.
     * <p>
     * The lifecycle of this Window's Screen and Display is handled via {@link Screen#addReference()}
     * and {@link Screen#removeReference()}.
     * </p>
     */
    public static GLWindow create(Screen screen, GLCapabilitiesImmutable caps) {
        return new GLWindow(NewtFactory.createWindow(screen, caps));
    }

    /**
     * Creates a new GLWindow attaching the given window.
     * <p>
     * The lifecycle of this Window's Screen and Display is handled via {@link Screen#addReference()}
     * and {@link Screen#removeReference()}.
     * </p>
     */
    public static GLWindow create(Window window) {
        return new GLWindow(window);
    }

    /**
     * Creates a new GLWindow attaching a new child Window
     * of the given <code>parentNativeWindow</code> with the given GLCapabilities.
     * <p>
     * The Display/Screen will be compatible with the <code>parentNativeWindow</code>,
     * or even identical in case it's a Newt Window.
     * An already instantiated compatible Display will be reused.
     * </p>
     * <p>
     * The lifecycle of this Window's Screen and Display is handled via {@link Screen#addReference()}
     * and {@link Screen#removeReference()}.
     * </p>
     */
    public static GLWindow create(NativeWindow parentNativeWindow, GLCapabilitiesImmutable caps) {
        return new GLWindow(NewtFactory.createWindow(parentNativeWindow, caps));
    }

    //----------------------------------------------------------------------
    // WindowClosingProtocol implementation
    //
    @Override
    public WindowClosingMode getDefaultCloseOperation() {
        return window.getDefaultCloseOperation();
    }

    @Override
    public WindowClosingMode setDefaultCloseOperation(WindowClosingMode op) {
        return window.setDefaultCloseOperation(op);
    }

    //----------------------------------------------------------------------
    // Window Access
    //

    @Override
    public CapabilitiesChooser setCapabilitiesChooser(CapabilitiesChooser chooser) {
        return window.setCapabilitiesChooser(chooser);
    }

    @Override
    public final CapabilitiesImmutable getChosenCapabilities() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getChosenGLCapabilities() : window.getChosenCapabilities();
    }

    @Override
    public final CapabilitiesImmutable getRequestedCapabilities() {
        return window.getRequestedCapabilities();
    }

    @Override
    public final Window getDelegatedWindow() {
        return window.getDelegatedWindow();
    }

    @Override
    public final NativeWindow getParent() {
        return window.getParent();
    }

    @Override
    public final Screen getScreen() {
        return window.getScreen();
    }

    @Override
    public final MonitorDevice getMainMonitor() {
        return window.getMainMonitor();
    }

    @Override
    public final void setTitle(String title) {
        window.setTitle(title);
    }

    @Override
    public final String getTitle() {
        return window.getTitle();
    }

    @Override
    public final boolean isPointerVisible() {
        return window.isPointerVisible();
    }

    @Override
    public final void setPointerVisible(boolean mouseVisible) {
        window.setPointerVisible(mouseVisible);
    }

    @Override
    public final PointerIcon getPointerIcon() {
        return window.getPointerIcon();
    }

    @Override
    public final void setPointerIcon(final PointerIcon pi) {
        window.setPointerIcon(pi);
    }

    @Override
    public final boolean isPointerConfined() {
        return window.isPointerConfined();
    }

    @Override
    public final void confinePointer(boolean grab) {
        window.confinePointer(grab);
    }

    @Override
    public final void setUndecorated(boolean value) {
        window.setUndecorated(value);
    }

    @Override
    public final void warpPointer(int x, int y) {
        window.warpPointer(x, y);
    }
    @Override
    public final boolean isUndecorated() {
        return window.isUndecorated();
    }

    @Override
    public final void setAlwaysOnTop(boolean value) {
        window.setAlwaysOnTop(value);
    }

    @Override
    public final boolean isAlwaysOnTop() {
        return window.isAlwaysOnTop();
    }

    @Override
    public final void setFocusAction(FocusRunnable focusAction) {
        window.setFocusAction(focusAction);
    }

    @Override
    public void setKeyboardFocusHandler(KeyListener l) {
        window.setKeyboardFocusHandler(l);
    }

    @Override
    public final void requestFocus() {
        window.requestFocus();
    }

    @Override
    public final void requestFocus(boolean wait) {
        window.requestFocus(wait);
    }

    @Override
    public boolean hasFocus() {
        return window.hasFocus();
    }

    @Override
    public final InsetsImmutable getInsets() {
        return window.getInsets();
    }

    @Override
    public final int getX() {
        return window.getX();
    }

    @Override
    public final int getY() {
        return window.getY();
    }

    @Override
    public final int getWidth() {
        return window.getWidth();
    }

    @Override
    public final int getHeight() {
        return window.getHeight();
    }

    @Override
    public final Rectangle getBounds() {
        return window.getBounds();
    }

    @Override
    public final int getSurfaceWidth() {
        return window.getSurfaceWidth();
    }

    @Override
    public final int getSurfaceHeight() {
        return window.getSurfaceHeight();
    }

    @Override
    public final int[] convertToWindowUnits(final int[] pixelUnitsAndResult) {
        return window.convertToWindowUnits(pixelUnitsAndResult);
    }

    @Override
    public final int[] convertToPixelUnits(final int[] windowUnitsAndResult) {
        return window.convertToPixelUnits(windowUnitsAndResult);
    }

    @Override
    public final void setSurfaceScale(final int[] pixelScale) {
        window.setSurfaceScale(pixelScale);
    }

    @Override
    public final int[] getRequestedSurfaceScale(final int[] result) {
        return window.getRequestedSurfaceScale(result);
    }

    @Override
    public final int[] getCurrentSurfaceScale(final int[] result) {
        return window.getCurrentSurfaceScale(result);
    }

    @Override
    public final int[] getNativeSurfaceScale(final int[] result) {
        return window.getNativeSurfaceScale(result);
    }

    @Override
    public final float[] getPixelsPerMM(final float[] ppmmStore) {
        return window.getPixelsPerMM(ppmmStore);
    }

    @Override
    public final void setPosition(int x, int y) {
        window.setPosition(x, y);
    }
    @Override
    public void setTopLevelPosition(int x, int y) {
        window.setTopLevelPosition(x, y);
    }

    @Override
    public final boolean setFullscreen(boolean fullscreen) {
        return window.setFullscreen(fullscreen);
    }

    @Override
    public boolean setFullscreen(List<MonitorDevice> monitors) {
        return window.setFullscreen(monitors);
    }

    @Override
    public final boolean isFullscreen() {
        return window.isFullscreen();
    }

    @Override
    public final boolean isVisible() {
        return window.isVisible();
    }

    @Override
    public final String toString() {
        return "NEWT-GLWindow[ \n\tHelper: " + helper + ", \n\tDrawable: " + drawable +
               ", \n\tContext: " + context + ", \n\tWindow: "+window+ /** ", \n\tFactory: "+factory+ */ "]";
    }

    @Override
    public final ReparentOperation reparentWindow(NativeWindow newParent, int x, int y, int hints) {
        return window.reparentWindow(newParent, x, y, hints);
    }

    @Override
    public final boolean removeChild(NativeWindow win) {
        return window.removeChild(win);
    }

    @Override
    public final boolean addChild(NativeWindow win) {
        return window.addChild(win);
    }

    //----------------------------------------------------------------------
    // Window.LifecycleHook Implementation
    //

    @Override
    public final void destroy() {
        window.destroy();
    }

    @Override
    public void setWindowDestroyNotifyAction(Runnable r) {
        window.setWindowDestroyNotifyAction(r);
    }

    @Override
    public final void setVisible(boolean visible) {
        window.setVisible(visible);
    }

    @Override
    public void setVisible(boolean wait, boolean visible) {
        window.setVisible(wait, visible);
    }

    @Override
    public final void setSize(int width, int height) {
        window.setSize(width, height);
    }
    @Override
    public final void setSurfaceSize(int pixelWidth, int pixelHeight) {
        window.setSurfaceSize(pixelWidth, pixelHeight);
    }
    @Override
    public void setTopLevelSize(int width, int height) {
        window.setTopLevelSize(width, height);
    }

    @Override
    public final boolean isNativeValid() {
        return window.isNativeValid();
    }

    @Override
    public Point getLocationOnScreen(Point storage) {
        return window.getLocationOnScreen(storage);
    }

    // Hide methods here ..
    protected class GLLifecycleHook implements WindowImpl.LifecycleHook {

        @Override
        public void preserveGLStateAtDestroy(boolean value) {
            GLWindow.this.preserveGLStateAtDestroy(value);
        }

        @Override
        public synchronized void destroyActionPreLock() {
            // nop
        }

        @Override
        public synchronized void destroyActionInLock() {
            if(Window.DEBUG_IMPLEMENTATION) {
                String msg = "GLWindow.destroy() "+WindowImpl.getThreadName()+", start";
                System.err.println(msg);
                //Exception e1 = new Exception(msg);
                //e1.printStackTrace();
            }

            destroyImplInLock();

            if(Window.DEBUG_IMPLEMENTATION) {
                System.err.println("GLWindow.destroy() "+WindowImpl.getThreadName()+", fin");
            }
        }

        @Override
        public synchronized void resetCounter() {
            if(Window.DEBUG_IMPLEMENTATION) {
                System.err.println("GLWindow.resetCounter() "+WindowImpl.getThreadName());
            }
            GLWindow.this.resetFPSCounter();
            final GLAnimatorControl animator = GLWindow.this.getAnimator();
            if( null != animator ) {
                animator.resetFPSCounter();
            }
        }

        @Override
        public synchronized void setVisibleActionPost(boolean visible, boolean nativeWindowCreated) {
            long t0;
            if(Window.DEBUG_IMPLEMENTATION) {
                t0 = System.nanoTime();
                System.err.println("GLWindow.setVisibleActionPost("+visible+", "+nativeWindowCreated+") "+WindowImpl.getThreadName()+", start");
            } else {
                t0 = 0;
            }

            if (null == drawable && visible && 0 != window.getWindowHandle() && 0<getSurfaceWidth()*getSurfaceHeight()) {
                if( ( null != context ) ) {
                    throw new InternalError("GLWindow.LifecycleHook.setVisiblePost: "+WindowImpl.getThreadName()+" - Null drawable, but valid context - "+GLWindow.this);
                }
                final GLContext[] shareWith = { null };
                if( !helper.isSharedGLContextPending(shareWith) ) {
                    final NativeSurface ns;
                    {
                        final NativeSurface wrapped_ns = window.getWrappedSurface();
                        ns = null != wrapped_ns ? wrapped_ns : window;
                    }
                    final GLCapabilitiesImmutable glCaps = (GLCapabilitiesImmutable) ns.getGraphicsConfiguration().getChosenCapabilities();
                    if(null==factory) {
                        factory = GLDrawableFactory.getFactory(glCaps.getGLProfile());
                    }
                    drawable = (GLDrawableImpl) factory.createGLDrawable(ns);
                    drawable.setRealized(true);

                    if( !GLWindow.this.restoreGLEventListenerState() ) {
                        context = (GLContextImpl) drawable.createContext(shareWith[0]);
                        context.setContextCreationFlags(additionalCtxCreationFlags);
                    }
                }
            }
            if(Window.DEBUG_IMPLEMENTATION) {
                System.err.println("GLWindow.setVisibleActionPost("+visible+", "+nativeWindowCreated+") "+WindowImpl.getThreadName()+", fin: dt "+ (System.nanoTime()-t0)/1e6 +"ms");
            }
        }

        private GLAnimatorControl savedAnimator = null;

        @Override
        public synchronized boolean pauseRenderingAction() {
            boolean animatorPaused = false;
            savedAnimator = GLWindow.this.getAnimator();
            if ( null != savedAnimator ) {
                animatorPaused = savedAnimator.pause();
            }
            return animatorPaused;
        }

        @Override
        public synchronized void resumeRenderingAction() {
            if ( null != savedAnimator && savedAnimator.isPaused() ) {
                savedAnimator.resume();
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void shutdownRenderingAction() {
            final GLAnimatorControl anim = GLWindow.this.getAnimator();
            if ( null != anim && anim.isAnimating() ) {
                final Thread animThread = anim.getThread();
                if( animThread == Thread.currentThread() ) {
                    anim.stop(); // on anim thread, non-blocking
                } else {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            if( anim.isAnimating() && null != animThread ) {
                                try {
                                    animThread.stop();
                                } catch(Throwable t) {
                                    if( DEBUG ) {
                                        System.err.println("Catched "+t.getClass().getName()+": "+t.getMessage());
                                        t.printStackTrace();
                                    }
                                }
                            }
                            return null;
                        } } );
                }
            }
        }
    }

    //----------------------------------------------------------------------
    // OpenGL-related methods and state
    //

    @Override
    protected final RecursiveLock getLock() {
        return window.getLock();
    }

    @Override
    public void display() {
        if( !isNativeValid() || !isVisible() ) { return; }

        if(sendDestroy || ( window.hasDeviceChanged() && GLAutoDrawable.SCREEN_CHANGE_ACTION_ENABLED ) ) {
            sendDestroy=false;
            destroy();
            return;
        }

        final boolean done;
        final RecursiveLock lock = window.getLock();
        lock.lock(); // sync: context/drawable could have been recreated/destroyed while animating
        try {
            if( null != context ) {
                // surface is locked/unlocked implicit by context's makeCurrent/release
                helper.invokeGL(drawable, context, defaultDisplayAction, defaultInitAction);
                done = true;
            } else {
                done = false;
            }
        } finally {
            lock.unlock();
        }
        if( !done && ( 0 < getSurfaceWidth() && 0 < getSurfaceHeight() ) ) {
            // retry drawable and context creation, will itself issue resize -> display
            setVisible(true);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * GLWindow supports GL state preservation, hence returns <code>true</code>.
     * </p>
     */
    @Override
    public final boolean isGLStatePreservationSupported() { return true; }

    //----------------------------------------------------------------------
    // GLDrawable methods
    //
    private GLDrawableFactory factory;

    @Override
    public final GLDrawableFactory getFactory() {
        return factory;
    }

    @Override
    public final void swapBuffers() throws GLException {
         defaultSwapBuffers();
    }

    //----------------------------------------------------------------------
    // NEWTEventConsumer
    //
    @Override
    public boolean consumeEvent(NEWTEvent event) {
        return window.consumeEvent(event);
    }

    //----------------------------------------------------------------------
    // Window completion
    //
    @Override
    public final void windowRepaint(int x, int y, int width, int height) {
        window.windowRepaint(x, y, width, height);
    }

    @Override
    public final void enqueueEvent(boolean wait, com.jogamp.newt.event.NEWTEvent event) {
        window.enqueueEvent(wait, event);
    }

    @Override
    public final void runOnEDTIfAvail(boolean wait, final Runnable task) {
        window.runOnEDTIfAvail(wait, task);
    }

    @Override
    public void sendWindowEvent(int eventType) {
        window.sendWindowEvent(eventType);
    }

    @Override
    public final WindowListener getWindowListener(int index) {
        return window.getWindowListener(index);
    }

    @Override
    public final WindowListener[] getWindowListeners() {
        return window.getWindowListeners();
    }

    @Override
    public final void removeWindowListener(WindowListener l) {
        window.removeWindowListener(l);
    }

    @Override
    public final void addWindowListener(WindowListener l) {
        window.addWindowListener(l);
    }

    @Override
    public final void addWindowListener(int index, WindowListener l) throws IndexOutOfBoundsException {
        window.addWindowListener(index, l);
    }

    @Override
    public final void setKeyboardVisible(boolean visible) {
        window.setKeyboardVisible(visible);
    }

    @Override
    public final boolean isKeyboardVisible() {
        return window.isKeyboardVisible();
    }

    @Override
    public final void addKeyListener(KeyListener l) {
        window.addKeyListener(l);
    }

    @Override
    public final void addKeyListener(int index, KeyListener l) {
        window.addKeyListener(index, l);
    }

    @Override
    public final void removeKeyListener(KeyListener l) {
        window.removeKeyListener(l);
    }

    @Override
    public final KeyListener getKeyListener(int index) {
        return window.getKeyListener(index);
    }

    @Override
    public final KeyListener[] getKeyListeners() {
        return window.getKeyListeners();
    }

    @Override
    public final void addMouseListener(MouseListener l) {
        window.addMouseListener(l);
    }

    @Override
    public final void addMouseListener(int index, MouseListener l) {
        window.addMouseListener(index, l);
    }

    @Override
    public final void removeMouseListener(MouseListener l) {
        window.removeMouseListener(l);
    }

    @Override
    public final MouseListener getMouseListener(int index) {
        return window.getMouseListener(index);
    }

    @Override
    public final MouseListener[] getMouseListeners() {
        return window.getMouseListeners();
    }

    @Override
    public void setDefaultGesturesEnabled(boolean enable) {
        window.setDefaultGesturesEnabled(enable);
    }
    @Override
    public boolean areDefaultGesturesEnabled() {
        return window.areDefaultGesturesEnabled();
    }
    @Override
    public final void addGestureHandler(GestureHandler gh) {
        window.addGestureHandler(gh);
    }
    @Override
    public final void addGestureHandler(int index, GestureHandler gh) {
        window.addGestureHandler(index, gh);
    }
    @Override
    public final void removeGestureHandler(GestureHandler gh) {
        window.removeGestureHandler(gh);
    }
    @Override
    public final void addGestureListener(GestureHandler.GestureListener gl) {
        window.addGestureListener(-1, gl);
    }
    @Override
    public final void addGestureListener(int index, GestureHandler.GestureListener gl) {
        window.addGestureListener(index, gl);
    }
    @Override
    public final void removeGestureListener(GestureHandler.GestureListener gl) {
        window.removeGestureListener(gl);
    }

    //----------------------------------------------------------------------
    // NativeWindow completion
    //

    @Override
    public final int lockSurface() throws NativeWindowException, RuntimeException {
        return window.lockSurface();
    }

    @Override
    public final void unlockSurface() {
        window.unlockSurface();
    }

    @Override
    public final boolean isSurfaceLockedByOtherThread() {
        return window.isSurfaceLockedByOtherThread();
    }

    @Override
    public final Thread getSurfaceLockOwner() {
        return window.getSurfaceLockOwner();

    }

    @Override
    public final boolean surfaceSwap() {
        return window.surfaceSwap();
    }

    @Override
    public final void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        window.removeSurfaceUpdatedListener(l);
    }

    @Override
    public final void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        window.addSurfaceUpdatedListener(l);
    }

    @Override
    public final void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) throws IndexOutOfBoundsException {
        window.addSurfaceUpdatedListener(index, l);
    }

    @Override
    public final void surfaceUpdated(Object updater, NativeSurface ns, long when) {
        window.surfaceUpdated(updater, ns, when);
    }

    @Override
    public final long getWindowHandle() {
        return window.getWindowHandle();

    }

    @Override
    public final long getSurfaceHandle() {
        return window.getSurfaceHandle();

    }

    @Override
    public final AbstractGraphicsConfiguration getGraphicsConfiguration() {
        return window.getGraphicsConfiguration();
    }

    @Override
    public final long getDisplayHandle() {
        return window.getDisplayHandle();
    }

    @Override
    public final int  getScreenIndex() {
        return window.getScreenIndex();
    }

    /**
     * A most simple JOGL AWT test entry
     */
    public static void main(String args[]) {
        final boolean forceES2;
        final boolean forceES3;
        final boolean forceGL3;
        final boolean forceGL4ES3;
        {
            boolean _forceES2 = false;
            boolean _forceES3 = false;
            boolean _forceGL3 = false;
            boolean _forceGL4ES3 = false;
            if( null != args ) {
                for(int i=0; i<args.length; i++) {
                    if(args[i].equals("-es2")) {
                        _forceES2 = true;
                    } else if(args[i].equals("-es3")) {
                        _forceES3 = true;
                    } else if(args[i].equals("-gl3")) {
                        _forceGL3 = true;
                    } else if(args[i].equals("-gl4es3")) {
                        _forceGL4ES3 = true;
                    }
                }
            }
            forceES2 = _forceES2;
            forceES3 = _forceES3;
            forceGL3 = _forceGL3;
            forceGL4ES3 = _forceGL4ES3;
        }
        System.err.println("forceES2    "+forceES2);
        System.err.println("forceES3    "+forceES3);
        System.err.println("forceGL3    "+forceGL3);
        System.err.println("forceGL4ES3 "+forceGL4ES3);

        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(GlueGenVersion.getInstance());
        System.err.println(JoglVersion.getInstance());

        System.err.println(JoglVersion.getDefaultOpenGLInfo(null, null, true).toString());

        final GLProfile glp;
        if(forceGL4ES3) {
            glp = GLProfile.get(GLProfile.GL4ES3);
        } else if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES3) {
            glp = GLProfile.get(GLProfile.GLES3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else {
            glp = GLProfile.getDefault();
        }
        final GLCapabilitiesImmutable caps = new GLCapabilities( glp );
        System.err.println("Requesting: "+caps);

        GLWindow glWindow = GLWindow.create(caps);
        glWindow.setSize(128, 128);

        glWindow.addGLEventListener(new GLEventListener() {
            @Override
            public void init(GLAutoDrawable drawable) {
                GL gl = drawable.getGL();
                System.err.println(JoglVersion.getGLInfo(gl, null));
                System.err.println("Requested: "+drawable.getNativeSurface().getGraphicsConfiguration().getRequestedCapabilities());
                System.err.println("Chosen   : "+drawable.getChosenGLCapabilities());
                System.err.println("GL impl. class "+gl.getClass().getName());
                if( gl.isGL4ES3() ) {
                    GL4ES3 _gl = gl.getGL4ES3();
                    System.err.println("GL4ES3 retrieved, impl. class "+_gl.getClass().getName());
                }
                if( gl.isGL3() ) {
                    GL3 _gl = gl.getGL3();
                    System.err.println("GL3 retrieved, impl. class "+_gl.getClass().getName());
                }
                if( gl.isGLES3() ) {
                    GLES3 _gl = gl.getGLES3();
                    System.err.println("GLES3 retrieved, impl. class "+_gl.getClass().getName());
                }
                if( gl.isGLES2() ) {
                    GLES2 _gl = gl.getGLES2();
                    System.err.println("GLES2 retrieved, impl. class "+_gl.getClass().getName());
                }
            }

            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            }

            @Override
            public void display(GLAutoDrawable drawable) {
            }

            @Override
            public void dispose(GLAutoDrawable drawable) {
            }
        });

        glWindow.setVisible(true);
        glWindow.destroy();
    }
}
