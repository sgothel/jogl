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

import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
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
 * Tests that a basic SWT app can open without crashing under different GL profiles. Uses the SWT GL canvas.
 * @author Wade Walker
 */
public class TestSWT01GLn extends UITestCase {

    static int duration = 250;

    static final int iwidth = 640;
    static final int iheight = 480;

    Display display = null;
    Shell shell = null;
    Composite composite = null;

    @BeforeClass
    public static void startup() {
        System.out.println( "GLProfile " + GLProfile.glAvailabilityToString() );
    }

    @Before
    public void init() {
        display = new Display();
        Assert.assertNotNull( display );
        shell = new Shell( display );
        Assert.assertNotNull( shell );
        shell.setLayout( new FillLayout() );
        composite = new Composite( shell, SWT.NONE );
        composite.setLayout( new FillLayout() );
        Assert.assertNotNull( composite );
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );
        try {
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
    }

    protected void runTestAGL( GLProfile glprofile ) throws InterruptedException {
        GLData gldata = new GLData();
        gldata.doubleBuffer = true;
        // need SWT.NO_BACKGROUND to prevent SWT from clearing the window
        // at the wrong times (we use glClear for this instead)
        final GLCanvas glcanvas = new GLCanvas( composite, SWT.NO_BACKGROUND, gldata );
        Assert.assertNotNull( glcanvas );
        glcanvas.setCurrent();
        final GLContext glcontext = GLDrawableFactory.getFactory( glprofile ).createExternalGLContext();
        Assert.assertNotNull( glcontext );

        // fix the viewport when the user resizes the window
        glcanvas.addListener( SWT.Resize, new Listener() {
            public void handleEvent( Event event ) {
                Rectangle rectangle = glcanvas.getClientArea();
                glcanvas.setCurrent();
                glcontext.makeCurrent();
                GL2 gl = glcontext.getGL().getGL2();
                OneTriangle.setup( gl, rectangle.width, rectangle.height );
                glcontext.release();
                System.err.println("resize");
            }
        });

        // draw the triangle when the OS tells us that any part of the window needs drawing
        glcanvas.addPaintListener( new PaintListener() {
            public void paintControl( PaintEvent paintevent ) {
                Rectangle rectangle = glcanvas.getClientArea();
                glcanvas.setCurrent();
                glcontext.makeCurrent();
                GL2 gl = glcontext.getGL().getGL2();
                OneTriangle.render( gl, rectangle.width, rectangle.height );
                glcanvas.swapBuffers();
                glcontext.release();
                System.err.println("paint");
            }
        });
        
        shell.setText( getClass().getName() );
        shell.setSize( 640, 480 );
        shell.open();

        long lStartTime = System.currentTimeMillis();
        long lEndTime = lStartTime + duration;
        try {
            while( (System.currentTimeMillis() < lEndTime) && !glcanvas.isDisposed() ) {
                if( !display.readAndDispatch() ) {
                    // blocks on linux .. display.sleep();
                    Thread.sleep(10);
                }
            }
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        glcanvas.dispose();
    }

    @Test
    public void testA01GLDefault() throws InterruptedException {
        GLProfile glprofile = GLProfile.getDefault();
        System.out.println( "GLProfile Default: " + glprofile );
        runTestAGL( glprofile );
    }

    @Test
    public void test02GL2() throws InterruptedException {
        GLProfile glprofile = GLProfile.get(GLProfile.GL2);
        System.out.println( "GLProfile GL2: " + glprofile );
        runTestAGL( glprofile );
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = atoi(args[++i]);
            }
        }
        System.out.println("durationPerTest: "+duration);
        org.junit.runner.JUnitCore.main(TestSWT01GLn.class.getName());
    }
}
