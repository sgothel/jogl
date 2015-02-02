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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLFBODrawable;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

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
 * {@link GLDrawableFactory#createOffscreenAutoDrawable(com.jogamp.nativewindow.AbstractGraphicsDevice, GLCapabilitiesImmutable, com.jogamp.opengl.GLCapabilitiesChooser, int, int, GLContext) factory model}.
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
    public void test01a_GL2ES2_Demo1_SingleBuffer_Normal() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setDoubleBuffered(false);
        testGLFBODrawableImpl(caps, GLFBODrawable.FBOMODE_USE_TEXTURE, new GearsES2(0));
    }
    @Test
    public void test01b_GL2ES2_Demo1_SingleBuffer_NoTex() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setDoubleBuffered(false);
        testGLFBODrawableImpl(caps, 0, new GearsES2(0));
    }
    @Test
    public void test01c_GL2ES2_Demo1_SingleBuffer_NoTexNoDepth() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setDoubleBuffered(false);
        caps.setDepthBits(0);
        testGLFBODrawableImpl(caps, 0, new GearsES2(0));
    }

    @Test
    public void test02a_GL2ES2_Demo1_DoubleBuffer_Normal() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setDoubleBuffered(true); // default
        testGLFBODrawableImpl(caps, GLFBODrawable.FBOMODE_USE_TEXTURE, new GearsES2(0));
    }

    @Test
    public void test03a_GL2ES2_Demo2MSAA4_Normal() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        testGLFBODrawableImpl(caps, GLFBODrawable.FBOMODE_USE_TEXTURE, new MultisampleDemoES2(true));
    }
    @Test
    public void test03b_GL2ES2_Demo2MSAA4_NoTex() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        testGLFBODrawableImpl(caps, 0, new MultisampleDemoES2(true));
    }
    @Test
    public void test03c_GL2ES2_Demo2MSAA4_NoTexNoDepth() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        caps.setDepthBits(0);
        testGLFBODrawableImpl(caps, 0, new MultisampleDemoES2(true));
    }

    @Test
    public void test04_GL2ES2_FBODemoMSAA4_Normal() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final FBOMix2DemosES2 demo = new FBOMix2DemosES2(0);
        demo.setDoRotation(false);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        testGLFBODrawableImpl(caps, GLFBODrawable.FBOMODE_USE_TEXTURE, demo);
    }

    @Test
    public void test11_EGLES2_Demo0Normal() throws InterruptedException {
        if( GLProfile.isAvailable(GLProfile.GLES2) )  {
            final GLProfile glp = GLProfile.get(GLProfile.GLES2);
            final GLCapabilities caps = new GLCapabilities(glp);
            testGLFBODrawableImpl(caps, GLFBODrawable.FBOMODE_USE_TEXTURE, new GearsES2(0));
        } else {
            System.err.println("EGL ES2 n/a");
        }
    }

    @Test
    public void test13_EGLES2_Demo0MSAA4() throws InterruptedException {
        if( GLProfile.isAvailable(GLProfile.GLES2) )  {
            final GLProfile glp = GLProfile.get(GLProfile.GLES2);
            final GLCapabilities caps = new GLCapabilities(glp);
            caps.setSampleBuffers(true);
            caps.setNumSamples(4);
            testGLFBODrawableImpl(caps, GLFBODrawable.FBOMODE_USE_TEXTURE, new GearsES2(0));
        } else {
            System.err.println("EGL ES2 n/a");
        }
    }

    @Test
    public void test21_GL3_Demo0Normal() throws InterruptedException {
        if( GLProfile.isAvailable(GLProfile.GL3) )  {
            final GLProfile glp = GLProfile.get(GLProfile.GL3);
            final GLCapabilities caps = new GLCapabilities(glp);
            testGLFBODrawableImpl(caps, GLFBODrawable.FBOMODE_USE_TEXTURE, new GearsES2(0));
        } else {
            System.err.println("GL3 n/a");
        }
    }

    void testGLFBODrawableImpl(final GLCapabilities caps, final int fboMode, final GLEventListener demo) throws InterruptedException {
        caps.setFBO(true);
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLOffscreenAutoDrawable.FBO glad = (GLOffscreenAutoDrawable.FBO)
                factory.createOffscreenAutoDrawable(null, caps, null, widthStep*szStep, heightStep*szStep);
        Assert.assertNotNull(glad);

        System.out.println("Requested:     "+caps);
        System.out.println("Realized GLAD: "+glad);
        System.out.println("Realized GLAD: "+glad.getChosenGLCapabilities());
        Assert.assertTrue("FBO drawable is initialized before ctx creation", !glad.isInitialized());
        glad.setFBOMode(fboMode);

        glad.display(); // initial display incl. init!
        {
            final GLContext context = glad.getContext();
            Assert.assertNotNull(context);
            Assert.assertTrue(context.isCreated());
        }
        Assert.assertTrue("FBO drawable is not initialized after ctx creation", glad.isInitialized());

        final boolean expDepth = caps.getDepthBits() > 0;
        final boolean reqDepth = glad.getRequestedGLCapabilities().getDepthBits() > 0;
        final boolean hasDepth = glad.getChosenGLCapabilities().getDepthBits() > 0;
        System.out.println("Depth: exp "+expDepth+", req "+reqDepth+", has "+hasDepth);
        Assert.assertEquals("Depth: expected not passed to requested", expDepth, reqDepth);
        Assert.assertEquals("Depth: requested not passed to chosen", reqDepth, hasDepth);

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
            Assert.assertNotEquals("FBO are equal: "+fboFront+" == "+fboBack, fboFront, fboBack);
            Assert.assertNotSame(fboFront, fboBack);
        } else {
            Assert.assertEquals("FBO are not equal: "+fboFront+" != "+fboBack, fboFront, fboBack);
            Assert.assertSame(fboFront, fboBack);
        }

        final FBObject.Colorbuffer color0, color1;

        color0 = glad.getColorbuffer(GL.GL_FRONT);
        if(0==glad.getNumSamples()) {
            color1 = glad.getColorbuffer(GL.GL_BACK);
        } else {
            color1 = null;
        }

        final boolean expTexture = 0 != ( GLFBODrawable.FBOMODE_USE_TEXTURE & glad.getFBOMode() );
        System.out.println("Texture: exp "+expTexture+", hasFront "+color0.isTextureAttachment());
        Assert.assertEquals("Texture: Front", expTexture, color0.isTextureAttachment());
        if(0==glad.getNumSamples()) {
            Assert.assertEquals("Texture: Back", expTexture, color1.isTextureAttachment());
        }

        final FBObject.Colorbuffer colorA, colorB;
        final FBObject.RenderAttachment depthA, depthB;

        colorA = fboFront.getColorbuffer(0);
        Assert.assertNotNull(colorA);
        colorB = fboBack.getColorbuffer(0);
        Assert.assertNotNull(colorB);

        Assert.assertEquals("Texture: Front", expTexture, colorA.isTextureAttachment());
        if(0==glad.getNumSamples()) {
            Assert.assertEquals("Texture: Back", expTexture, colorB.isTextureAttachment());
        } else {
            Assert.assertEquals("Texture: MSAA Back is Texture", false, colorB.isTextureAttachment());
        }

        if( hasDepth ) {
            depthA = fboFront.getDepthAttachment();
            Assert.assertNotNull(depthA);
            depthB = fboBack.getDepthAttachment();
            Assert.assertNotNull(depthB);
        } else {
            depthA = null;
            depthB = null;
        }

        glad.display(); // SWAP_ODD

        if( chosenCaps.getDoubleBuffered() ) {
            // double buffer or MSAA
            Assert.assertNotEquals("Color attachments are equal: "+colorB+" == "+colorA, colorA, colorB);
            Assert.assertNotSame(colorB, colorA);
            if( hasDepth ) {
                Assert.assertNotEquals("Depth attachments are equal: "+depthB+" == "+depthA, depthA, depthB);
                Assert.assertNotSame(depthB, depthA);
            }
        } else {
            // single buffer
            Assert.assertEquals(colorA, colorB);
            Assert.assertSame(colorA, colorB);
            Assert.assertEquals(depthA, depthB);
            Assert.assertSame(depthA, depthB);
        }

        Assert.assertEquals(color0, colorA);
        Assert.assertSame(color0, colorA);
        if(0==glad.getNumSamples()) {
            Assert.assertEquals(color1, colorB);
            Assert.assertSame(color1, colorB);
        }

        if( chosenCaps.getNumSamples() > 0 ) {
            // MSAA
            final FBObject _fboFront = glad.getFBObject(GL.GL_FRONT);
            final FBObject _fboBack = glad.getFBObject(GL.GL_BACK);
            Assert.assertEquals("FBO are not equal: "+fboFront+" != "+_fboFront, fboFront, _fboFront);
            Assert.assertSame(fboFront, _fboFront);
            Assert.assertEquals("FBO are not equal: "+fboBack+" != "+_fboBack, fboBack, _fboBack);
            Assert.assertSame(fboBack, _fboBack);
        } else if( chosenCaps.getDoubleBuffered() ) {
            // real double buffer
            final FBObject _fboFront = glad.getFBObject(GL.GL_FRONT);
            final FBObject _fboBack = glad.getFBObject(GL.GL_BACK);
            Assert.assertEquals("FBO are not equal: "+fboBack+" != "+_fboFront, fboBack, _fboFront);
            Assert.assertSame(fboBack, _fboFront);
            Assert.assertEquals("FBO are not equal: "+fboFront+" != "+_fboBack, fboFront, _fboBack);
            Assert.assertSame(fboFront, _fboBack);
        } else {
            // single buffer
            final FBObject _fboFront = glad.getFBObject(GL.GL_FRONT);
            final FBObject _fboBack = glad.getFBObject(GL.GL_BACK);
            Assert.assertEquals("FBO are not equal: "+fboFront+" != "+_fboFront, fboFront, _fboFront);
            Assert.assertSame(fboFront, _fboFront);
            Assert.assertEquals("FBO are not equal: "+fboBack+" != "+_fboFront, fboBack, _fboFront);
            Assert.assertSame(fboBack, _fboFront);
            Assert.assertEquals("FBO are not equal: "+fboBack+" != "+_fboBack, fboBack, _fboBack);
            Assert.assertSame(fboBack, _fboBack);
            Assert.assertEquals("FBO are not equal: "+fboFront+" != "+_fboBack, fboFront, _fboBack);
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
            if( hasDepth ) {
                Assert.assertNotNull(_depth);
            }

            Assert.assertEquals(depthA, _depth);
            Assert.assertSame(depthA, _depth);
            _depth = _fboBack.getDepthAttachment();
            if( hasDepth ) {
                Assert.assertNotNull(_depth);
            }
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
            if( hasDepth ) {
                Assert.assertNotNull(_depth); // MSAA back w/ depth
            }
            Assert.assertEquals(depthB, _depth);
            Assert.assertSame(depthB, _depth);

            _depth = fboFront.getDepthAttachment();
            if( hasDepth ) {
                Assert.assertNotNull(_depth);
            }
            Assert.assertEquals(depthA, _depth);
            Assert.assertSame(depthA, _depth);

            _color = fboBack.getColorbuffer(colorB);
            Assert.assertNotNull(_color);
            Assert.assertEquals(colorB, _color);
            Assert.assertSame(colorB, _color);
        }

        // 6 + 7 (samples + display)
        final int oldSampleCount = chosenCaps.getNumSamples();
        final int newSampleCount = oldSampleCount > 0 ? 0 : 4;
        System.out.println("Resize3.sampleCount: "+oldSampleCount+" -> "+newSampleCount);
        glad.setNumSamples(glad.getGL(), newSampleCount); // triggers repaint
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

    public static void main(final String args[]) throws Exception {
        org.junit.runner.JUnitCore.main(TestFBOAutoDrawableFactoryNEWT.class.getName());
    }

}
