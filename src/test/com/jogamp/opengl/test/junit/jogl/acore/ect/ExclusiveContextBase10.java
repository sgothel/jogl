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
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import javax.media.nativewindow.Capabilities;
import javax.media.nativewindow.util.InsetsImmutable;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLProfile;

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
    
    static boolean showFPS = true;
    static int showFPSRate = 60;
    
    static final int demoSize = 128;
    
    static InsetsImmutable insets = null;
    static int scrnHeight, scrnWidth;
    static int num_x, num_y;
    
    static int swapInterval = 0;
    
    @BeforeClass
    public static void initClass00() {
        Window dummyWindow = NewtFactory.createWindow(new Capabilities());
        dummyWindow.setSize(demoSize, demoSize);
        dummyWindow.setVisible(true);
        Assert.assertEquals(true, dummyWindow.isVisible());
        Assert.assertEquals(true, dummyWindow.isNativeValid());
        insets = dummyWindow.getInsets();        
        scrnHeight = dummyWindow.getScreen().getHeight();
        scrnWidth = dummyWindow.getScreen().getWidth();        
        num_x = scrnWidth  / ( demoSize + insets.getTotalWidth() )  - 2;
        num_y = scrnHeight / ( demoSize + insets.getTotalHeight() ) - 2;
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

    protected void runTestGL(GLCapabilitiesImmutable caps, int drawableCount, boolean exclusive) throws InterruptedException {
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
        final AnimatorBase animator = createAnimator();
        if( !useAWTRenderThread ) {
            animator.setModeBits(false, Animator.MODE_EXPECT_AWT_RENDERING_THREAD);
        }
        final GLAutoDrawable[] drawables = new GLAutoDrawable[drawableCount];
        for(int i=0; i<drawableCount; i++) {
            final int x = (  i          % num_x ) * ( demoSize + insets.getTotalHeight() ) + insets.getLeftWidth();
            final int y = ( (i / num_x) % num_y ) * ( demoSize + insets.getTotalHeight() ) + insets.getTopHeight();
            
            drawables[i] = createGLAutoDrawable("Win #"+i, x, y, demoSize, demoSize, caps);
            Assert.assertNotNull(drawables[i]);
            final GearsES2 demo = new GearsES2(swapInterval);
            demo.setVerbose(false);
            drawables[i].addGLEventListener(demo);
        }
        
        for(int i=0; i<drawableCount; i++) {
            animator.add(drawables[i]);
        }
        if( exclusive ) {
            if( useAWTRenderThread ) {
                Assert.assertEquals(null, animator.setExclusiveContext(awtRenderThread));
            } else {
                Assert.assertEquals(false, animator.setExclusiveContext(true));
            }
        }
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());
        
        // Animator Start
        Assert.assertTrue(animator.start());
        
        Assert.assertTrue(animator.isStarted());
        Assert.assertTrue(animator.isAnimating());
        Assert.assertEquals(exclusive, animator.isExclusiveContextEnabled());
        
        // After start, ExclusiveContextThread is set
        {
            final Thread ect = animator.getExclusiveContextThread();
            if(exclusive) {
                if( useAWTRenderThread ) {
                    Assert.assertEquals(awtRenderThread, ect);
                } else {
                    Assert.assertEquals(animator.getThread(), ect);
                }
            } else {
                Assert.assertEquals(null, ect);
            }   
            for(int i=0; i<drawableCount; i++) {
                Assert.assertEquals(ect, drawables[i].getExclusiveContextThread());
            }
            setGLAutoDrawableVisible(drawables);
        }
        animator.setUpdateFPSFrames(showFPSRate, showFPS ? System.err : null);
        
        // Normal run ..
        Thread.sleep(duration);
        
        // Animator Stop #2
        Assert.assertTrue(animator.stop());       
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());
        Assert.assertFalse(animator.isPaused());        
        Assert.assertEquals(exclusive, animator.isExclusiveContextEnabled());
        Assert.assertEquals(null, animator.getExclusiveContextThread());
        
        // Destroy GLWindows
        for(int i=0; i<drawableCount; i++) {
            destroyGLAutoDrawableVisible(drawables[i]);
            Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(drawables[i], false));
        }        
    }    
    
    @Test
    public void test01Normal_1Win() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 1 /* numWin */, false /* exclusive */);
    }

    @Test
    public void test03Excl_1Win() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 1 /* numWin */, true /* exclusive */);
    }
    
    @Test
    public void test05Normal_4Win() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 4 /* numWin */, false /* exclusive */);
    }
    
    @Test
    public void test07Excl_4Win() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, 4 /* numWin */, true /* exclusive */);
    }    
}
