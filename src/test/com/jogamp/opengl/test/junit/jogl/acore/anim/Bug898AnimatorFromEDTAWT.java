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

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
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
        GLCanvas panel = new GLCanvas(new GLCapabilities(GLProfile.getMaxProgrammable(true)));
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
            public void init(GLAutoDrawable glad) {
                startTime = System.currentTimeMillis();
            }

            @Override
            public void dispose(GLAutoDrawable glad) {
            }

            @Override
            public void display(GLAutoDrawable glad) {
                long time = System.currentTimeMillis();
                if (animator.isAnimating() && step * 2000 < time - startTime) {
                    long td = time - lastTime;
                    lastTime = time;
                    animator.pause();
                    System.out.println(Thread.currentThread().getName()+": #"+step+" "+td+" ms: animator.pause(): paused "+animator);
                    new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    long td = System.currentTimeMillis() - lastTime;
                                    if (animator.isPaused()) {
                                        animator.resume(); //Doesn't work on v2.0.2 or higher
                                        System.out.println(Thread.currentThread().getName()+": #"+step+" "+td+" ms: animator.resume(): animating "+animator);
                                    } else {
                                        System.out.println(Thread.currentThread().getName()+": #"+step+" "+td+" ms: animator.resume(): Ooops - not paused! - animating "+animator);
                                    }
                                } } );
                        }
                    }.start();
                    step++;
                }
            }

            @Override
            public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {
            }
        });
        //Start animation
        animator.start();
        System.out.println("animator.start()");
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Bug898AnimatorFromEDTAWT().setVisible(true);
            }
        });
    }
}
