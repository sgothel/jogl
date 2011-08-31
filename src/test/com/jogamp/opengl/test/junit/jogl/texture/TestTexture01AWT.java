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
 
package com.jogamp.opengl.test.junit.jogl.texture;

import com.jogamp.opengl.test.junit.jogl.util.texture.gl2.TextureGL2ListenerDraw1;

import com.jogamp.opengl.test.junit.util.UITestCase;

import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import com.jogamp.opengl.util.Animator;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.IOException;
import org.junit.Assert;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTexture01AWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    BufferedImage textureImage;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.get(GLProfile.GL2GL3);
        Assert.assertNotNull(glp);
        caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
    }

    @Before
    public void initTest() {
        // create base image
        BufferedImage baseImage = new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR);
        Assert.assertNotNull(baseImage);
        Graphics2D g = baseImage.createGraphics();
        Assert.assertNotNull(g);
        g.setPaint(new GradientPaint(0, 0, Color.CYAN,
                                 baseImage.getWidth(), baseImage.getHeight(), Color.BLUE));
        g.fillRect(0, 0, baseImage.getWidth(), baseImage.getHeight());
        g.dispose();

        // create texture image
        int imageType = BufferedImage.TYPE_3BYTE_BGR;
        textureImage = new BufferedImage(baseImage.getWidth(),
                                         baseImage.getHeight(),
                                         imageType);
        Assert.assertNotNull(textureImage);
        g = textureImage.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(baseImage, 0, 0, null);
        g.dispose();

        baseImage.flush();
        baseImage=null;
    }

    @After
    public void cleanupTest() {
        Assert.assertNotNull(textureImage);
        textureImage.flush();
        textureImage=null;
    }

    @Test
    public void test1() throws InterruptedException {
        final GLCanvas glCanvas = new GLCanvas(caps);
        final Frame frame = new Frame("Texture Test");
        Assert.assertNotNull(frame);
        frame.add(glCanvas);
        frame.setSize(512, 512);

        // create texture    
        TextureData textureData = AWTTextureIO.newTextureData(caps.getGLProfile(), textureImage, false);
        glCanvas.addGLEventListener(new TextureGL2ListenerDraw1(textureData));

        Animator animator = new Animator(glCanvas);
        frame.setVisible(true);
        animator.start();

        Thread.sleep(500); // 500 ms

        animator.stop();
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.remove(glCanvas);
                    frame.dispose();
                }});
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }                
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestTexture01AWT.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }
}
