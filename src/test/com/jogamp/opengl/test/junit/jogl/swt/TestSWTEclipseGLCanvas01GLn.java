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

import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

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
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.opengl.test.junit.jogl.demos.es1.OneTriangle;
import com.jogamp.opengl.test.junit.util.SWTTestUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Tests that a basic SWT app can open without crashing under different GL profiles.
 * <p>
 * Uses the SWT GLCanvas <code>org.eclipse.swt.opengl.GLCanvas</code>.
 * </p>
 * @author Wade Walker, et.al.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSWTEclipseGLCanvas01GLn extends UITestCase {

    static int duration = 250;

    static final int iwidth = 640;
    static final int iheight = 480;

    Display display = null;
    Shell shell = null;
    Composite composite = null;
    GLCanvas glcanvas = null;
    volatile boolean triangleSet = false;


    @BeforeClass
    public static void startup() {
        System.out.println( "GLProfile " + GLProfile.glAvailabilityToString() );
    }

    @Before
    public void init() {
        triangleSet = false;
        SWTAccessor.invokeOnOSTKThread(true, new Runnable() {
            public void run() {
                display = new Display();
                Assert.assertNotNull( display );
            } } );
        SWTAccessor.invokeOnSWTThread(display, true, new Runnable() {
            public void run() {
                shell = new Shell( display );
                Assert.assertNotNull( shell );
                shell.setLayout( new FillLayout() );
                composite = new Composite( shell, SWT.NONE );
                composite.setLayout( new FillLayout() );
                Assert.assertNotNull( composite );
            }});
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );
        try {
            SWTAccessor.invokeOnSWTThread(display, true, new Runnable() {
               public void run() {
                if( null != glcanvas ) {
                    glcanvas.dispose();
                }
                composite.dispose();
                shell.dispose();
                display.dispose();
               }});
        }
        catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        display = null;
        shell = null;
        composite = null;
        glcanvas = null;
    }

    protected void runTestAGL( final GLProfile glprofile ) throws InterruptedException {
        final GLContext glcontext[] = { null };

        SWTAccessor.invokeOnSWTThread(display, true, new Runnable() {
            public void run() {
                shell.setText( getClass().getName() );
                shell.setSize( 640, 480 );
                shell.open();

                final GLData gldata = new GLData();
                gldata.doubleBuffer = true;

                // need SWT.NO_BACKGROUND to prevent SWT from clearing the window
                // at the wrong times (we use glClear for this instead)
                glcanvas = new GLCanvas( composite, SWT.NO_BACKGROUND, gldata );
                Assert.assertNotNull( glcanvas );

                glcanvas.setCurrent();
                glcontext[0] = GLDrawableFactory.getFactory( glprofile ).createExternalGLContext();
                Assert.assertNotNull( glcontext[0] );

                // fix the viewport when the user resizes the window
                glcanvas.addListener( SWT.Resize, new Listener() {
                    public void handleEvent( final Event event ) {
                        final Rectangle rectangle = glcanvas.getClientArea();
                        glcanvas.setCurrent();
                        glcontext[0].makeCurrent();
                        final GL2ES1 gl = glcontext[0].getGL().getGL2ES1();
                        OneTriangle.setup( gl, rectangle.width, rectangle.height );
                        triangleSet = true;
                        glcontext[0].release();
                        System.err.println("resize");
                    }
                });

                // draw the triangle when the OS tells us that any part of the window needs drawing
                glcanvas.addPaintListener( new PaintListener() {
                    public void paintControl( final PaintEvent paintevent ) {
                        final Rectangle rectangle = glcanvas.getClientArea();
                        glcanvas.setCurrent();
                        glcontext[0].makeCurrent();
                        final GL2ES1 gl = glcontext[0].getGL().getGL2ES1();
                        if( !triangleSet ) {
                            OneTriangle.setup( gl, rectangle.width, rectangle.height );
                            triangleSet = true;
                        }
                        OneTriangle.render( gl, rectangle.width, rectangle.height );
                        glcanvas.swapBuffers();
                        glcontext[0].release();
                        System.err.println("paint");
                    }
                });
            } } );

        final SWTTestUtil.WaitAction generalWaitAction = new SWTTestUtil.WaitAction(display, true, 16);

        final long lStartTime = System.currentTimeMillis();
        final long lEndTime = lStartTime + duration;
        try {
            while( (System.currentTimeMillis() < lEndTime) && !glcanvas.isDisposed() ) {
                SWTAccessor.invokeOnSWTThread(display, true, new Runnable() {
                    public void run() {
                        if( !triangleSet ) {
                            glcanvas.setSize( 640, 480 );
                        }
                        // glcanvas.redraw();
                    } } );
                generalWaitAction.run();
            }
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    @Test
    public void test() throws InterruptedException {
        final GLProfile glprofile = GLProfile.getGL2ES1();
        runTestAGL( glprofile );
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = atoi(args[++i]);
            }
        }
        System.out.println("durationPerTest: "+duration);
        org.junit.runner.JUnitCore.main(TestSWTEclipseGLCanvas01GLn.class.getName());
    }
}
