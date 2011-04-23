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
 
package com.jogamp.opengl.test.junit.jogl.offscreen;


import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.opengl.GLWindow;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es1.RedSquare;
import java.io.IOException;

public class TestOffscreen02BitmapNEWT extends UITestCase {
    static GLProfile glpDefault;
    static GLDrawableFactory glDrawableFactory;
    static int width, height;
    GLCapabilities capsDefault;

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
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
        glWindow.setVisible(true);

        GLEventListener demo = new RedSquare();
        WindowUtilNEWT.setDemoFields(demo, window, glWindow, false);
        glWindow.addGLEventListener(demo);

        while ( glWindow.getTotalFPSFrames() < 2) {
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

        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        Window window = NewtFactory.createWindow(screen, caps2);
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
        String tstname = TestOffscreen02BitmapNEWT.class.getName();
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
