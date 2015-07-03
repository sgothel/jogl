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

import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import com.jogamp.opengl.test.junit.util.MiscUtils;

import java.awt.Dimension;
import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestIsRealizedConcurrency01AWT extends UITestCase {
    static long durationPerTest = 500; // ms

    @Test
    public void testAddRemove() throws InterruptedException, InvocationTargetException {
        final Dimension f_sz = new Dimension(512, 512);

        final GLCanvas glCanvas = new GLCanvas();
        Assert.assertNotNull(glCanvas);
        glCanvas.addGLEventListener(new GearsES2());

        final Animator animator = new Animator(glCanvas);
        animator.start();

        final Frame frame = new Frame("Frame");
        Assert.assertNotNull(frame);
        frame.add(glCanvas);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setLocation(0, 0);
                frame.setPreferredSize(f_sz);
                frame.setSize(f_sz);
                frame.pack();
                frame.setVisible(true);
            }});
        Thread.sleep(durationPerTest/2);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.remove(glCanvas);
                frame.validate();
                frame.add(glCanvas);
                frame.validate();
            }});
        Thread.sleep(durationPerTest/2);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                glCanvas.destroy();
                frame.dispose();
            }});

        animator.stop();
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            }
        }
        org.junit.runner.JUnitCore.main(TestIsRealizedConcurrency01AWT.class.getName());
    }
}
