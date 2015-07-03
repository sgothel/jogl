/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Simple visibility test ..
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParenting01aSWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 800;
    static GLCapabilities glCaps;

    Display display = null;
    Shell shell = null;
    Composite composite1 = null;
    com.jogamp.newt.Display swtNewtDisplay = null;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
    }

    @Before
    public void init() {
        SWTAccessor.invoke(true, new Runnable() {
            public void run() {
                display = new Display();
                Assert.assertNotNull( display );
                shell = new Shell( display );
                Assert.assertNotNull( shell );
                shell.setLayout( new FillLayout() );
                composite1 = new Composite( shell, SWT.NONE );
                composite1.setLayout( new FillLayout() );
                Assert.assertNotNull( composite1 );
            }});
        swtNewtDisplay = NewtFactory.createDisplay(null, false); // no-reuse
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite1 );
        try {
            SWTAccessor.invoke(true, new Runnable() {
               public void run() {
                composite1.dispose();
                shell.dispose();
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
        composite1 = null;
    }

    @Test
    public void testWindowParenting01CreateVisibleDestroy1() throws InterruptedException, InvocationTargetException {

        final com.jogamp.newt.Screen screen = NewtFactory.createScreen(swtNewtDisplay, 0);
        final GLWindow glWindow1 = GLWindow.create(screen, glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        final NewtCanvasSWT canvas1 = NewtCanvasSWT.create( composite1, 0, glWindow1 );
        Assert.assertNotNull(canvas1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());

        SWTAccessor.invoke(true, new Runnable() {
           public void run() {
              shell.setText( getSimpleTestName(".") );
              shell.setSize( 640, 480 );
              shell.open();
           }
        });

        // visible test
        Assert.assertEquals(canvas1.getNativeWindow(),glWindow1.getParent());

        for(int i=0; i*10<durationPerTest; i++) {
            if( !display.readAndDispatch() ) {
                // blocks on linux .. display.sleep();
                Thread.sleep(10);
            }
        }

        SWTAccessor.invoke(true, new Runnable() {
           public void run() {
               canvas1.setVisible(false);
           }
        });
        Assert.assertEquals(true, glWindow1.isNativeValid());

        SWTAccessor.invoke(true, new Runnable() {
           public void run() {
               canvas1.setVisible(true);
           }
        });
        Assert.assertEquals(true, glWindow1.isNativeValid());

        canvas1.dispose();

        Assert.assertEquals(false, glWindow1.isNativeValid());
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
        final String tstname = TestParenting01aSWT.class.getName();
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
