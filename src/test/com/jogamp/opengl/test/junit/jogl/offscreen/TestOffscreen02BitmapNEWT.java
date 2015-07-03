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
import com.jogamp.newt.opengl.GLWindow;
import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.*;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es1.RedSquareES1;
import java.io.IOException;

/**
 * Using ES1 GL demo, since pixmap might not be hw accelerated,
 * hence it is possible to not have GLSL.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestOffscreen02BitmapNEWT extends UITestCase {
    static final int width = 640, height = 480;

    @Test
    public void test11OffscreenWindowPixmap() {
        // we need to stay w/ generic profile GL2ES1
        // since software rasterizer might be required (pixmap/bitmap)
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES1);
        Assert.assertNotNull(glp);
        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);

        final GLCapabilities caps2 = WindowUtilNEWT.fixCaps(caps, false, false, false);

        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        final Window window = NewtFactory.createWindow(screen, caps2);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        final GLWindow glWindow = GLWindow.create(window);
        Assert.assertNotNull(glWindow);
        glWindow.setVisible(true);

        final GLEventListener demo = new RedSquareES1();
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
        // we need to stay w/ generic profile GL2ES1
        // since software rasterizer might be required (pixmap/bitmap)
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES1);
        Assert.assertNotNull(glp);
        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);

        final GLCapabilities caps2 = WindowUtilNEWT.fixCaps(caps, false, false, false);

        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        final Window window = NewtFactory.createWindow(screen, caps2);
        Assert.assertNotNull(window);
        window.setSize(width, height);
        final GLWindow glWindow = GLWindow.create(window);
        Assert.assertNotNull(glWindow);
        glWindow.setVisible(true);

        WindowUtilNEWT.run(getSimpleTestName("."), glWindow, new RedSquareES1(), null, null, null, null,
                           2 /* frames */, true /*snapshot*/, false /*debug*/);

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
    public static void main(final String args[]) throws IOException {
        final String tstname = TestOffscreen02BitmapNEWT.class.getName();
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
