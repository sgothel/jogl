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

package com.jogamp.opengl.test.junit.jogl.acore.ect;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.awt.GLCanvas;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionNumber;

/**
 * ExclusiveContextThread base implementation to test correctness of the ExclusiveContext feature _and_ AnimatorBase with AWT.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class ExclusiveContextBase00AWT extends ExclusiveContextBase00 {

    static Thread awtEDT;
    static boolean osxCALayerAWTModBug;

    @BeforeClass
    public static void initClass00AWT() {

        final VersionNumber version170 = new VersionNumber(1, 7, 0);
        osxCALayerAWTModBug = Platform.OSType.MACOS == Platform.getOSType() &&
                              0 > Platform.getJavaVersionNumber().compareTo(version170);
        System.err.println("OSX CALayer AWT-Mod Bug "+osxCALayerAWTModBug);
        System.err.println("OSType "+Platform.getOSType());
        System.err.println("Java Version "+Platform.getJavaVersionNumber());

        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    awtEDT = Thread.currentThread();
                } } );
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.assertNull(e);
        }

    }

    @AfterClass
    public static void releaseClass00AWT() {
    }

    @Override
    protected boolean isAWTTestCase() { return true; }

    @Override
    protected Thread getAWTRenderThread() {
        return awtEDT;
    }

    @Override
    protected GLAutoDrawable createGLAutoDrawable(final String title, final int x, final int y, final int width, final int height, final GLCapabilitiesImmutable caps) {
        final GLCanvas glCanvas = new GLCanvas();

        // FIXME: Below AWT layouts freezes OSX/Java7 @ setVisible: Window.setVisible .. CWrapper@NSWindow.isKeyWindow
        // final Dimension sz = new Dimension(width, height);
        // glCanvas.setMinimumSize(sz);
        // glCanvas.setPreferredSize(sz);
        // glCanvas.setSize(sz);
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    final Frame frame = new Frame();
                    frame.setLayout(new BorderLayout());
                    frame.setMinimumSize(new Dimension(width, height));
                    frame.setBounds(x, y, width, height);
                    frame.add(glCanvas, BorderLayout.CENTER);
                    // frame.pack();
                    frame.validate();
                    if( !osxCALayerAWTModBug ) {
                        frame.setTitle(title);
                    }
                } });
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.assertNull(e);
        }

        return glCanvas;
    }

    protected Frame getFrame(final GLAutoDrawable glad) {
        Container p = ((Component)glad).getParent();
        while( null != p && !( p instanceof Frame ) ) {
            p = p.getParent();
        }
        return (Frame)p;
    }

    @Override
    protected void setGLAutoDrawableVisible(final GLAutoDrawable[] glads) {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    final int count = glads.length;
                    for(int i=0; i<count; i++) {
                        final GLAutoDrawable glad = glads[i];
                        final Frame frame = getFrame(glad);
                        frame.setVisible(true);
                    }
                } } );
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.assertNull(e);
        }
    }

    @Override
    protected void destroyGLAutoDrawableVisible(final GLAutoDrawable glad) {
        final Frame frame = getFrame(glad);
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    frame.dispose();
                } } );
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.assertNull(e);
        }
    }
}
