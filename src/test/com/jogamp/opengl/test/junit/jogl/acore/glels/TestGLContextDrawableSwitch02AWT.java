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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLDrawableUtil;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test re-association (switching) of GLContext/GLDrawables,
 * from GLCanvas to an GLOffscreenAutoDrawable and back.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLContextDrawableSwitch02AWT extends UITestCase {
    static int width, height;

    static GLCapabilities getCaps(String profile) {
        if( !GLProfile.isAvailable(profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return null;
        }
        return new GLCapabilities(GLProfile.get(profile));
    }

    @BeforeClass
    public static void initClass() {
        width  = 256;
        height = 256;
    }

    private GLAutoDrawable createGLAutoDrawable(final Frame frame, GLCapabilities caps, int width, int height) throws InterruptedException, InvocationTargetException {
        final GLAutoDrawable glad;
        if( caps.isOnscreen() ) {
            GLCanvas glCanvas = new GLCanvas(caps);
            Assert.assertNotNull(glCanvas);
            Dimension glc_sz = new Dimension(width, height);
            glCanvas.setMinimumSize(glc_sz);
            glCanvas.setPreferredSize(glc_sz);
            glCanvas.setSize(glc_sz);
            glad = glCanvas;

            frame.setLayout(new BorderLayout());
            frame.add(glCanvas, BorderLayout.CENTER);
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.pack();
                    frame.setVisible(true);
                }});

        } else {
            final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
            glad = factory.createOffscreenAutoDrawable(null, caps, null, width, height);
            Assert.assertNotNull(glad);
        }
        return glad;
    }

    @Test(timeout=30000)
    public void testSwitch2AWTGLCanvas2OffscreenGL2ES2() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        testSwitch2AWTGLCanvas2OffscreenImpl(reqGLCaps);
    }

    private void testSwitch2AWTGLCanvas2OffscreenImpl(GLCapabilities capsOnscreen) throws InterruptedException, InvocationTargetException {
        final GLCapabilities capsOffscreen = (GLCapabilities) capsOnscreen.clone();
        capsOffscreen.setOnscreen(false);

        final QuitAdapter quitAdapter = new QuitAdapter();

        final Frame frame = new Frame("Gears AWT Test");
        Assert.assertNotNull(frame);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter)).addTo(frame);

        GLAutoDrawable glCanvas = createGLAutoDrawable(frame, capsOnscreen, width, height);

        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        GearsES2 gears = new GearsES2(1);
        glCanvas.addGLEventListener(gears);
        glCanvas.addGLEventListener(snapshotGLEventListener);
        snapshotGLEventListener.setMakeSnapshot();

        Animator animator = new Animator();
        animator.add(glCanvas);
        animator.start();

        int s = 0;
        long t0 = System.currentTimeMillis();
        long t1 = t0;

        GLAutoDrawable glOffscreen = createGLAutoDrawable(null,  capsOffscreen, width, height);
        while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration ) {
            if( ( t1 - t0 ) / period > s) {
                s++;
                System.err.println(s+" - switch - START "+ ( t1 - t0 ));

                // switch context _and_ the demo synchronously
                GLDrawableUtil.swapGLContextAndAllGLEventListener(glCanvas, glOffscreen);
                snapshotGLEventListener.setMakeSnapshot();

                System.err.println(s+" - switch - END "+ ( t1 - t0 ));
            }
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        animator.stop();
        // glCanvas.destroy();
        glOffscreen.destroy();

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                final Frame _frame = frame;
                _frame.dispose();
            }});
    }

    // default timing for 2 switches
    static long duration = 2900; // ms
    static long period = 1000; // ms

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-period")) {
                i++;
                try {
                    period = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */
        org.junit.runner.JUnitCore.main(TestGLContextDrawableSwitch02AWT.class.getName());
    }
}
