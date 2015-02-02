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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLException01NEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;

    @SuppressWarnings("serial")
    static class AnimException extends RuntimeException {
        final Thread thread;
        final GLAnimatorControl animator;
        final GLAutoDrawable drawable;
        public AnimException(final Thread thread, final GLAnimatorControl animator, final GLAutoDrawable drawable, final Throwable cause) {
            super(cause);
            this.thread = thread;
            this.animator = animator;
            this.drawable = drawable;
        }
    }

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
                             final boolean throwInReshape, final boolean throwInInvoke,
                             final boolean throwInDispose) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle(getTestMethodName());
        final GearsES2 demo1 = new GearsES2();
        demo1.setVerbose(false);
        glWindow.addGLEventListener(demo1);
        final AtomicInteger cleanInitCount = new AtomicInteger();
        final AtomicInteger cleanDisposeCount = new AtomicInteger();
        final AtomicInteger cleanDisplayCount = new AtomicInteger();
        final AtomicInteger cleanReshapeCount = new AtomicInteger();
        final AtomicInteger cleanInvokeCount = new AtomicInteger();
        final AtomicInteger allInitCount = new AtomicInteger();
        final AtomicInteger allDisposeCount = new AtomicInteger();
        final AtomicInteger allDisplayCount = new AtomicInteger();
        final AtomicInteger allReshapeCount = new AtomicInteger();
        final AtomicInteger allInvokeCount = new AtomicInteger();
        final AtomicInteger exceptionSent = new AtomicInteger();

        glWindow.addGLEventListener(new GLEventListener() {
            @Override
            public void init(final GLAutoDrawable drawable) {
                if( throwInInit ) {
                    exceptionSent.incrementAndGet();
                    throw new RuntimeException("<Injected GLEventListener exception in init: #"+exceptionSent.get()+" on thread "+Thread.currentThread().getName()+">");
                }
            }
            @Override
            public void dispose(final GLAutoDrawable drawable) {
                if( throwInDispose ) {
                    exceptionSent.incrementAndGet();
                    throw new RuntimeException("<Injected GLEventListener exception in dispose: #"+exceptionSent.get()+" on thread "+Thread.currentThread().getName()+">");
                }
            }
            @Override
            public void display(final GLAutoDrawable drawable) {
                if( throwInDisplay ) {
                    exceptionSent.incrementAndGet();
                    throw new RuntimeException("<Injected GLEventListener exception in display: #"+exceptionSent.get()+" on thread "+Thread.currentThread().getName()+">");
                }
            }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                if( throwInReshape ) {
                    exceptionSent.incrementAndGet();
                    throw new RuntimeException("<Injected GLEventListener exception in reshape: #"+exceptionSent.get()+" on thread "+Thread.currentThread().getName()+">");
                }
            }
        });
        final GLRunnable glRunnableInject = new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable drawable) {
                if( throwInInvoke ) {
                    exceptionSent.incrementAndGet();
                    throw new RuntimeException("<Injected GLEventListener exception in invoke: #"+exceptionSent.get()+" on thread "+Thread.currentThread().getName()+">");
                }
                return true;
            }
        };
        final GLRunnable glRunnableCount = new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable drawable) {
                if( 0 == exceptionSent.get() ) {
                    cleanInvokeCount.incrementAndGet();
                }
                allInvokeCount.incrementAndGet();
                return true;
            }
        };

        glWindow.addGLEventListener(new GLEventListener() {
            @Override
            public void init(final GLAutoDrawable drawable) {
                if( 0 == exceptionSent.get() ) {
                    cleanInitCount.incrementAndGet();
                }
                allInitCount.incrementAndGet();
            }
            @Override
            public void dispose(final GLAutoDrawable drawable) {
                if( 0 == exceptionSent.get() ) {
                    cleanDisposeCount.incrementAndGet();
                }
                allDisposeCount.incrementAndGet();
            }
            @Override
            public void display(final GLAutoDrawable drawable) {
                if( 0 == exceptionSent.get() ) {
                    cleanDisplayCount.incrementAndGet();
                }
                allDisplayCount.incrementAndGet();
            }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                if( 0 == exceptionSent.get() ) {
                    cleanReshapeCount.incrementAndGet();
                }
                allReshapeCount.incrementAndGet();
            }
        });

        RuntimeException exceptionAtInitReshapeDisplay = null;
        RuntimeException exceptionAtInvoke = null;
        RuntimeException exceptionAtDispose = null;
        final List<AnimException> exceptionsAtGLAnimatorControl = new ArrayList<AnimException>();
        final GLAnimatorControl.UncaughtExceptionHandler uncaughtExceptionHandler;

        final Animator animator;
        if( onThread ) {
            animator = null;
            uncaughtExceptionHandler = null;
        } else {
            animator = new Animator(glWindow);
            uncaughtExceptionHandler = new GLAnimatorControl.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(final GLAnimatorControl animator, final GLAutoDrawable drawable, final Throwable cause) {
                    final AnimException ae = new AnimException(animator.getThread(), animator, drawable, cause);
                    exceptionsAtGLAnimatorControl.add(ae);
                    dumpThrowable(ae);
                } };
            animator.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        }

        glWindow.setSize(width, height);

        if( !onThread ) {
            animator.setUpdateFPSFrames(1, null);
            animator.start();
        }
        try {
            glWindow.setVisible(true);
        } catch (final RuntimeException re) {
            exceptionAtInitReshapeDisplay = re;
            dumpThrowable(re);
        }

        try {
            glWindow.invoke(true, glRunnableInject);
            glWindow.invoke(true, glRunnableCount);
        } catch (final RuntimeException re) {
            exceptionAtInvoke = re;
            dumpThrowable(re);
        }

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(0 == exceptionSent.get() && ( onThread || animator.isAnimating() ) && t1-t0<duration ) {
            if( onThread ) {
                try {
                    glWindow.display();
                } catch (final RuntimeException re) {
                    exceptionAtInitReshapeDisplay = re;
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
            exceptionAtDispose = re;
            dumpThrowable(re);
        }

        final boolean onAnimThread = !onThread && !throwInDispose; /** dispose happens on [AWT|NEWT] EDT, not on animator thread! */

        System.err.println("This-Thread                     : "+onThread);
        System.err.println("Anim-Thread                     : "+onAnimThread);
        System.err.println("ExceptionSent                   : "+exceptionSent.get());
        System.err.println("Exception @ Init/Reshape/Display: "+(null != exceptionAtInitReshapeDisplay));
        System.err.println("Exception @ Invoke              : "+(null != exceptionAtInvoke));
        System.err.println("Exception @ Dispose             : "+(null != exceptionAtDispose));
        System.err.println("Exception @ GLAnimatorControl   : "+exceptionsAtGLAnimatorControl.size());
        System.err.println("Init Count                      : "+cleanInitCount.get()+" / "+allInitCount.get());
        System.err.println("Reshape Count                   : "+cleanReshapeCount.get()+" / "+allReshapeCount.get());
        System.err.println("Display Count                   : "+cleanDisplayCount.get()+" / "+allDisplayCount.get());
        System.err.println("Invoke Count                    : "+cleanInvokeCount.get()+" / "+allInvokeCount.get());
        System.err.println("Dispose Count                   : "+cleanDisposeCount.get()+" / "+allDisposeCount.get());

        if( throwInInit || throwInReshape || throwInDisplay || throwInDispose || throwInInvoke ) {
            Assert.assertTrue("Not one exception sent, but "+exceptionSent.get(), 0 < exceptionSent.get());
            if( onAnimThread ) {
                Assert.assertEquals("No exception forwarded from init to animator-handler", 1, exceptionsAtGLAnimatorControl.size());
                Assert.assertNull("Exception forwarded from init, on-thread", exceptionAtInitReshapeDisplay);
            }
            if( throwInInit ) {
                if( !onAnimThread ) {
                    Assert.assertNotNull("No exception forwarded from init, on-thread", exceptionAtInitReshapeDisplay);
                    Assert.assertEquals("Exception forwarded from init to animator-handler", 0, exceptionsAtGLAnimatorControl.size());
                }
                Assert.assertEquals("Init Count", 0, cleanInitCount.get());
                Assert.assertEquals("Reshape Count", 0, cleanReshapeCount.get());
                Assert.assertEquals("Display Count", 0, cleanDisplayCount.get());
                Assert.assertEquals("Invoke Count", 0, cleanInvokeCount.get());
                Assert.assertEquals("Dispose Count", 0, cleanDisposeCount.get());
            } else if( throwInReshape ) {
                if( !onAnimThread ) {
                    Assert.assertNotNull("No exception forwarded from reshape, on-thread", exceptionAtInitReshapeDisplay);
                    Assert.assertEquals("Exception forwarded from init to animator-handler", 0, exceptionsAtGLAnimatorControl.size());
                }
                Assert.assertEquals("Init Count", 1, cleanInitCount.get());
                Assert.assertEquals("Reshape Count", 0, cleanReshapeCount.get());
                Assert.assertEquals("Display Count", 0, cleanDisplayCount.get());
                Assert.assertEquals("Invoke Count", 0, cleanInvokeCount.get());
                Assert.assertEquals("Dispose Count", 0, cleanDisposeCount.get());
            } else if( throwInDisplay ) {
                if( !onAnimThread ) {
                    Assert.assertNotNull("No exception forwarded from display, on-thread", exceptionAtInitReshapeDisplay);
                    Assert.assertEquals("Exception forwarded from init to animator-handler", 0, exceptionsAtGLAnimatorControl.size());
                }
                Assert.assertEquals("Init Count", 1, cleanInitCount.get());
                Assert.assertEquals("Reshape Count", 1, cleanReshapeCount.get());
                Assert.assertEquals("Display Count", 0, cleanDisplayCount.get());
                Assert.assertEquals("Invoke Count", 0, cleanInvokeCount.get());
                Assert.assertEquals("Dispose Count", 0, cleanDisposeCount.get());
            } else if( throwInInvoke ) {
                if( !onAnimThread ) {
                    Assert.assertNotNull("No exception forwarded from invoke, on-thread", exceptionAtInvoke);
                    Assert.assertEquals("Exception forwarded from init to animator-handler", 0, exceptionsAtGLAnimatorControl.size());
                }
                Assert.assertEquals("Init Count", 1, cleanInitCount.get());
                Assert.assertEquals("Reshape Count", 1, cleanReshapeCount.get());
                Assert.assertTrue  ("Display count not greater-equal 1, but "+cleanDisplayCount.get(), 1 <= cleanDisplayCount.get());
                Assert.assertEquals("Invoke Count", 0, cleanInvokeCount.get());
                Assert.assertEquals("Dispose Count", 0, cleanDisposeCount.get());
            } else if( throwInDispose ) {
                if( !onAnimThread ) {
                    Assert.assertNotNull("No exception forwarded from dispose, on-thread", exceptionAtDispose);
                    Assert.assertEquals("Exception forwarded from init to animator-handler", 0, exceptionsAtGLAnimatorControl.size());
                }
                Assert.assertEquals("Init Count", 1, cleanInitCount.get());
                Assert.assertEquals("Reshape Count", 1, cleanReshapeCount.get());
                Assert.assertTrue  ("Display count not greater-equal 1, but "+cleanDisplayCount.get(), 1 <= cleanDisplayCount.get());
                Assert.assertEquals("Invoke Count", 1, cleanInvokeCount.get());
                Assert.assertEquals("Dispose Count", 0, cleanDisposeCount.get());
            }
        }
    }

    @Test
    public void test01OnThreadAtInit() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, true /* onThread */, true /* init */, false /* display */, false /* reshape */, false /* invoke */, false /* dispose */);
    }
    @Test
    public void test02OnThreadAtReshape() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, true /* onThread */, false /* init */, false /* display */, true /* reshape */, false /* invoke */, false /* dispose */);
    }
    @Test
    public void test03OnThreadAtDisplay() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, true /* onThread */, false /* init */, true /* display */, false /* reshape */, false /* invoke */, false /* dispose */);
    }
    @Test
    public void test04OnThreadAtInvoke() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, true /* onThread */, false /* init */, true /* display */, false /* reshape */, true /* invoke */, false /* dispose */);
    }
    @Test
    public void test05OnThreadAtDispose() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, true /* onThread */, false /* init */, false /* display */, false /* reshape */, false /* invoke */, true /* dispose */);
    }

    @Test
    public void test11OffThreadAtInit() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, false /* onThread */, true /* init */, false /* display */, false /* reshape */, false /* invoke */, false /* dispose */);
    }
    @Test
    public void test12OffThreadAtReshape() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, false /* onThread */, false /* init */, false /* display */, true /* reshape */, false /* invoke */, false /* dispose */);
    }
    @Test
    public void test13OffThreadAtDisplay() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, false /* onThread */, false /* init */, true /* display */, false /* reshape */, false /* invoke */, false /* dispose */);
    }
    @Test
    public void test14OffThreadAtInvoke() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, false /* onThread */, false /* init */, true /* display */, false /* reshape */, true /* invoke */, false /* dispose */);
    }
    @Test
    public void test15OffThreadAtDispose() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, false /* onThread */, false /* init */, false /* display */, false /* reshape */, false /* invoke */, true /* dispose */);
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
