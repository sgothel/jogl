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

public class AWTMouseAdapter extends AWTAdapter implements java.awt.event.MouseListener, java.awt.event.MouseMotionListener
{
    public AWTMouseAdapter(com.jogamp.newt.event.MouseListener newtListener) {
        super(newtListener);
    }

    public AWTMouseAdapter(com.jogamp.newt.event.MouseListener newtListener, com.jogamp.newt.Window newtProxy) {
        super(newtListener, newtProxy);
    }

    public AWTMouseAdapter(com.jogamp.newt.Window downstream) {
        super(downstream);
    }

    public AWTAdapter addTo(java.awt.Component awtComponent) {
        awtComponent.addMouseListener(this);
        awtComponent.addMouseMotionListener(this);
        return this;
    }

    public AWTAdapter removeFrom(java.awt.Component awtComponent) {
        awtComponent.removeMouseListener(this);
        awtComponent.removeMouseMotionListener(this);
        return this;
    }

    public void mouseClicked(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseClicked(event);
        } else {
            enqueueEvent(event);
        }
    }

    public void mouseEntered(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseEntered(event);
        } else {
            enqueueEvent(event);
        }
    }

    public void mouseExited(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseExited(event);
        } else {
            enqueueEvent(event);
        }
    }

    public void mousePressed(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mousePressed(event);
        } else {
            enqueueEvent(event);
        }
    }

    public void mouseReleased(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseReleased(event);
        } else {
            enqueueEvent(event);
        }
    }

    public void mouseDragged(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseDragged(event);
        } else {
            enqueueEvent(event);
        }
    }

    public void mouseMoved(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseMoved(event);
        } else {
            enqueueEvent(event);
        }
    }
}

