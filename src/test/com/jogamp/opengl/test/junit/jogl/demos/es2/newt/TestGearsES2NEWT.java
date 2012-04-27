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
 
package com.jogamp.opengl.test.junit.jogl.demos.es2.newt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.PointImmutable;
import javax.media.nativewindow.util.DimensionImmutable;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

public class TestGearsES2NEWT extends UITestCase {    
    static int screenIdx = 0;
    static PointImmutable wpos;
    static DimensionImmutable wsize;

    static long duration = 500; // ms
    static boolean opaque = true;
    static boolean undecorated = false;
    static boolean alwaysOnTop = false;
    static boolean fullscreen = false;
    static boolean pmvUseBackingArray = true;
    static int swapInterval = 1;
    static boolean waitForKey = false;
    static boolean mouseVisible = true;
    static boolean mouseConfined = false;
    static boolean showFPS = false;
    static int loops = 1;
    static GLProfile.ShutdownType loop_shutdown = null;
    static boolean forceES2 = false;
    
    @BeforeClass
    public static void initClass() {
        if(null == wsize) {
            wsize = new Dimension(200, 200);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(GLCapabilitiesImmutable caps, boolean undecorated) throws InterruptedException {
        System.err.println("requested: vsync "+swapInterval+", "+caps);
        Display dpy = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(dpy, screenIdx);
        final GLWindow glWindow = GLWindow.create(screen, caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Gears NEWT Test (translucent "+!caps.isBackgroundOpaque()+"), swapInterval "+swapInterval+", size "+wsize+", pos "+wpos);
        glWindow.setSize(wsize.getWidth(), wsize.getHeight());
        if(null != wpos) {
            glWindow.setPosition(wpos.getX(), wpos.getY());
        }
        glWindow.setUndecorated(undecorated);
        glWindow.setAlwaysOnTop(alwaysOnTop);
        glWindow.setFullscreen(fullscreen);
        glWindow.setPointerVisible(mouseVisible);
        glWindow.confinePointer(mouseConfined);

        final GearsES2 demo = new GearsES2(swapInterval);
        demo.setPMVUseBackingArray(pmvUseBackingArray);
        glWindow.addGLEventListener(demo);
        if(waitForKey) {
            glWindow.addGLEventListener(new GLEventListener() {
                public void init(GLAutoDrawable drawable) { }
                public void dispose(GLAutoDrawable drawable) { }
                public void display(GLAutoDrawable drawable) {
                    GLAnimatorControl  actrl = drawable.getAnimator();
                    if(waitForKey && actrl.getTotalFPSFrames() == 60*3) {
                        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                        System.err.println("Press enter to continue");
                        try {
                            System.err.println(stdin.readLine());
                        } catch (IOException e) { }
                        actrl.resetFPSCounter();
                        waitForKey = false;
                    }
                }
                public void reshape(GLAutoDrawable drawable, int x, int y,
                        int width, int height) { }
            });
        }

        Animator animator = new Animator(glWindow);
        QuitAdapter quitAdapter = new QuitAdapter();

        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        glWindow.addWindowListener(new WindowAdapter() {
            public void windowResized(WindowEvent e) {
                System.err.println("window resized: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight());
            }
            public void windowMoved(WindowEvent e) {
                System.err.println("window moved:   "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight());
            }            
        });
        
        glWindow.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar()=='f') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set fullscreen  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setFullscreen(!glWindow.isFullscreen());
                            System.err.println("[set fullscreen post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                    } }.start();
                } else if(e.getKeyChar()=='a') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set alwaysontop pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setAlwaysOnTop(!glWindow.isAlwaysOnTop());
                            System.err.println("[set alwaysontop post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                    } }.start();
                } else if(e.getKeyChar()=='d') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set undecorated  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", d "+glWindow.isUndecorated()+", "+glWindow.getInsets());
                            glWindow.setUndecorated(!glWindow.isUndecorated());
                            System.err.println("[set undecorated post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", d "+glWindow.isUndecorated()+", "+glWindow.getInsets());
                    } }.start();
                } else if(e.getKeyChar()=='s') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set position  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", "+glWindow.getInsets());
                            glWindow.setPosition(100, 100);
                            System.err.println("[set position post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", "+glWindow.getInsets());
                    } }.start();
                } else if(e.getKeyChar()=='i') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set mouse visible pre]: "+glWindow.isPointerVisible());
                            glWindow.setPointerVisible(!glWindow.isPointerVisible());
                            System.err.println("[set mouse visible post]: "+glWindow.isPointerVisible());
                    } }.start();
                } else if(e.getKeyChar()=='j') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set mouse confined pre]: "+glWindow.isPointerConfined());
                            glWindow.confinePointer(!glWindow.isPointerConfined());
                            System.err.println("[set mouse confined post]: "+glWindow.isPointerConfined());
                            if(!glWindow.isPointerConfined()) {
                                demo.setConfinedFixedCenter(false);
                            }
                    } }.start();
                } else if(e.getKeyChar()=='J') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set mouse confined pre]: "+glWindow.isPointerConfined());
                            glWindow.confinePointer(!glWindow.isPointerConfined());
                            System.err.println("[set mouse confined post]: "+glWindow.isPointerConfined());
                            demo.setConfinedFixedCenter(glWindow.isPointerConfined());
                    } }.start();
                } else if(e.getKeyChar()=='w') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set mouse pos pre]");
                            glWindow.warpPointer(glWindow.getWidth()/2, glWindow.getHeight()/2);
                            System.err.println("[set mouse pos post]");
                    } }.start();
                }
            }
        });

        animator.start();
        // glWindow.setSkipContextReleaseThread(animator.getThread());

        glWindow.setVisible(true);
        
        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", "+glWindow.getInsets());
        
        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        
        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());
        glWindow.destroy();
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow, false));
    }

    @Test
    public void test01GL2ES2() throws InterruptedException {
        for(int i=1; i<=loops; i++) {
            System.err.println("Loop "+i+"/"+loops);
            GLCapabilities caps = new GLCapabilities(forceES2 ? GLProfile.get(GLProfile.GLES2) : GLProfile.getGL2ES2());
            caps.setBackgroundOpaque(opaque);
            runTestGL(caps, undecorated);
            if(null != loop_shutdown) {
                GLProfile.shutdown(loop_shutdown);
            }
        }
    }

    public static void main(String args[]) throws IOException {        
        int x=0, y=0, w=200, h=200;
        boolean useSize = false;
        boolean usePos = false;
        
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-translucent")) {
                opaque = false;
            } else if(args[i].equals("-undecorated")) {
                undecorated = true;
            } else if(args[i].equals("-atop")) {
                alwaysOnTop = true;
            } else if(args[i].equals("-fullscreen")) {
                fullscreen = true;
            } else if(args[i].equals("-pmvDirect")) {
                pmvUseBackingArray = false;
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-mouseInvisible")) {
                mouseVisible = false;
            } else if(args[i].equals("-mouseConfine")) {
                mouseConfined = true;
            } else if(args[i].equals("-showFPS")) {
                showFPS = true;
            } else if(args[i].equals("-width")) {
                i++;
                w = MiscUtils.atoi(args[i], w);
                useSize = true;
            } else if(args[i].equals("-height")) {
                i++;
                h = MiscUtils.atoi(args[i], h);
                useSize = true;
            } else if(args[i].equals("-x")) {
                i++;
                x = MiscUtils.atoi(args[i], x);
                usePos = true;
            } else if(args[i].equals("-y")) {
                i++;
                y = MiscUtils.atoi(args[i], y);
                usePos = true;
            } else if(args[i].equals("-screen")) {
                i++;
                screenIdx = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-loops")) {
                i++;
                loops = MiscUtils.atoi(args[i], 1);
            } else if(args[i].equals("-loop-shutdown")) {
                i++;
                switch(MiscUtils.atoi(args[i], 0)) {
                    case 1: loop_shutdown = GLProfile.ShutdownType.SHARED_ONLY; break; 
                    case 2: loop_shutdown = GLProfile.ShutdownType.COMPLETE; break;
                    default: throw new IllegalArgumentException("should be [0..2], 0-off, 1-shared, 2-complete");
                }
            }
        }
        if(useSize) {
            wsize = new Dimension(w, h);
        }
        if(usePos) {
            wpos = new Point(x, y);
        }
        System.err.println("position "+wpos);
        System.err.println("size "+wsize);
        System.err.println("screen "+screenIdx);
        System.err.println("translucent "+(!opaque));
        System.err.println("undecorated "+undecorated);
        System.err.println("atop "+alwaysOnTop);
        System.err.println("fullscreen "+fullscreen);
        System.err.println("pmvDirect "+(!pmvUseBackingArray));
        System.err.println("swapInterval "+swapInterval);
        System.err.println("mouseVisible "+mouseVisible);
        System.err.println("mouseConfined "+mouseConfined);
        System.err.println("loops "+loops);
        System.err.println("loop shutdown "+loop_shutdown);
        System.err.println("forceES2 "+forceES2);

        if(waitForKey) {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (IOException e) { }
        }
        org.junit.runner.JUnitCore.main(TestGearsES2NEWT.class.getName());
    }
}
