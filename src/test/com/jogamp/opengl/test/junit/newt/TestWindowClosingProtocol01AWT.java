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
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.Assert;

import java.lang.reflect.InvocationTargetException;
import java.awt.Frame;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil.WindowClosingListener;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestWindowClosingProtocol01AWT extends UITestCase {

    @Test
    public void testCloseFrameGLCanvas() throws InterruptedException, InvocationTargetException {
        final Frame frame = new Frame("testCloseFrameGLCanvas AWT");
        final WindowClosingListener closingListener = AWTRobotUtil.addClosingListener(frame);
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
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
        WindowClosingMode op = glCanvas.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingMode.DO_NOTHING_ON_CLOSE, op);

        Assert.assertEquals(true, AWTRobotUtil.closeWindow(frame, false, closingListener)); // nop
        Thread.sleep(100);
        Assert.assertEquals(true,  frame.isDisplayable());
        Assert.assertEquals(true,  frame.isVisible());
        Assert.assertEquals(true,  glCanvas.isValid());
        Assert.assertEquals(true,  glCanvas.isDisplayable());
        Assert.assertEquals(true,  closingListener.isWindowClosing());
        Assert.assertEquals(false, closingListener.isWindowClosed());

        //
        // close with op (GLCanvas): DISPOSE_ON_CLOSE -> dispose
        //
        glCanvas.setDefaultCloseOperation(WindowClosingMode.DISPOSE_ON_CLOSE);
        op = glCanvas.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingMode.DISPOSE_ON_CLOSE, op);

        Thread.sleep(300);

        Assert.assertEquals(true,  AWTRobotUtil.closeWindow(frame, false, closingListener)); // no frame close, but GLCanvas's GL resources will be destroyed
        Thread.sleep(100);
        Assert.assertEquals(true,  frame.isDisplayable());
        Assert.assertEquals(true,  frame.isVisible());
        Assert.assertEquals(true,  closingListener.isWindowClosing());
        Assert.assertEquals(false, closingListener.isWindowClosed());
        for (int wait=0; wait<AWTRobotUtil.POLL_DIVIDER && glCanvas.isRealized(); wait++) {
            Thread.sleep(AWTRobotUtil.TIME_SLICE);
        }
        Assert.assertEquals(false, glCanvas.isRealized());

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            } });
    }

    @Test
    public void testCloseJFrameGLCanvas() throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame("testCloseJFrameGLCanvas AWT");
        final WindowClosingListener closingListener = AWTRobotUtil.addClosingListener(frame);

        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        final GLCanvas glCanvas = new GLCanvas(caps);
        glCanvas.addGLEventListener(new GearsES2());
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.getContentPane().add(glCanvas);
                frame.pack();
                frame.setSize(512, 512);
                frame.validate();
                frame.setVisible(true);
            } });
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, true));

        //
        // close with op: DO_NOTHING_ON_CLOSE -> NOP / HIDE (default)
        //
        Assert.assertEquals(WindowConstants.HIDE_ON_CLOSE, frame.getDefaultCloseOperation());
        WindowClosingMode op = glCanvas.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingMode.DO_NOTHING_ON_CLOSE, op);

        Thread.sleep(300);

        Assert.assertEquals(true,  AWTRobotUtil.closeWindow(frame, false, closingListener)); // hide
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, false)); // hide -> invisible
        Assert.assertEquals(true,  frame.isDisplayable());
        Assert.assertEquals(false, frame.isVisible());
        Assert.assertEquals(true,  glCanvas.isValid());
        Assert.assertEquals(true,  glCanvas.isDisplayable());

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(true);
            } });
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, true));
        Assert.assertEquals(true,  frame.isDisplayable());
        Assert.assertEquals(true,  frame.isVisible());

        //
        // close with op (JFrame): DISPOSE_ON_CLOSE -- GLCanvas --> dispose
        //
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Assert.assertEquals(WindowConstants.DISPOSE_ON_CLOSE, frame.getDefaultCloseOperation());
        op = glCanvas.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingMode.DISPOSE_ON_CLOSE, op);

        Assert.assertEquals(true,  AWTRobotUtil.closeWindow(frame, true, closingListener));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, false));
        Assert.assertEquals(false, frame.isDisplayable());
        Assert.assertEquals(false, glCanvas.isValid());
        Assert.assertEquals(false, glCanvas.isDisplayable());
        Assert.assertEquals(false, glCanvas.isRealized());
    }

    public static void main(final String[] args) {
        final String tstname = TestWindowClosingProtocol01AWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
