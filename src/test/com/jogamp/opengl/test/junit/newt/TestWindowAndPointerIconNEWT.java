/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
import java.util.Properties;

import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GLCapabilities;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.IOUtil;
import com.jogamp.junit.util.SingletonJunitCase;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestWindowAndPointerIconNEWT extends SingletonJunitCase {

    static long duration = 1000; // ms

    // As early as possible
    static {
        setPointerIcons();
    }

    static void setPointerIcons() {
        final Properties sysp = System.getProperties();
        sysp.put("jnlp.newt.window.icons", "red-16x16.png red-32x32.png");
    }

    @AfterClass
    public static void unsetPointerIcons() {
        final Properties sysp = System.getProperties();
        sysp.remove("jnlp.newt.window.icons");
    }

    @Test
    public void test() throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(new GLCapabilities(null));
        Assert.assertNotNull(glWindow);

        glWindow.setSize(800, 600);

        final GearsES2 demo = new GearsES2(1);
        glWindow.addGLEventListener(demo);

        final QuitAdapter quitAdapter = new QuitAdapter();
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        final PointerIcon pointerIcon;
        {
            final Display disp = glWindow.getScreen().getDisplay();
            disp.createNative();
            final int idx = 0;
            {
                PointerIcon _pointerIcon = null;
                final IOUtil.ClassResources res = new IOUtil.ClassResources(new String[] { "arrow-red-alpha-64x64.png" }, glWindow.getClass().getClassLoader(), null);
                try {
                    _pointerIcon = disp.createPointerIcon(res, 0, 0);
                    System.err.printf("Create PointerIcon #%02d: %s%n", idx, _pointerIcon.toString());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                pointerIcon = _pointerIcon;
            }
        }
        glWindow.setPointerIcon(pointerIcon);
        System.err.println("Set PointerIcon: "+glWindow.getPointerIcon());

        final Animator animator = new Animator();
        animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
        animator.add(glWindow);
        animator.start();

        glWindow.setVisible(true);
        glWindow.warpPointer(3*glWindow.getSurfaceWidth()/4, 3*glWindow.getSurfaceHeight()/4);

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1-t0<duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        animator.stop();

        glWindow.destroy();
        if( NativeWindowFactory.isAWTAvailable() ) {
            Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow, false));
        }
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestWindowAndPointerIconNEWT.class.getName());
    }
}
