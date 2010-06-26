/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name Sven Gothel or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.test.junit.newt;

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
import java.awt.Frame;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.test.junit.util.*;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;

public class TestListenerCom01AWT {
    static {
        GLProfile.initSingleton();
    }

    static int width, height;
    static long durationPerTest = 500;
    static boolean verbose = false;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
    }

    @Test
    public void testListenerStringPassingAndOrder() throws InterruptedException {
        // setup NEWT GLWindow ..
        GLWindow glWindow = GLWindow.create(new GLCapabilities(null));
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("NEWT - CHILD");

        System.out.println("durationPerTest "+durationPerTest);

        GLEventListener demo = new Gears();
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
        Frame frame = new Frame("AWT Parent Frame");
        frame.add(newtCanvasAWT);
        frame.setSize(width, height);
        frame.setVisible(true);

        Animator animator1 = new Animator(glWindow);
        animator1.start();
        while(animator1.isAnimating() && animator1.getDuration()<durationPerTest) {
            Thread.sleep(100);
            width+=10; height+=10;
            frame.setSize(width, height);
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        frame.dispose();
        glWindow.destroy(true);
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        Window window = glWindow.getInnerWindow();
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
