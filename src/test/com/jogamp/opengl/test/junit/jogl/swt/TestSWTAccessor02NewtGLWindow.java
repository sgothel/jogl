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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.UpstreamWindowHookMutableSizePos;
import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.newt.util.EDTUtil;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.GLTestUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NewtTestUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

import jogamp.newt.swt.SWTEDTUtil;

/**
 * Tests utilizing {@link SWTAccessor#getWindowHandle(org.eclipse.swt.widgets.Control)}
 * for NEWT native window reparenting also using GL rendering {@link GLWindow#reparentWindow(NativeWindow, int, int, int)}.
 * <p>
 * This tests re-creates {@link NewtCanvasSWT}'s implementation ad-hock, allowing simplified debugging.
 * </p>
 * <p>
 * Enhanced version of {@link TestSWTAccessor01}.
 * </p>
 * <p>
 * Bug 1362 inspired this unit test, i.e. finding the issue of SWT >= 4.10 + GTK3.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSWTAccessor02NewtGLWindow extends UITestCase {

    static int duration = 250;

    Display display = null;
    Shell shell = null;
    Composite composite = null;

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

    protected void release(final GLWindow glwin) throws InterruptedException, InvocationTargetException {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );

        SWTAccessor.invoke(true, new Runnable() {
            public void run() {
                glwin.destroy();
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

        final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
        final GLWindow glwin = GLWindow.create(caps);
        final GearsES2 demo = new GearsES2();
        glwin.addGLEventListener(demo);
        glwin.setSize(600, 600);

        // set SWT EDT and start it
        {
            final com.jogamp.newt.Display newtDisplay = glwin.getScreen().getDisplay();
            final EDTUtil edtUtil = new SWTEDTUtil(newtDisplay, display);
            edtUtil.start();
            newtDisplay.setEDTUtil( edtUtil );
        }
        final Canvas canvas[] = { null };
        try {
            display.syncExec( new Runnable() {
               public void run() {
                    canvas[0] = new Canvas (composite, SWT.NO_BACKGROUND);
                    // Bug 1362 fix or workaround: Seems SWT/GTK3 at least performs lazy initialization
                    // Minimal action required: setBackground of the parent canvas before reparenting!
                    canvas[0].setBackground(new Color(display, 255, 255, 255));
                    shell.setText( getClass().getName() );
                    shell.setBounds( 0, 0, 700, 700 );
                    shell.open();

                    // A full rolled-out native window reparenting example, very suitable to debug
                    final long parentWinHandle = SWTAccessor.getWindowHandle(canvas[0]);
                    final AbstractGraphicsScreen aScreen = NativeWindowFactory.createScreen(NativeWindowFactory.createDevice(null, true /* own */), -1);
                    final UpstreamWindowHookMutableSizePos upstreamSizePosHook = new UpstreamWindowHookMutableSizePos(0, 0, 600, 600, 600, 600);
                    final Listener listener = new Listener () {
                        @Override
                        public void handleEvent (final Event event) {
                            switch (event.type) {
                            case SWT.Paint:
                                glwin.display();
                                break;
                            case SWT.Move:
                            case SWT.Resize: {
                                    final Rectangle nClientArea = canvas[0].getClientArea();
                                    if( null != nClientArea ) {
                                        upstreamSizePosHook.setSurfaceSize(nClientArea.width, nClientArea.height);
                                        upstreamSizePosHook.setWinSize(nClientArea.width, nClientArea.height);
                                        upstreamSizePosHook.setWinPos(nClientArea.x, nClientArea.y);
                                        if( SWT.Resize == event.type ) {
                                            glwin.setSize(nClientArea.width, nClientArea.height);
                                        }
                                    }
                                }
                                break;
                            case SWT.Dispose:
                                glwin.destroy();
                                break;
                            }
                        }
                    };
                    canvas[0].addListener (SWT.Move, listener);
                    canvas[0].addListener (SWT.Resize, listener);
                    canvas[0].addListener (SWT.Paint, listener);
                    canvas[0].addListener (SWT.Dispose, listener);

                    final Rectangle r = canvas[0].getClientArea();
                    final NativeWindow parentWindow = NativeWindowFactory.createWrappedWindow(aScreen, 0 /* surfaceHandle*/, parentWinHandle, upstreamSizePosHook);
                    glwin.setSize(r.width, r.height);
                    glwin.reparentWindow(parentWindow, 0, 0, 0);
                    glwin.setPosition(r.x, r.y);
                    glwin.setVisible(true);
                    canvas[0].redraw();
                }});

            final Runnable waitAction = new Runnable() {
                public void run() {
                    if( !display.readAndDispatch() ) {
                        try {
                            Thread.sleep(10);
                        } catch (final InterruptedException e) { }
                    }
                } };
            Assert.assertEquals(true,  NewtTestUtil.waitForVisible(glwin, true, waitAction));
            Assert.assertEquals(true,  GLTestUtil.waitForRealized(glwin, true, waitAction));

            System.err.println("Window handle.1 0x"+Long.toHexString(SWTAccessor.getWindowHandle(canvas[0])));

            final long lStartTime = System.currentTimeMillis();
            final long lEndTime = lStartTime + duration;
            while( System.currentTimeMillis() < lEndTime && !composite.isDisposed() ) {
                waitAction.run();
            }
        } finally {
            release(glwin);
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
        org.junit.runner.JUnitCore.main( TestSWTAccessor02NewtGLWindow.class.getName() );
    }
}
