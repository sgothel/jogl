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
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.LandscapeES2;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestLandscapeES2NEWT extends UITestCase {
    static int width = 500, height = 290;
    static int swapInterval = 1;
    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static boolean mainRun = false;
    static boolean exclusiveContext = false;
    static boolean useAnimator = true;

    protected void runTestGL(final GLCapabilities caps) throws InterruptedException {
        System.err.println("requested: swapInterval "+swapInterval+", "+caps);
        final GLWindow glWindow = GLWindow.create(caps);
        glWindow.setTitle(getSimpleTestName("."));
        glWindow.setSize(width, height);

        final LandscapeES2 demo = new LandscapeES2(swapInterval);
        glWindow.addGLEventListener(demo);

        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        snap.setPostSNDetail(demo.getClass().getSimpleName());
        glWindow.addGLEventListener(snap);

        final Animator animator = useAnimator ? new Animator() : null;
        if( useAnimator ) {
            animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
            animator.setExclusiveContext(exclusiveContext);
        }

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

        if( useAnimator ) {
            animator.add(glWindow);
            animator.start();
        }
        glWindow.setVisible(true);
        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, System.err);
        }

        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());

        snap.setMakeSnapshot();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        if( useAnimator ) {
            animator.stop();
        }
        glWindow.destroy();
    }

    @Test
    public void test01GL2ES2() throws InterruptedException {
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

    static long duration = 1000; // ms

    public static void main(final String args[]) {
        mainRun = true;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-exclctx")) {
                exclusiveContext = true;
            } else if(args[i].equals("-noanim")) {
                useAnimator  = false;
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            }
        }
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceGL3 "+forceGL3);
        org.junit.runner.JUnitCore.main(TestLandscapeES2NEWT.class.getName());
    }
}
