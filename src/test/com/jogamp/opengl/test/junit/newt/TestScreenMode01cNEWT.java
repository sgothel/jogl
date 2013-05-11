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
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

import java.util.ArrayList;
import java.util.List;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.RectangleImmutable;

/**
 * Fullscreen on separate monitors ..
 */
public class TestScreenMode01cNEWT extends UITestCase {
    static GLProfile glp;
    static int width, height;
    
    static long waitTimeShort = 2000;
    static long duration = 4000;

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
    public void testScreenFullscreenSingleQ1() throws InterruptedException {
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        screen.addReference(); // trigger creation
        try {
            RectangleImmutable monitorVp = screen.getMonitorDevices().get(0).getViewport();
            testScreenFullscreenImpl(screen, monitorVp.getX(), monitorVp.getY(), false, null);
        } finally {
            screen.removeReference();
        }
    }
    
    @Test
    public void testScreenFullscreenSingleQ2() throws InterruptedException {
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
            testScreenFullscreenImpl(screen, monitorVp.getX(), monitorVp.getY(), false, null);
        } finally {
            screen.removeReference();
        }
    }
        
    @Test
    public void testScreenFullscreenSpanQ1Q2() throws InterruptedException {
        final Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);
        screen.addReference(); // trigger creation        
        try {
            final int crtCount = screen.getMonitorDevices().size();
            if( 2 >= crtCount ) {
                System.err.println("Test Disabled (2): Spanning monitor count "+2+" >= screen monitor count: "+screen);
                return;            
            }            
            final ArrayList<MonitorDevice> monitors = new ArrayList<MonitorDevice>();
            monitors.add(screen.getMonitorDevices().get(0)); // Q1
            monitors.add(screen.getMonitorDevices().get(1)); // Q2
            RectangleImmutable monitorVp = screen.getMonitorDevices().get(0).getViewport();
            testScreenFullscreenImpl(screen, monitorVp.getX()+50, monitorVp.getY()+50, true, monitors);
        } finally {
            screen.removeReference();
        }
    }
    
    @Test
    public void testScreenFullscreenSpanALL() throws InterruptedException {
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
            RectangleImmutable monitorVp = screen.getMonitorDevices().get(1).getViewport();
            testScreenFullscreenImpl(screen, monitorVp.getX()-50, monitorVp.getY()+50, true, null);
        } finally {
            screen.removeReference();
        }
    }
    
    void testScreenFullscreenImpl(final Screen screen, int xpos, int ypos, boolean spanAcrossMonitors, List<MonitorDevice> monitors) throws InterruptedException {
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

        MonitorDevice monitor = window0.getMainMonitor();
        System.err.println("Test.0: Window monitor: "+monitor);
        if( !spanAcrossMonitors ) {
            window0.setFullscreen(true);
        } else {
            window0.setFullscreen(monitors);
        }

        monitor = window0.getMainMonitor();
        System.err.println("Test.1: Window bounds: "+window0.getX()+"/"+window0.getY()+" "+window0.getWidth()+"x"+window0.getHeight()+" within "+screen.getViewport());
        System.err.println("Test.1: Window monitor: "+monitor.getViewport());
        Rectangle window0Rect = new Rectangle(window0.getX(), window0.getY(), window0.getWidth(), window0.getHeight());
        if( !spanAcrossMonitors ) {
            Assert.assertEquals(monitor.getViewport(),  window0Rect);
        } else {
            List<MonitorDevice> monitorsUsed = monitors;
            if( null == monitorsUsed ) {
                monitorsUsed = window0.getScreen().getMonitorDevices();
            }
            Rectangle monitorsUsedViewport = MonitorDevice.unionOfViewports(new Rectangle(), monitorsUsed);
            Assert.assertEquals(monitorsUsedViewport,  window0Rect);
        }
        
        Thread.sleep(duration);

        window0.setFullscreen(false);
        
        window0Rect = new Rectangle(window0.getX(), window0.getY(), window0.getWidth(), window0.getHeight());
        monitor = window0.getMainMonitor();
        System.err.println("Test.2: Window bounds: "+window0.getX()+"/"+window0.getY()+" "+window0.getWidth()+"x"+window0.getHeight()+" within "+screen.getViewport());
        System.err.println("Test.2: Window monitor: "+monitor.getViewport());        
                
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
        String tstname = TestScreenMode01cNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
