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

import com.jogamp.opengl.util.Animator;

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
import java.util.List;
import com.jogamp.nativewindow.util.Dimension;

/**
 * Tests MonitorMode change w/ changed rotation,
 * w/ and w/o fullscreen, pre and post MonitorMode change.
 * <p>
 * MonitorMode change does not use highest resolution.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestScreenMode02aNEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;

    static int waitTimeShort = 4000; // 4 sec
    static int waitTimeLong = 8000; // 8 sec

    @BeforeClass
    public static void initClass() {
        setResetXRandRIfX11AfterClass();
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

    static GLWindow createWindow(final Screen screen, final GLCapabilities caps, final int width, final int height, final boolean onscreen, final boolean undecorated) {
        Assert.assertNotNull(caps);
        caps.setOnscreen(onscreen);

        final GLWindow window = GLWindow.create(screen, caps);
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
    public void testScreenRotationChange01_PreWin() throws InterruptedException {
        testScreenRotationChangeImpl(true, true, false);
    }

    @Test
    public void testScreenRotationChange02_PreFull() throws InterruptedException {
        testScreenRotationChangeImpl(true, true, true);
    }

    @Test
    public void testScreenRotationChange11_PostWin() throws InterruptedException {
        testScreenRotationChangeImpl(true, false, false);
    }

    @Test
    public void testScreenRotationChange12_PostFull() throws InterruptedException {
        testScreenRotationChangeImpl(true, false, true);
    }

    void testScreenRotationChangeImpl(final boolean changeMode, final boolean preVis, final boolean fullscreen) throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        final GLWindow window = createWindow(screen, caps, width, height, true /* onscreen */, false /* undecorated */);
        Assert.assertNotNull(window);
        if( preVis ) {
            window.setVisible(true);
            if( fullscreen ) {
                window.setFullscreen(true);
            }
        } else {
            screen.createNative();
            Assert.assertEquals(true,display.isNativeValid());
            Assert.assertEquals(true,screen.isNativeValid());
        }

        final Animator animator = new Animator(window);
        animator.start();

        final MonitorDevice monitor = window.getMainMonitor();
        final MonitorMode mmOrig = monitor.getOriginalMode();
        Assert.assertNotNull(mmOrig);
        if(changeMode) {
            Thread.sleep(waitTimeShort);

            List<MonitorMode> monitorModes = monitor.getSupportedModes();
            if(monitorModes.size()==1) {
                // no support ..
                System.err.println("Your platform has no ScreenMode change support, sorry");
                animator.stop();
                destroyWindow(window);
                return;
            }
            Assert.assertTrue(monitorModes.size()>0);

            MonitorMode mmCurrent = monitor.getCurrentMode();
            Assert.assertNotNull(mmCurrent);
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
        }

        if( !preVis ) {
            if( fullscreen ) {
                window.setFullscreen(true);
            }
            window.setVisible(true);
        }

        Thread.sleep(waitTimeLong);

        if( !preVis && fullscreen ) {
            window.setFullscreen(false);
        }

        if(changeMode) {
            Thread.sleep(waitTimeShort);

            // manual restore!
            {
                System.err.println("[1] set orig: "+mmOrig);
                final boolean smOk = monitor.setCurrentMode(mmOrig);
                final MonitorMode mmCurrent = monitor.getCurrentMode();
                System.err.println("[1] has orig?: "+mmCurrent+", changeOK "+smOk);
                Assert.assertFalse(monitor.isModeChangedByUs());
                Assert.assertEquals(mmOrig, mmCurrent);
                Assert.assertTrue(smOk);
            }
            Thread.sleep(waitTimeShort);
        }

        if( preVis && fullscreen ) {
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
        final String tstname = TestScreenMode02aNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
