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
 
package com.jogamp.opengl.test.junit.jogl.demos.gl2.gears;

import javax.media.opengl.*;

import com.jogamp.opengl.util.FPSAnimator;
import javax.media.opengl.awt.GLJPanel;

import com.jogamp.opengl.test.junit.util.UITestCase;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestGearsGLJPanelAWT extends UITestCase {
    static GLProfile glp;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(false);
        glp = GLProfile.getDefault();
        Assert.assertNotNull(glp);
        width  = 512;
        height = 512;
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(GLCapabilities caps)
            throws AWTException, InterruptedException, InvocationTargetException
    {
        JFrame frame = new JFrame("Swing GLJPanel");
        Assert.assertNotNull(frame);

        GLJPanel glJPanel = new GLJPanel(caps);
        Assert.assertNotNull(glJPanel);
        glJPanel.addGLEventListener(new Gears());

        FPSAnimator animator = new FPSAnimator(glJPanel, 60);

        final JFrame _frame = frame;
        final GLJPanel _glJPanel = glJPanel;
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    _frame.getContentPane().add(_glJPanel, BorderLayout.CENTER);
                    _frame.setSize(512, 512);
                    _frame.setVisible(true);
                } } ) ;

        animator.setUpdateFPSFrames(1, null);        
        animator.start();
        Assert.assertEquals(true, animator.isAnimating());

        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glJPanel);
        Assert.assertNotNull(animator);

        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    _frame.setVisible(false);
                    _frame.getContentPane().remove(_glJPanel);
                    _frame.remove(_glJPanel);
                    _glJPanel.destroy();
                    _frame.dispose();
                } } );
    }

    @Test
    public void test01()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        runTestGL(caps);
    }

    static long duration = 500; // ms

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestGearsGLJPanelAWT.class.getName());
    }
}
