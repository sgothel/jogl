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
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.util.List;
import javax.media.nativewindow.util.Dimension;

/**
 * Documents remedy B) for NV RANDR/GL bug
 * 
 * @see TestScreenMode01NEWT#cleanupGL()
 */
public class TestScreenMode01aNEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;
    
    static int waitTimeShort = 2000;
    static int waitTimeLong = 2000;

    @BeforeClass
    public static void initClass() {
        width  = 100;
        height = 100;
        glp = GLProfile.getDefault();
    }

    @AfterClass
    public static void releaseClass() throws InterruptedException {
        Thread.sleep(waitTimeShort);
    }
    
    static Window createWindow(Screen screen, GLCapabilities caps, String name, int x, int y, int width, int height) {
        Assert.assertNotNull(caps);

        GLWindow window = GLWindow.create(screen, caps);
        // Window window = NewtFactory.createWindow(screen, caps);
        window.setTitle(name);
        window.setPosition(x, y);
        window.setSize(width, height);
        window.addGLEventListener(new GearsES2());
        Assert.assertNotNull(window);
        window.setVisible(true);
        return window;
    }

    static void destroyWindow(Window window) {
        if(null!=window) {
            window.destroy();
        }
    }
    
    @Test
    public void testScreenModeChange01() throws InterruptedException {
        Thread.sleep(waitTimeShort);

        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        Window window0 = createWindow(screen, caps, "win0", 0, 0, width, height);
        Assert.assertNotNull(window0);        

        List<MonitorMode> allMonitorModes = screen.getMonitorModes();
        Assert.assertTrue(allMonitorModes.size()>0);
        if(allMonitorModes.size()==1) {
            // no support ..
            System.err.println("Your platform has no MonitorMode change support (all), sorry");
            destroyWindow(window0);
            return;
        }

        MonitorDevice monitor = window0.getMainMonitor();
        
        List<MonitorMode> monitorModes = monitor.getSupportedModes();
        Assert.assertTrue(monitorModes.size()>0);
        if(monitorModes.size()==1) {
            // no support ..
            System.err.println("Your platform has no MonitorMode change support (monitor), sorry");
            destroyWindow(window0);
            return;
        }
        Assert.assertTrue(allMonitorModes.containsAll(monitorModes));
                
        MonitorMode mmCurrent = monitor.queryCurrentMode();
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

        MonitorMode mm = monitorModes.get(0);
        System.err.println("[0] set current: "+mm);
        Assert.assertTrue(monitor.setCurrentMode(mm));
        Assert.assertTrue(monitor.isModeChangedByUs());
        Assert.assertEquals(mm, monitor.getCurrentMode());
        Assert.assertNotSame(mmOrig, monitor.getCurrentMode());
        Assert.assertEquals(mm, monitor.queryCurrentMode());

        Thread.sleep(waitTimeShort);

        // check manual reset ..

        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window0.isNativeValid());
        Assert.assertEquals(true,window0.isVisible());

        screen.addReference(); // keep it alive !
        Assert.assertTrue(monitor.setCurrentMode(mmOrig));
        Assert.assertFalse(monitor.isModeChangedByUs());
        Assert.assertEquals(mmOrig, monitor.getCurrentMode());
        Assert.assertNotSame(mm, monitor.getCurrentMode());
        Assert.assertEquals(mmOrig, monitor.queryCurrentMode());
        
        destroyWindow(window0);
        Assert.assertEquals(false,window0.isVisible());
        Assert.assertEquals(false,window0.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid()); // alive !
        Assert.assertEquals(true,display.isNativeValid());
                
        Thread.sleep(waitTimeShort);

        Window window1 = createWindow(screen, caps, "win1", 
                                      width+window0.getInsets().getTotalWidth(), 0, 
                                      width, height);
        Assert.assertNotNull(window1);
        Assert.assertEquals(true,window1.isNativeValid());
        Assert.assertEquals(true,window1.isVisible());
        
        Thread.sleep(waitTimeShort);
        
        destroyWindow(window1);
        Assert.assertEquals(false,window1.isNativeValid());
        Assert.assertEquals(false,window1.isVisible());
        
        screen.removeReference();
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,display.isNativeValid());                
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestScreenMode01aNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
