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

package com.jogamp.opengl.test.junit.jogl.awt;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.util.FPSAnimator;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import com.jogamp.opengl.test.junit.util.MiscUtils;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Insets;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLCanvasAWTActionDeadlock00AWT extends UITestCase {
    static long durationPerTest = 1000; // ms
    static final int width = 512;
    static final int height = 512;

    GLEventListener gle1 = null;
    GLEventListener gle2 = null;

    @Test
    public void test01Animator() throws InterruptedException {
        testImpl(new Animator(), 0, false);
    }

    @Test
    public void test02FPSAnimator() throws InterruptedException {
        testImpl(new FPSAnimator(30), 0, false);
    }

    @Test
    public void test02FPSAnimator_RestartOnAWTEDT() throws InterruptedException {
        testImpl(new FPSAnimator(30), 100, false);
    }

    /** May crash due to invalid thread usage, i.e. non AWT-EDT
    @Test
    public void test02FPSAnimator_RestartOnCurrentThread() throws InterruptedException {
        testImpl(new FPSAnimator(30), 100, true);
    } */

    void testImpl(final AnimatorBase animator, final int restartPeriod, final boolean restartOnCurrentThread) throws InterruptedException {
        final Frame frame1 = new Frame("Frame 1");
        gle1 = new GLEventListener() {
            @Override
            public void init(final GLAutoDrawable drawable) {
            }

            @Override
            public void dispose(final GLAutoDrawable drawable) {
            }

            @Override
            public void display(final GLAutoDrawable drawable) {
                frame1.setTitle("f "+frameCount+", fps "+animator.getLastFPS());
                frameCount++;
            }

            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
            }
        };
        gle2 = new GearsES2();

        Assert.assertNotNull(frame1);
        {
            final Insets insets = frame1.getInsets();
            final int w = width + insets.left + insets.right;
            final int h = height + insets.top + insets.bottom;
            frame1.setSize(w, h);
        }
        frame1.setLocation(0, 0);
        frame1.setTitle("Generic Title");

        GLCanvas glCanvas = createGLCanvas();
        glCanvas.addGLEventListener(gle1);
        glCanvas.addGLEventListener(gle2);

        animator.setUpdateFPSFrames(60, System.err);
        animator.add(glCanvas);
        animator.start();

        attachGLCanvas(frame1, glCanvas, false);

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame1.setVisible(true);
                }});
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }

        final long sleep = 0 < restartPeriod ? restartPeriod : 100;
        long togo = durationPerTest;
        while( 0 < togo ) {
            if(0 < restartPeriod) {
                glCanvas = restart(frame1, glCanvas, restartOnCurrentThread);
            }

            Thread.sleep(sleep);

            togo -= sleep;
        }

        dispose(frame1, glCanvas);
        animator.stop();

        gle1 = null;
        gle2 = null;
    }

    void dispose(final Frame frame, final GLCanvas glCanvas) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    glCanvas.destroy();
                    frame.dispose();
                }});
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
    }

    GLCanvas restart(final Frame frame, GLCanvas glCanvas, final boolean restartOnCurrentThread) throws InterruptedException {
        glCanvas.disposeGLEventListener(gle1, true);
        glCanvas.disposeGLEventListener(gle2, true);
        detachGLCanvas(frame, glCanvas, restartOnCurrentThread);

        glCanvas = createGLCanvas();

        attachGLCanvas(frame, glCanvas, restartOnCurrentThread);
        glCanvas.addGLEventListener(gle1);
        glCanvas.addGLEventListener(gle2);

        return glCanvas;
    }

    void attachGLCanvas(final Frame frame, final GLCanvas glCanvas, final boolean restartOnCurrentThread) {
        System.err.println("*** attachGLCanvas.0 on-current-thread "+restartOnCurrentThread+", currentThread "+Thread.currentThread().getName());
        if( restartOnCurrentThread ) {
            frame.setLayout(new BorderLayout());
            frame.add(glCanvas, BorderLayout.CENTER);
            frame.validate();
        } else {
            try {
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.setLayout(new BorderLayout());
                        frame.add(glCanvas, BorderLayout.CENTER);
                        frame.validate();
                    }});
            } catch (final Throwable t) {
                t.printStackTrace();
                Assume.assumeNoException(t);
            }
        }
        System.err.println("*** attachGLCanvas.X");
    }

    void detachGLCanvas(final Frame frame, final GLCanvas glCanvas, final boolean restartOnCurrentThread) {
        System.err.println("*** detachGLCanvas.0 on-current-thread "+restartOnCurrentThread+", currentThread "+Thread.currentThread().getName());
        if( restartOnCurrentThread ) {
            frame.remove(glCanvas);
            frame.validate();
        } else {
            try {
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.remove(glCanvas);
                        frame.validate();
                    }});
            } catch (final Throwable t) {
                t.printStackTrace();
                Assume.assumeNoException(t);
            }
        }
        System.err.println("*** detachGLCanvas.X");
    }

    int frameCount = 0;

    GLCanvas createGLCanvas() {
        System.err.println("*** createGLCanvas.0");
        final GLCanvas glCanvas = new GLCanvas();
        glCanvas.setBounds(0, 0, width, height);
        Assert.assertNotNull(glCanvas);
        System.err.println("*** createGLCanvas.X");
        return glCanvas;
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            }
        }
        org.junit.runner.JUnitCore.main(TestGLCanvasAWTActionDeadlock00AWT.class.getName());
    }
}
