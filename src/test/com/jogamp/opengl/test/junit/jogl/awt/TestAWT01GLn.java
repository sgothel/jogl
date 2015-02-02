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

import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import java.awt.Frame;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAWT01GLn extends UITestCase {
    @BeforeClass
    public static void startup() {
        System.out.println("GLProfile "+GLProfile.glAvailabilityToString());
    }

    protected void runTestGL(final GLCapabilities caps) throws InterruptedException {
        final Frame frame = new Frame("Texture Test");
        Assert.assertNotNull(frame);

        final GLCanvas glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);

        glCanvas.addGLEventListener(new GearsES2());
        frame.add(glCanvas);

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    // Revalidate size/layout.
                    // Always validate if component added/removed.
                    // Ensure 1st paint of GLCanvas will have a valid size, hence drawable gets created.
                    frame.setSize(512, 512);
                    frame.validate();

                    frame.setVisible(true);
                }});
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }

        glCanvas.display(); // one in process display

        final Animator animator = new Animator(glCanvas);
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
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
    }

    @Test
    public void test01GLDefault() throws InterruptedException {
        final GLProfile glp = GLProfile.getDefault();
        System.out.println("GLProfile Default: "+glp);
        if(glp.isGL2ES2()) {
            final GLCapabilities caps = new GLCapabilities(glp);
            runTestGL(caps);
        } else {
            System.out.println("not a GL2ES2 profile");
        }
    }

    @Test
    public void test02GL2() throws InterruptedException {
        if(GLProfile.isAvailable(GLProfile.GL2)) {
            final GLProfile glprofile = GLProfile.get(GLProfile.GL2);
            System.out.println( "GLProfile GL2: " + glprofile );
            final GLCapabilities caps = new GLCapabilities(glprofile);
            runTestGL(caps);
        } else {
            System.out.println("GL2 n/a");
        }
    }

    @Test
    public void test02ES2() throws InterruptedException {
        if(GLProfile.isAvailable(GLProfile.GLES2)) {
            final GLProfile glprofile = GLProfile.get(GLProfile.GLES2);
            System.out.println( "GLProfile GLES2: " + glprofile );
            final GLCapabilities caps = new GLCapabilities(glprofile);
            runTestGL(caps);
        } else {
            System.out.println("GLES2 n/a");
        }
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestAWT01GLn.class.getName());
    }
}
