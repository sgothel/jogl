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

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Raw initialization of multiple offscreen GLAutoDrawables
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPerf001RawInit00NEWT extends UITestCase {

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton();
    }

    public void testChooseOnly(final int runNum, final Screen screen, final int count) throws InterruptedException {
        final long[] t = new long[10];
        final GLProfile glp = GLProfile.getGL2ES2();
        final int[] chosenCfgs = { 0 };

        final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        final GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(screen.getDisplay().getGraphicsDevice(), caps);

        if( wait && 0 == runNum ) {
            UITestCase.waitForKey("Pre-Init");
        }
        System.err.println("INIT START #"+runNum);
        screen.getDisplay().getEDTUtil().invoke(true, new Runnable() {
            public void run() {
                t[0] = Platform.currentTimeMillis();
                for(int i=0; i<count; i++) {
                    final AbstractGraphicsConfiguration cfg = factory.chooseGraphicsConfiguration(caps, caps, null, screen.getGraphicsScreen(), VisualIDHolder.VID_UNDEFINED);
                    if( null != cfg ) {
                        chosenCfgs[0]++;
                    }
                }
                t[1] = Platform.currentTimeMillis();
            } } );

        final double countF = count;
        System.err.printf("Run: %d, count %d/%d raw:%n\tchoose\t%6d/t %6.2f/1%n",
                runNum, chosenCfgs[0], count, t[1]-t[0], (t[1]-t[0])/countF);
        System.err.println("INIT END #"+runNum);
        if( wait && 2 == runNum ) {
            UITestCase.waitForKey("Post-Init");
        }
    }

    public void testFull(final int runNum, final int width, final int height, final int count) {
        // panel.setBounds(0, 0, width, height);
        final long[] t = new long[10];
        final GLDrawable[] glDrawables = new GLDrawable[count];
        final GLContext[] glConti = new GLContext[count];
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
        if( wait && 0 == runNum ) {
            UITestCase.waitForKey("Pre-Init");
        }
        System.err.println("INIT START #"+runNum);
        t[0] = Platform.currentTimeMillis();
        for(int i=0; i<count; i++) {
            glDrawables[i] = factory.createOffscreenDrawable(null, caps, null, width, height);
        }
        t[1] = Platform.currentTimeMillis();
        for(int i=0; i<count; i++) {
            glDrawables[i].setRealized(true);
        }
        t[2] = Platform.currentTimeMillis();
        // 1st makeCurrent - context creation incl. release
        for(int i=0; i<count; i++) {
            final GLContext context = glDrawables[i].createContext(null);
            if( GLContext.CONTEXT_NOT_CURRENT >= context.makeCurrent() ) {
                // oops
                glDrawables[i].setRealized(false);
                glDrawables[i] = null;
                glConti[i] = null;
                continue;
            }
            glConti[i] = context;
            context.release();
        }
        t[3] = Platform.currentTimeMillis();
        // 2nd makeCurrent and release
        for(int i=0; i<count; i++) {
            final GLContext context = glConti[i];
            if( GLContext.CONTEXT_NOT_CURRENT >= context.makeCurrent() ) {
                // oops
                glDrawables[i].setRealized(false);
                glDrawables[i] = null;
                glConti[i] = null;
                continue;
            }
            context.release();
        }
        t[4] = Platform.currentTimeMillis();

        final double countF = count;
        System.err.printf("Run: %d, count %d raw:%n\tglad-create\t%6d/t %6.2f/1%n"+
                          "\tglad-realize\t%6d/t %6.2f/1%n"+
                          "\tctx-create1\t%6d/t %6.2f/1%n"+
                          "\tctx-curren2\t%6d/t %6.2f/1%n"+
                          "\tglad-ctx-init\t%6d/t %6.2f/1%n",
                runNum, count,
                t[1]-t[0], (t[1]-t[0])/countF, // create
                t[2]-t[1], (t[2]-t[1])/countF, // realize
                t[3]-t[2], (t[3]-t[2])/countF, // context-create1
                t[4]-t[3], (t[4]-t[3])/countF, // context-curren2
                t[3]-t[0], (t[3]-t[0])/countF);// init total
        System.err.println("INIT END #"+runNum);
        if( wait && 2 == runNum ) {
            UITestCase.waitForKey("Post-Init");
        }

        // destroy
        for(int i=0; i<count; i++) {
            final GLContext context = glConti[i];
            if( null != context ) {
                context.destroy();
            }
            final GLDrawable glDrawable = glDrawables[i];
            if( null != glDrawable ) {
                glDrawable.setRealized(false);
            }
            glConti[i] = null;
            glDrawables[i] = null;
        }
    }

    @Test
    public void test01ChooseOnly() throws InterruptedException, InvocationTargetException {
        if( 0 != manualTest && 1 != manualTest ) {
            return;
        }
        final Display display = NewtFactory.createDisplay(null, false);
        final Screen screen = NewtFactory.createScreen(display, 0);
        screen.addReference();
        try {
            testChooseOnly(0, screen, count); // warm-up
            testChooseOnly(1, screen, count);
            testChooseOnly(2, screen, count);
        } finally {
            screen.removeReference();
        }
    }

    @Test
    public void test02Full() throws InterruptedException, InvocationTargetException {
        if( 0 != manualTest && 2 != manualTest ) {
            return;
        }
        testFull(0, width, height, count); // warm-up
        testFull(1, width, height, count);
        testFull(2, width, height, count);
    }

    static boolean wait = false;
    static int manualTest = 0;
    static int width = 800, height = 600, count = 50;

    public static void main(final String[] args) {
        boolean waitMain = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-width")) {
                width = MiscUtils.atoi(args[++i], width);
            } else if(args[i].equals("-height")) {
                height = MiscUtils.atoi(args[++i], height);
            } else if(args[i].equals("-count")) {
                count = MiscUtils.atoi(args[++i], count);
            } else if(args[i].equals("-wait")) {
                wait = true;
            } else if(args[i].equals("-waitMain")) {
                waitMain = true;
            } else if(args[i].equals("-test")) {
                manualTest = MiscUtils.atoi(args[++i], manualTest);
            }
        }
        if( waitMain ) {
            UITestCase.waitForKey("Main-Start");
        }
        org.junit.runner.JUnitCore.main(TestPerf001RawInit00NEWT.class.getName());
    }

}
