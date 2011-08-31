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
 
package com.jogamp.opengl.test.junit.jogl.awt;

import com.jogamp.opengl.test.junit.util.UITestCase;
import javax.media.opengl.GLProfile;

import java.awt.*;
import java.awt.event.*;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;

public class TestAWT02WindowClosing extends UITestCase {

    static long durationPerTest = 200; // ms

    @Test
    public void test01WindowClosing() throws InterruptedException {
        Frame frame = new Frame();
        frame.setSize(500, 500);
        ClosingWindowAdapter closingWindowAdapter = new ClosingWindowAdapter(frame);
        frame.addWindowListener(closingWindowAdapter);
        final Frame _frame = frame;
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    _frame.setVisible(true);
                }});
        } catch (Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }

        Thread.sleep(durationPerTest);
        if(!closingWindowAdapter.closingCalled) {
            // programatically issue windowClosing
            Toolkit tk = Toolkit.getDefaultToolkit();
            EventQueue evtQ = tk.getSystemEventQueue();
            evtQ.postEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            Thread.sleep(200);
        }
        Assert.assertEquals(true, closingWindowAdapter.closingCalled);
    }

    static class ClosingWindowAdapter extends WindowAdapter {
        boolean closingCalled = false;
        Frame frame;
        public ClosingWindowAdapter(Frame frame) {
            this.frame = frame;
        }
        public void windowClosing(WindowEvent ev) {
                System.out.println("windowClosing() called ..");
                closingCalled = true;
                frame.dispose();
        }
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        System.out.println("durationPerTest: "+durationPerTest);
        org.junit.runner.JUnitCore.main(TestAWT02WindowClosing.class.getName());
    }
}
