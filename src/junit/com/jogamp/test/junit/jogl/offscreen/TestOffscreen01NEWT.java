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

package com.jogamp.test.junit.jogl.offscreen;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.After;
import org.junit.Test;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;
import java.io.IOException;

public class TestOffscreen01NEWT {
    static GLProfile glpDefault;
    static GLDrawableFactory glDrawableFactory;
    static int width, height;
    GLCapabilities capsDefault;

    @BeforeClass
    public static void initClass() {
        glpDefault = GLProfile.getDefault();
        Assert.assertNotNull(glpDefault);
        glDrawableFactory = GLDrawableFactory.getFactory(glpDefault);
        System.out.println("INFO: PBuffer supported: "+ glDrawableFactory.canCreateGLPbuffer(null));
        width  = 640;
        height = 480;
    }

    @AfterClass
    public static void releaseClass() {
    }

    @Before
    public void init() {
        capsDefault = new GLCapabilities(glpDefault);
        Assert.assertNotNull(capsDefault);
    }

    private void do01OffscreenWindowPBuffer(GLCapabilities caps) {
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        Window window = NewtFactory.createWindow(screen, caps, false /* undecorated */);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        GLWindow glWindow = GLWindow.create(window);
        Assert.assertNotNull(glWindow);
        glWindow.setVisible(true);
        GLEventListener demo = new RedSquare();
        WindowUtilNEWT.setDemoFields(demo, window, glWindow, false);
        glWindow.addGLEventListener(demo);

        while ( glWindow.getTotalFrames() < 2) {
            glWindow.display();
        }

        if(null!=glWindow) {
            glWindow.destroy();
        }
        if(null!=window) {
            window.destroy();
        }
        if(null!=screen) {
            screen.destroy();
        }
        if(null!=display) {
            display.destroy();
        }
    }

    @Test
    public void test01aOffscreenWindowPBuffer() {
        if(!glDrawableFactory.canCreateGLPbuffer(null)) {
            System.out.println("WARNING: PBuffer not supported on this platform - cannot test");
            return;
        }
        GLCapabilities caps2 = WindowUtilNEWT.fixCaps(capsDefault, false, true, false);
        do01OffscreenWindowPBuffer(caps2);
    }

    @Test
    public void test01bOffscreenWindowPBufferStencil() {
        if(!glDrawableFactory.canCreateGLPbuffer(null)) {
            System.out.println("WARNING: PBuffer not supported on this platform - cannot test");
            return;
        }
        GLCapabilities caps2 = WindowUtilNEWT.fixCaps(capsDefault, false, true, false);
        caps2.setStencilBits(8);
        do01OffscreenWindowPBuffer(caps2);
    }

    @Test
    public void test01cOffscreenWindowPBufferStencilAlpha() {
        if(!glDrawableFactory.canCreateGLPbuffer(null)) {
            System.out.println("WARNING: PBuffer not supported on this platform - cannot test");
            return;
        }
        GLCapabilities caps2 = WindowUtilNEWT.fixCaps(capsDefault, false, true, false);
        caps2.setStencilBits(8);
        caps2.setAlphaBits(8);
        do01OffscreenWindowPBuffer(caps2);
    }

    @Test
    public void test01cOffscreenWindowPBuffer555() {
        if(!glDrawableFactory.canCreateGLPbuffer(null)) {
            System.out.println("WARNING: PBuffer not supported on this platform - cannot test");
            return;
        }
        GLCapabilities caps2 = WindowUtilNEWT.fixCaps(capsDefault, false, true, false);
        caps2.setRedBits(5);
        caps2.setGreenBits(5);
        caps2.setBlueBits(5);
        do01OffscreenWindowPBuffer(caps2);
    }

    @Test
    public void test02Offscreen3Windows1DisplayPBuffer() {
        if(!glDrawableFactory.canCreateGLPbuffer(null)) {
            System.out.println("WARNING: PBuffer not supported on this platform - cannot test");
            return;
        }
        GLCapabilities caps2 = WindowUtilNEWT.fixCaps(capsDefault, false, true, false);
        int winnum = 3, i;
        Window windows[] = new Window[winnum];
        GLWindow glWindows[] = new GLWindow[winnum];
        GLEventListener demos[] = new GLEventListener[winnum];

        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        for(i=0; i<winnum; i++) {
            System.out.println("Create Window "+i);
            windows[i] = NewtFactory.createWindow(screen, caps2, false /* undecorated */);
            Assert.assertNotNull(windows[i]);
            windows[i].setSize(width, height);
            glWindows[i] = GLWindow.create(windows[i]);
            Assert.assertNotNull(glWindows[i]);
            glWindows[i].setVisible(true);
            demos[i] = new RedSquare();
            WindowUtilNEWT.setDemoFields(demos[i], windows[i], glWindows[i], false);
            glWindows[i].addGLEventListener(demos[i]);
        }

        while ( glWindows[0].getTotalFrames() < 2) {
            for(i=0; i<winnum; i++) {
                glWindows[i].display();
            }
        }

        for(i=0; i<winnum; i++) {
            if(null!=glWindows[i]) {
                glWindows[i].destroy();
            }
            if(null!=windows[i]) {
                windows[i].destroy();
            }
        }
        if(null!=screen) {
            screen.destroy();
        }
        if(null!=display) {
            display.destroy();
        }
    }

