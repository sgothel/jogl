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

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import jogamp.nativewindow.jawt.JAWTUtil;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug664GLCanvasSetVisibleSwingAWT extends UITestCase {
    static long durationPerTest = 500;
    static boolean shallUseOffscreenFBOLayer = false;
    static boolean shallUseOffscreenPBufferLayer = false;
    static GLProfile glp;
    static int width, height;
    static boolean waitForKey = false;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.get(GLProfile.GL2ES2);
            Assert.assertNotNull(glp);
            width  = 640;
            height = 480;
        } else {
            setTestSupported(false);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected JPanel create(final JFrame[] top, final int width, final int height, final int num)
            throws InterruptedException, InvocationTargetException
    {
        final JPanel[] jPanel = new JPanel[] { null };
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    jPanel[0] = new JPanel();
                    jPanel[0].setLayout(new BorderLayout());

                    final JFrame jFrame1 = new JFrame("JFrame #"+num);
                    // jFrame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    jFrame1.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // equivalent to Frame, use windowClosing event!
                    jFrame1.getContentPane().add(jPanel[0]);
                    jFrame1.setSize(width, height);

                    top[0] = jFrame1;
                } } );
        return jPanel[0];
    }

    protected void add(final Container cont, final Component comp, final JFrame jFrame)
            throws InterruptedException, InvocationTargetException
    {
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    cont.add(comp, BorderLayout.CENTER);
                    jFrame.pack();
                    jFrame.validate();
                } } );
    }

    protected void dispose(final GLCanvas glc)
            throws InterruptedException, InvocationTargetException
    {
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    glc.destroy();
                } } );
    }

    protected void setFrameVisible(final JFrame jFrame, final boolean visible) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    jFrame.setVisible(visible);
                } } ) ;
    }

    protected void setComponentVisible(final Component comp, final boolean visible) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    comp.setVisible(visible);
                } } ) ;
    }

    protected void dispose(final JFrame jFrame) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    jFrame.dispose();
                } } ) ;
    }

    private volatile int frameCount = 0;

    protected void runTestGL(final boolean onscreen, final GLCapabilities caps)
            throws AWTException, InterruptedException, InvocationTargetException
    {

        for(int i=0; i<1; i++) {
            final Animator anim = new Animator();
            final GLCanvas glc = new GLCanvas(caps);
            Assert.assertNotNull(glc);
            anim.add(glc);
            if( !onscreen ) {
                glc.setShallUseOffscreenLayer(true);
            }
            final Dimension glc_sz = new Dimension(width, height);
            glc.setMinimumSize(glc_sz);
            glc.setPreferredSize(glc_sz);
            glc.setSize(glc_sz);
            glc.addGLEventListener(new GLEventListener() {
                @Override
                public void init(final GLAutoDrawable drawable) {}
                @Override
                public void dispose(final GLAutoDrawable drawable) {}
                @Override
                public void display(final GLAutoDrawable drawable) {
                    frameCount++;
                }
                @Override
                public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
            });
            final GearsES2 gears = new GearsES2(1);
            gears.setVerbose(false);
            glc.addGLEventListener(gears);

            final JFrame[] top = new JFrame[] { null };
            final Container glcCont = create(top, width, height, i);
            add(glcCont, glc, top[0]);

            System.err.println("XXXX Visible Part 1/3");
            frameCount = 0;
            setFrameVisible(top[0], true);
            Assert.assertTrue("Component didn't become visible", AWTRobotUtil.waitForVisible(glc, true));
            Assert.assertTrue("Component didn't become realized", AWTRobotUtil.waitForRealized(glc, true));

            anim.setUpdateFPSFrames(60, System.err);
            anim.start();
            anim.resetFPSCounter();

            while( anim.getTotalFPSDuration() < durationPerTest ) {
                Thread.sleep(60);
            }

            System.err.println("XXXXX Invisible Part 2/3");
            setComponentVisible(glc, false);
            Assert.assertTrue("Component didn't become invisible", AWTRobotUtil.waitForVisible(glc, false));
            final int frameCountT0 = frameCount;
            anim.resetFPSCounter();

            while( anim.getTotalFPSDuration() < durationPerTest ) {
                Thread.sleep(60);
            }

            final int frameCountT1 = frameCount;
            System.err.println("GLCanvas invisible frame count: Before "+frameCountT0+", after "+frameCountT1);
            Assert.assertTrue("GLCanvas rendered more that 4 times while being invisible, before "+frameCountT0+", after "+frameCountT1,
                    4 >= frameCountT1 - frameCountT0);

            System.err.println("XXXX Visible Part 3/3");
            setComponentVisible(glc, true);
            Assert.assertTrue("Component didn't become visible", AWTRobotUtil.waitForVisible(glc, true));
            anim.resetFPSCounter();

            while( anim.getTotalFPSDuration() < durationPerTest ) {
                Thread.sleep(60);
            }

            System.err.println("GLCanvas isOffscreenLayerSurfaceEnabled: "+glc.isOffscreenLayerSurfaceEnabled()+": "+glc.getChosenGLCapabilities());

            dispose(top[0]);
        }
    }

    @Test
    public void test01Onscreen()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( shallUseOffscreenFBOLayer || shallUseOffscreenPBufferLayer || JAWTUtil.isOffscreenLayerRequired() ) {
            System.err.println("Offscreen test requested or platform requires it.");
            return;
        }
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        if(shallUseOffscreenPBufferLayer) {
            caps.setPBuffer(true);
            caps.setOnscreen(true); // simulate normal behavior ..
        }
        runTestGL(true, caps);
    }

    @Test
    public void test02Offscreen()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( !JAWTUtil.isOffscreenLayerSupported() ) {
            System.err.println("Platform doesn't support offscreen test.");
            return;
        }
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        if(shallUseOffscreenPBufferLayer) {
            caps.setPBuffer(true);
            caps.setOnscreen(true); // simulate normal behavior ..
        }
        runTestGL(false, caps);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    durationPerTest = Long.parseLong(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-layeredFBO")) {
                shallUseOffscreenFBOLayer = true;
            } else if(args[i].equals("-layeredPBuffer")) {
                shallUseOffscreenPBufferLayer = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            }
        }
        System.err.println("waitForKey                    "+waitForKey);

        System.err.println("shallUseOffscreenFBOLayer     "+shallUseOffscreenFBOLayer);
        System.err.println("shallUseOffscreenPBufferLayer "+shallUseOffscreenPBufferLayer);
        if(waitForKey) {
            UITestCase.waitForKey("Start");
        }
        org.junit.runner.JUnitCore.main(TestBug664GLCanvasSetVisibleSwingAWT.class.getName());
    }
}
