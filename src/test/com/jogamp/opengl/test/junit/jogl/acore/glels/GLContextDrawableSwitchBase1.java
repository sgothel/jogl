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

package com.jogamp.opengl.test.junit.jogl.acore.glels;

import java.awt.Dimension;
import java.awt.Frame;

import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import jogamp.nativewindow.jawt.JAWTUtil;

import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.GLEventListenerState;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.GLEventListenerCounter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;

/**
 * Test re-association of GLContext/GLDrawables,
 * here GLContext's survival of GLDrawable destruction
 * and reuse w/ new or recreated GLDrawable.
 * <p>
 * Test utilizes {@link GLEventListenerState} for preserving the
 * GLAutoDrawable state, i.e. GLContext, all GLEventListener
 * and the GLAnimatorControl association.
 * </p>
 * <p>
 * See Bug 665 - https://jogamp.org/bugzilla/show_bug.cgi?id=665.
 * </p>
 */
public abstract class GLContextDrawableSwitchBase1 extends UITestCase {
    static protected enum GLADType { GLCanvasOnscreen, GLCanvasOffscreen, GLWindow, GLOffscreen };

    // default period for 1 GLAD cycle
    static long duration = 1000; // ms

    static int width, height;

    static GLCapabilities getCaps(final String profile) {
        if( !GLProfile.isAvailable(profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return null;
        }
        return new GLCapabilities(GLProfile.get(profile));
    }

    @BeforeClass
    public static void initClass() {
        width  = 256;
        height = 256;
    }

    static void setGLCanvasSize(final GLCanvas glc, final Dimension new_sz) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    glc.setMinimumSize(new_sz);
                    glc.setPreferredSize(new_sz);
                    glc.setSize(new_sz);
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    static void setFrameVisible(final Frame frame) throws InterruptedException {
        try {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.pack();
                frame.setVisible(true);
            }});
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    static void destroyFrame(final Frame frame) throws InterruptedException {
        try {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            }});
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    private GLOffscreenAutoDrawable createGLOffscreenAutoDrawable(final GLCapabilities caps, final int width, final int height) throws InterruptedException {
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        return factory.createOffscreenAutoDrawable(null, caps, null, width, height);
    }

    protected static boolean validateOnOffscreenLayer(final GLADType gladType1, final GLADType gladType2) {
        final boolean useOffscreenLayer = GLADType.GLCanvasOffscreen == gladType1 || GLADType.GLCanvasOffscreen == gladType2 ;
        final boolean useOnscreenLayer = GLADType.GLCanvasOnscreen == gladType1 || GLADType.GLCanvasOnscreen == gladType2 ;
        if( useOffscreenLayer ) {
            if( !JAWTUtil.isOffscreenLayerSupported() ) {
                System.err.println("Platform doesn't support offscreen rendering.");
                return false;
            }
        } else if( useOnscreenLayer ) {
            if( JAWTUtil.isOffscreenLayerRequired() ) {
                System.err.println("Platform requires offscreen rendering.");
                return false;
            }
        }
        return true;
    }

    protected void testGLADOneLifecycle(final Screen screen, final GLCapabilities caps, final GLADType gladType, final int width,
                                        final int height, final GLEventListenerCounter glelTracker,
                                        final SnapshotGLEventListener snapshotGLEventListener, final GLEventListenerState glelsIn, final GLEventListenerState glelsOut[], final GLAnimatorControl animator)
            throws InterruptedException {

        System.err.println("GLAD Lifecycle.0 "+gladType+", restoring "+((null!=glelsIn)?true:false)+", preserving "+((null!=glelsOut)?true:false));
        final Frame frame;
        final GLAutoDrawable glad;
        if( GLADType.GLCanvasOnscreen == gladType ) {
            if( jogamp.nativewindow.jawt.JAWTUtil.isOffscreenLayerRequired() ) {
                throw new InternalError("Platform requires offscreen rendering, but onscreen requested: "+gladType);
            }
            frame = new Frame("AWT GLCanvas");

            glad = new GLCanvas(caps);
            setGLCanvasSize((GLCanvas)glad, new Dimension(width, height));
            frame.add((GLCanvas)glad);
        } else if( GLADType.GLCanvasOffscreen == gladType ) {
            if( !jogamp.nativewindow.jawt.JAWTUtil.isOffscreenLayerSupported() ) {
                throw new InternalError("Platform doesn't support offscreen rendering: "+gladType);
            }
            frame = new Frame("AWT GLCanvas");

            glad = new GLCanvas(caps);
            ((GLCanvas)glad).setShallUseOffscreenLayer(true);
            setGLCanvasSize((GLCanvas)glad, new Dimension(width, height));
            frame.add((GLCanvas)glad);
        } else if( GLADType.GLWindow == gladType ) {
            frame = null;

            if( null != screen ) {
                glad = GLWindow.create(screen, caps);
            } else {
                glad = GLWindow.create(caps);
            }
            ((GLWindow)glad).setTitle("Newt GLWindow");
            ((GLWindow)glad).setSize(width, height);
        } else if( GLADType.GLOffscreen == gladType ) {
            frame = null;

            glad = this.createGLOffscreenAutoDrawable(caps, width, height);
        } else {
            throw new InternalError("Unsupported: "+gladType);
        }

        if( null == glelsIn ) {
            if( null != animator ) {
                animator.add(glad);
            }
            glad.addGLEventListener(glelTracker);
            glad.addGLEventListener(new GearsES2(1));
            glad.addGLEventListener(snapshotGLEventListener);
        }
        snapshotGLEventListener.setMakeSnapshot();

        if( GLADType.GLCanvasOnscreen == gladType || GLADType.GLCanvasOffscreen == gladType ) {
            setFrameVisible(frame);
            Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        } else if( GLADType.GLWindow == gladType ) {
            ((GLWindow)glad).setVisible(true);
        }
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glad, true));
        Assert.assertNotNull(glad.getContext());
        Assert.assertTrue(glad.isRealized());

        if( null != glelsIn ) {
            Assert.assertEquals(0, glad.getGLEventListenerCount());
            System.err.println(".. restoring.0");
            glelsIn.moveTo(glad);
            System.err.println(".. restoring.X");

            Assert.assertEquals(1, glelTracker.initCount);
            Assert.assertTrue(1 <= glelTracker.reshapeCount);
            Assert.assertTrue(1 <= glelTracker.displayCount);
            Assert.assertEquals(0, glelTracker.disposeCount);
            Assert.assertEquals(3, glad.getGLEventListenerCount());

            Assert.assertEquals(glelsIn.context, glad.getContext());
            Assert.assertEquals(glelsIn.listenerCount(), glad.getGLEventListenerCount());
            Assert.assertEquals(glelsIn.context.getGLReadDrawable(), glad.getDelegatedDrawable());
            Assert.assertEquals(glelsIn.context.getGLDrawable(), glad.getDelegatedDrawable());
            Assert.assertEquals(false, glelsIn.isOwner());
        }

        for (int wait=0; wait<AWTRobotUtil.POLL_DIVIDER &&
                         ( 1 > glelTracker.initCount || 1 > glelTracker.reshapeCount || 1 > glelTracker.displayCount );
             wait++) {
            Thread.sleep(AWTRobotUtil.TIME_SLICE);
        }

        final long t0 = System.currentTimeMillis();
        long t1 = t0;

        while( ( t1 - t0 ) < duration ) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        Assert.assertEquals(1, glelTracker.initCount);
        Assert.assertTrue(1 <= glelTracker.reshapeCount);
        Assert.assertTrue(1 <= glelTracker.displayCount);
        Assert.assertEquals(0, glelTracker.disposeCount);

        if( null != glelsOut ) {
            final GLContext context1 = glad.getContext();
            System.err.println(".. preserving.0");
            glelsOut[0] = GLEventListenerState.moveFrom(glad);
            System.err.println(".. preserving.X");

            Assert.assertEquals(context1, glelsOut[0].context);
            Assert.assertNull(context1.getGLReadDrawable());
            Assert.assertNull(context1.getGLDrawable());
            Assert.assertEquals(3, glelsOut[0].listenerCount());
            Assert.assertEquals(true, glelsOut[0].isOwner());
            Assert.assertEquals(null, glad.getContext());
            Assert.assertEquals(0, glad.getGLEventListenerCount());
        }
        if( GLADType.GLCanvasOnscreen == gladType || GLADType.GLCanvasOffscreen == gladType ) {
            destroyFrame(frame);
            Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, false));
        } else if( GLADType.GLWindow == gladType ) {
            glad.destroy();
        } else if( GLADType.GLOffscreen == gladType ) {
            glad.destroy();
        }
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glad, false));

        Assert.assertEquals(1, glelTracker.initCount);
        Assert.assertTrue(1 <= glelTracker.reshapeCount);
        Assert.assertTrue(1 <= glelTracker.displayCount);
        if( null != glelsOut ) {
            Assert.assertEquals(0, glelTracker.disposeCount);
        } else {
            Assert.assertEquals(1, glelTracker.disposeCount);
        }
        System.err.println("GLAD Lifecycle.X "+gladType);
    }
}
