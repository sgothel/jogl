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

import java.util.List;

import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.Rectangle;

/**
 * Tests X11 XRandR MonitorMode reset via {@link UITestCase#resetXRandRIfX11()}.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestScreenMode00cNEWT extends UITestCase {
    static boolean manualTest = false;
    static GLProfile glp;
    static int width, height;

    static final int waitTimeShort = 2000;
    static long duration = 4000;

    static int mm_width = 800;
    static int mm_height = 600;

    @BeforeClass
    public static void initClass() {
        setResetXRandRIfX11AfterClass();
        NativeWindowFactory.initSingleton();
        if( !manualTest || NativeWindowFactory.TYPE_X11 != NativeWindowFactory.getNativeWindowType(true) ) {
            setTestSupported(false);
            return;
        }
        width  = 100;
        height = 100;
        glp = GLProfile.getDefault();
    }

    @AfterClass
    public static void releaseClass() throws InterruptedException {
        Thread.sleep(waitTimeShort);
    }

    static Window createWindow(final Screen screen, final GLCapabilities caps, final String name, final int x, final int y, final int width, final int height) {
        Assert.assertNotNull(caps);

        final GLWindow window = GLWindow.create(screen, caps);
        // Window window = NewtFactory.createWindow(screen, caps);
        window.setTitle(name);
        window.setPosition(x, y);
        window.setSize(width, height);
        window.addGLEventListener(new GearsES2());
        Assert.assertNotNull(window);
        window.setVisible(true);
        return window;
    }

    static void destroyWindow(final Window window) throws InterruptedException {
        if(null!=window) {
            window.destroy();
            Assert.assertTrue(AWTRobotUtil.waitForRealized(window, false));
        }
    }

    @Test
    public void testScreenModeChange01() throws InterruptedException {
        Thread.sleep(waitTimeShort);

        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        final Window window0 = createWindow(screen, caps, "win0", 0, 0, width, height);
        Assert.assertNotNull(window0);

        Rectangle window0WindowBounds = window0.getBounds();
        DimensionImmutable window0SurfaceSize = new Dimension(window0.getSurfaceWidth(), window0.getSurfaceHeight());
        System.err.println("Test.0: Window bounds    : "+window0WindowBounds+" [wu] within "+screen.getViewportInWindowUnits()+" [wu]");
        System.err.println("Test.0: Window size      : "+window0SurfaceSize+" [pixels]");
        System.err.println("Test.0: Screen viewport  : "+screen.getViewport()+" [pixels]");

        final List<MonitorMode> allMonitorModes = screen.getMonitorModes();
        Assert.assertTrue(allMonitorModes.size()>0);
        if(allMonitorModes.size()==1) {
            // no support ..
            System.err.println("Your platform has no MonitorMode change support (all), sorry");
            destroyWindow(window0);
            return;
        }

        final MonitorDevice monitor = screen.getMonitorDevices().get(0);
        List<MonitorMode> monitorModes = monitor.getSupportedModes();
        Assert.assertTrue(monitorModes.size()>0);
        if(monitorModes.size()==1) {
            // no support ..
            System.err.println("Your platform has no MonitorMode change support (monitor), sorry");
            destroyWindow(window0);
            return;
        }
        Assert.assertTrue(allMonitorModes.containsAll(monitorModes));

        final MonitorMode mmSet0 = monitor.queryCurrentMode();
        Assert.assertNotNull(mmSet0);
        final MonitorMode mmOrig = monitor.getOriginalMode();
        Assert.assertNotNull(mmOrig);
        System.err.println("[0] orig   : "+mmOrig);
        System.err.println("[0] current: "+mmSet0);
        Assert.assertEquals(mmSet0, mmOrig);


        monitorModes = MonitorModeUtil.filterByFlags(monitorModes, 0); // no interlace, double-scan etc
        Assert.assertNotNull(monitorModes);
        Assert.assertTrue(monitorModes.size()>0);
        monitorModes = MonitorModeUtil.filterByRotation(monitorModes, 0);
        Assert.assertNotNull(monitorModes);
        Assert.assertTrue(monitorModes.size()>0);
        monitorModes = MonitorModeUtil.filterByResolution(monitorModes, new Dimension(mm_width+1, mm_height+1));
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
            System.err.println("[1] set current: "+mm);
            final boolean smOk = monitor.setCurrentMode(mm);
            final MonitorMode mmCachedCurrent = monitor.getCurrentMode();
            final MonitorMode mmQueriedCurrent = monitor.queryCurrentMode();
            final boolean mmCurrentEquals = mmQueriedCurrent.equals(mmCachedCurrent);
            System.err.println("[1] changeOK             : "+smOk);
            System.err.println("[1] has current cached   : "+mmCachedCurrent);
            System.err.println("[1] has current queried  : "+mmQueriedCurrent+", equal "+mmCurrentEquals);
            window0WindowBounds = window0.getBounds();
            window0SurfaceSize = new Dimension(window0.getSurfaceWidth(), window0.getSurfaceHeight());
            System.err.println("Test.1: Window bounds    : "+window0WindowBounds+" [wu] within "+screen.getViewportInWindowUnits()+" [wu]");
            System.err.println("Test.1: Window size      : "+window0SurfaceSize+" [pixels]");
            System.err.println("Test.1: Screen viewport  : "+screen.getViewport()+" [pixels]");

            Assert.assertTrue(monitor.isModeChangedByUs());
            Assert.assertEquals(mm, mmCachedCurrent);
            Assert.assertNotSame(mmOrig, mmCachedCurrent);
            Assert.assertEquals(mmCachedCurrent, mmQueriedCurrent);
            Assert.assertTrue(smOk);
        }

        Thread.sleep(duration);

        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window0.isNativeValid());
        Assert.assertEquals(true,window0.isVisible());

        // WARNING: See note in 'UITestCase.resetXRandRIfX11();'
        final int xrandrErrorCode;
        if( 0 == ( xrandrErrorCode = UITestCase.resetXRandRIfX11() ) ) {
            System.err.println("XRandR Reset :"+monitor.queryCurrentMode());
        } else {
            System.err.println("XRandR Reset : Failed w/ errorCode "+xrandrErrorCode+", fall back to manual reset");
            final boolean smOk = monitor.setCurrentMode(mmOrig);
            System.err.println("[X] changeOK             : "+smOk);
        }
        Thread.sleep(duration);
        validateScreenModeReset0(mmOrig);

        destroyWindow(window0);

        Thread.sleep(waitTimeShort);
        validateScreenModeReset(mmOrig);
    }

    void validateScreenModeReset0(final MonitorMode mmOrig) {
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        screen.addReference();
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());

        final MonitorDevice monitor = screen.getMonitorDevices().get(0);
        Assert.assertEquals(mmOrig, monitor.queryCurrentMode());

        screen.removeReference();
    }
    void validateScreenModeReset(final MonitorMode mmOrig) {
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertEquals(false,screen.isNativeValid());
        screen.addReference();
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());

        final MonitorDevice monitor = screen.getMonitorDevices().get(0);
        Assert.assertEquals(mmOrig, monitor.getCurrentMode());

        screen.removeReference();
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertEquals(false,screen.isNativeValid());
    }

    public static void main(final String args[]) throws IOException {
        manualTest = true;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-mwidth")) {
                i++;
                mm_width = MiscUtils.atoi(args[i], mm_width);
            } else if(args[i].equals("-mheight")) {
                i++;
                mm_height = MiscUtils.atoi(args[i], mm_height);
            }
        }
        System.err.println("Desired mode w/ resolution "+mm_width+"x"+mm_height);
        final String tstname = TestScreenMode00cNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
