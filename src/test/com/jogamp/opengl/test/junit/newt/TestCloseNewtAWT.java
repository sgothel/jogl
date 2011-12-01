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

package com.jogamp.opengl.test.junit.newt;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.util.Point;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.Window;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

public class TestCloseNewtAWT extends UITestCase {

    GLWindow newtWindow = null;
    NewtCanvasAWT newtCanvas = null;
    JFrame frame = null;

    @SuppressWarnings("serial")
    class MyCanvas extends NewtCanvasAWT {
         public MyCanvas(Window window) {
            super(window);
         }

         public void addNotify() {
            System.err.println("MyCanvas START add: "+Thread.currentThread()+", holds AWTTreeLock: "+Thread.holdsLock(this.getTreeLock()));
            super.addNotify();
            System.err.println("MyCanvas END add: "+Thread.currentThread()+", holds AWTTreeLock: "+Thread.holdsLock(this.getTreeLock()));
         }

         public void removeNotify() {
            System.err.println("MyCanvas START remove: "+Thread.currentThread()+", holds AWTTreeLock: "+Thread.holdsLock(this.getTreeLock()));

            // trigger critical situation around the AWT TreeLock
            newtWindow.runOnEDTIfAvail(true, new Runnable() {
                public void run() {
                    // NEWT EDT while AWT is locked
                    System.err.println("MyCanvas On NEWT-EDT From AWT-EDT: "+Thread.currentThread()+
                                       ", holds AWTTreeLock: "+Thread.holdsLock(MyCanvas.this.getTreeLock()));

                    // Critical: Within NEWT EDT, while AWT is locked
                    NativeWindow nw = MyCanvas.this.getNativeWindow();
                    if(null != nw) {
                        Point p = nw.getLocationOnScreen(null);
                        System.err.println("MyCanvas On NEWT-EDT: position: "+p);                        
                    } else {
                        System.err.println("MyCanvas On NEWT-EDT: position n/a, null NativeWindow");
                    }
                }
            });
            System.err.println("MyCanvas passed critical: "+Thread.currentThread()+", holds AWTTreeLock: "+Thread.holdsLock(this.getTreeLock()));

            super.removeNotify();

            System.err.println("MyCanvas END remove: "+Thread.currentThread()+", holds AWTTreeLock: "+Thread.holdsLock(this.getTreeLock()));
         }
    }

    @Test
    public void testCloseNewtAWT() throws InterruptedException, InvocationTargetException {
        newtWindow = GLWindow.create(new GLCapabilities(GLProfile.getDefault()));
        newtCanvas = new MyCanvas(newtWindow);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame("NEWT Close Test");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.getContentPane().add(newtCanvas);
                frame.pack();
                frame.setSize(800, 600);
                frame.setVisible(true);
            }
        });
        Thread.sleep(500);

        Assert.assertEquals(true,  AWTRobotUtil.closeWindow(frame, true));
    }

    public static void main(String[] args) {
        String tstname = TestCloseNewtAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
