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

package com.jogamp.opengl.test.junit.newt.parenting;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.Window;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * Using {@link NewtCanvasAWT#setNEWTChild(Window)} for reparenting, i.e. NEWT/AWT hopping
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParenting04AWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 800;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        width  = 400;
        height = 400;
        glCaps = new GLCapabilities(null);
    }

    @Test
    public void test01WinHopFrame2FrameDirectHop() throws InterruptedException, InvocationTargetException {
        // Will produce some artifacts .. resizing etc
        winHopFrame2Frame(false);
    }

    @Test
    public void test02WinHopFrame2FrameDetachFirst() throws InterruptedException, InvocationTargetException {
        // Note: detaching first setNEWTChild(null) is much cleaner visually
        winHopFrame2Frame(true);
    }

    protected void winHopFrame2Frame(final boolean detachFirst) throws InterruptedException, InvocationTargetException {
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        final Animator anim1 = new Animator(glWindow1);

        final GLWindow glWindow2 = GLWindow.create(glCaps);
        final GLEventListener demo2 = new GearsES2();
        setDemoFields(demo2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);
        final Animator anim2 = new Animator(glWindow2);

        final NewtCanvasAWT canvas1 = new NewtCanvasAWT(glWindow1);
        final NewtCanvasAWT canvas2 = new NewtCanvasAWT(glWindow2);

        final Frame frame1 = new Frame("AWT Parent Frame");
        frame1.setLayout(new BorderLayout());
        frame1.add(new Button("North"), BorderLayout.NORTH);
        frame1.add(new Button("South"), BorderLayout.SOUTH);
        frame1.add(new Button("East"), BorderLayout.EAST);
        frame1.add(new Button("West"), BorderLayout.WEST);
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame1.setSize(width, height);
               frame1.setLocation(0, 0);
               frame1.setVisible(true);
               frame1.validate();
           }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame1.add(canvas1, BorderLayout.CENTER);
               frame1.validate();
           }
        });
        Assert.assertEquals(canvas1.getNativeWindow(),glWindow1.getParent());

        final Frame frame2 = new Frame("AWT Parent Frame");
        frame2.setLayout(new BorderLayout());
        frame2.add(new Button("North"), BorderLayout.NORTH);
        frame2.add(new Button("South"), BorderLayout.SOUTH);
        frame2.add(new Button("East"), BorderLayout.EAST);
        frame2.add(new Button("West"), BorderLayout.WEST);
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame2.setSize(width, height);
               frame2.setLocation(width+50, 0);
               frame2.setVisible(true);
               frame2.validate();
           }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame2.add(canvas2, BorderLayout.CENTER);
               frame2.validate();
           }
        });
        Assert.assertEquals(canvas2.getNativeWindow(),glWindow2.getParent());

        anim1.start();
        anim2.start();

        int state;
        for(state=0; state<3; state++) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    SwingUtilities.invokeAndWait(new Runnable() {
                       public void run() {
                           // 1 -> 2
                           if(detachFirst) {
                               canvas1.setNEWTChild(null);
                               canvas2.setNEWTChild(null);
                           } else {
                               canvas2.setNEWTChild(null);  // free g2 of w2
                           }
                           canvas1.setNEWTChild(glWindow2); // put g2 -> w1. free g1 of w1
                           canvas2.setNEWTChild(glWindow1); // put g1 -> w2
                           frame1.invalidate();
                           frame2.invalidate();
                           frame1.validate();
                           frame2.validate();
                       }
                    });
                    break;
                case 1:
                    SwingUtilities.invokeAndWait(new Runnable() {
                       public void run() {
                           // 2 -> 1
                           if(detachFirst) {
                               canvas1.setNEWTChild(null);
                               canvas2.setNEWTChild(null);
                           } else {
                               canvas2.setNEWTChild(null);
                           }
                           canvas1.setNEWTChild(glWindow1);
                           canvas2.setNEWTChild(glWindow2);
                           frame1.invalidate();
                           frame2.invalidate();
                           frame1.validate();
                           frame2.validate();
                       }
                    });
                    break;
            }
        }

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
                frame1.dispose();
                frame2.dispose();
           } } );
        glWindow1.destroy();
        glWindow2.destroy();
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertEquals(false, glWindow2.isNativeValid());
    }

    public static void setDemoFields(final GLEventListener demo, final GLWindow glWindow, final boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        final Window window = glWindow.getDelegatedWindow();
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        final String tstname = TestParenting04AWT.class.getName();
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
