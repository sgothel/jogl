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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.io.IOException;

import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import jogamp.opengl.GLGraphicsConfigurationUtil;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Toolkit agnostic {@link GLOffscreenAutoDrawable} tests using the
 * {@link GLDrawableFactory#createOffscreenAutoDrawable(com.jogamp.nativewindow.AbstractGraphicsDevice, GLCapabilitiesImmutable, com.jogamp.opengl.GLCapabilitiesChooser, int, int, GLContext) factory model}.
 * <p>
 * The created {@link GLOffscreenAutoDrawable} is being used to run the {@link GLEventListener}.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLAutoDrawableFactoryGL2OffscrnCapsNEWT extends UITestCase {
    static final int widthStep = 800/4;
    static final int heightStep = 600/4;
    volatile int szStep = 2;

    static GLCapabilities getCaps(final String profile) {
        if( !GLProfile.isAvailable(profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return null;
        }
        return new GLCapabilities(GLProfile.get(profile));
    }

    void doTest(final GLCapabilitiesImmutable reqGLCaps, final GLEventListener demo) throws InterruptedException {
        System.out.println("Requested  GL Caps: "+reqGLCaps);
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(reqGLCaps.getGLProfile());
        final GLCapabilitiesImmutable expGLCaps = GLGraphicsConfigurationUtil.fixGLCapabilities(reqGLCaps, factory, null);
        System.out.println("Expected   GL Caps: "+expGLCaps);

        //
        // Create native OpenGL resources .. XGL/WGL/CGL ..
        // equivalent to GLAutoDrawable methods: setVisible(true)
        //
        final GLOffscreenAutoDrawable glad = factory.createOffscreenAutoDrawable(null, reqGLCaps, null, widthStep*szStep, heightStep*szStep);

        Assert.assertNotNull(glad);
        System.out.println("Drawable    Pre-GL(0): "+glad.getClass().getName()+", "+glad.getNativeSurface().getClass().getName());
        Assert.assertTrue(glad.isRealized());

        // Check caps of NativeWindow config w/o GL
        final CapabilitiesImmutable chosenCaps = glad.getChosenGLCapabilities();
        System.out.println("Drawable Caps Pre_GL : "+chosenCaps);
        Assert.assertNotNull(chosenCaps);
        Assert.assertTrue(chosenCaps.getGreenBits()>4);
        Assert.assertTrue(chosenCaps.getBlueBits()>4);
        Assert.assertTrue(chosenCaps.getRedBits()>4);

        glad.display(); // force native context creation

        // Check caps of GLDrawable after realization
        final GLCapabilitiesImmutable chosenGLCaps = glad.getChosenGLCapabilities();
        System.out.println("Chosen     GL CTX (1): "+glad.getContext().getGLVersion());
        System.out.println("Chosen     GL Caps(1): "+chosenGLCaps);
        System.out.println("Chosen     GL Caps(2): "+glad.getNativeSurface().getGraphicsConfiguration().getChosenCapabilities());

        Assert.assertNotNull(chosenGLCaps);
        Assert.assertTrue(chosenGLCaps.getGreenBits()>4);
        Assert.assertTrue(chosenGLCaps.getBlueBits()>4);
        Assert.assertTrue(chosenGLCaps.getRedBits()>4);
        Assert.assertTrue(chosenGLCaps.getDepthBits()>4);
        Assert.assertEquals(expGLCaps.isOnscreen(), chosenGLCaps.isOnscreen());
        Assert.assertEquals(expGLCaps.isFBO(), chosenGLCaps.isFBO());
        Assert.assertEquals(expGLCaps.isPBuffer(), chosenGLCaps.isPBuffer());
        Assert.assertEquals(expGLCaps.isBitmap(), chosenGLCaps.isBitmap());
        /** Single/Double buffer cannot be checked since result may vary ..
        if(chosenGLCaps.isOnscreen() || chosenGLCaps.isFBO()) {
            // dbl buffer may be disabled w/ offscreen pbuffer and bitmap
            Assert.assertEquals(expGLCaps.getDoubleBuffered(), chosenGLCaps.getDoubleBuffered());
        } */

        glad.addGLEventListener(demo);

        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        glad.addGLEventListener(snapshotGLEventListener);

        glad.display(); // initial resize/display

        // 1 - szStep = 2
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();

        // 2, 3 (resize + display)
        szStep = 1;
        glad.setSurfaceSize(widthStep*szStep, heightStep*szStep);
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();

        // 4, 5 (resize + display)
        szStep = 4;
        glad.setSurfaceSize(widthStep*szStep, heightStep*szStep);
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();

        Thread.sleep(50);

        glad.destroy();
        System.out.println("Fin Drawable: "+glad);
    }

    @Test
    public void testAvailableInfo() {
        GLDrawableFactory f = GLDrawableFactory.getDesktopFactory();
        if(null != f) {
            System.err.println(JoglVersion.getDefaultOpenGLInfo(f.getDefaultDevice(), null, true).toString());
        }
        f = GLDrawableFactory.getEGLFactory();
        if(null != f) {
            System.err.println(JoglVersion.getDefaultOpenGLInfo(f.getDefaultDevice(), null, true).toString());
        }
    }

    @Test
    public void testGL2OffScreenAutoDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenFBODblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenFBOSglBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        reqGLCaps.setDoubleBuffered(false);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenFBODblBufStencil() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        reqGLCaps.setStencilBits(1);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenFBODblBufMSAA() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        reqGLCaps.setSampleBuffers(true);
        reqGLCaps.setNumSamples(4);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenFBODblBufStencilMSAA() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        reqGLCaps.setStencilBits(1);
        reqGLCaps.setSampleBuffers(true);
        reqGLCaps.setNumSamples(4);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenPbufferDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenPbufferSglBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        reqGLCaps.setDoubleBuffered(false);
        doTest(reqGLCaps, new Gears(1));
    }

    // Might be reduced to !stencil
    @Test
    public void testGL2OffScreenPbufferDblBufStencil() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        reqGLCaps.setStencilBits(1);
        doTest(reqGLCaps, new Gears(1));
    }

    // Might be reduced to !MSAA
    @Test
    public void testGL2OffScreenPbufferDblBufMSAA() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        reqGLCaps.setSampleBuffers(true);
        reqGLCaps.setNumSamples(4);
        doTest(reqGLCaps, new Gears(1));
    }

    // Might be reduced to !stencil && !MSAA
    @Test
    public void testGL2OffScreenPbufferDblBufStencilMSAA() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        reqGLCaps.setStencilBits(1);
        reqGLCaps.setSampleBuffers(true);
        reqGLCaps.setNumSamples(4);
        doTest(reqGLCaps, new Gears(1));
    }


    // Might be reduced to !double-buff
    @Test
    public void testGL2OffScreenBitmapDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenBitmapDblBufRGBA8881() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setRedBits(8);
        reqGLCaps.setGreenBits(8);
        reqGLCaps.setBlueBits(8);
        reqGLCaps.setAlphaBits(1);
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenBitmapDblBufRGB555() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setRedBits(5);
        reqGLCaps.setGreenBits(5);
        reqGLCaps.setBlueBits(5);
        reqGLCaps.setAlphaBits(0);
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenBitmapDblBufRGBA5551() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setRedBits(5);
        reqGLCaps.setGreenBits(5);
        reqGLCaps.setBlueBits(5);
        reqGLCaps.setAlphaBits(1);
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        doTest(reqGLCaps, new Gears(1));
    }

    @Test
    public void testGL2OffScreenBitmapSglBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        reqGLCaps.setDoubleBuffered(false);
        doTest(reqGLCaps, new Gears(1));
    }

    // Might be reduced to !stencil
    @Test
    public void testGL2OffScreenBitmapDblBufStencil() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        reqGLCaps.setStencilBits(1);
        doTest(reqGLCaps, new Gears(1));
    }

    // Might be reduced to !MSAA
    @Test
    public void testGL2OffScreenBitmapDblBufMSAA() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        reqGLCaps.setSampleBuffers(true);
        reqGLCaps.setNumSamples(4);
        doTest(reqGLCaps, new Gears(1));
    }

    // Might be reduced to !stencil && !MSAA
    @Test
    public void testGL2OffScreenBitmapDblBufStencilMSAA() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        reqGLCaps.setStencilBits(1);
        reqGLCaps.setSampleBuffers(true);
        reqGLCaps.setNumSamples(4);
        doTest(reqGLCaps, new Gears(1));
    }

    public static void main(final String args[]) throws IOException {
        org.junit.runner.JUnitCore.main(TestGLAutoDrawableFactoryGL2OffscrnCapsNEWT.class.getName());
    }

}
