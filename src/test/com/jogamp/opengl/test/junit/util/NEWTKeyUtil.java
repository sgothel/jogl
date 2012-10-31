/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.junit.Assert;

import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.newt.event.KeyEvent;

public class NEWTKeyUtil {
    public static class CodeSeg {
        public final int min;
        public final int max;
        public final String description;
        
        public CodeSeg(int min, int max, String description) {
            this.min = min;
            this.max = max;
            this.description = description;
        }
    }
    public static class CodeEvent {
        public final int code;
        public final String description;
        public final KeyEvent event;
        
        public CodeEvent(int code, String description, KeyEvent event) {
            this.code = code;
            this.description = description;
            this.event = event;
        }
        public String toString() {
            return "Code 0x"+Integer.toHexString(code)+" != "+event+" // "+description;
        }
    }
            
    public static void dumpKeyEvents(List<EventObject> keyEvents) {
        for(int i=0; i<keyEvents.size(); i++) {
            System.err.println(i+": "+keyEvents.get(i));
        }        
    }
        
    public static boolean validateKeyCodes(CodeSeg[] codeSegments, List<List<EventObject>> keyEventsList, boolean verbose) {
        final List<CodeEvent> missCodes = new ArrayList<CodeEvent>();
        int totalCodeCount = 0;
        boolean res = true;
        for(int i=0; i<codeSegments.length; i++) {
            final CodeSeg codeSeg = codeSegments[i];
            totalCodeCount += codeSeg.max - codeSeg.min + 1;
            final List<EventObject> keyEvents = keyEventsList.get(i);
            res &= validateKeyCodes(missCodes, codeSeg, keyEvents, verbose);
        }        
        if(verbose) {
            System.err.println("*** Total KeyCode Misses "+missCodes.size()+" / "+totalCodeCount+", valid "+res);
            for(int i=0; i<missCodes.size(); i++) {
                System.err.println("Miss["+i+"]: "+missCodes.get(i));
            }
        }
        return res;
    }
    public static boolean validateKeyCodes(List<CodeEvent> missCodes, CodeSeg codeSeg, List<EventObject> keyEvents, boolean verbose) {
        final int codeCount = codeSeg.max - codeSeg.min + 1;
        int misses = 0;
        for(int i=0; i<codeCount; i++) {
            final int c = codeSeg.min + i;
            final int j = i*3+0; // KEY_PRESSED
            final KeyEvent e = (KeyEvent) ( j < keyEvents.size() ? keyEvents.get(j) : null );
            if( null == e || c != e.getKeyCode() ) {
                missCodes.add(new CodeEvent(c, codeSeg.description, e));
                misses++;
            }
        }
        final boolean res = 3*codeCount == keyEvents.size() && 0 == missCodes.size();
        if(verbose) {
            System.err.println("+++ Code Segment "+codeSeg.description+", Misses: "+misses+" / "+codeCount+", events "+keyEvents.size()+", valid "+res);
        }
        return res;
    }
    
    public static void validateKeyEvent(KeyEvent e, int eventType, int modifier, int keyCode) {
        if(0 <= keyCode) {
            Assert.assertTrue("KeyEvent code mismatch, expected 0x"+Integer.toHexString(keyCode)+", has "+e, keyCode == e.getKeyCode());
        }
        if(0 <= eventType) {
            Assert.assertTrue("KeyEvent type mismatch, expected 0x"+Integer.toHexString(eventType)+", has "+e, eventType == e.getEventType());
        }
        if(0 <= modifier) {
            Assert.assertTrue("KeyEvent modifier mismatch, expected 0x"+Integer.toHexString(modifier)+", has "+e, modifier == e.getModifiers());
        }
    }
    
    public static int getNextKeyEventType(int et) {
        switch( et ) {
            case KeyEvent.EVENT_KEY_PRESSED:
                return KeyEvent.EVENT_KEY_RELEASED;
            case KeyEvent.EVENT_KEY_RELEASED:
                return KeyEvent.EVENT_KEY_TYPED;
            case KeyEvent.EVENT_KEY_TYPED:
                return KeyEvent.EVENT_KEY_PRESSED;
            default:
                Assert.assertTrue("Invalid event type "+et, false);
                return 0;
        }        
    }
    
    public static void validateKeyEventOrder(List<EventObject> keyEvents) {
        IntIntHashMap keyCode2NextEvent = new IntIntHashMap(); 
        for(int i=0; i<keyEvents.size(); i++) {
            final KeyEvent e = (KeyEvent) keyEvents.get(i);
            int eet = keyCode2NextEvent.get(e.getKeyCode());
            if( 0 >= eet ) {
                eet = KeyEvent.EVENT_KEY_PRESSED;
            }
            final int et = e.getEventType();
            Assert.assertEquals("Key event not in proper order", eet, et);
            eet = getNextKeyEventType(et);
            keyCode2NextEvent.put(e.getKeyCode(), eet);
        }        
    }
    
    /**
     * 
     * @param keyAdapter
     * @param expTotalCount number of key press/release/types events
     * @param expARCount number of key press/release/types Auto-Release events
     */
    public static void validateKeyAdapterStats(NEWTKeyAdapter keyAdapter, int expTotalCount, int expARCount) {
        final int keyPressed = keyAdapter.getKeyPressedCount(false);
        final int keyPressedAR = keyAdapter.getKeyPressedCount(true);
        final int keyReleased = keyAdapter.getKeyReleasedCount(false);
        final int keyReleasedAR = keyAdapter.getKeyReleasedCount(true);
        final int keyTyped = keyAdapter.getKeyTypedCount(false);
        final int keyTypedAR = keyAdapter.getKeyTypedCount(true);
        final int keyPressedNR = keyPressed-keyPressedAR;
        final int keyReleasedNR = keyReleased-keyReleasedAR;
        final int keyTypedNR = keyTyped-keyTypedAR;
        System.err.println("Total Press "+keyPressed  +", Release "+keyReleased  +", Typed "+keyTyped);
        System.err.println("AutoR Press "+keyPressedAR+", Release "+keyReleasedAR+", Typed "+keyTypedAR);
        System.err.println("No AR Press "+keyPressedNR+", Release "+keyReleasedNR+", Typed "+keyTypedNR);
        
        final List<EventObject> keyEvents = keyAdapter.getQueued();
        Assert.assertEquals("Key event count not multiple of 3", 0, keyEvents.size()%3);
        Assert.assertEquals("Key event count not 3 * press_release_count", expTotalCount, keyEvents.size());        
        Assert.assertEquals("Key press count failure", expTotalCount/3, keyPressed);
        Assert.assertEquals("Key press count failure (AR)", expARCount/3, keyPressedAR);
        Assert.assertEquals("Key released count failure", expTotalCount/3, keyReleased);
        Assert.assertEquals("Key released count failure (AR)", expARCount/3, keyReleasedAR);
        Assert.assertEquals("Key typed count failure", expTotalCount/3, keyTyped);
        Assert.assertEquals("Key typed count failure (AR)", expARCount/3, keyTypedAR);
        
        // should be true - always, reaching this point - duh!
        Assert.assertEquals( ( expTotalCount - expARCount ) / 3, keyPressedNR);
        Assert.assertEquals( ( expTotalCount - expARCount ) / 3, keyReleasedNR);
        Assert.assertEquals( ( expTotalCount - expARCount ) / 3, keyTypedNR);
    }        
}
