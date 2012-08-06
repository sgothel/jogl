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
 
package com.jogamp.opengl.test.junit.jogl.acore;

import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import jogamp.opengl.GLFBODrawableImpl;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.OffscreenAutoDrawable;
import com.jogamp.opengl.test.junit.jogl.demos.es2.FBOMix2DemosES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.MultisampleDemoES2;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

public class TestFBODrawableNEWT extends UITestCase {
    
    static final int widthStep = 800/4;
    static final int heightStep = 600/4;
    volatile int szStep = 2;
    
    @Test
    public void testGL2ES2_Demo1Normal() throws InterruptedException {    
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        testGLFBODrawableImpl(caps, new GearsES2(0));
    }
    
    @Test
    public void testGL2ES2_Demo1MSAA4() throws InterruptedException {    
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        testGLFBODrawableImpl(caps, new GearsES2(0));
    }
    
    @Test
    public void testGL2ES2_Demo2Normal() throws InterruptedException {    
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        testGLFBODrawableImpl(caps, new MultisampleDemoES2(false));
    }
    
    @Test
    public void testGL2ES2_Demo2MSAA4() throws InterruptedException {    
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        testGLFBODrawableImpl(caps, new MultisampleDemoES2(true));
    }
    
    @Test
    public void testGL2ES2_FBODemoNormal() throws InterruptedException {    
        final GLProfile glp = GLProfile.getGL2ES2();
        final FBOMix2DemosES2 demo = new FBOMix2DemosES2(0);
        demo.setDoRotation(false);
        final GLCapabilities caps = new GLCapabilities(glp);
        testGLFBODrawableImpl(caps, demo);
    }
    
    @Test
    public void testGL2ES2_FBODemoMSAA4() throws InterruptedException {    
        final GLProfile glp = GLProfile.getGL2ES2();
        final FBOMix2DemosES2 demo = new FBOMix2DemosES2(0);
        demo.setDoRotation(false);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        testGLFBODrawableImpl(caps, demo);
    }
    
    @Test
    public void testEGLES2_Demo0Normal() throws InterruptedException {    
        if( GLProfile.isAvailable(GLProfile.GLES2) )  {
            final GLProfile glp = GLProfile.get(GLProfile.GLES2);
            final GLCapabilities caps = new GLCapabilities(glp);
            testGLFBODrawableImpl(caps, new GearsES2(0));
        } else {
            System.err.println("EGL ES2 n/a");
        }
    }
    
    @Test
    public void testEGLES2_Demo0MSAA4() throws InterruptedException {    
        if( GLProfile.isAvailable(GLProfile.GLES2) )  {
            final GLProfile glp = GLProfile.get(GLProfile.GLES2);
            final GLCapabilities caps = new GLCapabilities(glp);
            caps.setSampleBuffers(true);
            caps.setNumSamples(4);
            testGLFBODrawableImpl(caps, new GearsES2(0));
        } else {
            System.err.println("EGL ES2 n/a");
        }
    }

    boolean skipShot = false;
    
    void testGLFBODrawableImpl(GLCapabilities caps, GLEventListener demo) throws InterruptedException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(false, false);
        caps.setFBO(true);
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLDrawable fboDrawable = factory.createOffscreenDrawable(null, caps, null, widthStep*szStep, heightStep*szStep);
        Assert.assertNotNull(fboDrawable);
        Assert.assertTrue("Not an FBO Drawable", fboDrawable instanceof GLFBODrawableImpl);
        
        fboDrawable.setRealized(true);
        Assert.assertTrue(fboDrawable.isRealized());
        
        final FBObject fbo = ((GLFBODrawableImpl)fboDrawable).getFBObject();
        
        System.out.println("Realized: "+fboDrawable);
        System.out.println("Realized: "+fboDrawable.getChosenGLCapabilities());
        System.out.println("Realized: "+fbo);
        
        final GLContext context = fboDrawable.createContext(null);
        Assert.assertNotNull(context);
        
        int res = context.makeCurrent();
        Assert.assertTrue(GLContext.CONTEXT_CURRENT_NEW==res || GLContext.CONTEXT_CURRENT==res);
        context.release();
        
        System.out.println("Post Create-Ctx: "+fbo);        
        final FBObject.Colorbuffer colorA = fbo.getColorbuffer(0);
        Assert.assertNotNull(colorA);
        final FBObject.RenderAttachment depthA = fbo.getDepthAttachment();
        Assert.assertNotNull(depthA);
        
        final OffscreenAutoDrawable glad = new OffscreenAutoDrawable(fboDrawable, context, true);

