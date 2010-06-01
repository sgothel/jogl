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

import java.lang.reflect.InvocationTargetException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.JFrame;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assume.*;
import static org.junit.Assert.*;
import static javax.swing.SwingUtilities.*;

public class TestAWT01GLn {

    private Window[] windows;


    @BeforeClass
    public static void startup() {
        System.out.println("GLProfile <static> "+GLProfile.glAvailabilityToString());
    }

    @Before
    public void init() {
        windows = new Window[]{
            new Window(null),
            new Frame("JFrame GL test"),
            new JFrame("Frame GL test")
        };
    }

    @After
    public void release() throws InterruptedException, InvocationTargetException {
        invokeAndWait(new Runnable() {
            public void run() {
                boolean allReleased = true;
                for (final Window window : windows) {
                    window.setVisible(false);
                    try {
                        window.removeAll();
                    } catch (Throwable t) {
                        allReleased = false;
                        t.printStackTrace();
                    }
                    window.dispose();
                }
                assumeTrue(allReleased);
            }
        });
    }



    protected void runTestGL(final GLCapabilities caps) throws InterruptedException, InvocationTargetException {

        for (final Window window : windows) {

            System.out.println("testing with "+window.getClass().getName());

            // final array as mutable container hack
            final GLCanvas[] glCanvas = new GLCanvas[1];

            invokeAndWait(new Runnable() {
                public void run() {
                    
                    glCanvas[0] = new GLCanvas(caps);
                    glCanvas[0].addGLEventListener(new Gears());

                    window.add(glCanvas[0]);
                    window.setSize(512, 512);
                    glCanvas[0].display(); // one in process display

                    window.setVisible(true);

                }

            });

            Animator animator = new Animator(glCanvas[0]);
            animator.start();
            Thread.sleep(500);
            animator.stop();

        }
    }

    @Test
    public void test01GLDefault() throws InterruptedException, InvocationTargetException {
        GLProfile glp = GLProfile.getDefault();
        System.out.println("GLProfile Default: "+glp);
        GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps);
    }

    @Test
    public void test03GLMaxFixed() throws InterruptedException, InvocationTargetException {
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
                assumeNoException(t);
             }
             // else .. serious unexpected exception
        }
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestAWT01GLn.class.getName());
    }
}
