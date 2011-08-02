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

import jogamp.newt.awt.event.AWTNewtEventFactory;

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
            enqueueEvent(false, event);
        }
    }

    public void mouseEntered(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseEntered(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    public void mouseExited(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseExited(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    public void mousePressed(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mousePressed(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    public void mouseReleased(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseReleased(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    public void mouseDragged(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseDragged(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    public void mouseMoved(java.awt.event.MouseEvent e) {
        com.jogamp.newt.event.MouseEvent event = AWTNewtEventFactory.createMouseEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.MouseListener)newtListener).mouseMoved(event);
        } else {
            enqueueEvent(false, event);
        }
    }
}

