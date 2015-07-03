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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.io.IOException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

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
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;

/**
 * Test using a NEWT {@link Window} for onscreen case.
 * <p>
 * Creates a {@link GLDrawable} using the
 * {@link GLDrawableFactory#createGLDrawable(com.jogamp.nativewindow.NativeSurface) factory model}.
 * The {@link GLContext} is derived {@link GLDrawable#createContext(GLContext) from the drawable}.
 * </p>
 * <p>
 * Finally a {@link GLAutoDrawableDelegate} is created with the just created {@link GLDrawable} and {@link GLContext}.
 * It is being used to run the {@link GLEventListener}.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLAutoDrawableDelegateNEWT extends UITestCase {
    static long duration = 500; // ms

    void doTest(final GLCapabilitiesImmutable reqGLCaps, final GLEventListener demo) throws InterruptedException {
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(reqGLCaps.getGLProfile());

        //
        // Create native windowing resources .. X11/Win/OSX
        //
        final Window window = NewtFactory.createWindow(reqGLCaps);
        Assert.assertNotNull(window);
        window.setSize(640, 400);
        window.setVisible(true);
        Assert.assertTrue(AWTRobotUtil.waitForVisible(window, true));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(window, true));
        System.out.println("Window: "+window.getClass().getName());

        final GLDrawable drawable = factory.createGLDrawable(window);
        Assert.assertNotNull(drawable);
        drawable.setRealized(true);

        final GLAutoDrawableDelegate glad = new GLAutoDrawableDelegate(drawable, null, window, false, null) {
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

        final QuitAdapter quitAdapter = new QuitAdapter();
        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        window.addKeyListener(quitAdapter);
        window.addWindowListener(quitAdapter);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(60, System.err);
        animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
        animator.add(glad);
        animator.start();
        Assert.assertTrue(animator.isStarted());
        Assert.assertTrue(animator.isAnimating());

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        System.out.println("Fin start ...");

        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());
        glad.destroy();
        System.out.println("Fin Drawable: "+drawable);
        System.out.println("Fin Window: "+window);
    }

    @Test
    public void testOnScreenDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = new GLCapabilities( GLProfile.getGL2ES2() );
        doTest(reqGLCaps, new GearsES2(1));
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestGLAutoDrawableDelegateNEWT.class.getName());
    }

}
