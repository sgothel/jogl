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
 

package com.jogamp.opengl.test.junit.newt.parenting;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.opengl.*;
import javax.media.nativewindow.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

import java.io.IOException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
// import com.jogamp.opengl.test.junit.jogl.demos.es1.RedSquareES1;
// import com.jogamp.opengl.test.junit.jogl.demos.es1.GearsES1;

public class TestParenting02NEWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 500;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
    }

    static Window createWindow(Screen screen, Capabilities caps) {
        Assert.assertNotNull(caps);
        Window window = NewtFactory.createWindow(screen, caps) ;
        Assert.assertNotNull(window);
        return window;
    }

    static Window createWindow(NativeWindow parent, Capabilities caps) {
        Assert.assertNotNull(caps);
        Window window = NewtFactory.createWindow(parent, caps);
        window.setUndecorated(true);
        Assert.assertNotNull(window);
        return window;
    }

    static void destroyWindow(Display display, Screen screen, Window window, GLWindow glWindow) {
        if(null!=glWindow) {
            glWindow.destroy();
        }
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
    public void testWindowParenting01NewtOnNewtParentChildDraw() throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(null);
        Assert.assertNotNull(caps);
        Display display = NewtFactory.createDisplay(null); // local display
        Assert.assertNotNull(display);
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        Assert.assertNotNull(screen);

        int x = 1;
        int y = 1;

        NEWTEventFiFo eventFifo = new NEWTEventFiFo();

        Window window1 = createWindow(screen, caps);
        Assert.assertNotNull(window1);
        GLWindow glWindow1 = GLWindow.create(window1);
        Assert.assertNotNull(glWindow1);
        glWindow1.setSize(width, height);
        Assert.assertEquals(width,glWindow1.getWidth());
        Assert.assertEquals(height,glWindow1.getHeight());
        glWindow1.setTitle("testWindowParenting01NewtOnNewtParentChildDraw - PARENT");
        glWindow1.setPosition(x,y);
        glWindow1.addKeyListener(new TraceKeyAdapter(new KeyAction(eventFifo)));
        glWindow1.addWindowListener(new TraceWindowAdapter());

        GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, window1, glWindow1, false);
        // glWindow1.addGLEventListener(demo1);

        glWindow1.setVisible(true);
        CapabilitiesImmutable capsChosen = glWindow1.getGraphicsConfiguration().getChosenCapabilities();
        Assert.assertNotNull(capsChosen);
        Assert.assertTrue(capsChosen.isOnscreen()==true);

        Window window2 = createWindow(window1, caps);
        Assert.assertNotNull(window2);
        GLWindow glWindow2 = GLWindow.create(window2);
        Assert.assertNotNull(glWindow2);
        glWindow2.setSize(width/2, height/2);
        //Assert.assertEquals(width/2,glWindow2.getWidth());
        //Assert.assertEquals(height/2,glWindow2.getHeight());
        glWindow2.setTitle("testWindowParenting01NewtOnNewtParentChildDraw - CHILD");
        glWindow2.setPosition(glWindow1.getWidth()/2, glWindow1.getHeight()/2);
        glWindow2.addKeyListener(new TraceKeyAdapter(new KeyAction(eventFifo)));
        glWindow2.addWindowListener(new TraceWindowAdapter(new WindowAction(eventFifo)));
        // glWindow2.addMouseListener(new TraceMouseAdapter());

        GLEventListener demo2 = new GearsES2();
        setDemoFields(demo2, window2, glWindow2, false);
        // glWindow2.addGLEventListener(demo2);

        glWindow2.setVisible(true);
        capsChosen = glWindow2.getGraphicsConfiguration().getChosenCapabilities();
        Assert.assertNotNull(capsChosen);
        Assert.assertTrue(capsChosen.isOnscreen()==true);

        glWindow1.addGLEventListener(demo1);
        glWindow2.addGLEventListener(demo2);

        boolean shouldQuit = false;
        long duration = durationPerTest;
        long step = 20;
        NEWTEvent event;

        while (duration>0 && !shouldQuit) {
            glWindow1.display();
            glWindow2.display();
            duration -= step;
            x += 1;
            y += 1;
            // glWindow1.setPosition(x,y);
            glWindow2.setPosition(glWindow1.getWidth()/2,glWindow1.getHeight()/2-y);
            Thread.sleep(step);

            while( null != ( event = (NEWTEvent) eventFifo.get() ) ) {
                Window source = (Window) event.getSource();
                if(WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY == event.getEventType()) {
                    shouldQuit = true;
                } else if(event instanceof KeyEvent) {
                    KeyEvent keyEvent = (KeyEvent) event;
                    switch(keyEvent.getKeyChar()) {
                        case 'q':
                            shouldQuit = true;
                            break;
                        case 'f':
                            source.setFullscreen(!source.isFullscreen());
                            break;
                    }
                } 
            }
        }
        destroyWindow(null, null, window2, glWindow2);
        destroyWindow(display, screen, window1, glWindow1);
    }

    public static void setDemoFields(GLEventListener demo, Window window, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(window);
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        String tstname = TestParenting02NEWT.class.getName();
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
