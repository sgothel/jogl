/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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


import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.TextureDraw01Accessor;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureDraw01ES2Listener;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.TextureDraw01GL2Listener;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPNGTextureFromFileNEWT extends UITestCase {
    static boolean showFPS = false;
    static long duration = 100; // ms
    InputStream grayTextureStream;
    InputStream testTextureStreamN;
    InputStream testTextureStreamI;
    InputStream testTextureStreamIG;
    InputStream testTextureStreamPRGB;
    InputStream testTextureStreamPRGBA;
    InputStream testTextureStreamNRGBA;
    InputStream testTextureStreamIRGBA;

    @Before
    public void initTest() throws IOException {
        grayTextureStream = TestPNGTextureFromFileNEWT.class.getResourceAsStream( "grayscale_texture.png" );
        Assert.assertNotNull(grayTextureStream);
        {
            URLConnection testTextureUrlConn = IOUtil.getResource(this.getClass(), "test-ntscN01-160x90.png");
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamN = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamN);
        }
        {
            URLConnection testTextureUrlConn = IOUtil.getResource(this.getClass(), "test-ntscI01-160x90.png");
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamI = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamI);
        }
        {
            URLConnection testTextureUrlConn = IOUtil.getResource(this.getClass(), "test-ntscIG01-160x90.png");
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamIG = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamIG);
        }
        {
            URLConnection testTextureUrlConn = IOUtil.getResource(this.getClass(), "test-ntscP301-160x90.png");
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamPRGB = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamPRGB);
        }
        {
            URLConnection testTextureUrlConn = IOUtil.getResource(this.getClass(), "test-ntscP401-160x90.png");
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamPRGBA = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamPRGBA);
        }
        {
            URLConnection testTextureUrlConn = IOUtil.getResource(this.getClass(), "test-ntscN401-160x90.png");
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamNRGBA = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamNRGBA);
        }
        {
            URLConnection testTextureUrlConn = IOUtil.getResource(this.getClass(), "test-ntscI401-160x90.png");
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStreamIRGBA = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStreamIRGBA);
        }
    }

    @After
    public void cleanupTest() {
        grayTextureStream = null;
        testTextureStreamN = null;
        testTextureStreamI = null;
        testTextureStreamIG = null;
        testTextureStreamPRGB = null;
        testTextureStreamPRGBA = null;
        testTextureStreamNRGBA = null;
        testTextureStreamIRGBA = null;
    }

    public void testImpl(boolean useFFP, final InputStream istream) throws InterruptedException, IOException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        GLProfile glp;
        if(useFFP && GLProfile.isAvailable(GLProfile.GL2GL3)) {
            glp = GLProfile.getGL2GL3();
        } else if(!useFFP && GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.getGL2ES2();
        } else {
            System.err.println(getSimpleTestName(".")+": GLProfile n/a, useFFP: "+useFFP);
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        final TextureData texData = TextureIO.newTextureData(glp, istream, false /* mipmap */, TextureIO.PNG);
        System.err.println("TextureData: "+texData);
        
        final GLWindow glad = GLWindow.create(caps);
        glad.setTitle("TestPNGTextureGL2FromFileNEWT");
        // Size OpenGL to Video Surface
        glad.setSize(texData.getWidth(), texData.getHeight());
        
        // load texture from file inside current GL context to match the way
        // the bug submitter was doing it
        final GLEventListener gle = useFFP ? new TextureDraw01GL2Listener( texData ) : new TextureDraw01ES2Listener( texData ) ;
        glad.addGLEventListener(gle);
        glad.addGLEventListener(new GLEventListener() {                    
            boolean shot = false;
            
            @Override public void init(GLAutoDrawable drawable) {}
            
            public void display(GLAutoDrawable drawable) {
                // 1 snapshot
                if(null!=((TextureDraw01Accessor)gle).getTexture() && !shot) {
                    shot = true;
                    snapshot(0, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
                }
            }
            
            @Override public void dispose(GLAutoDrawable drawable) { }
            @Override public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
        });

        Animator animator = new Animator(glad);
        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        QuitAdapter quitAdapter = new QuitAdapter();
        glad.addKeyListener(quitAdapter);
        glad.addWindowListener(quitAdapter);
        glad.setVisible(true);
        animator.start();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        
        animator.stop();
        glad.destroy();
    }
    
    @Test
    public void testGrayPNGJLoaderGL2() throws InterruptedException, IOException {
        testImpl(true, grayTextureStream);        
    }    
    @Test
    public void testGrayPNGJLoaderES2() throws InterruptedException, IOException {
        testImpl(false, grayTextureStream);        
    }
    
    @Test
    public void testTestN_PNGJLoaderGL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStreamN);        
    }    
    @Test
    public void testTestN_PNGJLoaderES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamN);        
    }
    
    @Test
    public void testTestI_PNGJLoaderGL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStreamI);        
    }    
    @Test
    public void testTestI_PNGJLoaderES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamI);        
    }
    
    @Test
    public void testTestIG_PNGJLoaderGL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStreamIG);        
    }    
    @Test
    public void testTestIG_PNGJLoaderES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamIG);        
    }
    
    @Test
    public void testTestPRGB_PNGJLoaderGL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStreamPRGB);        
    }    
    @Test
    public void testTestPRGB_PNGJLoaderES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamPRGB);        
    }
    
    @Test
    public void testTestPRGBA_PNGJLoaderGL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStreamPRGBA);        
    }    
    @Test
    public void testTestPRGBA_PNGJLoaderES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamPRGBA);        
    }
    
    @Test
    public void testTestNRGBA_PNGJLoaderGL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStreamNRGBA);        
    }    
    @Test
    public void testTestNRGBA_PNGJLoaderES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamNRGBA);        
    }
    
    @Test
    public void testTestIRGBA_PNGJLoaderGL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStreamIRGBA);        
    }    
    @Test
    public void testTestIRGBA_PNGJLoaderES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStreamIRGBA);        
    }
    
    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestPNGTextureFromFileNEWT.class.getName());        
    }
}
