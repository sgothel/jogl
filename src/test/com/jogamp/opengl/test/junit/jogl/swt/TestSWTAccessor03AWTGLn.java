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

import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;


import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.opengl.test.junit.jogl.demos.es1.OneTriangle;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Tests that a basic SWT app can open without crashing under different GL profiles. Uses the AWT GL canvas with
 * the SWT_AWT bridge.
 * @author Wade Walker, et.al.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSWTAccessor03AWTGLn extends UITestCase {

    static int duration = 250;

    Display display = null;
    Shell shell = null;
    Composite composite = null;
    Frame frame = null;
    GLCanvas glcanvas = null;

    @BeforeClass
    public static void startup() {
        if( Platform.getOSType() == Platform.OSType.MACOS ) {
            // NSLocking issues on OSX and AWT, able to freeze whole test suite!
            // Since this test is merely a technical nature to validate the accessor w/ SWT
            // we can drop it w/o bothering.
            UITestCase.setTestSupported(false);
            return;
        }
        System.out.println( "GLProfile " + GLProfile.glAvailabilityToString() );
        final Frame f0 = new Frame("Test - AWT 1st");
        f0.add(new java.awt.Label("AWT was here 1st"));
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    f0.pack();
                    f0.setVisible(true);
                }});
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        if(!GLProfile.isAvailable(GLProfile.GL2)) {
            setTestSupported(false);
        }
    }

    protected void init() throws InterruptedException, InvocationTargetException {
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

    protected void release() throws InterruptedException, InvocationTargetException {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );
        Assert.assertNotNull( glcanvas );
        final Runnable releaseAWT = new Runnable() {
            public void run() {
                // deadlocks Java7 on Windows
                frame.setVisible(false);
                frame.remove(glcanvas);
                frame.dispose();
                frame = null;
                glcanvas = null;
            } };
         // Deadlocks Java7 on Windows
        // javax.swing.SwingUtilities.invokeAndWait( releaseAWT );
        releaseAWT.run();

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

    protected void runTestGL( final GLProfile glprofile ) throws InterruptedException, InvocationTargetException {
        init();
        try {
            final GLCapabilities glcapabilities = new GLCapabilities( glprofile );
            glcanvas = new GLCanvas( glcapabilities );
            Assert.assertNotNull( glcanvas );
            frame.add( glcanvas );

            glcanvas.addGLEventListener( new GLEventListener() {
                /* @Override */
                public void init( final GLAutoDrawable glautodrawable ) {
                }

                /* @Override */
                public void dispose( final GLAutoDrawable glautodrawable ) {
                }

                /* @Override */
                public void display( final GLAutoDrawable glautodrawable ) {
                    final Rectangle rectangle = new Rectangle( 0, 0, glautodrawable.getSurfaceWidth(), glautodrawable.getSurfaceHeight() );
                    final GL2ES1 gl = glautodrawable.getGL().getGL2ES1();
                    OneTriangle.render( gl, rectangle.width, rectangle.height );
                }

                /* @Override */
                public void reshape( final GLAutoDrawable glautodrawable, final int x, final int y, final int width, final int height ) {
                    final Rectangle rectangle = new Rectangle( 0, 0, glautodrawable.getSurfaceWidth(), glautodrawable.getSurfaceHeight() );
                    final GL2ES1 gl = glautodrawable.getGL().getGL2ES1();
                    OneTriangle.setup( gl, rectangle.width, rectangle.height );
                }
            });

            SWTAccessor.invoke(true, new Runnable() {
                public void run() {
                    shell.setText( getClass().getName() );
                    shell.setSize( 640, 480 );
                    shell.open();
                }});

            final long lStartTime = System.currentTimeMillis();
            final long lEndTime = lStartTime + duration;
            try {
                while( (System.currentTimeMillis() < lEndTime) && !composite.isDisposed() ) {
                    SWTAccessor.invoke(true, new Runnable() {
                        public void run() {
                            if( !display.readAndDispatch() ) {
                                // blocks on linux .. display.sleep();
                                try {
                                    Thread.sleep(10);
                                } catch (final InterruptedException e) { }
                            }
                        }});
                }
            }
            catch( final Throwable throwable ) {
                throwable.printStackTrace();
                Assume.assumeNoException( throwable );
            }
        } finally {
            release();
        }
    }

    @Test
    public void test() throws InterruptedException, InvocationTargetException {
        final GLProfile glprofile = GLProfile.getGL2ES1();
        runTestGL( glprofile );
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i], duration);
            }
        }
        org.junit.runner.JUnitCore.main( TestSWTAccessor03AWTGLn.class.getName() );
    }
}
