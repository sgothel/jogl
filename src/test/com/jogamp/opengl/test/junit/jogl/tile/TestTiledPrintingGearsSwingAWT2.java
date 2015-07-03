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
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTiledPrintingGearsSwingAWT2 extends TiledPrintingAWTBase  {

    static boolean waitForKey = false;
    static GLProfile glp;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2)) {
            glp = GLProfile.get(GLProfile.GL2);
            Assert.assertNotNull(glp);
            width  = 560; // 640;
            height = 420; // 480;
        } else {
            setTestSupported(false);
        }
        // Runtime.getRuntime().traceInstructions(true);
        // Runtime.getRuntime().traceMethodCalls(true);
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(final GLCapabilities caps, final boolean addLayout, final boolean layered, final boolean skipGLOrientationVerticalFlip, final boolean useAnim) throws InterruptedException, InvocationTargetException {
        final Dimension glc_sz = new Dimension(width, height);
        final GLJPanel glJPanel1 = new GLJPanel(caps);
        Assert.assertNotNull(glJPanel1);
        glJPanel1.setSkipGLOrientationVerticalFlip(skipGLOrientationVerticalFlip);
        glJPanel1.setMinimumSize(glc_sz);
        glJPanel1.setPreferredSize(glc_sz);
        glJPanel1.setBounds(0, 0, glc_sz.width, glc_sz.height);
        {
            final Gears demo = new Gears();
            demo.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
            glJPanel1.addGLEventListener(demo);
        }

        final JComponent tPanel, demoPanel;
        if( layered ) {
            glJPanel1.setOpaque(true);
            final JButton tb = new JButton("On Top");
            tb.setBounds(width/2, height/2, 200, 50);
            if( addLayout ) {
                tPanel = null;
                final Dimension lsz = new Dimension(width, height);
                demoPanel = new JLayeredPane();
                demoPanel.setMinimumSize(lsz);
                demoPanel.setPreferredSize(lsz);
                demoPanel.setBounds(0, 0, lsz.width, lsz.height);
                demoPanel.setBorder(BorderFactory.createTitledBorder("Layered Pane"));
                demoPanel.add(glJPanel1, JLayeredPane.DEFAULT_LAYER);
                demoPanel.add(tb, Integer.valueOf(2));
            } else {
                tPanel = new TransparentPanel();
                tPanel.setBounds(0, 0, width, height);
                tPanel.setLayout(null);
                tPanel.add(tb);
                demoPanel = glJPanel1;
            }
        } else {
            tPanel = null;
            if( addLayout ) {
                demoPanel = new JPanel();
                demoPanel.add(glJPanel1);
            } else {
                demoPanel = glJPanel1;
            }
        }

        final JFrame frame = new JFrame("Swing Print");
        Assert.assertNotNull(frame);

        final ActionListener print72DPIAction = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                doPrintManual(frame, 72, 0, -1, -1);
            } };
        final ActionListener print150DPIAction = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                doPrintManual(frame, 150, -1, -1, -1);
            } };
        final ActionListener print300DPIAction = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                doPrintManual(frame, 300, -1, -1, -1);
            } };
        final Button print72DPIButton = new Button("72dpi");
        print72DPIButton.addActionListener(print72DPIAction);
        final Button print150DPIButton = new Button("150dpi");
        print150DPIButton.addActionListener(print150DPIAction);
        final Button print300DPIButton = new Button("300dpi");
        print300DPIButton.addActionListener(print300DPIAction);

        final JPanel printPanel = new JPanel();
        printPanel.add(print72DPIButton);
        printPanel.add(print150DPIButton);
        printPanel.add(print300DPIButton);
        final JPanel southPanel = new JPanel();
        southPanel.add(new Label("South"));
        final JPanel eastPanel = new JPanel();
        eastPanel.add(new Label("East"));
        final JPanel westPanel = new JPanel();
        westPanel.add(new Label("West"));

        final Animator animator = useAnim ? new Animator() : null;
        if( null != animator ) {
            animator.add(glJPanel1);
        }
        final QuitAdapter quitAdapter = new QuitAdapter();
        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter), glJPanel1).addTo(glJPanel1);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glJPanel1).addTo(frame);

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    final Container fcont = frame.getContentPane();
                    if( addLayout ) {
                        fcont.setLayout(new BorderLayout());
                        fcont.add(printPanel, BorderLayout.NORTH);
                        fcont.add(demoPanel, BorderLayout.CENTER);
                        fcont.add(southPanel, BorderLayout.SOUTH);
                        fcont.add(eastPanel, BorderLayout.EAST);
                        fcont.add(westPanel, BorderLayout.WEST);
                        fcont.validate();
                        frame.pack();
                    } else {
                        frame.setSize(glc_sz);
                        fcont.setLayout(null);
                        if( null != tPanel ) {
                            fcont.add(tPanel);
                        }
                        fcont.add(demoPanel);
                    }
                    frame.setVisible(true);
                } } ) ;

        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glJPanel1, true));

        if( null != animator ) {
            animator.setUpdateFPSFrames(60, System.err);
            animator.start();
            Assert.assertEquals(true, animator.isAnimating());
        }

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        boolean printDone = false;
        while( !quitAdapter.shouldQuit() && ( 0 == duration || ( t1 - t0 ) < duration ) ) {
            Thread.sleep(200);
            if( !printDone ) {
                printDone = true;
                {
                    // No AA needed for 150 dpi and greater :)
                    final PrintableBase p = doPrintAuto(frame, PageFormat.PORTRAIT, null, -1 /* offscreen-type */, 150, -1, -1, -1, false);
                    waitUntilPrintJobsIdle(p);
                }
            }
            t1 = System.currentTimeMillis();
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glJPanel1);

        if( null != animator ) {
            animator.stop();
            Assert.assertEquals(false, animator.isAnimating());
        }
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
    public void test001_flip1_norm_layout0_layered0() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, false /* addLayout */, false /* layered */, false /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test002_flip1_norm_layout1_layered0() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, true /* addLayout */, false /* layered */, false /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test003_flip1_norm_layout0_layered1() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, false /* addLayout */, true /* layered */, false /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test004_flip1_norm_layout1_layered1() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, true /* addLayout */, true /* layered */, false /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test011_flip1_bitm_layout0_layered0() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS != Platform.getOSType() ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBitmap(true);
        runTestGL(caps, false /* addLayout */, false /* layered */, false /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test012_flip1_bitm_layout1_layered0() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS != Platform.getOSType() ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBitmap(true);
        runTestGL(caps, true /* addLayout */, false /* layered */, false /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test013_flip1_bitm_layout0_layered1() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS != Platform.getOSType() ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBitmap(true);
        runTestGL(caps, false /* addLayout */, true /* layered */, false /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test014_flip1_bitm_layout1_layered1() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS != Platform.getOSType() ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBitmap(true);
        runTestGL(caps, true /* addLayout */, true /* layered */, false /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test101_flip1_norm_layout0_layered0() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, false /* addLayout */, false /* layered */, true /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test102_flip1_norm_layout1_layered0() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, true /* addLayout */, false /* layered */, true /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test103_flip1_norm_layout0_layered1() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, false /* addLayout */, true /* layered */, true /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test104_flip1_norm_layout1_layered1() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, true /* addLayout */, true /* layered */, true /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test111_flip1_bitm_layout0_layered0() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS != Platform.getOSType() ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBitmap(true);
        runTestGL(caps, false /* addLayout */, false /* layered */, true /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test112_flip1_bitm_layout1_layered0() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS != Platform.getOSType() ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBitmap(true);
        runTestGL(caps, true /* addLayout */, false /* layered */, true /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test113_flip1_bitm_layout0_layered1() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS != Platform.getOSType() ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBitmap(true);
        runTestGL(caps, false /* addLayout */, true /* layered */, true /* skipGLOrientationVerticalFlip */, false /* useAnim */);
    }

    @Test
    public void test114_flip1_bitm_layout1_layered1() throws InterruptedException, InvocationTargetException {
        if( Platform.OSType.WINDOWS != Platform.getOSType() ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setBitmap(true);
        runTestGL(caps, true /* addLayout */, true /* layered */, true /* skipGLOrientationVerticalFlip */, false /* useAnim */);
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
        org.junit.runner.JUnitCore.main(TestTiledPrintingGearsSwingAWT2.class.getName());
    }
}
