/**
 * Copyright 2020 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NewtTestUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
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

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.util.FPSAnimator;

/**
 * Test for Bug 1421: Child Newt Window position within an SWT (tab) layout
 * using NewtCanvasSWT on MacOS High-DPI.
 * <p>
 * Bug 1421 {@link #test01_tabFolderParent()} shows that the
 * inner child NEWT GLWindow is position wrongly.
 * It's position is shifted down about the height of the
 * parent TabFolder and right about the width of the same.
 * Since this works well on non High-DPI, I have to assume that
 * the scaling multiple is missing in calculating the window
 * position offset somehow.
 * </p>
 */
public class TestBug1421NewtCanvasSWTPosInTabs {

    static int duration = 250;
    Display display = null;
    Shell shell = null;

	private static final int FPS = 60; // animator's target frames per second

    @BeforeClass
    public static void startup() {
        GLProfile.initSingleton();
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
            }});
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        try {
            display.syncExec(new Runnable() {
               public void run() {
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
        display = null;
        shell = null;
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

    protected void runTestInLayout() throws InterruptedException {
		shell.setText("OneTriangle SWT");
		shell.setLayout(new FillLayout());
		shell.setSize(640, 480);

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new FillLayout());
		final CTabFolder tabFolder = new CTabFolder(composite, SWT.TOP);
		tabFolder.setBorderVisible(true);
		tabFolder.setLayoutData(new FillLayout());
		final CTabItem tabItem1 = new CTabItem(tabFolder, SWT.NONE, 0);
		tabItem1.setText("GLTab");
		final CTabItem tabItem2 = new CTabItem(tabFolder, SWT.NONE, 1);
		tabItem2.setText("Tab");

		// Get the default OpenGL profile, reflecting the best for your running platform
		final GLProfile glp = GLProfile.getDefault();
		// Specifies a set of OpenGL capabilities, based on your profile.
		final GLCapabilities caps = new GLCapabilities(glp);
		// Create the OpenGL rendering canvas
		final GLWindow window = GLWindow.create(caps);

		// Create a animator that drives canvas' display() at the specified FPS.
		final FPSAnimator animator = new FPSAnimator(window, FPS, true);

		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDestroyNotify(final WindowEvent arg0) {
				// Use a dedicate thread to run the stop() to ensure that the
				// animator stops before program exits.
				new Thread() {
					@Override
					public void run() {
						if (animator.isStarted())
							animator.stop(); // stop the animator loop
						System.exit(0);
					}
				}.start();
			}
		});

		window.addGLEventListener(new GearsES2());
		// window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		// window.setTitle(TITLE);
		// window.setVisible(true);

		final NewtCanvasSWT canvas = new NewtCanvasSWT(tabFolder, SWT.NO_BACKGROUND, window);
		// canvas.setSize(tabFolder.getClientArea().width,
		// tabFolder.getClientArea().height);
		tabItem1.setControl(canvas);
		canvas.setFocus();
		animator.start(); // start the animator loop
        display.syncExec( new Runnable() {
           public void run() {
              shell.open();
           }
        });
        Assert.assertTrue("GLWindow didn't become visible natively!", NewtTestUtil.waitForRealized(window, true, awtRobotWaitAction));

        final long lStartTime = System.currentTimeMillis();
        final long lEndTime = lStartTime + duration;
        try {
            while( (System.currentTimeMillis() < lEndTime) && !canvas.isDisposed() ) {
                generalWaitAction.run();
            }
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        if(null != animator) {
            animator.stop();
        }

        try {
            display.syncExec(new Runnable() {
               public void run() {
                canvas.dispose();
                tabFolder.dispose();
                composite.dispose();
               }});
        }
        catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
	}
    @Test
    public void test01_tabFolderParent() throws InterruptedException {
        runTestInLayout();
    }
    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i],  duration);
            }
        }
        System.out.println("durationPerTest: "+duration);
        org.junit.runner.JUnitCore.main(TestBug1421NewtCanvasSWTPosInTabs.class.getName());
    }
}
