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

import java.awt.Dimension;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Test realize GLCanvas and setVisible(true) AWT-Frames on AWT-EDT and on current thread (non AWT-EDT)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug572AWT extends UITestCase {
     static long durationPerTest = 150; // ms

     static class Cleanup implements Runnable {
        Window window;

        public Cleanup(final Window w) {
            window = w;
        }

        public void run() {
            System.err.println("cleaning up...");
            window.setVisible(false);
            try {
                window.removeAll();
            } catch (final Throwable t) {
                Assume.assumeNoException(t);
                t.printStackTrace();
            }
            window.dispose();
        }
    }

    private void testRealizeGLCanvas(final boolean onAWTEDT, final boolean setFrameSize) throws InterruptedException, InvocationTargetException {
        final Window window = new JFrame(this.getSimpleTestName(" - "));
        final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
        final GLCanvas glCanvas = new GLCanvas(caps);
        final SnapshotGLEventListener snapshooter = new SnapshotGLEventListener();
        snapshooter.setMakeSnapshotAlways(true);
        glCanvas.addGLEventListener(new GearsES2());
        glCanvas.addGLEventListener(snapshooter);
        window.add(glCanvas);

        final Runnable realizeAction = new Runnable() {
            @Override
            public void run() {
                // Revalidate size/layout.
                // Always validate if component added/removed.
                // Ensure 1st paint of GLCanvas will have a valid size, hence drawable gets created.
                if( setFrameSize ) {
                    window.setSize(512, 512);
                    window.validate();
                } else {
                    final Dimension size = new Dimension(512, 512);
                    glCanvas.setPreferredSize(size);
                    glCanvas.setMinimumSize(size);
                    window.pack();
                }
                window.setVisible(true);
            } };
        if( onAWTEDT ) {
            // trigger realization on AWT-EDT, otherwise it won't immediatly ..
            SwingUtilities.invokeAndWait( realizeAction );
        } else {
            // trigger realization on non AWT-EDT, realization will happen at a later time ..
            realizeAction.run();

            // Wait until it's displayable after issuing initial setVisible(true) on current thread (non AWT-EDT)!
            Assert.assertTrue("GLCanvas didn't become visible", AWTRobotUtil.waitForVisible(glCanvas, true));
            Assert.assertTrue("GLCanvas didn't become realized", AWTRobotUtil.waitForRealized(glCanvas, true)); // implies displayable
        }

        System.err.println("XXXX-0 "+glCanvas.getDelegatedDrawable().isRealized()+", "+glCanvas);

        Assert.assertTrue("GLCanvas didn't become displayable", glCanvas.isDisplayable());
        Assert.assertTrue("GLCanvas didn't become realized", glCanvas.isRealized());

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

        Thread.sleep(durationPerTest);

        // After initial 'setVisible(true)' all AWT manipulation needs to be done
        // via the AWT EDT, according to the AWT spec.

        // AWT / Swing on EDT..
        SwingUtilities.invokeAndWait(new Cleanup(window));
    }

    @Test(timeout = 10000) // 10s timeout
    public void test01RealizeGLCanvasOnAWTEDTUseFrameSize() throws InterruptedException, InvocationTargetException {
        testRealizeGLCanvas(true, true);
    }

    @Test(timeout = 10000) // 10s timeout
    public void test02RealizeGLCanvasOnAWTEDTUseGLCanvasSize() throws InterruptedException, InvocationTargetException {
        testRealizeGLCanvas(true, false);
    }

    @Test(timeout = 10000) // 10s timeout
    public void test11RealizeGLCanvasOnMainTUseFrameSize() throws InterruptedException, InvocationTargetException {
        testRealizeGLCanvas(false, true);
    }

    @Test(timeout = 10000) // 10s timeout
    public void test12RealizeGLCanvasOnMainTUseGLCanvasSize() throws InterruptedException, InvocationTargetException {
        testRealizeGLCanvas(false, false);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestBug572AWT.class.getName());
    }
}
