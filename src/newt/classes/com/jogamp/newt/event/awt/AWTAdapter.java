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

import com.jogamp.newt.util.EDTUtil;
import com.jogamp.newt.impl.Debug;

/**
 * Convenient adapter forwarding AWT events to NEWT via the event listener model.<br>
 * <p>
 * You may attach an instance of this adapter to an AWT Component. When an event happen,
 * it is converted to a NEWT event and the given NEWT listener is being called.<br></p>
 * <p>
 * This adapter fullfills three use cases. First as a plain utility to write code AWT agnostic,
 * ie write an {@link javax.media.opengl.GLEvenListener} and some appropriate NEWT {@link com.jogamp.newt.event.NEWTEventListener}.<br></p>
 * <p>
 * Attach the {@link javax.media.opengl.GLEvenListener} to a NEWT {@link javax.media.opengl.GLAutoDrawable}, e.g. {@link com.jogamp.newt.opengl.GLWindow},<br>
 * or to an AWT {@link javax.media.opengl.GLAutoDrawable}, e.g. {@link javax.media.opengl.awt.GLCanvas}.<br>
 * <br>
 * Attach the NEWT {@link com.jogamp.newt.event.NEWTEventListener} to a NEWT component, e.g. {@link com.jogamp.newt.Window},<br>
 * or to an AWT component, e.g. {@link java.awt.Component}.<br></p>
 * <p>
 * Common:<br>
 * <code>
    javax.media.opengl.GLEvenListener demo1 = new javax.media.opengl.GLEvenListener() { ... } ;<br>
    com.jogamp.newt.event.MouseListener mouseListener = new com.jogamp.newt.event.MouseAdapter() { ... } ;<br>
 * </code> </p>
 * <p>
 * NEWT Usage:<br>
 * <code>
    GLWindow glWindow = GLWindow.create();<br>
    glWindow.addGLEventListener(demo1);<br>
    glWindow.addMouseListener(mouseListener);<br>
 * </code> </p>
 * <p>
 * AWT Usage:<br>
 * <code>
    GLCanvas glCanvas = new GLCanvas();<br>
    glCanvas.addGLEventListener(demo1);<br>
    <br>
    new AWTMouseAdapter(mouseListener).addTo(glCanvas);<br>
 * </code> </p>
 * <p>
 * AWT Usage (previous form in detail):<br>
 * <code>
    AWTMouseAdapter mouseAdapter = new AWTMouseAdapter(mouseListener);<br>
    glCanvas.addMouseListener(mouseAdapter);<br>
    glCanvas.addMouseMotionListener(mouseAdapter);<br>
 * </code> </p>
 *
 * <p>
 * Second is just a litte variation, where we pass a NEWT Window <br>
 * to impersonate as the source of the event.<br></p>
 * <p>
 * <code>
    com.jogamp.newt.event.MouseListener mouseListener = new com.jogamp.newt.event.MouseAdapter() { ... } ;<br>
    Component comp = ... ;                 // the AWT  component<br>
    GLWindow glWindow = GLWindow.create(); // the NEWT component<br>
    <br>
    new AWTMouseAdapter(mouseListener, glWindow).addTo(comp);<br>
 * </code> </p>
 * 
 * Last but not least, the AWTAdapter maybe used as a general AWT event forwarder to NEWT.<br>
 *
 * <p>
 * <code>
    com.jogamp.newt.event.MouseListener mouseListener = new com.jogamp.newt.event.MouseAdapter() { ... } ;<br>
    Component comp = ... ;                 // the AWT  component<br>
    GLWindow glWindow = GLWindow.create(); // the NEWT component<br>
    glWindow.addMouseListener(mouseListener); // add the custom EventListener to the NEWT component<br>
    <br>
    new AWTMouseAdapter(glWindow).addTo(comp); // forward all AWT events to glWindow, as NEWT events<br>
 * </code> </p>
 * 
 * @see #attachTo
 */
public abstract class AWTAdapter implements java.util.EventListener
{
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

    com.jogamp.newt.event.NEWTEventListener newtListener;
    com.jogamp.newt.Window newtWindow;

    /** 
     * Simply wrap aroung a NEWT EventListener, exposed as an AWT EventListener.<br>
     * The NEWT EventListener will be called when an event happens.<br>
     */
    public AWTAdapter(com.jogamp.newt.event.NEWTEventListener newtListener) {
        if(null==newtListener) {
            throw new RuntimeException("Argument newtListener is null");
        }
        this.newtListener = newtListener;
        this.newtWindow = null;
    }

    /** 
     * Wrap aroung a NEWT EventListener, exposed as an AWT EventListener,<br>
     * where the given NEWT Window impersonates as the event's source.
     * The NEWT EventListener will be called when an event happens.<br>
     */
    public AWTAdapter(com.jogamp.newt.event.NEWTEventListener newtListener, com.jogamp.newt.Window newtProxy) {
        if(null==newtListener) {
            throw new RuntimeException("Argument newtListener is null");
        }
        if(null==newtProxy) {
            throw new RuntimeException("Argument newtProxy is null");
        }
        this.newtListener = newtListener;
        this.newtWindow = newtProxy;
    }

    /** 
     * Create a pipeline adapter, AWT EventListener.<br>
     * Once attached to an AWT component, it sends the converted AWT events to the NEWT downstream window.<br>
     * This is only supported with EDT enabled!
     */
    public AWTAdapter(com.jogamp.newt.Window downstream) {
        if(null==downstream) {
            throw new RuntimeException("Argument downstream is null");
        }
        this.newtListener = null;
        this.newtWindow = downstream;
        if( null == newtWindow.getScreen().getDisplay().getEDTUtil() ) {
            throw new RuntimeException("EDT not enabled");
        }
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

    void enqueueEvent(boolean wait, com.jogamp.newt.event.NEWTEvent event) {
        newtWindow.enqueueEvent(wait, event);
    }
}

