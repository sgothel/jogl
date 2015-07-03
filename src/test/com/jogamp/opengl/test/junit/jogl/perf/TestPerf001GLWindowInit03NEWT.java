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
package com.jogamp.opengl.test.junit.jogl.perf;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Multiple GLJPanels in a JFrame
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPerf001GLWindowInit03NEWT extends UITestCase {
    final long INIT_TIMEOUT = 10L*1000L; // 10s

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton();
    }

    public void test(final GLCapabilitiesImmutable caps, final boolean useGears, final int width, final int height, final int frameCount, final boolean reuseDevice) {
        final int cols = (int)Math.round(Math.sqrt(frameCount));
        final int rows = frameCount / cols;
        final int eWidth = width/cols;
        final int eHeight = height/rows;

        final GLWindow[] frame = new GLWindow[frameCount];
        final long[] t = new long[10];
        if( wait ) {
            UITestCase.waitForKey("Pre-Init");
        }
        System.err.println("INIT START");
        initCount.set(0);

        t[0] = Platform.currentTimeMillis();
        int x = 32, y = 32;
        for(int i=0; i<frameCount; i++) {
            final Screen screen = NewtFactory.createScreen(NewtFactory.createDisplay(null, reuseDevice), 0);
            frame[i] = GLWindow.create(screen, caps);
            frame[i].setTitle("frame_"+i+"/"+frameCount);
            frame[i].setPosition(x, y);
            x+=eWidth+32;
            if(x>=width) {
                x=32;
                y+=eHeight+32;
            }
            frame[i].setSize(eWidth, eHeight);
            if( useGears ) {
                frame[i].addGLEventListener(new GearsES2());
            }
            frame[i].addGLEventListener(new GLEventListener() {
                @Override
                public void init(final GLAutoDrawable drawable) {
                    initCount.incrementAndGet();
                }
                @Override
                public void dispose(final GLAutoDrawable drawable) {}
                @Override
                public void display(final GLAutoDrawable drawable) {}
                @Override
                public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
            });
        }
        t[1] = Platform.currentTimeMillis();
        for(int i=0; i<frameCount; i++) {
            frame[i].setVisible(false /*wait*/, true /*visible*/);
        }
        t[2] = Platform.currentTimeMillis();

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while( frameCount > initCount.get() && INIT_TIMEOUT > t1 - t0 ) {
            try {
                Thread.sleep(100);
                System.err.println("Sleep initialized: "+initCount+"/"+frameCount);
            } catch (final InterruptedException e1) {
                e1.printStackTrace();
            }
            t1 = System.currentTimeMillis();
        }
        t[3] = Platform.currentTimeMillis();
        final double panelCountF = initCount.get();
        System.err.printf("P: %d GLWindow:%n\tctor\t%6d/t %6.2f/1%n\tvisible\t%6d/t %6.2f/1%n\tsum-i\t%6d/t %6.2f/1%n",
                initCount.get(),
                t[1]-t[0], (t[1]-t[0])/panelCountF,
                t[3]-t[1], (t[3]-t[1])/panelCountF,
                t[3]-t[0], (t[3]-t[0])/panelCountF);

        System.err.println("INIT END: "+initCount+"/"+frameCount);
        if( wait ) {
            UITestCase.waitForKey("Post-Init");
        }
        try {
            Thread.sleep(duration);
        } catch (final InterruptedException e1) {
            e1.printStackTrace();
        }
        t[4] = Platform.currentTimeMillis();
        for(int i=0; i<frameCount; i++) {
            frame[i].destroy();
        }

        final long ti_net = (t[4]-t[0])-duration;
        System.err.printf("T: duration %d %d%n\ttotal-d\t%6d/t %6.2f/1%n\ttotal-i\t%6d/t %6.2f/1%n",
                duration, t[4]-t[3],
                t[4]-t[0], (t[4]-t[0])/panelCountF,
                ti_net, ti_net/panelCountF);
        System.err.println("Total: "+(t[4]-t[0]));
    }

    static GLCapabilitiesImmutable caps = null;

    @Test
    public void test01NopGLWindowNoReuse() throws InterruptedException, InvocationTargetException {
        if(!mainRun) {
            System.err.println("Disabled for auto unit test until further analysis - Windows/ATI driver crash");
            return;
        }
        test(new GLCapabilities(null), false /*useGears*/, width, height , frameCount, false /* reuseDevice */);
    }
    @Test
    public void test02NopGLWindowReuse() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, width, height , frameCount, true /* reuseDevice */);
    }

    static long duration = 0; // ms
    static boolean wait = false, mainRun = false;
    static int width = 800, height = 600, frameCount = 25;

    AtomicInteger initCount = new AtomicInteger(0);

    public static void main(final String[] args) {
        mainRun = true;
        boolean useGears = false, manual=false;
        boolean waitMain = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-width")) {
                width = MiscUtils.atoi(args[++i], width);
            } else if(args[i].equals("-height")) {
                height = MiscUtils.atoi(args[++i], height);
            } else if(args[i].equals("-count")) {
                frameCount = MiscUtils.atoi(args[++i], frameCount);
            } else if(args[i].equals("-gears")) {
                useGears = true;
            } else if(args[i].equals("-wait")) {
                wait = true;
                manual = true;
            } else if(args[i].equals("-waitMain")) {
                waitMain = true;
                manual = true;
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        if( waitMain ) {
            UITestCase.waitForKey("Main-Start");
        }
        if( manual ) {
            GLProfile.initSingleton();
            final TestPerf001GLWindowInit03NEWT demo = new TestPerf001GLWindowInit03NEWT();
            demo.test(null, useGears, width, height, frameCount, false /* reuseDevice */);
        } else {
            org.junit.runner.JUnitCore.main(TestPerf001GLWindowInit03NEWT.class.getName());
        }
    }

}
