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

import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.nativewindow.Capabilities;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.ValidateLockListener;
import com.jogamp.opengl.util.Animator;

/**
 * Concurrent and lock-free initialization and rendering using exclusive NEWT Display EDT instances, or
 * concurrent locked initialization and lock-free rendering using a shared NEWT Display EDT instances.
 * <p>
 * Rendering is always lock-free and independent of the EDT.
 * </p>
 * <p>
 * Each test is decorated w/ {@link GLProfile#shutdown()} to ensure that
 * implicit {@link GLProfile#initSingleton()} is also being tested.
 * </p>
 */
public abstract class InitConcurrentBaseNEWT extends UITestCase {

    static final int demoWinSize = 128;

    static long duration = 300; // ms

    static InsetsImmutable insets = null;
    static int num_x, num_y;

    @BeforeClass
    public static void initClass() {
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

    public static class JOGLTask implements Runnable {
        private final int id;
        private final Object postSync;
        private final boolean reuse;
        private boolean done = false;

        public JOGLTask(final Object postSync, final int id, final boolean reuse) {
            this.postSync = postSync;
            this.id = id;
            this.reuse = reuse;
        }
        public void run() {
            final int x = (  id          % num_x ) * ( demoWinSize + insets.getTotalHeight() );
            final int y = ( (id / num_x) % num_y ) * ( demoWinSize + insets.getTotalHeight() );

            System.err.println("JOGLTask "+id+": START: "+x+"/"+y+", reuse "+reuse+" - "+Thread.currentThread().getName());
            final Display display = NewtFactory.createDisplay(null, reuse);
            final Screen screen = NewtFactory.createScreen(display, 0);
            final GLWindow glWindow = GLWindow.create(screen, new GLCapabilities(GLProfile.getDefault()));
            Assert.assertNotNull(glWindow);
            glWindow.setTitle("Task "+id);
            glWindow.setPosition(x + insets.getLeftWidth(), y + insets.getTopHeight() );

            glWindow.addGLEventListener(new ValidateLockListener());
            glWindow.addGLEventListener(new GearsES2(0));

            final Animator animator = new Animator(glWindow);

            glWindow.setSize(demoWinSize, demoWinSize);
            glWindow.setVisible(true);
            animator.setUpdateFPSFrames(60, null);

            System.err.println("JOGLTask "+id+": INITIALIZED: "+", "+display+" - "+Thread.currentThread().getName());

            animator.start();
            Assert.assertEquals(true, animator.isAnimating());
            Assert.assertEquals(true, glWindow.isVisible());
            Assert.assertEquals(true, glWindow.isNativeValid());
            Assert.assertEquals(true, glWindow.isRealized());
            System.err.println("JOGLTask "+id+": RUNNING: "+Thread.currentThread().getName());

            while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }

            animator.stop();
            glWindow.destroy();

            System.err.println("JOGLTask "+id+": DONE/SYNC: "+Thread.currentThread().getName());
            synchronized (postSync) {
                done = true;
                System.err.println("JOGLTask "+id+": END: "+Thread.currentThread().getName());
                postSync.notifyAll();
            }
        }

        public boolean done() { return done; }
    }

    protected static boolean done(final JOGLTask[] tasks) {
        for(int i=tasks.length-1; i>=0; i--) {
            if(!tasks[i].done()) {
                return false;
            }
        }
        return true;
    }
    protected static String doneDump(final JOGLTask[] tasks) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i=0; i<tasks.length; i++) {
            if(i>0) {
                sb.append(", ");
            }
            sb.append(i).append(": ").append(tasks[i].done());
        }
        sb.append("]");
        return sb.toString();
    }

    protected static boolean isDead(final Thread[] threads) {
        for(int i=threads.length-1; i>=0; i--) {
            if(threads[i].isAlive()) {
                return false;
            }
        }
        return true;
    }
    protected static String isAliveDump(final Thread[] threads) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i=0; i<threads.length; i++) {
            if(i>0) {
                sb.append(", ");
            }
            sb.append(i).append(": ").append(threads[i].isAlive());
        }
        sb.append("]");
        return sb.toString();
    }

    protected void runJOGLTasks(final int num, final boolean reuse) throws InterruptedException {
        GLProfile.shutdown();
        System.err.println("InitConcurrentBaseNEWT "+num+" threads, reuse display: "+reuse);
        final String currentThreadName = Thread.currentThread().getName();
        final Object syncDone = new Object();
        final JOGLTask[] tasks = new JOGLTask[num];
        final InterruptSource.Thread[] threads = new InterruptSource.Thread[num];
        int i;
        for(i=0; i<num; i++) {
            tasks[i] = new JOGLTask(syncDone, i, reuse);
            threads[i] = new InterruptSource.Thread(null, tasks[i], currentThreadName+"-jt"+i);
        }
        final long t0 = System.currentTimeMillis();

        for(i=0; i<num; i++) {
            threads[i].start();
        }
        i=0;
        synchronized (syncDone) {
            while(!done(tasks)) {
                try {
                    syncDone.wait(500);
                } catch (final InterruptedException e) {
                    throw new InterruptedRuntimeException(e);
                }
                System.err.println(i+": "+doneDump(tasks));
                i++;
            }
        }
        final long t1 = System.currentTimeMillis();
        System.err.println("total: "+(t1-t0)/1000.0+"s");

        Assert.assertTrue("Tasks are incomplete. Complete: "+doneDump(tasks), done(tasks));
        i=0;
        while(i<30 && !isDead(threads)) {
            Thread.sleep(100);
            i++;
        }
        Assert.assertTrue("Threads are still alive after 3s. Alive: "+isAliveDump(threads), isDead(threads));
        GLProfile.shutdown();
    }
}
