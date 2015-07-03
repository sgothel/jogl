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

package com.jogamp.opengl.test.junit.util;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class AWTKeyAdapter extends java.awt.event.KeyAdapter implements KeyEventCountAdapter {

    String prefix;
    int keyPressed, keyReleased;
    int consumed;
    boolean pressed;
    List<EventObject> queue = new ArrayList<EventObject>();
    boolean verbose = true;

    public AWTKeyAdapter(final String prefix) {
        this.prefix = prefix;
        reset();
    }

    public synchronized void setVerbose(final boolean v) { verbose = v; }

    public synchronized boolean isPressed() {
        return pressed;
    }

    public synchronized int getCount() {
        return keyReleased;
    }

    public synchronized int getConsumedCount() {
        return consumed;
    }

    public synchronized int getKeyPressedCount(final boolean autoRepeatOnly) {
        return keyPressed;
    }

    public synchronized int getKeyReleasedCount(final boolean autoRepeatOnly) {
        return keyReleased;
    }

    public synchronized List<EventObject> copyQueue() {
        return new ArrayList<EventObject>(queue);
    }

    public synchronized int getQueueSize() {
        return queue.size();
    }

    public synchronized void reset() {
        keyPressed = 0;
        keyReleased = 0;
        consumed = 0;
        pressed = false;
        queue.clear();
    }

    public synchronized void keyPressed(final KeyEvent e) {
        pressed = true;
        keyPressed++;
        queue.add(e);
        if( verbose ) {
            System.err.println("KEY AWT PRESSED ["+pressed+"]: "+prefix+", "+e);
        }
    }

    public synchronized void keyReleased(final KeyEvent e) {
        pressed = false;
        keyReleased++;
        if(e.isConsumed()) {
            consumed++;
        }
        queue.add(e);
        if( verbose ) {
            System.err.println("KEY AWT RELEASED ["+pressed+"]: "+prefix+", "+e);
        }
    }

    public String toString() { return prefix+"[pressed "+pressed+", keyReleased "+keyReleased+", consumed "+consumed+"]"; }
}

