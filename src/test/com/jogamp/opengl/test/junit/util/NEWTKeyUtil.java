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
    public static final int TIME_OUT     = 2000; // 2s
    public static final int POLL_DIVIDER   = 20; // TO/20
    public static final int TIME_SLICE   = TIME_OUT / POLL_DIVIDER ;

    public static class CodeSeg {
        public final short min;
        public final short max;
        public final String description;

        public CodeSeg(final int min, final int max, final String description) {
            this.min = (short)min;
            this.max = (short)max;
            this.description = description;
        }
    }
    public static class CodeEvent {
        public final short code;
        public final String description;
        public final KeyEvent event;

        public CodeEvent(final short code, final String description, final KeyEvent event) {
            this.code = code;
            this.description = description;
            this.event = event;
        }
        public String toString() {
            return "Code 0x"+Integer.toHexString( code & 0x0000FFFF )+" != "+event+" // "+description;
        }
    }

    public static void dumpKeyEvents(final List<EventObject> keyEvents) {
        for(int i=0; i<keyEvents.size(); i++) {
            System.err.println(i+": "+keyEvents.get(i));
        }
    }

    public static boolean validateKeyCodes(final CodeSeg[] codeSegments, final List<List<EventObject>> keyEventsList, final boolean verbose) {
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
    public static boolean validateKeyCodes(final List<CodeEvent> missCodes, final CodeSeg codeSeg, final List<EventObject> keyEvents, final boolean verbose) {
        final int codeCount = codeSeg.max - codeSeg.min + 1;
        int misses = 0;
        int evtIdx = 0;
        for(int i=0; i<codeCount; i++) {
            // evtIdx -> KEY_PRESSED !
            final short c = (short) ( codeSeg.min + i );
            final KeyEvent e = (KeyEvent) ( evtIdx < keyEvents.size() ? keyEvents.get(evtIdx) : null );
            if( null == e ) {
                missCodes.add(new CodeEvent(c, codeSeg.description, e));
                misses++;
                evtIdx++;
            } else {
                if( c != e.getKeyCode() ) {
                    missCodes.add(new CodeEvent(c, codeSeg.description, e));
                    misses++;
                }
                evtIdx += 2;
            }
        }
        final boolean res = evtIdx == keyEvents.size() && 0 == missCodes.size();
        if(verbose) {
            System.err.println("+++ Code Segment "+codeSeg.description+", Misses: "+misses+" / "+codeCount+", events "+keyEvents.size()+", valid "+res);
        }
        return res;
    }

    public static void validateKeyEvent(final KeyEvent e, final short eventType, final int modifiers, final short keyCode, final char keyChar) {
        if(0 <= eventType) {
            Assert.assertTrue("KeyEvent type mismatch, expected 0x"+Integer.toHexString(eventType)+", has "+e, eventType == e.getEventType());
        }
        if(0 <= modifiers) {
            Assert.assertTrue("KeyEvent modifier mismatch, expected 0x"+Integer.toHexString(modifiers)+", has "+e, modifiers == e.getModifiers());
        }
        if(KeyEvent.VK_UNDEFINED !=  keyCode) {
            Assert.assertTrue("KeyEvent code mismatch, expected 0x"+Integer.toHexString(keyCode)+", has "+e, keyCode == e.getKeyCode());
        }
        if(KeyEvent.NULL_CHAR != keyChar) {
            Assert.assertTrue("KeyEvent char mismatch, expected 0x"+Integer.toHexString(keyChar)+", has "+e, keyChar == e.getKeyChar());
        }
    }

    public static short getNextKeyEventType(final KeyEvent e) {
        final int et = e.getEventType();
        switch( et ) {
            case KeyEvent.EVENT_KEY_PRESSED:
                return KeyEvent.EVENT_KEY_RELEASED;
            case KeyEvent.EVENT_KEY_RELEASED:
                return KeyEvent.EVENT_KEY_PRESSED;
            default:
                Assert.assertTrue("Invalid event "+e, false);
                return 0;
        }
    }

    public static void validateKeyEventOrder(final List<EventObject> keyEvents) {
        final IntIntHashMap keyCode2NextEvent = new IntIntHashMap();
        for(int i=0; i<keyEvents.size(); i++) {
            final KeyEvent e = (KeyEvent) keyEvents.get(i);
            int eet = keyCode2NextEvent.get(e.getKeyCode());
            if( 0 >= eet ) {
                eet = KeyEvent.EVENT_KEY_PRESSED;
            }
            final int et = e.getEventType();
            Assert.assertEquals("Key event not in proper order "+i+"/"+keyEvents.size()+" - event "+e, eet, et);
            eet = getNextKeyEventType(e);
            keyCode2NextEvent.put(e.getKeyCode(), eet);
        }
    }

    /**
     * @param keyAdapter
     * @param expPressedCountSI number of single key press events
     * @param expReleasedCountSI number of single key release events
     * @param expPressedCountAR number of auto-repeat key press events
     * @param expReleasedCountAR number of auto-repeat key release events
     */
    public static void validateKeyAdapterStats(final NEWTKeyAdapter keyAdapter,
                                               final int expPressedCountSI, final int expReleasedCountSI,
                                               final int expPressedCountAR, final int expReleasedCountAR) {
        final int expPressReleaseCountSI = expPressedCountSI + expReleasedCountSI;
        final int expPressReleaseCountAR = expPressedCountAR + expReleasedCountAR;
        final int expPressReleaseCountALL = expPressReleaseCountSI + expPressReleaseCountAR;

        final int keyPressedALL = keyAdapter.getKeyPressedCount(false);
        final int keyPressedAR = keyAdapter.getKeyPressedCount(true);
        final int keyReleasedALL = keyAdapter.getKeyReleasedCount(false);
        final int keyReleasedAR = keyAdapter.getKeyReleasedCount(true);

        final int keyPressedSI = keyPressedALL-keyPressedAR;
        final int keyReleasedSI = keyReleasedALL-keyReleasedAR;

        final int pressReleaseCountALL = keyPressedALL + keyReleasedALL;
        final int pressReleaseCountSI = keyPressedSI + keyReleasedSI;
        final int pressReleaseCountAR = keyPressedAR + keyReleasedAR;

        System.err.println("Expec Single Press "+expPressedCountSI +", Release "+expReleasedCountSI);
        System.err.println("Expec AutoRp Press "+expPressedCountAR +", Release "+expReleasedCountAR);

        System.err.println("Total Single Press "+keyPressedSI   +", Release "+keyReleasedSI   +", Events "+pressReleaseCountSI);
        System.err.println("Total AutoRp Press "+keyPressedAR   +", Release "+keyReleasedAR   +", Events "+pressReleaseCountAR);
        System.err.println("Total ALL    Press "+keyPressedALL  +", Release "+keyReleasedALL  +", Events "+pressReleaseCountALL);

        Assert.assertEquals("Internal Error: pressReleaseSI != pressReleaseALL - pressReleaseAR", pressReleaseCountSI, pressReleaseCountALL - pressReleaseCountAR);

        Assert.assertEquals("Key press count failure (SI)", expPressedCountSI, keyPressedSI);
        Assert.assertEquals("Key released count failure (SI)", expReleasedCountSI, keyReleasedSI);

        Assert.assertEquals("Key press count failure (AR)", expPressedCountAR, keyPressedAR);
        Assert.assertEquals("Key released count failure (AR)", expReleasedCountAR, keyReleasedAR);

        Assert.assertEquals("Key pressRelease count failure (SI)", expPressReleaseCountSI, pressReleaseCountSI);
        Assert.assertEquals("Key pressRelease count failure (AR)", expPressReleaseCountAR, pressReleaseCountAR);

        final List<EventObject> keyEvents = keyAdapter.copyQueue();

        Assert.assertEquals("Key pressRelease count failure (ALL) w/ list sum  ", expPressReleaseCountALL, pressReleaseCountALL);
        Assert.assertEquals("Key total count failure (ALL) w/ list size ", pressReleaseCountALL, keyEvents.size());
    }
}
