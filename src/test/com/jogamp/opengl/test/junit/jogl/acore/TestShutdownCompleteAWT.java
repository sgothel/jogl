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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestShutdownCompleteAWT extends UITestCase {

    static long duration = 300; // ms

    protected void runTestGL() throws InterruptedException, InvocationTargetException {
        final Frame frame = new Frame("Gears AWT Test");
        Assert.assertNotNull(frame);

        final GLCanvas glCanvas = new GLCanvas(new GLCapabilities(GLProfile.getGL2ES2()));
        Assert.assertNotNull(glCanvas);
        frame.add(glCanvas);

        glCanvas.addGLEventListener(new GearsES2(1));

        final Animator animator = new Animator(glCanvas);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setSize(256, 256);
                frame.setVisible(true);
            }});

        animator.setUpdateFPSFrames(60, System.err);
        animator.start();
        Assert.assertEquals(true, animator.isAnimating());
        Assert.assertEquals(true, glCanvas.isVisible());
        Assert.assertEquals(true, glCanvas.isDisplayable());

        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        Assert.assertEquals(true, glCanvas.isRealized());

        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(false);
            }});
        Assert.assertEquals(false, frame.isVisible());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.remove(glCanvas);
                frame.dispose();
            }});
    }

    @AfterClass
    public static void afterAll() {
        if(waitForKey) {
            UITestCase.waitForKey("Exit");
        }
    }

    protected void oneLife(final boolean glInfo) throws InterruptedException, InvocationTargetException {
        final long t0 = Platform.currentTimeMicros();
        GLProfile.initSingleton();
        final long t1 = Platform.currentTimeMicros();
        if(!initOnly) {
            runTestGL();
        }
        final long t2 = Platform.currentTimeMicros();
        if(glInfo) {
            System.err.println(JoglVersion.getDefaultOpenGLInfo(null, null, false).toString());
        }
        final long t3 = Platform.currentTimeMicros();
        GLProfile.shutdown();
        final long t4 = Platform.currentTimeMicros();
        System.err.println("Total:                          "+ (t4-t0)/1e3 +"ms");
        System.err.println("  GLProfile.initSingleton():    "+ (t1-t0)/1e3 +"ms");
        System.err.println("  Demo Code:                    "+ (t2-t1)/1e3 +"ms");
        System.err.println("  GLInfo:                       "+ (t3-t2)/1e3 +"ms");
        System.err.println("  GLProfile.shutdown():         "+ (t4-t3)/1e3 +"ms");
    }

    @Test
    public void test01OneLife() throws InterruptedException, InvocationTargetException {
        oneLife(false);
    }

    @Test
    public void test02AnotherLifeWithGLInfo() throws InterruptedException, InvocationTargetException {
        oneLife(true);
    }

    @Test
    public void test03AnotherLife() throws InterruptedException, InvocationTargetException {
        oneLife(true);
    }

    @Test
    public void test03TwoLifes() throws InterruptedException, InvocationTargetException {
        oneLife(false);
        oneLife(false);
    }

    static boolean initOnly = false;
    static boolean waitForKey = false;

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-initOnly")) {
                initOnly = true;
            }
        }

        if(waitForKey) {
            UITestCase.waitForKey("Start");
        }
        final String tstname = TestShutdownCompleteAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
