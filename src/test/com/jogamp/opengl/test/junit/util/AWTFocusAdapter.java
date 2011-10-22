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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class AWTFocusAdapter implements FocusEventCountAdapter, FocusListener {

    String prefix;
    int focusCount;
    boolean wasTemporary;

    public AWTFocusAdapter(String prefix) {
        this.prefix = prefix;
        reset();
    }

    public boolean focusLost() {
        return focusCount<0;        
    }
    
    public boolean focusGained() {
        return focusCount>0;
    }
        
    public void reset() {
        focusCount = 0;
        wasTemporary = false;
    }

    /** @return true, if the last change was temporary */
    public boolean getWasTemporary() {
        return wasTemporary;
    }

    /* @Override */
    public void focusGained(FocusEvent e) {
        if(focusCount<0) { focusCount=0; }
        focusCount++;
        wasTemporary = e.isTemporary();
        System.err.println("FOCUS AWT  GAINED "+(wasTemporary?"TEMP":"PERM")+" [fc "+focusCount+"]: "+prefix+", "+e);
    }

    /* @Override */
    public void focusLost(FocusEvent e) {
        if(focusCount>0) { focusCount=0; }
        focusCount--;
        wasTemporary = e.isTemporary();
        System.err.println("FOCUS AWT  LOST   "+(wasTemporary?"TEMP":"PERM")+" [fc "+focusCount+"]: "+prefix+", "+e);
    }
    
    public String toString() { return prefix+"[focusCount "+focusCount +", temp "+wasTemporary+"]"; }    
}
