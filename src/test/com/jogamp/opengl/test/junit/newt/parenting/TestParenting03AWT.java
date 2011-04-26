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
 
package com.jogamp.opengl.test.junit.newt.parenting;

import java.lang.reflect.*;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;

import javax.media.opengl.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.Gears;

public class TestParenting03AWT extends UITestCase {
    static Dimension size;
    static long durationPerTest = 400;
    static long waitAdd2nd = 200;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
        size = new Dimension(400,200);
        glCaps = new GLCapabilities(null);
        glCaps.setAlphaBits(8);
        glCaps.setBackgroundOpaque(false);
    }

    @Test
    public void testWindowParenting1AWTOneNewtChild01() throws InterruptedException, InvocationTargetException {
        testWindowParenting1AWTOneNewtChild();
    }

    public void testWindowParenting1AWTOneNewtChild() throws InterruptedException, InvocationTargetException {
        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setUpdateFPSFrames(1, null);
        glWindow1.setUndecorated(true);
        NewtCanvasAWT newtCanvasAWT1 = new NewtCanvasAWT(glWindow1);
        newtCanvasAWT1.setPreferredSize(size);

        GLEventListener demo1 = new Gears(1);
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        final NewtCanvasAWT f_newtCanvasAWT1 = newtCanvasAWT1;
        final GLWindow f_glWindow1 = glWindow1;
        glWindow1.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar()=='d') {
                    f_glWindow1.setUndecorated(!f_glWindow1.isUndecorated());
                } else if(e.getKeyChar()=='f') {
                    f_glWindow1.setFullscreen(!f_glWindow1.isFullscreen());
                } else if(e.getKeyChar()=='r') {
                    if(f_glWindow1.getParent()==null) {
                        System.err.println("XXX glWin1 to home");
                        f_glWindow1.reparentWindow(f_newtCanvasAWT1.getNativeWindow());
                    } else {
                        System.err.println("XXX glWin1 to TOP");
                        f_glWindow1.reparentWindow(null);
                    }
                }
            }
        });
        GLAnimatorControl animator1 = new Animator(glWindow1);
        animator1.start();

        Container cont1 = new Container();
        cont1.setLayout(new BorderLayout());
        cont1.add(newtCanvasAWT1, BorderLayout.CENTER);
        cont1.setVisible(true);

        Frame frame1 = new Frame("AWT Parent Frame");
        frame1.setLayout(new BorderLayout());
        frame1.add(cont1, BorderLayout.EAST);
        frame1.add(new Label("center"), BorderLayout.CENTER);
        frame1.setLocation(0, 0);
        frame1.setSize((int)size.getWidth(), (int)size.getHeight());
        final Frame f_frame1 = frame1;
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f_frame1.pack();
                f_frame1.setVisible(true);
            }});

        Assert.assertEquals(newtCanvasAWT1.getNativeWindow(),glWindow1.getParent());
        Assert.assertEquals(true, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertNotNull(animator1.getThread());

        Thread.sleep(durationPerTest);

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertEquals(null, animator1.getThread());

        frame1.dispose();
        glWindow1.destroy();
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
