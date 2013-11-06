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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * Multiple GLJPanels in a JFrame's Grid
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPerf001GLJPanelInit01AWT extends UITestCase {

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton();
    }

    public void test(final GLCapabilitiesImmutable caps, final boolean useGears, final int width, final int height, final int rows,
                     final int columns, final boolean useGLJPanel, final boolean useAnim) {
        final GLAnimatorControl animator = useAnim ? new Animator() : null;

        final JFrame frame;
        final JPanel panel;

        panel = new JPanel();
        frame = new JFrame("DemoGLJPanelGridAWT");

        panel.setLayout(new GridLayout(rows, columns));
        // panel.setBounds(0, 0, width, height);
        final int panelCount = rows*columns;
        final Dimension eSize = new Dimension(width/columns, height/rows);
        final long[] t = new long[10];
        if( wait ) {
            UITestCase.waitForKey("Pre-Init");
        }
        System.err.println("INIT START");
        initCount = 0;
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    t[0] = Platform.currentTimeMillis();
                    for(int i=0; i<panelCount; i++) {
                        final GLAutoDrawable glad = useGLJPanel ? createGLJPanel(caps, useGears, animator, eSize) : createGLCanvas(caps, useGears, animator, eSize);
                        glad.addGLEventListener(new GLEventListener() {
                            @Override
                            public void init(GLAutoDrawable drawable) {
                                initCount++;
                            }
                            @Override
                            public void dispose(GLAutoDrawable drawable) {}
                            @Override
                            public void display(GLAutoDrawable drawable) {}
                            @Override
                            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
                        });
                        panel.add((Component)glad);
                    }
                    t[1] = Platform.currentTimeMillis();
                    frame.getContentPane().add(panel);

                    // frame.validate();
                    frame.pack();
                    frame.setVisible(true);
                    t[2] = Platform.currentTimeMillis();
                } } );
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        while( panelCount > initCount ) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        t[3] = Platform.currentTimeMillis();
        final double panelCountF = panelCount;
        System.err.printf("P: %d %s:%n\tctor\t%6d/t %6.2f/1%n\tvisible\t%6d/t %6.2f/1%n\tsum-i\t%6d/t %6.2f/1%n",
                panelCount,
                useGLJPanel?"GLJPanel":"GLCanvas",
                t[1]-t[0], (t[1]-t[0])/panelCountF,
                t[3]-t[1], (t[3]-t[1])/panelCountF,
                t[3]-t[0], (t[3]-t[0])/panelCountF);
        System.err.println("INIT END: "+initCount);
        if( wait ) {
            UITestCase.waitForKey("Post-Init");
        }
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        t[4] = Platform.currentTimeMillis();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.dispose();
                    } } );
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        final long ti_net = (t[4]-t[0])-duration;
        System.err.printf("T: duration %d %d%n\ttotal-d\t%6d/t %6.2f/1%n\ttotal-i\t%6d/t %6.2f/1%n",
                duration, t[4]-t[3],
                t[4]-t[0], (t[4]-t[0])/panelCountF,
                ti_net, ti_net/panelCountF);
        System.err.println("Total: "+(t[4]-t[0]));
    }

    private GLAutoDrawable createGLCanvas(GLCapabilitiesImmutable caps, boolean useGears, GLAnimatorControl anim, Dimension size) {
        GLCanvas canvas = new GLCanvas(caps);
        canvas.setSize(size);
        canvas.setPreferredSize(size);
        if( useGears ) {
            canvas.addGLEventListener(new GearsES2());
        }
        if( null != anim ) {
            anim.add(canvas);
        }
        return canvas;
    }
    private GLAutoDrawable createGLJPanel(GLCapabilitiesImmutable caps, boolean useGears, GLAnimatorControl anim, Dimension size) {
        GLJPanel canvas = new GLJPanel(caps);
        canvas.setSize(size);
        canvas.setPreferredSize(size);
        if( useGears ) {
            canvas.addGLEventListener(new GearsES2());
        }
        if( null != anim ) {
            anim.add(canvas);
        }
        return canvas;
    }

    @Test
    public void test01NopGLJPanelDef() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, width, height, rows, cols, true /* useGLJPanel */, false /*useAnim*/);
    }

    @Test
    public void test02NopGLJPanelBitmap() throws InterruptedException, InvocationTargetException {
        GLCapabilities caps = new GLCapabilities(null);
        caps.setBitmap(true);
        test(caps, false /*useGears*/, width, height, rows, cols, true /* useGLJPanel */, false /*useAnim*/);
    }

    @Test
    public void test11NopGLCanvasDef() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, width, height, rows, cols, false /* useGLJPanel */, false /*useAnim*/);
    }

    static long duration = 0; // ms
    static boolean wait = false;
    static int width = 800, height = 600, rows = 5, cols = 5;

    volatile int initCount = 0;

    public static void main(String[] args) {
        boolean useGLJPanel = true, useGears = false, manual=false;
        boolean waitMain = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-width")) {
                width = MiscUtils.atoi(args[++i], width);
            } else if(args[i].equals("-height")) {
                height = MiscUtils.atoi(args[++i], height);
            } else if(args[i].equals("-rows")) {
                rows = MiscUtils.atoi(args[++i], rows);
            } else if(args[i].equals("-cols")) {
                cols = MiscUtils.atoi(args[++i], cols);
            } else if(args[i].equals("-glcanvas")) {
                useGLJPanel = false;
            } else if(args[i].equals("-gears")) {
                useGears = true;
            } else if(args[i].equals("-wait")) {
                wait = true;
                manual = true;
            } else if(args[i].equals("-waitMain")) {
                waitMain = true;
                manual = true;
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        if( waitMain ) {
            UITestCase.waitForKey("Main-Start");
        }
        if( manual ) {
            GLProfile.initSingleton();
            TestPerf001GLJPanelInit01AWT demo = new TestPerf001GLJPanelInit01AWT();
            demo.test(null, useGears, width, height, rows, cols, useGLJPanel, false /*useAnim*/);
        } else {
            org.junit.runner.JUnitCore.main(TestPerf001GLJPanelInit01AWT.class.getName());
        }
    }

}
