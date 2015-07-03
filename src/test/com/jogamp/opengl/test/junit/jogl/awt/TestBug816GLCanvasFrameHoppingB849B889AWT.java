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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Moving GLCanvas between 2 AWT JFrame
 * <p>
 * Validates bugs:
 * <ul>
 *   <li>Bug 816: OSX CALayer Positioning Bug</li>
 *   <li>Bug 729: OSX CALayer shall honor the Component's visibility state</li>
 *   <li>Bug 849: AWT GLAutoDrawables (JAWTWindow) shall honor it's parent visibility state</li>
 *   <li>Bug 878: JAWTWindow's HierarchyListener doesn't set component visible (again) on 'addNotify(..)' - GLCanvas in JtabbedPane disappear</li>
 *   <li>Bug 889: GLCanvas disappear when moves between two JFrame</li>
 * </ul>
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug816GLCanvasFrameHoppingB849B889AWT extends UITestCase {
    static long durationPerTest = 500*4; // ms
    static boolean manual = false;

    @Test
    public void test01AllVisible() throws InterruptedException, InvocationTargetException {
        test(false);
    }

    @Test
    public void test02VisibleWithCanvas() throws InterruptedException, InvocationTargetException {
        test(true);
    }

    private void test(final boolean onlyVisibleWithCanvas) throws InterruptedException, InvocationTargetException {
        final JFrame frame1 = new JFrame("Bug889 #1");
        final JPanel panel1 = new javax.swing.JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.setSize(new java.awt.Dimension(640, 480));
        frame1.setContentPane(panel1);
        frame1.setSize(640, 480);
        frame1.setLocation(64, 64);

        final JFrame frame2 = new JFrame("Bug889 #2");
        final JPanel panel2 = new javax.swing.JPanel();
        panel2.setLayout(new BorderLayout());
        panel2.setSize(new java.awt.Dimension(640, 480));
        frame2.setContentPane(panel2);
        frame2.setSize(640, 480);
        frame2.setLocation(800, 64);

        final GLProfile profile = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities glCapabilities = new GLCapabilities(profile);
        final GLCanvas glCanvas = new GLCanvas(glCapabilities);
        glCanvas.setSize(new java.awt.Dimension(640, 480));
        glCanvas.addGLEventListener(new GearsES2(1));
        panel1.add(glCanvas, BorderLayout.CENTER);

        final JButton bMoveP1toP2 = new JButton("Move to Panel2");
        bMoveP1toP2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                System.err.println("XXXX Move P1 -> P2 - START");
                dumpGLCanvasStats(glCanvas);
                panel2.add(glCanvas, BorderLayout.CENTER);
                if( onlyVisibleWithCanvas ) {
                    frame1.setVisible(false);
                    frame2.setVisible(true);
                    frame2.toFront();
                } else {
                    frame1.validate();
                    frame2.validate();
                }
                dumpGLCanvasStats(glCanvas);
                System.err.println("XXXX Move P1 -> P2 - END");
            }
        });
        panel1.add(bMoveP1toP2, BorderLayout.NORTH);

        final JButton bMoveP2toP1 = new JButton("Move to Panel1");
        bMoveP2toP1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                System.err.println("XXXX Move P2 -> P1 - START");
                dumpGLCanvasStats(glCanvas);
                panel1.add(glCanvas, BorderLayout.CENTER);
                if( onlyVisibleWithCanvas ) {
                    frame2.setVisible(false);
                    frame1.setVisible(true);
                    frame1.toFront();
                } else {
                    frame2.validate();
                    frame1.validate();
                }
                dumpGLCanvasStats(glCanvas);
                System.err.println("XXXX Move P2 -> P1 - END");
            }
        });
        panel2.add(bMoveP2toP1, BorderLayout.NORTH);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                // frame1.pack();
                System.err.println("XXX SetVisible ON XXX GLCanvas on Panel1("+id(panel1)+")");
                if( onlyVisibleWithCanvas ) {
                    frame1.setVisible(true);
                } else {
                    frame1.setVisible(true);
                    frame2.setVisible(true);
                }
            }});
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame1, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, true));
        dumpGLCanvasStats(glCanvas);

        if(manual) {
            for(long w=durationPerTest; w>0; w-=100) {
                Thread.sleep(100);
            }
        } else {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("XXXX Add GLCanvas Panel1("+id(panel1)+" -> Panel2("+id(panel2)+") START");
                    dumpGLCanvasStats(glCanvas);
                    panel2.add(glCanvas, BorderLayout.CENTER);
                    if( onlyVisibleWithCanvas ) {
                        frame1.setVisible(false);
                        frame2.setVisible(true);
                        frame2.toFront();
                    } else {
                        frame1.validate();
                        frame2.validate();
                    }
                    dumpGLCanvasStats(glCanvas);
                }});
            Thread.sleep(durationPerTest/4);

            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("XXXX Add GLCanvas Panel2("+id(panel2)+") -> Panel1("+id(panel1)+" START");
                    dumpGLCanvasStats(glCanvas);
                    panel1.add(glCanvas, BorderLayout.CENTER);
                    if( onlyVisibleWithCanvas ) {
                        frame2.setVisible(false);
                        frame1.setVisible(true);
                        frame1.toFront();
                    } else {
                        frame2.validate();
                        frame1.validate();
                    }
                    dumpGLCanvasStats(glCanvas);
                }});
            Thread.sleep(durationPerTest/4);

            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("XXXX Add GLCanvas Panel1("+id(panel1)+" -> Panel2("+id(panel2)+") START");
                    dumpGLCanvasStats(glCanvas);
                    panel2.add(glCanvas, BorderLayout.CENTER);
                    if( onlyVisibleWithCanvas ) {
                        frame1.setVisible(false);
                        frame2.setVisible(true);
                        frame2.toFront();
                    } else {
                        frame1.validate();
                        frame2.validate();
                    }
                    dumpGLCanvasStats(glCanvas);
                }});
            Thread.sleep(durationPerTest/4);

            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("XXXX Add GLCanvas Panel2("+id(panel2)+") -> Panel1("+id(panel1)+" START");
                    dumpGLCanvasStats(glCanvas);
                    panel1.add(glCanvas, BorderLayout.CENTER);
                    if( onlyVisibleWithCanvas ) {
                        frame2.setVisible(false);
                        frame1.setVisible(true);
                        frame1.toFront();
                    } else {
                        frame2.validate();
                        frame1.validate();
                    }
                    dumpGLCanvasStats(glCanvas);
                }});
            Thread.sleep(durationPerTest/4);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                System.err.println("XXX SetVisible OFF XXX");
                frame1.dispose();
                frame2.dispose();
            } });
    }

    private static String id(final Object obj) { return "0x"+Integer.toHexString(obj.hashCode()); }

    static void dumpGLCanvasStats(final GLCanvas glCanvas) {
        System.err.println("XXXX GLCanvas: comp "+glCanvas+", visible "+glCanvas.isVisible()+", showing "+glCanvas.isShowing()+
                ", displayable "+glCanvas.isDisplayable()+", "+glCanvas.getSurfaceWidth()+"x"+glCanvas.getSurfaceHeight());
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        org.junit.runner.JUnitCore.main(TestBug816GLCanvasFrameHoppingB849B889AWT.class.getName());
    }

}
