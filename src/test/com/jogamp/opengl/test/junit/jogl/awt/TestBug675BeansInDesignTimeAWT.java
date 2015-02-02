/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.beans.Beans;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug675BeansInDesignTimeAWT extends UITestCase {
    static boolean waitForKey = false;
    static long durationPerTest = 200;

    @Test
    public void test01() throws InterruptedException, InvocationTargetException {
        Beans.setDesignTime(true);

        final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
        final GLCanvas glCanvas = new GLCanvas(caps);
        final Dimension preferredGLSize = new Dimension(400,200);
        glCanvas.setPreferredSize(preferredGLSize);
        glCanvas.setMinimumSize(preferredGLSize);
        glCanvas.setSize(preferredGLSize);

        glCanvas.addGLEventListener(new GearsES2());

        final Window window = new JFrame(this.getSimpleTestName(" - "));
        window.setLayout(new BorderLayout());
        window.add(glCanvas, BorderLayout.CENTER);

        // trigger realization on AWT-EDT, otherwise it won't immediatly ..
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                window.pack();
                window.validate();
                window.setVisible(true);
            }
        } );

        // Immediately displayable after issuing initial setVisible(true) on AWT-EDT!
        Assert.assertTrue("GLCanvas didn't become displayable", glCanvas.isDisplayable());
        if( !Beans.isDesignTime() ) {
            Assert.assertTrue("GLCanvas didn't become realized", glCanvas.isRealized());
        }

        Thread.sleep(durationPerTest);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                window.dispose();
            }
        } );
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atol(args[++i], durationPerTest);
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            }
        }
        if(waitForKey) {
            UITestCase.waitForKey("Start");
        }
        org.junit.runner.JUnitCore.main(TestBug675BeansInDesignTimeAWT.class.getName());
    }
}