    @Test
    public void test03Offscreen3Windows3DisplaysPBuffer() {
        if(!glDrawableFactory.canCreateGLPbuffer(null)) {
            System.out.println("WARNING: PBuffer not supported on this platform - cannot test");
            return;
        }
        GLCapabilities caps2 = WindowUtilNEWT.fixCaps(capsDefault, false, true, false);
        int winnum = 3, i;
        Display displays[] = new Display[winnum];
        Screen screens[] = new Screen[winnum];
        Window windows[] = new Window[winnum];
        GLWindow glWindows[] = new GLWindow[winnum];
        GLEventListener demos[] = new GLEventListener[winnum];

        for(i=0; i<winnum; i++) {
            System.out.println("Create Window "+i);
            displays[i] = NewtFactory.createDisplay(null); // local display
            Assert.assertNotNull(displays[i]);
            screens[i]  = NewtFactory.createScreen(displays[i], 0); // screen 0
            Assert.assertNotNull(screens[i]);
            windows[i] = NewtFactory.createWindow(screens[i], caps2, false /* undecorated */);
            Assert.assertNotNull(windows[i]);
            windows[i].setSize(width, height);
            glWindows[i] = GLWindow.create(windows[i]);
            Assert.assertNotNull(glWindows[i]);
            glWindows[i].setVisible(true);
            demos[i] = new RedSquare();
            WindowUtilNEWT.setDemoFields(demos[i], windows[i], glWindows[i], false);
            glWindows[i].addGLEventListener(demos[i]);
        }

        while ( glWindows[0].getTotalFrames() < 2) {
            for(i=0; i<winnum; i++) {
                glWindows[i].display();
            }
        }

        for(i=0; i<winnum; i++) {
            if(null!=glWindows[i]) {
                glWindows[i].destroy();
            }
            if(null!=windows[i]) {
                windows[i].destroy();
            }
            if(null!=screens[i]) {
                screens[i].destroy();
            }
            if(null!=displays[i]) {
                displays[i].destroy();
            }
        }
    }

    @Test
    public void test04OffscreenSnapshotWithDemoPBuffer() {
        if(!glDrawableFactory.canCreateGLPbuffer(null)) {
            System.out.println("WARNING: PBuffer not supported on this platform - cannot test");
            return;
        }
        GLCapabilities caps2 = WindowUtilNEWT.fixCaps(capsDefault, false, true, false);

        System.out.println("Create Window 1");
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        Window window = NewtFactory.createWindow(screen, caps2, false /* undecorated */);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        GLWindow glWindow = GLWindow.create(window);
        Assert.assertNotNull(glWindow);
        glWindow.setVisible(true);

        GLWindow windowOnScreen = null;
        WindowListener wl=null;
        MouseListener ml=null;
        SurfaceUpdatedListener ul=null;

        GLEventListener demo = new RedSquare();
        Assert.assertNotNull(demo);

        WindowUtilNEWT.run(glWindow, demo, windowOnScreen, wl, ml, ul, 2, true /*snapshot*/, false /*debug*/);

        if(null!=windowOnScreen) {
            windowOnScreen.destroy();
        }
        if(null!=glWindow) {
            glWindow.destroy();
        }
        if(null!=window) {
            window.destroy();
        }
        if(null!=screen) {
            screen.destroy();
        }
        if(null!=display) {
            display.destroy();
        }
    }

    @Test
    public void test11OffscreenWindowPixmap() {
        // Offscreen doesn't work on >= GL3 (ATI)
        GLProfile glp = GLProfile.get(GLProfile.GL2);
        Assert.assertNotNull(glp);
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);

        GLCapabilities caps2 = WindowUtilNEWT.fixCaps(caps, false, false, false);

        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        Window window = NewtFactory.createWindow(screen, caps2, false /* undecorated */);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        GLWindow glWindow = GLWindow.create(window);
        Assert.assertNotNull(glWindow);
        glWindow.setVisible(true);
        GLEventListener demo = new RedSquare();
        WindowUtilNEWT.setDemoFields(demo, window, glWindow, false);
        glWindow.addGLEventListener(demo);

        while ( glWindow.getTotalFrames() < 2) {
            glWindow.display();
        }

        if(null!=glWindow) {
            glWindow.destroy();
        }
        if(null!=window) {
            window.destroy();
        }
        if(null!=screen) {
            screen.destroy();
        }
        if(null!=display) {
            display.destroy();
        }
    }

    @Test
    public void test14OffscreenSnapshotWithDemoPixmap() {
        // Offscreen doesn't work on >= GL3 (ATI)
        GLProfile glp = GLProfile.get(GLProfile.GL2);
        Assert.assertNotNull(glp);
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);

        GLCapabilities caps2 = WindowUtilNEWT.fixCaps(caps, false, false, false);

        System.out.println("Create Window 1");
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        Window window = NewtFactory.createWindow(screen, caps2, false /* undecorated */);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        GLWindow glWindow = GLWindow.create(window);
        Assert.assertNotNull(glWindow);
        glWindow.setVisible(true);

        GLWindow windowOnScreen = null;
        WindowListener wl=null;
        MouseListener ml=null;
        SurfaceUpdatedListener ul=null;

        GLEventListener demo = new RedSquare();
        Assert.assertNotNull(demo);

        WindowUtilNEWT.run(glWindow, demo, windowOnScreen, wl, ml, ul, 2, true /*snapshot*/, false /*debug*/);

        if(null!=windowOnScreen) {
            windowOnScreen.destroy();
        }
        if(null!=glWindow) {
            glWindow.destroy();
        }
        if(null!=window) {
            window.destroy();
        }
        if(null!=screen) {
            screen.destroy();
        }
        if(null!=display) {
            display.destroy();
        }
    }
    public static void main(String args[]) throws IOException {
        String tstname = TestOffscreen01NEWT.class.getName();
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
