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

package com.jogamp.opengl.test.junit.jogl.tile;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.opengl.test.junit.jogl.demos.es1.RedSquareES1;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTiledPrintingGearsSwingAWT extends TiledPrintingAWTBase  {

    static boolean waitForKey = false;
    /** only when run manually .. */
    static boolean allow600dpi = false;
    static GLProfile glp;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2)) {
            glp = GLProfile.get(GLProfile.GL2);
            Assert.assertNotNull(glp);
            width  = 640;
            height = 480;
        } else {
            setTestSupported(false);
        }
        // Runtime.getRuntime().traceInstructions(true);
        // Runtime.getRuntime().traceMethodCalls(true);
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(final GLCapabilities caps, final boolean layered, final boolean skipGLOrientationVerticalFlip) throws InterruptedException, InvocationTargetException {
        final int layerStepX = width/6, layerStepY = height/6;
        final Dimension glc_sz = new Dimension(layered ? width - 2*layerStepX : width/2, layered ? height - 2*layerStepY : height);
        final GLJPanel glJPanel1 = new GLJPanel(caps);
        Assert.assertNotNull(glJPanel1);
        glJPanel1.setSkipGLOrientationVerticalFlip(skipGLOrientationVerticalFlip);
        glJPanel1.setMinimumSize(glc_sz);
        glJPanel1.setPreferredSize(glc_sz);
        if( layered ) {
            glJPanel1.setBounds(layerStepX/2, layerStepY/2, glc_sz.width, glc_sz.height);
        } else {
            glJPanel1.setBounds(0, 0, glc_sz.width, glc_sz.height);
        }
        {
            final Gears demo = new Gears();
            demo.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
            glJPanel1.addGLEventListener(demo);
        }

        final GLJPanel glJPanel2 = new GLJPanel(caps);
        Assert.assertNotNull(glJPanel2);
        glJPanel2.setSkipGLOrientationVerticalFlip(skipGLOrientationVerticalFlip);
        glJPanel2.setMinimumSize(glc_sz);
        glJPanel2.setPreferredSize(glc_sz);
        if( layered ) {
            glJPanel2.setBounds(3*layerStepY, 2*layerStepY, glc_sz.width, glc_sz.height);
        } else {
            glJPanel2.setBounds(0, 0, glc_sz.width, glc_sz.height);
        }
        {
            final RedSquareES1 demo = new RedSquareES1();
            demo.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
            glJPanel2.addGLEventListener(demo);
        }

        final JComponent demoPanel;
        if( layered ) {
            glJPanel1.setOpaque(true);
            glJPanel2.setOpaque(false);
            final Dimension lsz = new Dimension(width, height);
            demoPanel = new JLayeredPane();
            demoPanel.setMinimumSize(lsz);
            demoPanel.setPreferredSize(lsz);
            demoPanel.setBounds(0, 0, lsz.width, lsz.height);
            demoPanel.setBorder(BorderFactory.createTitledBorder("Layered Pane"));
            demoPanel.add(glJPanel1, JLayeredPane.DEFAULT_LAYER);
            demoPanel.add(glJPanel2, Integer.valueOf(1));
            final JButton tb = new JButton("On Top");
            tb.setBounds(4*layerStepY, 3*layerStepY, 100, 50);
            demoPanel.add(tb, Integer.valueOf(2));
        } else {
            demoPanel = new JPanel();
            demoPanel.add(glJPanel1);
            demoPanel.add(glJPanel2);
        }

        final JFrame frame = new JFrame("Swing Print");
        Assert.assertNotNull(frame);

        final ActionListener print72DPIAction = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                doPrintManual(frame, 72, 0, -1, -1);
            } };
        final ActionListener print300DPIAction = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                doPrintManual(frame, 300, -1, -1, -1);
            } };
        final ActionListener print600DPIAction = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                doPrintManual(frame, 600, -1, -1, -1);
            } };
        final Button print72DPIButton = new Button("72dpi");
        print72DPIButton.addActionListener(print72DPIAction);
        final Button print300DPIButton = new Button("300dpi");
        print300DPIButton.addActionListener(print300DPIAction);
        final Button print600DPIButton = new Button("600dpi");
        print600DPIButton.addActionListener(print600DPIAction);

        final JPanel printPanel = new JPanel();
        printPanel.add(print72DPIButton);
        printPanel.add(print300DPIButton);
        printPanel.add(print600DPIButton);
        final JPanel southPanel = new JPanel();
        southPanel.add(new Label("South"));
        final JPanel eastPanel = new JPanel();
        eastPanel.add(new Label("East"));
        final JPanel westPanel = new JPanel();
        westPanel.add(new Label("West"));

        final Animator animator = new Animator();
        animator.add(glJPanel1);
        animator.add(glJPanel2);
        final QuitAdapter quitAdapter = new QuitAdapter();
        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter), glJPanel1).addTo(glJPanel1);
        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter), glJPanel2).addTo(glJPanel2);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glJPanel2).addTo(frame);

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    final Container fcont = frame.getContentPane();
                    fcont.setLayout(new BorderLayout());
                    fcont.add(printPanel, BorderLayout.NORTH);
                    fcont.add(demoPanel, BorderLayout.CENTER);
                    fcont.add(southPanel, BorderLayout.SOUTH);
                    fcont.add(eastPanel, BorderLayout.EAST);
                    fcont.add(westPanel, BorderLayout.WEST);
                    fcont.validate();
                    frame.pack();
                    frame.setVisible(true);
                } } ) ;

        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glJPanel1, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glJPanel2, true));

        animator.setUpdateFPSFrames(60, System.err);
        animator.start();
        Assert.assertEquals(true, animator.isAnimating());

        boolean printDone = false;
        while(!quitAdapter.shouldQuit() && animator.isAnimating() && ( 0 == duration || animator.getTotalFPSDuration()<duration )) {
            Thread.sleep(200);
            if( !printDone ) {
                printDone = true;
                {
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, -1 /* offscreen-type */, 72, 0, -1, -1, false /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
                {
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, -1 /* offscreen-type */, 72, 8, -1, -1, false /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
                {
                    // No AA needed for 150 dpi and greater :)
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, -1 /* offscreen-type */, 150, -1, -1, -1, false /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
                {
                    // No AA needed for 150 dpi and greater :)
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, -1 /* offscreen-type */, 150, -1, 2048, 2048, false /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
                {
                    // No AA needed for 150 dpi and greater :)
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, -1 /* offscreen-type */, 150, -1, -1, -1, true /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
                {
                    // No AA needed for 150 dpi and greater :)
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, BufferedImage.TYPE_INT_ARGB_PRE /* offscreen-type */, 150, -1, -1, -1, false /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
                {
                    // No AA needed for 150 dpi and greater :)
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, BufferedImage.TYPE_INT_ARGB /* offscreen-type */, 150, -1, -1, -1, false /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
                {
                    // No AA needed for 150 dpi and greater :)
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, BufferedImage.TYPE_INT_RGB /* offscreen-type */, 150, -1, -1, -1, false /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
                {
                    // No AA needed for 150 dpi and greater :)
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, BufferedImage.TYPE_INT_BGR /* offscreen-type */, 150, -1, -1, -1, false /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
                {
                    // No AA needed for 150 dpi and greater :)
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, BufferedImage.TYPE_INT_ARGB_PRE /* offscreen-type */, 150, -1, -1, -1, true /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
                if( allow600dpi ) {
                    // No AA needed for 300 dpi and greater :)
                    final PrintableBase p = doPrintAuto(frame, PageFormat.LANDSCAPE, null, -1 /* offscreen-type */, 600, -1, -1, -1, false /* resizeWithinPrint */);
                    waitUntilPrintJobsIdle(p);
                }
            }
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glJPanel1);
        Assert.assertNotNull(glJPanel2);
        Assert.assertNotNull(animator);

        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(false);
            }});
        Assert.assertEquals(false, frame.isVisible());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                final Frame _frame = frame;
                _frame.remove(demoPanel);
                _frame.dispose();
            }});
    }

    @Test
    public void test01_flip1_aa0() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, false, false);
    }

    @Test
    public void test01_flip1_aa0_layered() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(8);
        runTestGL(caps, true, false);
    }

    @Test
    public void test01_flip1_aa0_bitmap() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS == Platform.getOSType() ) {
            final GLCapabilities caps = new GLCapabilities(glp);
            caps.setBitmap(true);
            runTestGL(caps, false, false);
        } // issues w/ AMD catalyst driver and pixmap surface ..
    }

    @Test
    public void test01_flip1_aa0_bitmap_layered() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS == Platform.getOSType() ) {
            final GLCapabilities caps = new GLCapabilities(glp);
            caps.setBitmap(true);
            caps.setAlphaBits(8);
            runTestGL(caps, true, false);
        } // issues w/ AMD catalyst driver and pixmap surface ..
    }

    @Test
    public void test02_flip1_aa8() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(8);
        runTestGL(caps, false, false);
    }

    @Test
    public void test11_flip0_aa0() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, false, true);
    }

    @Test
    public void test11_flip0_aa0_layered() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(8);
        runTestGL(caps, true, true);
    }

    @Test
    public void test11_flip0_aa0_bitmap() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS == Platform.getOSType() ) {
            final GLCapabilities caps = new GLCapabilities(glp);
            caps.setBitmap(true);
            runTestGL(caps, false, true);
        } // issues w/ AMD catalyst driver and pixmap surface ..
    }

    @Test
    public void test11_flip0_aa0_bitmap_layered() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS == Platform.getOSType() ) {
            final GLCapabilities caps = new GLCapabilities(glp);
            caps.setBitmap(true);
            caps.setAlphaBits(8);
            runTestGL(caps, true, true);
        } // issues w/ AMD catalyst driver and pixmap surface ..
    }

    @Test
    public void test12_flip0_aa8() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(8);
        runTestGL(caps, false, true);
    }

    static long duration = 500; // ms

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-width")) {
                i++;
                width = MiscUtils.atoi(args[i], width);
            } else if(args[i].equals("-height")) {
                i++;
                height = MiscUtils.atoi(args[i], height);
            } else if(args[i].equals("-600dpi")) {
                allow600dpi = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            }
        }
        if(waitForKey) {
            final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (final IOException e) { }
        }
        org.junit.runner.JUnitCore.main(TestTiledPrintingGearsSwingAWT.class.getName());
    }
}
