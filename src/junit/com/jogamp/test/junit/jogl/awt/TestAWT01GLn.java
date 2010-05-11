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

package com.jogamp.test.junit.jogl.awt;

import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;
import java.awt.Frame;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;

public class TestAWT01GLn {
    Frame frame=null;
    GLCanvas glCanvas=null;

    @BeforeClass
    public static void startup() {
        System.out.println("GLProfile <static> "+GLProfile.glAvailabilityToString());
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
        frame.setVisible(false);
        try {
            frame.remove(glCanvas);
        } catch (Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
        frame.dispose();
        frame=null;
        glCanvas=null;
    }

    protected void runTestGL(GLCapabilities caps) throws InterruptedException {
        glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);
        frame.add(glCanvas);
        frame.setSize(512, 512);

        glCanvas.addGLEventListener(new Gears());

        glCanvas.display(); // one in process display 

        Animator animator = new Animator(glCanvas);
        frame.setVisible(true);
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
    public void test03GLMaxFixed() throws InterruptedException {
        GLProfile maxFixed = GLProfile.getMaxFixedFunc();
        System.out.println("GLProfile MaxFixed: "+maxFixed);
        GLCapabilities caps = new GLCapabilities(maxFixed);
        try {
            runTestGL(caps);
        } catch (Throwable t) {
             // FIXME: 
             // Stop test and ignore if GL3bc and GL4bc
             // currently this won't work on ATI!
             if(maxFixed.equals(GLProfile.GL3bc) ||
                maxFixed.equals(GLProfile.GL4bc)) {
                t.printStackTrace();
                Assume.assumeNoException(t);
             }
             // else .. serious unexpected exception
        }
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestAWT01GLn.class.getName());
    }
}
