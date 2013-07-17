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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import com.jogamp.opengl.test.junit.jogl.demos.es2.FBOMix2DemosES2;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.Test;

public class TestFBOMix2DemosES2NEWT extends UITestCase {    
    static long duration = 500; // ms
    static int swapInterval = 1;
    static boolean showFPS = false;
    static boolean forceES2 = false;
    static boolean doRotate = true;
    static boolean demo0Only = false;
    static int globalNumSamples = 0;
    static boolean mainRun = false;
    
    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(GLCapabilitiesImmutable caps, int numSamples) throws InterruptedException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        System.err.println("requested: vsync "+swapInterval+", "+caps);
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Gears NEWT Test (translucent "+!caps.isBackgroundOpaque()+"), swapInterval "+swapInterval);
        if(mainRun) {
            glWindow.setSize(512, 512);            
        } else {
            glWindow.setSize(128, 128);
        }

        final FBOMix2DemosES2 demo = new FBOMix2DemosES2(swapInterval);
        demo.setMSAA(numSamples);
        demo.setDoRotation(doRotate);
        demo.setDemo0Only(demo0Only);
        glWindow.addGLEventListener(demo);
        glWindow.addGLEventListener(new GLEventListener() {
            int i=0, c=0;
            int origS;
            public void init(GLAutoDrawable drawable) {
                origS = demo.getMSAA();
            }
            public void dispose(GLAutoDrawable drawable) {}
            public void display(GLAutoDrawable drawable) {
                if(mainRun) return;
                
                final int dw = drawable.getWidth();
                final int dh = drawable.getHeight();
                c++;
                
                if(dw<800) {
                    System.err.println("XXX: "+dw+"x"+dh+", c "+c);
                    if(0 == c%3) {
                        snapshot(i++, "msaa"+demo.getMSAA(), drawable.getGL(), screenshot, TextureIO.PNG, null);                        
                    }
                    if( 3 == c ) {
                        new Thread() { 
                            @Override
                            public void run() {
                                demo.setMSAA(4);
                            } }.start();
                    } else if( 6 == c ) {
                        new Thread() { 
                            @Override
                            public void run() {
                                demo.setMSAA(8);
                            } }.start();
                    } else if(9 == c) {
                        c=0;
                        new Thread() { 
                            @Override
                            public void run() {
                                glWindow.setSize(dw+256, dh+256);
                                demo.setMSAA(origS);
                            } }.start();                            
                    }
                }
            }
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
        });
        
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
            public void keyReleased(KeyEvent e) {
                if( !e.isPrintableKey() || e.isAutoRepeat() ) {
                    return;
                }            
                System.err.println("*** "+e);
                if(e.getKeyChar()=='f') {
                    new Thread() {
                        public void run() {
                            System.err.println("[set fullscreen  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setFullscreen(!glWindow.isFullscreen());
                            System.err.println("[set fullscreen post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                    } }.start();
                } else if(e.getKeyChar()=='d') {
                    demo.setDemo0Only(!demo.getDemo0Only());
                } else {
                    int num = e.getKeyChar() - '0';
                    System.err.println("*** "+num);
                    if(0 <= num && num <= 8) {
                        System.err.println("MSAA: "+demo.getMSAA()+" -> "+num);
                        demo.setMSAA(num);                        
                    }
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
    public void test01_Main() throws InterruptedException {
        if( mainRun ) {
            GLCapabilities caps = new GLCapabilities(forceES2 ? GLProfile.get(GLProfile.GLES2) : GLProfile.getGL2ES2());
            caps.setAlphaBits(1);
            runTestGL(caps, globalNumSamples);            
        }
    }
    
    @Test
    public void test01() throws InterruptedException {
        if( mainRun ) return ;
        GLCapabilities caps = new GLCapabilities(forceES2 ? GLProfile.get(GLProfile.GLES2) : GLProfile.getGL2ES2());
        caps.setAlphaBits(1);
        runTestGL(caps, 0);
    }

    public static void main(String args[]) throws IOException {        
        boolean waitForKey = false;
        
        mainRun = true;
        
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-showFPS")) {
                showFPS = true;
            } else if(args[i].equals("-samples")) {
                i++;
                globalNumSamples = MiscUtils.atoi(args[i], globalNumSamples);
            } else if(args[i].equals("-norotate")) {
                doRotate = false;
            } else if(args[i].equals("-demo0Only")) {
                demo0Only = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-nomain")) {
                mainRun = false;
            }
        }
        
        System.err.println("swapInterval "+swapInterval);
        System.err.println("forceES2 "+forceES2);

        if(waitForKey) {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (IOException e) { }
        }
        org.junit.runner.JUnitCore.main(TestFBOMix2DemosES2NEWT.class.getName());
    }
}
