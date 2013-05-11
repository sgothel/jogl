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
 
package com.jogamp.opengl.test.junit.newt;

import java.io.IOException;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.opengl.util.Animator;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
import com.jogamp.opengl.test.junit.util.UITestCase;
import java.util.List;
import javax.media.nativewindow.util.Dimension;

public class TestScreenMode02NEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;
    
    static int waitTimeShort = 2000; // 2 sec
    static int waitTimeLong = 8000; // 8 sec

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

    @AfterClass
    public static void releaseClass() throws InterruptedException {
        Thread.sleep(waitTimeShort);
    }
    
    static GLWindow createWindow(Screen screen, GLCapabilities caps, int width, int height, boolean onscreen, boolean undecorated) {
        Assert.assertNotNull(caps);
        caps.setOnscreen(onscreen);

        GLWindow window = GLWindow.create(screen, caps);
        window.setSize(width, height);
        window.addGLEventListener(new GearsES2());
        Assert.assertNotNull(window);
        window.setVisible(true);
        Assert.assertTrue(window.isVisible());
        return window;
    }

    static void destroyWindow(Window window) {
        if(null!=window) {
            window.destroy();
        }
    }
    
    @Test
    public void testScreenRotationChange01() throws InterruptedException {
        Thread.sleep(waitTimeShort);

        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        GLWindow window = createWindow(screen, caps, width, height, true /* onscreen */, false /* undecorated */);
        Assert.assertNotNull(window);

        MonitorDevice monitor = window.getMainMonitor();
        List<MonitorMode> monitorModes = monitor.getSupportedModes();
        if(monitorModes.size()==1) {
            // no support ..
            System.err.println("Your platform has no ScreenMode change support, sorry");
            destroyWindow(window);
            return;
        }
        Assert.assertTrue(monitorModes.size()>0);

        Animator animator = new Animator(window);
        animator.start();

        MonitorMode mmCurrent = monitor.getCurrentMode();
        Assert.assertNotNull(mmCurrent);
        MonitorMode mmOrig = monitor.getOriginalMode();
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

        MonitorMode sm = (MonitorMode) monitorModes.get(0);
        System.err.println("[0] set current: "+sm);
        monitor.setCurrentMode(sm);
        Assert.assertTrue(monitor.isModeChangedByUs());
        Assert.assertEquals(sm, monitor.getCurrentMode());
        Assert.assertNotSame(mmOrig, monitor.getCurrentMode());
        Assert.assertEquals(sm, monitor.queryCurrentMode());

        Thread.sleep(waitTimeLong);

        // check reset ..

        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());

        animator.stop();
        destroyWindow(window);

        Assert.assertEquals(false,window.isVisible());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,display.isNativeValid());

        screen.createNative(); // trigger native re-creation

        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());

        mmCurrent = monitor.getCurrentMode();
        System.err.println("[1] current/orig: "+mmCurrent);

        Assert.assertNotNull(mmCurrent);
        Assert.assertEquals(mmCurrent, mmOrig);

        screen.destroy();

        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,display.isNativeValid());
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestScreenMode02NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
