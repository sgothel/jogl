/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLException01NEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getGL2ES2();
        Assert.assertNotNull(glp);
        width  = 512;
        height = 512;
    }

    public static void dumpThrowable(final Throwable t) {
        System.err.println("User caught exception "+t.getClass().getSimpleName()+": "+t.getMessage()+" on thread "+Thread.currentThread().getName());
        t.printStackTrace();
    }

    protected void runTestGL(final GLCapabilities caps, final boolean onThread,
                             final boolean throwInInit, final boolean throwInDisplay,
                             final boolean throwInReshape, final boolean throwInDispose) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("NEWT Exception Test");
        final GearsES2 demo1 = new GearsES2();
        demo1.setVerbose(false);
        glWindow.addGLEventListener(demo1);
        final AtomicInteger initCount = new AtomicInteger();
        final AtomicInteger disposeCount = new AtomicInteger();
        final AtomicInteger displayCount = new AtomicInteger();
        final AtomicInteger reshapeCount = new AtomicInteger();
        final AtomicInteger exceptionSent = new AtomicInteger();

        glWindow.addGLEventListener(new GLEventListener() {
            @Override
            public void init(final GLAutoDrawable drawable) {
                if( throwInInit && 0 == exceptionSent.get() ) {
                    exceptionSent.incrementAndGet();
                    throw new RuntimeException("Injected GLEventListener exception in init");
                }
            }
            @Override
            public void dispose(final GLAutoDrawable drawable) {
                if( throwInDispose && 0 == exceptionSent.get() ) {
                    exceptionSent.incrementAndGet();
                    throw new RuntimeException("Injected GLEventListener exception in dispose");
                }
            }
            @Override
            public void display(final GLAutoDrawable drawable) {
                if( throwInDisplay && 0 == exceptionSent.get() ) {
                    exceptionSent.incrementAndGet();
                    throw new RuntimeException("Injected GLEventListener exception in display");
                }
            }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                if( throwInReshape && 0 == exceptionSent.get() ) {
                    exceptionSent.incrementAndGet();
                    throw new RuntimeException("Injected GLEventListener exception in reshape");
                }
            }
        });
        glWindow.addGLEventListener(new GLEventListener() {
            @Override
            public void init(final GLAutoDrawable drawable) {
                initCount.incrementAndGet();
            }
            @Override
            public void dispose(final GLAutoDrawable drawable) {
                disposeCount.incrementAndGet();
            }
            @Override
            public void display(final GLAutoDrawable drawable) {
                displayCount.incrementAndGet();
            }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                reshapeCount.incrementAndGet();
            }
        });

        RuntimeException execptionAtInitReshapeDisplay = null;
        RuntimeException execptionAtDispose = null;

        final Animator animator = !onThread ? new Animator(glWindow) : null;

        glWindow.setSize(width, height);

        if( !onThread ) {
            animator.setUpdateFPSFrames(1, null);
            animator.start();
        }
        try {
            glWindow.setVisible(true);
        } catch (final RuntimeException re) {
            execptionAtInitReshapeDisplay = re;
            dumpThrowable(re);
        }

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(0 == exceptionSent.get() && ( onThread || animator.isAnimating() ) && t1-t0<duration ) {
            if( onThread ) {
                try {
                    glWindow.display();
                } catch (final RuntimeException re) {
                    execptionAtInitReshapeDisplay = re;
                    dumpThrowable(re);
                }
            }
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        if( !onThread ) {
            animator.stop();
        }
        try {
            glWindow.destroy();
        } catch (final RuntimeException re) {
            execptionAtDispose = re;
            dumpThrowable(re);
        }

        if( throwInInit || throwInReshape || throwInDisplay || throwInDispose ) {
            Assert.assertEquals("Not one exception sent", 1, exceptionSent.get());
            if( throwInInit ) {
                Assert.assertNotNull("No exception forwarded from init", execptionAtInitReshapeDisplay);
            }
            if( throwInReshape ) {
                Assert.assertNotNull("No exception forwarded from reshape", execptionAtInitReshapeDisplay);
            }
            if( throwInDisplay ) {
                Assert.assertNotNull("No exception forwarded from display", execptionAtInitReshapeDisplay);
            }
            if( throwInDispose ) {
                Assert.assertNotNull("No exception forwarded from dispose", execptionAtDispose);
            }
        }
    }

    @Test
    public void test01OnThreadAtInit() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, true /* onThread */, true /* init */, false /* display */, false /* reshape */, false /* dispose */);
    }
    @Test
    public void test02OnThreadAtReshape() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, true /* onThread */, false /* init */, false /* display */, true /* reshape */, false /* dispose */);
    }
    @Test
    public void test03OnThreadAtDisplay() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, true /* onThread */, false /* init */, true /* display */, false /* reshape */, false /* dispose */);
    }
    @Test
    public void test04OnThreadAtDispose() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, true /* onThread */, false /* init */, false /* display */, false /* reshape */, true /* dispose */);
    }

    static long duration = 500; // ms

    public static void main(final String args[]) {
        boolean waitForKey = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            }
        }
        if( waitForKey ) {
            UITestCase.waitForKey("main");
        }
        org.junit.runner.JUnitCore.main(TestGLException01NEWT.class.getName());
    }
}
