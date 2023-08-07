/**
 * Copyright 2013-2023 JogAmp Community. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Remove and re-add a GLJPanel from its Swing parent
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLJPanelReadd01Bug1310AWT extends UITestCase {

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton();
    }

    static final Dimension gljPanelSize = new Dimension(800, 600);
    static GLCapabilitiesImmutable caps = null;
    static long duration = 500; // ms

    public void test(final GLCapabilitiesImmutable caps, final GLEventListener demo) {
        final JFrame[] frame = { null };
        final JPanel[] container = { null };
        final GLJPanel[] glJPanel = { null };
        final FPSAnimator animator = new FPSAnimator(60);

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                @SuppressWarnings("serial")
                @Override
                public void run() {
                    final JFrame _frame = new JFrame("Testing");
                    frame[0] = _frame;
                    _frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    final Container content = _frame.getContentPane();
                    content.setLayout(new BorderLayout());

                    final JToolBar toolbar = new JToolBar();

                    final JPanel _container = new JPanel();
                    container[0] = _container;
                    _container.setLayout(new GridLayout(1, 1));
                    final GLJPanel _glJPanel = new GLJPanel(caps);
                    glJPanel[0] = _glJPanel;
                    _glJPanel.addGLEventListener(demo);
                    _glJPanel.setPreferredSize(gljPanelSize);
                    _container.add(_glJPanel);
                    animator.add(_glJPanel);

                    toolbar.add(new AbstractAction("Remove and add") {
                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            _container.removeAll();
                            _container.add(_glJPanel);
                            _glJPanel.invalidate();
                            _glJPanel.repaint();
                        }
                    });

                    content.add(toolbar, BorderLayout.NORTH);
                    content.add(_container, BorderLayout.CENTER);

                    _frame.pack();
                    _frame.setLocationRelativeTo(null);
                    _frame.setVisible(true);
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        animator.start();

        try {
            Thread.sleep(500);
        } catch (final InterruptedException e1) {
            e1.printStackTrace();
        }
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    container[0].removeAll();
                    container[0].add(glJPanel[0]);
                    glJPanel[0].invalidate();
                    glJPanel[0].repaint();

                }
            });
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        try {
            Thread.sleep(duration);
        } catch (final InterruptedException e1) {
            e1.printStackTrace();
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        frame[0].dispose();
                    } } );
        } catch (final Exception e1) {
            e1.printStackTrace();
        }
    }

    @Test
    public void test00() throws InterruptedException, InvocationTargetException {
        // test(new GLCapabilities(null), new RedSquareES2());
        test(new GLCapabilities(null), new MyRotTriangle());
    }

    public static void main(final String[] args) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestGLJPanelReadd01Bug1310AWT.class.getName());
    }

    class MyRotTriangle implements GLEventListener {
      private final GLReadBufferUtil screenshot;
      private int sn = 0;

      private double theta = 0;
      private double s = 0;
      private double c = 0;
      private boolean doScreenshot = false;

      public MyRotTriangle() {
          screenshot = new GLReadBufferUtil(true, false);
      }
      @Override
      public void display(final GLAutoDrawable drawable) {
        update();
        render(drawable);
        if( doScreenshot ) {
            snapshot(sn++, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
            doScreenshot = false;
        }
      }

      @Override
      public void dispose(final GLAutoDrawable drawable) {
          System.err.println("GLEL dispose");
      }

      @Override
      public void init(final GLAutoDrawable drawable) {
          System.err.println("GLEL init, swap-ival "+drawable.getGL().getSwapInterval());
          theta = 0;
          s = 0;
          c = 0;
          doScreenshot = true;
      }

      @Override
      public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int w, final int h) {
      }

      private void update() {
        theta += 0.01;
        s = Math.sin(theta);
        c = Math.cos(theta);
      }

      private void render(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        // draw a triangle filling the window
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glColor3f(1, 0, 0);
        gl.glVertex2d(-c, -c);
        gl.glColor3f(0, 1, 0);
        gl.glVertex2d(0, c);
        gl.glColor3f(0, 0, 1);
        gl.glVertex2d(s, -s);
        gl.glEnd();
      }
    }
}
