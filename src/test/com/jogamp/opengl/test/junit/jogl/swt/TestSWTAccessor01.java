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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Tests {@link SWTAccessor#getWindowHandle(org.eclipse.swt.widgets.Control)} on a basic simple SWT window.
 * <p>
 * Bug 1362 inspired this unit test, i.e. finding the issue of SWT >= 4.10 + GTK3.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSWTAccessor01 extends UITestCase {

    static int duration = 250;

    Display display = null;
    Shell shell = null;
    Composite composite = null;

    @BeforeClass
    public static void startup() {
        if( Platform.getOSType() == Platform.OSType.MACOS ) {
            // NSLocking issues on OSX and AWT, able to freeze whole test suite!
            // Since this test is merely a technical nature to validate the accessor w/ SWT
            // we can drop it w/o bothering.
            JunitTracer.setTestSupported(false);
            return;
        }
    }

    protected void init() throws InterruptedException, InvocationTargetException {
        System.err.println("SWT Platform: "+SWT.getPlatform()+", Version "+SWT.getVersion());
        System.err.println("GTK_VERSION: "+SWTAccessor.GTK_VERSION());
        SWTAccessor.invoke(true, new Runnable() {
            public void run() {
                display = new Display();
                Assert.assertNotNull( display );
                shell = new Shell( display );
                Assert.assertNotNull( shell );
                shell.setLayout( new GridLayout(3, false) );
                shell.setBackground(new Color(display, 0, 0, 255));
                new Text(shell, SWT.NONE).setText("1");
                new Text(shell, SWT.NONE).setText("2");
                new Text(shell, SWT.NONE).setText("3");
                new Text(shell, SWT.NONE).setText("4");
                composite = new Composite( shell, SWT.NO_BACKGROUND /** | SWT.EMBEDDED */ );
                composite.setLayout( new FillLayout() );
                composite.setBackground(new Color(display, 0, 255, 0));
                final GridData gd = new GridData (GridData.FILL, GridData.FILL, true /* grabExcessHorizontalSpace */, true /* grabExcessVerticalSpace */);
                composite.setLayoutData(gd);
                new Text(shell, SWT.NONE).setText("6");
                new Text(shell, SWT.NONE).setText("7");
                new Text(shell, SWT.NONE).setText("8");
                new Text(shell, SWT.NONE).setText("9");
                Assert.assertNotNull( composite );
            }});
    }

    protected void release() throws InterruptedException, InvocationTargetException {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );

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

    protected void runTest() throws InterruptedException, InvocationTargetException {
        init();
        final Canvas canvas[] = { null };
        try {
            SWTAccessor.invoke(true, new Runnable() {
                public void run() {
                    canvas[0] = new Canvas (composite, SWT.NONE);
                    canvas[0].setBackground(new Color(display, 255, 255, 255));
                    canvas[0].setForeground(new Color(display, 255, 0, 0));
                    canvas[0].addPaintListener (new PaintListener() {
                        public void paintControl(final PaintEvent e) {
                            final Rectangle r = canvas[0].getClientArea();
                            e.gc.fillRectangle(0, 0, r.width, r.height);
                            e.gc.drawRectangle(50, 50, r.width-100, r.height-100);
                            e.gc.drawString("I am a Canvas", r.width/2, r.height/2);
                        }});
                    try {
                        System.err.println("Window handle.0 0x"+Long.toHexString(SWTAccessor.getWindowHandle(canvas[0])));
                    } catch (final Exception e) {
                        System.err.println(e.getMessage());
                    }
                    shell.setText( getClass().getName() );
                    shell.setBounds( 0, 0, 700, 700 );
                    shell.open();
                    canvas[0].redraw();
                }});

            System.err.println("Window handle.1 0x"+Long.toHexString(SWTAccessor.getWindowHandle(canvas[0])));

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
                SWTAccessor.invoke(true, new Runnable() {
                    public void run() {
                        System.err.println("Window handle.X 0x"+Long.toHexString(SWTAccessor.getWindowHandle(canvas[0])));
                    }});
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
        runTest();
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i], duration);
            }
        }
        org.junit.runner.JUnitCore.main( TestSWTAccessor01.class.getName() );
    }
}
