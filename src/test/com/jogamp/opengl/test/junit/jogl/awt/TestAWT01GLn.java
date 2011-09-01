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
 
package com.jogamp.opengl.test.junit.jogl.awt;

import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import java.awt.Frame;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;


public class TestAWT01GLn extends UITestCase {
    Frame frame=null;
    GLCanvas glCanvas=null;

    @BeforeClass
    public static void startup() {
        System.out.println("GLProfile "+GLProfile.glAvailabilityToString());
    }

    @Before
    public void init() {
        frame = new Frame("Texture Test");
        Assert.assertNotNull(frame);
    }

    @After
    public void release() {
        Assert.assertNotNull(frame);
        Assert.assertNotNull(glCanvas);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.remove(glCanvas);
                    frame.dispose();
                }});
        } catch (Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
        frame=null;
        glCanvas=null;
    }

    protected void runTestGL(GLCapabilities caps) throws InterruptedException {
        glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);
        glCanvas.addGLEventListener(new GearsES2());
        frame.add(glCanvas);

        // Revalidate size/layout.
        // Always validate if component added/removed.
        // Ensure 1st paint of GLCanvas will have a valid size, hence drawable gets created.
        frame.setSize(512, 512);
        frame.validate();

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(true);
                }});
        } catch (Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }

        glCanvas.display(); // one in process display

        Animator animator = new Animator(glCanvas);
        animator.start();

        Thread.sleep(500); // 500 ms

        animator.stop();
    }

    @Test
    public void test01GLDefault() throws InterruptedException {
        GLProfile glp = GLProfile.getDefault();
        System.out.println("GLProfile Default: "+glp);
        GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps);
    }

    @Test
    public void test02GL2() throws InterruptedException {
        GLProfile glprofile = GLProfile.get(GLProfile.GL2);
        System.out.println( "GLProfile GL2: " + glprofile );
        GLCapabilities caps = new GLCapabilities(glprofile);
        runTestGL(caps);
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestAWT01GLn.class.getName());
    }
}
