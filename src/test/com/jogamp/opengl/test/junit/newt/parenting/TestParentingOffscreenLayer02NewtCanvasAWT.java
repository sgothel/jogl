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
 
package com.jogamp.opengl.test.junit.newt.parenting;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;

import jogamp.nativewindow.jawt.JAWTUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.newt.Window;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

public class TestParentingOffscreenLayer02NewtCanvasAWT extends UITestCase {
    static Dimension frameSize0;
    static Dimension frameSize1;
    static Dimension preferredGLSize;
    static Dimension minGLSize;
    static long durationPerTest = 1000;

    @BeforeClass
    public static void initClass() {
        frameSize0 = new Dimension(500,300);
        frameSize1 = new Dimension(800,600);
        preferredGLSize = new Dimension(400,200);
        minGLSize = new Dimension(200,100);
    }

    private void setupFrameAndShow(final Frame f, java.awt.Component comp) throws InterruptedException, InvocationTargetException {
        
        Container c = new Container();
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
                f.validate();
                f.setVisible(true);
            }});        
    }
    private void end(GLAnimatorControl actrl, final Frame f, Window w) throws InterruptedException, InvocationTargetException {
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
    public void testOnscreenLayerNewtCanvas_Onscreen() throws InterruptedException, InvocationTargetException {
        testOffscreenLayerNewtCanvas_Impl(false, false);
    }
    
    @Test
    public void testOffscreenLayerNewtCanvas_OffscreenLayerWithOffscreenClass() throws InterruptedException, InvocationTargetException {
        testOffscreenLayerNewtCanvas_Impl(true, true);
    }
    
    @Test
    public void testOffscreenLayerNewtCanvas_OffscreenLayerWithOnscreenClass() throws InterruptedException, InvocationTargetException {
        testOffscreenLayerNewtCanvas_Impl(true, false);
    }
    
    private void testOffscreenLayerNewtCanvas_Impl(boolean offscreenLayer, boolean offscreenClass) throws InterruptedException, InvocationTargetException {
        final Frame frame1 = new Frame("AWT Parent Frame");
        
        GLCapabilities glCaps = new GLCapabilities(null);
        if(offscreenClass) {
            glCaps.setOnscreen(false);
            glCaps.setPBuffer(true);
        }

        GLWindow glWindow1 = GLWindow.create(glCaps);
        
        final NewtCanvasAWT newtCanvasAWT1 = new NewtCanvasAWT(glWindow1);
        newtCanvasAWT1.setShallUseOffscreenLayer(offscreenLayer); // trigger offscreen layer - if supported
        newtCanvasAWT1.setPreferredSize(preferredGLSize);
        newtCanvasAWT1.setMinimumSize(minGLSize);
        
        GLEventListener demo1 = new GearsES2(1);
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        glWindow1.addKeyListener(new NewtAWTReparentingKeyAdapter(frame1, newtCanvasAWT1, glWindow1));
        
        frame1.setSize(frameSize0);
        setupFrameAndShow(frame1, newtCanvasAWT1);
        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glWindow1, true));
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(glWindow1, true));
        Assert.assertEquals(newtCanvasAWT1.getNativeWindow(),glWindow1.getParent());
        Assert.assertEquals(JAWTUtil.isOffscreenLayerSupported() && offscreenLayer, 
                            newtCanvasAWT1.isOffscreenLayerSurfaceEnabled());

        GLAnimatorControl animator1 = new Animator(glWindow1);
        animator1.start();

        Thread.sleep(durationPerTest/2);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.setSize(frameSize1);
                frame1.validate();
            }});        
        
        Thread.sleep(durationPerTest/2);
        
        end(animator1, frame1, glWindow1);
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        Window window = glWindow.getDelegatedWindow();
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        String tstname = TestParentingOffscreenLayer02NewtCanvasAWT.class.getName();
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
