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
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import jogamp.opengl.GLGraphicsConfigurationUtil;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.opengl.GLAutoDrawableDelegate;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Tests using a NEWT {@link Window} for on- and offscreen cases.
 * <p>
 * Each test creates a {@link GLDrawable} using the
 * {@link GLDrawableFactory#createGLDrawable(com.jogamp.nativewindow.NativeSurface) factory model}.
 * The {@link GLContext} is derived {@link GLDrawable#createContext(GLContext) from the drawable}.
 * </p>
 * <p>
 * Finally a {@link GLAutoDrawableDelegate} is created with the just created {@link GLDrawable} and {@link GLContext}.
 * It is being used to run the {@link GLEventListener}.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLAutoDrawableDelegateOnOffscrnCapsNEWT extends UITestCase {
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
        // Create native windowing resources .. X11/Win/OSX
        //
        final Window window = NewtFactory.createWindow(reqGLCaps);
        Assert.assertNotNull(window);
        window.setSize(widthStep*szStep, heightStep*szStep);
        window.setVisible(true);
        Assert.assertTrue(AWTRobotUtil.waitForVisible(window, true));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(window, true));
        System.out.println("Window: "+window.getClass().getName());

        // Check caps of NativeWindow config w/o GL
        final CapabilitiesImmutable chosenCaps = window.getGraphicsConfiguration().getChosenCapabilities();
        System.out.println("Window Caps Pre_GL: "+chosenCaps);
        Assert.assertNotNull(chosenCaps);
        Assert.assertTrue(chosenCaps.getGreenBits()>5);
        Assert.assertTrue(chosenCaps.getBlueBits()>5);
        Assert.assertTrue(chosenCaps.getRedBits()>5);

        //
        // Create native OpenGL resources .. XGL/WGL/CGL ..
        // equivalent to GLAutoDrawable methods: setVisible(true)
        //
        final GLDrawable drawable = factory.createGLDrawable(window);
        Assert.assertNotNull(drawable);
        System.out.println("Drawable    Pre-GL(0): "+drawable.getClass().getName()+", "+drawable.getNativeSurface().getClass().getName());

        //
        drawable.setRealized(true);
        Assert.assertTrue(drawable.isRealized());

        System.out.println("Window Caps PostGL   : "+window.getGraphicsConfiguration().getChosenCapabilities());
        System.out.println("Drawable   Post-GL(1): "+drawable.getClass().getName()+", "+drawable.getNativeSurface().getClass().getName());

        // Note: FBO Drawable realization happens at 1st context.makeCurrent(),
        //       and hence only then it's caps can _fully_ reflect expectations,
        //       i.e. depth, stencil and MSAA will be valid only after makeCurrent(),
        //       where on-/offscreen state after setRealized(true)
        //       See GLFBODrawable API doc in this regard!


        final GLCapabilitiesImmutable chosenGLCaps01 = drawable.getChosenGLCapabilities();
        System.out.println("Chosen     GL Caps(1): "+chosenGLCaps01);
        Assert.assertNotNull(chosenGLCaps01);
        Assert.assertEquals(expGLCaps.isOnscreen(), chosenGLCaps01.isOnscreen());
        Assert.assertEquals(expGLCaps.isFBO(), chosenGLCaps01.isFBO());
        Assert.assertEquals(expGLCaps.isPBuffer(), chosenGLCaps01.isPBuffer());
        Assert.assertEquals(expGLCaps.isBitmap(), chosenGLCaps01.isBitmap());

        final GLContext context = drawable.createContext(null);
        Assert.assertNotNull(context);
        final int res = context.makeCurrent();
        Assert.assertTrue(GLContext.CONTEXT_CURRENT_NEW==res || GLContext.CONTEXT_CURRENT==res);
        context.release();

        // Check caps of GLDrawable after realization
        final GLCapabilitiesImmutable chosenGLCaps02 = drawable.getChosenGLCapabilities();
        System.out.println("Chosen     GL Caps(2): "+chosenGLCaps02);
        System.out.println("Chosen     GL CTX (2): "+context.getGLVersion());
        System.out.println("Drawable   Post-GL(2): "+drawable.getClass().getName()+", "+drawable.getNativeSurface().getClass().getName());
        Assert.assertNotNull(chosenGLCaps02);
        Assert.assertTrue(chosenGLCaps02.getGreenBits()>5);
        Assert.assertTrue(chosenGLCaps02.getBlueBits()>5);
        Assert.assertTrue(chosenGLCaps02.getRedBits()>5);
        Assert.assertTrue(chosenGLCaps02.getDepthBits()>4);
        Assert.assertEquals(expGLCaps.isOnscreen(), chosenGLCaps02.isOnscreen());
        Assert.assertEquals(expGLCaps.isFBO(), chosenGLCaps02.isFBO());
        Assert.assertEquals(expGLCaps.isPBuffer(), chosenGLCaps02.isPBuffer());
        Assert.assertEquals(expGLCaps.isBitmap(), chosenGLCaps02.isBitmap());
        /** Single/Double buffer cannot be checked since result may vary ..
        if(chosenGLCaps.isOnscreen() || chosenGLCaps.isFBO()) {
            // dbl buffer may be disabled w/ offscreen pbuffer and bitmap
            Assert.assertEquals(expGLCaps.getDoubleBuffered(), chosenGLCaps.getDoubleBuffered());
        } */

        final GLAutoDrawableDelegate glad = new GLAutoDrawableDelegate(drawable, context, window, false, null) {
                @Override
                protected void destroyImplInLock() {
                    super.destroyImplInLock();  // destroys drawable/context
                    window.destroy(); // destroys the actual window, incl. the device
                }
            };

        window.setWindowDestroyNotifyAction( new Runnable() {
            public void run() {
                glad.windowDestroyNotifyOp();
            } } );

        window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowRepaint(final WindowUpdateEvent e) {
                    glad.windowRepaintOp();
                }

                @Override
                public void windowResized(final WindowEvent e) {
                    glad.windowResizedOp(window.getSurfaceWidth(), window.getSurfaceHeight());
                }
            });

        glad.addGLEventListener(demo);

        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        glad.addGLEventListener(snapshotGLEventListener);

        glad.display(); // initial resize/display

        // 1 - szStep = 2
        final int[] expSurfaceSize = glad.getNativeSurface().convertToPixelUnits(new int[] { widthStep*szStep, heightStep*szStep });
        Assert.assertTrue("Surface Size not reached: Expected "+expSurfaceSize[0]+"x"+expSurfaceSize[1]+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, expSurfaceSize[0], expSurfaceSize[1]));
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();

        // 2, 3 (resize + display)
        szStep = 1;
        window.setSize(widthStep*szStep, heightStep*szStep);
        expSurfaceSize[0] = widthStep*szStep;
        expSurfaceSize[1] = heightStep*szStep;
        glad.getNativeSurface().convertToPixelUnits(expSurfaceSize);
        Assert.assertTrue("Surface Size not reached: Expected "+expSurfaceSize[0]+"x"+expSurfaceSize[1]+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, expSurfaceSize[0], expSurfaceSize[1]));
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();

        // 4, 5 (resize + display)
        szStep = 4;
        window.setSize(widthStep*szStep, heightStep*szStep);
        expSurfaceSize[0] = widthStep*szStep;
        expSurfaceSize[1] = heightStep*szStep;
        glad.getNativeSurface().convertToPixelUnits(expSurfaceSize);
        Assert.assertTrue("Surface Size not reached: Expected "+expSurfaceSize[0]+"x"+expSurfaceSize[1]+", Is "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight(),
                          AWTRobotUtil.waitForSize(glad, expSurfaceSize[0], expSurfaceSize[1]));
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();

        Thread.sleep(50);

        glad.destroy();
        System.out.println("Fin Drawable: "+drawable);
        System.out.println("Fin Window: "+window);
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
    public void testGL2OnScreenSglBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setDoubleBuffered(false);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testGL2OnScreenDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testGL2OffScreenAutoDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testGL2OffScreenFBOSglBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        reqGLCaps.setDoubleBuffered(false);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testGL2OffScreenFBODblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testGL2OffScreenPbufferDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testGL2OffScreenPbufferSglBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        reqGLCaps.setDoubleBuffered(false);
        doTest(reqGLCaps, new GearsES2(1));
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

    @Test
    public void testES2OnScreenSglBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setDoubleBuffered(false);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testES2OnScreenDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testES2OffScreenAutoDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testES2OffScreenFBOSglBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        reqGLCaps.setDoubleBuffered(false);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testES2OffScreenFBODblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testES2OffScreenPbufferDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testES2OffScreenPbufferSglBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        reqGLCaps.setDoubleBuffered(false);
        doTest(reqGLCaps, new GearsES2(1));
    }

    /** Not implemented !
    @Test
    public void testES2OffScreenBitmapDblBuf() throws InterruptedException {
        if(!checkProfile(GLProfile.GLES2)) {
            return;
        }
        final GLCapabilities reqGLCaps = new GLCapabilities(GLProfile.get(GLProfile.GLES2));
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setBitmap(true);
        doTest(reqGLCaps, new GearsES2(1));
    } */

    public static void main(final String args[]) throws IOException {
        org.junit.runner.JUnitCore.main(TestGLAutoDrawableDelegateOnOffscrnCapsNEWT.class.getName());
    }

}
