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

import java.io.IOException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

public class TestInitConcurrentNEWT extends UITestCase {

    static long duration = 300; // ms
    
    public class JOGLTask implements Runnable {
        private Object postSync;
        private boolean done = false;
        
        public JOGLTask(Object postSync) {
            this.postSync = postSync;
        }
        public void run() {
            System.err.println(Thread.currentThread().getName()+" START");
            GLWindow glWindow = GLWindow.create(new GLCapabilities(GLProfile.getDefault()));
            Assert.assertNotNull(glWindow);
            glWindow.setTitle("Gears NEWT Test");
    
            glWindow.addGLEventListener(new GearsES2(0));
    
            Animator animator = new Animator(glWindow);
    
            glWindow.setSize(128, 128);
            glWindow.setVisible(true);
            animator.setUpdateFPSFrames(60, System.err);
            animator.start();
            Assert.assertEquals(true, animator.isAnimating());
            Assert.assertEquals(true, glWindow.isVisible());
            Assert.assertEquals(true, glWindow.isNativeValid());
            Assert.assertEquals(true, glWindow.isRealized());
    
            while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
    
            animator.stop();
            glWindow.destroy();
            
            System.err.println(Thread.currentThread().getName()+" sync");
            synchronized (postSync) {
                done = true;
                System.err.println(Thread.currentThread().getName()+" END");
                postSync.notifyAll();
            }
        }
        
        public boolean done() { return done; }
    }

    protected static boolean done(JOGLTask[] tasks) {
        for(int i=tasks.length-1; i>=0; i--) {
            if(!tasks[i].done()) {
                return false;
            }
        }
        return true;
    }
    protected static String doneDump(JOGLTask[] tasks) {
        StringBuffer sb = new StringBuffer();
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
    
    protected static boolean isDead(Thread[] threads) {
        for(int i=threads.length-1; i>=0; i--) {
            if(threads[i].isAlive()) {
                return false;
            }
        }
        return true;
    }
    protected static String isAliveDump(Thread[] threads) {
        StringBuffer sb = new StringBuffer();
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
    
    protected void runJOGLTasks(int num) throws InterruptedException {
        final String currentThreadName = Thread.currentThread().getName();
        final Object sync = new Object();
        final JOGLTask[] tasks = new JOGLTask[num];
        final Thread[] threads = new Thread[num];
        int i;
        for(i=0; i<num; i++) {
            tasks[i] = new JOGLTask(sync);
            threads[i] = new Thread(tasks[i], currentThreadName+"-jt"+i);
        }
        synchronized (sync) {
            for(i=0; i<num; i++) {
                threads[i].start();
            }
            while(!done(tasks)) {
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Assert.assertTrue("Tasks are incomplete. Complete: "+doneDump(tasks), done(tasks));
        i=0;
        while(i<30 && !isDead(threads)) {
            Thread.sleep(100);
            i++;
        }
        Assert.assertTrue("Threads are still alive after 3s. Alive: "+isAliveDump(threads), isDead(threads));
    }
    
    @Test
    public void test01OneThread() throws InterruptedException {
        runJOGLTasks(1);
    }

    @Test
    public void test02TwoThreads() throws InterruptedException {
        runJOGLTasks(2);
    }
    
    @Test
    public void test04FourThreads() throws InterruptedException {
        runJOGLTasks(4);
    }
    
    @Test
    public void test16SixteenThreads() throws InterruptedException {
        runJOGLTasks(16);
    }
    
    public static void main(String args[]) throws IOException {
        String tstname = TestInitConcurrentNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
