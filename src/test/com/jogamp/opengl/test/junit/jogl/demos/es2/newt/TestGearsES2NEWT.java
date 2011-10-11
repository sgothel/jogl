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

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

public class TestGearsES2NEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
        /*if(GLProfile.isAvailable(GLProfile.getDefaultEGLDevice(), GLProfile.GLES2)) {
            // exact match
            glp = GLProfile.get(GLProfile.getDefaultEGLDevice(), GLProfile.GLES2);
        } else */ {
            // default device, somehow ES2 compatible
            glp = GLProfile.getGL2ES2(); 
        }
        Assert.assertNotNull(glp);
        width  = 512;
        height = 512;
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(GLCapabilities caps, boolean undecorated) throws InterruptedException {
        System.err.println("requested: "+caps);
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Gears NEWT Test (translucent "+!caps.isBackgroundOpaque()+")");
        glWindow.setSize(width, height);
        glWindow.setUndecorated(undecorated);
        glWindow.setAlwaysOnTop(alwaysOnTop);
        glWindow.setFullscreen(fullscreen);
        glWindow.setPointerVisible(mouseVisible);
        glWindow.confinePointer(mouseConfined);

        final GearsES2 demo = new GearsES2(vsync ? 1 : 0);
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

        glWindow.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar()=='f') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set fullscreen  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", "+glWindow.getInsets());
                            glWindow.setFullscreen(!glWindow.isFullscreen());
                            System.err.println("[set fullscreen post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", "+glWindow.getInsets());
                    } }.start();
                } else if(e.getKeyChar()=='a') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set alwaysontop pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setAlwaysOnTop(!glWindow.isAlwaysOnTop());
                            System.err.println("[set alwaysontop post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
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

        animator.setUpdateFPSFrames(60, System.err);
        animator.start();
        // glWindow.setSkipContextReleaseThread(animator.getThread());

        glWindow.setVisible(true);
        
        System.err.println("size/pos: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", "+glWindow.getInsets());
        System.err.println("chosen: "+glWindow.getChosenCapabilities());
        
        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        glWindow.destroy();
    }

    @Test
    public void test01() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(opaque);
        runTestGL(caps, undecorated);
    }

    static long duration = 500; // ms
    static boolean opaque = true;
    static boolean undecorated = false;
    static boolean alwaysOnTop = false;
    static boolean fullscreen = false;
    static boolean pmvUseBackingArray = true;
    static boolean vsync = false;
    static boolean waitForKey = false;
    static boolean mouseVisible = true;
    static boolean mouseConfined = false;

    public static void main(String args[]) throws IOException {
        
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
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
                vsync = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-mouseInvisible")) {
                mouseVisible = false;
            } else if(args[i].equals("-mouseConfine")) {
                mouseConfined = true;
            }
        }
        System.err.println("translucent "+(!opaque));
        System.err.println("undecorated "+undecorated);
        System.err.println("atop "+alwaysOnTop);
        System.err.println("fullscreen "+fullscreen);
        System.err.println("pmvDirect "+(!pmvUseBackingArray));
        System.err.println("vsync "+vsync);
        System.err.println("mouseVisible "+mouseVisible);
        System.err.println("mouseConfined "+mouseConfined);

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
