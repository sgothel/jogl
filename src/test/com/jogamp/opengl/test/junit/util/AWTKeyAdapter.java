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

public class AWTKeyAdapter extends java.awt.event.KeyAdapter implements InputEventCountAdapter {

    String prefix;
    int keyTyped;
    boolean pressed;

    public AWTKeyAdapter(String prefix) {
        this.prefix = prefix;
        reset();
    }

    public boolean isPressed() {
        return pressed;
    }
    
    public int getCount() {
        return keyTyped;
    }

    public void reset() {
        keyTyped = 0;
        pressed = false;
    }

    public void keyPressed(KeyEvent e) {
        pressed = true;
        System.err.println("KEY AWT PRESSED ["+pressed+"]: "+prefix+", "+e);
    }

    public void keyReleased(KeyEvent e) {
        pressed = false;
        System.err.println("KEY AWT RELEASED ["+pressed+"]: "+prefix+", "+e);
    }

    public void keyTyped(java.awt.event.KeyEvent e) {
        ++keyTyped;
        System.err.println("KEY AWT  TYPED ["+keyTyped+"]: "+prefix+", "+e);
    }
    
    public String toString() { return prefix+"[pressed "+pressed+", typed "+keyTyped+"]"; }
}

