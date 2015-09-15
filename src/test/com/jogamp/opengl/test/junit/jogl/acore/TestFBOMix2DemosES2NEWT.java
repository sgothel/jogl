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

import com.jogamp.common.util.InterruptSource;
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

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFBOMix2DemosES2NEWT extends UITestCase {
    static long duration = 1000; // ms
    static int swapInterval = 1;
    static boolean showFPS = false;
    static boolean forceES2 = false;
    static boolean doRotate = true;
    static boolean demo0Only = false;
    static int globalNumSamples = 0;
    static boolean manual = false;

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(final GLCapabilitiesImmutable caps, final int numSamples) throws InterruptedException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        System.err.println("requested: vsync "+swapInterval+", "+caps);
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Gears NEWT Test (translucent "+!caps.isBackgroundOpaque()+"), swapInterval "+swapInterval);
        if(manual) {
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
            public void init(final GLAutoDrawable drawable) {
                origS = demo.getMSAA();
            }
            public void dispose(final GLAutoDrawable drawable) {}
            public void display(final GLAutoDrawable drawable) {
                if(manual) return;

                final int dw = drawable.getSurfaceWidth();
                final int dh = drawable.getSurfaceHeight();
                c++;

                if(dw<800) {
                    System.err.println("XXX: "+dw+"x"+dh+", c "+c);
                    if(0 == c%3) {
                        snapshot(i++, "msaa"+demo.getMSAA(), drawable.getGL(), screenshot, TextureIO.PNG, null);
                    }
                    if( 3 == c ) {
                        demo.setMSAA(4);
                    } else if( 6 == c ) {
                        new InterruptSource.Thread() {
                            @Override
                            public void run() {
                                glWindow.setSize(dw+64, dh+64);
                            } }.start();
                    } else if( 9 == c ) {
                        demo.setMSAA(8);
                    } else if( 12 == c ) {
                        demo.setMSAA(0);
                    } else if( 15 == c ) {
                        new InterruptSource.Thread() {
                            @Override
                            public void run() {
                                glWindow.setSize(dw+128, dh+128);
                            } }.start();
                    } else if( 18 == c ) {
                        c=0;
                        new InterruptSource.Thread() {
                            @Override
                            public void run() {
                                glWindow.setSize(dw+256, dh+256);
                                demo.setMSAA(origS);
                            } }.start();
                    }
                }
            }
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        });

        final Animator animator = new Animator(glWindow);
        final QuitAdapter quitAdapter = new QuitAdapter();

        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        glWindow.addWindowListener(new WindowAdapter() {
            public void windowResized(final WindowEvent e) {
                System.err.println("window resized: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
            }
            public void windowMoved(final WindowEvent e) {
                System.err.println("window moved:   "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
            }
        });

        glWindow.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e) {
                if( !e.isPrintableKey() || e.isAutoRepeat() ) {
                    return;
                }
                System.err.println("*** "+e);
                if(e.getKeyChar()=='f') {
                    new InterruptSource.Thread() {
                        public void run() {
                            System.err.println("[set fullscreen  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setFullscreen(!glWindow.isFullscreen());
                            System.err.println("[set fullscreen post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                    } }.start();
                } else if(e.getKeyChar()=='d') {
                    demo.setDemo0Only(!demo.getDemo0Only());
                } else {
                    final int num = e.getKeyChar() - '0';
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
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());

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
    public void test00_Manual() throws InterruptedException {
        if( manual ) {
            final GLCapabilities caps = new GLCapabilities(forceES2 ? GLProfile.get(GLProfile.GLES2) : GLProfile.getGL2ES2());
            caps.setAlphaBits(1);
            runTestGL(caps, globalNumSamples);
        }
    }

    @Test
    public void test01_startMSAA0() throws InterruptedException {
        if( manual ) return ;
        final GLCapabilities caps = new GLCapabilities(forceES2 ? GLProfile.get(GLProfile.GLES2) : GLProfile.getGL2ES2());
        caps.setAlphaBits(1);
        runTestGL(caps, 0);
    }

    @Test
    public void test02_startMSAA4() throws InterruptedException {
        if( manual ) return ;
        final GLCapabilities caps = new GLCapabilities(forceES2 ? GLProfile.get(GLProfile.GLES2) : GLProfile.getGL2ES2());
        caps.setAlphaBits(1);
        runTestGL(caps, 4);
    }

    public static void main(final String args[]) throws IOException {
        boolean waitForKey = false;

        manual = false;

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
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }

        System.err.println("swapInterval "+swapInterval);
        System.err.println("forceES2 "+forceES2);
        System.err.println("manual "+manual);

        if(waitForKey) {
            final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (final IOException e) { }
        }
        org.junit.runner.JUnitCore.main(TestFBOMix2DemosES2NEWT.class.getName());
    }
}
