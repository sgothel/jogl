/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.*;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Bug 816: OSX CALayer Positioning Bug - Swing JFrame w/ 2 JRootPanes and 2 JSplitPanes
 * <p>
 * Diff. OSX CALayer positioning w/ java6, [7uxx..7u40[, and >= 7u40
 * </p>
 * <p>
 * See also {@link com.jogamp.opengl.test.junit.jogl.demos.es2.awt.Bug816AppletOSXCALayerPos03b}
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug816OSXCALayerPos02AWT extends UITestCase {
    static long duration = 1600; // ms
    static int width=640, height=480;

    @Test
    public void test() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(getGLP());

        final JFrame frame = new JFrame("TestBug816OSXCALayerPos02AWT");
        Assert.assertNotNull(frame);

        final GLCanvas glCanvas1 = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas1);
        glCanvas1.addGLEventListener(new GearsES2(1));

        final Animator animator = new Animator();
        animator.add(glCanvas1);

        final QuitAdapter quitAdapter = new QuitAdapter();
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glCanvas1).addTo(frame);

        // Build a GUI where the canvas 3D is located at top right of the frame
        // and can be resized with split panes dividers
        final JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            true, new JScrollPane(), glCanvas1);
        verticalSplitPane.setResizeWeight(0.5);
        final JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            true, new JScrollPane(), verticalSplitPane);
        horizontalSplitPane.setResizeWeight(0.5);
        final JRootPane intermediateRootPane = new JRootPane();
        intermediateRootPane.setContentPane(horizontalSplitPane);
        frame.add(intermediateRootPane);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setSize(width, height);
                frame.setVisible(true);
            }});
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas1, true));

        animator.start();
        Assert.assertTrue(animator.isStarted());
        Assert.assertTrue(animator.isAnimating());

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1 - t0 < duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glCanvas1);

        Assert.assertNotNull(animator);
        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(false);
            }});
        Assert.assertEquals(false, frame.isVisible());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.remove(glCanvas1);
                frame.dispose();
            }});
    }

    static GLProfile getGLP() {
        return GLProfile.getMaxProgrammableCore(true);
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }

        org.junit.runner.JUnitCore.main(TestBug816OSXCALayerPos02AWT.class.getName());
    }
}
