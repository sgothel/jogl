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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.jogamp.newt.opengl.GLWindow;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import com.jogamp.opengl.util.GLReadBufferUtil;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGLReadBufferUtilTextureIOWrite02NEWT extends UITestCase {
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

    protected void snapshot(GLAutoDrawable drawable, GLReadBufferUtil screenshot, String filename) {
        if(screenshot.readPixels(drawable.getGL(), drawable, false)) {
            screenshot.write(new File(filename));
        }                
    }
    
    @Test
    public void testWriteTGAWithResize() throws InterruptedException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Shared Gears NEWT Test");
        glWindow.setSize(width, height);
        glWindow.addGLEventListener(new GearsES2(1));
        glWindow.addGLEventListener(new GLEventListener() {
            int i=0;
            public void init(GLAutoDrawable drawable) {}
            public void dispose(GLAutoDrawable drawable) {}
            public void display(GLAutoDrawable drawable) {
                final StringWriter filename = new StringWriter();
                {
                    final PrintWriter pw = new PrintWriter(filename);
                    final String pfmt = drawable.getChosenGLCapabilities().getAlphaBits() > 0 ? "rgba" : "rgb_";
                    pw.printf("%s-F_rgba-I_%s-%s-%03dx%03d-n%03d.tga", 
                            getSimpleTestName("."), pfmt, drawable.getGLProfile().getName(), 
                            drawable.getWidth(), drawable.getHeight(), i++);
                }
                if(screenshot.readPixels(drawable.getGL(), drawable, false)) {
                    screenshot.write(new File(filename.toString()));
                }                
            }
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
        });
        glWindow.setVisible(true);
        Thread.sleep(60);
        glWindow.setSize(300, 300);
        Thread.sleep(60);
        glWindow.setSize(400, 400);
        Thread.sleep(60);
        glWindow.destroy();
    }

    @Test
    public void testWritePNGWithResize() throws InterruptedException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Shared Gears NEWT Test");
        glWindow.setSize(width, height);
        glWindow.addGLEventListener(new GearsES2(1));
        glWindow.addGLEventListener(new GLEventListener() {
            int i=0;
            public void init(GLAutoDrawable drawable) {}
            public void dispose(GLAutoDrawable drawable) {}
            public void display(GLAutoDrawable drawable) {
                final StringWriter filename = new StringWriter();
                {
                    final PrintWriter pw = new PrintWriter(filename);
                    final String pfmt = drawable.getChosenGLCapabilities().getAlphaBits() > 0 ? "rgba" : "rgb_";
                    pw.printf("%s-F_rgba-I_%s-%s-%03dx%03d-n%03d.png", 
                            getSimpleTestName("."), pfmt, drawable.getGLProfile().getName(), 
                            drawable.getWidth(), drawable.getHeight(), i++);
                }
                if(screenshot.readPixels(drawable.getGL(), drawable, false)) {
                    screenshot.write(new File(filename.toString()));
                }                
            }
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
        });
        glWindow.setVisible(true);
        Thread.sleep(60);
        glWindow.setSize(300, 300);
        Thread.sleep(60);
        glWindow.setSize(400, 400);
        Thread.sleep(60);
        glWindow.destroy();
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestGLReadBufferUtilTextureIOWrite02NEWT.class.getName());
    }
}
