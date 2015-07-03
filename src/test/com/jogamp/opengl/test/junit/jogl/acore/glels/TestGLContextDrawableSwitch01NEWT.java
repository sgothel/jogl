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

package com.jogamp.opengl.test.junit.jogl.acore.glels;

import java.io.IOException;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

import com.jogamp.opengl.GLAutoDrawableDelegate;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLDrawableUtil;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test re-association (switching) of GLContext/GLDrawables,
 * i.e. ctx1/draw1, ctx2/draw2 -> ctx1/draw2, ctx2/draw1.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLContextDrawableSwitch01NEWT extends UITestCase {
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

    private GLAutoDrawable createGLAutoDrawable(final GLCapabilities caps, final int x, final int y, final int width, final int height, final WindowListener wl) throws InterruptedException {
        final Window window = NewtFactory.createWindow(caps);
        Assert.assertNotNull(window);
        window.setPosition(x, y);
        window.setSize(width, height);
        window.setVisible(true);
        Assert.assertTrue(AWTRobotUtil.waitForVisible(window, true));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(window, true));

        final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLDrawable drawable = factory.createGLDrawable(window);
        Assert.assertNotNull(drawable);

        drawable.setRealized(true);
        Assert.assertTrue(drawable.isRealized());

        final GLContext context = drawable.createContext(null);
        Assert.assertNotNull(context);

        final GLAutoDrawableDelegate glad = new GLAutoDrawableDelegate(drawable, context, window, false, null) {
            @Override
            protected void destroyImplInLock() {
                super.destroyImplInLock();
                window.destroy(); // destroys the actual window
            }
        };

        window.setWindowDestroyNotifyAction( new Runnable() {
            public void run() {
                glad.windowDestroyNotifyOp();
            } } );

        // add basic window interaction
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
        window.addWindowListener(wl);

        return glad;
    }

    @Test(timeout=30000)
    public void testSwitch2WindowSingleContextGL2ES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        testSwitch2WindowSingleContextImpl(reqGLCaps);
    }

    @Test(timeout=30000)
    public void testSwitch2WindowSingleContextGLES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        testSwitch2WindowSingleContextImpl(reqGLCaps);
    }

    private void testSwitch2WindowSingleContextImpl(final GLCapabilities caps) throws InterruptedException {
        final QuitAdapter quitAdapter = new QuitAdapter();

        final GLAutoDrawable glad1 = createGLAutoDrawable(caps,         64, 64,     width,     height, quitAdapter);
        final GLAutoDrawable glad2 = createGLAutoDrawable(caps, 2*64+width, 64, width+100, height+100, quitAdapter);

        // create single context using glad1 and assign it to glad1,
        // destroy the prev. context afterwards.
        {
            final GLContext newCtx = glad1.createContext(null);
            Assert.assertNotNull(newCtx);
            final GLContext oldCtx = glad1.setContext(newCtx, true);
            Assert.assertNotNull(oldCtx);
            Assert.assertFalse(oldCtx.isCreated());
            final int res = newCtx.makeCurrent();
            Assert.assertTrue(GLContext.CONTEXT_CURRENT_NEW==res || GLContext.CONTEXT_CURRENT==res);
            newCtx.release();
        }

        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        final GearsES2 gears = new GearsES2(1);
        glad1.addGLEventListener(gears);
        glad1.addGLEventListener(snapshotGLEventListener);
        snapshotGLEventListener.setMakeSnapshot();

        final Animator animator = new Animator();
        animator.add(glad1);
        animator.add(glad2);
        animator.start();

        int s = 0;
        final long t0 = System.currentTimeMillis();
        long t1 = t0;

        while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration ) {
            if( ( t1 - t0 ) / period > s) {
                s++;
                System.err.println(s+" - switch - START "+ ( t1 - t0 ));

                // switch context _and_ the demo synchronously
                GLDrawableUtil.swapGLContextAndAllGLEventListener(glad1, glad2);

                System.err.println(s+" - switch - END "+ ( t1 - t0 ));
            }
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        animator.stop();
        glad1.destroy();
        glad2.destroy();
    }

    @Test(timeout=30000)
    public void testSwitch2GLWindowOneDemoGL2ES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        testSwitch2GLWindowOneDemoImpl(reqGLCaps);
    }

    @Test(timeout=30000)
    public void testSwitch2GLWindowOneDemoGLES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        testSwitch2GLWindowOneDemoImpl(reqGLCaps);
    }

    private void testSwitch2GLWindowOneDemoImpl(final GLCapabilities caps) throws InterruptedException {
        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        final GearsES2 gears = new GearsES2(1);
        final QuitAdapter quitAdapter = new QuitAdapter();

        final GLWindow glWindow1 = GLWindow.create(caps);
        glWindow1.setTitle("win1");
        glWindow1.setSize(width, height);
        glWindow1.setPosition(64, 64);
        glWindow1.addGLEventListener(0, gears);
        glWindow1.addGLEventListener(snapshotGLEventListener);
        glWindow1.addWindowListener(quitAdapter);

        final GLWindow glWindow2 = GLWindow.create(caps);
        glWindow2.setTitle("win2");
        glWindow2.setSize(width+100, height+100);
        glWindow2.setPosition(2*64+width, 64);
        glWindow2.addWindowListener(quitAdapter);

        final Animator animator = new Animator();
        animator.add(glWindow1);
        animator.add(glWindow2);
        animator.start();

        glWindow1.setVisible(true);
        glWindow2.setVisible(true);

        snapshotGLEventListener.setMakeSnapshot();

        int s = 0;
        final long t0 = System.currentTimeMillis();
        long t1 = t0;

        while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration ) {
            if( ( t1 - t0 ) / period > s) {
                s++;
                System.err.println(s+" - switch - START "+ ( t1 - t0 ));
                System.err.println(s+" - A w1-h 0x"+Long.toHexString(glWindow1.getHandle())+",-ctx 0x"+Long.toHexString(glWindow1.getContext().getHandle()));
                System.err.println(s+" - A w2-h 0x"+Long.toHexString(glWindow2.getHandle())+",-ctx 0x"+Long.toHexString(glWindow2.getContext().getHandle()));

                // switch context _and_ the demo synchronously
                GLDrawableUtil.swapGLContextAndAllGLEventListener(glWindow1, glWindow2);

                System.err.println(s+" - B w1-h 0x"+Long.toHexString(glWindow1.getHandle())+",-ctx 0x"+Long.toHexString(glWindow1.getContext().getHandle()));
                System.err.println(s+" - B w2-h 0x"+Long.toHexString(glWindow2.getHandle())+",-ctx 0x"+Long.toHexString(glWindow2.getContext().getHandle()));
                System.err.println(s+" - switch - END "+ ( t1 - t0 ));

                snapshotGLEventListener.setMakeSnapshot();
            }
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        animator.stop();
        glWindow1.destroy();
        glWindow2.destroy();

    }

    @Test(timeout=30000)
    public void testSwitch2GLWindowEachWithOwnDemoGL2ES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        testSwitch2GLWindowEachWithOwnDemoImpl(reqGLCaps);
    }

    @Test(timeout=30000)
    public void testSwitch2GLWindowEachWithOwnDemoGLES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        testSwitch2GLWindowEachWithOwnDemoImpl(reqGLCaps);
    }

    public void testSwitch2GLWindowEachWithOwnDemoImpl(final GLCapabilities caps) throws InterruptedException {
        final GearsES2 gears = new GearsES2(1);
        final RedSquareES2 rsquare = new RedSquareES2(1);
        final QuitAdapter quitAdapter = new QuitAdapter();
        final SnapshotGLEventListener snapshotGLEventListener1 = new SnapshotGLEventListener();
        final SnapshotGLEventListener snapshotGLEventListener2 = new SnapshotGLEventListener();

        final GLWindow glWindow1 = GLWindow.create(caps);
        glWindow1.setTitle("win1");
        glWindow1.setSize(width, height);
        glWindow1.setPosition(64, 64);
        glWindow1.addGLEventListener(0, gears);
        glWindow1.addGLEventListener(snapshotGLEventListener1);
        glWindow1.addWindowListener(quitAdapter);

        final GLWindow glWindow2 = GLWindow.create(caps);
        glWindow2.setTitle("win2");
        glWindow2.setSize(width+100, height+100);
        glWindow2.setPosition(2*64+width, 64);
        glWindow2.addGLEventListener(0, rsquare);
        glWindow2.addGLEventListener(snapshotGLEventListener2);
        glWindow2.addWindowListener(quitAdapter);

        final Animator animator = new Animator();
        animator.add(glWindow1);
        animator.add(glWindow2);
        animator.start();

        glWindow1.setVisible(true);
        glWindow2.setVisible(true);

        snapshotGLEventListener1.setMakeSnapshot();
        snapshotGLEventListener2.setMakeSnapshot();

        int s = 0;
        final long t0 = System.currentTimeMillis();
        long t1 = t0;

        while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration ) {
            if( ( t1 - t0 ) / period > s) {
                s++;
                System.err.println(s+" - switch - START "+ ( t1 - t0 ));
                System.err.println(s+" - A w1-h 0x"+Long.toHexString(glWindow1.getHandle())+",-ctx 0x"+Long.toHexString(glWindow1.getContext().getHandle()));
                System.err.println(s+" - A w2-h 0x"+Long.toHexString(glWindow2.getHandle())+",-ctx 0x"+Long.toHexString(glWindow2.getContext().getHandle()));
                GLDrawableUtil.swapGLContextAndAllGLEventListener(glWindow1, glWindow2);
                System.err.println(s+" - B w1-h 0x"+Long.toHexString(glWindow1.getHandle())+",-ctx 0x"+Long.toHexString(glWindow1.getContext().getHandle()));
                System.err.println(s+" - B w2-h 0x"+Long.toHexString(glWindow2.getHandle())+",-ctx 0x"+Long.toHexString(glWindow2.getContext().getHandle()));
                System.err.println(s+" - switch - END "+ ( t1 - t0 ));
                snapshotGLEventListener1.setMakeSnapshot();
                snapshotGLEventListener2.setMakeSnapshot();
            }
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        animator.stop();
        // System.err.println("pre -del-w1: w1: "+glWindow1);
        // System.err.println("pre -del-w1: w2: "+glWindow2);
        glWindow1.destroy();
        // System.err.println("post-del-w1: w1: "+glWindow1);
        // System.err.println("post-del-w1: w2: "+glWindow2);
        glWindow2.destroy();

    }

    // default timing for 2 switches
    static long duration = 2200; // ms
    static long period = 1000; // ms

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-period")) {
                i++;
                try {
                    period = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */
        org.junit.runner.JUnitCore.main(TestGLContextDrawableSwitch01NEWT.class.getName());
    }
}
