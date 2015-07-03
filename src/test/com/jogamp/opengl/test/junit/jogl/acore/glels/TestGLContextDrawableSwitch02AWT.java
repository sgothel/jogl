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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.awt.GLCanvas;

import jogamp.nativewindow.awt.AWTMisc;

import com.jogamp.opengl.test.junit.util.QuitAdapter;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test re-association (switching) of GLContext/GLDrawables,
 * from GLCanvas to an GLOffscreenAutoDrawable and back.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLContextDrawableSwitch02AWT extends GLContextDrawableSwitchBase0 {

    @Override
    public GLAutoDrawable createGLAutoDrawable(final QuitAdapter quitAdapter, final GLCapabilitiesImmutable caps, final int width, final int height) throws InterruptedException, InvocationTargetException {
        final GLAutoDrawable glad;
        if( caps.isOnscreen() ) {
            final Frame frame = new Frame("Gears AWT Test");
            Assert.assertNotNull(frame);

            final GLCanvas glCanvas = new GLCanvas(caps);
            Assert.assertNotNull(glCanvas);
            final Dimension glc_sz = new Dimension(width, height);
            glCanvas.setMinimumSize(glc_sz);
            glCanvas.setPreferredSize(glc_sz);
            glCanvas.setSize(glc_sz);
            glad = glCanvas;

            new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glCanvas).addTo(frame);

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

    @Override
    public void destroyGLAutoDrawable(final GLAutoDrawable glad) throws InterruptedException, InvocationTargetException {
        if( glad.getChosenGLCapabilities().isOnscreen() ) {
            final Frame frame = AWTMisc.getFrame((Component)glad);
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    final Frame _frame = frame;
                    _frame.dispose();
                }});
        } else {
            glad.destroy();
        }
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-period")) {
                i++;
                try {
                    period = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-testUnsafe")) {
                testEvenUnsafeSwapGLContext = true;
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */
        org.junit.runner.JUnitCore.main(TestGLContextDrawableSwitch02AWT.class.getName());
    }
}
