/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.newt;

import java.io.IOException;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.SourcedInterruptedException;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.junit.util.SingletonJunitCase;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.EDTUtil;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.util.Animator;

/**
 * Unit test to identify Thread.interrupt() caller for DefaultEDTUtil.invokeImpl(..) wait interruption.
 * <ul>
 *   <li>resize</li>
 *   <li>create/destroy</li>
 * </ul>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug1211IRQ00NEWT extends SingletonJunitCase {
    static long durationTest00 = 1000; // ms
    static long durationTest01 = 1000; // ms
    static int width = 800;
    static int height = 600;

    static GLWindow createWindow(final GLCapabilitiesImmutable caps) {
        Assert.assertNotNull(caps);
        //
        // Create native windowing resources .. X11/Win/OSX
        //
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setSize(width, height);

        glWindow.setUpdateFPSFrames(1, null);

        final GearsES2 demo = new GearsES2();
        demo.setVerbose(false);
        glWindow.addGLEventListener(demo);

        return glWindow;
    }

    static void destroyWindow(final GLWindow glWindow) throws InterruptedException {
        if(null!=glWindow) {
            glWindow.destroy();
            Assert.assertEquals(false,glWindow.isNativeValid());
        }
    }

    static class MyThread extends InterruptSource.Thread implements Thread.UncaughtExceptionHandler {
        volatile boolean myThreadStarted = false;
        volatile boolean myThreadStopped = false;

        public MyThread(final Runnable target, final String name) {
            super(null, target, name);
            setUncaughtExceptionHandler(this);
        }

        public static void testInterrupted1() throws InterruptedException {
            if( java.lang.Thread.interrupted() ) {
                throw SourcedInterruptedException.wrap(
                        new InterruptedException(java.lang.Thread.currentThread().getName()+".testInterrupted -> TRUE (silent interruption)"));
            }
        }
        public synchronized void testInterrupted(final boolean ignore) throws InterruptedException {
            if( isInterrupted() ) {
                final boolean current;
                if( this == java.lang.Thread.currentThread() ) {
                    java.lang.Thread.interrupted(); // clear!
                    current = true;
                } else {
                    current = false;
                }
                final int counter = getInterruptCounter(false);
                final Throwable source = getInterruptSource(true);
                final InterruptedException e = new SourcedInterruptedException(
                                                getName()+".testInterrupted -> TRUE (current "+current+", counter "+counter+")",
                                                null, source);
                if( !ignore ) {
                    throw e;
                } else {
                    ExceptionUtils.dumpThrowable("Ignored", e);
                }
            }
        }

        @Override
        public void run() {
            myThreadStarted = true;
            try {
                super.run();
            } finally {
                myThreadStopped = true;
            }
        }

        @Override
        public void uncaughtException(final java.lang.Thread t, final Throwable e) {
            System.err.println("UncaughtException on Thread "+t.getName()+": "+e.getMessage());
            ExceptionUtils.dumpThrowable("UncaughtException", e);
        }
    }


    volatile boolean interrupt1 = false;
    volatile boolean interrupt2 = false;
    volatile boolean interruptInit0 = false;

    public void initTest() {
        interrupt1 = false;
        interrupt2 = false;
    }

    /**
     * Test whether resize triggers DefaultEDTUtil.invokeImpl(..) wait interruption.
     */
    public void subTest00() {
        final MyThread t = (MyThread)Thread.currentThread();
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        Assert.assertNotNull(caps);
        final GLWindow window1 = createWindow(caps); // local
        final EDTUtil edt = window1.getScreen().getDisplay().getEDTUtil();
        final Animator anim = new Animator(window1);
        try {
            window1.setVisible(true);
            Assert.assertEquals(true,window1.isVisible());
            Assert.assertEquals(true,window1.isNativeValid());
            anim.start();
            boolean ok = true;
            for(int i=0; ok && i*100<durationTest00; i++) {
                Thread.sleep(100);
                final int ow = window1.getWidth();
                final int oh = window1.getHeight();
                final int nw, nh;
                if( 0 == i % 2 ) {
                    nw = ow + 100;
                    nh = oh + 100;
                } else {
                    nw = ow - 100;
                    nh = oh - 100;
                }
                System.err.println("test00.resize["+i+"]: "+ow+"x"+oh+" -> "+nw+"x"+nh);
                window1.setSize(nw, nh);
                ok = 0==t.getInterruptCounter(false) && !t.isInterrupted() && edt.isRunning() && anim.isAnimating();
                t.testInterrupted(false);
            }
        } catch (final InterruptedException e) {
            ExceptionUtils.dumpThrowable("InterruptedException-1", e);
            interrupt1 = true;
        }
        try {
            anim.stop();
            destroyWindow(window1);
            t.testInterrupted(false);
        } catch (final InterruptedException e) {
            ExceptionUtils.dumpThrowable("InterruptedException-2", e);
            interrupt2 = true;
        }
        Assert.assertEquals("interruptCounter not zero", 0, t.getInterruptCounter(false));
        Assert.assertFalse("interrupt() occured!", t.isInterrupted());
        Assert.assertFalse("Interrupt-1 occured!", interrupt1);
        Assert.assertFalse("Interrupt-2 occured!", interrupt2);
    }

    /**
     * Test whether create/destroy triggers DefaultEDTUtil.invokeImpl(..) wait interruption.
     */
    public void subTest01() {
        final MyThread t = (MyThread)Thread.currentThread();
        GLWindow lastWindow = null;
        try {
            final boolean ok = true;
            for(int i=0; ok && i*100<durationTest01; i++) {
                final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
                Assert.assertNotNull(caps);
                final GLWindow window1 = createWindow(caps); // local
                lastWindow = window1;
                window1.setVisible(true);
                Assert.assertEquals(true,window1.isVisible());
                Assert.assertEquals(true,window1.isNativeValid());
                System.err.println("test01.create["+i+"]: "+window1.getStateMaskString()+", "+window1.getWidth()+"x"+window1.getHeight());
                final Animator anim = new Animator(window1);
                anim.start();
                Thread.sleep(100);
                anim.stop();
                destroyWindow(window1);
                t.testInterrupted(false);
            }
        } catch (final InterruptedException e) {
            ExceptionUtils.dumpThrowable("InterruptedException-1", e);
            interrupt1 = true;
        }
        try {
            destroyWindow(lastWindow);
            t.testInterrupted(false);
        } catch (final InterruptedException e) {
            ExceptionUtils.dumpThrowable("InterruptedException-2", e);
            interrupt2 = true;
        }
        Assert.assertEquals("interruptCounter not zero", 0, t.getInterruptCounter(false));
        Assert.assertFalse("interrupt() occured!", t.isInterrupted());
        Assert.assertFalse("Interrupt-1 occured!", interrupt1);
        Assert.assertFalse("Interrupt-2 occured!", interrupt2);
    }

    @Test
    public void testAll() {
        interruptInit0 = false;
        final MyThread t = new MyThread(new Runnable() {
            public void run() {
                final MyThread t = (MyThread)Thread.currentThread();
                TestBug1211IRQ00NEWT test = null;
                try {
                    System.err.println(VersionUtil.getPlatformInfo());
                    GLProfile.initSingleton();
                    test = new TestBug1211IRQ00NEWT();
                    t.testInterrupted(false);
                } catch (final InterruptedException e) {
                    ExceptionUtils.dumpThrowable("InterruptedException-Init0", e);
                    interruptInit0 = true;
                    test = null;
                }
                t.clearInterruptSource();
                if( null != test ) {
                    test.initTest();
                    test.subTest00();

                    test.initTest();
                    test.subTest01();
                }
            }
        }, "MyMainThread");
        t.start();
        boolean interrupted = false;
        try {
            MyThread.testInterrupted1();
            while( !t.myThreadStarted ) {
                Thread.yield();
                MyThread.testInterrupted1();
            }
            while( !t.myThreadStopped ) {
                Thread.yield();
                MyThread.testInterrupted1();
            }
            MyThread.testInterrupted1();
        } catch (final InterruptedException e) {
            ExceptionUtils.dumpThrowable("InterruptedException-All", e);
            interrupted = true;
        }
        Assert.assertFalse("Thread Interrupt-All occured!", interrupted);
        Assert.assertFalse("Interrupt-Init0 occured!", interruptInit0);
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException {
        // We like to allow concurrent manual tests!
        SingletonJunitCase.enableSingletonLock(false);

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time00")) {
                durationTest00 = atoi(args[++i]);
            } else if(args[i].equals("-time01")) {
                durationTest01 = atoi(args[++i]);
            } else if(args[i].equals("-width")) {
                width = atoi(args[++i]);
            } else if(args[i].equals("-height")) {
                height = atoi(args[++i]);
            }
        }
        System.out.println("durationTest00: "+durationTest00);
        System.out.println("durationTest01: "+durationTest01);
        System.out.println("defaultSize   : "+width+"x"+height);
        final String tstname = TestBug1211IRQ00NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
