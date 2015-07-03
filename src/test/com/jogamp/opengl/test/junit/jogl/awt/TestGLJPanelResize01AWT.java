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
package com.jogamp.opengl.test.junit.jogl.awt;

import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Multiple GLJPanels in a JFrame
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLJPanelResize01AWT extends UITestCase {

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton();
    }

    static Dimension[] esize00 = {
        new Dimension(281, 151),
        new Dimension(282, 151),
        new Dimension(283, 151),
        new Dimension(284, 151),

        new Dimension(284, 152),
        new Dimension(283, 152),
        new Dimension(282, 152),
        new Dimension(281, 152),

        new Dimension(291, 153),
        new Dimension(292, 153),
        new Dimension(293, 153),
        new Dimension(294, 153),

        new Dimension(281, 154),
        new Dimension(282, 154),
        new Dimension(283, 154),
        new Dimension(284, 154)
    };
    static Dimension[] esize01 = {
        new Dimension(283, 154), // #3: new sub-aligned image in pixelBuffer-1
        new Dimension(291, 154), // #2: new pixelBuffer-1
        new Dimension(282, 154), // #1: new pixelBuffer-0
    };
    static Dimension[] esize02 = {
        new Dimension(291, 154), // #2: new pixelBuffer-1
        new Dimension(282, 154), // #1: new pixelBuffer-0
    };

    public void test(final GLCapabilitiesImmutable caps, final Dimension[] dims, final boolean useSwingDoubleBuffer) {
        final int cols = 4;
        final int rows = dims.length / cols + ( dims.length % cols > 0 ? 1 : 0 );
        final JFrame[] frame = new JFrame[] { null };

        System.err.println("Frame size: cols x rows "+cols+"x"+rows);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame[0] = new JFrame();
                    frame[0].setLocation(64, 64);
                    final JPanel panel = new JPanel();
                    panel.setLayout(null); // new BorderLayout());
                    panel.setDoubleBuffered(useSwingDoubleBuffer);
                    frame[0].getContentPane().add(panel);

                    final int x0 = 4;
                    int x = x0, y = 4;
                    int maxRowWidth = 0;
                    for(int i=0; i<rows; i++) {
                        int maxColHeight = 0;
                        for(int j=0; j<cols; j++) {
                            final int idx = i*cols+j;
                            if( idx >= dims.length ) { break; }
                            final Dimension d = dims[idx];
                            if( d.height > maxColHeight ) {
                                maxColHeight = d.height;
                            }
                            final GLJPanel glad = createGLJPanel(useSwingDoubleBuffer, caps, d, "[r "+i+", c "+j+"]");
                            panel.add(glad);
                            glad.setLocation(x, y);
                            x+=d.width+4;
                        }
                        if( x > maxRowWidth ) {
                            maxRowWidth = x;
                        }
                        x = x0;
                        y += maxColHeight+4;
                    }
                    frame[0].setSize(maxRowWidth+4+64, y+4+64);
                    // frame[0].pack();
                    frame[0].setVisible(true);
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        try {
            Thread.sleep(duration);
        } catch (final InterruptedException e1) {
            e1.printStackTrace();
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame[0].dispose();
                    } } );
        } catch (final Exception e1) {
            e1.printStackTrace();
        }
    }

    private GLJPanel createGLJPanel(final boolean useSwingDoubleBuffer, final GLCapabilitiesImmutable caps, final Dimension size, final String name) {
        final GLJPanel canvas = new GLJPanel(caps);
        canvas.setName(name);
        canvas.setSize(size);
        canvas.setPreferredSize(size);
        canvas.setMinimumSize(size);
        canvas.setDoubleBuffered(useSwingDoubleBuffer);
        final GearsES2 g = new GearsES2(0);
        g.setVerbose(false);
        canvas.addGLEventListener(g);
        return canvas;
    }

    static GLCapabilitiesImmutable caps = null;

    // @Test
    public void test00() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), esize00, false /*useSwingDoubleBuffer*/);
    }

    @Test
    public void test01() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), esize01, false /*useSwingDoubleBuffer*/);
    }

    @Test
    public void test02() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), esize02, false /*useSwingDoubleBuffer*/);
    }

    static long duration = 600; // ms

    public static void main(final String[] args) {
        boolean useSwingDoubleBuffer=false, manual=false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-swingDoubleBuffer")) {
                useSwingDoubleBuffer = true;
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        if( manual ) {
            GLProfile.initSingleton();
            final TestGLJPanelResize01AWT demo = new TestGLJPanelResize01AWT();
            demo.test(new GLCapabilities(null), esize01, useSwingDoubleBuffer);
        } else {
            org.junit.runner.JUnitCore.main(TestGLJPanelResize01AWT.class.getName());
        }
    }

}
