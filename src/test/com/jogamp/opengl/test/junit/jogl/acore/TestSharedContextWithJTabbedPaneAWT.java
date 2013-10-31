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

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
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
            GLCapabilities caps = new GLCapabilities(GLProfile.getMaxFixedFunc(true));

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

            public SharedGLPanel(GLCanvas shareWith, int width, int height) {
                GLContext sharedCtx = shareWith != null ? shareWith.getContext() : null;
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
            public void init(GLAutoDrawable glAutoDrawable) {
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
                gl.glShadeModel(GL2.GL_FLAT);
            }

            @Override
            public void dispose(GLAutoDrawable glAutoDrawable) {}

            @Override
            public void display(GLAutoDrawable glAutoDrawable) {
                final GL2 gl = glAutoDrawable.getGL().getGL2();

                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

                gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferId[0]);
                gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);
                gl.glDrawArrays(GL2.GL_LINES, 0, 2);
            }

            @Override
            public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {
                int w = getWidth();
                int h = getHeight();

                final GL2 gl = glAutoDrawable.getGL().getGL2();

                gl.glViewport(0, 0, w, h);
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadIdentity();
                gl.glOrtho(0, 1, 0, 1, -1, 1);
                gl.glMatrixMode(GL2.GL_MODELVIEW);
                gl.glLoadIdentity();
            }
        }

        protected void makeVBO(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();

            bufferId = new int[1];
            gl.glGenBuffers(1, bufferId, 0);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferId[0]);

            FloatBuffer vertices = Buffers.newDirectFloatBuffer(6);
            vertices.put(0).put(0).put(0);
            vertices.put(1).put(1).put(0);
            gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertices.capacity() * 4, vertices.rewind(), GL2.GL_STATIC_DRAW);
        }

        public JTabbedPane tabbedPanel;

        public DemoInstance(JFrame f) {
            try
            {
                GLProfile.initSingleton(); // Lets have init debug messages above below marker
                System.err.println("XXX START DEMO XXX");

                // Create the application frame and the tabbed pane and add the pane to the frame.
                tabbedPanel = new JTabbedPane();
                f.add(tabbedPanel, BorderLayout.CENTER);

                // Create two World Windows that share resources.
                SharedGLPanel wwpA = new SharedGLPanel(null, 600, 600);
                SharedGLPanel wwpB = new SharedGLPanel(wwpA.canvas, wwpA.getWidth(), wwpA.getHeight());

                tabbedPanel.add(wwpA, "Window A");
                tabbedPanel.add(wwpB, "Window B");

                // Add the card panel to the frame.
                f.add(tabbedPanel, BorderLayout.CENTER);

                // Position and display the frame.
                f.setTitle("Multi-Window Tabbed Pane");
                f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                f.pack();
                f.setResizable(true);
            } catch (Exception e) {
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

    public static void main(String args[]) {
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
