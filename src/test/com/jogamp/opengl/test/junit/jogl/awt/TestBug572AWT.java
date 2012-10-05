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
 
package com.jogamp.opengl.test.junit.jogl.awt;

import java.awt.Window;
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import junit.framework.Assert;

import org.junit.Assume;
import org.junit.Test;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Tests context creation + display on various kinds of Window implementations.
 */
public class TestBug572AWT extends UITestCase {

    protected void runTestGL() throws InterruptedException, InvocationTargetException {
        final Window window = new JFrame(this.getSimpleTestName(" - "));
        final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());        
        final GLCanvas glCanvas = new GLCanvas(caps);
        final SnapshotGLEventListener snapshooter = new SnapshotGLEventListener();
        snapshooter.setMakeSnapshotAlways(true);
        glCanvas.addGLEventListener(new GearsES2());
        glCanvas.addGLEventListener(snapshooter);
        window.add(glCanvas);

        // Revalidate size/layout.
        // Always validate if component added/removed.
        // Ensure 1st paint of GLCanvas will have a valid size, hence drawable gets created.
        window.setSize(512, 512);
        window.validate();

        window.setVisible(true);
        System.err.println("XXXX-0 "+glCanvas.getDelegatedDrawable().isRealized()+", "+glCanvas);
        
        // Immediately displayable after issuing initial setVisible(true) .. even not within AWT-EDT ?
        Assert.assertTrue("GLCanvas didn't become displayable", glCanvas.isDisplayable());
        Assert.assertTrue("GLCanvas didn't become realized", glCanvas.isRealized());
        
        // Would be required if not immediately displayable ...   
        // Assert.assertTrue("GLCanvas didn't become displayable and realized", AWTRobotUtil.waitForRealized(glCanvas, true));
        // System.err.println("XXXX-1 "+glCanvas.getDelegatedDrawable().isRealized()+", "+glCanvas);
        
        // The AWT-EDT reshape/repaint events happen offthread later ..
        System.err.println("XXXX-1 reshapeCount "+snapshooter.getReshapeCount());
        System.err.println("XXXX-1 displayCount "+snapshooter.getDisplayCount());
        
        // Wait unitl AWT-EDT has issued reshape/repaint
        for (int wait=0; wait<AWTRobotUtil.POLL_DIVIDER &&
                         ( 0 == snapshooter.getReshapeCount() || 0 == snapshooter.getDisplayCount() ); 
             wait++) {
            Thread.sleep(AWTRobotUtil.TIME_SLICE);
        }
        System.err.println("XXXX-2 reshapeCount "+snapshooter.getReshapeCount());
        System.err.println("XXXX-2 displayCount "+snapshooter.getDisplayCount());
        
        Assert.assertTrue("GLCanvas didn't reshape", snapshooter.getReshapeCount()>0);
        Assert.assertTrue("GLCanvas didn't display", snapshooter.getDisplayCount()>0);
        
        // After initial 'setVisible(true)' all AWT manipulation needs to be done
        // via the AWT EDT, according to the AWT spec.

        Runnable cleanup = new Runnable() {
            public void run() {
                System.err.println("cleaning up...");
                window.setVisible(false);
                try {
                    window.removeAll();
                } catch (Throwable t) {
                    Assume.assumeNoException(t);
                    t.printStackTrace();
                }
                window.dispose();
            }

        };

        // AWT / Swing on EDT..
        SwingUtilities.invokeAndWait(cleanup);
    }

    @Test
    public void test01() throws InterruptedException, InvocationTargetException {
        runTestGL();
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestBug572AWT.class.getName());
    }
}
