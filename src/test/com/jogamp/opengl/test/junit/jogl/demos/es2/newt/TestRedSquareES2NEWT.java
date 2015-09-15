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

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareMappedES2;

import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRedSquareES2NEWT extends UITestCase {
    static int width, height;
    static int loops = 1;
    static boolean loop_shutdown = false;
    static boolean vsync = false;
    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static boolean mainRun = false;
    static boolean doRotate = true;
    static boolean useMappedBuffers = false;

    @BeforeClass
    public static void initClass() {
        width  = 512;
        height = 512;
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(final GLCapabilities caps) throws InterruptedException {
        System.err.println("requested: vsync "+vsync+", "+caps);
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle(getSimpleTestName("."));
        glWindow.setSize(width, height);

        final GLEventListener demo;
        if( useMappedBuffers ) {
            final RedSquareMappedES2 red = new RedSquareMappedES2(vsync ? 1 : -1);
            red.setDoRotation(doRotate);
            demo = red;
        } else {
            final RedSquareES2 red = new RedSquareES2(vsync ? 1 : -1);
            red.setDoRotation(doRotate);
            demo = red;
        }
        glWindow.addGLEventListener(demo);

        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        snap.setPostSNDetail(demo.getClass().getSimpleName());
        glWindow.addGLEventListener(snap);

        final Animator animator = new Animator(glWindow);
        final QuitAdapter quitAdapter = new QuitAdapter();

        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        glWindow.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e) {
                if( !e.isPrintableKey() || e.isAutoRepeat() ) {
                    return;
                }
                if(e.getKeyChar()=='f') {
                    glWindow.invokeOnNewThread(null, false, new Runnable() {
                        public void run() {
                            glWindow.setFullscreen(!glWindow.isFullscreen());
                    } } );
                } else if(e.getKeyChar()=='d') {
                    glWindow.invokeOnNewThread(null, false, new Runnable() {
                        public void run() {
                            glWindow.setUndecorated(!glWindow.isUndecorated());
                    } } );
                }
            }
        });

        animator.start();

        glWindow.setVisible(true);

        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());

        animator.setUpdateFPSFrames(60, System.err);
        snap.setMakeSnapshot();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());
        glWindow.destroy();
        if( NativeWindowFactory.isAWTAvailable() ) {
            Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow, false));
        }
    }

    @Test
    public void test01GL2ES2() throws InterruptedException {
        for(int i=1; i<=loops; i++) {
            System.err.println("Loop "+i+"/"+loops);
            final GLProfile glp;
            if(forceGL3) {
                glp = GLProfile.get(GLProfile.GL3);
            } else if(forceES2) {
                glp = GLProfile.get(GLProfile.GLES2);
            } else {
                glp = GLProfile.getGL2ES2();
            }
            final GLCapabilities caps = new GLCapabilities(glp);
            runTestGL(caps);
            if(loop_shutdown) {
                GLProfile.shutdown();
            }
        }
    }

    @Test
    public void test02GL3() throws InterruptedException {
        if(mainRun) return;

        if( !GLProfile.isAvailable(GLProfile.GL3) ) {
            System.err.println("GL3 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL3);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps);
    }

    static long duration = 500; // ms

    public static void main(final String args[]) {
        mainRun = true;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-norotate")) {
                doRotate = false;
            } else if(args[i].equals("-mappedBuffers")) {
                useMappedBuffers = true;
            } else if(args[i].equals("-loops")) {
                i++;
                loops = MiscUtils.atoi(args[i], 1);
            } else if(args[i].equals("-loop-shutdown")) {
                loop_shutdown = true;
            }
        }
        System.err.println("duration "+duration);
        System.err.println("loops "+loops);
        System.err.println("loop shutdown "+loop_shutdown);
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceGL3 "+forceGL3);
        System.err.println("mappedBuffers "+useMappedBuffers);
        org.junit.runner.JUnitCore.main(TestRedSquareES2NEWT.class.getName());
    }
}
