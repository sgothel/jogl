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

package com.jogamp.opengl.test.junit.jogl.acore.glels;

import java.io.IOException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;


import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.opengl.GLEventListenerState;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.util.GLEventListenerCounter;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test re-association of GLContext/GLDrawables,
 * here GLContext's survival of GLDrawable destruction
 * and reuse w/ new or recreated GLDrawable.
 * <p>
 * Test utilizes {@link GLEventListenerState} for preserving the
 * GLAutoDrawable state, i.e. GLContext, all GLEventListener
 * and the GLAnimatorControl association.
 * </p>
 * <p>
 * This test moves the {@link GLEventListenerState} from a
 * NEWT GLWindow before it's destruction to an AWT GLCanvas after it's creation
 * and vice versa
 * </p>
 * <p>
 * See Bug 665 - https://jogamp.org/bugzilla/show_bug.cgi?id=665.
 * </p>
 * <p>
 * Interesting artifact w/ ATI proprietary driver is that the
 * bug causing the quirk {@link GLRendererQuirks#DontCloseX11Display}
 * also causes an XCB crash when reusing the X11 display connection
 * from AWT -> NEWT. Pre-allocating the X11 Display and keeping it referenced
 * to avoid such re-usage worksaround this problem.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLContextDrawableSwitch13Newt2AWT extends GLContextDrawableSwitchBase1 {

    @Test(timeout=30000)
    public void test01GLCanvasOnScrn2GLWindowGL2ES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        testGLCanvas2GLWindowImpl(null, reqGLCaps, GLADType.GLCanvasOnscreen, GLADType.GLWindow);
    }

    @Test(timeout=30000)
    public void test02GLCanvasOnScrn2GLWindowGLES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        testGLCanvas2GLWindowImpl(null, reqGLCaps, GLADType.GLCanvasOnscreen, GLADType.GLWindow);
    }

    @Test(timeout=30000)
    public void test11GLWindow2GLCanvasOnScrnGL2ES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        final Display dpy = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(dpy, 0);
        screen.addReference();
        testGLCanvas2GLWindowImpl(screen, reqGLCaps, GLADType.GLWindow, GLADType.GLCanvasOnscreen);
        screen.removeReference();
    }

    @Test(timeout=30000)
    public void test12GLWindow2GLCanvasOnScrnGLES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        final Display dpy = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(dpy, 0);
        screen.addReference();
        testGLCanvas2GLWindowImpl(screen, reqGLCaps, GLADType.GLWindow, GLADType.GLCanvasOnscreen);
        screen.removeReference();
    }

    @Test(timeout=30000)
    public void test21GLCanvasOffScrn2GLWindowGL2ES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        testGLCanvas2GLWindowImpl(null, reqGLCaps, GLADType.GLCanvasOffscreen, GLADType.GLWindow);
    }

    @Test(timeout=30000)
    public void test22GLCanvasOffScrn2GLWindowGLES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        testGLCanvas2GLWindowImpl(null, reqGLCaps, GLADType.GLCanvasOffscreen, GLADType.GLWindow);
    }

    @Test(timeout=30000)
    public void test31GLWindow2GLCanvasOffScrnGL2ES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        final Display dpy = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(dpy, 0);
        screen.addReference();
        testGLCanvas2GLWindowImpl(screen, reqGLCaps, GLADType.GLWindow, GLADType.GLCanvasOffscreen);
        screen.removeReference();
    }

    @Test(timeout=30000)
    public void test32GLWindow2GLCanvasOffScrnGLES2() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GLES2);
        if(null == reqGLCaps) return;
        final Display dpy = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(dpy, 0);
        screen.addReference();
        testGLCanvas2GLWindowImpl(screen, reqGLCaps, GLADType.GLWindow, GLADType.GLCanvasOffscreen);
        screen.removeReference();
    }

    private void testGLCanvas2GLWindowImpl(final Screen screen, final GLCapabilities caps, final GLADType gladType1, final GLADType gladType2) throws InterruptedException {
        if( !validateOnOffscreenLayer(gladType1, gladType2) ) {
            return;
        }
        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        final GLEventListenerCounter glelTracker = new GLEventListenerCounter();
        final Animator animator = new Animator();
        animator.start();

        final GLEventListenerState glels[] = new GLEventListenerState[1];

        // - create glad1 w/o context
        // - create context using glad1 and assign it to glad1
        {
            testGLADOneLifecycle(screen, caps, gladType1, width,
                                 height, glelTracker,
                                 snapshotGLEventListener,
                                 null, glels, animator);
        }

        // - create glad2 w/ survived context
        {
            testGLADOneLifecycle(screen, caps, gladType2, width+100,
                                 height+100, glelTracker,
                                 snapshotGLEventListener,
                                 glels[0], null, null);
        }
        animator.stop();
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */
        org.junit.runner.JUnitCore.main(TestGLContextDrawableSwitch13Newt2AWT.class.getName());
    }
}
