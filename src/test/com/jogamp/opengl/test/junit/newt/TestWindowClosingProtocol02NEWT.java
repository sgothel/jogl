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

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import org.junit.Assert;

import javax.media.nativewindow.WindowClosingProtocol;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

public class TestWindowClosingProtocol02NEWT extends UITestCase {

    @Test
    public void testCloseGLWindow() throws InterruptedException, InvocationTargetException {
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        final GLWindow glWindow = GLWindow.create(caps);
        final AWTRobotUtil.WindowClosingListener windowClosingListener = AWTRobotUtil.addClosingListener(glWindow);

        glWindow.addGLEventListener(new GearsES2());
        glWindow.setSize(512, 512);
        glWindow.setVisible(true);
        Assert.assertEquals(true, glWindow.isVisible());

        // CHECK DEFAULT ..
        int op = glWindow.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingProtocol.DISPOSE_ON_CLOSE, op);

        //
        // close with op: DO_NOTHING_ON_CLOSE -> NOP
        //
        glWindow.setDefaultCloseOperation(WindowClosingProtocol.DO_NOTHING_ON_CLOSE);
        op = glWindow.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingProtocol.DO_NOTHING_ON_CLOSE, op);

        Assert.assertEquals(true, AWTRobotUtil.closeWindow(glWindow, false)); // nop
        Assert.assertEquals(true, glWindow.isNativeValid());
        Assert.assertEquals(true, windowClosingListener.isWindowClosing());
        windowClosingListener.reset();

        //
        // close with op (GLCanvas): DISPOSE_ON_CLOSE -> dispose
        //
        glWindow.setDefaultCloseOperation(WindowClosingProtocol.DISPOSE_ON_CLOSE);
        op = glWindow.getDefaultCloseOperation();
        Assert.assertEquals(WindowClosingProtocol.DISPOSE_ON_CLOSE, op);

        Assert.assertEquals(true,  AWTRobotUtil.closeWindow(glWindow, true));
        Assert.assertEquals(false, glWindow.isNativeValid());
        Assert.assertEquals(true,  windowClosingListener.isWindowClosing());
    }

    public static void main(String[] args) {
        String tstname = TestWindowClosingProtocol02NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
