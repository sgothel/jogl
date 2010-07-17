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

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

import java.io.IOException;

import com.jogamp.test.junit.util.*;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;

public class TestParenting01NEWT {
    static {
        GLProfile.initSingleton();
    }

    static int width, height;
    static long durationPerTest = 500;
    static long waitReparent = 0;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
    }

    @Test
    public void testWindowParenting01CreateVisibleDestroy() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeWindowValid());
        Assert.assertNull(glWindow1.getParentNativeWindow());
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        glWindow1.setSize(640, 480);
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        GLWindow glWindow2 = GLWindow.create(glWindow1, glCaps);
        Assert.assertNotNull(glWindow2);
        Assert.assertEquals(false, glWindow2.isVisible());
        Assert.assertEquals(false, glWindow2.isNativeWindowValid());
        Assert.assertEquals(glWindow1,glWindow2.getParentNativeWindow());
        glWindow2.setSize(320, 240);
        GLEventListener demo2 = new Gears();
        setDemoFields(demo2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);

        // visible test
        glWindow1.setVisible(true);
        Assert.assertEquals(true, glWindow1.isVisible());
        Assert.assertEquals(true, glWindow1.isNativeWindowValid());
        Assert.assertEquals(true, glWindow2.isVisible());
        Assert.assertEquals(true, glWindow2.isNativeWindowValid());
        glWindow1.setVisible(false);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(true, glWindow1.isNativeWindowValid());
        Assert.assertEquals(false, glWindow2.isVisible());
        Assert.assertEquals(true, glWindow2.isNativeWindowValid());
        glWindow1.setVisible(true);
        Assert.assertEquals(true, glWindow1.isVisible());
        Assert.assertEquals(true, glWindow1.isNativeWindowValid());
        Assert.assertEquals(true, glWindow2.isVisible());
        Assert.assertEquals(true, glWindow2.isNativeWindowValid());

        Animator animator1 = new Animator(glWindow1);
        animator1.start();
        Animator animator2 = new Animator(glWindow2);
        animator2.start();
        while(animator1.isAnimating() && animator1.getDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        animator2.stop();
        Assert.assertEquals(false, animator2.isAnimating());

        glWindow1.destroy(); // false

        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeWindowValid());
        Assert.assertEquals(false, glWindow1.isDestroyed());

        Assert.assertEquals(false, glWindow2.isVisible());
        Assert.assertEquals(false, glWindow2.isNativeWindowValid());
        Assert.assertEquals(false, glWindow2.isDestroyed());

        glWindow1.destroy(true);
        Assert.assertEquals(true, glWindow1.isDestroyed());
        Assert.assertEquals(true, glWindow2.isDestroyed());

        // test double destroy ..
        glWindow2.destroy(true);
        Assert.assertEquals(true, glWindow2.isDestroyed());
    }

    @Test
    public void testWindowParenting02ReparentTop2Win() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setTitle("testWindowParenting02ReparentTop2Win");
        glWindow1.setSize(640, 480);
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        GLWindow glWindow2 = GLWindow.create(glCaps);
        glWindow2.setSize(320, 240);
        GLEventListener demo2 = new Gears();
        setDemoFields(demo2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);

        glWindow1.setVisible(true);
        glWindow2.setVisible(true);

        Animator animator1 = new Animator(glWindow1);
        animator1.start();
        Animator animator2 = new Animator(glWindow2);
        animator2.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    Assert.assertEquals(true, glWindow2.isVisible());
                    glWindow2.reparentWindow(glWindow1, null);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow2.isNativeWindowValid());
                    Assert.assertEquals(glWindow1,glWindow2.getParentNativeWindow());
                    break;
                case 1:
                    Assert.assertEquals(true, glWindow2.isVisible());
                    glWindow2.reparentWindow(null, null);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow2.isNativeWindowValid());
                    Assert.assertNull(glWindow2.getParentNativeWindow());
                    break;
            }
            state++;
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        animator2.stop();
        Assert.assertEquals(false, animator2.isAnimating());

        glWindow1.destroy(true);
        Assert.assertEquals(true, glWindow1.isDestroyed());
        Assert.assertEquals(false, glWindow2.isDestroyed());
        glWindow2.destroy(true);
        Assert.assertEquals(true, glWindow1.isDestroyed());
        Assert.assertEquals(true, glWindow2.isDestroyed());
    }

    @Test
    public void testWindowParenting03ReparentWin2Top() throws InterruptedException {
        int x = 0;
        int y = 0;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setTitle("testWindowParenting03ReparentWin2Top");
        glWindow1.setSize(640, 480);
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        GLWindow glWindow2 = GLWindow.create(glWindow1, glCaps);
        glWindow2.setSize(320, 240);
        GLEventListener demo2 = new Gears();
        setDemoFields(demo2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);

        glWindow1.setVisible(true);

        Animator animator1 = new Animator(glWindow1);
        animator1.start();
        Animator animator2 = new Animator(glWindow2);
        animator2.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    Assert.assertEquals(true, glWindow2.isVisible());
                    glWindow2.reparentWindow(null, null); 
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow2.isNativeWindowValid());
                    Assert.assertNull(glWindow2.getParentNativeWindow());
                    break;
                case 1:
                    Assert.assertEquals(true, glWindow2.isVisible());
                    glWindow2.reparentWindow(glWindow1, null);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow2.isNativeWindowValid());
                    Assert.assertEquals(glWindow1,glWindow2.getParentNativeWindow());
                    break;
            }
            state++;
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        animator2.stop();
        Assert.assertEquals(false, animator2.isAnimating());

        glWindow1.destroy(true);
        Assert.assertEquals(true, glWindow1.isDestroyed());
        Assert.assertEquals(true, glWindow2.isDestroyed());
        glWindow2.destroy(true);
        Assert.assertEquals(true, glWindow1.isDestroyed());
        Assert.assertEquals(true, glWindow2.isDestroyed());
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
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            } else if(args[i].equals("-wait")) {
                waitReparent = atoi(args[++i]);
            }
        }
        String tstname = TestParenting01NEWT.class.getName();
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
