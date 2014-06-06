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

package com.jogamp.opengl.test.junit.jogl.awt;


import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureDraw02ES2ListenerFBO;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;

import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.TextureState;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;

import java.awt.Dimension;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Unit test for bug 826, test {@link GLJPanel}'s {@link TextureState} save and restore.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLJPanelTextureStateAWT extends UITestCase {
    static boolean showFPS = false;
    static long duration = 100; // ms

    @BeforeClass
    public static void initClass() {
    }

    public void testImpl(final boolean keepTextureBound, final int texUnit)
            throws InterruptedException, IOException
    {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        GLProfile glp;
        if(GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.getGL2ES2();
        } else {
            System.err.println(getSimpleTestName(".")+": GLProfile n/a");
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);

        final GLJPanel glc = new GLJPanel(caps);
        Dimension glc_sz = new Dimension(800, 400);
        glc.setMinimumSize(glc_sz);
        glc.setPreferredSize(glc_sz);
        final JFrame frame = new JFrame("TestGLJPanelTextureStateAWT");
        Assert.assertNotNull(frame);
        frame.getContentPane().add(glc);

        final TextureDraw02ES2ListenerFBO gle0;
        {
            final GearsES2 gle0sub = new GearsES2( 0 );
            // gle1sub.setClearBuffers(false);
            gle0 = new TextureDraw02ES2ListenerFBO(gle0sub, 1, texUnit ) ;
        }
        gle0.setKeepTextureBound(keepTextureBound);
        gle0.setClearBuffers(false);

        final RedSquareES2 gle1 = new RedSquareES2( 1 ) ;
        gle1.setClearBuffers(false);

        glc.addGLEventListener(new GLEventListener() {
            int gle0X, gle0Y, gle0W, gle0H;
            int gle1X, gle1Y, gle1W, gle1H;
            int tX, tY, tW, tH;
            int shot = 0;

            void setupTex(GL gl) {
                // Note: FBObject uses diff defaults, i.e.: GL_NEAREST and GL_CLAMP_TO_EDGE
                if( keepTextureBound ) {
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
                }
            }

            @Override
            public void init(GLAutoDrawable drawable) {
                // Initialize w/ arbitrary values !
                GL2ES2 gl = drawable.getGL().getGL2ES2();
                gl.glActiveTexture(GL.GL_TEXTURE0 + 1);
                gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
                gl.glActiveTexture(GL.GL_TEXTURE0 + 0);
                gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);

                gle0.init(drawable);
                gle1.init(drawable);
                setupTex(gl);
            }

            @Override
            public void dispose(GLAutoDrawable drawable) {
                gle0.dispose(drawable);
                gle1.dispose(drawable);
            }
            @Override
            public void display(GLAutoDrawable drawable) {
                GL2ES2 gl = drawable.getGL().getGL2ES2();

                gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

                // restore viewport test
                final int[] viewport = new int[] { 0, 0, 0, 0 };
                gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
                if( gle0X != viewport[0] || gle0Y != viewport[1] || gle0W != viewport[2] || gle0H != viewport[3] ) {
                    final String msg = "Expected "+viewport[0]+"/"+viewport[1]+" "+viewport[2]+"x"+viewport[3]+
                                        ", actual "+gle0X+"/"+gle0Y+" "+gle0W+"x"+gle0H;
                    Assert.assertTrue("Viewport not restored: "+msg, false);
                }

                // gl.glViewport(gle0X, gle0Y, gle0W, gle0H); // restore viewport test
                gle0.display(drawable);

                gl.glViewport(gle1X, gle1Y, gle1W, gle1H);
                gle1.display(drawable);

                shot++;
                if( 4 == shot ) {
                    gl.glViewport(tX, tY, tW, tH);
                    snapshot(0, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
                }

                gl.glViewport(gle0X, gle0Y, gle0W, gle0H); // restore viewport test

                final TextureState ts = new TextureState(drawable.getGL(), GL.GL_TEXTURE_2D);
                // System.err.println("XXX: "+ts);
                Assert.assertEquals("Texture unit changed", GL.GL_TEXTURE0+texUnit, ts.getUnit());
                if( keepTextureBound ) {
                    Assert.assertEquals("Texture mag-filter changed", GL.GL_LINEAR, ts.getMagFilter());
                    Assert.assertEquals("Texture mag-filter changed", GL.GL_LINEAR, ts.getMinFilter());
                    Assert.assertEquals("Texture wrap-s changed", GL.GL_REPEAT, ts.getWrapS());
                    Assert.assertEquals("Texture wrap-t changed", GL.GL_REPEAT, ts.getWrapT());
                }
            }
            final int border = 5;
            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
                gle0X = x + border;
                gle0Y = y;
                gle0W = width/2 - 2*border;
                gle0H = height;

                gle1X = gle0X + gle0W + 2*border;
                gle1Y = y;
                gle1W = width/2 - 2*border;
                gle1H = height;

                tX = x;
                tY = y;
                tW = width;
                tH = height;

                GL2ES2 gl = drawable.getGL().getGL2ES2();
                gl.glViewport(gle0X, gle0Y, gle0W, gle0H);
                gle0.reshape(drawable, gle0X, gle0Y, gle0W, gle0H);

                gl.glViewport(gle1X, gle1Y, gle1W, gle1H);
                gle1.reshape(drawable, gle1X, gle1Y, gle1W, gle1H);

                gl.glViewport(gle0X, gle0Y, gle0W, gle0H); // restore viewport test

                if( keepTextureBound ) {
                    setupTex(gl);
                }
            }
        });

        Animator animator = new Animator(glc);
        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        final QuitAdapter quitAdapter = new QuitAdapter();
        new com.jogamp.newt.event.awt.AWTKeyAdapter(quitAdapter, glc).addTo(glc);
        new com.jogamp.newt.event.awt.AWTWindowAdapter(quitAdapter, glc).addTo(glc);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.pack();
                    frame.setVisible(true);
                }});
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }

        animator.start();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.remove(glc);
                    frame.dispose();
                }});
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    @Test
    public void test01_texUnit0_keepTex0_ES2() throws InterruptedException, IOException {
        testImpl(false /* keepTextureBound */, 0 /* texUnit */);
    }
    @Test
    public void test02_texUnit0_keepTex1_ES2() throws InterruptedException, IOException {
        testImpl(true /* keepTextureBound */, 0 /* texUnit */);
    }
    @Test
    public void test03_texUnit1_keepTex1_ES2() throws InterruptedException, IOException {
        testImpl(true /* keepTextureBound */, 1 /* texUnit */);
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestGLJPanelTextureStateAWT.class.getName());
    }
}
