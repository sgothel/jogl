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

package com.jogamp.opengl.test.junit.jogl.acore.ect;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.opengl.test.junit.util.GLTestUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

import com.jogamp.opengl.util.AnimatorBase;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.common.os.Platform;
import com.jogamp.nativewindow.Capabilities;
import com.jogamp.nativewindow.util.InsetsImmutable;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * ExclusiveContextThread base implementation to test performance impact of the ExclusiveContext feature with AnimatorBase.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class ExclusiveContextBase10 extends UITestCase {
    static boolean testExclusiveWithAWT = false;
    static long duration = 1400;

    static boolean showFPS = false;
    static int showFPSRate = 60;

    static final int demoWinSize = 128;

    static InsetsImmutable insets = null;
    static int num_x, num_y;

    static int swapInterval = 0;

    @BeforeClass
    public static void initClass00() {
        final Window dummyWindow = NewtFactory.createWindow(new Capabilities());
        dummyWindow.setSize(demoWinSize, demoWinSize);
        dummyWindow.setVisible(true);
        Assert.assertEquals(true, dummyWindow.isVisible());
        Assert.assertEquals(true, dummyWindow.isNativeValid());
        insets = dummyWindow.getInsets();
        final int scrnHeight = dummyWindow.getScreen().getHeight();
        final int scrnWidth = dummyWindow.getScreen().getWidth();
        final int[] demoScreenSize = dummyWindow.convertToPixelUnits(new int[] { demoWinSize, demoWinSize });
        final int[] insetsScreenSize = dummyWindow.convertToPixelUnits(new int[] { insets.getTotalWidth(), insets.getTotalHeight() });
        num_x = scrnWidth  / ( demoScreenSize[0] + insetsScreenSize[0] ) - 2;
        num_y = scrnHeight / ( demoScreenSize[1] + insetsScreenSize[1] ) - 2;
        dummyWindow.destroy();
    }

    @AfterClass
    public static void releaseClass00() {
    }

    protected abstract boolean isAWTTestCase();
    protected abstract Thread getAWTRenderThread();
    protected abstract AnimatorBase createAnimator();
    protected abstract GLAutoDrawable createGLAutoDrawable(String title, int x, int y, int width, int height, GLCapabilitiesImmutable caps);
    protected abstract void setGLAutoDrawableVisible(GLAutoDrawable[] glads);
    protected abstract void destroyGLAutoDrawableVisible(GLAutoDrawable glad);

    /**
     * @deprecated Use {@link #runTestGL(GLCapabilitiesImmutable,int,boolean,boolean)} instead
     */
    protected void runTestGL(final GLCapabilitiesImmutable caps, final int drawableCount, final boolean exclusive) throws InterruptedException {
        runTestGL(caps, drawableCount, exclusive, false);
    }

    protected void runTestGL(final GLCapabilitiesImmutable caps, final int drawableCount, final boolean exclusive, final boolean preVisible) throws InterruptedException {
        final boolean useAWTRenderThread = isAWTTestCase();
        if( useAWTRenderThread && exclusive ) {
            if( testExclusiveWithAWT ) {
                System.err.println("Warning: Testing AWT + Exclusive -> Not advised!");
            } else {
                System.err.println("Info: Skip test: AWT + Exclusive!");
                return;
            }
        }
        if( useAWTRenderThread && exclusive && !testExclusiveWithAWT) {
            System.err.println("Skip test: AWT + Exclusive -> Not advised!");
            return;
        }
        final Thread awtRenderThread = getAWTRenderThread();
        final AnimatorBase[] animators = new AnimatorBase[drawableCount];
        final GLAutoDrawable[] drawables = new GLAutoDrawable[drawableCount];
        for(int i=0; i<drawableCount; i++) {
            animators[i] = createAnimator();
            if( !useAWTRenderThread ) {
                animators[i].setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
            }

            final int x = (  i          % num_x ) * ( demoWinSize + insets.getTotalHeight() ) + insets.getLeftWidth();
            final int y = ( (i / num_x) % num_y ) * ( demoWinSize + insets.getTotalHeight() ) + insets.getTopHeight();
            drawables[i] = createGLAutoDrawable("Win #"+i, x, y, demoWinSize, demoWinSize, caps);
            Assert.assertNotNull(drawables[i]);
            final GearsES2 demo = new GearsES2(swapInterval);
            demo.setVerbose(false);
            drawables[i].addGLEventListener(demo);
        }

        if( preVisible ) {
            setGLAutoDrawableVisible(drawables);

            // Made visible, check if drawables are realized
            for(int i=0; i<drawableCount; i++) {
                Assert.assertEquals(true, drawables[i].isRealized());
                animators[i].setUpdateFPSFrames(showFPSRate, showFPS ? System.err : null);
            }
        }

        for(int i=0; i<drawableCount; i++) {
            animators[i].add(drawables[i]);

            if( exclusive ) {
                if( useAWTRenderThread ) {
                    Assert.assertEquals(null, animators[i].setExclusiveContext(awtRenderThread));
                } else {
                    Assert.assertEquals(false, animators[i].setExclusiveContext(true));
                }
            }
            Assert.assertFalse(animators[i].isAnimating());
            Assert.assertFalse(animators[i].isStarted());
        }

        // Animator Start
        for(int i=0; i<drawableCount; i++) {
            Assert.assertTrue(animators[i].start());

            Assert.assertTrue(animators[i].isStarted());
            Assert.assertTrue(animators[i].isAnimating());
            Assert.assertEquals(exclusive, animators[i].isExclusiveContextEnabled());
        }

        // After start, ExclusiveContextThread is set
        for(int i=0; i<drawableCount; i++) {
            final Thread ect = animators[i].getExclusiveContextThread();
            if(exclusive) {
                if( useAWTRenderThread ) {
                    Assert.assertEquals(awtRenderThread, ect);
                } else {
                    Assert.assertEquals(animators[i].getThread(), ect);
                }
            } else {
                Assert.assertEquals(null, ect);
            }
            Assert.assertEquals(ect, drawables[i].getExclusiveContextThread());
        }

        if( !preVisible ) {
            setGLAutoDrawableVisible(drawables);

            // Made visible, check if drawables are realized
            for(int i=0; i<drawableCount; i++) {
                Assert.assertEquals(true, drawables[i].isRealized());
                animators[i].setUpdateFPSFrames(showFPSRate, showFPS ? System.err : null);
            }
        }

        // Normal run ..
        Thread.sleep(duration);

        // Animator Stop #2
        for(int i=0; i<drawableCount; i++) {
            Assert.assertTrue(animators[i].stop());
            Assert.assertFalse(animators[i].isAnimating());
            Assert.assertFalse(animators[i].isStarted());
            Assert.assertFalse(animators[i].isPaused());
            Assert.assertEquals(exclusive, animators[i].isExclusiveContextEnabled());
            Assert.assertEquals(null, animators[i].getExclusiveContextThread());
        }

        // Destroy GLWindows
        for(int i=0; i<drawableCount; i++) {
            destroyGLAutoDrawableVisible(drawables[i]);
            Assert.assertEquals(true,  GLTestUtil.waitForRealized(drawables[i], false, null));
        }
    }

    @Test
    public void test01Normal_1WinPostVis() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 1 /* numWin */, false /* exclusive */, false /* preVisible */);
    }

    @Test
    public void test03Excl_1WinPostVis() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 1 /* numWin */, true /* exclusive */, false /* preVisible */);
    }

    @Test
    public void test05Normal_4WinPostVis() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 4 /* numWin */, false /* exclusive */, false /* preVisible */);
    }

    @Test
    public void test07Excl_4WinPostVis() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 4 /* numWin */, true /* exclusive */, false /* preVisible */);
    }

    @Test
    public void test11Normal_1WinPreVis() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 1 /* numWin */, false /* exclusive */, true /* preVisible */);
    }

    @Test
    public void test13Excl_1WinPreVis() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 1 /* numWin */, true /* exclusive */, true /* preVisible */);
    }

    @Test
    public void test15Normal_4WinPreVis() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 4 /* numWin */, false /* exclusive */, true /* preVisible */);
    }

    @Test
    public void test17Excl_4WinPreVis() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 4 /* numWin */, true /* exclusive */, true /* preVisible */);
    }

}
