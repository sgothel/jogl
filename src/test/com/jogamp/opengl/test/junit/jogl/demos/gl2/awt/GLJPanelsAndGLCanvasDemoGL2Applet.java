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
package com.jogamp.opengl.test.junit.jogl.demos.gl2.awt;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.awt.GLJPanel;
import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.SwingConstants;

import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Teapot;
import com.jogamp.opengl.util.Animator;

/**
 */
public class GLJPanelsAndGLCanvasDemoGL2Applet extends JApplet {

    private static final long serialVersionUID = 1L;

    private Animator[] animator;

  public static JFrame frame;
  public static JPanel appletHolder;
  public static boolean isApplet = true;

  static public void main(final String args[]) {
    isApplet = false;

    final JApplet myApplet = new GLJPanelsAndGLCanvasDemoGL2Applet();

    appletHolder = new JPanel();

    frame = new JFrame("Bug818GLJPanelApplet");
    frame.getContentPane().add(myApplet);

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(final WindowEvent e) {
        System.exit(0);
      }
    });

    try {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                myApplet.init();
                frame.validate();
                frame.pack();
                frame.setVisible(true);
            } } );
    } catch( final Throwable throwable ) {
        throwable.printStackTrace();
    }

    myApplet.start();
  }


    @Override
    public void init() {

        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));
        System.err.println("Pre  Orientation L2R: "+panel.getComponentOrientation().isLeftToRight());
        panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        System.err.println("Post Orientation L2R: "+panel.getComponentOrientation().isLeftToRight());
        setContentPane(panel);

        animator = new Animator[3*2];
        int animIdx = 0;

        panel.add(new JLabel("GLJPanel Auto V-Flip", SwingConstants.CENTER));
        {
            {
                final GLJPanel gljPanel = new GLJPanel();
                gljPanel.addGLEventListener(new Teapot());
                animator[animIdx++] = new Animator(gljPanel);
                gljPanel.setPreferredSize(new Dimension(300, 300));
                panel.add(gljPanel);
            }
            {
                final GLJPanel gljPanel = new GLJPanel();
                gljPanel.addGLEventListener(new Gears(0));
                animator[animIdx++] = new Animator(gljPanel);
                gljPanel.setPreferredSize(new Dimension(300, 300));
                panel.add(gljPanel);
            }
        }
        panel.add(new JLabel("GLJPanel User V-Flip", SwingConstants.CENTER));
        {
            {
                final GLJPanel gljPanel = new GLJPanel();
                gljPanel.setSkipGLOrientationVerticalFlip(true);
                gljPanel.addGLEventListener(new Teapot());
                animator[animIdx++] = new Animator(gljPanel);
                gljPanel.setPreferredSize(new Dimension(300, 300));
                panel.add(gljPanel);
            }
            {
                final GLJPanel gljPanel = new GLJPanel();
                gljPanel.setSkipGLOrientationVerticalFlip(true);
                gljPanel.addGLEventListener(new Gears(0));
                animator[animIdx++] = new Animator(gljPanel);
                gljPanel.setPreferredSize(new Dimension(300, 300));
                panel.add(gljPanel);
            }
        }

        panel.add(new JLabel("GLCanvas", SwingConstants.CENTER));
        {
            {
                final GLCanvas glCanvas = new GLCanvas();
                glCanvas.addGLEventListener(new Teapot());
                animator[animIdx++] = new Animator(glCanvas);
                glCanvas.setPreferredSize(new Dimension(300, 300));
                panel.add(glCanvas);
            }
            {
                final GLCanvas glCanvas = new GLCanvas();
                glCanvas.addGLEventListener(new Gears(1));
                animator[animIdx++] = new Animator(glCanvas);
                glCanvas.setPreferredSize(new Dimension(300, 300));
                panel.add(glCanvas);
            }
        }
    }

    @Override
    public void start() {
        for(int i=0; i<animator.length; i++) {
            animator[i].start();
            animator[i].setUpdateFPSFrames(60, System.err);
        }
    }

    @Override
    public void stop() {
        for(int i=0; i<animator.length; i++) {
            animator[i].stop();
        }
    }

    @Override
    public void destroy() {}
}

