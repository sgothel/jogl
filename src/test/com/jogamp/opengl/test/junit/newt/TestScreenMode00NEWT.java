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

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.util.MonitorMode;
import com.jogamp.newt.util.ScreenModeUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import java.util.Iterator;
import java.util.List;
import javax.media.nativewindow.Capabilities;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionReadOnly;
import javax.media.nativewindow.util.SurfaceSize;

public class TestScreenMode00NEWT extends UITestCase {
    static int width, height;
    
    static int waitTimeShort = 4; //1 sec
    static int waitTimeLong = 6; //6 sec
    
    

    @BeforeClass
    public static void initClass() {
        NativeWindowFactory.initSingleton(true);
        width  = 640;
        height = 480;
    }

    @Test
    public void testScreenModeInfo00() throws InterruptedException {
        DimensionReadOnly res = new Dimension(640, 480);
        SurfaceSize surfsz = new SurfaceSize(res, 32);
        DimensionReadOnly mm = new Dimension(500, 400);
        MonitorMode mon = new MonitorMode(surfsz, mm, 60);
        ScreenMode sm_out = new ScreenMode(mon, 90);
        System.err.println("00 out: "+sm_out);

        int[] props = ScreenModeUtil.streamOut(sm_out);
        ScreenMode sm_in = ScreenModeUtil.streamIn(props, 0);
        System.err.println("00 in : "+sm_in);

        Assert.assertEquals(sm_in.getMonitorMode().getSurfaceSize().getResolution(),
                            sm_out.getMonitorMode().getSurfaceSize().getResolution());

        Assert.assertEquals(sm_in.getMonitorMode().getSurfaceSize(),
                            sm_out.getMonitorMode().getSurfaceSize());

        Assert.assertEquals(sm_in.getMonitorMode().getScreenSizeMM(),
                            sm_out.getMonitorMode().getScreenSizeMM());

        Assert.assertEquals(sm_in.getMonitorMode(), sm_out.getMonitorMode());

        Assert.assertEquals(sm_in, sm_out);
        
        Assert.assertEquals(sm_out.hashCode(), sm_in.hashCode());
    }

    @Test
    public void testScreenModeInfo01() throws InterruptedException {
        Capabilities caps = new Capabilities();
        Window window = NewtFactory.createWindow(caps);
        window.setSize(width, height);
        window.setVisible(true);

        Screen screen = window.getScreen();

        List screenModes = screen.getScreenModes();
        if(null != screenModes) {
            Assert.assertTrue(screenModes.size()>0);
            int i=0;
            for(Iterator iter=screenModes.iterator(); iter.hasNext(); i++) {
                System.err.println(i+": "+iter.next());
            }
            ScreenMode sm_o = screen.getOriginalScreenMode();
            Assert.assertNotNull(sm_o);            
            ScreenMode sm_c = screen.getOriginalScreenMode();
            Assert.assertNotNull(sm_c);
            System.err.println("orig: "+sm_o);
            System.err.println("curr: "+sm_c);
        } else {
            // no support ..
            System.err.println("Your platform has no ScreenMode change support, sorry");
        }

        window.destroy();

        Assert.assertEquals(false,window.isVisible());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,screen.getDisplay().isNativeValid());
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestScreenMode00NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
