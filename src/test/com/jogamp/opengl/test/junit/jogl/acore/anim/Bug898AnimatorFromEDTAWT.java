/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.jogl.acore.anim;

import java.awt.BorderLayout;
import java.awt.Dimension;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.util.Animator;

/**
 * Manual test case to validate Animator pause/resume on AWT-EDT.
 * <p>
 * Even though (AWT) Animator is not able to block until pause/resume is finished
 * when issued on AWT-EDT, best effort shall be made to preserve functionality.
 * </p>
 * Original Author: <i>kosukek</i> from JogAmp forum; Modifier a bit.
 */
@SuppressWarnings("serial")
public class Bug898AnimatorFromEDTAWT extends javax.swing.JFrame {

    public Bug898AnimatorFromEDTAWT() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        //Layout
        setMinimumSize(new Dimension(640, 480));
        getContentPane().setLayout(new BorderLayout());
        final GLCanvas panel = new GLCanvas(new GLCapabilities(GLProfile.getMaxProgrammable(true)));
        getContentPane().add(panel, BorderLayout.CENTER);
        pack();
        //Animator
        final Animator animator = new Animator();
        animator.add(panel);
        //GLEventListener
        panel.addGLEventListener(new GearsES2(1));
        panel.addGLEventListener(new GLEventListener() {
            long startTime = 0, lastTime = 0;
            long step = 1;

            @Override
            public void init(final GLAutoDrawable glad) {
                startTime = System.currentTimeMillis();
            }

            @Override
            public void dispose(final GLAutoDrawable glad) {
            }

            @Override
            public void display(final GLAutoDrawable glad) {
                final long time = System.currentTimeMillis();
                if (animator.isAnimating() && step * 2000 < time - startTime) {
                    final long td = time - lastTime;
                    lastTime = time;
                    animator.pause();
                    System.out.println(Thread.currentThread().getName()+": #"+step+" "+td+" ms: animator.pause(): paused "+animator);
                    new InterruptSource.Thread() {
                        public void run() {
                            try {
                                java.lang.Thread.sleep(1000);
                            } catch (final InterruptedException e) {
                                e.printStackTrace();
                            }
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    final long td = System.currentTimeMillis() - lastTime;
                                    if (animator.isPaused()) {
                                        animator.resume(); //Doesn't work on v2.0.2 or higher
                                        System.out.println(java.lang.Thread.currentThread().getName()+": #"+step+" "+td+" ms: animator.resume(): animating "+animator);
                                    } else {
                                        System.out.println(java.lang.Thread.currentThread().getName()+": #"+step+" "+td+" ms: animator.resume(): Ooops - not paused! - animating "+animator);
                                    }
                                } } );
                        }
                    }.start();
                    step++;
                }
            }

            @Override
            public void reshape(final GLAutoDrawable glad, final int i, final int i1, final int i2, final int i3) {
            }
        });
        //Start animation
        animator.start();
        System.out.println("animator.start()");
    }

    public static void main(final String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Bug898AnimatorFromEDTAWT().setVisible(true);
            }
        });
    }
}
