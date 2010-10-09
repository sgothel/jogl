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
 
package com.jogamp.test.junit.newt.parenting;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.test.junit.util.*;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;

public class TestParenting03AWT extends UITestCase {
    static {
        GLProfile.initSingleton();
    }

    static Dimension size;
    static long durationPerTest = 800;
    static long waitAdd2nd = 500;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        size = new Dimension(400,200);
        glCaps = new GLCapabilities(null);
    }

    @Test
    public void testWindowParenting1AWT2NewtChilds01() throws InterruptedException, InvocationTargetException {
        testWindowParenting1AWT2NewtChilds(true);
    }

    public void testWindowParenting1AWT2NewtChilds(boolean visibleChild2) throws InterruptedException, InvocationTargetException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setUndecorated(true);
        GLEventListener demo1 = new Gears();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        final GLWindow f_glWindow1 = glWindow1;
        glWindow1.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar()=='f') {
                    f_glWindow1.invoke(false, new GLRunnable() {
                        public void run(GLAutoDrawable drawable) {
                            GLWindow win = (GLWindow)drawable;
                            win.setFullscreen(!win.isFullscreen());
                        } });
                }
            }
        });
        GLAnimatorControl animator1 = new Animator(glWindow1);
        animator1.start();

        GLWindow glWindow2 = GLWindow.create(glCaps);
        glWindow2.setUndecorated(true);
        GLEventListener demo2 = new Gears();
        setDemoFields(demo2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);
        final GLWindow f_glWindow2 = glWindow2;
        glWindow2.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar()=='f') {
                    f_glWindow2.invoke(false, new GLRunnable() {
                        public void run(GLAutoDrawable drawable) {
                            GLWindow win = (GLWindow)drawable;
                            win.setFullscreen(!win.isFullscreen());
                        } });
                }
            }
        });
        GLAnimatorControl animator2 = new Animator(glWindow2);
        animator2.start();

        NewtCanvasAWT newtCanvasAWT1 = new NewtCanvasAWT(glWindow1);
        newtCanvasAWT1.setPreferredSize(size);
        Container cont1 = new Container();
        cont1.setLayout(new BorderLayout());
        cont1.add(newtCanvasAWT1, BorderLayout.CENTER);
        cont1.setVisible(true);
        final Container f_cont1 = cont1;

        NewtCanvasAWT newtCanvasAWT2 = new NewtCanvasAWT(glWindow2);
        newtCanvasAWT2.setPreferredSize(size);
        Container cont2 = new Container();
        cont2.setLayout(new BorderLayout());
        cont2.add(newtCanvasAWT2, BorderLayout.CENTER);
        cont2.setVisible(true);
        final Container f_cont2 = cont2;

        Frame frame1 = new Frame("AWT Parent Frame");
        frame1.setLayout(new BorderLayout());
        frame1.add(cont1, BorderLayout.EAST);
        frame1.add(new Label("center"), BorderLayout.CENTER);
        frame1.setLocation(0, 0);
        frame1.setSize((int)size.getWidth()*2, (int)size.getHeight()*2);
        final Frame f_frame1 = frame1;
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f_frame1.pack();
                f_frame1.setVisible(true);
            }});

        Assert.assertEquals(newtCanvasAWT1.getNativeWindow(),glWindow1.getParent());
        Assert.assertEquals(newtCanvasAWT2.getNativeWindow(),glWindow2.getParent());

        Assert.assertEquals(true, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertNotNull(animator1.getThread());

        Assert.assertEquals(true, animator2.isAnimating());
        Assert.assertEquals(false, animator2.isPaused());
        Assert.assertNotNull(animator2.getThread());

        Thread.sleep(waitAdd2nd);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f_frame1.add(f_cont2, BorderLayout.WEST);
                f_frame1.pack();
            }});

        Thread.sleep(durationPerTest);

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertEquals(null, animator1.getThread());

        animator2.stop();
        Assert.assertEquals(false, animator2.isAnimating());
        Assert.assertEquals(false, animator2.isPaused());
        Assert.assertEquals(null, animator2.getThread());

        frame1.dispose();
        glWindow1.destroy(true);
        glWindow2.destroy(true);
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        Window window = glWindow.getWindow();
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
            } else if(args[i].equals("-wait")) {
                waitAdd2nd = atoi(args[++i]);
            }
        }
        String tstname = TestParenting03AWT.class.getName();
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
