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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAddRemove03GLWindowNEWT extends UITestCase {
    static long durationPerTest = 50;
    static int addRemoveCount = 15;
    static int pauseEach = 0;
    static int pauseDuration = 500;
    static GLProfile glp;
    static int width, height;
    static boolean waitForKey = false;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.get(GLProfile.GL2ES2);
            Assert.assertNotNull(glp);
            width  = 640;
            height = 480;
        } else {
            setTestSupported(false);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(final GLCapabilities caps, final int addRemoveOpCount)
            throws InterruptedException, InvocationTargetException
    {

        for(int i=0; i<addRemoveOpCount; i++) {
            System.err.println("Loop # "+i+" / "+addRemoveCount);
            final GLWindow glw = GLWindow.create(caps);
            Assert.assertNotNull(glw);
            glw.setTitle("GLWindow #"+i);
            glw.setSize(width, height);
            final GearsES2 gears = new GearsES2(1);
            gears.setVerbose(false);
            glw.addGLEventListener(gears);

            glw.setVisible(true);

            final long t0 = System.currentTimeMillis();
            do {
                glw.display();
                Thread.sleep(10);
            } while ( ( System.currentTimeMillis() - t0 ) < durationPerTest ) ;

            System.err.println("GLWindow: "+glw.getChosenGLCapabilities());

            glw.destroy();

            if( 0 < pauseEach && 0 == i % pauseEach ) {
                System.err.println("******* P A U S E ********");
                Thread.sleep(pauseDuration);
            }
        }
    }

    @Test
    public void test01Onscreen()
            throws InterruptedException, InvocationTargetException
    {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, addRemoveCount);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    durationPerTest = Long.parseLong(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-loops")) {
                i++;
                addRemoveCount = MiscUtils.atoi(args[i], addRemoveCount);
            } else if(args[i].equals("-pauseEach")) {
                i++;
                pauseEach = MiscUtils.atoi(args[i], pauseEach);
            } else if(args[i].equals("-pauseDuration")) {
                i++;
                pauseDuration = MiscUtils.atoi(args[i], pauseDuration);
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            }
        }
        System.err.println("waitForKey                    "+waitForKey);

        System.err.println("addRemoveCount                "+addRemoveCount);
        System.err.println("pauseEach                     "+pauseEach);
        System.err.println("pauseDuration                 "+pauseDuration);

        if(waitForKey) {
            UITestCase.waitForKey("Start");
        }
        org.junit.runner.JUnitCore.main(TestAddRemove03GLWindowNEWT.class.getName());
    }
}
