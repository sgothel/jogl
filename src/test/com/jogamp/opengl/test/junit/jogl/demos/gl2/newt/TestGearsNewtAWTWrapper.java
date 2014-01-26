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
 
package com.jogamp.opengl.test.junit.jogl.demos.gl2.newt;

import javax.media.nativewindow.*;
import javax.media.opengl.*;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGearsNewtAWTWrapper extends UITestCase {
    static GLProfile glp;
    static int width, height;
    static boolean useAnimator = true;
    static boolean doResizeTest = true;
    static long duration = 500; // ms

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getGL2ES2();
        Assert.assertNotNull(glp);
        width  = 640;
        height = 480;
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(GLCapabilitiesImmutable caps) throws InterruptedException {
        Display nDisplay = NewtFactory.createDisplay(NativeWindowFactory.TYPE_AWT, null, false); // local display
        Screen nScreen  = NewtFactory.createScreen(nDisplay, 0); // screen 0
        Window nWindow = NewtFactory.createWindow(nScreen, caps);

        GLWindow glWindow = GLWindow.create(nWindow);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Gears NewtAWTWrapper Test");

        glWindow.addGLEventListener(new GearsES2(1));

        Animator animator = useAnimator ? new Animator(glWindow) : null;
        QuitAdapter quitAdapter = new QuitAdapter();

        glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));

        if( useAnimator ) {
            animator.start();
        }
        
        int div = 3;
        glWindow.setSize(width/div, height/div);
        glWindow.setVisible(true);
        if( doResizeTest ) {
            glWindow.display();
            Assert.assertTrue("Size not reached: Expected "+(width/div)+"x"+(height/div)+", Is "+glWindow.getWidth()+"x"+glWindow.getHeight(), 
                              AWTRobotUtil.waitForSize(glWindow, width/div, height/div));
            Thread.sleep(600);
    
            div = 2;
            glWindow.setSize(width/div, height/div);
            glWindow.display();
            Assert.assertTrue("Size not reached: Expected "+(width/div)+"x"+(height/div)+", Is "+glWindow.getWidth()+"x"+glWindow.getHeight(), 
                              AWTRobotUtil.waitForSize(glWindow, width/div, height/div));
            Thread.sleep(600);
               
            div = 1;
            glWindow.setSize(width/div, height/div);
            glWindow.display();
            Assert.assertTrue("Size not reached: Expected "+(width/div)+"x"+(height/div)+", Is "+glWindow.getWidth()+"x"+glWindow.getHeight(), 
                              AWTRobotUtil.waitForSize(glWindow, width/div, height/div));
            Thread.sleep(600);
        }

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1-t0<duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        if( useAnimator ) {
            animator.stop();
        }
        glWindow.destroy();
    }

    @Test
    public void test01() throws InterruptedException {
        GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        runTestGL(caps);
    }

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-noanim")) {
                useAnimator  = false;
            } else if(args[i].equals("-noresize")) {
                doResizeTest  = false;
            }
        }
        System.err.println("useAnimator "+useAnimator);
        org.junit.runner.JUnitCore.main(TestGearsNewtAWTWrapper.class.getName());
    }
}
