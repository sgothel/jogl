/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.newt.mm;

import java.io.IOException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;

/**
 * Fullscreen on separate monitors, incl. spanning across multiple monitors.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestScreenMode01cNEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;

    static long waitTimeShort = 2000;
    static long duration = 4000;

    @BeforeClass
    public static void initClass() {
        setResetXRandRIfX11AfterClass();
        width  = 200;
        height = 200;
        glp = GLProfile.getDefault();
    }

    @AfterClass
    public static void releaseClass() throws InterruptedException {
        Thread.sleep(waitTimeShort);
    }

    static GLWindow createWindow(final Screen screen, final GLCapabilities caps, final String name, final int screenXPos, final int screenYPos, final int width, final int height) throws InterruptedException {
        Assert.assertNotNull(caps);

        final GLWindow window = GLWindow.create(screen, caps);
        // Window window = NewtFactory.createWindow(screen, caps);
        final int[] winPos = window.convertToWindowUnits(new int[] { screenXPos, screenYPos });
        window.setTitle(name);
        window.setPosition(winPos[0], winPos[1]);
        window.setSize(width, height);
        window.addGLEventListener(new GearsES2());
        Assert.assertNotNull(window);
        final long t0 = System.currentTimeMillis();
        window.setVisible(true);
        System.err.println("Time for visible/pos: "+(System.currentTimeMillis()-t0)+" ms");
        return window;
    }

    static void destroyWindow(final Window window) throws InterruptedException {
        if(null!=window) {
            window.destroy();
            AWTRobotUtil.waitForRealized(window, false); // don't override a previous assertion failure
        }
    }

    @Test
    public void test01ScreenFullscreenSingleQ1() throws InterruptedException {
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        screen.addReference(); // trigger creation
        try {
            final RectangleImmutable monitorVp = screen.getMonitorDevices().get(0).getViewport();
            testScreenFullscreenImpl(screen, monitorVp.getX(), monitorVp.getY(), false, null);
        } finally {
            screen.removeReference();
            AWTRobotUtil.waitForRealized(screen, false); // don't override a previous assertion failure
        }
    }

    @Test
    public void test02ScreenFullscreenSingleQ2() throws InterruptedException {
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        screen.addReference(); // trigger creation
        try {
            if( 2 > screen.getMonitorDevices().size() ) {
                System.err.println("Test Disabled (1): Monitor count < 2: "+screen);
                return;
            }
            final RectangleImmutable monitorVp = screen.getMonitorDevices().get(1).getViewport();
            testScreenFullscreenImpl(screen, monitorVp.getX(), monitorVp.getY(), false, null);
        } finally {
            screen.removeReference();
            AWTRobotUtil.waitForRealized(screen, false); // don't override a previous assertion failure
        }
    }

    @Test
    public void test03ScreenFullscreenSpanQ1Q2() throws InterruptedException {
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        screen.addReference(); // trigger creation
        try {
            if( 2 > screen.getMonitorDevices().size() ) {
                System.err.println("Test Disabled (2): Spanning monitor count < 2: "+screen);
                return;
            }
            final ArrayList<MonitorDevice> monitors = new ArrayList<MonitorDevice>();
            monitors.add(screen.getMonitorDevices().get(0)); // Q1
            monitors.add(screen.getMonitorDevices().get(1)); // Q2
            final RectangleImmutable monitorVp = screen.getMonitorDevices().get(0).getViewport();
            testScreenFullscreenImpl(screen, monitorVp.getX()+50, monitorVp.getY()+50, true, monitors);
        } finally {
            screen.removeReference();
            AWTRobotUtil.waitForRealized(screen, false); // don't override a previous assertion failure
        }
    }

    @Test
    public void test04ScreenFullscreenSpanALL() throws InterruptedException {
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        screen.addReference(); // trigger creation
        try {
            if( 2 > screen.getMonitorDevices().size() ) {
                System.err.println("Test Disabled (3): Monitor count < 2: "+screen);
                return;
            }
            final RectangleImmutable monitorVp = screen.getMonitorDevices().get(1).getViewport();
            testScreenFullscreenImpl(screen, monitorVp.getX()-50, monitorVp.getY()+50, true, null);
        } finally {
            screen.removeReference();
            AWTRobotUtil.waitForRealized(screen, false); // don't override a previous assertion failure
        }
    }

    void testScreenFullscreenImpl(final Screen screen, final int screenXPos, final int screenYPos,
                                  final boolean spanAcrossMonitors, final List<MonitorDevice> monitors) throws InterruptedException {
        Thread.sleep(waitTimeShort);

        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        final Display display = screen.getDisplay();

        System.err.println("Test.0: Window screen: "+screen);

        System.err.println("Test.0: Window bounds (pre): screenPos "+screenXPos+"/"+screenYPos+" [pixels], windowSize "+width+"x"+height+" [wu] within "+screen.getViewport()+" [pixels]");

        final GLWindow window0 = createWindow(screen, caps, "win0", screenXPos, screenYPos, width, height);
        Assert.assertNotNull(window0);
        Rectangle window0WindowBounds = window0.getBounds();
        DimensionImmutable window0SurfaceSize = new Dimension(window0.getSurfaceWidth(), window0.getSurfaceHeight());
        System.err.println("Test.0: Window bounds    : "+window0WindowBounds+" [wu] within "+screen.getViewportInWindowUnits()+" [wu]");
        System.err.println("Test.0: Window size      : "+window0SurfaceSize+" [pixels]");
        System.err.println("Test.0: Screen viewport  : "+screen.getViewport()+" [pixels]");

        final Animator anim = new Animator(window0);
        anim.start();

        final List<MonitorMode> allMonitorModes = screen.getMonitorModes();
        Assert.assertTrue(allMonitorModes.size()>0);

        MonitorDevice monitor = window0.getMainMonitor();
        System.err.println("Test.0: Window monitor: "+monitor);
        if( !spanAcrossMonitors ) {
            window0.setFullscreen(true);
        } else {
            window0.setFullscreen(monitors);
        }

        monitor = window0.getMainMonitor();
        window0WindowBounds = window0.getBounds();
        window0SurfaceSize = new Dimension(window0.getSurfaceWidth(), window0.getSurfaceHeight());
        System.err.println("Test.1: Window bounds    : "+window0WindowBounds+" [wu] within "+screen.getViewportInWindowUnits()+" [wu]");
        System.err.println("Test.1: Window size      : "+window0SurfaceSize+" [pixels]");
        System.err.println("Test.1: Screen viewport  : "+screen.getViewport()+" [pixels]");
        System.err.println("Test.1: Monitor viewport : "+monitor.getViewport()+" [pixels], "+monitor.getViewportInWindowUnits()+" [wu]");
        if( !spanAcrossMonitors ) {
            Assert.assertEquals(monitor.getViewportInWindowUnits(), window0WindowBounds);
        } else {
            List<MonitorDevice> monitorsUsed = monitors;
            if( null == monitorsUsed ) {
                monitorsUsed = window0.getScreen().getMonitorDevices();
            }
            final Rectangle monitorsUsedViewport = new Rectangle();
            MonitorDevice.unionOfViewports(null, monitorsUsedViewport, monitorsUsed);
            Assert.assertEquals(monitorsUsedViewport,  window0WindowBounds);
        }

        Thread.sleep(duration);

        window0.setFullscreen(false);

        window0WindowBounds = window0.getBounds();
        window0SurfaceSize = new Dimension(window0.getSurfaceWidth(), window0.getSurfaceHeight());;
        monitor = window0.getMainMonitor();
        System.err.println("Test.2: Window bounds    : "+window0WindowBounds+" [wu] within "+screen.getViewportInWindowUnits()+" [wu]");
        System.err.println("Test.2: Window size      : "+window0SurfaceSize+" [pixels]");
        System.err.println("Test.2: Screen viewport  : "+screen.getViewport()+" [pixels]");
        System.err.println("Test.2: Monitor viewport : "+monitor.getViewport()+" [pixels], "+monitor.getViewportInWindowUnits()+" [wu]");

        Thread.sleep(duration);
        anim.stop();
        destroyWindow(window0);
        Assert.assertEquals(false,window0.isVisible());
        Assert.assertEquals(false,window0.isNativeValid());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        final String tstname = TestScreenMode01cNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
