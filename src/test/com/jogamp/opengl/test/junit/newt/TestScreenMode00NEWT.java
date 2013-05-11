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
import javax.media.nativewindow.NativeWindowFactory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.Screen;
import com.jogamp.opengl.test.junit.util.UITestCase;
import java.util.Iterator;
import java.util.List;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.SurfaceSize;
import javax.media.opengl.GLProfile;

import jogamp.newt.MonitorDeviceImpl;
import jogamp.newt.MonitorModeProps;

public class TestScreenMode00NEWT extends UITestCase {
    static int screenIdx = 0;
    static int width, height;
    
    static int waitTimeShort = 4; //1 sec
    static int waitTimeLong = 6; //6 sec
    
    

    @BeforeClass
    public static void initClass() {
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
        final MonitorDevice monOut = new MonitorDeviceImpl(null, -1, sizeMM, viewport, modeOut, supportedModes);
        System.err.println("01 out : "+monOut);
        cache.monitorDevices.add(monOut);
        {
            final int[] props = MonitorModeProps.streamOutMonitorDevice(monOut);
            final MonitorDevice monIn = MonitorModeProps.streamInMonitorDevice(null, cache, null, props, 0);
            System.err.println("01 in : "+monIn);
            
            Assert.assertEquals(monOut.getCurrentMode(), monOut.getOriginalMode());            
            Assert.assertEquals(monOut.getSupportedModes(), monIn.getSupportedModes());
            Assert.assertEquals(monOut.getViewport(), monIn.getViewport());
            Assert.assertEquals(monOut.getOriginalMode(), monIn.getOriginalMode());
            Assert.assertEquals(monOut.getCurrentMode(), monIn.getCurrentMode());
            Assert.assertEquals(monOut.hashCode(), monIn.hashCode());
            Assert.assertEquals(monOut, monIn);
        }
    }

    @Test
    public void testScreenModeInfo01() throws InterruptedException {
        Display dpy = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(dpy, screenIdx);
        screen.addReference();
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,screen.getDisplay().isNativeValid());
        System.err.println("Screen: "+screen.toString());
        List<MonitorMode> allMonitorModes = screen.getMonitorModes();
        Assert.assertTrue(allMonitorModes.size()>0);
        {
            int i=0;
            for(Iterator<MonitorMode> iMode=allMonitorModes.iterator(); iMode.hasNext(); i++) {
                System.err.println("All["+i+"]: "+iMode.next());
            }
        }
        
        List<MonitorDevice> monitors = screen.getMonitorDevices();
        Assert.assertTrue(monitors.size()>0);
        int j=0;
        for(Iterator<MonitorDevice> iMonitor=monitors.iterator(); iMonitor.hasNext(); j++) {
            MonitorDevice monitor = iMonitor.next();
            System.err.println(j+": "+monitor);
            List<MonitorMode> modes = monitor.getSupportedModes();
            Assert.assertTrue(modes.size()>0);
            int i=0;
            for(Iterator<MonitorMode> iMode=modes.iterator(); iMode.hasNext(); i++) {
                System.err.println("["+j+"]["+i+"]: "+iMode.next());
            }
            Assert.assertTrue(allMonitorModes.containsAll(modes));

            MonitorMode sm_o = monitor.getOriginalMode();
            Assert.assertNotNull(sm_o);            
            MonitorMode sm_c = monitor.queryCurrentMode();
            System.err.println("[0] orig   : "+sm_o);
            System.err.println("[0] current: "+sm_c);
            Assert.assertNotNull(sm_c);
            Assert.assertEquals(sm_o, sm_c);
        }

        screen.removeReference();

        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,screen.getDisplay().isNativeValid());
    }

    static int atoi(String a) {
        try {
            return Integer.parseInt(a);
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
    
    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-screen")) {
                i++;
                screenIdx = atoi(args[i]);
            }
        }
        String tstname = TestScreenMode00NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
