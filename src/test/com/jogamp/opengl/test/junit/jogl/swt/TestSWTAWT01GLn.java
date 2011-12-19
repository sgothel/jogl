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

package com.jogamp.opengl.test.junit.jogl.swt;

import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import jogamp.nativewindow.swt.SWTAccessor;

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

import com.jogamp.opengl.test.junit.jogl.demos.gl2.OneTriangle;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Tests that a basic SWT app can open without crashing under different GL profiles. Uses the AWT GL canvas with
 * the SWT_AWT bridge.
 * @author Wade Walker, et.al.
 */
public class TestSWTAWT01GLn extends UITestCase {

    static final int duration = 250;

    Display display = null;
    Shell shell = null;
    Composite composite = null;
    Frame frame = null;
    GLCanvas glcanvas = null;

    @BeforeClass
    public static void startup() {
        System.out.println( "GLProfile " + GLProfile.glAvailabilityToString() );
    }

    @Before
    public void init() throws InterruptedException, InvocationTargetException {
        SWTAccessor.invoke(true, new Runnable() {
            public void run() {        
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
            }});
    }

    @After
    public void release() throws InterruptedException, InvocationTargetException {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );
        Assert.assertNotNull( glcanvas );
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(false);
                frame.remove(glcanvas);
                frame.dispose();
                frame = null;
                glcanvas = null;
            }});
        
        SWTAccessor.invoke(true, new Runnable() {
            public void run() {
                composite.dispose();
                shell.close();
                shell.dispose();
                display.dispose();
                display = null;
                shell = null;
                composite = null;
            }});
    }

    protected void runTestGL( GLProfile glprofile ) throws InterruptedException {
        GLCapabilities glcapabilities = new GLCapabilities( glprofile );
        glcanvas = new GLCanvas( glcapabilities );
        Assert.assertNotNull( glcanvas );
        frame.add( glcanvas );

        glcanvas.addGLEventListener( new GLEventListener() {
            /* @Override */
            public void init( GLAutoDrawable glautodrawable ) {
            }

            /* @Override */
            public void dispose( GLAutoDrawable glautodrawable ) {
            }

            /* @Override */
            public void display( GLAutoDrawable glautodrawable ) {
                Rectangle rectangle = new Rectangle( 0, 0, glautodrawable.getWidth(), glautodrawable.getHeight() );
                GL2 gl = glautodrawable.getGL().getGL2();
                OneTriangle.render( gl, rectangle.width, rectangle.height );
            }

            /* @Override */
            public void reshape( GLAutoDrawable glautodrawable, int x, int y, int width, int height ) {
                Rectangle rectangle = new Rectangle( 0, 0, glautodrawable.getWidth(), glautodrawable.getHeight() );
                GL2 gl = glautodrawable.getGL().getGL2();
                OneTriangle.setup( gl, rectangle.width, rectangle.height );
            }
        });

        SWTAccessor.invoke(true, new Runnable() {
            public void run() {
                shell.setText( getClass().getName() );
                shell.setSize( 640, 480 );
                shell.open();
            }});

        long lStartTime = System.currentTimeMillis();
        long lEndTime = lStartTime + duration;
        try {
            while( (System.currentTimeMillis() < lEndTime) && !composite.isDisposed() ) {
                SWTAccessor.invoke(true, new Runnable() {
                    public void run() {
                        if( !display.readAndDispatch() ) {
                            // blocks on linux .. display.sleep();
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) { }
                        }
                    }});
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
    public void test02GL2() throws InterruptedException {
        GLProfile glprofile = GLProfile.get(GLProfile.GL2);
        System.out.println( "GLProfile GL2: " + glprofile );
        runTestGL( glprofile );
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main( TestSWTAWT01GLn.class.getName() );
    }
}
