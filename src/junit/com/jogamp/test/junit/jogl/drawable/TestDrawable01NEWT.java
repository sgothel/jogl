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

package com.jogamp.test.junit.jogl.drawable;


import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import javax.media.opengl.*;

import com.jogamp.newt.*;
import java.io.IOException;

public class TestDrawable01NEWT {
    static GLProfile glp;
    static GLDrawableFactory factory;
    static int width, height;
    GLCapabilities caps;
    Window window;
    GLDrawable drawable;
    GLContext context;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getDefault();
        Assert.assertNotNull(glp);
        factory = GLDrawableFactory.getFactory(glp);
        Assert.assertNotNull(factory);
        width  = 640;
        height = 480;
    }

    @AfterClass
    public static void releaseClass() {
        Assert.assertNotNull(factory);
        factory=null;
    }

    @Before
    public void initTest() {
        caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
    }

    void createWindow(boolean onscreen, boolean pbuffer, boolean undecorated) {
        caps.setOnscreen(onscreen);
        caps.setPBuffer(!onscreen && pbuffer);
        caps.setDoubleBuffered(!onscreen);
        // System.out.println("Requested: "+caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);

        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        window = NewtFactory.createWindow(screen, caps, onscreen && undecorated);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        window.setVisible(true);
        // System.out.println("Created: "+window);

        //
        // Create native OpenGL resources .. XGL/WGL/CGL .. 
        // equivalent to GLAutoDrawable methods: setVisible(true)
        // 
        GLCapabilities glCaps = (GLCapabilities) window.getGraphicsConfiguration().getNativeGraphicsConfiguration().getChosenCapabilities();
        Assert.assertNotNull(glCaps);
        Assert.assertTrue(glCaps.getGreenBits()>5);
        Assert.assertTrue(glCaps.getBlueBits()>5);
        Assert.assertTrue(glCaps.getRedBits()>5);
        Assert.assertTrue(glCaps.isOnscreen()==onscreen);
        Assert.assertTrue(onscreen || !pbuffer || glCaps.isPBuffer()); // pass if onscreen, or !pbuffer req. or have pbuffer
        Assert.assertTrue(glCaps.getDoubleBuffered()==!onscreen);
        Assert.assertTrue(glCaps.getDepthBits()>4);

        drawable = factory.createGLDrawable(window);
        Assert.assertNotNull(drawable);
        // System.out.println("Pre: "+drawable);
        //
        drawable.setRealized(true);
        Assert.assertTrue(width==drawable.getWidth());
        Assert.assertTrue(height==drawable.getHeight());
        // Assert.assertTrue(glCaps==drawable.getChosenGLCapabilities());
        Assert.assertTrue(window==drawable.getNativeWindow());
        // System.out.println("Post: "+drawable);

        context = drawable.createContext(null);
        Assert.assertNotNull(context);
        // System.out.println(context);
        
        int res = context.makeCurrent();
        Assert.assertTrue(GLContext.CONTEXT_CURRENT_NEW==res || GLContext.CONTEXT_CURRENT==res);

        // draw something ..

        drawable.swapBuffers();
        context.release();

        // System.out.println("Final: "+window);
    }

    void destroyWindow() {
        // GLWindow.dispose(..) sequence
        Assert.assertNotNull(context);
        context.destroy();

        Assert.assertNotNull(drawable);
        drawable.setRealized(false);

        // GLWindow.destroy(..) sequence cont..
        Assert.assertNotNull(window);
        window.destroy(true); // incl screen + display

        drawable = null;
        context = null;
        window = null;
    }

    @Test
    public void testOnScreenDecorated() throws InterruptedException {
        createWindow(true, false, false);
        Thread.sleep(1000); // 1000 ms
        destroyWindow();
    }

    @Test
    public void testOnScreenUndecorated() throws InterruptedException {
        createWindow(true, false, true);
        Thread.sleep(1000); // 1000 ms
        destroyWindow();
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestDrawable01NEWT.class.getName();
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