        glad.addGLEventListener(demo);
        glad.addGLEventListener(new GLEventListener() {
            volatile int displayCount=0;
            volatile int reshapeCount=0;
            public void init(GLAutoDrawable drawable) {}
            public void dispose(GLAutoDrawable drawable) {}
            public void display(GLAutoDrawable drawable) {
                final GL gl = drawable.getGL();
                // System.err.println(Thread.currentThread().getName()+": ** display: "+displayCount+": step "+szStep+" "+drawable.getWidth()+"x"+drawable.getHeight());
                // System.err.println(Thread.currentThread().getName()+": ** FBO-THIS: "+fbo);
                // System.err.println(Thread.currentThread().getName()+": ** FBO-SINK: "+fbo.getSamplingSinkFBO());
                // System.err.println(Thread.currentThread().getName()+": ** drawable-read: "+gl.getDefaultReadFramebuffer());
                if(skipShot) {
                    skipShot=false;
                } else {
                    snapshot(getSimpleTestName("."), displayCount, "msaa"+fbo.getNumSamples(), gl, screenshot, TextureIO.PNG, null);
                }
                Assert.assertEquals(drawable.getWidth(), widthStep*szStep);
                Assert.assertEquals(drawable.getHeight(), heightStep*szStep); 
                displayCount++;
            }
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
                System.err.println(Thread.currentThread().getName()+": ** reshape: "+reshapeCount+": step "+szStep+" "+width+"x"+height+" - "+drawable.getWidth()+"x"+drawable.getHeight());
                Assert.assertEquals(drawable.getWidth(), widthStep*szStep);
                Assert.assertEquals(drawable.getHeight(), heightStep*szStep);                
                reshapeCount++;
            }
        });

        // 0 - szStep = 2
        glad.display();
        
        // 1, 2 (resize + display)
        szStep = 1;
        skipShot=true;
        glad.setSize(widthStep*szStep, heightStep*szStep);
        glad.display();
        Assert.assertEquals(glad.getWidth(), widthStep*szStep);
        Assert.assertEquals(glad.getHeight(), heightStep*szStep);
        {
            // Check whether the attachment reference are still valid!
            FBObject.Colorbuffer _colorA = fbo.getColorbuffer(0);
            Assert.assertNotNull(_colorA);
            Assert.assertTrue(colorA == _colorA);
            Assert.assertTrue(colorA.equals(_colorA));
            FBObject.RenderAttachment _depthA = fbo.getDepthAttachment();
            Assert.assertNotNull(_depthA);
            Assert.assertTrue(depthA == _depthA);
            Assert.assertTrue(depthA.equals(_depthA));
            
            _colorA = fbo.getColorbuffer(colorA);
            Assert.assertNotNull(_colorA);
            Assert.assertTrue(colorA == _colorA);
            Assert.assertTrue(colorA.equals(_colorA));
        }
        
        // 3, 4 (resize + display)
        szStep = 4;
        skipShot=true;
        glad.setSize(widthStep*szStep, heightStep*szStep);
        glad.display();
        Assert.assertEquals(glad.getWidth(), widthStep*szStep);
        Assert.assertEquals(glad.getHeight(), heightStep*szStep);
        {
            // Check whether the attachment reference are still valid!
            FBObject.Colorbuffer _colorA = fbo.getColorbuffer(0);
            Assert.assertNotNull(_colorA);
            Assert.assertTrue(colorA == _colorA);
            final FBObject.RenderAttachment _depthA = fbo.getDepthAttachment();
            Assert.assertNotNull(_depthA);
            Assert.assertTrue(depthA == _depthA);
            
            _colorA = fbo.getColorbuffer(colorA);
            Assert.assertNotNull(_colorA);
            Assert.assertTrue(colorA == _colorA);
        }
        
        // 5
        glad.display();
        Assert.assertEquals(glad.getWidth(), widthStep*szStep);
        Assert.assertEquals(glad.getHeight(), heightStep*szStep);
        
        // 6, 7 (resize + display)
        szStep = 3;
        skipShot=true;
        glad.setSize(widthStep*szStep, heightStep*szStep);
        glad.display();
        Assert.assertEquals(glad.getWidth(), widthStep*szStep);
        Assert.assertEquals(glad.getHeight(), heightStep*szStep);
        
        glad.destroy();
        System.out.println("Fin: "+fboDrawable);
        
        // final GLAutoDrawableDelegate glad = new GLAutoDrawableDelegate(fboDrawable, context);
    }
    
    public static void main(String args[]) throws IOException {
        org.junit.runner.JUnitCore.main(TestFBODrawableNEWT.class.getName());
    }

}
