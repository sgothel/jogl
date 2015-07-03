/**
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
 package com.jogamp.opengl.test.junit.jogl.demos.gl2.awt;

import com.jogamp.opengl.*;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.gl2.GLUgl2;

import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test for bug 450, which causes the right part of the frame to be black
 * for all x >= height.
 *
 * Draws the Gears demo in a window that's twice as wide than it is tall,
 * and checks to see if a particular pixel in the right half of the frame
 * is colored.
 *
 * @author Wade Walker (adapted from TestGearsGLJPanelAWT)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLJPanelAWTBug450 extends UITestCase {
    static GLProfile glp;
    static int width, height;
    static int r_x, r_y;
    /** Set this if test fails. Needed because we can't throw an exception
     * all the way up the stack from where we test the pixel. */
    static boolean failed;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getGL2ES2();
        Assert.assertNotNull(glp);
        height = 256;
        width  = 2*height;
        r_x    = 5*height/4; // 5/8 * width
        r_y    =   height/2;
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(final GLCapabilities caps)
            throws AWTException, InterruptedException, InvocationTargetException
    {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        final JFrame frame = new JFrame("Swing GLJPanel");
        Assert.assertNotNull(frame);

        final GLJPanel glJPanel = new GLJPanel(caps);
        Assert.assertNotNull(glJPanel);
        final RedSquareES2 demo = new RedSquareES2();
        demo.setAspect((float)width/(float)height);
        demo.setDoRotation(false);
        glJPanel.addGLEventListener(demo);
        glJPanel.addGLEventListener(new GLEventListener() {
            int f = 0;
            @Override
            public void init(final GLAutoDrawable drawable) {
                // drawable.getGL().glClearColor(0, 0, 1, 1);
            }
            @Override
            public void display(final GLAutoDrawable drawable) {
                // look at one pixel at the bottom of the frame, just right of
                // the center line, and make sure it's not black
                final GL2 gl = GLUgl2.getCurrentGL2();
                final ByteBuffer bytebuffer = ByteBuffer.allocateDirect( 3 );
                gl.glReadPixels( r_x, r_y, 1, 1, GL2GL3.GL_BGR, GL.GL_UNSIGNED_BYTE, bytebuffer );
                final byte byte0 = bytebuffer.get( 0 );
                final byte byte1 = bytebuffer.get( 1 );
                final byte byte2 = bytebuffer.get( 2 );
                if( (byte0 == 0) && (byte1 == 0) && (byte2 == 0) ) {
                    failed = true;
                }
                if(0 == f) {
                    System.err.println("BGR ("+r_x+"/"+r_y+"): "+byte0+", "+byte1+", "+byte2+" - OK "+(!failed));
                    snapshot(f, null, gl, screenshot, TextureIO.PNG, null);
                }
                f++;
            }
            @Override
            public void dispose(final GLAutoDrawable drawable) {}
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        });

        final FPSAnimator animator = new FPSAnimator(glJPanel, 60);

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.getContentPane().add(glJPanel, BorderLayout.CENTER);
                    frame.setSize(width, height);
                    frame.setVisible(true);
                } } ) ;

        animator.setUpdateFPSFrames(1, null);
        animator.start();
        Assert.assertEquals(true, animator.isAnimating());

        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glJPanel);
        Assert.assertNotNull(animator);

        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.getContentPane().remove(glJPanel);
                    frame.remove(glJPanel);
                    glJPanel.destroy();
                    frame.dispose();
                } } );

        Assert.assertFalse( failed );
    }

    @Test
    public void test01()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        runTestGL(caps);
    }

    static long duration = 500; // ms

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestGLJPanelAWTBug450.class.getName());
    }
}
