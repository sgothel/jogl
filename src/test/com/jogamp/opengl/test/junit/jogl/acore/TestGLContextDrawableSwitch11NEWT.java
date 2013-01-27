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
 
package com.jogamp.opengl.test.junit.jogl.acore;

import java.io.IOException;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import jogamp.opengl.GLEventListenerState;

import com.jogamp.opengl.GLAutoDrawableDelegate;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.GLEventListenerCounter;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test re-association of GLContext/GLDrawables,
 * here GLContext's survival of GLDrawable desctruction 
 * and reuse w/ new or recreated GLDrawable.
 * <p>
 * See Bug 665 - https://jogamp.org/bugzilla/show_bug.cgi?id=665.
 * </p>
 */
public class TestGLContextDrawableSwitch11NEWT extends UITestCase {
    static int width, height;

    static GLCapabilities getCaps(String profile) {
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

    private GLAutoDrawable createGLAutoDrawableWithoutContext(GLCapabilities caps, int x, int y, int width, int height, WindowListener wl) throws InterruptedException {
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
        
        final GLAutoDrawableDelegate glad = new GLAutoDrawableDelegate(drawable, null, window, false, null) {
            @Override
            protected void destroyImplInLock() {
                super.destroyImplInLock();
                window.destroy(); // destroys the actual window
            }            
        };
        
        // add basic window interaction
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowRepaint(WindowUpdateEvent e) {
                glad.windowRepaintOp();
            }
            @Override
            public void windowResized(WindowEvent e) {
                glad.windowResizedOp(window.getWidth(), window.getHeight());
            }
            @Override
            public void windowDestroyNotify(WindowEvent e) {
                glad.windowDestroyNotifyOp();
            }
        });
        window.addWindowListener(wl);
        
