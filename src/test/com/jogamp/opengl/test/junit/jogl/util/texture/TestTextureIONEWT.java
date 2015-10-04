/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.test.junit.jogl.demos.TextureDraw01Accessor;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureDraw01ES2Listener;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.TextureDraw01GL2Listener;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.ImageType;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTextureIONEWT extends UITestCase {
    static long duration = 100; // ms

    ImageTstFiles imageTstFiles;

    @Before
    public void initTest() throws IOException {
        imageTstFiles = new ImageTstFiles();
        imageTstFiles.init();
    }

    @After
    public void cleanupTest() {
        imageTstFiles.clear();
    }

    public void testImpl(final List<ImageTstFiles.NamedInputStream> streams, final ImageType expImageType) throws InterruptedException, IOException {
        for(int i=0; i<streams.size(); i++) {
            final ImageTstFiles.NamedInputStream s = streams.get(i);
            System.err.printf("Test %3d: path %s, exp-type %s%n", i, s.basePath, expImageType);
            testImpl(s.stream, expImageType);
        }
    }
    public void testImpl(final InputStream istream, final ImageType expImageType) throws InterruptedException, IOException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        final GLProfile glp = GLProfile.isAvailable(GLProfile.GL2ES2) ? GLProfile.getGL2ES2() : GLProfile.getDefault();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(1);

        final TextureData texData = TextureIO.newTextureData(glp, istream, false /* mipmap */, expImageType.type);
        System.err.println("TextureData: "+texData);
        Assert.assertEquals(expImageType, texData.getSourceImageType());

        final GLWindow glad = GLWindow.create(caps);
        glad.setTitle("TestTextureIONEWT."+expImageType.type);
        // Size OpenGL to Video Surface
        glad.setSize(texData.getWidth(), texData.getHeight());

        // load texture from file inside current GL context to match the way
        // the bug submitter was doing it
        final GLEventListener gle = glp.isGL2ES2() ? new TextureDraw01ES2Listener( texData, 0 ) : new TextureDraw01GL2Listener( texData ) ;
        glad.addGLEventListener(gle);
        glad.addGLEventListener(new GLEventListener() {
            boolean shot = false;

            @Override public void init(final GLAutoDrawable drawable) {}

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

        final Animator animator = new Animator(glad);
        animator.setUpdateFPSFrames(60, null);
        final QuitAdapter quitAdapter = new QuitAdapter();
        glad.addKeyListener(quitAdapter);
        glad.addWindowListener(quitAdapter);
        glad.setVisible(true);
        animator.start();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        glad.destroy();
    }

    @Test
    public void test01AllPNG() throws InterruptedException, IOException {
        testImpl(imageTstFiles.pngStreams, new ImageType(ImageType.T_PNG));
    }

    @Test
    public void test02AllJPG() throws InterruptedException, IOException {
        testImpl(imageTstFiles.jpgStreams, new ImageType(ImageType.T_JPG));
    }

    @Test
    public void test03AllTGA() throws InterruptedException, IOException {
        testImpl(imageTstFiles.tgaStreams, new ImageType(ImageType.T_TGA));
    }

    @Test
    public void test04AllDDS() throws InterruptedException, IOException {
        testImpl(imageTstFiles.ddsStreams, new ImageType(ImageType.T_DDS));
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestTextureIONEWT.class.getName());
    }
}
