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

package com.jogamp.opengl.test.junit.jogl.caps;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTranslucencyNEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;

    @BeforeClass
    public static void initClass() {
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

    protected void runTestGL(final GLCapabilities caps, final boolean undecorated) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Gears NEWT Test (translucent "+!caps.isBackgroundOpaque()+")");
        glWindow.setUndecorated(undecorated);
        glWindow.addGLEventListener(new GearsES2());

        final Animator animator = new Animator(glWindow);
        final QuitAdapter quitAdapter = new QuitAdapter();

        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        final GLWindow f_glWindow = glWindow;
        glWindow.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e) {
                if( !e.isPrintableKey() || e.isAutoRepeat() ) {
                    return;
                }
                if(e.getKeyChar()=='f') {
                    new InterruptSource.Thread() {
                        public void run() {
                            f_glWindow.setFullscreen(!f_glWindow.isFullscreen());
                    } }.start();
                } else if(e.getKeyChar()=='d') {
                    new InterruptSource.Thread() {
                        public void run() {
                            f_glWindow.setUndecorated(!f_glWindow.isUndecorated());
                    } }.start();
                }
            }
        });

        glWindow.setSize(width, height);
        glWindow.setVisible(true);
        animator.setUpdateFPSFrames(1, null);
        animator.start();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        glWindow.destroy();
    }

    @Test
    public void test01OpaqueDecorated() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(true); // default
        runTestGL(caps, false);
    }

    @Test
    public void test01TransparentDecorated() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(false);
        // This is done implicit now ..
        // caps.setAlphaBits(1);
        runTestGL(caps, false);
    }

    @Test
    public void test01TransparentUndecorated() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBackgroundOpaque(false);
        // This is done implicit now ..
        // caps.setAlphaBits(1);
        runTestGL(caps, true);
    }

    static long duration = 500; // ms

    public static void main(final String args[]) {
        boolean waitForKey = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            }
        }
        if( waitForKey ) {
            JunitTracer.waitForKey("main");
        }
        org.junit.runner.JUnitCore.main(TestTranslucencyNEWT.class.getName());
    }
}
