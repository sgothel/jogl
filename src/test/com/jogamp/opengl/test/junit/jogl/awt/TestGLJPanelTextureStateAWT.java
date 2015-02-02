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
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.awt.GLJPanel;
import javax.swing.JFrame;

import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.TextureState;
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
    static long duration = 250; // ms

    @BeforeClass
    public static void initClass() {
    }

    static void setFrameSize(final JFrame frame, final boolean frameLayout, final java.awt.Dimension new_sz) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setSize(new_sz);
                    if( frameLayout ) {
                        frame.validate();
                    }
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
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
        // final GLCanvas glc = new GLCanvas(caps);
        final Dimension glc_sz = new Dimension(640, 480);
        final Dimension glc_sz2 = new Dimension(800, 400);
        glc.setMinimumSize(glc_sz);
        glc.setPreferredSize(glc_sz);
        final JFrame frame = new JFrame("TestGLJPanelTextureStateAWT");
        Assert.assertNotNull(frame);
        frame.getContentPane().add(glc);

        final GLEventListener gle0;
        {
            final GearsES2 gle0sub = new GearsES2( 0 );
            // gle1sub.setClearBuffers(false);
            final TextureDraw02ES2ListenerFBO demo = new TextureDraw02ES2ListenerFBO(gle0sub, 1, texUnit ) ;
            demo.setKeepTextureBound(keepTextureBound);
            demo.setClearBuffers(false);
            gle0 = demo;
        }

        final GLEventListener gle1;
        {
            final RedSquareES2 demo = new RedSquareES2( 1 ) ;
            demo.setClearBuffers(false);
            gle1 = demo;
        }

        final boolean[] glelError = { false };

        glc.addGLEventListener(new GLEventListener() {
            int gle0X, gle0Y, gle0W, gle0H;
            int gle1X, gle1Y, gle1W, gle1H;
            int tX, tY, tW, tH;
            int shot = 0;

            void setupTex(final GL gl) {
                // Note: FBObject uses diff defaults, i.e.: GL_NEAREST and GL_CLAMP_TO_EDGE
                if( keepTextureBound ) {
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
                }
            }

            @Override
            public void init(final GLAutoDrawable drawable) {
                // Initialize w/ arbitrary values !
                final GL2ES2 gl = drawable.getGL().getGL2ES2();
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
            public void dispose(final GLAutoDrawable drawable) {
                gle0.dispose(drawable);
                gle1.dispose(drawable);
            }
            @Override
            public void display(final GLAutoDrawable drawable) {
                final GL2ES2 gl = drawable.getGL().getGL2ES2();

                gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

                // test viewport
                {
                    final int[] viewport = new int[] { 0, 0, 0, 0 };
                    gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
                    if( gle1X != viewport[0] || gle1Y != viewport[1] || gle1W != viewport[2] || gle1H != viewport[3] ) {
                        final String msg = "Expected "+gle1X+"/"+gle1Y+" "+gle1W+"x"+gle1H+
                                            ", actual "+viewport[0]+"/"+viewport[1]+" "+viewport[2]+"x"+viewport[3];
                        Assert.assertTrue("Viewport not restored: "+msg, false);
                        glelError[0] = true;
                    }
                }

                gl.glViewport(gle0X, gle0Y, gle0W, gle0H);
                gle0.display(drawable);

                gl.glViewport(gle1X, gle1Y, gle1W, gle1H);
                gle1.display(drawable);

                shot++;
                if( 4 == shot ) {
                    gl.glViewport(tX, tY, tW, tH);
                    snapshot(0, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
                    gl.glViewport(gle1X, gle1Y, gle1W, gle1H); // restore viewport test
                }

                final TextureState ts = new TextureState(drawable.getGL(), GL.GL_TEXTURE_2D); // as set via gle0!
                // System.err.println("XXX: "+ts);
                Assert.assertEquals("Texture unit changed", GL.GL_TEXTURE0+texUnit, ts.getUnit());
                if( keepTextureBound ) {
                    Assert.assertEquals("Texture mag-filter changed: "+ts, GL.GL_LINEAR, ts.getMagFilter());
                    Assert.assertEquals("Texture mag-filter changed: "+ts, GL.GL_LINEAR, ts.getMinFilter());
                    Assert.assertEquals("Texture wrap-s changed: "+ts, GL.GL_REPEAT, ts.getWrapS());
                    Assert.assertEquals("Texture wrap-t changed: "+ts, GL.GL_REPEAT, ts.getWrapT());
                    glelError[0] = GL.GL_LINEAR != ts.getMagFilter() || GL.GL_LINEAR != ts.getMinFilter() ||
                                   GL.GL_REPEAT != ts.getWrapS()     || GL.GL_REPEAT != ts.getWrapT();
                }
            }
            final int border = 5;
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                gle0X = x + border;
                gle0Y = y;
                gle0W = width/2 - 2*border;
                gle0H = height;
                // System.err.println("GLEL0 "+gle0X+"/"+gle0Y+" "+gle0W+"x"+gle0H);

                gle1X = gle0X + gle0W + 2*border;
                gle1Y = y;
                gle1W = width/2 - 2*border;
                gle1H = height;
                // System.err.println("GLEL1 "+gle1X+"/"+gle1Y+" "+gle1W+"x"+gle1H);

                tX = x;
                tY = y;
                tW = width;
                tH = height;
                // System.err.println("Total "+tX+"/"+tY+" "+tW+"x"+tH);

                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                gl.glViewport(gle0X, gle0Y, gle0W, gle0H);
                gle0.reshape(drawable, 0, 0, gle0W, gle0H); // don't 'skip' about gle0X/gle0Y

                gl.glViewport(gle1X, gle1Y, gle1W, gle1H);
                gle1.reshape(drawable, 0, 0, gle1W, gle1H); // don't 'skip' about gle0X/gle0Y

                if( keepTextureBound ) {
                    setupTex(gl);
                }
            }
        });

        final QuitAdapter quitAdapter = new QuitAdapter();
        new com.jogamp.newt.event.awt.AWTKeyAdapter(quitAdapter, glc).addTo(glc);
        new com.jogamp.newt.event.awt.AWTWindowAdapter(quitAdapter, glc).addTo(glc);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.pack();
                    frame.setVisible(true);
                }});
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        Assert.assertTrue("Component didn't become visible", AWTRobotUtil.waitForVisible(glc, true));
        Assert.assertTrue("Component didn't become realized", AWTRobotUtil.waitForRealized(glc, true));
        Thread.sleep(100);
        setFrameSize(frame, true, glc_sz2);
        System.err.println("window resize pos/siz: "+glc.getX()+"/"+glc.getY()+" "+glc.getSurfaceWidth()+"x"+glc.getSurfaceHeight());
        Thread.sleep(100);

        final long t0 = System.currentTimeMillis();
        while(!quitAdapter.shouldQuit() && System.currentTimeMillis()-t0<duration) {
            glc.display();
            Thread.sleep(100);
        }

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.remove(glc);
                    frame.dispose();
                }});
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        Assume.assumeFalse("Error occured in GLEL .. see log file above", glelError[0]);
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

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestGLJPanelTextureStateAWT.class.getName());
    }
}
