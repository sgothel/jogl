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

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

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
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.MultisampleDemoES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Tests that a basic SWT app can open without crashing under different GL profiles
 * _and_ custom GLCapabilities.
 * <p>
 * Uses JOGL's NewtCanvasSWT, which allows to be a native container of a NEWT Window.<br/>
 * This method allows utilizing custom GLCapability settings,
 * independent from the already instantiated SWT visual.
 * </p>
 * <p>
 * Note that {@link SWTAccessor#invoke(boolean, Runnable)} is still used to comply w/
 * SWT running on Mac OSX, i.e. to enforce UI action on the main thread.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNewtCanvasSWTGLn extends UITestCase {

    static int duration = 250;

    static final int iwidth = 640;
    static final int iheight = 480;

    Display display = null;
    Shell shell = null;
    Composite composite = null;
    com.jogamp.newt.Display swtNewtDisplay = null;

    @BeforeClass
    public static void startup() {
        System.out.println( "GLProfile " + GLProfile.glAvailabilityToString() );
    }

    @Before
    public void init() {
        SWTAccessor.invoke(true, new Runnable() {
            public void run() {
                display = new Display();
                Assert.assertNotNull( display );
            }});
        display.syncExec(new Runnable() {
            public void run() {
                shell = new Shell( display );
                Assert.assertNotNull( shell );
                shell.setLayout( new FillLayout() );
                composite = new Composite( shell, SWT.NONE );
                composite.setLayout( new FillLayout() );
                Assert.assertNotNull( composite );
            }});
        swtNewtDisplay = NewtFactory.createDisplay(null, false); // no-reuse
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );
        try {
            display.syncExec(new Runnable() {
               public void run() {
                composite.dispose();
                shell.dispose();
               }});
            SWTAccessor.invoke(true, new Runnable() {
               public void run() {
                display.dispose();
               }});
        }
        catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        swtNewtDisplay = null;
        display = null;
        shell = null;
        composite = null;
    }

    class WaitAction implements Runnable {
        private final long sleepMS;

        WaitAction(final long sleepMS) {
            this.sleepMS = sleepMS;
        }
        public void run() {
            if( !display.readAndDispatch() ) {
                // blocks on linux .. display.sleep();
                try {
                    Thread.sleep(sleepMS);
                } catch (final InterruptedException e) { }
            }
        }
    }
    final WaitAction awtRobotWaitAction = new WaitAction(AWTRobotUtil.TIME_SLICE);
    final WaitAction generalWaitAction = new WaitAction(10);

    protected void runTestAGL( final GLCapabilitiesImmutable caps, final GLEventListener demo,
                               final boolean postAttach, final boolean useAnimator ) throws InterruptedException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(false, false);

        final Screen screen = NewtFactory.createScreen(swtNewtDisplay, 0);
        final GLWindow glWindow1 = GLWindow.create(screen, caps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        glWindow1.addGLEventListener(demo);
        glWindow1.addGLEventListener(new GLEventListener() {
           int displayCount = 0;
           public void init(final GLAutoDrawable drawable) { }
           public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
           public void display(final GLAutoDrawable drawable) {
              if(displayCount < 3) {
                  snapshot(displayCount++, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
              }
           }
           public void dispose(final GLAutoDrawable drawable) { }
        });

        final NewtCanvasSWT canvas1 = NewtCanvasSWT.create( composite, 0, postAttach ? null : glWindow1 );
        Assert.assertNotNull( canvas1 );

        display.syncExec( new Runnable() {
           public void run() {
              shell.setText( getSimpleTestName(".") );
              shell.setSize( 640, 480 );
              shell.open();
           }
        });

        if(postAttach) {
            canvas1.setNEWTChild(glWindow1);
        }

        Assert.assertTrue("GLWindow didn't become visible natively!", AWTRobotUtil.waitForRealized(glWindow1, awtRobotWaitAction, true));

        System.err.println("GLWindow LOS.0: "+glWindow1.getLocationOnScreen(null));
        System.err.println("NewtCanvasSWT LOS.0: "+canvas1.getNativeWindow().getLocationOnScreen(null));

        // canvas1.update();

        Animator anim;
        if(useAnimator) {
            anim = new Animator(glWindow1);
            anim.start();
        } else {
            anim = null;
        }

        final long lStartTime = System.currentTimeMillis();
        final long lEndTime = lStartTime + duration;
        try {
            while( (System.currentTimeMillis() < lEndTime) && !canvas1.isDisposed() ) {
                generalWaitAction.run();
            }
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        if(null != anim) {
            anim.stop();
        }

        canvas1.dispose();
    }

    @Test
    public void preAttach_WithAnimator() throws InterruptedException {
        runTestAGL( new GLCapabilities(GLProfile.getGL2ES2()), new GearsES2(), false /* postAttach */, true /* animator */);
    }

    @Test
    public void preAttach_NoAnimator() throws InterruptedException {
        runTestAGL( new GLCapabilities(GLProfile.getGL2ES2()), new GearsES2(), false /* postAttach */, false /* animator */);
    }

    @Test
    public void postAttach_WithAnimator() throws InterruptedException {
        runTestAGL( new GLCapabilities(GLProfile.getGL2ES2()), new GearsES2(), true /* postAttach */, true /* animator */);
    }

    @Test
    public void test_MultisampleAndAlpha() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
        caps.setSampleBuffers(true);
        caps.setNumSamples(2);
        runTestAGL( caps, new MultisampleDemoES2(true), false /* postAttach */, false /* animator */);
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i],  duration);
            }
        }
        System.out.println("durationPerTest: "+duration);
        org.junit.runner.JUnitCore.main(TestNewtCanvasSWTGLn.class.getName());
    }
}
