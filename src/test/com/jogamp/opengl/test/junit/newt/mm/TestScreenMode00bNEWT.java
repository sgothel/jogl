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

import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

import java.util.Iterator;
import java.util.List;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;

/**
 * Queries the current MonitorMode 50 times,
 * stressing a possible race condition.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestScreenMode00bNEWT extends UITestCase {
    static int width, height;

    static int waitTimeShort = 4; //1 sec
    static int waitTimeLong = 6; //6 sec

    @BeforeClass
    public static void initClass() {
        setResetXRandRIfX11AfterClass();
        NativeWindowFactory.initSingleton();
        width  = 640;
        height = 480;
    }

    @Test
    public void testScreenModeInfo01() throws InterruptedException {
        final Display display = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(display, 0);
        // screen.addReference();

        // put some load on the screen/display context ..
        final GLCapabilitiesImmutable caps = new GLCapabilities(null);
        final GLWindow window = GLWindow.create(screen, caps);
        window.addGLEventListener(new GearsES2());
        window.setSize(256, 256);
        window.setVisible(true);
        final Animator anim = new Animator(window);
        anim.start();

        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,display.isNativeValid());

        final List<MonitorMode> screenModes = screen.getMonitorModes();
        Assert.assertTrue(screenModes.size()>0);
        int i=0;
        for(final Iterator<MonitorMode> iter=screenModes.iterator(); iter.hasNext(); i++) {
            System.err.println(i+": "+iter.next());
        }
        final MonitorDevice monitor = window.getMainMonitor();
        final MonitorMode mm_o = monitor.getOriginalMode();

        Assert.assertNotNull(mm_o);
        MonitorMode mm_c = monitor.queryCurrentMode();
        Assert.assertNotNull(mm_c);
        System.err.println("orig: "+mm_o);
        System.err.println("curr: "+mm_c);

        for(i=0; i<50; i++) {
            mm_c = monitor.queryCurrentMode();
            Assert.assertNotNull(mm_c);
            System.err.print("."+i);
        }
        System.err.println("!");

        // screen.removeReference();
        anim.stop();
        window.destroy();

        Assert.assertEquals(false,window.isVisible());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertTrue(AWTRobotUtil.waitForRealized(screen, false));
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,display.isNativeValid());
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestScreenMode00bNEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