        return glad;
    }
    
    @Test(timeout=30000)
    public void test01GLADDelegateGL2ES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        test01GLADDelegateImpl(reqGLCaps);
    }
    
    @Test(timeout=30000)
    public void test01GLADDelegateGLES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        test01GLADDelegateImpl(reqGLCaps);
    }
    
    private void test01GLADDelegateImpl(GLCapabilities caps) throws InterruptedException {
        final QuitAdapter quitAdapter = new QuitAdapter();
        
        final GLEventListenerCounter glelCounter = new GLEventListenerCounter();
        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();        
        final Animator animator = new Animator();
        animator.start();
        
        final long t0 = System.currentTimeMillis();
        final GLEventListenerState glls1;
        
        // - create glad1 w/o context
        // - create context using glad1 and assign it to glad1
        {
            final GLAutoDrawable glad1 = createGLAutoDrawableWithoutContext(caps,         64, 64,     width,     height, quitAdapter);
            final GLContext context1 = glad1.createContext(null);
            glad1.setContext(context1);
            animator.add(glad1);
            
            glad1.addGLEventListener(glelCounter);
            glad1.addGLEventListener(new GearsES2(1));
            glad1.addGLEventListener(snapshotGLEventListener);
            snapshotGLEventListener.setMakeSnapshot();
            
            long t1 = System.currentTimeMillis();
            
            while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration/2 ) {
                Thread.sleep(100);
                t1 = System.currentTimeMillis();
            }
    
            Assert.assertEquals(1, glelCounter.initCount);
            Assert.assertTrue(1 <= glelCounter.reshapeCount);
            Assert.assertTrue(1 <= glelCounter.displayCount);
            Assert.assertEquals(0, glelCounter.disposeCount);
            Assert.assertEquals(context1, glad1.getContext());
            Assert.assertEquals(3, glad1.getGLEventListenerCount());
            Assert.assertEquals(context1.getGLReadDrawable(), glad1.getDelegatedDrawable());
            Assert.assertEquals(context1.getGLDrawable(), glad1.getDelegatedDrawable());
            
            // - dis-associate context from glad1
            // - destroy glad1
            glls1 = GLEventListenerState.moveFrom(glad1);
            
            Assert.assertEquals(1, glelCounter.initCount);
            Assert.assertTrue(1 <= glelCounter.reshapeCount);
            Assert.assertTrue(1 <= glelCounter.displayCount);
            Assert.assertEquals(0, glelCounter.disposeCount);
            Assert.assertEquals(context1, glls1.context);
            Assert.assertNull(context1.getGLReadDrawable());
            Assert.assertNull(context1.getGLDrawable());
            Assert.assertEquals(3, glls1.listenerCount());
            Assert.assertEquals(true, glls1.isOwner());
            Assert.assertEquals(null, glad1.getContext());
            Assert.assertEquals(0, glad1.getGLEventListenerCount());
            
            glad1.destroy();
            Assert.assertEquals(1, glelCounter.initCount);
            Assert.assertTrue(1 <= glelCounter.reshapeCount);
            Assert.assertTrue(1 <= glelCounter.displayCount);
            Assert.assertEquals(0, glelCounter.disposeCount);
        }
        
        // - create glad2 w/ survived context
        {
            final GLAutoDrawable glad2 = createGLAutoDrawableWithoutContext(caps, 2*64+width, 64, width+100, height+100, quitAdapter);
            snapshotGLEventListener.setMakeSnapshot();
            
            Assert.assertEquals(null, glad2.getContext());
            Assert.assertEquals(0, glad2.getGLEventListenerCount());
            
            glls1.moveTo(glad2);
            
            Assert.assertTrue(glad2.isRealized());
            
            Assert.assertEquals(1, glelCounter.initCount);
            Assert.assertTrue(1 <= glelCounter.reshapeCount);
            Assert.assertTrue(1 <= glelCounter.displayCount);
            Assert.assertEquals(0, glelCounter.disposeCount);
            Assert.assertEquals(glls1.context, glad2.getContext());
            Assert.assertEquals(3, glad2.getGLEventListenerCount());
            Assert.assertEquals(glls1.context.getGLReadDrawable(), glad2.getDelegatedDrawable());
            Assert.assertEquals(glls1.context.getGLDrawable(), glad2.getDelegatedDrawable());
            Assert.assertEquals(false, glls1.isOwner());
            
            long t1 = System.currentTimeMillis();
            
            while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration/1 ) {
                Thread.sleep(100);
                t1 = System.currentTimeMillis();
            }
    
            glad2.destroy();
            Assert.assertEquals(1, glelCounter.initCount);
            Assert.assertTrue(1 <= glelCounter.reshapeCount);
            Assert.assertTrue(1 <= glelCounter.displayCount);
            Assert.assertEquals(1, glelCounter.disposeCount);
        }
        animator.stop();
    }
    
    @Test(timeout=30000)
    public void test02GLWindowGL2ES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        test02GLWindowImpl(reqGLCaps);
    }
    
    @Test(timeout=30000)
    public void test02GLWindowGLES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        test02GLWindowImpl(reqGLCaps);
    }
    
    private void test02GLWindowImpl(GLCapabilities caps) throws InterruptedException {
        final QuitAdapter quitAdapter = new QuitAdapter();
        
        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();        
        final GLEventListenerCounter glelTracker = new GLEventListenerCounter();
        final Animator animator = new Animator();
        animator.start();
        
        final long t0 = System.currentTimeMillis();
        final GLEventListenerState glls1;
        
        // - create glad1 w/o context
        // - create context using glad1 and assign it to glad1
        {
            final GLWindow glad1 = GLWindow.create(caps);
            glad1.setSize(width, height);
            glad1.setPosition(64, 64);
            glad1.addWindowListener(quitAdapter);
            animator.add(glad1);
            
            glad1.addGLEventListener(glelTracker);
            glad1.addGLEventListener(new GearsES2(1));
            glad1.addGLEventListener(snapshotGLEventListener);
            snapshotGLEventListener.setMakeSnapshot();
                        
            glad1.setVisible(true);
            
            long t1 = System.currentTimeMillis();
            
            while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration/2 ) {
                Thread.sleep(100);
                t1 = System.currentTimeMillis();
            }
    
            // - dis-associate context from glad1
            // - destroy glad1
            Assert.assertEquals(1, glelTracker.initCount);
            Assert.assertTrue(1 <= glelTracker.reshapeCount);
            Assert.assertTrue(1 <= glelTracker.displayCount);
            Assert.assertEquals(0, glelTracker.disposeCount);
            Assert.assertEquals(3, glad1.getGLEventListenerCount());
            Assert.assertEquals(glad1.getContext().getGLReadDrawable(), glad1.getDelegatedDrawable());
            Assert.assertEquals(glad1.getContext().getGLDrawable(), glad1.getDelegatedDrawable());
            
            final GLContext context1 = glad1.getContext();
            glls1 = GLEventListenerState.moveFrom(glad1);
            
            Assert.assertEquals(1, glelTracker.initCount);
            Assert.assertTrue(1 <= glelTracker.reshapeCount);
            Assert.assertTrue(1 <= glelTracker.displayCount);
            Assert.assertEquals(0, glelTracker.disposeCount);
            Assert.assertEquals(context1, glls1.context);
            Assert.assertNull(context1.getGLReadDrawable());
            Assert.assertNull(context1.getGLDrawable());
            Assert.assertEquals(3, glls1.listenerCount());
            Assert.assertEquals(true, glls1.isOwner());
            Assert.assertEquals(null, glad1.getContext());
            Assert.assertEquals(0, glad1.getGLEventListenerCount());
            
            glad1.destroy();
            Assert.assertEquals(1, glelTracker.initCount);
            Assert.assertTrue(1 <= glelTracker.reshapeCount);
            Assert.assertTrue(1 <= glelTracker.displayCount);
            Assert.assertEquals(0, glelTracker.disposeCount);
        }
        
        // - create glad2 w/ survived context
        {
            final GLWindow glad2 = GLWindow.create(caps);
            glad2.setSize(width+100, height+100);
            glad2.setPosition(2*64+width, 64);
            glad2.addWindowListener(quitAdapter);
            snapshotGLEventListener.setMakeSnapshot();
            glad2.setVisible(true);
            
            Assert.assertNotNull(glad2.getContext());
            Assert.assertEquals(0, glad2.getGLEventListenerCount());
            
            glls1.moveTo(glad2);            
            
            Assert.assertEquals(1, glelTracker.initCount);
            Assert.assertTrue(1 <= glelTracker.reshapeCount);
            Assert.assertTrue(1 <= glelTracker.displayCount);
            Assert.assertEquals(0, glelTracker.disposeCount);
            Assert.assertEquals(glls1.context, glad2.getContext());
            Assert.assertEquals(3, glad2.getGLEventListenerCount());
            Assert.assertEquals(glls1.context.getGLReadDrawable(), glad2.getDelegatedDrawable());
            Assert.assertEquals(glls1.context.getGLDrawable(), glad2.getDelegatedDrawable());
            Assert.assertEquals(false, glls1.isOwner());
            
            long t1 = System.currentTimeMillis();
            
            while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration/1 ) {
                Thread.sleep(100);
                t1 = System.currentTimeMillis();
            }
    
            glad2.destroy();
            Assert.assertEquals(1, glelTracker.initCount);
            Assert.assertTrue(1 <= glelTracker.reshapeCount);
            Assert.assertTrue(1 <= glelTracker.displayCount);
            Assert.assertEquals(1, glelTracker.disposeCount);
        }
        animator.stop();
    }
    
    // default timing for 2 switches
    static long duration = 2200; // ms

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */         
        org.junit.runner.JUnitCore.main(TestGLContextDrawableSwitch11NEWT.class.getName());
    }
}
