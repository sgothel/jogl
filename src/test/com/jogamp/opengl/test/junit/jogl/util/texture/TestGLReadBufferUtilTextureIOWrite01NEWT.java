/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.util.texture;

import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.offscreen.WindowUtilNEWT;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLReadBufferUtilTextureIOWrite01NEWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getDefault();
        Assert.assertNotNull(glp);
        caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        caps.setAlphaBits(1); // req. alpha channel
        width  = 256;
        height = 256;
    }

    @Test
    public void testOnscreenWritePNG_TGA_PAM() throws InterruptedException {
        final GLReadBufferUtil screenshotRGB = new GLReadBufferUtil(false, false);
        final GLReadBufferUtil screenshotRGBA = new GLReadBufferUtil(true, false);
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Shared Gears NEWT Test");
        glWindow.setSize(width, height);
        glWindow.addGLEventListener(new GearsES2(1));
        glWindow.addGLEventListener(new GLEventListener() {
            int f = 0;
            public void init(final GLAutoDrawable drawable) {}
            public void dispose(final GLAutoDrawable drawable) {}
            public void display(final GLAutoDrawable drawable) {
                snapshot(f++, null, drawable.getGL(), screenshotRGBA, TextureIO.PNG, null);
                snapshot(f++, null, drawable.getGL(),  screenshotRGB, TextureIO.PNG, null);
                snapshot(f++, null, drawable.getGL(), screenshotRGBA, TextureIO.TGA, null);
                snapshot(f++, null, drawable.getGL(),  screenshotRGB, TextureIO.TGA, null);
                snapshot(f++, null, drawable.getGL(), screenshotRGBA, TextureIO.PAM, null);
                snapshot(f++, null, drawable.getGL(),  screenshotRGB, TextureIO.PAM, null);
            }
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        });
        glWindow.setVisible(true);
        Thread.sleep(60);
        glWindow.destroy();
    }

    @Test
    public void testOffscreenWritePNG() throws InterruptedException {
        final GLReadBufferUtil screenshotRGB = new GLReadBufferUtil(false, false);
        final GLReadBufferUtil screenshotRGBA = new GLReadBufferUtil(true, false);
        final GLCapabilities caps2 = WindowUtilNEWT.fixCaps(caps, false, true, false);
        final GLWindow glWindow = GLWindow.create(caps2);
        Assert.assertNotNull(glWindow);
        glWindow.setSize(width, height);
        glWindow.addGLEventListener(new GearsES2(1));
        glWindow.addGLEventListener(new GLEventListener() {
            int f = 0;
            public void init(final GLAutoDrawable drawable) {}
            public void dispose(final GLAutoDrawable drawable) {}
            public void display(final GLAutoDrawable drawable) {
                snapshot(f, null, drawable.getGL(), screenshotRGBA, TextureIO.PNG, null);
                snapshot(f, null, drawable.getGL(),  screenshotRGB, TextureIO.PNG, null);
                f++;
            }
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        });
        glWindow.setVisible(true);
        Thread.sleep(60);
        glWindow.destroy();
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestGLReadBufferUtilTextureIOWrite01NEWT.class.getName());
    }
}
