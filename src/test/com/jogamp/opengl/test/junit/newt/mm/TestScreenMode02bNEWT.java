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

import com.jogamp.common.os.Platform;
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
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

import java.util.List;
import com.jogamp.nativewindow.util.Dimension;

/**
 * Tests MonitorMode change w/ changed rotation and fullscreen.
 * <p>
 * MonitorMode change uses highest resolution.
 * </p>
 * <p>
 * Bug 734 could not be reproduced, however on tests systems
 * here - AMD fglrx and Intel Mesa, the rotated height
 * is cut off .. probably due to bug of driver code and rotation.
 * </p>
 * <p>
 * Documents remedy B) for NV RANDR/GL bug
 * </p>
 *
 * @see TestScreenMode01NEWT#cleanupGL()
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestScreenMode02bNEWT extends UITestCase {
    static GLProfile glp;

    static int waitTimeShort = 2000; // 2 sec
    static int waitTimeLong = 8000; // 8 sec

    @BeforeClass
    public static void initClass() {
        setResetXRandRIfX11AfterClass();
        glp = GLProfile.getDefault();
    }

    @AfterClass
    public static void releaseClass() throws InterruptedException {
        Thread.sleep(waitTimeShort);
    }

    static GLWindow createWindow(final Screen screen, final GLCapabilities caps, final String name, final int x, final int y, final int width, final int height) {
        Assert.assertNotNull(caps);

        final GLWindow window = GLWindow.create(screen, caps);
        // Window window = NewtFactory.createWindow(screen, caps);
        window.setTitle(name);
        window.setPosition(x, y);
        window.setSize(width, height);
        window.addGLEventListener(new GearsES2(1));
        Assert.assertNotNull(window);
        return window;
    }

    static void destroyWindow(final Window window) throws InterruptedException {
        if(null!=window) {
            window.destroy();
            Assert.assertTrue(AWTRobotUtil.waitForRealized(window, false));
        }
    }

    @Test
    public void testScreenModeChange01_PreFull() throws InterruptedException {
        testScreenModeChangeImpl(true);
    }

    @Test
    public void testScreenModeChange02_PostFull() throws InterruptedException {
        testScreenModeChangeImpl(false);
    }

    void testScreenModeChangeImpl(final boolean preVis) throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        screen.createNative(); // instantiate for resolution query and keep it alive !
        final int swidth = screen.getWidth();
        final int sheight = screen.getHeight();

        final GLWindow window = createWindow(screen, caps, "win0", 0, 0, 640, 480);
        if( preVis ) {
            window.setVisible(true);
            window.setFullscreen(true);
        }
        window.setUndecorated(true);
        Assert.assertNotNull(window);

        final List<MonitorMode> allMonitorModes = screen.getMonitorModes();
        Assert.assertTrue(allMonitorModes.size()>0);
        if(allMonitorModes.size()==1) {
            // no support ..
            System.err.println("Your platform has no MonitorMode change support (all), sorry");
            destroyWindow(window);
            return;
        }

        final MonitorDevice monitor = window.getMainMonitor();
        List<MonitorMode> monitorModes = monitor.getSupportedModes();
        Assert.assertTrue(monitorModes.size()>0);
        if(monitorModes.size()==1) {
            // no support ..
            System.err.println("Your platform has no MonitorMode change support (monitor), sorry");
            destroyWindow(window);
            return;
        }
        Assert.assertTrue(allMonitorModes.containsAll(monitorModes));

        final Animator animator = new Animator(window);
        animator.start();

        MonitorMode mmCurrent = monitor.queryCurrentMode();
        Assert.assertNotNull(mmCurrent);
        final MonitorMode mmOrig = monitor.getOriginalMode();
        Assert.assertNotNull(mmOrig);
        System.err.println("[0] orig   : "+mmOrig);
        System.err.println("[0] current: "+mmCurrent);
        Assert.assertEquals(mmCurrent, mmOrig);

        monitorModes = MonitorModeUtil.filterByFlags(monitorModes, 0); // no interlace, double-scan etc
        Assert.assertNotNull(monitorModes);
        Assert.assertTrue(monitorModes.size()>0);
        monitorModes = MonitorModeUtil.filterByRotation(monitorModes, 90);
        if(null==monitorModes || Platform.getOSType() == Platform.OSType.MACOS ) {
            // no rotation support ..
            System.err.println("Your platform has no rotation support, sorry");
            animator.stop();
            destroyWindow(window);
            return;
        }
        monitorModes = MonitorModeUtil.filterByResolution(monitorModes, new Dimension(swidth, sheight));
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
            final MonitorMode mm = monitorModes.get(0); // highest resolution ..
            System.err.println("[0] set current: "+mm);
            final boolean smOk = monitor.setCurrentMode(mm);
            mmCurrent = monitor.getCurrentMode();
            System.err.println("[0] has current: "+mmCurrent+", changeOK "+smOk);
            if( !smOk ) {
                System.err.println("ERROR: Full MonitorMode w/ rotation failure - Expected on some platforms (NV driver) - Tolerated for now.");
                animator.stop();
                destroyWindow(window);
                return;
            }
            Assert.assertTrue(monitor.isModeChangedByUs());
            Assert.assertEquals(mm, mmCurrent);
            Assert.assertNotSame(mmOrig, mmCurrent);
            Assert.assertEquals(mmCurrent, monitor.queryCurrentMode());
            Assert.assertTrue(smOk);
        }

        if( !preVis ) {
            window.setFullscreen(true);
            window.setVisible(true);
        }

        Thread.sleep(waitTimeLong);

        if( !preVis ) {
            window.setFullscreen(false);
        }

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
        Thread.sleep(waitTimeShort);

        if( preVis ) {
            window.setFullscreen(false);
        }

        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());

        animator.stop();
        destroyWindow(window);

        Assert.assertEquals(false,window.isVisible());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertTrue(AWTRobotUtil.waitForRealized(screen, false));
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,display.isNativeValid());
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestScreenMode02bNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
