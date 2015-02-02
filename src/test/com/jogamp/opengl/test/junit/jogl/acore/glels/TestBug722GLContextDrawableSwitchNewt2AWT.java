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
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.test.junit.util.GLEventListenerCounter;
import com.jogamp.opengl.test.junit.util.MiscUtils;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Tests Bug 722
 * <p>
 * See Bug 722 - https://jogamp.org/bugzilla/show_bug.cgi?id=722.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug722GLContextDrawableSwitchNewt2AWT extends GLContextDrawableSwitchBase1 {

    static int loops = 10;
    static long duration2 = 100; // ms

    /**
     * Interesting artifact w/ ATI proprietary driver is that the
     * bug causing the quirk {@link GLRendererQuirks#DontCloseX11Display}
     * also causes an XCB crash when reusing the X11 display connection
     * from AWT -> NEWT. Pre-allocating the X11 Display and keeping it referenced
     * to avoid such re-usage worksaround this problem.
     */
    public static boolean fixedNewtDisplay = true;

    @Test(timeout=180000) // TO 3 min
    public void test11GLWindow2GLCanvasOnScrnGL2ES2() throws InterruptedException {
        final GLCapabilities caps = getCaps(GLProfile.GL2ES2);
        if(null == caps) {
            System.err.println("GL2ES2 n/a, test n/a.");
            return;
        }
        if( jogamp.nativewindow.jawt.JAWTUtil.isOffscreenLayerRequired() ) {
            System.err.println("JAWT required offscreen, test n/a.");
            return;
        }


        final GLADType gladType1 = GLADType.GLWindow;
        final GLADType gladType2 = GLADType.GLCanvasOnscreen;

        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        final Animator animator = new Animator();
        animator.start();

        final Display dpy;
        final Screen screen;
        if( fixedNewtDisplay ) {
            dpy = NewtFactory.createDisplay(null);
            screen = NewtFactory.createScreen(dpy, 0);
            screen.addReference();
        } else {
            dpy = null;
            screen = null;
        }

        duration = duration2;

        for(int i=0; i<loops; i++) {
            final GLEventListenerState glels[] = new GLEventListenerState[1];
            final GLEventListenerCounter glelTracker = new GLEventListenerCounter();

            // - create glad1 w/o context
            // - create context using glad1 and assign it to glad1
            {
                System.err.println("Test "+i+"/"+loops+".1: GLAD-1 "+gladType1+", preserving.");
                testGLADOneLifecycle(screen, caps, gladType1, width, height,
                                     glelTracker, snapshotGLEventListener,
                                     null,
                                     glels, animator);
                System.err.println("Test "+i+"/"+loops+".1: done");
            }

            // - create glad2 w/ survived context
            {
                System.err.println("Test "+i+"/"+loops+".2: GLAD-1 "+gladType2+", restoring.");
                testGLADOneLifecycle(screen, caps, gladType2, width+100, height+100,
                                     glelTracker, snapshotGLEventListener,
                                     glels[0],
                                     null, null);
                System.err.println("Test "+i+"/"+loops+".2: done.");
            }
        }
        animator.stop();

        if( fixedNewtDisplay ) {
            screen.removeReference();
        }
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration2 = MiscUtils.atol(args[i], duration2);
            } else if(args[i].equals("-loops")) {
                i++;
                loops = MiscUtils.atoi(args[i], loops);
            } else if(args[i].equals("-noFixedNewtDisplay")) {
                fixedNewtDisplay = false;
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */
        org.junit.runner.JUnitCore.main(TestBug722GLContextDrawableSwitchNewt2AWT.class.getName());
    }
}
