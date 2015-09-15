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

package com.jogamp.opengl.test.junit.jogl.demos.es1.newt;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.es1.GearsES1;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGearsES1NEWT extends UITestCase {
    static int width, height;
    static boolean forceES2 = false;
    static boolean forceFFPEmu = false;
    static int swapInterval = 1;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(final GLCapabilities caps, final boolean forceFFPEmu) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Gears NEWT Test");

        final GearsES1 demo = new GearsES1(swapInterval);
        demo.setForceFFPEmu(forceFFPEmu, forceFFPEmu, false, false);
        glWindow.addGLEventListener(demo);
        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        glWindow.addGLEventListener(snap);

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
        snap.setMakeSnapshot();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        glWindow.destroy();
    }

    @Test
    public void test00() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(forceES2 ? GLProfile.get(GLProfile.GLES2) : GLProfile.getGL2ES1());
        runTestGL(caps, forceFFPEmu);
    }

    static long duration = 500; // ms

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-ffpemu")) {
                forceFFPEmu = true;
            }
        }
        org.junit.runner.JUnitCore.main(TestGearsES1NEWT.class.getName());
    }
}
