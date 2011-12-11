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

import javax.media.opengl.*;

import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;
import java.io.IOException;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import javax.media.nativewindow.AbstractGraphicsDevice;

public class TestGLWindows00NEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;
    static long durationPerTest = 100; // ms

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

    static GLWindow createWindow(Screen screen, GLCapabilitiesImmutable caps)
        throws InterruptedException
    {
        Assert.assertNotNull(caps);
        //
        // Create native windowing resources .. X11/Win/OSX
        // 
        GLWindow glWindow;
        if(null!=screen) {
            glWindow = GLWindow.create(screen, caps);
            Assert.assertNotNull(glWindow);
        } else {
            glWindow = GLWindow.create(caps);
            Assert.assertNotNull(glWindow);
        }
        glWindow.setUpdateFPSFrames(1, null);        

        GLEventListener demo = new GearsES2();
        glWindow.addGLEventListener(demo);

        glWindow.setSize(512, 512);
        glWindow.setVisible(true);
        Assert.assertEquals(true,glWindow.isVisible());
        Assert.assertEquals(true,glWindow.isNativeValid());

        return glWindow;
    }

    static void destroyWindow(GLWindow glWindow) {
        if(null!=glWindow) {
            glWindow.destroy();
            Assert.assertEquals(false,glWindow.isNativeValid());
        }
    }

    @Test
    public void testWindow00() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        GLWindow window1 = createWindow(null, caps); // local
        Assert.assertEquals(true,window1.isNativeValid());
        Assert.assertEquals(true,window1.isVisible());
        AbstractGraphicsDevice device1 = window1.getScreen().getDisplay().getGraphicsDevice();

        System.err.println("GLProfiles window1: "+device1.getConnection()+": "+GLProfile.glAvailabilityToString(device1));

        for(int state=0; state*100<durationPerTest; state++) {
            Thread.sleep(100);
        }

        destroyWindow(window1);
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
            }
        }
        System.out.println("durationPerTest: "+durationPerTest);
        String tstname = TestGLWindows00NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
