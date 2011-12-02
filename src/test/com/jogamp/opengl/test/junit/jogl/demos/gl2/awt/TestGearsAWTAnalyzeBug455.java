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
 
package com.jogamp.opengl.test.junit.jogl.demos.gl2.awt;

import javax.media.opengl.*;

import com.jogamp.opengl.util.Animator;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;

import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

public class TestGearsAWTAnalyzeBug455 extends UITestCase {
    static long duration = 500; // ms
    static boolean waitForKey = false; // for manual profiling
    static boolean altSwap = true;     // using alternate GL swap method (copy pixel) no [auto-]swap

    static GLProfile glp;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getDefault();
        Assert.assertNotNull(glp);
        width  = 512;
        height = 512;
    }

    @AfterClass
    public static void releaseClass() {
    }

    static class Swapper implements GLEventListener {
        public void init(GLAutoDrawable drawable) {
            System.err.println("auto-swap: "+drawable.getAutoSwapBufferMode());
        }
        public void dispose(GLAutoDrawable drawable) {
        }
        public void display(GLAutoDrawable drawable) {
            if(!drawable.getAutoSwapBufferMode()) {
                GL2 gl = drawable.getGL().getGL2();
                // copy the colored content of the back buffer into the front buffer
                // gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT);
                gl.glReadBuffer(GL.GL_BACK);  // def. in dbl buff mode: GL_BACK
                gl.glDrawBuffer(GL.GL_FRONT); // def. in dbl buff mode: GL_BACK
                gl.glCopyPixels(0, 0, drawable.getWidth(), drawable.getHeight(), GL2.GL_COLOR);
                // gl.glPopAttrib();
                gl.glDrawBuffer(GL.GL_BACK); // def. in dbl buff mode: GL_BACK
            }            
        }
        public void reshape(GLAutoDrawable drawable, int x, int y, int width,
                int height) {
        }        
    }
    protected void runTestGL(GLCapabilities caps) throws InterruptedException, InvocationTargetException {
        final Frame frame = new Frame("Gears AWT Test");
        Assert.assertNotNull(frame);

        final GLCanvas glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);
        glCanvas.setAutoSwapBufferMode(!altSwap);
        frame.add(glCanvas);
        frame.setSize(512, 512);

        glCanvas.addGLEventListener(new Gears(0));
        glCanvas.addGLEventListener(new Swapper());

        Animator animator = new Animator(glCanvas);
        QuitAdapter quitAdapter = new QuitAdapter();

        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter)).addTo(glCanvas);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter)).addTo(frame);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(true);
            }});
        animator.setUpdateFPSFrames(60, System.err);        
        animator.start();

        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glCanvas);
        Assert.assertNotNull(animator);

        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());
        frame.setVisible(false);
        Assert.assertEquals(false, frame.isVisible());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.remove(glCanvas);
                frame.dispose();
            }});
    }

    @Test
    public void test01() throws InterruptedException, InvocationTargetException {
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setDoubleBuffered(true); // code assumes dbl buffer setup
        runTestGL(caps);
    }

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-autoswap")) {
                altSwap = false;
            }
        }
        System.err.println("altSwap "+altSwap);
        if(waitForKey) {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (IOException e) { }
        }
        org.junit.runner.JUnitCore.main(TestGearsAWTAnalyzeBug455.class.getName());
    }
}
