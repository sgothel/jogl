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

package com.jogamp.opengl.test.junit.newt.parenting;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * Using {@link NewtCanvasSWT#setNEWTChild(Window)} for reparenting, i.e. NEWT/AWT hopping
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParenting04SWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 800;
    static GLCapabilities glCaps;

    Display display = null;
    Shell shell1 = null;
    Shell shell2 = null;
    Composite composite1 = null;
    Composite composite2 = null;
    com.jogamp.newt.Display swtNewtDisplay = null;

    @BeforeClass
    public static void initClass() {
        width  = 400;
        height = 400;
        glCaps = new GLCapabilities(null);
    }

    @Before
    public void init() {
        SWTAccessor.invoke(true, new Runnable() {
            public void run() {
                display = new Display();
                Assert.assertNotNull( display );

                shell1 = new Shell( display );
                Assert.assertNotNull( shell1 );
                shell1.setLayout( new FillLayout() );
                composite1 = new Composite( shell1, SWT.NONE );
                composite1.setLayout( new FillLayout() );
                Assert.assertNotNull( composite1 );

                shell2 = new Shell( display );
                Assert.assertNotNull( shell2 );
                shell2.setLayout( new FillLayout() );
                composite2 = new Composite( shell2, SWT.NONE );
                composite2.setLayout( new FillLayout() );
                Assert.assertNotNull( composite2 );
            }});
        swtNewtDisplay = NewtFactory.createDisplay(null, false); // no-reuse
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell1 );
        Assert.assertNotNull( shell2 );
        Assert.assertNotNull( composite1 );
        Assert.assertNotNull( composite2 );
        try {
            SWTAccessor.invoke(true, new Runnable() {
               public void run() {
                composite1.dispose();
                composite2.dispose();
                shell1.dispose();
                shell2.dispose();
                display.dispose();
               }});
        }
        catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        swtNewtDisplay = null;
        display = null;
        shell1 = null;
        shell2 = null;
        composite1 = null;
        composite2 = null;
    }

    @Test
    public void test01WinHopFrame2FrameDirectHop() throws InterruptedException, InvocationTargetException {
        // Will produce some artifacts .. resizing etc
        winHopFrame2Frame(false);
    }

    @Test
    public void test02WinHopFrame2FrameDetachFirst() throws InterruptedException, InvocationTargetException {
        // Note: detaching first setNEWTChild(null) is much cleaner visually
        winHopFrame2Frame(true);
    }

    protected void winHopFrame2Frame(final boolean detachFirst) throws InterruptedException, InvocationTargetException {
        final com.jogamp.newt.Screen screen = NewtFactory.createScreen(swtNewtDisplay, 0);

        final GLWindow glWindow1 = GLWindow.create(screen, glCaps);
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        final Animator anim1 = new Animator(glWindow1);

        final GLWindow glWindow2 = GLWindow.create(screen, glCaps);
        final GLEventListener demo2 = new GearsES2();
        setDemoFields(demo2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);
        final Animator anim2 = new Animator(glWindow2);

        final NewtCanvasSWT canvas1 = NewtCanvasSWT.create( composite1, 0, glWindow1 );
        final NewtCanvasSWT canvas2 = NewtCanvasSWT.create( composite2, 0, glWindow2 );

        SWTAccessor.invoke(true, new Runnable() {
           public void run() {
              shell1.setText( getSimpleTestName(".")+"-Win1" );
              shell1.setSize( width, height);
              shell1.setLocation(0, 0);
              shell1.open();
              shell2.setText( getSimpleTestName(".")+"-Win2" );
              shell2.setSize( width, height);
              shell2.setLocation(width + 50, 0);
              shell2.open();
           }
        });
        Assert.assertEquals(canvas1.getNativeWindow(),glWindow1.getParent());
        Assert.assertEquals(canvas2.getNativeWindow(),glWindow2.getParent());

        anim1.start();
        anim2.start();

        int state;
        for(state=0; state<3; state++) {
            for(int i=0; i*10<durationPerTest; i++) {
                if( !display.readAndDispatch() ) {
                    // blocks on linux .. display.sleep();
                    Thread.sleep(10);
                }
            }
            switch(state) {
                case 0:
                    SWTAccessor.invoke(true, new Runnable() {
                       public void run() {
                           // 1 -> 2
                           if(detachFirst) {
                               canvas1.setNEWTChild(null);
                               canvas2.setNEWTChild(null);
                           } else {
                               canvas2.setNEWTChild(null);  // free g2 of w2
                           }
                           canvas1.setNEWTChild(glWindow2); // put g2 -> w1. free g1 of w1
                           canvas2.setNEWTChild(glWindow1); // put g1 -> w2
                       } } );
                    break;
                case 1:
                    SWTAccessor.invoke(true, new Runnable() {
                       public void run() {
                           // 2 -> 1
                           if(detachFirst) {
                               canvas1.setNEWTChild(null);
                               canvas2.setNEWTChild(null);
                           } else {
                               canvas2.setNEWTChild(null);
                           }
                           canvas1.setNEWTChild(glWindow1);
                           canvas2.setNEWTChild(glWindow2);
                       } } );
                    break;
            }
        }

        canvas1.dispose();
        canvas2.dispose();
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertEquals(false, glWindow2.isNativeValid());
    }

    public static void setDemoFields(final GLEventListener demo, final GLWindow glWindow, final boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        final Window window = glWindow.getDelegatedWindow();
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        final String tstname = TestParenting04SWT.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }

}
