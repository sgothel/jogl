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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.jawt.JAWTUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.junit.util.JunitTracer;
import com.jogamp.newt.Window;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.newt.parenting.NewtAWTReparentingKeyAdapter;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestOffscreenLayer02NewtCanvasAWT extends UITestCase {
    static boolean singleBuffer = false;
    static boolean useMSAA = false;
    static boolean addComp = true;
    static int swapInterval = 1;
    static boolean shallUseOffscreenPBufferLayer = false;
    static boolean noAnimation = false;
    static Dimension frameSize0;
    static Dimension frameSize1;
    static Dimension preferredGLSize;
    static long durationPerTest = 1000;
    static boolean waitForKey = false;

    @BeforeClass
    public static void initClass() {
        frameSize0 = new Dimension(500,300);
        frameSize1 = new Dimension(800,600);
        preferredGLSize = new Dimension(400,200);
    }

    private void setupFrameAndShow(final Frame f, final java.awt.Component comp) throws InterruptedException, InvocationTargetException {
        final Container c = new Container();
        c.setLayout(new BorderLayout());
        c.add(new Button("north"), BorderLayout.NORTH);
        c.add(new Button("south"), BorderLayout.SOUTH);
        c.add(new Button("east"), BorderLayout.EAST);
        c.add(new Button("west"), BorderLayout.WEST);
        c.add(comp, BorderLayout.CENTER);

        f.setLayout(new BorderLayout());
        f.add(new Button("NORTH"), BorderLayout.NORTH);
        f.add(new Button("SOUTH"), BorderLayout.SOUTH);
        f.add(new Button("EAST"), BorderLayout.EAST);
        f.add(new Button("WEST"), BorderLayout.WEST);
        f.add(c, BorderLayout.CENTER);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f.pack();
                f.validate();
                f.setVisible(true);
            }});
    }

    private void end(final GLAnimatorControl actrl, final Frame f, final Window w) throws InterruptedException, InvocationTargetException {
        actrl.stop();
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f.dispose();
            } } );
        if(null != w) {
            w.destroy();
        }
    }

    @Test
    public void test01_GLDefault() throws InterruptedException, InvocationTargetException {
        testOffscreenLayerNewtCanvas_Impl(null);
    }

    @Test
    public void test02_GL3() throws InterruptedException, InvocationTargetException {
        if( !GLProfile.isAvailable(GLProfile.GL3) ) {
            System.err.println("GL3 n/a");
            return;
        }
        testOffscreenLayerNewtCanvas_Impl(GLProfile.get(GLProfile.GL3));
    }

    private void testOffscreenLayerNewtCanvas_Impl(final GLProfile glp) throws InterruptedException, InvocationTargetException {
        if(!JAWTUtil.isOffscreenLayerSupported()) {
            System.err.println("offscreen layer n/a");
            return;
        }
        final Frame frame1 = new Frame("AWT Parent Frame");

        final GLCapabilities caps = new GLCapabilities(glp);
        if(singleBuffer) {
            caps.setDoubleBuffered(false);
        }
        if(useMSAA) {
            caps.setNumSamples(4);
            caps.setSampleBuffers(true);
        }
        if(shallUseOffscreenPBufferLayer) {
            caps.setPBuffer(true);
            caps.setOnscreen(true); // get native NEWT Window, not OffscreenWindow
        }
        final GLWindow glWindow1 = GLWindow.create(caps);

        final NewtCanvasAWT newtCanvasAWT1 = new NewtCanvasAWT(glWindow1);
        newtCanvasAWT1.setShallUseOffscreenLayer(true); // trigger offscreen layer - if supported
        newtCanvasAWT1.setPreferredSize(preferredGLSize);
        newtCanvasAWT1.setMinimumSize(preferredGLSize);
        newtCanvasAWT1.setSize(preferredGLSize);

        final GearsES2 demo1 = new GearsES2(swapInterval);
        if(noAnimation) {
            demo1.setDoRotation(false);
        }
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        glWindow1.addKeyListener(new NewtAWTReparentingKeyAdapter(frame1, newtCanvasAWT1, glWindow1));

        frame1.setSize(frameSize0);
        setupFrameAndShow(frame1, newtCanvasAWT1);
        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glWindow1, true));
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(glWindow1, true));
        Assert.assertEquals(newtCanvasAWT1.getNativeWindow(),glWindow1.getParent());
        Assert.assertEquals(true, newtCanvasAWT1.isOffscreenLayerSurfaceEnabled());

        final GLAnimatorControl animator1 = new Animator(glWindow1);
        if(!noAnimation) {
            animator1.start();
        }
        animator1.setUpdateFPSFrames(60, System.err);

        Thread.sleep(durationPerTest/2);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.setSize(frameSize1);
                frame1.pack();
                frame1.validate();
            }});

        Thread.sleep(durationPerTest/2);

        end(animator1, frame1, glWindow1);
        if( waitForKey ) {
            JunitTracer.waitForKey("Continue");
        }
    }

    public static void setDemoFields(final GLEventListener demo, final GLWindow glWindow, final boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        final Window window = glWindow.getDelegatedWindow();
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-layeredPBuffer")) {
                shallUseOffscreenPBufferLayer = true;
            } else if(args[i].equals("-msaa")) {
                useMSAA = true;
            } else if(args[i].equals("-single")) {
                singleBuffer = true;
            } else if(args[i].equals("-still")) {
                noAnimation = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            }
        }
        if(waitForKey) {
            JunitTracer.waitForKey("Start");
        }
        final String tstname = TestOffscreenLayer02NewtCanvasAWT.class.getName();
        /*
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } ); */
        org.junit.runner.JUnitCore.main(tstname);
    }

}
