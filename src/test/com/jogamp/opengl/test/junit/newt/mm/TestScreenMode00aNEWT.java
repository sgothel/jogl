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

import com.jogamp.nativewindow.NativeWindowFactory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.Screen;
import com.jogamp.newt.util.MonitorModeUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.util.Iterator;
import java.util.List;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.nativewindow.util.SurfaceSize;
import com.jogamp.opengl.GLProfile;

import jogamp.newt.MonitorDeviceImpl;
import jogamp.newt.MonitorModeProps;

/**
 * Validating consistency of MonitorMode data from Screen (all modes)
 * and from a particular MonitorDevice.
 * <p>
 * Also validates the descending order of the given MonitorMode lists.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestScreenMode00aNEWT extends UITestCase {
    static int screenIdx = 0;
    static int width, height;

    static int waitTimeShort = 4; //1 sec
    static int waitTimeLong = 6; //6 sec



    @BeforeClass
    public static void initClass() {
        setResetXRandRIfX11AfterClass();
        GLProfile.initSingleton(); // hack to initialize GL for BCM_IV (Rasp.Pi)
        NativeWindowFactory.initSingleton();
        width  = 640;
        height = 480;
    }

    @Test
    public void testScreenModeInfo00() throws InterruptedException {
        final DimensionImmutable res = new Dimension(640, 480);
        final SurfaceSize surfsz = new SurfaceSize(res, 32);
        final MonitorMode modeOut = new MonitorMode(surfsz, 60.0f, 0, 0);
        System.err.println("00 out: "+modeOut);
        final MonitorModeProps.Cache cache = new MonitorModeProps.Cache();
        cache.monitorModes.add(modeOut);
        {
            final int[] props = MonitorModeProps.streamOutMonitorMode(modeOut);
            final MonitorMode modeIn = MonitorModeProps.streamInMonitorMode(null, cache, props, 0);
            System.err.println("00 in : "+modeIn);

            Assert.assertEquals(modeOut.getSurfaceSize().getResolution(), modeIn.getSurfaceSize().getResolution());

            Assert.assertEquals(modeOut.getSurfaceSize(), modeIn.getSurfaceSize());

            Assert.assertEquals(modeOut.hashCode(), modeIn.hashCode());

            Assert.assertEquals(modeOut, modeIn);
        }

        final DimensionImmutable sizeMM = new Dimension(50, 50);
        final Rectangle viewport = new Rectangle(0, 0, 1920, 1080);
        final ArrayHashSet<MonitorMode> supportedModes = new ArrayHashSet<MonitorMode>();
        supportedModes.add(modeOut);
        final MonitorDevice monOut = new MonitorDeviceImpl(null, -1, false, sizeMM, modeOut, null, viewport, viewport, supportedModes);
        System.err.println("01 out : "+monOut);
        cache.monitorDevices.add(monOut);
        {
            final int[] props = MonitorModeProps.streamOutMonitorDevice(monOut);
            final MonitorDevice monIn = MonitorModeProps.streamInMonitorDevice(cache, null, null, props, 0, null);
            System.err.println("01 in : "+monIn);

            Assert.assertEquals(monOut.getCurrentMode(), monOut.getOriginalMode());
            Assert.assertEquals(monOut.getSupportedModes(), monIn.getSupportedModes());
            Assert.assertEquals(monOut.getViewport(), monIn.getViewport());
            Assert.assertEquals(monOut.getViewportInWindowUnits(), monIn.getViewportInWindowUnits());
            Assert.assertEquals(monOut.getOriginalMode(), monIn.getOriginalMode());
            Assert.assertEquals(monOut.getCurrentMode(), monIn.getCurrentMode());
            Assert.assertEquals(monOut.hashCode(), monIn.hashCode());
            Assert.assertEquals(monOut, monIn);
        }
    }

    @Test
    public void testScreenModeInfo01() throws InterruptedException {
        final Display dpy = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(dpy, screenIdx);
        screen.addReference();
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,screen.getDisplay().isNativeValid());
        System.err.println("Screen: "+screen.toString());
        final List<MonitorMode> allMonitorModes = screen.getMonitorModes();
        Assert.assertTrue(allMonitorModes.size()>0);
        {
            int i=0;
            MonitorMode mmPre = null;
            for(final Iterator<MonitorMode> iMode=allMonitorModes.iterator(); iMode.hasNext(); i++) {
                final MonitorMode mm = iMode.next();
                System.err.println(String.format("All-0[%03d]: %s", i, mm));
                if( null != mmPre ) {
                    Assert.assertTrue("Wrong order", mmPre.compareTo(mm) >= 0);
                }
                mmPre = mm;
            }
        }
        MonitorModeUtil.sort(allMonitorModes, true /* ascendingOrder*/);
        {
            int i=0;
            MonitorMode mmPre = null;
            for(final Iterator<MonitorMode> iMode=allMonitorModes.iterator(); iMode.hasNext(); i++) {
                final MonitorMode mm = iMode.next();
                System.err.println(String.format("All-1[%03d]: %s", i, mm));
                if( null != mmPre ) {
                    Assert.assertTrue("Wrong order", mmPre.compareTo(mm) <= 0);
                }
                mmPre = mm;
            }
        }

        final List<MonitorDevice> monitors = screen.getMonitorDevices();
        Assert.assertTrue(monitors.size()>0);

        // Dump all Monitor's and its modes
        int j=0;
        for(final Iterator<MonitorDevice> iMonitor=monitors.iterator(); iMonitor.hasNext(); j++) {
            final MonitorDevice monitor = iMonitor.next();
            System.err.println(j+": "+monitor);
            final List<MonitorMode> modes = monitor.getSupportedModes();
            Assert.assertTrue(modes.size()>0);
            int i=0;
            MonitorMode mmPre = null;
            for(final Iterator<MonitorMode> iMode=modes.iterator(); iMode.hasNext(); i++) {
                final MonitorMode mm = iMode.next();
                System.err.println(String.format("[%02d][%03d]: %s", j, i, mm));
                if( null != mmPre ) {
                    Assert.assertTrue("Wrong order", mmPre.compareTo(mm) >= 0);
                }
                mmPre = mm;
            }
            Assert.assertTrue(allMonitorModes.containsAll(modes));
        }

        // Dump all Monitor's and its DPI and current/original mode
        j=0;
        for(final Iterator<MonitorDevice> iMonitor=monitors.iterator(); iMonitor.hasNext(); j++) {
            final MonitorDevice monitor = iMonitor.next();
            System.err.println(j+": "+monitor);
            final float[] pixelPerMM = monitor.getPixelsPerMM(new float[2]);
            System.err.println(j+" pixel/mm ["+pixelPerMM[0]+", "+pixelPerMM[1]+"]");
            System.err.println(j+" pixel/in ["+pixelPerMM[0]*25.4f+", "+pixelPerMM[1]*25.4f+"]");
            final MonitorMode sm_o = monitor.getOriginalMode();
            Assert.assertNotNull(sm_o);
            final MonitorMode sm_c = monitor.queryCurrentMode();
            System.err.println("[0] orig   : "+sm_o);
            System.err.println("[0] current: "+sm_c);
            Assert.assertNotNull(sm_c);
            Assert.assertEquals(sm_o, sm_c);
        }

        final RectangleImmutable zero = new Rectangle();

        final Rectangle monitorViewPU = new Rectangle();
        final Rectangle monitorViewWU = new Rectangle();
        MonitorDevice.unionOfViewports(monitorViewPU, monitorViewWU, monitors);
        System.err.println("Test.0: Monitor union viewport: "+monitorViewPU+" [pu] / "+monitorViewWU+" [wu]");
        Assert.assertNotEquals(zero, monitorViewPU);
        Assert.assertNotEquals(zero, monitorViewWU);

        final RectangleImmutable screenViewPU = screen.getViewport();
        final RectangleImmutable screenViewWU = screen.getViewportInWindowUnits();
        System.err.println("Test.1: Screen viewport: "+screenViewPU+" [pu] / "+screenViewWU+" [wu]");
        Assert.assertNotEquals(zero, screenViewPU);
        Assert.assertNotEquals(zero, screenViewWU);

        screen.removeReference();

        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,screen.getDisplay().isNativeValid());
    }

    static int atoi(final String a) {
        try {
            return Integer.parseInt(a);
        } catch (final Exception ex) { throw new RuntimeException(ex); }
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-screen")) {
                i++;
                screenIdx = atoi(args[i]);
            }
        }
        final String tstname = TestScreenMode00aNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
