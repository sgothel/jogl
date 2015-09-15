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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLContextSurfaceLockNEWT extends UITestCase {
    static final int demoSize = 64;

    public abstract class MyRunnable implements Runnable {
        final Object postSync;
        final int id;
        boolean done = false;

        public MyRunnable(final Object postSync, final int id) {
            this.postSync = postSync;
            this.id = id;
        }

        public boolean done() { return done; }
    }

    public class RudeAnimator extends MyRunnable {
        private final GLAutoDrawable glad;
        private final int frameCount;

        public RudeAnimator(final GLAutoDrawable glad, final int frameCount, final Object postSync, final int id) {
            super(postSync, id);
            this.glad = glad;
            this.frameCount = frameCount;
        }

        public void run() {
            System.err.println("Animatr "+id+", count "+frameCount+": PRE: "+Thread.currentThread().getName());

            for(int c=0; c<frameCount; c++) {
                glad.display();
            }

            System.err.println("Animatr "+id+": DONE/SYNC: "+Thread.currentThread().getName());
            synchronized (postSync) {
                done = true;
                System.err.println("Animatr "+id+": END: "+Thread.currentThread().getName());
                postSync.notifyAll();
            }
        }
    }

    /**
     * Emulates a resize behavior with immediate display() call
     * while the surface is still locked.
     */
    public class RudeResizer extends MyRunnable {
        private final GLWindow win;
        private final int actionCount;

        public RudeResizer(final GLWindow win, final int actionCount, final Object postSync, final int id) {
            super(postSync, id);
            this.win = win;
            this.actionCount = actionCount;
        }

        public void run() {
            System.err.println("Resizer "+id+", count "+actionCount+": PRE: "+Thread.currentThread().getName());

            for(int c=0; c<actionCount; c++) {
                win.runOnEDTIfAvail(true, new Runnable() {
                    public void run() {
                        // Normal resize, may trigger immediate display within lock
                        win.setSize(win.getSurfaceWidth()+1, win.getSurfaceHeight()+1);

                        // Force display within surface lock.
                        // This procedure emulates the sensitive behavior
                        // for all platforms directly.
                        final int res = win.lockSurface();
                        if(res > NativeSurface.LOCK_SURFACE_NOT_READY) {
                            try {
                                win.display();
                            } finally {
                                win.unlockSurface();
                            }
                        }
                    }});
            }

            System.err.println("Resizer "+id+": DONE/SYNC: "+Thread.currentThread().getName());
            synchronized (postSync) {
                done = true;
                System.err.println("Resizer "+id+": END: "+Thread.currentThread().getName());
                postSync.notifyAll();
            }
        }
    }

    protected static boolean done(final MyRunnable[] tasks) {
        for(int i=tasks.length-1; i>=0; i--) {
            if(!tasks[i].done()) {
                return false;
            }
        }
        return true;
    }

    protected static boolean isDead(final Thread[] threads) {
        for(int i=threads.length-1; i>=0; i--) {
            if(threads[i].isAlive()) {
                return false;
            }
        }
        return true;
    }

    protected static class MyEventCounter implements GLEventListener {
        AtomicInteger reshapeCount = new AtomicInteger(0);
        AtomicInteger displayCount = new AtomicInteger(0);

        @Override
        public void init(final GLAutoDrawable drawable) {
        }

        @Override
        public void dispose(final GLAutoDrawable drawable) {
            System.err.println("*** reshapes: "+reshapeCount+", displays "+displayCount);
        }

        @Override
        public void display(final GLAutoDrawable drawable) {
            displayCount.incrementAndGet();
        }

        @Override
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
            reshapeCount.incrementAndGet();
        }

        public void reset() {
            reshapeCount.set(0);
            displayCount.set(0);
        }
    }

    protected void runJOGLTasks(final int animThreadCount, final int frameCount, final int reszThreadCount, final int resizeCount) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(new GLCapabilities(GLProfile.getDefault()));
        final MyEventCounter myEventCounter = new MyEventCounter();

        glWindow.addGLEventListener(new GearsES2(0));
        glWindow.addGLEventListener(myEventCounter);
        glWindow.setSize(demoSize, demoSize);
        glWindow.setVisible(true);

        final String currentThreadName = Thread.currentThread().getName();
        final Object sync = new Object();
        final MyRunnable[] animTasks = new MyRunnable[animThreadCount];
        final MyRunnable[] resizeTasks = new MyRunnable[animThreadCount];
        final InterruptSource.Thread[] animThreads = new InterruptSource.Thread[reszThreadCount];
        final InterruptSource.Thread[] resizeThreads = new InterruptSource.Thread[reszThreadCount];

        System.err.println("animThreadCount "+animThreadCount+", frameCount "+frameCount);
        System.err.println("reszThreadCount "+reszThreadCount+", resizeCount "+resizeCount);
        System.err.println("tasks "+(animTasks.length+resizeTasks.length)+", threads "+(animThreads.length+resizeThreads.length));

        for(int i=0; i<animThreadCount; i++) {
            System.err.println("create anim task/thread "+i);
            animTasks[i] = new RudeAnimator(glWindow, frameCount, sync, i);
            animThreads[i] = new InterruptSource.Thread(null, animTasks[i], currentThreadName+"-anim"+i);
        }
        for(int i=0; i<reszThreadCount; i++) {
            System.err.println("create resz task/thread "+i);
            resizeTasks[i] = new RudeResizer(glWindow, resizeCount, sync, i);
            resizeThreads[i] = new InterruptSource.Thread(null, resizeTasks[i], currentThreadName+"-resz"+i);
        }

        myEventCounter.reset();

        int j=0, k=0;
        for(int i=0; i<reszThreadCount+animThreadCount; i++) {
            if(0==i%2) {
                System.err.println("start resize thread "+j);
                resizeThreads[j++].start();
            } else {
                System.err.println("start anim thread "+k);
                animThreads[k++].start();
            }
        }
        synchronized (sync) {
            while(!done(resizeTasks) || !done(animTasks)) {
                try {
                    sync.wait();
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        int i=0;
        while(i<30 && !isDead(animThreads) && !isDead(resizeThreads)) {
            Thread.sleep(100);
            i++;
        }

        glWindow.destroy();
    }

    @Test
    public void test01_1A1RThreads_100Resizes() throws InterruptedException {
        runJOGLTasks(1, 200, 1, 100);
    }

    @Test
    public void test01_3A3RThreads_50Resizes() throws InterruptedException {
        runJOGLTasks(3, 100, 3, 50);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    // duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        final String tstname = TestGLContextSurfaceLockNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
