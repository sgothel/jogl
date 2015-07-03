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

import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;

import jogamp.nativewindow.SurfaceScaleUtils;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAWT03GLCanvasRecreate01 extends UITestCase {
    static long durationPerTest = 500; // ms

    final static int sizeEps = 64;
    final static Dimension size1 = new Dimension(512,               512-sizeEps-1);
    final static Dimension size2 = new Dimension(512+sizeEps+1+256, 512+256);
    final static Dimension size3 = new Dimension(512-256,           512-sizeEps-1-256);

    Frame frame1=null;
    Frame frame2=null;
    Frame frame3=null;
    GLCanvas glComp=null;
    Label label1 = null;
    Label label2 = null;
    Label label3 = null;
    Animator animator = null;

    @BeforeClass
    public static void startup() {
        System.out.println("GLProfile "+GLProfile.glAvailabilityToString());
    }

    @Before
    public void init() {
        glComp = new GLCanvas();
        Assert.assertNotNull(glComp);
        glComp.addGLEventListener(new GearsES2());

        animator = new Animator(glComp);
        animator.start();

        label1 = new Label("L1 - No GLCanvas");
        label1.setMinimumSize(size1);
        label1.setPreferredSize(size1);
        frame1 = new Frame("Frame 1");
        Assert.assertNotNull(frame1);
        frame1.add(label1);
        frame1.setLocation(0, 0);

        label2 = new Label("L2 - No GLCanvas");
        label2.setMinimumSize(size2);
        label2.setPreferredSize(size2);
        frame2 = new Frame("Frame 2");
        Assert.assertNotNull(frame2);
        frame2.add(label2);
        frame2.setLocation(size1.width + size1.width/2, 0);

        label3 = new Label("L3 - No GLCanvas");
        label3.setMinimumSize(size3);
        label3.setPreferredSize(size3);
        frame3 = new Frame("Frame 3");
        Assert.assertNotNull(frame3);
        frame3.add(label3);
        frame3.setLocation(0, size1.height + size1.height/2);
    }

    @After
    public void release() {
        Assert.assertNotNull(frame1);
        Assert.assertNotNull(frame2);
        Assert.assertNotNull(glComp);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    glComp.destroy();
                    frame1.dispose();
                    frame2.dispose();
                    frame3.dispose();
                }});
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
        frame1=null;
        frame2=null;
        frame3=null;
        glComp=null;

        animator.stop();
        animator=null;
    }

    private void addCanvas(final Frame frame, final Label label, final Dimension size) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.remove(label);
                    glComp.setPreferredSize(size);
                    glComp.setMinimumSize(size);
                    frame.add(glComp);
                    frame.pack();
                }});
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
    }

    private void removeCanvas(final Frame frame, final Label label) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.remove(glComp);
                    frame.add(label);
                    frame.pack();
                    frame.repaint();
                }});
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
    }

    private void setVisible(final Frame frame, final boolean v) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.pack();
                    frame.setVisible(v);
                }});
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
    }

    private void assertSize(final Dimension expSize) {
        final float[] scale = { 1f, 1f };
        glComp.getMaximumSurfaceScale(scale);

        final Dimension hasSize = glComp.getSize(null);

        Assert.assertTrue("AWT Size.width mismatch: expected "+expSize+", has "+hasSize,
                Math.abs(expSize.width-hasSize.width) <= sizeEps);
        Assert.assertTrue("AWT Size.height mismatch: expected "+expSize+", has "+hasSize,
                Math.abs(expSize.height-hasSize.height) <= sizeEps);

        final int expSurfWidth = SurfaceScaleUtils.scale(expSize.width, scale[0]);
        final int expSurfHeight = SurfaceScaleUtils.scale(expSize.height, scale[0]);
        final int hasSurfWidth = glComp.getSurfaceWidth();
        final int hasSurfHeight = glComp.getSurfaceHeight();

        Assert.assertTrue("GL Size.width mismatch: expected "+expSurfWidth+", has "+hasSurfWidth,
                Math.abs(expSurfWidth-hasSurfWidth) <= sizeEps);
        Assert.assertTrue("GL Size.height mismatch: expected "+expSurfHeight+", has "+hasSurfHeight,
                Math.abs(expSurfHeight-hasSurfHeight) <= sizeEps);
    }

    @Test
    public void testAddRemove3Times() throws InterruptedException {
        setVisible(frame1, true);
        setVisible(frame2, true);
        setVisible(frame3, true);

        // Init Frame 1
        addCanvas(frame1, label1, size1);
        Thread.sleep(durationPerTest);
        assertSize(size1);

        // Frame 1 -> Frame 2
        removeCanvas(frame1, label1);
        addCanvas(frame2, label2, size2);
        Thread.sleep(durationPerTest);
        assertSize(size2);

        // Frame 2 -> Frame 3
        removeCanvas(frame2, label2);
        addCanvas(frame3, label3, size3);
        Thread.sleep(durationPerTest);
        assertSize(size3);

        // Frame 3 -> Frame 1
        removeCanvas(frame3, label3);
        addCanvas(frame1, label1, size1);
        Thread.sleep(durationPerTest);
        assertSize(size1);
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            }
        }
        org.junit.runner.JUnitCore.main(TestAWT03GLCanvasRecreate01.class.getName());
    }
}
