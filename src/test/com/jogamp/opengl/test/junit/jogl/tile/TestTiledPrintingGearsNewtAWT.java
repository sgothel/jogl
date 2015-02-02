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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTiledPrintingGearsNewtAWT extends TiledPrintingAWTBase  {

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

    protected void runTestGL(final GLCapabilities caps) throws InterruptedException, InvocationTargetException {
        final Dimension glc_sz = new Dimension(width/2, height);
        final GLWindow glad1 = GLWindow.create(caps);
        Assert.assertNotNull(glad1);
        final NewtCanvasAWT canvas1 = new NewtCanvasAWT(glad1);
        Assert.assertNotNull(canvas1);
        canvas1.setMinimumSize(glc_sz);
        canvas1.setPreferredSize(glc_sz);
        canvas1.setSize(glc_sz);
        glad1.addGLEventListener(new Gears());

        final GLWindow glad2 = GLWindow.create(caps);
        Assert.assertNotNull(glad2);
        final NewtCanvasAWT canvas2 = new NewtCanvasAWT(glad2);
        Assert.assertNotNull(canvas2);
        canvas2.setMinimumSize(glc_sz);
        canvas2.setPreferredSize(glc_sz);
        canvas2.setSize(glc_sz);
        glad2.addGLEventListener(new RedSquareES2());

        final Panel demoPanel = new Panel();
        demoPanel.add(canvas1);
        demoPanel.add(canvas2);

        final Frame frame = new Frame("Newt/AWT Print");
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

        frame.setLayout(new BorderLayout());
        final Panel printPanel = new Panel();
        printPanel.add(print72DPIButton);
        printPanel.add(print300DPIButton);
        printPanel.add(print600DPIButton);
        final Panel southPanel = new Panel();
        southPanel.add(new Label("South"));
        final Panel eastPanel = new Panel();
        eastPanel.add(new Label("East"));
        final Panel westPanel = new Panel();
        westPanel.add(new Label("West"));
        frame.add(printPanel, BorderLayout.NORTH);
        frame.add(demoPanel, BorderLayout.CENTER);
        frame.add(southPanel, BorderLayout.SOUTH);
        frame.add(eastPanel, BorderLayout.EAST);
        frame.add(westPanel, BorderLayout.WEST);
        frame.setTitle("Tiles Newt/AWT Print Test");

        final Animator animator = new Animator();
        animator.add(glad1);
        animator.add(glad2);

        final QuitAdapter quitAdapter = new QuitAdapter();
        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter), canvas1.getNEWTChild()).addTo(canvas1);
        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter), canvas2.getNEWTChild()).addTo(canvas2);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), canvas2.getNEWTChild()).addTo(frame);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.pack();
                frame.setVisible(true);
            }});
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(canvas1, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(canvas2, true));

        animator.setUpdateFPSFrames(60, System.err);
        animator.start();

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
        Assert.assertNotNull(canvas1);
        Assert.assertNotNull(canvas2);
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
        glad1.destroy();
        glad2.destroy();
    }

    @Test
    public void test01_aa0() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps);
    }

    @Test
    public void test02_aa8() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setSampleBuffers(true);
        caps.setNumSamples(8);
        runTestGL(caps);
    }

    static long duration = 500; // ms

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
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
        org.junit.runner.JUnitCore.main(TestTiledPrintingGearsNewtAWT.class.getName());
    }
}
