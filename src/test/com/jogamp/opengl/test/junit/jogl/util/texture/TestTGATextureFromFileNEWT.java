/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.TextureDraw01Accessor;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureDraw01ES2Listener;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.TextureDraw01GL2Listener;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTGATextureFromFileNEWT extends UITestCase {
    static boolean showFPS = false;
    static long duration = 100; // ms
    
    InputStream testTextureStream01U32;
    InputStream testTextureStream02RLE32;
    
    @Before
    public void initTest() throws IOException {
        {
            URLConnection testTextureUrlConn = IOUtil.getResource(this.getClass(), "test-u32.tga");
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream01U32 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream01U32);
        }
        {
            URLConnection testTextureUrlConn = IOUtil.getResource(this.getClass(), "bug744-rle32.tga");
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream02RLE32 = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream02RLE32);
        }
    }
    
    public void testImpl(boolean useFFP, final InputStream istream) throws InterruptedException, IOException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        GLProfile glp;
        if(useFFP && GLProfile.isAvailable(GLProfile.GL2)) {
            glp = GLProfile.getMaxFixedFunc(true);
        } else if(!useFFP && GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.getGL2ES2();
        } else {
            System.err.println(getSimpleTestName(".")+": GLProfile n/a, useFFP: "+useFFP);
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(1);
        
        final TextureData texData = TextureIO.newTextureData(glp, istream, false /* mipmap */, TextureIO.TGA);
        System.err.println("TextureData: "+texData);        
        
        final GLWindow glad = GLWindow.create(caps);
        glad.setTitle("TestTGATextureGL2FromFileNEWT");
        // Size OpenGL to Video Surface
        glad.setSize(texData.getWidth(), texData.getHeight());
        
        // load texture from file inside current GL context to match the way
        // the bug submitter was doing it
        final GLEventListener gle = useFFP ? new TextureDraw01GL2Listener( texData ) : new TextureDraw01ES2Listener( texData, 0 ) ;
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
    public void test01U32__GL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStream01U32);        
    }
    
    @Test
    public void test02RLE32__GL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStream02RLE32);        
    }
    
    @After
    public void cleanupTest() {
        testTextureStream01U32 = null;
        testTextureStream02RLE32 = null;
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestTGATextureFromFileNEWT.class.getName());        
    }
}
