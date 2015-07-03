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

import jogamp.newt.awt.event.AWTNewtEventFactory;

/**
 * AWT:
 *   printable:     PRESSED (t0), TYPED (t0), RELEASED (t1)
 *   non-printable: PRESSED (t0), RELEASED (t1)
 */
public class AWTKeyAdapter extends AWTAdapter implements java.awt.event.KeyListener
{
    public AWTKeyAdapter(final com.jogamp.newt.event.KeyListener newtListener, final NativeSurfaceHolder nsProxy) {
        super(newtListener, nsProxy);
    }

    public AWTKeyAdapter(final com.jogamp.newt.event.KeyListener newtListener, final com.jogamp.newt.Window newtProxy) {
        super(newtListener, newtProxy);
    }

    public AWTKeyAdapter(final com.jogamp.newt.Window downstream) {
        super(downstream);
    }

    public AWTKeyAdapter() {
        super();
    }

    @Override
    public synchronized AWTAdapter addTo(final java.awt.Component awtComponent) {
        awtComponent.addKeyListener(this);
        return this;
    }

    @Override
    public synchronized AWTAdapter removeFrom(final java.awt.Component awtComponent) {
        awtComponent.removeKeyListener(this);
        return this;
    }

    @Override
    public synchronized void keyPressed(final java.awt.event.KeyEvent e) {
        if( !isSetup ) { return; }
        final com.jogamp.newt.event.KeyEvent event = AWTNewtEventFactory.createKeyEvent(com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED, e, nsHolder);
        if( consumeAWTEvent ) {
            e.consume();
        }
        if( EventProcRes.DISPATCH == processEvent(false, event) ) {
            ((com.jogamp.newt.event.KeyListener)newtListener).keyPressed(event);
        }
    }

    @Override
    public synchronized void keyReleased(final java.awt.event.KeyEvent e) {
        if( !isSetup ) { return; }
        final com.jogamp.newt.event.KeyEvent event = AWTNewtEventFactory.createKeyEvent(com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED, e, nsHolder);
        if( consumeAWTEvent ) {
            e.consume();
        }
        if( EventProcRes.DISPATCH == processEvent(false, event) ) {
            ((com.jogamp.newt.event.KeyListener)newtListener).keyReleased(event);
        }
    }

    @Override
    public synchronized void keyTyped(final java.awt.event.KeyEvent e) {
        if( !isSetup ) { return; }
        if( consumeAWTEvent ) {
            e.consume();
        }
    }
}

