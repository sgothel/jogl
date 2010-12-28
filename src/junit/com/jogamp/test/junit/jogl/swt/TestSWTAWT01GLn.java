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

package com.jogamp.test.junit.jogl.swt;

import java.awt.Frame;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;

import com.jogamp.test.junit.util.UITestCase;

/**
 * Tests that a basic SWT app can open without crashing under different GL profiles. Uses the AWT GL canvas with
 * the SWT_AWT bridge.
 * @author Wade Walker
 */
public class TestSWTAWT01GLn extends UITestCase {

	Display display = null;
    Shell shell = null;
    Composite composite = null;
    Frame frame = null;
    GLCanvas glcanvas = null;

    @BeforeClass
    public static void startup() {
        GLProfile.initSingleton( true );
        System.out.println( "GLProfile " + GLProfile.glAvailabilityToString() );
    }

    @Before
    public void init() {
        display = new Display();
        Assert.assertNotNull( display );
		shell = new Shell( display );
        Assert.assertNotNull( shell );
		shell.setLayout( new FillLayout() );
		composite = new Composite( shell, SWT.EMBEDDED | SWT.NO_BACKGROUND );
        composite.setLayout( new FillLayout() );
        Assert.assertNotNull( composite );
        frame = SWT_AWT.new_Frame( composite );
        Assert.assertNotNull( frame );
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );
        Assert.assertNotNull( glcanvas );
        try {
            frame.setVisible( false );
            frame.remove( glcanvas );
            frame.dispose();
	        composite.dispose();
	        shell.dispose();
			display.dispose();
        }
        catch( Throwable throwable ) {
        	throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        display = null;
        shell = null;
		composite = null;
		frame = null;
        glcanvas = null;
    }

    protected void runTestGL( GLProfile glprofile ) throws InterruptedException {
    	GLCapabilities glcapabilities = new GLCapabilities( glprofile );
        glcanvas = new GLCanvas( glcapabilities );
        Assert.assertNotNull( glcanvas );
        frame.add( glcanvas ); 

        glcanvas.addGLEventListener( new GLEventListener() {
			@Override
			public void init( GLAutoDrawable glautodrawable ) {
			}

			@Override
			public void dispose( GLAutoDrawable glautodrawable ) {
			}

			@Override
			public void display( GLAutoDrawable glautodrawable ) {
                Rectangle rectangle = new Rectangle( 0, 0, glautodrawable.getWidth(), glautodrawable.getHeight() );
                GL2 gl = glautodrawable.getGL().getGL2();
                OneTriangle.render( gl, rectangle );
			}

			@Override
			public void reshape( GLAutoDrawable glautodrawable, int x, int y, int width, int height ) {
                Rectangle rectangle = new Rectangle( 0, 0, glautodrawable.getWidth(), glautodrawable.getHeight() );
                GL2 gl = glautodrawable.getGL().getGL2();
                OneTriangle.setup( gl, rectangle );
			}
        });

		shell.setText( getClass().getName() );
		shell.setSize( 640, 480 );
		shell.open();

		long lStartTime = System.currentTimeMillis();
		long lEndTime = lStartTime + 1000;
		try {
    		while( (System.currentTimeMillis() < lEndTime) && !composite.isDisposed() ) {
    			if( !display.readAndDispatch() )
    				display.sleep();
    		}
        }
		catch( Throwable throwable ) {
        	throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    @Test
    public void test01GLDefault() throws InterruptedException {
        GLProfile glprofile = GLProfile.getDefault();
        System.out.println( "GLProfile Default: " + glprofile );
        runTestGL( glprofile );
    }

    @Test
    public void test02GLMaxFixed() throws InterruptedException {
        GLProfile glprofileMaxFixed = GLProfile.getMaxFixedFunc();
        System.out.println( "GLProfile MaxFixed: " + glprofileMaxFixed );
        try {
            runTestGL( glprofileMaxFixed );
        }
        catch( Throwable throwable ) {
             // FIXME: 
             // Stop test and ignore if GL3bc and GL4bc
             // currently this won't work on ATI!
             if( glprofileMaxFixed.equals(GLProfile.GL3bc) || glprofileMaxFixed.equals(GLProfile.GL4bc) ) {
            	 throwable.printStackTrace();
                Assume.assumeNoException( throwable );
             }
             // else .. serious unexpected exception
        }
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main( TestSWTAWT01GLn.class.getName() );
    }
}