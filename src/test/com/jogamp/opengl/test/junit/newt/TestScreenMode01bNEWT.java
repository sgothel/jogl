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
 
package com.jogamp.opengl.test.junit.newt;

import java.io.IOException;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.MonitorModeUtil;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

import java.util.List;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.RectangleImmutable;

/**
 * Mode change on separate monitors ..
 */
public class TestScreenMode01bNEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;
    
    static long waitTimeShort = 2000;
    static long duration = 6000;

    @BeforeClass
    public static void initClass() {
        width  = 200;
        height = 200;
        glp = GLProfile.getDefault();
    }

    @AfterClass
    public static void releaseClass() throws InterruptedException {
        Thread.sleep(waitTimeShort);
    }
    
    static GLWindow createWindow(Screen screen, GLCapabilities caps, String name, int x, int y, int width, int height) throws InterruptedException {
        Assert.assertNotNull(caps);

        GLWindow window = GLWindow.create(screen, caps);
        // Window window = NewtFactory.createWindow(screen, caps);
        window.setTitle(name);
        window.setPosition(x, y);
        window.setSize(width, height);
        window.addGLEventListener(new GearsES2());
        Assert.assertNotNull(window);
        final long t0 = System.currentTimeMillis();
        window.setVisible(true);
        System.err.println("Time for visible/pos: "+(System.currentTimeMillis()-t0)+" ms");
        return window;
    }

    static void destroyWindow(Window window) {
        if(null!=window) {
            window.destroy();
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
            RectangleImmutable monitorVp = screen.getMonitorDevices().get(0).getViewport();
            testScreenModeChangeImpl(screen, monitorVp.getX(), monitorVp.getY());
        } finally {
            screen.removeReference();
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
            RectangleImmutable monitorVp = screen.getMonitorDevices().get(1).getViewport();
            testScreenModeChangeImpl(screen, monitorVp.getX(), monitorVp.getY());
        } finally {
            screen.removeReference();
        }
    }
        
    void testScreenModeChangeImpl(final Screen screen, int xpos, int ypos) throws InterruptedException {
        Thread.sleep(waitTimeShort);

        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        final Display display = screen.getDisplay();
        System.err.println("Test.0: Window screen: "+screen);
        
        System.err.println("Test.0: Window bounds (pre): "+xpos+"/"+ypos+" "+width+"x"+height+" within "+screen.getViewport());
        
        GLWindow window0 = createWindow(screen, caps, "win0", xpos, ypos, width, height);
        Assert.assertNotNull(window0);        
        System.err.println("Test.0: Window bounds: "+window0.getX()+"/"+window0.getY()+" "+window0.getWidth()+"x"+window0.getHeight()+" within "+screen.getViewport());

        final Animator anim = new Animator(window0);
        anim.start();
        
        List<MonitorMode> allMonitorModes = screen.getMonitorModes();
        Assert.assertTrue(allMonitorModes.size()>0);
        if(allMonitorModes.size()==1) {
            // no support ..
            System.err.println("Your platform has no MonitorMode change support (all), sorry");
            destroyWindow(window0);
            return;
        }

        MonitorDevice monitor = window0.getMainMonitor();
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
        MonitorMode mmOrig = monitor.getOriginalMode();
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

        MonitorMode sm = (MonitorMode) monitorModes.get(0);
        System.err.println("[1] set current: "+sm);
        Assert.assertTrue(monitor.setCurrentMode(sm));
        mmCurrent = monitor.getCurrentMode();
        System.err.println("[1] current: "+mmCurrent);
        Assert.assertTrue(monitor.isModeChangedByUs());
        Assert.assertEquals(sm, monitor.getCurrentMode());
        Assert.assertNotSame(mmOrig, monitor.getCurrentMode());
        Assert.assertEquals(sm, monitor.queryCurrentMode());

        System.err.println("Test.1: Window screen: "+screen);
        System.err.println("Test.1: Window bounds: "+window0.getX()+"/"+window0.getY()+" "+window0.getWidth()+"x"+window0.getHeight()+" within "+screen.getViewport());
        System.err.println("Test.1: Window monitor: "+window0.getMainMonitor());
        
        Thread.sleep(duration);

        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window0.isNativeValid());
        Assert.assertEquals(true,window0.isVisible());

        Assert.assertTrue(monitor.setCurrentMode(mmOrig));
        Assert.assertFalse(monitor.isModeChangedByUs());
        Assert.assertEquals(mmOrig, monitor.getCurrentMode());
        Assert.assertNotSame(sm, monitor.getCurrentMode());
        Assert.assertEquals(mmOrig, monitor.queryCurrentMode());
        
        System.err.println("Test.2: Window screen: "+screen);
        System.err.println("Test.2: Window bounds: "+window0.getX()+"/"+window0.getY()+" "+window0.getWidth()+"x"+window0.getHeight()+" within "+screen.getViewport());
        System.err.println("Test.2: Window monitor: "+window0.getMainMonitor());
        
        Thread.sleep(duration);
        anim.stop();
        destroyWindow(window0);
        Assert.assertEquals(false,window0.isVisible());
        Assert.assertEquals(false,window0.isNativeValid());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        String tstname = TestScreenMode01bNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
