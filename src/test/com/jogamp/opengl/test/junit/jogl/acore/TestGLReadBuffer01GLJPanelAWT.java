/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Dimension;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.MultisampleDemoES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

/**
 * Test synchronous GLAutoDrawable display, swap-buffer and read-pixels with AWT GLJPanel
 * including non-MSAA and MSAA framebuffer.
 * <p>
 * See {@link GLReadBuffer00Base} for related bugs and further details.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLReadBuffer01GLJPanelAWT extends GLReadBuffer00BaseAWT {

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton();
    }

    @Override
    public void test(final GLCapabilitiesImmutable caps, final boolean useSwingDoubleBuffer, final boolean skipGLOrientationVerticalFlip) {
        final AWTGLReadBufferUtil awtGLReadBufferUtil = new AWTGLReadBufferUtil(caps.getGLProfile(), false);
        final JFrame frame = new JFrame();
        final Dimension d = new Dimension(320, 240);
        final GLJPanel glad = createGLJPanel(skipGLOrientationVerticalFlip, useSwingDoubleBuffer, caps, d);
        final TextRendererGLEL textRendererGLEL = new TextRendererGLEL();
        final SnapshotGLELAWT snapshotGLEL = doSnapshot ? new SnapshotGLELAWT(textRendererGLEL, awtGLReadBufferUtil, skipGLOrientationVerticalFlip) : null;
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setLocation(64, 64);
                    final JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    panel.setDoubleBuffered(useSwingDoubleBuffer);
                    frame.getContentPane().add(panel);

                    glad.addGLEventListener(new GLEventListener() {
                        @Override
                        public void init(final GLAutoDrawable drawable) {
                            final GL gl = drawable.getGL();
                            System.err.println(VersionUtil.getPlatformInfo());
                            System.err.println("GLEventListener init on "+Thread.currentThread());
                            System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
                            System.err.println("INIT GL IS: " + gl.getClass().getName());
                            System.err.println(JoglVersion.getGLStrings(gl, null, false).toString());
                        }
                        @Override
                        public void dispose(final GLAutoDrawable drawable) {}
                        @Override
                        public void display(final GLAutoDrawable drawable) {}
                        @Override
                        public void reshape(final GLAutoDrawable drawable, final int x,final int y, final int width, final int height) {}
                    });
                    {
                        final GearsES2 gears = new GearsES2(1);
                        gears.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
                        gears.setVerbose(false);
                        glad.addGLEventListener(gears);
                    }
                    {
                        final MultisampleDemoES2 demo = new MultisampleDemoES2(caps.getSampleBuffers());
                        demo.setClearBuffers(false);;
                        glad.addGLEventListener(demo);
                    }
                    textRendererGLEL.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
                    glad.addGLEventListener(textRendererGLEL);
                    if( doSnapshot ) {
                        glad.addGLEventListener(snapshotGLEL);
                    }
                    panel.add(glad);
                    frame.pack();
                    frame.setVisible(true);
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        if( keyFrame ) {
            waitForKey("Post init: Frame# "+textRendererGLEL.frameNo);
        }
        glad.display(); // trigger initialization to get chosen-caps!
        final Dimension size0 = frame.getSize();
        final Dimension size1 = new Dimension(size0.width+100, size0.height+100);
        final Dimension size2 = new Dimension(size0.width-100, size0.height-100);
        try {
            for(int i=0; i<3; i++) {
                final String str = "Frame# "+textRendererGLEL.frameNo+", user #"+(i+1);
                System.err.println(str);
                if( keyFrame ) {
                    waitForKey(str);
                }
                textRendererGLEL.userCounter = i + 1;
                glad.display();
            }
            try { Thread.sleep(duration); } catch (final InterruptedException e) { }
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.setSize(size1);
                        frame.validate();
                    } } );
            try { Thread.sleep(duration); } catch (final InterruptedException e) { }
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.setSize(size2);
                        frame.validate();
                    } } );
            try { Thread.sleep(duration); } catch (final InterruptedException e) { }
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.setSize(size0);
                        frame.validate();
                    } } );
            try { Thread.sleep(duration); } catch (final InterruptedException e) { }

            if( doSnapshot ) {
                glad.disposeGLEventListener(snapshotGLEL, true /* remove */);
            }
            final Animator anim = new Animator(glad);
            anim.start();
            try { Thread.sleep(2*duration); } catch (final InterruptedException e) { }
            anim.stop();
        } catch (final Exception e1) {
            e1.printStackTrace();
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.dispose();
                    } } );
        } catch (final Exception e1) {
            e1.printStackTrace();
        }
    }

    private GLJPanel createGLJPanel(final boolean skipGLOrientationVerticalFlip, final boolean useSwingDoubleBuffer, final GLCapabilitiesImmutable caps, final Dimension size) {
        final GLJPanel canvas = new GLJPanel(caps);
        canvas.setSize(size);
        canvas.setPreferredSize(size);
        canvas.setMinimumSize(size);
        canvas.setDoubleBuffered(useSwingDoubleBuffer);
        canvas.setSkipGLOrientationVerticalFlip(skipGLOrientationVerticalFlip);
        return canvas;
    }

    static GLCapabilitiesImmutable caps = null;
    static boolean doSnapshot = true;
    static boolean keyFrame = false;

    public static void main(final String[] args) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-keyFrame")) {
                keyFrame = true;
            } else if(args[i].equals("-noSnapshot")) {
                doSnapshot = false;
            }
        }
        org.junit.runner.JUnitCore.main(TestGLReadBuffer01GLJPanelAWT.class.getName());
    }

}
