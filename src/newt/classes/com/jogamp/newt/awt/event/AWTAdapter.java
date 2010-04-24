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
package com.jogamp.newt.awt.event;

/**
 * Convenient adapter forwarding AWT events to NEWT via the event listener model.<br>
 *
 * You may attach an instance of this adapter to an AWT Component. When an event happen,
 * it is converted to a NEWT event and the given NEWT listener is being called.<br>
 *
 * This adapter fullfills three use cases. First as a plain utility to write code AWT agnostic,
 * ie write an {@link javax.media.opengl.GLEvenListener} and some appropriate NEWT {@link com.jogamp.newt.event.EventListener}.<br>
 *
 * Attach the {@link javax.media.opengl.GLEvenListener} to a NEWT {@link javax.media.opengl.GLAutoDrawable}, e.g. {@link com.jogamp.newt.opengl.GLWindow},
 * or to an AWT {@link javax.media.opengl.GLAutoDrawable}, e.g. {@link javax.media.opengl.awt.GLCanvas}.<br>
 * Attach the NEWT {@link com.jogamp.newt.event.EventListener} to a NEWT component, e.g. {@link com.jogamp.newt.Window},
 * or to an AWT component, e.g. {@link java.awt.Component}.<br><br>
 * <code>
    javax.media.opengl.GLEvenListener demo1 = new javax.media.opengl.GLEvenListener() { ... } ;
    com.jogamp.newt.event.MouseListener mouseListener = new com.jogamp.newt.event.MouseAdapter() { ... } ;

    // NEWT Usage
    GLWindow glWindow = GLWindow.create();
    glWindow.addGLEventListener(demo1);
    glWindow.addMouseListener(mouseListener);
    ..

    // AWT Usage
    GLCanvas glCanvas = new GLCanvas();
    glCanvas.addGLEventListener(demo1);
    new AWTMouseAdapter(mouseListener).addTo(glCanvas);

    // This last line is nothing else but a simplified form of:
    AWTMouseAdapter mouseAdapter = new AWTMouseAdapter(mouseListener);
    glCanvas.addMouseListener(mouseAdapter);
    glCanvas.addMouseMotionListener(mouseAdapter);
 * </code>
 *
 * Second is just a litte variation, where we pass a NEWT Window 
 * to impersonate as the source of the event.<br>
 * 
 * <code>
    com.jogamp.newt.event.MouseListener mouseListener = new com.jogamp.newt.event.MouseAdapter() { ... } ;
    Component comp = ... ;                 // the AWT  component
    GLWindow glWindow = GLWindow.create(); // the NEWT component

    new AWTMouseAdapter(mouseListener, glWindow).addTo(comp);
 * </code>
 * 
 * Last but not least, the AWTAdapter maybe used as a general AWT event forwarder to NEWT.<br>
 *
 * <code>
    com.jogamp.newt.event.MouseListener mouseListener = new com.jogamp.newt.event.MouseAdapter() { ... } ;
    Component comp = ... ;                 // the AWT  component
    GLWindow glWindow = GLWindow.create(); // the NEWT component
    glWindow.addMouseListener(mouseListener); // add the custom EventListener to the NEWT component

    new AWTMouseAdapter(glWindow).addTo(comp); // forward all AWT events to glWindow, as NEWT events
 * </code>
 * 
 * </code>
 *
 * @see #attachTo
 */
public abstract class AWTAdapter implements java.util.EventListener
{
    com.jogamp.newt.event.EventListener newtListener;
    com.jogamp.newt.Window newtWindow;

    /** 
     * Simply wrap aroung a NEWT EventListener, exposed as an AWT EventListener.<br>
     * The NEWT EventListener will be called when an event happens.<br>
     */
    public AWTAdapter(com.jogamp.newt.event.EventListener newtListener) {
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
    public AWTAdapter(com.jogamp.newt.event.EventListener newtListener, com.jogamp.newt.Window newtProxy) {
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
     */
    public AWTAdapter(com.jogamp.newt.Window downstream) {
        if(null==downstream) {
            throw new RuntimeException("Argument downstream is null");
        }
        this.newtListener = null;
        this.newtWindow = downstream;
    }

    /** 
     * Due to the fact that some NEWT {@link com.jogamp.newt.event.EventListener}
     * are mapped to more than one {@link java.util.EventListener},
     * this method is for your convenience to use this Adapter as a listener for all types.<br>
     * E.g. {@link com.jogamp.newt.event.MouseListener} is mapped to {@link java.awt.event.MouseListener} and {@link java.awt.event.MouseMotionListener}.
     */
    public abstract AWTAdapter addTo(java.awt.Component awtComponent);

    void enqueueEvent(com.jogamp.newt.event.Event event) {
        try {
            newtWindow.getScreen().getDisplay().enqueueEvent(event);
        } catch (NullPointerException npe) {
            /* that's ok .. window might be down already */
        }
    }
}

