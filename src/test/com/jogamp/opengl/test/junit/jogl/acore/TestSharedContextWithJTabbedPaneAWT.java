/**
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 *
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextWithJTabbedPaneAWT extends UITestCase {

    static class DemoInstance {
        protected static GLCapabilities getCaps() {
            final GLCapabilities caps = new GLCapabilities(GLProfile.getMaxFixedFunc(true));

            caps.setAlphaBits(8);
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            caps.setDepthBits(24);
            caps.setDoubleBuffered(true);

            return caps;
        }

        int[] bufferId;

        @SuppressWarnings("serial")
        class SharedGLPanel extends JPanel implements GLEventListener {
            final GLCanvas canvas;
            final boolean shared;

            public SharedGLPanel(final GLCanvas shareWith, final int width, final int height) {
                final GLContext sharedCtx = shareWith != null ? shareWith.getContext() : null;
                System.err.println("XXX WWPanel: shareWith "+shareWith+", sharedCtx "+sharedCtx);
                canvas = new GLCanvas(getCaps()); // same caps for 1st and 2nd shared ctx !
                if( null != shareWith) {
                    canvas.setSharedAutoDrawable(shareWith);
                    shared = true;
                } else {
                    shared = false;
                }
                canvas.setSize(new java.awt.Dimension(width, height));

                setLayout(new BorderLayout(5, 5));
                add(canvas, BorderLayout.CENTER);
                setOpaque(false);

                canvas.addGLEventListener(this);
            }

            @Override
            public void init(final GLAutoDrawable glAutoDrawable) {
                if (!shared) {
                    Assert.assertNull("Buffer is set, but instance is share master", bufferId);
                    makeVBO(glAutoDrawable);
                    System.err.println("XXX Create Buffer "+bufferId[0]);
                } else {
                    Assert.assertNotNull("Buffer is not set, but instance is share slave", bufferId);
                    Assert.assertTrue("Context is not shared", glAutoDrawable.getContext().isShared());
                    System.err.println("XXX Reuse Buffer "+bufferId[0]);
                }
                final GL2 gl = glAutoDrawable.getGL().getGL2();
                if( shared ) {
                    gl.glColor3f(1, 1, 1);
                    gl.glClearColor(0.3f, 0.3f, 0.3f, 1f);
                } else {
                    gl.glColor3f(0, 0, 0);
                    gl.glClearColor(1f, 1f, 1f, 1f);
                }
                gl.glShadeModel(GLLightingFunc.GL_FLAT);
            }

            @Override
            public void dispose(final GLAutoDrawable glAutoDrawable) {}

            @Override
            public void display(final GLAutoDrawable glAutoDrawable) {
                final GL2 gl = glAutoDrawable.getGL().getGL2();

                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

                gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId[0]);
                gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
                gl.glDrawArrays(GL.GL_LINES, 0, 2);
            }

            @Override
            public void reshape(final GLAutoDrawable glAutoDrawable, final int i, final int i1, final int i2, final int i3) {
                final int w = getWidth();
                final int h = getHeight();

                final GL2 gl = glAutoDrawable.getGL().getGL2();

                gl.glViewport(0, 0, w, h);
                gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
                gl.glLoadIdentity();
                gl.glOrtho(0, 1, 0, 1, -1, 1);
                gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
                gl.glLoadIdentity();
            }
        }

        protected void makeVBO(final GLAutoDrawable drawable) {
            final GL2 gl = drawable.getGL().getGL2();

            bufferId = new int[1];
            gl.glGenBuffers(1, bufferId, 0);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferId[0]);

            final FloatBuffer vertices = Buffers.newDirectFloatBuffer(6);
            vertices.put(0).put(0).put(0);
            vertices.put(1).put(1).put(0);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.capacity() * 4, vertices.rewind(), GL.GL_STATIC_DRAW);
        }

        public JTabbedPane tabbedPanel;

        public DemoInstance(final JFrame f) {
            try
            {
                GLProfile.initSingleton(); // Lets have init debug messages above below marker
                System.err.println("XXX START DEMO XXX");

                // Create the application frame and the tabbed pane and add the pane to the frame.
                tabbedPanel = new JTabbedPane();
                f.add(tabbedPanel, BorderLayout.CENTER);

                // Create two World Windows that share resources.
                final SharedGLPanel wwpA = new SharedGLPanel(null, 600, 600);
                final SharedGLPanel wwpB = new SharedGLPanel(wwpA.canvas, wwpA.getWidth(), wwpA.getHeight());

                tabbedPanel.add(wwpA, "Window A");
                tabbedPanel.add(wwpB, "Window B");

                // Add the card panel to the frame.
                f.add(tabbedPanel, BorderLayout.CENTER);

                // Position and display the frame.
                f.setTitle("Multi-Window Tabbed Pane");
                f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                f.pack();
                f.setResizable(true);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    static long durationPerTest = 500*4; // ms
    static boolean manual = false;

    @Test
    public void test01() throws InterruptedException, InvocationTargetException {
        final JFrame f = new JFrame();
        f.setTitle("Shared GLContext AWT GLCanvas JTabbedPane");
        final DemoInstance demo = new DemoInstance(f);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                System.err.println("XXX SetVisible XXX");
                f.setVisible(true);
            } });

      if(manual) {
          for(long w=durationPerTest; w>0; w-=100) {
              Thread.sleep(100);
          }
      } else {
          javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                demo.tabbedPanel.setSelectedIndex(0);
            }});
          Thread.sleep(durationPerTest/4);

          javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                demo.tabbedPanel.setSelectedIndex(1);
            }});
          Thread.sleep(durationPerTest/4);

          javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                demo.tabbedPanel.setSelectedIndex(0);
            }});
          Thread.sleep(durationPerTest/4);

          javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                demo.tabbedPanel.setSelectedIndex(1);
            }});
          Thread.sleep(durationPerTest/4);
      }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                System.err.println("XXX SetVisible XXX");
                f.dispose();
            } });
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        org.junit.runner.JUnitCore.main(TestSharedContextWithJTabbedPaneAWT.class.getName());
    }
}
