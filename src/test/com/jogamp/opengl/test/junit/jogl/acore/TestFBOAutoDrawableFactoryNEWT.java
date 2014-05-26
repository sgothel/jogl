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

import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLOffscreenAutoDrawable;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.test.junit.jogl.demos.es2.FBOMix2DemosES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.MultisampleDemoES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Toolkit agnostic {@link GLOffscreenAutoDrawable.FBO} tests using the
 * {@link GLDrawableFactory#createOffscreenAutoDrawable(javax.media.nativewindow.AbstractGraphicsDevice, GLCapabilitiesImmutable, javax.media.opengl.GLCapabilitiesChooser, int, int, GLContext) factory model}.
 * <p>
 * The created {@link GLOffscreenAutoDrawable.FBO} is being used to run the {@link GLEventListener}.
 * </p>
 * <p>
 * Extensive FBO reconfiguration (size and sample buffer count) and validation are performed.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFBOAutoDrawableFactoryNEWT extends UITestCase {

    static final int widthStep = 800/4;
    static final int heightStep = 600/4;
    volatile int szStep = 2;

    interface MyGLEventListener extends GLEventListener {
        void setMakeSnapshot();
    }

    @Test
    public void test01_GL2ES2_Demo1_SingleBuffer_Normal() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setDoubleBuffered(false);
        testGLFBODrawableImpl(caps, new GearsES2(0));
    }

    @Test
    public void test02_GL2ES2_Demo1_DoubleBuffer_Normal() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setDoubleBuffered(true); // default
        testGLFBODrawableImpl(caps, new GearsES2(0));
    }

    @Test
    public void test03_GL2ES2_Demo2MSAA4() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        testGLFBODrawableImpl(caps, new MultisampleDemoES2(true));
    }

    @Test
    public void test04_GL2ES2_FBODemoMSAA4() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final FBOMix2DemosES2 demo = new FBOMix2DemosES2(0);
        demo.setDoRotation(false);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        testGLFBODrawableImpl(caps, demo);
    }

    @Test
    public void test05_EGLES2_Demo0Normal() throws InterruptedException {
        if( GLProfile.isAvailable(GLProfile.GLES2) )  {
            final GLProfile glp = GLProfile.get(GLProfile.GLES2);
            final GLCapabilities caps = new GLCapabilities(glp);
            testGLFBODrawableImpl(caps, new GearsES2(0));
        } else {
            System.err.println("EGL ES2 n/a");
        }
    }

    @Test
    public void test06_GL3_Demo0Normal() throws InterruptedException {
        if( GLProfile.isAvailable(GLProfile.GL3) )  {
            final GLProfile glp = GLProfile.get(GLProfile.GL3);
            final GLCapabilities caps = new GLCapabilities(glp);
            testGLFBODrawableImpl(caps, new GearsES2(0));
        } else {
            System.err.println("GL3 n/a");
        }
    }

    @Test
    public void test07_EGLES2_Demo0MSAA4() throws InterruptedException {
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

    void testGLFBODrawableImpl(GLCapabilities caps, GLEventListener demo) throws InterruptedException {
        caps.setFBO(true);
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLOffscreenAutoDrawable.FBO glad = (GLOffscreenAutoDrawable.FBO)
                factory.createOffscreenAutoDrawable(null, caps, null, widthStep*szStep, heightStep*szStep);
        Assert.assertNotNull(glad);

        System.out.println("Realized GLAD: "+glad);
        System.out.println("Realized GLAD: "+glad.getChosenGLCapabilities());
        Assert.assertTrue("FBO drawable is initialized before ctx creation", !glad.isInitialized());

        glad.display(); // initial display incl. init!
        {
            final GLContext context = glad.getContext();
            Assert.assertNotNull(context);
            Assert.assertTrue(context.isCreated());
        }
        Assert.assertTrue("FBO drawable is not initialized after ctx creation", glad.isInitialized());

        //
        // FBO incl. MSAA is fully initialized now
        //

        final GLCapabilitiesImmutable chosenCaps = glad.getChosenGLCapabilities();
        System.out.println("Init GLAD: "+glad);
        System.out.println("Init GLAD: "+chosenCaps);

        final FBObject fboFront = glad.getFBObject(GL.GL_FRONT);
        final FBObject fboBack  = glad.getFBObject(GL.GL_BACK);

        System.out.println("Init front FBO: "+fboFront);
        System.out.println("Init back  FBO: "+fboBack);

        Assert.assertTrue("FBO drawable is not initialized before ctx creation", glad.isInitialized());
        Assert.assertTrue("FBO Front is not initialized before ctx creation", fboFront.isInitialized());
        Assert.assertTrue("FBO Back  is not initialized before ctx creation", fboBack.isInitialized());

        if( chosenCaps.getDoubleBuffered() ) {
            Assert.assertTrue("FBO are equal: "+fboFront+" == "+fboBack, !fboFront.equals(fboBack));
            Assert.assertNotSame(fboFront, fboBack);
        } else {
            Assert.assertTrue("FBO are not equal: "+fboFront+" != "+fboBack, fboFront.equals(fboBack));
            Assert.assertSame(fboFront, fboBack);
        }

        final FBObject.TextureAttachment texAttachA, texAttachB;

        texAttachA = glad.getTextureBuffer(GL.GL_FRONT);
        if(0==glad.getNumSamples()) {
            texAttachB = glad.getTextureBuffer(GL.GL_BACK);
        } else {
            texAttachB = null;
        }

        final FBObject.Colorbuffer colorA, colorB;
        final FBObject.RenderAttachment depthA, depthB;

        colorA = fboFront.getColorbuffer(0);
        Assert.assertNotNull(colorA);
        colorB = fboBack.getColorbuffer(0);
        Assert.assertNotNull(colorB);

        depthA = fboFront.getDepthAttachment();
        Assert.assertNotNull(depthA);
        depthB = fboBack.getDepthAttachment();
        Assert.assertNotNull(depthB);

        glad.display(); // SWAP_ODD

        if( chosenCaps.getDoubleBuffered() ) {
            // double buffer or MSAA
            Assert.assertTrue("Color attachments are equal: "+colorB+" == "+colorA, !colorB.equals(colorA));
            Assert.assertNotSame(colorB, colorA);
            Assert.assertTrue("Depth attachments are equal: "+depthB+" == "+depthA, !depthB.equals(depthA));
            Assert.assertNotSame(depthB, depthA);
        } else {
            // single buffer
            Assert.assertEquals(colorA, colorB);
            Assert.assertSame(colorA, colorB);
            Assert.assertEquals(depthA, depthB);
            Assert.assertSame(depthA, depthB);
        }

        Assert.assertEquals(texAttachA, colorA);
        Assert.assertSame(texAttachA, colorA);
        if(0==glad.getNumSamples()) {
            Assert.assertEquals(texAttachB, colorB);
            Assert.assertSame(texAttachB, colorB);
        }

        if( chosenCaps.getNumSamples() > 0 ) {
            // MSAA
            FBObject _fboFront = glad.getFBObject(GL.GL_FRONT);
            FBObject _fboBack = glad.getFBObject(GL.GL_BACK);
            Assert.assertTrue("FBO are not equal: "+fboFront+" != "+_fboFront, fboFront.equals(_fboFront));
            Assert.assertSame(fboFront, _fboFront);
            Assert.assertTrue("FBO are not equal: "+fboBack+" != "+_fboBack, fboBack.equals(_fboBack));
            Assert.assertSame(fboBack, _fboBack);
        } else if( chosenCaps.getDoubleBuffered() ) {
            // real double buffer
            FBObject _fboFront = glad.getFBObject(GL.GL_FRONT);
            FBObject _fboBack = glad.getFBObject(GL.GL_BACK);
            Assert.assertTrue("FBO are not equal: "+fboBack+" != "+_fboFront, fboBack.equals(_fboFront));
            Assert.assertSame(fboBack, _fboFront);
            Assert.assertTrue("FBO are not equal: "+fboFront+" != "+_fboBack, fboFront.equals(_fboBack));
            Assert.assertSame(fboFront, _fboBack);
        } else {
            // single buffer
            FBObject _fboFront = glad.getFBObject(GL.GL_FRONT);
            FBObject _fboBack = glad.getFBObject(GL.GL_BACK);
            Assert.assertTrue("FBO are not equal: "+fboFront+" != "+_fboFront, fboFront.equals(_fboFront));
            Assert.assertSame(fboFront, _fboFront);
            Assert.assertTrue("FBO are not equal: "+fboBack+" != "+_fboFront, fboBack.equals(_fboFront));
            Assert.assertSame(fboBack, _fboFront);
            Assert.assertTrue("FBO are not equal: "+fboBack+" != "+_fboBack, fboBack.equals(_fboBack));
            Assert.assertSame(fboBack, _fboBack);
            Assert.assertTrue("FBO are not equal: "+fboFront+" != "+_fboBack, fboFront.equals(_fboBack));
            Assert.assertSame(fboFront, _fboBack);
        }

        glad.addGLEventListener(demo);

        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        glad.addGLEventListener(snapshotGLEventListener);

        glad.display(); // - SWAP_EVEN

        // 1 - szStep = 2
        snapshotGLEventListener.setMakeSnapshot();
        glad.display(); // - SWAP_ODD

        // 2, 3 (resize + display)
        szStep = 1;
        glad.setSurfaceSize(widthStep*szStep, heightStep*szStep); // SWAP_EVEN
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();  //  - SWAP_ODD
        glad.display();  //  - SWAP_EVEN
        {
            // Check whether the attachment reference are still valid!
            final FBObject _fboFront = glad.getFBObject(GL.GL_FRONT);
            final FBObject _fboBack = glad.getFBObject(GL.GL_BACK);
            System.out.println("Resize1.oldFront: "+fboFront);
            System.out.println("Resize1.nowFront: "+_fboFront);
            System.out.println("Resize1.oldBack : "+fboBack);
            System.out.println("Resize1.nowBack : "+_fboBack);
            Assert.assertEquals(fboFront, _fboFront);
            Assert.assertSame(fboFront, _fboFront);
            Assert.assertEquals(fboBack,  _fboBack);
            Assert.assertSame(fboBack,  _fboBack);

            FBObject.Colorbuffer _color = _fboFront.getColorbuffer(0);
            Assert.assertNotNull(_color);
            Assert.assertEquals(colorA, _color);
            Assert.assertSame(colorA, _color);

            FBObject.RenderAttachment _depth = _fboFront.getDepthAttachment();
            System.err.println("Resize1.oldDepth "+depthA);
            System.err.println("Resize1.newDepth "+_depth);
            Assert.assertNotNull(_depth);

            Assert.assertEquals(depthA, _depth);
            Assert.assertSame(depthA, _depth);
            _depth = _fboBack.getDepthAttachment();
            Assert.assertNotNull(_depth);
            Assert.assertEquals(depthB, _depth);
            Assert.assertSame(depthB, _depth);

            _color = _fboFront.getColorbuffer(colorA);
            Assert.assertNotNull(_color);
            Assert.assertEquals(colorA, _color);
            Assert.assertSame(colorA, _color);
        }

        // 4, 5 (resize + display)
        szStep = 4;
        glad.setSurfaceSize(widthStep*szStep, heightStep*szStep); // SWAP_ODD
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        snapshotGLEventListener.setMakeSnapshot();
        glad.display(); //  - SWAP_EVEN
        glad.display(); //  - SWAP_ODD
        {
            // Check whether the attachment reference are still valid!
            final FBObject _fboFront = glad.getFBObject(GL.GL_FRONT);
            final FBObject _fboBack = glad.getFBObject(GL.GL_BACK);
            System.out.println("Resize2.oldFront: "+fboFront);
            System.out.println("Resize2.nowFront: "+_fboFront);
            System.out.println("Resize2.oldBack : "+fboBack);
            System.out.println("Resize2.nowBack : "+_fboBack);
            if(chosenCaps.getDoubleBuffered() && 0==chosenCaps.getNumSamples()) {
                // real double buffer
                Assert.assertEquals(fboBack,  _fboFront);
                Assert.assertEquals(fboFront, _fboBack);
            } else {
                // single or MSAA
                Assert.assertEquals(fboFront,  _fboFront);
                Assert.assertEquals(fboBack,   _fboBack);
            }

            FBObject.Colorbuffer _color = fboBack.getColorbuffer(0);
            Assert.assertNotNull(_color);
            Assert.assertEquals(colorB, _color);
            Assert.assertSame(colorB, _color);

            FBObject.RenderAttachment _depth = fboBack.getDepthAttachment();
            Assert.assertNotNull(_depth); // MSAA back w/ depth
            Assert.assertEquals(depthB, _depth);
            Assert.assertSame(depthB, _depth);

            _depth = fboFront.getDepthAttachment();
            Assert.assertNotNull(_depth);
            Assert.assertEquals(depthA, _depth);
            Assert.assertSame(depthA, _depth);

            _color = fboBack.getColorbuffer(colorB);
            Assert.assertNotNull(_color);
            Assert.assertEquals(colorB, _color);
            Assert.assertSame(colorB, _color);
        }

        // 6 + 7 (samples + display)
        glad.setNumSamples(glad.getGL(), chosenCaps.getNumSamples() > 0 ? 0 : 4); // triggers repaint
        snapshotGLEventListener.setMakeSnapshot();
        glad.display(); // actual screenshot

        // 8, 9 (resize + samples + display)
        szStep = 3;
        glad.setSurfaceSize(widthStep*szStep, heightStep*szStep);
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();

        glad.destroy();
        System.out.println("Fin: "+glad);
    }

    public static void main(String args[]) throws Exception {
        org.junit.runner.JUnitCore.main(TestFBOAutoDrawableFactoryNEWT.class.getName());
    }

}
