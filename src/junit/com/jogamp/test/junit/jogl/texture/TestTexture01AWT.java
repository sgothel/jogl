/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name Sven Gothel or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.test.junit.jogl.texture;

import com.jogamp.test.junit.jogl.util.texture.gl2.TextureGL2ListenerDraw1;

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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTexture01AWT {
    static {
        GLProfile.initSingleton();
    }

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
        GLCanvas glCanvas = new GLCanvas(caps);

        Frame frame = new Frame("Texture Test");
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
        frame.setVisible(false);
        frame.remove(glCanvas);
        glCanvas=null;
        Assert.assertNotNull(frame);
        frame.dispose();
        frame=null;
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
