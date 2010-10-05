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
 
package com.jogamp.test.junit.jogl.offscreen;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.AfterClass;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

import com.jogamp.test.junit.util.UITestCase;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;
import java.io.IOException;

public class TestOffscreen01NEWT extends UITestCase {
    static {
        GLProfile.initSingleton();
    }

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
        Window window = NewtFactory.createWindow(screen, caps);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        GLWindow glWindow = GLWindow.create(window);
        Assert.assertNotNull(glWindow);
        try {
            glWindow.setVisible(true);
        } catch (Throwable t) {
             // stop test and ignore if pixmap cannot be used
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
        GLEventListener demo = new RedSquare();
        WindowUtilNEWT.setDemoFields(demo, window, glWindow, false);
        glWindow.addGLEventListener(demo);

        while ( glWindow.getTotalFrames() < 2) {
            try {
                glWindow.display();
            } catch (Throwable t) {
                 // stop test and ignore if pbuffer cannot be used
                t.printStackTrace();
                Assume.assumeNoException(t);
            }
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
            windows[i] = NewtFactory.createWindow(screen, caps2);
            Assert.assertNotNull(windows[i]);
            windows[i].setSize(width, height);
            glWindows[i] = GLWindow.create(windows[i]);
            Assert.assertNotNull(glWindows[i]);
            try {
                glWindows[i].setVisible(true);
            } catch (Throwable t) {
                 // stop test and ignore if pixmap cannot be used
                t.printStackTrace();
                Assume.assumeNoException(t);
            }
            demos[i] = new RedSquare();
            WindowUtilNEWT.setDemoFields(demos[i], windows[i], glWindows[i], false);
            glWindows[i].addGLEventListener(demos[i]);
        }

        try {
            while ( glWindows[0].getTotalFrames() < 2) {
                for(i=0; i<winnum; i++) {
                    glWindows[i].display();
                }
            }
        } catch (Throwable t) {
             // stop test and ignore if pbuffer cannot be used
            t.printStackTrace();
            Assume.assumeNoException(t);
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
            windows[i] = NewtFactory.createWindow(screens[i], caps2);
            Assert.assertNotNull(windows[i]);
            windows[i].setSize(width, height);
            glWindows[i] = GLWindow.create(windows[i]);
            Assert.assertNotNull(glWindows[i]);
            try {
                glWindows[i].setVisible(true);
            } catch (Throwable t) {
                 // stop test and ignore if pixmap cannot be used
                t.printStackTrace();
                Assume.assumeNoException(t);
            }
            demos[i] = new RedSquare();
            WindowUtilNEWT.setDemoFields(demos[i], windows[i], glWindows[i], false);
            glWindows[i].addGLEventListener(demos[i]);
        }

        try {
            while ( glWindows[0].getTotalFrames() < 2) {
                for(i=0; i<winnum; i++) {
                    glWindows[i].display();
                }
            }
        } catch (Throwable t) {
             // stop test and ignore if pbuffer cannot be used
            t.printStackTrace();
            Assume.assumeNoException(t);
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
        Window window = NewtFactory.createWindow(screen, caps2);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        GLWindow glWindow = GLWindow.create(window);
        Assert.assertNotNull(glWindow);
        try {
            glWindow.setVisible(true);
        } catch (Throwable t) {
             // stop test and ignore if pixmap cannot be used
            t.printStackTrace();
            Assume.assumeNoException(t);
        }

        GLWindow windowOnScreen = null;
        WindowListener wl=null;
        MouseListener ml=null;
        SurfaceUpdatedListener ul=null;

        GLEventListener demo = new RedSquare();
        Assert.assertNotNull(demo);

        try {
            WindowUtilNEWT.run(glWindow, demo, windowOnScreen, wl, ml, ul, 2, true /*snapshot*/, false /*debug*/);
        } catch (Throwable t) {
             // stop test and ignore if pbuffer cannot be used
            t.printStackTrace();
            Assume.assumeNoException(t);
        }

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
        Window window = NewtFactory.createWindow(screen, caps2);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        GLWindow glWindow = GLWindow.create(window);
        Assert.assertNotNull(glWindow);
        try {
            glWindow.setVisible(true);
        } catch (Throwable t) {
             // stop test and ignore if pixmap cannot be used
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
        GLEventListener demo = new RedSquare();
        WindowUtilNEWT.setDemoFields(demo, window, glWindow, false);
        glWindow.addGLEventListener(demo);

        while ( glWindow.getTotalFrames() < 2) {
            try {
                glWindow.display();
            } catch (Throwable t) {
                 // stop test and ignore if pixmap cannot be used
                t.printStackTrace();
                Assume.assumeNoException(t);
            }
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
        Window window = NewtFactory.createWindow(screen, caps2);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        GLWindow glWindow = GLWindow.create(window);
        Assert.assertNotNull(glWindow);
        try {
            glWindow.setVisible(true);
        } catch (Throwable t) {
             // stop test and ignore if pixmap cannot be used
            t.printStackTrace();
            Assume.assumeNoException(t);
        }

        GLWindow windowOnScreen = null;
        WindowListener wl=null;
        MouseListener ml=null;
        SurfaceUpdatedListener ul=null;

        GLEventListener demo = new RedSquare();
        Assert.assertNotNull(demo);

        try {
            WindowUtilNEWT.run(glWindow, demo, windowOnScreen, wl, ml, ul, 2, true /*snapshot*/, false /*debug*/);
        } catch (Throwable t) {
             // stop test and ignore if pixmap cannot be used
            t.printStackTrace();
            Assume.assumeNoException(t);
        }

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
