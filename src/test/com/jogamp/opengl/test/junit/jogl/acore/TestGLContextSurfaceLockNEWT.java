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

import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.UITestCase;

public class TestGLContextSurfaceLockNEWT extends UITestCase {
    static final int demoSize = 64;
    
    public abstract class MyRunnable implements Runnable {
        final Object postSync;
        final int id;
        boolean done = false;
        
        public MyRunnable(Object postSync, int id) {
            this.postSync = postSync;
            this.id = id;
        }
        
        public boolean done() { return done; }
    }
    
    public class RudeAnimator extends MyRunnable {
        private final GLAutoDrawable glad;
        private final int frameCount;
        
        public RudeAnimator(GLAutoDrawable glad, int frameCount, Object postSync, int id) {
            super(postSync, id);
            this.glad = glad;
            this.frameCount = frameCount;
        }
        
        public void run() {
            System.err.println("Animatr "+id+": PRE: "+Thread.currentThread().getName());
            
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
        
        public RudeResizer(GLWindow win, int actionCount, Object postSync, int id) {
            super(postSync, id);
            this.win = win;
            this.actionCount = actionCount;
        }
        
        public void run() {
            System.err.println("Resizer "+id+": PRE: "+Thread.currentThread().getName());
            
            for(int c=0; c<actionCount; c++) {
                win.runOnEDTIfAvail(false, new Runnable() {
                    public void run() {
                        // Normal resize, may trigger immediate display within lock
                        win.setSize(win.getWidth()+1, win.getHeight()+1);

                        // Force display within surface lock.
                        // This procedure emulates the sensitive behavior 
                        // for all platforms directly.
                        int res = win.lockSurface();
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
    
    protected static boolean done(MyRunnable[] tasks) {
        for(int i=tasks.length-1; i>=0; i--) {
            if(!tasks[i].done()) {
                return false;
            }
        }
        return true;
    }
    
    protected static boolean isDead(Thread[] threads) {
        for(int i=threads.length-1; i>=0; i--) {
            if(threads[i].isAlive()) {
                return false;
            }
        }
        return true;
    }
    
    protected void runJOGLTasks(int animThreadCount, int frameCount, int reszThreadCount, int resizeCount) throws InterruptedException {
        GLWindow glWindow = GLWindow.create(new GLCapabilities(GLProfile.getDefault()));
        Assert.assertNotNull(glWindow);
        
        glWindow.addGLEventListener(new GearsES2(0));
        glWindow.setSize(demoSize, demoSize);
        glWindow.setVisible(true);
        
        final String currentThreadName = Thread.currentThread().getName();
        final Object sync = new Object();
        final MyRunnable[] tasks = new MyRunnable[animThreadCount+reszThreadCount];
        final Thread[] threads = new Thread[animThreadCount+reszThreadCount];
        int i=0;
        
        System.err.println("animThreadCount "+animThreadCount+", frameCount "+frameCount);
        System.err.println("reszThreadCount "+reszThreadCount+", resizeCount "+resizeCount);
        System.err.println("tasks "+tasks.length+", threads "+threads.length);

        for(; i<animThreadCount; i++) {
            System.err.println("create anim task/thread "+i);
            tasks[i] = new RudeAnimator(glWindow, frameCount, sync, i);                
            threads[i] = new Thread(tasks[i], currentThreadName+"-anim"+i);
        }
        for(; i<animThreadCount+reszThreadCount; i++) {
            System.err.println("create resz task/thread "+i);
            tasks[i] = new RudeResizer(glWindow, resizeCount, sync, i);
            threads[i] = new Thread(tasks[i], currentThreadName+"-resz"+i);
        }

        for(i=0; i<animThreadCount+reszThreadCount; i++) {
            System.err.println("start thread "+i);
            threads[i].start();
        }
        synchronized (sync) {
            while(!done(tasks)) {
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        i=0;
        while(i<30 && !isDead(threads)) {
            Thread.sleep(100);
            i++;
        }
    }
    
    @Test
    public void test01_1AThreads_600Frames() throws InterruptedException {
        runJOGLTasks(1, 600, 1, 600);
    }
    
    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    // duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        String tstname = TestGLContextSurfaceLockNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
