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

public class AWTKeyAdapter extends AWTAdapter implements java.awt.event.KeyListener
{
    public AWTKeyAdapter(com.jogamp.newt.event.KeyListener newtListener) {
        super(newtListener);
    }

    public AWTKeyAdapter(com.jogamp.newt.event.KeyListener newtListener, com.jogamp.newt.Window newtProxy) {
        super(newtListener, newtProxy);
    }

    public AWTKeyAdapter(com.jogamp.newt.Window downstream) {
        super(downstream);
    }

    public AWTAdapter addTo(java.awt.Component awtComponent) {
        awtComponent.addKeyListener(this);
        return this;
    }

    public AWTAdapter removeFrom(java.awt.Component awtComponent) {
        awtComponent.removeKeyListener(this);
        return this;
    }

    public void keyPressed(java.awt.event.KeyEvent e) {
        com.jogamp.newt.event.KeyEvent event = AWTNewtEventFactory.createKeyEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.KeyListener)newtListener).keyPressed(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    public void keyReleased(java.awt.event.KeyEvent e) {
        com.jogamp.newt.event.KeyEvent event = AWTNewtEventFactory.createKeyEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.KeyListener)newtListener).keyReleased(event);
        } else {
            enqueueEvent(false, event);
        }
    }

    public void keyTyped(java.awt.event.KeyEvent e) {
        com.jogamp.newt.event.KeyEvent event = AWTNewtEventFactory.createKeyEvent(e, newtWindow);
        if(null!=newtListener) {
            ((com.jogamp.newt.event.KeyListener)newtListener).keyTyped(event);
        } else {
            enqueueEvent(false, event);
        }
    }
}

