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

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.eclipse.swt.SWT;
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

import com.jogamp.opengl.swt.GLCanvas;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.OneTriangle;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Tests that a basic SWT app can open without crashing under different GL profiles.
 * <p> 
 * Uses JOGL's new SWT GLCanvas.
 * </p>
 * <p>
 * Holds AWT in it's test name, since our impl. still needs the AWT threading, 
 * see {@link GLCanvas}.
 * </p>
 * @author Wade Walker, et.al.
 */
public class TestSWTJOGLGLCanvas01GLnAWT extends UITestCase {

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
        // need SWT.NO_BACKGROUND to prevent SWT from clearing the window
        // at the wrong times (we use glClear for this instead)
        final GLCapabilitiesImmutable caps = new GLCapabilities( glprofile );
        
        final GLCanvas canvas = new GLCanvas( composite, SWT.NO_BACKGROUND, caps, null, null);
        Assert.assertNotNull( canvas );

        canvas.addGLEventListener(new GLEventListener() {
           public void init(final GLAutoDrawable drawable) { }
           public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                OneTriangle.setup( drawable.getGL().getGL2(), width, height );
           }                 
           public void display(final GLAutoDrawable drawable) {
                OneTriangle.render( drawable.getGL().getGL2(), drawable.getWidth(), drawable.getHeight());
           }
           public void dispose(final GLAutoDrawable drawable) {}         
        });
       
        shell.setText( getClass().getName() );
        shell.setSize( 640, 480 );
        shell.open();

        long lStartTime = System.currentTimeMillis();
        long lEndTime = lStartTime + duration;
        try {
            while( (System.currentTimeMillis() < lEndTime) && !canvas.isDisposed() ) {
                if( !display.readAndDispatch() ) {
                    // blocks on linux .. display.sleep();
                    Thread.sleep(10);
                }
            }
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        canvas.dispose();
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
        org.junit.runner.JUnitCore.main(TestSWTJOGLGLCanvas01GLnAWT.class.getName());
    }
}
