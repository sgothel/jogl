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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.GLEventListenerCounter;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * Bug 1245
 * <p>
 * https://jogamp.org/bugzilla/show_bug.cgi?id=1245
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug1245JTabbedPanelCrashAWT extends UITestCase {

    static long durationPerTest = 500*4; // ms
    static boolean manual = false;

    @SuppressWarnings("serial")
    static class View3D extends JPanel {
        final GLCanvas canvas;
        final Animator animator;
        final int num;

        public View3D(final int num) {
            this.num = num;
            this.setLayout(new BorderLayout());
            canvas = new GLCanvas();
            canvas.setSize(new Dimension(100, 100));
            canvas.setMinimumSize(new Dimension(100, 100));
            add(canvas, BorderLayout.CENTER);
            animator = new Animator();
            animator.add(canvas);
            // could do animator.start() here as well,
            // just to be nice - we start/stop at add/remove Notify
        }
        @Override
        public void addNotify() {
            System.err.println("View3D["+num+"].addNotify()");
            super.addNotify();
            if( null != animator ) {
                animator.start();
            }
        }
        @Override
        public void removeNotify() {
            System.err.println("View3D["+num+"].removeNotify()");
            if( null != animator ) {
                animator.stop();
            }
            super.removeNotify();
        }

        public String getGLCanvasStats() {
            return "GLCanvas: comp "+canvas.getBounds()+", visible "+canvas.isVisible()+", showing "+canvas.isShowing()+
                    ", displayable "+canvas.isDisplayable()+", "+canvas.getSurfaceWidth()+"x"+canvas.getSurfaceHeight()+
                    ", "+canvas.getChosenGLCapabilities()+", drawable 0x"+Long.toHexString(canvas.getHandle());
        }
    }

    final GLEventListenerCounter glelCounter = new GLEventListenerCounter();

    private JTabbedPane createAndShowGUI(final JFrame frame, final View3D[] views) {
        final JPanel panel = new JPanel(new GridLayout(1, 1));
        final JTabbedPane tabbedPanel = new JTabbedPane();
        for(int i=0; i<views.length; i++) {
            final GLEventListener demo;
            if( i%2 == 0 ) {
                final GearsES2 gears = new GearsES2(1);
                gears.setVerbose(false);
                demo = gears;
            } else {
                final RedSquareES2 red = new RedSquareES2(1);
                red.setVerbose(false);
                demo = red;
            }
            views[i] = new View3D(i);
            views[i].canvas.addGLEventListener(glelCounter);
            views[i].canvas.addGLEventListener(demo);
            tabbedPanel.addTab("Tab "+i, null, views[i], "Does nothing");
        }
        tabbedPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPanel.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(final javax.swing.event.ChangeEvent evt) {
                final int idx = tabbedPanel.getSelectedIndex();
                if( 0 <= idx && idx < views.length ) {
                    System.err.println("Pane["+idx+"]: State Changed: "+evt);
                    System.err.println("Pane["+idx+"]: "+views[idx].getGLCanvasStats());
                }
            }
        });

        panel.add(tabbedPanel);
        frame.add(panel, BorderLayout.CENTER);
        frame.setSize(640,480);

        return tabbedPanel;
    }

    private static String id(final Object obj) { return "0x"+Integer.toHexString(obj.hashCode()); }

    @BeforeClass
    public static void startup() {
        GLProfile.initSingleton();
    }

    @Test
    public void test01() throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame("Java3DApplication");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final View3D[] views = new View3D[4];
        final JTabbedPane[] tabbedPane = { null };
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                tabbedPane[0] = createAndShowGUI(frame, views);
                System.err.println("XXX SetVisible ON XXX");
                frame.setVisible(true);
            } } );
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        for(int i=0; i<views.length; i++) {
            System.err.printf("View "+i+": "+views[i]+",%n       "+views[i].getGLCanvasStats()+"%n%n");
        }

        System.err.println("XXX POST.VISIBLE: "+glelCounter);
        if(manual) {
            Thread.sleep(durationPerTest);
            System.err.println("XXX POST.ACTION: "+glelCounter);
        } else {
            final JTabbedPane tabbedPanel = tabbedPane[0];

            for(int i=0; i<views.length; i++) {
                Thread.sleep(durationPerTest/views.length);
                switchTab(tabbedPanel, views, i, (i+1)%views.length);
            }
            Thread.sleep(durationPerTest/views.length);
            switchTab(tabbedPanel, views, 0, 1);

            Thread.sleep(durationPerTest/views.length);
            switchTab(tabbedPanel, views, 1, 0);

            System.err.println("XXX POST.ACTION: "+glelCounter);
            Assert.assertTrue(glelCounter.initCount >= views.length);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                System.err.println("XXX SetVisible OFF XXX");
                frame.dispose();
            } });
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, false));
        System.err.println("XXX POST.DISPOSE: "+glelCounter);
    }

    void switchTab(final JTabbedPane tabbedPanel, final View3D[] views, final int thisId, final int nextId) throws InvocationTargetException, InterruptedException {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                System.err.println("XXXX Panel("+id(views[thisId])+" -> Panel("+id(views[nextId])+") START");
                tabbedPanel.setSelectedIndex(nextId);
            }});
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        org.junit.runner.JUnitCore.main(TestBug1245JTabbedPanelCrashAWT.class.getName());
    }
}
