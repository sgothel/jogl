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

package com.jogamp.opengl.test.junit.jogl.util.texture;


import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.test.junit.jogl.demos.TextureDraw01Accessor;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureDraw01ES2Listener;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.TextureDraw01GL2Listener;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.awt.GLCanvas;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.spi.TextureProvider;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Unit test for bug 417, which shows a GLException when reading a grayscale texture.
 * Couldn't duplicate the failure, so it must have been fixed unknowingly sometime
 * after the bug was submitted.
 * @author Wade Walker, et.al.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPNGTextureFromFileAWT extends UITestCase {
    static boolean showFPS = false;
    static long duration = 100; // ms
    InputStream grayTextureStream;
    InputStream testTextureStream;

    @BeforeClass
    public static void initClass() {
    }

    @Before
    public void initTest() throws IOException {
        grayTextureStream = TestPNGTextureFromFileAWT.class.getResourceAsStream( "grayscale_texture.png" );
        Assert.assertNotNull(grayTextureStream);
        {
            final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscN_3-01-160x90.png", this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(testTextureUrlConn);
            testTextureStream = testTextureUrlConn.getInputStream();
            Assert.assertNotNull(testTextureStream);
        }
    }

    @After
    public void cleanupTest() {
        grayTextureStream = null;
        testTextureStream = null;
    }

    public void testImpl(final boolean useFFP, final InputStream istream, final boolean useAWTIIOP)
            throws InterruptedException, IOException
    {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        GLProfile glp;
        if(useFFP && GLProfile.isAvailable(GLProfile.GL2)) {
            glp = GLProfile.getMaxFixedFunc(true);
        } else if(!useFFP && GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.getGL2ES2();
        } else {
            System.err.println(getSimpleTestName(".")+": GLProfile n/a, useFFP: "+useFFP);
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        final TextureData texData;
        if(useAWTIIOP) {
            final TextureProvider texProvider = new com.jogamp.opengl.util.texture.spi.awt.IIOTextureProvider();
            texData = texProvider.newTextureData(glp, istream, 0 /* internalFormat */, 0 /* pixelFormat */, false /* mipmap */, TextureIO.PNG);
        } else {
            texData = TextureIO.newTextureData(glp, istream, false /* mipmap */, TextureIO.PNG);
        }
        System.err.println("TextureData: "+texData);

        final GLCanvas glc = new GLCanvas(caps);
        final Dimension glc_sz = new Dimension(texData.getWidth(), texData.getHeight());
        glc.setMinimumSize(glc_sz);
        glc.setPreferredSize(glc_sz);
        final Frame frame = new Frame("TestPNGTextureGL2FromFileAWT");
        Assert.assertNotNull(frame);
        frame.add(glc);

        // load texture from file inside current GL context to match the way
        // the bug submitter was doing it
        final GLEventListener gle = useFFP ? new TextureDraw01GL2Listener( texData ) : new TextureDraw01ES2Listener( texData, 0 ) ;
        glc.addGLEventListener(gle);
        glc.addGLEventListener(new GLEventListener() {
            boolean shot = false;

            @Override public void init(final GLAutoDrawable drawable) {}

            @Override
            public void display(final GLAutoDrawable drawable) {
                // 1 snapshot
                if(null!=((TextureDraw01Accessor)gle).getTexture() && !shot) {
                    shot = true;
                    snapshot(0, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
                }
            }

            @Override public void dispose(final GLAutoDrawable drawable) { }
            @Override public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
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

        final Animator animator = new Animator(glc);
        animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
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
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    @Test
    public void testGrayAWTILoaderGL2() throws InterruptedException, IOException {
        testImpl(true, grayTextureStream, true);
    }
    @Test
    public void testGrayAWTILoaderES2() throws InterruptedException, IOException {
        testImpl(false, grayTextureStream, true);
    }

    @Test
    public void testGrayPNGJLoaderGL2() throws InterruptedException, IOException {
        testImpl(true, grayTextureStream, false);
    }
    @Test
    public void testGrayPNGJLoaderES2() throws InterruptedException, IOException {
        testImpl(false, grayTextureStream, false);
    }

    @Test
    public void testTestAWTILoaderGL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStream, true);
    }
    @Test
    public void testTestAWTILoaderES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream, true);
    }

    @Test
    public void testTestPNGJLoaderGL2() throws InterruptedException, IOException {
        testImpl(true, testTextureStream, false);
    }
    @Test
    public void testTestPNGJLoaderES2() throws InterruptedException, IOException {
        testImpl(false, testTextureStream, false);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestPNGTextureFromFileAWT.class.getName());
    }
}
