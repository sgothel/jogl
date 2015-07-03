/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.newt.event.awt;

import com.jogamp.nativewindow.NativeSurfaceHolder;

import jogamp.newt.Debug;

/**
 * Convenient adapter forwarding AWT events to NEWT via the event listener model.<br>
 * <p>
 * You may attach an instance of this adapter to an AWT Component. When an event happens,
 * it is converted to a NEWT event and the given NEWT listener is being called.<br></p>
 * <p>
 * This adapter fullfills three use cases. First as a plain utility to write code AWT agnostic,
 * ie write an {@link com.jogamp.opengl.GLEvenListener} and some appropriate NEWT {@link com.jogamp.newt.event.NEWTEventListener}.<br></p>
 * <p>
 * Attach the {@link com.jogamp.opengl.GLEvenListener} to a NEWT {@link com.jogamp.opengl.GLAutoDrawable}, e.g. {@link com.jogamp.newt.opengl.GLWindow},<br>
 * or to an AWT {@link com.jogamp.opengl.GLAutoDrawable}, e.g. {@link com.jogamp.opengl.awt.GLCanvas}.<br>
 * <br>
 * Attach the NEWT {@link com.jogamp.newt.event.NEWTEventListener} to a NEWT component, e.g. {@link com.jogamp.newt.Window},<br>
 * or to an AWT component, e.g. {@link java.awt.Component}.<br></p>
 * <p>
 * Common:<br>
 * <pre>
    // your demo/render code
    com.jogamp.opengl.GLEvenListener demo1 = new com.jogamp.opengl.GLEvenListener() { ... } ;

    // your AWT agnostic NEWT mouse listener code
    com.jogamp.newt.event.MouseListener mouseListener = new com.jogamp.newt.event.MouseAdapter() { ... } ;
 * </pre> </p>
 * <p>
 * Default NEWT use case, without using the AWTAdapter:<br>
 * <pre>
    // the NEWT GLAutoDrawable and Window
    GLWindow glWindow = GLWindow.create();

    // attach the renderer demo1
    glWindow.addGLEventListener(demo1);

    // attach the NEWT mouse event listener to glWindow
    glWindow.addMouseListener(mouseListener);
 * </pre> </p>
 * <p>
 * AWT use case, AWTAdapter used as an AWT event translator and forwarder to your NEWT listener:<br>
 * <pre>
    // the AWT GLAutoDrawable and Canvas
    GLCanvas glCanvas = new GLCanvas();

    // attach the renderer demo1
    glCanvas.addGLEventListener(demo1);

    // attach the AWTMouseAdapter to glCanvas, which translates and forwards events to the NEWT mouseListener
    new AWTMouseAdapter(mouseListener).addTo(glCanvas);
 * </pre> </p>
 * <p>
 * Previous code in detail:<br>
 * <pre>
    AWTMouseAdapter mouseAdapter = new AWTMouseAdapter(mouseListener);
    glCanvas.addMouseListener(mouseAdapter);
    glCanvas.addMouseMotionListener(mouseAdapter);
 * </pre> </p>
 *
 * <p>
 * Second use case is just a litte variation of the previous use case, where we pass a NEWT Window <br>
 * to be used as the source of the event.<br></p>
 * <p>
 * <pre>
    com.jogamp.newt.event.MouseListener mouseListener = new com.jogamp.newt.event.MouseAdapter() { ... } ;<br>
    Component comp = ... ;                 // the AWT  component<br>
    GLWindow glWindow = GLWindow.create(); // the NEWT component<br>
    <br>
    new AWTMouseAdapter(mouseListener, glWindow).addTo(comp);<br>
 * </pre> </p>
 *
 * Last but not least, the AWTAdapter maybe used as a general AWT event forwarder to NEWT.<br>
 *
 * <p>
 * <pre>
    com.jogamp.newt.event.MouseListener mouseListener = new com.jogamp.newt.event.MouseAdapter() { ... } ;<br>
    Component comp = ... ;                 // the AWT  component<br>
    GLWindow glWindow = GLWindow.create(); // the NEWT component<br>
    glWindow.addMouseListener(mouseListener); // add the custom EventListener to the NEWT component<br>
    <br>
    new AWTMouseAdapter(glWindow).addTo(comp); // forward all AWT events to glWindow, as NEWT events<br>
 * </pre> </p>
 *
 * @see #attachTo
 */
