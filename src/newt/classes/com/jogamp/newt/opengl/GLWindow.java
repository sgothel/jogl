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

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.CapabilitiesChooser;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.SurfaceUpdatedListener;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4ES3;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLES3;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.GLSharedContextSetter;

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
 * <p>
 * <a name="contextSharing"><h5>OpenGL Context Sharing</h5></a>
 * To share a {@link GLContext} see the following note in the documentation overview:
 * <a href="../../../../overview-summary.html#SHARING">context sharing</a>
 * as well as {@link GLSharedContextSetter}.
 * </p>
 */
public class GLWindow extends GLAutoDrawableBase implements GLAutoDrawable, Window, NEWTEventConsumer, FPSCounter {
    private final WindowImpl window;

    /**
     * Constructor. Do not call this directly -- use {@link #create()} instead.
     */
    protected GLWindow(final Window window) {
        super(null, null, false /* always handle device lifecycle ourselves */);
        this.window = (WindowImpl) window;
        this.window.setWindowDestroyNotifyAction( new Runnable() {
            @Override
            public void run() {
                defaultWindowDestroyNotifyOp();
            } } );
        window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowRepaint(final WindowUpdateEvent e) {
                    defaultWindowRepaintOp();
                }

                @Override
                public void windowResized(final WindowEvent e) {
                    defaultWindowResizedOp(getSurfaceWidth(), getSurfaceHeight());
                }

            });
        this.window.setLifecycleHook(new GLLifecycleHook());
    }

    @Override
    public final Object getUpstreamWidget() {
        return window;
    }

    @Override
    public final RecursiveLock getUpstreamLock() {
        return window.getLock();
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
    public static GLWindow create(final GLCapabilitiesImmutable caps) {
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
    public static GLWindow create(final Screen screen, final GLCapabilitiesImmutable caps) {
        return new GLWindow(NewtFactory.createWindow(screen, caps));
    }

    /**
     * Creates a new GLWindow attaching the given window.
     * <p>
     * The lifecycle of this Window's Screen and Display is handled via {@link Screen#addReference()}
     * and {@link Screen#removeReference()}.
     * </p>
     */
    public static GLWindow create(final Window window) {
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
    public static GLWindow create(final NativeWindow parentNativeWindow, final GLCapabilitiesImmutable caps) {
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
    public WindowClosingMode setDefaultCloseOperation(final WindowClosingMode op) {
        return window.setDefaultCloseOperation(op);
    }

    //----------------------------------------------------------------------
    // Window Access
    //

    @Override
    public final int getStatePublicBitCount() {
        return window.getStatePublicBitCount();
    }

    @Override
    public final int getStatePublicBitmask() {
        return window.getStatePublicBitmask();
    }

    @Override
    public final int getStateMask() {
        return window.getStateMask();
    }

    @Override
    public final String getStateMaskString() {
        return window.getStateMaskString();
    }

    @Override
    public final int getSupportedStateMask() {
        return window.getSupportedStateMask();
    }

    @Override
    public final String getSupportedStateMaskString() {
        return window.getSupportedStateMaskString();
    }

    @Override
    public CapabilitiesChooser setCapabilitiesChooser(final CapabilitiesChooser chooser) {
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
    public final void setTitle(final String title) {
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
    public final void setPointerVisible(final boolean mouseVisible) {
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
    public final void confinePointer(final boolean grab) {
        window.confinePointer(grab);
    }

    @Override
    public final void setUndecorated(final boolean value) {
        window.setUndecorated(value);
    }

    @Override
    public final void warpPointer(final int x, final int y) {
        window.warpPointer(x, y);
    }
    @Override
    public final boolean isUndecorated() {
        return window.isUndecorated();
    }

    @Override
    public final void setAlwaysOnTop(final boolean value) {
        window.setAlwaysOnTop(value);
    }

    @Override
    public final boolean isAlwaysOnTop() {
        return window.isAlwaysOnTop();
    }

    @Override
    public final void setAlwaysOnBottom(final boolean value) {
        window.setAlwaysOnBottom(value);
    }

    @Override
    public final boolean isAlwaysOnBottom() {
        return window.isAlwaysOnBottom();
    }

    @Override
    public final void setResizable(final boolean value) {
        window.setResizable(value);
    }

    @Override
    public final boolean isResizable() {
        return window.isResizable();
    }

    @Override
    public final void setSticky(final boolean value) {
        window.setSticky(value);
    }

    @Override
    public final boolean isSticky() {
        return window.isSticky();
    }

    @Override
    public final void setMaximized(final boolean horz, final boolean vert) {
        window.setMaximized(horz, vert);
    }

    @Override
    public final boolean isMaximizedVert() {
        return window.isMaximizedVert();
    }

    @Override
    public final boolean isMaximizedHorz() {
        return window.isMaximizedHorz();
    }

    @Override
    public final void setFocusAction(final FocusRunnable focusAction) {
        window.setFocusAction(focusAction);
    }

    @Override
    public void setKeyboardFocusHandler(final KeyListener l) {
        window.setKeyboardFocusHandler(l);
    }

    @Override
    public final void requestFocus() {
        window.requestFocus();
    }

    @Override
    public final void requestFocus(final boolean wait) {
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
    public final boolean setSurfaceScale(final float[] pixelScale) {
        return window.setSurfaceScale(pixelScale);
    }

    @Override
    public final float[] getRequestedSurfaceScale(final float[] result) {
        return window.getRequestedSurfaceScale(result);
    }

    @Override
    public final float[] getCurrentSurfaceScale(final float[] result) {
        return window.getCurrentSurfaceScale(result);
    }

    @Override
    public final float[] getMinimumSurfaceScale(final float[] result) {
        return window.getMinimumSurfaceScale(result);
    }

    @Override
    public final float[] getMaximumSurfaceScale(final float[] result) {
        return window.getMaximumSurfaceScale(result);
    }

    @Override
    public final float[] getPixelsPerMM(final float[] ppmmStore) {
        return window.getPixelsPerMM(ppmmStore);
    }

    @Override
    public final void setPosition(final int x, final int y) {
        window.setPosition(x, y);
    }
    @Override
    public void setTopLevelPosition(final int x, final int y) {
        window.setTopLevelPosition(x, y);
    }

    @Override
    public final boolean setFullscreen(final boolean fullscreen) {
        return window.setFullscreen(fullscreen);
    }

    @Override
    public boolean setFullscreen(final List<MonitorDevice> monitors) {
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
    public final ReparentOperation reparentWindow(final NativeWindow newParent, final int x, final int y, final int hints) {
        return window.reparentWindow(newParent, x, y, hints);
    }
    @Override
    public final boolean isChildWindow() {
        return window.isChildWindow();
    }

    @Override
    public final boolean removeChild(final NativeWindow win) {
        return window.removeChild(win);
    }

    @Override
    public final boolean addChild(final NativeWindow win) {
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
    public void setWindowDestroyNotifyAction(final Runnable r) {
        window.setWindowDestroyNotifyAction(r);
    }

    @Override
    public final void setVisible(final boolean visible) {
        window.setVisible(visible);
    }

    @Override
    public void setVisible(final boolean wait, final boolean visible) {
        window.setVisible(wait, visible);
    }

    @Override
    public final void setSize(final int width, final int height) {
        window.setSize(width, height);
    }
    @Override
    public final void setSurfaceSize(final int pixelWidth, final int pixelHeight) {
        window.setSurfaceSize(pixelWidth, pixelHeight);
    }
    @Override
    public void setTopLevelSize(final int width, final int height) {
        window.setTopLevelSize(width, height);
    }

    @Override
    public final boolean isNativeValid() {
        return window.isNativeValid();
    }

    @Override
    public Point getLocationOnScreen(final Point storage) {
        return window.getLocationOnScreen(storage);
    }

    // Hide methods here ..
    protected class GLLifecycleHook implements WindowImpl.LifecycleHook {

        @Override
        public void preserveGLStateAtDestroy(final boolean value) {
            GLWindow.this.preserveGLStateAtDestroy(value);
        }

        @Override
        public synchronized void destroyActionPreLock() {
            // nop
        }

        @Override
        public synchronized void destroyActionInLock() {
            if(Window.DEBUG_IMPLEMENTATION) {
                final String msg = "GLWindow.destroy() "+WindowImpl.getThreadName()+", start";
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
        public synchronized void setVisibleActionPost(final boolean visible, final boolean nativeWindowCreated) {
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
            final boolean animatorPaused;
            savedAnimator = GLWindow.this.getAnimator();
            if ( null != savedAnimator ) {
                animatorPaused = savedAnimator.pause();
            } else {
                animatorPaused = false;
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
                                } catch(final Throwable t) {
                                    if( DEBUG ) {
                                        System.err.println("Caught "+t.getClass().getName()+": "+t.getMessage());
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
    public boolean consumeEvent(final NEWTEvent event) {
        return window.consumeEvent(event);
    }

    //----------------------------------------------------------------------
    // Window completion
    //
    @Override
    public final void windowRepaint(final int x, final int y, final int width, final int height) {
        window.windowRepaint(x, y, width, height);
    }

    @Override
    public final void enqueueEvent(final boolean wait, final com.jogamp.newt.event.NEWTEvent event) {
        window.enqueueEvent(wait, event);
    }

    @Override
    public final void runOnEDTIfAvail(final boolean wait, final Runnable task) {
        window.runOnEDTIfAvail(wait, task);
    }

    @Override
    public void sendWindowEvent(final int eventType) {
        window.sendWindowEvent(eventType);
    }

    @Override
    public final WindowListener getWindowListener(final int index) {
        return window.getWindowListener(index);
    }

    @Override
    public final WindowListener[] getWindowListeners() {
        return window.getWindowListeners();
    }

    @Override
    public final void removeWindowListener(final WindowListener l) {
        window.removeWindowListener(l);
    }

    @Override
    public final void addWindowListener(final WindowListener l) {
        window.addWindowListener(l);
    }

    @Override
    public final void addWindowListener(final int index, final WindowListener l) throws IndexOutOfBoundsException {
        window.addWindowListener(index, l);
    }

    @Override
    public final void setKeyboardVisible(final boolean visible) {
        window.setKeyboardVisible(visible);
    }

    @Override
    public final boolean isKeyboardVisible() {
        return window.isKeyboardVisible();
    }

    @Override
    public final void addKeyListener(final KeyListener l) {
        window.addKeyListener(l);
    }

    @Override
    public final void addKeyListener(final int index, final KeyListener l) {
        window.addKeyListener(index, l);
    }

    @Override
    public final void removeKeyListener(final KeyListener l) {
        window.removeKeyListener(l);
    }

    @Override
    public final KeyListener getKeyListener(final int index) {
        return window.getKeyListener(index);
    }

    @Override
    public final KeyListener[] getKeyListeners() {
        return window.getKeyListeners();
    }

    @Override
    public final void addMouseListener(final MouseListener l) {
        window.addMouseListener(l);
    }

    @Override
    public final void addMouseListener(final int index, final MouseListener l) {
        window.addMouseListener(index, l);
    }

    @Override
    public final void removeMouseListener(final MouseListener l) {
        window.removeMouseListener(l);
    }

    @Override
    public final MouseListener getMouseListener(final int index) {
        return window.getMouseListener(index);
    }

    @Override
    public final MouseListener[] getMouseListeners() {
        return window.getMouseListeners();
    }

    @Override
    public void setDefaultGesturesEnabled(final boolean enable) {
        window.setDefaultGesturesEnabled(enable);
    }
    @Override
    public boolean areDefaultGesturesEnabled() {
        return window.areDefaultGesturesEnabled();
    }
    @Override
    public final void addGestureHandler(final GestureHandler gh) {
        window.addGestureHandler(gh);
    }
    @Override
    public final void addGestureHandler(final int index, final GestureHandler gh) {
        window.addGestureHandler(index, gh);
    }
    @Override
    public final void removeGestureHandler(final GestureHandler gh) {
        window.removeGestureHandler(gh);
    }
    @Override
    public final void addGestureListener(final GestureHandler.GestureListener gl) {
        window.addGestureListener(-1, gl);
    }
    @Override
    public final void addGestureListener(final int index, final GestureHandler.GestureListener gl) {
        window.addGestureListener(index, gl);
    }
    @Override
    public final void removeGestureListener(final GestureHandler.GestureListener gl) {
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
    public final void removeSurfaceUpdatedListener(final SurfaceUpdatedListener l) {
        window.removeSurfaceUpdatedListener(l);
    }

    @Override
    public final void addSurfaceUpdatedListener(final SurfaceUpdatedListener l) {
        window.addSurfaceUpdatedListener(l);
    }

    @Override
    public final void addSurfaceUpdatedListener(final int index, final SurfaceUpdatedListener l) throws IndexOutOfBoundsException {
        window.addSurfaceUpdatedListener(index, l);
    }

    @Override
    public final void surfaceUpdated(final Object updater, final NativeSurface ns, final long when) {
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
    public static void main(final String args[]) {
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

        final GLWindow glWindow = GLWindow.create(caps);
        glWindow.setSize(128, 128);

        glWindow.addGLEventListener(new GLEventListener() {
            @Override
            public void init(final GLAutoDrawable drawable) {
                final MonitorDevice monitor = glWindow.getMainMonitor();
                System.err.println("Main Monitor: "+monitor);
                final float[] pixelPerMM = monitor.getPixelsPerMM(new float[2]);
                System.err.println("    pixel/mm ["+pixelPerMM[0]+", "+pixelPerMM[1]+"]");
                System.err.println("    pixel/in ["+pixelPerMM[0]*25.4f+", "+pixelPerMM[1]*25.4f+"]");
                final GL gl = drawable.getGL();
                System.err.println(JoglVersion.getGLInfo(gl, null));
                System.err.println("Requested: "+drawable.getNativeSurface().getGraphicsConfiguration().getRequestedCapabilities());
                System.err.println("Chosen   : "+drawable.getChosenGLCapabilities());
                System.err.println("GL impl. class "+gl.getClass().getName());
                if( gl.isGL4ES3() ) {
                    final GL4ES3 _gl = gl.getGL4ES3();
                    System.err.println("GL4ES3 retrieved, impl. class "+_gl.getClass().getName());
                }
                if( gl.isGL3() ) {
                    final GL3 _gl = gl.getGL3();
                    System.err.println("GL3 retrieved, impl. class "+_gl.getClass().getName());
                }
                if( gl.isGLES3() ) {
                    final GLES3 _gl = gl.getGLES3();
                    System.err.println("GLES3 retrieved, impl. class "+_gl.getClass().getName());
                }
                if( gl.isGLES2() ) {
                    final GLES2 _gl = gl.getGLES2();
                    System.err.println("GLES2 retrieved, impl. class "+_gl.getClass().getName());
                }
            }

            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
            }

            @Override
            public void display(final GLAutoDrawable drawable) {
            }

            @Override
            public void dispose(final GLAutoDrawable drawable) {
            }
        });

        glWindow.setVisible(true);
        glWindow.destroy();
    }
}
