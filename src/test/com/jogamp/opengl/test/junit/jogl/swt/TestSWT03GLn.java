/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
 
package com.jogamp.opengl.test.junit.jogl.swt;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import jogamp.opengl.swt.GLCanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.Gears;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;


public class TestSWT03GLn extends UITestCase {
	
    Display display = null;
    Shell shell = null;
    GLCanvas glCanvas;

    @BeforeClass
    public static void startup() {
        GLProfile.initSingleton(true);
        System.out.println("GLProfile "+GLProfile.glAvailabilityToString());
    }

    @Before
    public void init() {
        display = new Display();
        Assert.assertNotNull( display );
        shell = new Shell( display );
        Assert.assertNotNull( shell );
        shell.setLayout( new FillLayout() );
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( glCanvas );
        try {
        	glCanvas.dispose();
            shell.dispose();
            display.dispose();
        }
        catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        display = null;
        shell = null;
        glCanvas = null;
    }

    protected void runTestGL(GLCapabilities caps) throws InterruptedException {
        glCanvas = new GLCanvas(shell, SWT.NONE, caps, null);
        Assert.assertNotNull(glCanvas);
        glCanvas.addGLEventListener(new Gears());

        shell.setSize(512, 512);
        shell.open();

        glCanvas.display(); // one in process display

        Animator animator = new Animator(glCanvas);
        animator.start();

        Thread.sleep(500); // 500 ms

        animator.stop();
        
//    	while (!shell.isDisposed ()) {
//    		if (!display.readAndDispatch ()) display.sleep ();
//    	}
    }

    @Test
    public void test01GLDefault() throws InterruptedException {
        GLProfile glp = GLProfile.getDefault();
        System.out.println("GLProfile Default: "+glp);
        GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps);
    }

    @Test
    public void test02GL2() throws InterruptedException {
        GLProfile glprofile = GLProfile.get(GLProfile.GL2);
        System.out.println( "GLProfile GL2: " + glprofile );
        GLCapabilities caps = new GLCapabilities(glprofile);
        runTestGL(caps);
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestSWT03GLn.class.getName());
    }
}
