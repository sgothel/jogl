/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
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
 * Neither the name Sven Gothel or the names of
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
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
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

    void enqueueEvent(com.jogamp.newt.event.NEWTEvent event) {
        enqueueEvent(false, event);
    }

    void enqueueEvent(boolean wait, com.jogamp.newt.event.NEWTEvent event) {
        try {
            newtWindow.getScreen().getDisplay().enqueueEvent(wait, event);
        } catch (NullPointerException npe) {
            /* that's ok .. window might be down already */
        }
    }
}

