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
 
package com.jogamp.test.junit.newt;

import java.io.IOException;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.impl.ScreenMode;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.test.junit.jogl.demos.gl2.gears.Gears;
import com.jogamp.test.junit.util.UITestCase;

public class TestScreenMode01NEWT extends UITestCase {
	static GLProfile glp;
    static int width, height;
    
    static int waitTimeShort = 4; //1 sec
    static int waitTimeLong = 6; //6 sec
    
    

    @BeforeClass
    public static void initClass() {
        NativeWindowFactory.initSingleton(true);
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

    static GLWindow createWindow(Screen screen, GLCapabilities caps, int width, int height, boolean onscreen, boolean undecorated) {
        Assert.assertNotNull(caps);
        caps.setOnscreen(onscreen);

        boolean destroyWhenUnused = screen.getDestroyWhenUnused();
        GLWindow window = GLWindow.create(screen, caps);
        window.addGLEventListener(new Gears());
        Assert.assertNotNull(window);
        Assert.assertEquals(destroyWhenUnused, window.getScreen().getDestroyWhenUnused());
        window.setVisible(true);
        return window;
    }

    static void destroyWindow(Display display, Screen screen, Window window) {
        if(null!=window) {
            window.destroy();
        }
        if(null!=screen) {
            screen.destroy();
        }
        if(null!=display) {
            display.destroy();
        }
    }
    
    @Test
    public void testFullscreenChange01() throws InterruptedException {
    	GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        GLWindow window = createWindow(screen, caps, width, height, true /* onscreen */, false /* undecorated */);
        
        window.setFullscreen(true);
        Assert.assertEquals(true, window.isFullscreen());
        
        for(int state=0; state<waitTimeShort; state++) {
            Thread.sleep(100);
        }
        window.setFullscreen(false);
        Assert.assertEquals(false, window.isFullscreen());
        
        destroyWindow(display, screen, window);    	
    }

    @Test
    public void testScreenModeChange01() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        GLWindow window = createWindow(screen, caps, width, height, true /* onscreen */, false /* undecorated */);

        ScreenMode[] screenModes = screen.getScreenModes();
        Assert.assertNotNull(screenModes);
        
        int originalScreenMode = screen.getDesktopScreenModeIndex();
        short originalScreenRate = screen.getCurrentScreenRate();
        
        Assert.assertNotSame(-1, originalScreenMode);
        Assert.assertNotSame(-1, originalScreenRate);
        
        int modeIndex = 1;
        
        ScreenMode screenMode = screenModes[modeIndex];
        Assert.assertNotNull(screenMode);
        
        short modeRate = screenMode.getRates()[0];
        screen.setScreenMode(modeIndex, modeRate);
        
        Assert.assertEquals(modeIndex, screen.getDesktopScreenModeIndex());
        Assert.assertEquals(modeRate, screen.getCurrentScreenRate());
        
        for(int state=0; state<waitTimeLong; state++) {
            Thread.sleep(100);
        }
        
        screen.setScreenMode(-1, (short)-1);
        Assert.assertEquals(originalScreenMode, screen.getDesktopScreenModeIndex());
        Assert.assertEquals(originalScreenRate, screen.getCurrentScreenRate());
        
        destroyWindow(display, screen, window);
    }

    @Test
    public void testScreenModeChangeWithFS01() throws InterruptedException {
    	GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        GLWindow window = createWindow(screen, caps, width, height, true /* onscreen */, false /* undecorated */);
        ScreenMode[] screenModes = screen.getScreenModes();
        Assert.assertNotNull(screenModes);
        
        int originalScreenMode = screen.getDesktopScreenModeIndex();
        short originalScreenRate = screen.getCurrentScreenRate();
        
        Assert.assertNotSame(-1, originalScreenMode);
        Assert.assertNotSame(-1, originalScreenRate);
        
        int modeIndex = 1;
        
        ScreenMode screenMode = screenModes[modeIndex];
        Assert.assertNotNull(screenMode);
        
        short modeRate = screenMode.getRates()[0];
        screen.setScreenMode(modeIndex, modeRate);
        
        Assert.assertEquals(modeIndex, screen.getDesktopScreenModeIndex());
        Assert.assertEquals(modeRate, screen.getCurrentScreenRate());
        
        window.setFullscreen(true);
        Assert.assertEquals(true, window.isFullscreen());
        
        for(int state=0; state<waitTimeLong; state++) {
            Thread.sleep(100);
        }
        
        window.setFullscreen(false);
        Assert.assertEquals(false, window.isFullscreen());
        
        screen.setScreenMode(-1, (short)-1);
        Assert.assertEquals(originalScreenMode, screen.getDesktopScreenModeIndex());
        Assert.assertEquals(originalScreenRate, screen.getCurrentScreenRate());
        
        destroyWindow(display, screen, window);    
    }
    
    @Test
    public void testScreenModeChangeWithFS02() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        GLWindow window = createWindow(screen, caps, width, height, true /* onscreen */, false /* undecorated */);

        ScreenMode[] screenModes = screen.getScreenModes();
        Assert.assertNotNull(screenModes);
        
        int originalScreenMode = screen.getDesktopScreenModeIndex();
        short originalScreenRate = screen.getCurrentScreenRate();
        
        Assert.assertNotSame(-1, originalScreenMode);
        Assert.assertNotSame(-1, originalScreenRate);
        
        int modeIndex = 1;
        
        ScreenMode screenMode = screenModes[modeIndex];
        Assert.assertNotNull(screenMode);
        
        short modeRate = screenMode.getRates()[0];
        screen.setScreenMode(modeIndex, modeRate);
        
        Assert.assertEquals(modeIndex, screen.getDesktopScreenModeIndex());
        Assert.assertEquals(modeRate, screen.getCurrentScreenRate());
        
        window.setFullscreen(true);
        Assert.assertEquals(true, window.isFullscreen());
        
        for(int state=0; state<waitTimeLong; state++) {
            Thread.sleep(100);
        }
        
        screen.setScreenMode(-1, (short)-1);
        Assert.assertEquals(originalScreenMode, screen.getDesktopScreenModeIndex());
        Assert.assertEquals(originalScreenRate, screen.getCurrentScreenRate());
        
        window.setFullscreen(false);
        Assert.assertEquals(false, window.isFullscreen());
        
        destroyWindow(display, screen, window);    
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestScreenMode01NEWT.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }
}
