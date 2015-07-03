/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;


/**
 * TestMultipleNewtCanvasAWT
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMultipleNewtCanvasAWT extends UITestCase {

    static long durationPerTest = 1000;

    @BeforeClass
    public static void initClass() {
        if(!GLProfile.isAvailable(GLProfile.GL2ES2)) {
            setTestSupported(false);
        }
    }

    @Test
    public void test01() throws InterruptedException {
        testImpl();
    }

    public void testImpl() throws InterruptedException {
        final JFrame frame = new JFrame(this.getSimpleTestName("."));

        //
        // GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile.get(GLProfile.GL2));
        // GLContext sharedContext = factory.getOrCreateSharedContext(factory.getDefaultDevice());
        //
        final GLCapabilities glCapabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(4);

        final GearsES2 eventListener1 = new GearsES2(0);
        final GearsES2 eventListener2 = new GearsES2(1);

        final Component openGLComponent1;
        final Component openGLComponent2;
        final GLAutoDrawable openGLAutoDrawable1;
        final GLAutoDrawable openGLAutoDrawable2;

        final GLWindow glWindow1 = GLWindow.create(glCapabilities);
        final NewtCanvasAWT newtCanvasAWT1 = new NewtCanvasAWT(glWindow1);
        newtCanvasAWT1.setPreferredSize(new Dimension(640, 480));
        glWindow1.addGLEventListener(eventListener1);
        //
        final GLWindow glWindow2 = GLWindow.create(glCapabilities);
        final NewtCanvasAWT newtCanvasAWT2 = new NewtCanvasAWT(glWindow2);
        newtCanvasAWT2.setPreferredSize(new Dimension(640, 480));
        glWindow2.addGLEventListener(eventListener2);

        openGLComponent1 = newtCanvasAWT1;
        openGLComponent2 = newtCanvasAWT2;
        openGLAutoDrawable1 = glWindow1;
        openGLAutoDrawable2 = glWindow2;

        // group both OpenGL canvases / windows into a horizontal panel
        final JPanel openGLPanel = new JPanel();
        openGLPanel.setLayout(new BoxLayout(openGLPanel, BoxLayout.LINE_AXIS));
        openGLPanel.add(openGLComponent1);
        openGLPanel.add(Box.createHorizontalStrut(5));
        openGLPanel.add(openGLComponent2);

        final JPanel mainPanel = (JPanel) frame.getContentPane();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        mainPanel.add(Box.createHorizontalGlue());
        mainPanel.add(openGLPanel);
        mainPanel.add(Box.createHorizontalGlue());

        final Animator animator = new Animator(Thread.currentThread().getThreadGroup());
        animator.setUpdateFPSFrames(1, null);
        animator.add(openGLAutoDrawable1);
        animator.add(openGLAutoDrawable2);

        // make the window visible using the EDT
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                frame.pack();
                frame.setVisible(true);
            }
        });

        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(openGLComponent1, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(openGLComponent2, true));

        animator.start();

        // sleep for test duration, then request the window to close, wait for the window to close,s and stop the animation
        while(animator.isAnimating() && animator.getTotalFPSDuration() < durationPerTest) {
            Thread.sleep(100);
        }

        animator.stop();

        // ask the EDT to dispose of the frame;
        // if using newt, explicitly dispose of the canvases because otherwise it seems our destroy methods are not called
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                newtCanvasAWT1.destroy(); // removeNotify does not destroy GLWindow
                newtCanvasAWT2.destroy(); // removeNotify does not destroy GLWindow
                frame.dispose();
            }
        });
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(frame, false));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(openGLComponent1, false));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(openGLComponent2, false));
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String[] args) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                if (++i < args.length) {
                    durationPerTest = atoi(args[i]);
                }
            }
        }
        org.junit.runner.JUnitCore.main(TestMultipleNewtCanvasAWT.class.getName());
    }

}

