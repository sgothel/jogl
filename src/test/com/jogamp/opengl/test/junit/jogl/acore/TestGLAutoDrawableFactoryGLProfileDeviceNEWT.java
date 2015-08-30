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

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.GLEventListenerCounter;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Testing producing {@link GLContext} instances of different {@link GLProfile}s
 * using different {@link AbstractGraphicsDevice}s.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLAutoDrawableFactoryGLProfileDeviceNEWT extends UITestCase {
    static final int widthStep = 800/4;
    static final int heightStep = 600/4;
    volatile int szStep = 2;

    static GLProfile getProfile(final AbstractGraphicsDevice device, final String profile) {
        if( !GLProfile.isAvailable(device, profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return null;
        } else {
            return GLProfile.get(device, profile);
        }
    }

    void doTest(final boolean isEGL, final GLDrawableFactory factory, final AbstractGraphicsDevice device,
                final GLCapabilitiesImmutable reqGLCaps, final GLEventListener demo) throws InterruptedException {
        System.err.println("Factory: "+factory.getClass().getName());
        System.err.println("Requested GL Caps: "+reqGLCaps);

        //
        // Create native OpenGL resources .. XGL/WGL/CGL ..
        // equivalent to GLAutoDrawable methods: setVisible(true)
        //
        final GLOffscreenAutoDrawable glad = factory.createOffscreenAutoDrawable(device, reqGLCaps, null, widthStep*szStep, heightStep*szStep);

        Assert.assertNotNull(glad);
        Assert.assertTrue(glad.isRealized());

        // Check caps of NativeWindow config w/o GL
        final CapabilitiesImmutable chosenCaps = glad.getChosenGLCapabilities();
        Assert.assertNotNull(chosenCaps);

        glad.display(); // force native context creation

        // Check caps of GLDrawable after realization
        final GLCapabilitiesImmutable chosenGLCaps = glad.getChosenGLCapabilities();
        Assert.assertNotNull(chosenGLCaps);
        System.err.println("Choosen   GL Caps: "+chosenGLCaps);

        glad.addGLEventListener(demo);
        final GLEventListenerCounter glelc = new GLEventListenerCounter();
        glad.addGLEventListener(glelc);

        glad.display(); // initial resize/display

        // 1 - szStep = 2
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        glad.display();

        // 2, 3 (resize + display)
        szStep = 1;
        glad.setSurfaceSize(widthStep*szStep, heightStep*szStep);
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        glad.display();

        Thread.sleep(50);

        final AbstractGraphicsDevice adevice = glad.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
        glad.destroy();
        System.err.println("Fin isEGL "+isEGL+", "+adevice);
        System.err.println("Fin "+glelc);
        Assert.assertTrue("init count: "+glelc, glelc.initCount > 0);
        Assert.assertTrue("reshape count: "+glelc, glelc.reshapeCount > 0);
        Assert.assertTrue("display count: "+glelc, glelc.displayCount > 0);
        Assert.assertTrue("dispose count: "+glelc, glelc.disposeCount > 0);
        Assert.assertEquals("EGL/Desktop not matching: isEGL "+isEGL+", "+adevice, isEGL, adevice instanceof EGLGraphicsDevice);
    }

    @Test
    public void test00AvailableInfo() {
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
    public void test01ES2OnEGL() throws InterruptedException {
        final GLDrawableFactory factory = GLDrawableFactory.getEGLFactory();
        if( null == factory ) {
            System.err.println("EGL Factory n/a");
            return;
        }
        final AbstractGraphicsDevice prodDevice = factory.getDefaultDevice();
        final GLProfile glp = getProfile(prodDevice, GLProfile.GLES2);
        if(null != glp) {
            Assert.assertTrue("Not a GLES2 profile but "+glp, glp.isGLES2());
            Assert.assertTrue("Not a GL2ES2 profile but "+glp, glp.isGL2ES2());
        }
        if(null == glp) {
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(glp);
        reqGLCaps.setOnscreen(false);
        final GearsES2 demo = new GearsES2(1);
        demo.setVerbose(false);
        doTest(true /* isEGL */, factory, prodDevice, reqGLCaps, demo);
    }

    @Test
    public void test02GLOnEGL() throws InterruptedException {
        final GLDrawableFactory factory = GLDrawableFactory.getEGLFactory();
        if( null == factory ) {
            System.err.println("EGL Factory n/a");
            return;
        }
        final AbstractGraphicsDevice prodDevice = factory.getDefaultDevice();
        final GLProfile glp = getProfile(prodDevice, GLProfile.GL2GL3);
        if(null != glp) {
            Assert.assertTrue("Not a GL2GL3 profile but "+glp, glp.isGL2GL3());
        }
        if(null == glp || !glp.isGL2ES2()) {
            if( null != glp ) {
                System.err.println("Not a GL2ES2 profile but "+glp);
            }
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(glp);
        reqGLCaps.setOnscreen(false);
        final GearsES2 demo = new GearsES2(1);
        demo.setVerbose(false);
        doTest(true /* isEGL */, factory, prodDevice, reqGLCaps, demo);
    }

    @Test
    public void test11ES2OnDesktop() throws InterruptedException {
        final GLDrawableFactory deskFactory = GLDrawableFactory.getDesktopFactory();
        if( null == deskFactory ) {
            System.err.println("Desktop Factory n/a");
            return;
        }
        final AbstractGraphicsDevice prodDevice = deskFactory.getDefaultDevice();
        final GLProfile glp = getProfile(prodDevice, GLProfile.GLES2);
        if(null != glp) {
            Assert.assertTrue("Not a GLES2 profile but "+glp, glp.isGLES2());
            Assert.assertTrue("Not a GL2ES2 profile but "+glp, glp.isGL2ES2());
        }
        if(null == glp) {
            return;
        }
        final GLDrawableFactory prodFactory = GLDrawableFactory.getFactory(glp);
        if( null == prodFactory ) {
            System.err.println("Production Factory n/a");
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(glp);
        reqGLCaps.setOnscreen(false);
        final GearsES2 demo = new GearsES2(1);
        demo.setVerbose(false);
        doTest(true /* isEGL */, prodFactory, prodDevice, reqGLCaps, demo);
    }

    @Test
    public void test12GLOnDesktop() throws InterruptedException {
        final GLDrawableFactory factory = GLDrawableFactory.getDesktopFactory();
        if( null == factory ) {
            System.err.println("Desktop Factory n/a");
            return;
        }
        final AbstractGraphicsDevice prodDevice = factory.getDefaultDevice();
        final GLProfile glp = getProfile(prodDevice, GLProfile.GL2GL3);
        if(null != glp) {
            Assert.assertTrue("Not a GL2GL3 profile but "+glp, glp.isGL2GL3());
        }
        if(null == glp || !glp.isGL2ES2()) {
            if( null != glp ) {
                System.err.println("Not a GL2ES2 profile but "+glp);
            }
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(glp);
        reqGLCaps.setOnscreen(false);
        final GearsES2 demo = new GearsES2(1);
        demo.setVerbose(false);
        doTest(false /* isEGL */, factory, prodDevice, reqGLCaps, demo);
    }

    public static void main(final String args[]) throws IOException {
        org.junit.runner.JUnitCore.main(TestGLAutoDrawableFactoryGLProfileDeviceNEWT.class.getName());
    }

}
