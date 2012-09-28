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
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import com.jogamp.opengl.GLAutoDrawableDelegate;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGLContextDrawableSwitchNEWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getGL2ES2();
        caps = new GLCapabilities(glp);
        width  = 256;
        height = 256;
    }

    /** Note: No GLContext is attached w/ this implementation ! */
    private GLAutoDrawable createGLAutoDrawable(GLCapabilities caps, int x, int y, int width, int height, WindowListener wl) throws InterruptedException {
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
    public void testSwitch2WindowSingleContext() throws InterruptedException {
        final QuitAdapter quitAdapter = new QuitAdapter();
        
        GLAutoDrawable glad1 = createGLAutoDrawable(caps,         64, 64,     width,     height, quitAdapter);
        GLAutoDrawable glad2 = createGLAutoDrawable(caps, 2*64+width, 64, width+100, height+100, quitAdapter);
        
        // create single context using glad1 and assign it to glad1,
        // after destroying the prev. context!
        {
            final GLContext oldCtx = glad1.getContext();
            Assert.assertNotNull(oldCtx);
            oldCtx.destroy();
            final GLContext singleCtx = glad1.createContext(null);
            Assert.assertNotNull(singleCtx);        
            int res = singleCtx.makeCurrent();
            Assert.assertTrue(GLContext.CONTEXT_CURRENT_NEW==res || GLContext.CONTEXT_CURRENT==res);
            singleCtx.release();
            glad1.setContext(singleCtx);
        }
        
        GearsES2 gears = new GearsES2(1);
        glad1.addGLEventListener(gears);
        
        Animator animator = new Animator();
        animator.add(glad1);
        animator.add(glad2);
        animator.start();
        
        int s = 0;
        long t0 = System.currentTimeMillis();
        long t1 = t0;
        
        while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration ) {
            if( ( t1 - t0 ) / period > s) {
                s++;
                System.err.println(s+" - switch - START "+ ( t1 - t0 ));
                animator.pause();

                // switch context _and_ the demo synchronously
                if(0 == s%2) {
                    final GLEventListener demo = glad2.removeGLEventListener(0);
                    GLContext ctx1 = glad1.setContext(glad2.getContext());
                    glad2.setContext(ctx1);
                    glad1.addGLEventListener(0, demo);
                } else {
                    final GLEventListener demo = glad1.removeGLEventListener(0);
                    GLContext ctx2 = glad2.setContext(glad1.getContext());                    
                    glad1.setContext(ctx2);
                    glad2.addGLEventListener(0, demo);
                }
                System.err.println(s+" - switch - display-1");
                glad1.display();
                System.err.println(s+" - switch - display-2");
                glad2.display();
                
                System.err.println(s+" - switch - END "+ ( t1 - t0 ));
                
                animator.resume();
            }
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        animator.stop();
        glad1.destroy();
        glad2.destroy();        
    }
    
    @Test(timeout=30000)
    public void testSwitch2GLWindowOneDemo() throws InterruptedException {
        GearsES2 gears = new GearsES2(1);
        final QuitAdapter quitAdapter = new QuitAdapter();
        
        GLWindow glWindow1 = GLWindow.create(caps);        
        glWindow1.setTitle("win1");
        glWindow1.setSize(width, height);
        glWindow1.setPosition(64, 64);
        glWindow1.addGLEventListener(0, gears);
        glWindow1.addWindowListener(quitAdapter);
        
        GLWindow glWindow2 = GLWindow.create(caps);                
        glWindow2.setTitle("win2");        
        glWindow2.setSize(width+100, height+100);
        glWindow2.setPosition(2*64+width, 64);
        glWindow2.addWindowListener(quitAdapter);

        Animator animator = new Animator();
        animator.add(glWindow1);
        animator.add(glWindow2);
        animator.start();
        
        glWindow1.setVisible(true);
        glWindow2.setVisible(true);

        int s = 0;
        long t0 = System.currentTimeMillis();
        long t1 = t0;
        
        while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration ) {
            if( ( t1 - t0 ) / period > s) {
                s++;
                System.err.println(s+" - switch - START "+ ( t1 - t0 ));
                animator.pause();

                // switch context _and_ the demo synchronously
                if(0 == s%2) {
                    final GLEventListener demo = glWindow2.removeGLEventListener(0);
                    GLContext ctx1 = glWindow1.setContext(glWindow2.getContext());
                    glWindow1.addGLEventListener(0, demo);
                    glWindow2.setContext(ctx1);
                } else {
                    final GLEventListener demo = glWindow1.removeGLEventListener(0);
                    GLContext ctx2 = glWindow2.setContext(glWindow1.getContext());                    
                    glWindow2.addGLEventListener(0, demo);
                    glWindow1.setContext(ctx2);
                }
                
                System.err.println(s+" - switch - END "+ ( t1 - t0 ));
                
                animator.resume();
            }
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        animator.stop();
        glWindow1.destroy();
        glWindow2.destroy();
        
    }
    
    @Test(timeout=30000)
    public void testSwitch2GLWindowEachWithOwnDemo() throws InterruptedException {
        GearsES2 gears = new GearsES2(1);
        RedSquareES2 rsquare = new RedSquareES2(1);
        final QuitAdapter quitAdapter = new QuitAdapter();
        
        GLWindow glWindow1 = GLWindow.create(caps);        
        glWindow1.setTitle("win1");
        glWindow1.setSize(width, height);
        glWindow1.setPosition(64, 64);
        glWindow1.addGLEventListener(0, gears);
        glWindow1.addWindowListener(quitAdapter);
        
        GLWindow glWindow2 = GLWindow.create(caps);                
        glWindow2.setTitle("win2");        
        glWindow2.setSize(width+100, height+100);
        glWindow2.setPosition(2*64+width, 64);
        glWindow2.addGLEventListener(0, rsquare);
        glWindow2.addWindowListener(quitAdapter);

        Animator animator = new Animator();
        animator.add(glWindow1);
        animator.add(glWindow2);
        animator.start();
        
        glWindow1.setVisible(true);
        glWindow2.setVisible(true);

        int s = 0;
        long t0 = System.currentTimeMillis();
        long t1 = t0;
        
        while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration ) {
            if( ( t1 - t0 ) / period > s) {
                s++;
                System.err.println(s+" - switch - START "+ ( t1 - t0 ));
                animator.pause();
                
                GLEventListener demo1 = glWindow1.removeGLEventListener(0);
                GLEventListener demo2 = glWindow2.removeGLEventListener(0);
                
                GLContext ctx1 = glWindow1.setContext(glWindow2.getContext());
                glWindow1.addGLEventListener(0, demo2);
                
                glWindow2.setContext(ctx1);
                glWindow2.addGLEventListener(0, demo1);
                
                System.err.println(s+" - switch - END "+ ( t1 - t0 ));
                
                animator.resume();
            }
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        animator.stop();
        glWindow1.destroy();
        glWindow2.destroy();
        
    }

    // default timing for 2 switches
    static long duration = 2200; // ms
    static long period = 1000; // ms

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-period")) {
                i++;
                try {
                    period = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */         
        org.junit.runner.JUnitCore.main(TestGLContextDrawableSwitchNEWT.class.getName());
    }
}
