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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.test.junit.jogl.demos.GLFinishOnDisplay;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.Mix2TexturesES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.SurfaceUpdatedListener;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Toolkit agnostic {@link GLOffscreenAutoDrawable.FBO} tests using the
 * {@link GLDrawableFactory#createOffscreenAutoDrawable(com.jogamp.nativewindow.AbstractGraphicsDevice, GLCapabilitiesImmutable, com.jogamp.opengl.GLCapabilitiesChooser, int, int, GLContext) factory model}.
 * <p>
 * The created {@link GLOffscreenAutoDrawable.FBO} is being used to run the {@link GLEventListener}.
 * </p>
 * <p>
 * This test simulates shared off-thread GL context / texture usage,
 * where the producer use FBOs and delivers shared textures.
 * The receiver blends the shared textures onscreen.
 * In detail the test consist of:
 * <ul>
 *   <li>2 {@link GLOffscreenAutoDrawable.FBO} double buffered
 *   <ul>
 *     <li>each with their own {@link GLContext}, which is shares the {@link GLWindow} one (see below)</li>
 *     <li>both run within one {@link FPSAnimator} @ 30fps</li>
 *     <li>produce a texture</li>
 *     <li>notify the onscreen renderer about new textureID (swapping double buffer)</li>
 *   </ul></li>
 *   <li>1 onscreen {@link GLWindow}
 *   <ul>
 *     <li>shares it's {@link GLContext} w/ above FBOs</li>
 *     <li>running within one {@link Animator} at v-sync</li>
 *     <li>uses the shared FBO textures and blends them onscreen</li>
 *   </ul></li>
 * </ul>
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFBOOffThreadSharedContextMix2DemosES2NEWT extends UITestCase {
    static long duration = 500; // ms
    static int swapInterval = 1;
    static boolean showFPS = false;
    static boolean forceES2 = false;
    static boolean mainRun = false;

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(final GLCapabilitiesImmutable caps) throws InterruptedException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(false, false);
        System.err.println("requested: vsync "+swapInterval+", "+caps);

        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Gears NEWT Test (translucent "+!caps.isBackgroundOpaque()+"), swapInterval "+swapInterval);
        if(mainRun) {
            glWindow.setSize(512, 512);
        } else {
            glWindow.setSize(256, 256);
        }
        // eager initialization of context
        glWindow.setVisible(true);
        glWindow.display();

        final int fbod1_texUnit = 0;
        final int fbod2_texUnit = 1;

        final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLCapabilities fbodCaps = (GLCapabilities) caps.cloneMutable();
        // fbodCaps.setDoubleBuffered(false);

        final Mix2TexturesES2 mixerDemo = new Mix2TexturesES2(1, fbod1_texUnit, fbod2_texUnit);

        // FBOD1
        final GLOffscreenAutoDrawable.FBO fbod1 = (GLOffscreenAutoDrawable.FBO)
                factory.createOffscreenAutoDrawable(null, fbodCaps, null, glWindow.getSurfaceWidth(), glWindow.getSurfaceHeight());
        fbod1.setSharedAutoDrawable(glWindow);
        fbod1.setUpstreamWidget(glWindow); // connect the real GLWindow (mouse/key) to offscreen!
        fbod1.setTextureUnit(fbod1_texUnit);
        {
            final GearsES2 demo0 = new GearsES2(-1);
            fbod1.addGLEventListener(demo0);
            fbod1.addGLEventListener(new GLFinishOnDisplay());
            demo0.setIgnoreFocus(true);
        }
        fbod1.getNativeSurface().addSurfaceUpdatedListener(new SurfaceUpdatedListener() {
            @Override
            public void surfaceUpdated(final Object updater, final NativeSurface ns, final long when) {
                mixerDemo.setTexID0(fbod1.getColorbuffer(GL.GL_FRONT).getName());
            } });
        fbod1.display(); // init
        System.err.println("FBOD1 "+fbod1);
        Assert.assertTrue(fbod1.isInitialized());

        // FBOD2
        final GLOffscreenAutoDrawable.FBO fbod2 = (GLOffscreenAutoDrawable.FBO)
                factory.createOffscreenAutoDrawable(null, fbodCaps, null, glWindow.getSurfaceWidth(), glWindow.getSurfaceHeight());
        fbod2.setSharedAutoDrawable(glWindow);
        fbod2.setTextureUnit(fbod2_texUnit);
        fbod2.addGLEventListener(new RedSquareES2(-1));
        fbod2.addGLEventListener(new GLFinishOnDisplay());
        fbod2.getNativeSurface().addSurfaceUpdatedListener(new SurfaceUpdatedListener() {
            @Override
            public void surfaceUpdated(final Object updater, final NativeSurface ns, final long when) {
                mixerDemo.setTexID1(fbod2.getColorbuffer(GL.GL_FRONT).getName());
            } });
        fbod2.display(); // init
        System.err.println("FBOD2 "+fbod2);
        Assert.assertTrue(fbod2.isInitialized());

        // preinit texIDs
        mixerDemo.setTexID0(fbod1.getColorbuffer(GL.GL_FRONT).getName());
        mixerDemo.setTexID1(fbod2.getColorbuffer(GL.GL_FRONT).getName());

        glWindow.addGLEventListener(mixerDemo);
        glWindow.addGLEventListener(new GLEventListener() {
            int i=0, c=0;
            public void init(final GLAutoDrawable drawable) {}
            public void dispose(final GLAutoDrawable drawable) {}
            public void display(final GLAutoDrawable drawable) {
                if(mainRun) return;

                final int dw = drawable.getSurfaceWidth();
                final int dh = drawable.getSurfaceHeight();
                c++;

                if(dw<800) {
                    System.err.println("XXX: "+dw+"x"+dh+", c "+c);
                    if(8 == c) {
                        snapshot(i++, "msaa"+fbod1.getNumSamples(), drawable.getGL(), screenshot, TextureIO.PNG, null);
                    }
                    if(9 == c) {
                        c=0;
                        new InterruptSource.Thread() {
                            @Override
                            public void run() {
                                glWindow.setSize(dw+256, dh+256);
                            } }.start();
                    }
                }
            }
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                fbod1.setSurfaceSize(width, height);
                fbod2.setSurfaceSize(width, height);
            }
        });

        final FPSAnimator animator0 = new FPSAnimator(30);
        animator0.add(fbod1);
        animator0.add(fbod2);

        final Animator animator1 = new Animator();
        animator1.add(glWindow);

        final QuitAdapter quitAdapter = new QuitAdapter();

        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        glWindow.addWindowListener(new WindowAdapter() {
            public void windowResized(final WindowEvent e) {
                System.err.println("window resized: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
            }
            public void windowMoved(final WindowEvent e) {
                System.err.println("window moved:   "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
            }
        });

        animator0.start();
        animator1.start();
        // glWindow.setSkipContextReleaseThread(animator.getThread());

        glWindow.setVisible(true);

        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());

        animator0.setUpdateFPSFrames(30, showFPS ? System.err : null);
        animator1.setUpdateFPSFrames(60, showFPS ? System.err : null);

        while(!quitAdapter.shouldQuit() && animator1.isAnimating() && animator1.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator0.stop();
        Assert.assertFalse(animator0.isAnimating());
        Assert.assertFalse(animator0.isStarted());

        animator1.stop();
        Assert.assertFalse(animator1.isAnimating());
        Assert.assertFalse(animator1.isStarted());

        fbod1.destroy();
        fbod2.destroy();

        glWindow.destroy();
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow, false));
    }

    @Test
    public void test01() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(forceES2 ? GLProfile.get(GLProfile.GLES2) : GLProfile.getGL2ES2());
        caps.setAlphaBits(1);
        runTestGL(caps);
    }

    public static void main(final String args[]) throws IOException {
        boolean waitForKey = false;

        mainRun = true;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-showFPS")) {
                showFPS = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-nomain")) {
                mainRun = false;
            }
        }

        System.err.println("swapInterval "+swapInterval);
        System.err.println("forceES2 "+forceES2);

        if(waitForKey) {
            final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (final IOException e) { }
        }
        org.junit.runner.JUnitCore.main(TestFBOOffThreadSharedContextMix2DemosES2NEWT.class.getName());
    }
}
