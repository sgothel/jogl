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

package com.jogamp.opengl.test.junit.newt;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.InvocationTargetException;
import java.awt.Frame;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import javax.media.nativewindow.WindowClosingProtocol;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

public class TestWindowClosingProtocol01AWT extends UITestCase {

    @Test
    public void testCloseFrameGLCanvas() throws InterruptedException, InvocationTargetException {
        final Frame frame = new Frame("testCloseFrameGLCanvas AWT");

        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        final GLCanvas glCanvas = new GLCanvas(caps);
        glCanvas.addGLEventListener(new GearsES2());
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.add(glCanvas);
                frame.pack();
                frame.setSize(512, 512);
                frame.validate();
                frame.setVisible(true);
            } });
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, true));

        //
        // close with op: DO_NOTHING_ON_CLOSE -> NOP (default)
        //
        int op = glCanvas.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingProtocol.DO_NOTHING_ON_CLOSE, op);

        Assert.assertEquals(true, AWTRobotUtil.closeWindow(frame, false)); // nop
        Thread.sleep(100);
        Assert.assertEquals(true, frame.isDisplayable());
        Assert.assertEquals(true,  frame.isVisible());
        Assert.assertEquals(true, glCanvas.isValid());
        Assert.assertEquals(true, glCanvas.isDisplayable());

        //
        // close with op (GLCanvas): DISPOSE_ON_CLOSE -> dispose
        //
        glCanvas.setDefaultCloseOperation(WindowClosingProtocol.DISPOSE_ON_CLOSE);
        op = glCanvas.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingProtocol.DISPOSE_ON_CLOSE, op);

        Assert.assertEquals(true,  AWTRobotUtil.closeWindow(frame, false)); // no frame close
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, false));
        Assert.assertEquals(true,  frame.isDisplayable());
        Assert.assertEquals(true,  frame.isVisible());
        Assert.assertEquals(false, glCanvas.isRealized());

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            } });
    }

    @Test
    public void testCloseJFrameGLCanvas() throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame("testCloseJFrameGLCanvas AWT");

        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        GLCanvas glCanvas = new GLCanvas(caps);
        glCanvas.addGLEventListener(new GearsES2());
        frame.getContentPane().add(glCanvas);
        frame.pack();
        frame.setSize(512, 512);
        frame.validate();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(true);
            } });
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, true));

        //
        // close with op: DO_NOTHING_ON_CLOSE -> NOP / HIDE (default)
        //
        Assert.assertEquals(JFrame.HIDE_ON_CLOSE, frame.getDefaultCloseOperation());
        int op = glCanvas.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingProtocol.DO_NOTHING_ON_CLOSE, op);

        Assert.assertEquals(true,  AWTRobotUtil.closeWindow(frame, false)); // nop
        Thread.sleep(100);
        Assert.assertEquals(true,  frame.isDisplayable());
        Assert.assertEquals(true,  glCanvas.isValid());
        Assert.assertEquals(true,  glCanvas.isDisplayable());

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(true);
            } });
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, true));

        //
        // close with op (JFrame): DISPOSE_ON_CLOSE -- GLCanvas --> dispose
        //
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Assert.assertEquals(JFrame.DISPOSE_ON_CLOSE, frame.getDefaultCloseOperation());
        op = glCanvas.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingProtocol.DISPOSE_ON_CLOSE, op);

        Assert.assertEquals(true,  AWTRobotUtil.closeWindow(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, false));
        Assert.assertEquals(false, frame.isDisplayable());
        Assert.assertEquals(false, glCanvas.isValid());
        Assert.assertEquals(false, glCanvas.isDisplayable());
        Assert.assertEquals(false, glCanvas.isRealized());
    }

    public static void main(String[] args) {
        String tstname = TestWindowClosingProtocol01AWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