public abstract class AWTAdapter implements java.util.EventListener
{
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

    com.jogamp.newt.event.NEWTEventListener newtListener;
    com.jogamp.newt.Window newtWindow;
    NativeSurfaceHolder nsHolder;
    boolean consumeAWTEvent;
    protected boolean isSetup;

    /**
     * Create a proxy adapter, wrapping around an NEWT EventListener, exposed as an AWT EventListener,<br>
     * where the given {@link NativeSurfaceHolder} impersonates the event's source.
     * The NEWT EventListener will be called when an event happens.<br>
     */
    protected AWTAdapter(final com.jogamp.newt.event.NEWTEventListener newtListener, final NativeSurfaceHolder nsProxy) {
        if(null==newtListener) {
            throw new IllegalArgumentException("Argument newtListener is null");
        }
        if(null==nsProxy) {
            throw new IllegalArgumentException("Argument nwProxy is null");
        }
        this.newtListener = newtListener;
        this.newtWindow = null;
        this.nsHolder = nsProxy;
        this.consumeAWTEvent = false;
        this.isSetup = true;
    }

    /**
     * Create a proxy adapter, wrapping around an NEWT EventListener, exposed as an AWT EventListener,<br>
     * where the given {@link com.jogamp.newt.Window NEWT Window}, a  {@link NativeSurfaceHolder}, impersonates the event's source.
     * The NEWT EventListener will be called when an event happens.<br>
     */
    protected AWTAdapter(final com.jogamp.newt.event.NEWTEventListener newtListener, final com.jogamp.newt.Window newtProxy) {
        if(null==newtListener) {
            throw new IllegalArgumentException("Argument newtListener is null");
        }
        if(null==newtProxy) {
            throw new IllegalArgumentException("Argument newtProxy is null");
        }
        this.newtListener = newtListener;
        this.newtWindow = newtProxy;
        this.nsHolder = newtProxy;
        this.consumeAWTEvent = false;
        this.isSetup = true;
    }

    /**
     * Create a pipeline adapter, AWT EventListener.<br>
     * Once attached to an AWT component, it sends the converted AWT events to the NEWT downstream window.<br>
     * This is only supported with EDT enabled!
     * @throws IllegalStateException if EDT is not enabled
     */
    protected AWTAdapter(final com.jogamp.newt.Window downstream) throws IllegalStateException {
        this();
        setDownstream(downstream);
    }

    public AWTAdapter() {
        clear();
        this.consumeAWTEvent = false;
    }

    /**
     * Setup a pipeline adapter, AWT EventListener.<br>
     * Once attached to an AWT component, it sends the converted AWT events to the NEWT downstream window.<br>
     * This is only supported with EDT enabled!
     * @throws IllegalStateException if EDT is not enabled
     */
    public synchronized AWTAdapter setDownstream(final com.jogamp.newt.Window downstream) throws IllegalStateException {
        if(null==downstream) {
            throw new RuntimeException("Argument downstream is null");
        }
        this.newtListener = null;
        this.newtWindow = downstream;
        this.nsHolder = downstream;
        if( null == newtWindow.getScreen().getDisplay().getEDTUtil() ) {
            throw new IllegalStateException("EDT not enabled");
        }
        this.isSetup = true;
        return this;
    }

    /**
     * Removes all references, downstream and NEWT-EventListener.
     * <p>
     * Also sets the internal <code>setup</code> flag and {@link #setConsumeAWTEvent(boolean)} to <code>false</code>.
     * </p>
     */
    public synchronized AWTAdapter clear() {
        this.newtListener = null;
        this.newtWindow = null;
        this.nsHolder = null;
        this.isSetup = false;
        this.consumeAWTEvent = false;
        return this;
    }

    public final synchronized void setConsumeAWTEvent(final boolean v) {
        this.consumeAWTEvent = v;
    }

