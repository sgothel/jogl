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
import java.awt.Button;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;

import javax.media.opengl.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

public class TestParenting03AWT extends UITestCase {
    static Dimension glSize, fSize;
    static long durationPerTest = 1100;
    static long waitAdd2nd = 500;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        glSize = new Dimension(400,200);
        fSize = new Dimension(3*400,2*200);
        glCaps = new GLCapabilities(null);
    }

    @Test
    public void testWindowParenting1AWTOneNewtChilds01() throws InterruptedException, InvocationTargetException {
        testWindowParenting1AWT(false);
    }

    @Test
    public void testWindowParenting1AWTTwoNewtChilds01() throws InterruptedException, InvocationTargetException {
        testWindowParenting1AWT(true);
    }
    
    public void testWindowParenting1AWT(boolean use2nd) throws InterruptedException, InvocationTargetException {
        final Frame frame1 = new Frame("AWT Parent Frame");
        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setUpdateFPSFrames(1, null);
        final NewtCanvasAWT newtCanvasAWT1 = new NewtCanvasAWT(glWindow1);
        newtCanvasAWT1.setPreferredSize(glSize);

        GLEventListener demo1 = new GearsES2(1);
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        glWindow1.addKeyListener(new NewtAWTReparentingKeyAdapter(frame1, newtCanvasAWT1, glWindow1));
        GLAnimatorControl animator1 = new Animator(glWindow1);
        animator1.start();

        GLWindow glWindow2 = null;
        NewtCanvasAWT newtCanvasAWT2 = null;
        GLAnimatorControl animator2 = null;
        if(use2nd) {
            glWindow2 = GLWindow.create(glCaps);
            glWindow2.setUpdateFPSFrames(1, null);
            newtCanvasAWT2 = new NewtCanvasAWT(glWindow2);
            newtCanvasAWT2.setPreferredSize(glSize);
    
            GLEventListener demo2 = new GearsES2(1);
            setDemoFields(demo2, glWindow2, false);
            glWindow2.addGLEventListener(demo2);
            glWindow2.addKeyListener(new NewtAWTReparentingKeyAdapter(frame1, newtCanvasAWT2, glWindow2));
            animator2 = new Animator(glWindow2);
            animator2.start();
        }

        final Container cont1 = new Container();
        cont1.setLayout(new BorderLayout());
        cont1.add(new Button("NORTH"), BorderLayout.NORTH);
        cont1.add(new Button("SOUTH"), BorderLayout.SOUTH);
        cont1.add(new Button("EAST"), BorderLayout.EAST);
        cont1.add(new Button("WEST"), BorderLayout.WEST);
        cont1.add(newtCanvasAWT1, BorderLayout.CENTER);
        System.err.println("******* Cont1 setVisible");
        cont1.setVisible(true);

        final Container cont2 = new Container();
        cont2.setLayout(new BorderLayout());
        if(use2nd) {
            cont2.add(new Button("north"), BorderLayout.NORTH);
            cont2.add(new Button("sourth"), BorderLayout.SOUTH);
            cont2.add(new Button("east"), BorderLayout.EAST);
            cont2.add(new Button("west"), BorderLayout.WEST);
            cont2.add(newtCanvasAWT2, BorderLayout.CENTER);
        }
        System.err.println("******* Cont2 setVisible");
        cont2.setVisible(true);

        frame1.setLayout(new BorderLayout());
        frame1.add(new Button("NORTH"), BorderLayout.NORTH);
        frame1.add(new Button("CENTER"), BorderLayout.CENTER);
        frame1.add(new Button("SOUTH"), BorderLayout.SOUTH);
        frame1.add(cont1, BorderLayout.EAST);
        frame1.setLocation(0, 0);
        frame1.setSize(fSize);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                System.err.println("******* Frame setVisible");
                frame1.validate();                
                frame1.setVisible(true);
            }});

        Assert.assertEquals(newtCanvasAWT1.getNativeWindow(),glWindow1.getParent());
        
        Assert.assertEquals(true, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertNotNull(animator1.getThread());

        if(use2nd) {
            Assert.assertEquals(true, animator2.isAnimating());
            Assert.assertEquals(false, animator2.isPaused());
            Assert.assertNotNull(animator2.getThread());

            Thread.sleep(waitAdd2nd);
    
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame1.add(cont2, BorderLayout.WEST);
                    frame1.validate();
                }});
            Assert.assertEquals(newtCanvasAWT2.getNativeWindow(),glWindow2.getParent());
        }


        Thread.sleep(durationPerTest);

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertEquals(null, animator1.getThread());

        if(use2nd) {
            animator2.stop();
            Assert.assertEquals(false, animator2.isAnimating());
            Assert.assertEquals(false, animator2.isPaused());
            Assert.assertEquals(null, animator2.getThread());
        }

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.dispose();
            } } );
        glWindow1.destroy();
        if(use2nd) {
            glWindow2.destroy();
        }
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
