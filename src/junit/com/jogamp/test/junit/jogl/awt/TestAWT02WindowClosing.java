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

package com.jogamp.test.junit.jogl.awt;

import javax.media.opengl.GLProfile;

import java.awt.*;
import java.awt.event.*;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;

public class TestAWT02WindowClosing {
    static {
        GLProfile.initSingleton();
    }

    static long durationPerTest = 200; // ms

    @Test
    public void test01WindowClosing() throws InterruptedException {
        Frame frame = new Frame();
        frame.setSize(500, 500);
        ClosingWindowAdapter closingWindowAdapter = new ClosingWindowAdapter(frame);
        frame.addWindowListener(closingWindowAdapter);
        frame.setVisible(true);

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
