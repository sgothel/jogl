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
import com.jogamp.newt.util.MonitorModeUtil;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

import java.util.List;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;

/**
 * Mode change on separate monitors ..
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestScreenMode01bNEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;

    static long waitTimeShort = 2000;
    static long duration = 6000;

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
            Assert.assertTrue(AWTRobotUtil.waitForRealized(window, false));
        }
    }

    @Test
    public void testScreenModeChangeSingleQ1() throws InterruptedException {
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        screen.addReference(); // trigger creation
        try {
            final RectangleImmutable monitorVp = screen.getMonitorDevices().get(0).getViewport();
            testScreenModeChangeImpl(screen, monitorVp.getX(), monitorVp.getY());
        } finally {
            screen.removeReference();
            Assert.assertTrue(AWTRobotUtil.waitForRealized(screen, false));
        }
    }

    @Test
    public void testScreenModeChangeSingleQ2() throws InterruptedException {
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
            testScreenModeChangeImpl(screen, monitorVp.getX(), monitorVp.getY());
        } finally {
            screen.removeReference();
            Assert.assertTrue(AWTRobotUtil.waitForRealized(screen, false));
        }
    }

    void testScreenModeChangeImpl(final Screen screen, final int screenXPos, final int screenYPos) throws InterruptedException {
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

        final Animator anim = new Animator(window0);
        anim.start();

        final List<MonitorMode> allMonitorModes = screen.getMonitorModes();
        Assert.assertTrue(allMonitorModes.size()>0);
        if(allMonitorModes.size()==1) {
            // no support ..
            System.err.println("Your platform has no MonitorMode change support (all), sorry");
            destroyWindow(window0);
            return;
        }

        final MonitorDevice monitor = window0.getMainMonitor();
        System.err.println("Test.0: Window monitor: "+monitor);

        List<MonitorMode> monitorModes = monitor.getSupportedModes();
        Assert.assertTrue(monitorModes.size()>0);
        if(monitorModes.size()==1) {
            // no support ..
            System.err.println("Your platform has no MonitorMode change support (monitor), sorry");
            destroyWindow(window0);
            return;
        }
        Assert.assertTrue(allMonitorModes.containsAll(monitorModes));

        MonitorMode mmCurrent = monitor.getCurrentMode();
        Assert.assertNotNull(mmCurrent);
        final MonitorMode mmOrig = monitor.getOriginalMode();
        Assert.assertNotNull(mmOrig);
        System.err.println("[0] orig   : "+mmOrig);
        System.err.println("[0] current: "+mmCurrent);
        Assert.assertEquals(mmCurrent, mmOrig);

        monitorModes = MonitorModeUtil.filterByFlags(monitorModes, 0); // no interlace, double-scan etc
        Assert.assertNotNull(monitorModes);
        Assert.assertTrue(monitorModes.size()>0);
        monitorModes = MonitorModeUtil.filterByRotation(monitorModes, 0);
        Assert.assertNotNull(monitorModes);
        Assert.assertTrue(monitorModes.size()>0);
        monitorModes = MonitorModeUtil.filterByResolution(monitorModes, new Dimension(801, 601));
        Assert.assertNotNull(monitorModes);
        Assert.assertTrue(monitorModes.size()>0);
        monitorModes = MonitorModeUtil.filterByRate(monitorModes, mmOrig.getRefreshRate());
        Assert.assertNotNull(monitorModes);
        Assert.assertTrue(monitorModes.size()>0);

        monitorModes = MonitorModeUtil.getHighestAvailableBpp(monitorModes);
        Assert.assertNotNull(monitorModes);
        Assert.assertTrue(monitorModes.size()>0);

        // set mode
        {
            final MonitorMode mm = monitorModes.get(0);
            System.err.println("[0] set current: "+mm);
            final boolean smOk = monitor.setCurrentMode(mm);
            mmCurrent = monitor.getCurrentMode();
            System.err.println("[0] has current: "+mmCurrent+", changeOK "+smOk);
            Assert.assertTrue(monitor.isModeChangedByUs());
            Assert.assertEquals(mm, mmCurrent);
            Assert.assertNotSame(mmOrig, mmCurrent);
            Assert.assertEquals(mmCurrent, monitor.queryCurrentMode());
            Assert.assertTrue(smOk);
        }

        window0WindowBounds = window0.getBounds();
        window0SurfaceSize = new Dimension(window0.getSurfaceWidth(), window0.getSurfaceHeight());
        System.err.println("Test.1: Screen           : "+screen);
        System.err.println("Test.1: Window bounds    : "+window0WindowBounds+" [wu] within "+screen.getViewportInWindowUnits()+" [wu]");
        System.err.println("Test.1: Window size      : "+window0SurfaceSize+" [pixels]");
        System.err.println("Test.1: Screen viewport  : "+screen.getViewport()+" [pixels]");
        System.err.println("Test.1: Monitor viewport : "+monitor.getViewport()+" [pixels], "+monitor.getViewportInWindowUnits()+" [wu]");
        System.err.println("Test.1: Window main-mon  : "+window0.getMainMonitor());

        Thread.sleep(duration);

        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window0.isNativeValid());
        Assert.assertEquals(true,window0.isVisible());

        // manual restore!
        {
            System.err.println("[1] set orig: "+mmOrig);
            final boolean smOk = monitor.setCurrentMode(mmOrig);
            mmCurrent = monitor.getCurrentMode();
            System.err.println("[1] has orig?: "+mmCurrent+", changeOK "+smOk);
            Assert.assertFalse(monitor.isModeChangedByUs());
            Assert.assertEquals(mmOrig, mmCurrent);
            Assert.assertTrue(smOk);
        }

        window0WindowBounds = window0.getBounds();
        window0SurfaceSize = new Dimension(window0.getSurfaceWidth(), window0.getSurfaceHeight());
        System.err.println("Test.2: Screen           : "+screen);
        System.err.println("Test.2: Window bounds    : "+window0WindowBounds+" [wu] within "+screen.getViewportInWindowUnits()+" [wu]");
        System.err.println("Test.2: Window size      : "+window0SurfaceSize+" [pixels]");
        System.err.println("Test.2: Screen viewport  : "+screen.getViewport()+" [pixels]");
        System.err.println("Test.2: Monitor viewport : "+monitor.getViewport()+" [pixels], "+monitor.getViewportInWindowUnits()+" [wu]");
        System.err.println("Test.2: Window main-mon  : "+window0.getMainMonitor());

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
        final String tstname = TestScreenMode01bNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
