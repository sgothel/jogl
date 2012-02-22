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
 
package com.jogamp.opengl.test.junit.jogl.util;

import java.io.File;

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
        width  = 256;
        height = 256;
    }

    protected void snapshot(GLAutoDrawable drawable, boolean alpha, boolean flip, String filename) {
        GLReadBufferUtil screenshot = new GLReadBufferUtil(alpha, false);
        if(screenshot.readPixels(drawable.getGL(), drawable, flip)) {
            screenshot.write(new File(filename));
        }                
    }
    
    @Test
    public void testWriteTGAAndPAM() throws InterruptedException {
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Shared Gears NEWT Test");
        glWindow.setSize(width, height);
        glWindow.addGLEventListener(new GearsES2(1));
        glWindow.addGLEventListener(new GLEventListener() {
            public void init(GLAutoDrawable drawable) {}
            public void dispose(GLAutoDrawable drawable) {}
            public void display(GLAutoDrawable drawable) {
                // snapshot(drawable, false, true,  getSimpleTestName(".")+"-rgb_-"+drawable.getGLProfile().getName()+".ppm");
                snapshot(drawable, true,  false, getSimpleTestName(".")+"-rgba-"+drawable.getGLProfile().getName()+".tga");
                snapshot(drawable, true,  true,  getSimpleTestName(".")+"-rgba-"+drawable.getGLProfile().getName()+".pam");
                snapshot(drawable, false, false, getSimpleTestName(".")+"-rgb_-"+drawable.getGLProfile().getName()+".tga");
                snapshot(drawable, false, true,  getSimpleTestName(".")+"-rgb_-"+drawable.getGLProfile().getName()+".pam");
            }
            public void reshape(GLAutoDrawable drawable, int x, int y,
                    int width, int height) { }
        });
        glWindow.setVisible(true);
        Thread.sleep(60);
        glWindow.destroy();        
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestGLReadBufferUtilTextureIOWrite01NEWT.class.getName());
    }
}
