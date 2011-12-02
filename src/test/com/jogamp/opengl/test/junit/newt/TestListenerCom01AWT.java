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
 
package com.jogamp.opengl.test.junit.newt;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Frame;

import javax.media.opengl.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

public class TestListenerCom01AWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 500;
    static boolean verbose = false;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
    }

    @Test
    public void testListenerStringPassingAndOrder() throws InterruptedException, InvocationTargetException {
        // setup NEWT GLWindow ..
        GLWindow glWindow = GLWindow.create(new GLCapabilities(null));
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("NEWT - CHILD");

        System.out.println("durationPerTest "+durationPerTest);

        GLEventListener demo = new GearsES2();
        setDemoFields(demo, glWindow, false);
        glWindow.addGLEventListener(demo);

        WindowEventCom1 wl1 = new WindowEventCom1();
        WindowEventCom2 wl2 = new WindowEventCom2();
        WindowEventCom3 wl3 = new WindowEventCom3();

        // TraceWindowAdapter wlT = new TraceWindowAdapter();
        // glWindow.addWindowListener(0, wlT);
        // Assert.assertEquals(wlT, glWindow.getWindowListener(0));

        glWindow.addWindowListener(0, wl3);
        glWindow.addWindowListener(0, wl2);
        glWindow.addWindowListener(0, wl1);

        Assert.assertEquals(wl1, glWindow.getWindowListener(0));
        Assert.assertEquals(wl2, glWindow.getWindowListener(1));
        Assert.assertEquals(wl3, glWindow.getWindowListener(2));

        // attach NEWT GLWindow to AWT Canvas
        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow);
        final Frame frame = new Frame("AWT Parent Frame");
        frame.add(newtCanvasAWT);
        frame.setSize(width, height);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(true);
            }});

        Animator animator1 = new Animator(glWindow);
        animator1.setUpdateFPSFrames(1, null);        
        animator1.start();
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
            width+=10; height+=10;
            frame.setSize(width, height);
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            }});
        glWindow.destroy();
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
        verbose = true;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        String tstname = TestListenerCom01AWT.class.getName();
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
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }

}
