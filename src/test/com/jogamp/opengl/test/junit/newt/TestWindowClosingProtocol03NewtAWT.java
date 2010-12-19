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

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
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

import com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

public class TestWindowClosingProtocol03NewtAWT extends UITestCase {

    static class NEWTWindowClosingAdapter extends WindowAdapter {
        volatile boolean windowDestroyNotifyReached = false;

        public void reset() {
            windowDestroyNotifyReached = false;
        }
        public boolean windowDestroyNotifyReached() {
            return windowDestroyNotifyReached;
        }
        public void windowDestroyNotify(WindowEvent e) {
            windowDestroyNotifyReached = true;
        }
    }

    @Test
    public void testCloseJFrameNewtCanvasAWT() throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame("testCloseJFrameNewtCanvasAWT");

        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        final GLWindow glWindow = GLWindow.create(caps);
        final NEWTWindowClosingAdapter newtWindowClosingAdapter = new NEWTWindowClosingAdapter();

        glWindow.addWindowListener(newtWindowClosingAdapter);
        glWindow.addGLEventListener(new Gears());

        NewtCanvasAWT newtCanvas = new NewtCanvasAWT(glWindow);

        frame.getContentPane().add(newtCanvas);
        frame.pack();
        frame.setSize(512, 512);
        frame.validate();
        frame.setVisible(true);
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(frame, true));

        //
        // close with op: DO_NOTHING_ON_CLOSE -> NOP / HIDE (default)
        //
        Assert.assertEquals(JFrame.HIDE_ON_CLOSE, frame.getDefaultCloseOperation());
        int op = newtCanvas.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingProtocol.DO_NOTHING_ON_CLOSE, op);

        AWTRobotUtil.closeWindow(frame);
        Assert.assertEquals(false, frame.isVisible());
        Assert.assertEquals(true,  frame.isDisplayable());
        Assert.assertEquals(true,  newtCanvas.isValid());
        Assert.assertEquals(true,  newtCanvas.isVisible());
        Assert.assertEquals(true,  newtCanvas.isDisplayable());
        Assert.assertEquals(true,  glWindow.isValid());
        Assert.assertEquals(false, glWindow.isVisible());
        Assert.assertEquals(true,  glWindow.isNativeValid());
        Assert.assertEquals(true,  newtWindowClosingAdapter.windowDestroyNotifyReached());
        newtWindowClosingAdapter.reset();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame.setVisible(true);
            } });
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(frame, true));

        //
        // close with op (JFrame): DISPOSE_ON_CLOSE -- newtCanvas -- glWindow --> dispose
        //
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Assert.assertEquals(JFrame.DISPOSE_ON_CLOSE, frame.getDefaultCloseOperation());
        op = newtCanvas.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingProtocol.DISPOSE_ON_CLOSE, op);

        AWTRobotUtil.closeWindow(frame);
        Assert.assertEquals(false, frame.isVisible());
        Assert.assertEquals(false, frame.isDisplayable());
        Assert.assertEquals(false, newtCanvas.isValid());
        Assert.assertEquals(true,  newtCanvas.isVisible());
        Assert.assertEquals(false, newtCanvas.isDisplayable());
        Assert.assertEquals(true,  glWindow.isValid());
        Assert.assertEquals(false, glWindow.isVisible());
        Assert.assertEquals(false, glWindow.isNativeValid());
        Assert.assertEquals(true,  newtWindowClosingAdapter.windowDestroyNotifyReached());
    }

    public static void main(String[] args) {
        String tstname = TestWindowClosingProtocol03NewtAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