    /**
     * Returns the {@link NativeSurfaceHolder} acting {@link #AWTAdapter(com.jogamp.newt.Window) as downstream},
     * {@link #AWTAdapter(com.jogamp.newt.event.NEWTEventListener, com.jogamp.newt.Window) NEWT window proxy}
     * or as an {@link #AWTAdapter(com.jogamp.newt.event.NEWTEventListener, NativeSurfaceHolder) NativeSurfaceHolder proxy}.
     * <p>
     * Returned value is never null.
     * </p>
     */
    public final synchronized NativeSurfaceHolder getNativeSurfaceHolder() {
        return nsHolder;
    }

    /**
     * Returns the {@link com.jogamp.newt.Window NEWT Window} acting {@link #AWTAdapter(com.jogamp.newt.Window) as downstream}
     * or as a {@link #AWTAdapter(com.jogamp.newt.event.NEWTEventListener, com.jogamp.newt.Window) NEWT window proxy}.
     * <p>
     * Returned value maybe null if instance is used to be a
     * {@link #AWTAdapter(com.jogamp.newt.event.NEWTEventListener, NativeSurfaceHolder) NativeSurfaceHolder proxy}.
     * </p>
     */
    public final synchronized com.jogamp.newt.Window getNewtWindow() {
        return newtWindow;
    }

    /**
     * Returns the {@link com.jogamp.newt.event.NEWTEventListener NEWT event-listener} if instance
     * is used as an {@link #AWTAdapter(com.jogamp.newt.event.NEWTEventListener, NativeSurfaceHolder) NativeSurfaceHolder proxy}
     * or {@link #AWTAdapter(com.jogamp.newt.event.NEWTEventListener, com.jogamp.newt.Window) NEWT window proxy},
     * otherwise method returns <code>null</code>.
     */
    public final synchronized com.jogamp.newt.event.NEWTEventListener getNewtEventListener() {
        return newtListener;
    }

    /**
     * Due to the fact that some NEWT {@link com.jogamp.newt.event.NEWTEventListener}
     * are mapped to more than one {@link java.util.EventListener},
     * this method is for your convenience to use this Adapter as a listener for all types.<br>
     * E.g. {@link com.jogamp.newt.event.MouseListener} is mapped to {@link java.awt.event.MouseListener} and {@link java.awt.event.MouseMotionListener}.
     */
    public abstract AWTAdapter addTo(java.awt.Component awtComponent);

    /** @see #addTo(java.awt.Component) */
    public abstract AWTAdapter removeFrom(java.awt.Component awtComponent);

    /**
     * Return value for {@link AWTAdapter#processEvent(boolean, com.jogamp.newt.event.NEWTEvent) event processing}.
     */
    static enum EventProcRes {
        /** Event shall be dispatched appropriately */
        DISPATCH,
        /** Event has been enqueued */
        ENQUEUED,
        /** No known processing method applies */
        NOP
    }

    /**
     * Process the event.
     * <p>
     * If {@link #getNewtEventListener()} is not <code>null</code>,
     * {@link EventProcRes#DISPATCH DISPATCH} is returned and caller shall dispatch the event appropriately.
     * </p>
     * <p>
     * If {@link #getNewtWindow()} is not <code>null</code>,
     * {@link EventProcRes#ENQUEUED ENQUEUED} is returned and the event has been {@link com.jogamp.newt.Window#enqueueEvent(boolean, com.jogamp.newt.event.NEWTEvent) enqueued already}.
     * </p>
     * <p>
     * If none of the above matches, {@link EventProcRes#NOP NOP} is returned and none of the above processing method applies.
     * </p>
     *
     * @param wait In case the event will be {@link EventProcRes#ENQUEUED ENQUEUED},
     *             passing <code>true</code> will block until the event has been processed, otherwise method returns immediately.
     * @param event The {@link com.jogamp.newt.event.NEWTEvent event} to enqueue.
     * @return One of the {@link EventProcRes} values, see above.
     */
    EventProcRes processEvent(final boolean wait, final com.jogamp.newt.event.NEWTEvent event) {
        if(null != newtListener) {
            return EventProcRes.DISPATCH;
        }
        if( null != newtWindow ) {
            newtWindow.enqueueEvent(wait, event);
            return EventProcRes.ENQUEUED;
        }
        return EventProcRes.NOP;
    }
}

