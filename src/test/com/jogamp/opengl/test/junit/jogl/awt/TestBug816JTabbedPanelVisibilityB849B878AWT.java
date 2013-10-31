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
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
 * AWT JFrame w/ JTabbedPanel, Moving GLCanvas between it's tabs while selecting.
 * <p>
 * Validates bugs:
 * <ul>
 *   <li>Bug 816: OSX CALayer Positioning Bug</li>
 *   <li>Bug 729: OSX CALayer shall honor the Component's visibility state</li>
 *   <li>Bug 849: AWT GLAutoDrawables (JAWTWindow) shall honor it's parent visibility state</li>
 *   <li>Bug 878: JAWTWindow's HierarchyListener doesn't set component visible (again) on 'addNotify(..)' - GLCanvas in JtabbedPane disappear</li>
 * </ul>
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug816JTabbedPanelVisibilityB849B878AWT extends UITestCase {

    static long durationPerTest = 500*4; // ms
    static boolean manual = false;

    @Test
    public void test() throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame("TestBug816OSXCALayerPos03dBug878AWT");

        final JPanel panel1 = new javax.swing.JPanel();
        final JPanel panel2 = new javax.swing.JPanel();

        panel1.setLayout(new BorderLayout());
        panel2.setLayout(new BorderLayout());

        GLProfile profile = GLProfile.get(GLProfile.GL2ES2);
        GLCapabilities glCapabilities = new GLCapabilities(profile);
        final GLCanvas glCanvas = new GLCanvas(glCapabilities);
        glCanvas.setSize(new java.awt.Dimension(640, 480));
        glCanvas.addGLEventListener(new GearsES2(1));
        panel1.add(glCanvas, BorderLayout.CENTER);

        final JTabbedPane tabbedPanel = new JTabbedPane();
        tabbedPanel.addTab("tab1", panel1);
        tabbedPanel.addTab("tab2", panel2);

        tabbedPanel.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                if (tabbedPanel.getSelectedIndex() == 0) {
                    dumpGLCanvasStats(glCanvas);
                    panel1.add(glCanvas, BorderLayout.CENTER);
                    dumpGLCanvasStats(glCanvas);
                } else {
                    System.err.println("XXXX Add GLCanvas Panel1("+id(panel1)+" -> Panel2("+id(panel2)+") START");
                    dumpGLCanvasStats(glCanvas);
                    panel2.add(glCanvas, BorderLayout.CENTER);
                    dumpGLCanvasStats(glCanvas);
                }
            }
        });

        frame.setContentPane(tabbedPanel);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.pack();
                System.err.println("XXX SetVisible ON XXX GLCanvas on Panel1("+id(panel1)+")");
                frame.setVisible(true);
            }});
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, true));
        dumpGLCanvasStats(glCanvas);

        if(manual) {
            for(long w=durationPerTest; w>0; w-=100) {
                Thread.sleep(100);
            }
        } else {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("XXXX Add GLCanvas Panel2("+id(panel2)+") -> Panel1("+id(panel1)+" START");
                    tabbedPanel.setSelectedIndex(0);
                }});
            Thread.sleep(durationPerTest/4);

            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("XXXX Add GLCanvas Panel1("+id(panel1)+" -> Panel2("+id(panel2)+") START");
                    tabbedPanel.setSelectedIndex(1);
                }});
            Thread.sleep(durationPerTest/4);

            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("XXXX Add GLCanvas Panel2("+id(panel2)+") -> Panel1("+id(panel1)+" START");
                    tabbedPanel.setSelectedIndex(0);
                }});
            Thread.sleep(durationPerTest/4);

            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("XXXX Add GLCanvas Panel1("+id(panel1)+" -> Panel2("+id(panel2)+") START");
                    tabbedPanel.setSelectedIndex(1);
                }});
            Thread.sleep(durationPerTest/4);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                System.err.println("XXX SetVisible OFF XXX");
                frame.dispose();
            } });
    }

    private static String id(Object obj) { return "0x"+Integer.toHexString(obj.hashCode()); }

    static void dumpGLCanvasStats(GLCanvas glCanvas) {
        System.err.println("XXXX GLCanvas: comp "+glCanvas+", visible "+glCanvas.isVisible()+", showing "+glCanvas.isShowing()+
                ", displayable "+glCanvas.isDisplayable()+", "+glCanvas.getWidth()+"x"+glCanvas.getHeight());
    }

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        org.junit.runner.JUnitCore.main(TestBug816JTabbedPanelVisibilityB849B878AWT.class.getName());
    }
}
