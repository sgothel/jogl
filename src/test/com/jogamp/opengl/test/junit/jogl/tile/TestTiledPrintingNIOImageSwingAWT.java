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
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Label;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.ImageIO;
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

import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.nativewindow.awt.AWTPrintLifecycle;
import com.jogamp.nativewindow.awt.DirectDataBufferInt;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.texture.TextureIO;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTiledPrintingNIOImageSwingAWT extends UITestCase  {

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

    protected void printOffscreenToFile(final BufferedImage image, final Frame frame, final GLCapabilities caps, final int num, final String detail) {
        final Insets frameInsets = frame.getInsets();
        final int frameWidth = frame.getWidth();
        final int frameHeight= frame.getHeight();
        final int imageWidth = image.getWidth();
        final int imageHeight= image.getHeight();
        final double scaleComp72;
        // Note: Frame size contains the frame border (i.e. insets)!
        {
            final double sx = (double)imageWidth / frameWidth;
            final double sy = (double)imageHeight / frameHeight;
            scaleComp72 = Math.min(sx, sy);
        }
        System.err.println("PRINT DPI: scaleComp72 "+scaleComp72+", image-size "+imageWidth+"x"+imageHeight+", frame[border "+frameInsets+", size "+frameWidth+"x"+frameHeight+"]");

        System.err.println("XXX: image "+image);
        System.err.println("XXX: cm "+image.getColorModel());
        System.err.println("XXX: raster "+image.getRaster());
        System.err.println("XXX: dataBuffer "+image.getRaster().getDataBuffer());

        AWTEDTExecutor.singleton.invoke(true, new Runnable() {
            public void run() {
                final Graphics2D g2d = (Graphics2D) image.getGraphics();
                g2d.setClip(0, 0, image.getWidth(), image.getHeight());
                g2d.scale(scaleComp72, scaleComp72);
                // g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // frame.paintAll(g2d);
                final AWTPrintLifecycle.Context ctx = AWTPrintLifecycle.Context.setupPrint(frame, 1.0/scaleComp72, 1.0/scaleComp72, 0, -1, -1);
                try {
                    frame.printAll(g2d);
                } finally {
                    ctx.releasePrint();
                }
                // to file
                final String fname = getSnapshotFilename(num, detail, caps, image.getWidth(), image.getHeight(), false, TextureIO.PNG, null);
                System.err.println("XXX file "+fname);
                final File fout = new File(fname);
                try {
                    ImageIO.write(image, "png", fout);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            } });
    }

    protected void runTestGL(final GLCapabilities caps, final boolean layered) throws InterruptedException, InvocationTargetException {
        final int layerStepX = width/6, layerStepY = height/6;
        final Dimension glc_sz = new Dimension(layered ? width - 2*layerStepX : width/2, layered ? height - 2*layerStepY : height);
        final GLJPanel glJPanel1 = new GLJPanel(caps);
        Assert.assertNotNull(glJPanel1);
        glJPanel1.setMinimumSize(glc_sz);
        glJPanel1.setPreferredSize(glc_sz);
        if( layered ) {
            glJPanel1.setBounds(layerStepX/2, layerStepY/2, glc_sz.width, glc_sz.height);
        } else {
            glJPanel1.setBounds(0, 0, glc_sz.width, glc_sz.height);
        }
        glJPanel1.addGLEventListener(new Gears());

        final GLJPanel glJPanel2 = new GLJPanel(caps);
        Assert.assertNotNull(glJPanel2);
        glJPanel2.setMinimumSize(glc_sz);
        glJPanel2.setPreferredSize(glc_sz);
        if( layered ) {
            glJPanel2.setBounds(3*layerStepY, 2*layerStepY, glc_sz.width, glc_sz.height);
        } else {
            glJPanel2.setBounds(0, 0, glc_sz.width, glc_sz.height);
        }
        glJPanel2.addGLEventListener(new RedSquareES2());
        // glJPanel2.addGLEventListener(new Gears());

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

        final Button print72DPIButton = new Button("72dpi"); // dummy
        final Button print300DPIButton = new Button("300dpi"); // dummy
        final Button print600DPIButton = new Button("600dpi"); // dummy

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

        // paint offscreen: array 72dpi ARGB
        {
            final BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
            printOffscreenToFile(image, frame, caps, 0, "array_072dpi_argb");
        }
        // paint offscreen: NIO 72dpi ARGB
        {
            final BufferedImage image = DirectDataBufferInt.createBufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB, null /* location */, null /* properties */);
            printOffscreenToFile(image, frame, caps, 1, "newio_072dpi_argb");
        }
        // paint offscreen: NIO 150dpi ARGB
        {
            final int scale = (int) ( 150.0 / 72.0 + 0.5 );
            final BufferedImage image = DirectDataBufferInt.createBufferedImage(frame.getWidth()*scale, frame.getHeight()*scale, BufferedImage.TYPE_INT_ARGB, null /* location */, null /* properties */);
            printOffscreenToFile(image, frame, caps, 2, "newio_150dpi_argb");
        }
        // paint offscreen: NIO 150dpi ARGB_PRE
        {
            final int scale = (int) ( 150.0 / 72.0 + 0.5 );
            final BufferedImage image = DirectDataBufferInt.createBufferedImage(frame.getWidth()*scale, frame.getHeight()*scale, BufferedImage.TYPE_INT_ARGB_PRE, null /* location */, null /* properties */);
            printOffscreenToFile(image, frame, caps, 2, "newio_150dpi_argbp");
        }
        // paint offscreen: NIO 150dpi RGB
        {
            final int scale = (int) ( 150.0 / 72.0 + 0.5 );
            final BufferedImage image = DirectDataBufferInt.createBufferedImage(frame.getWidth()*scale, frame.getHeight()*scale, BufferedImage.TYPE_INT_RGB, null /* location */, null /* properties */);
            printOffscreenToFile(image, frame, caps, 2, "newio_150dpi_rgb");
        }
        // paint offscreen: NIO 150dpi BGR
        {
            final int scale = (int) ( 150.0 / 72.0 + 0.5 );
            final BufferedImage image = DirectDataBufferInt.createBufferedImage(frame.getWidth()*scale, frame.getHeight()*scale, BufferedImage.TYPE_INT_BGR, null /* location */, null /* properties */);
            printOffscreenToFile(image, frame, caps, 2, "newio_150dpi_bgr");
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glJPanel1);
        Assert.assertNotNull(glJPanel2);

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
    public void test01_Offscreen_aa0() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, false);
    }

    @Test
    public void test01_Offscreen_aa0_layered() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, true);
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
        org.junit.runner.JUnitCore.main(TestTiledPrintingNIOImageSwingAWT.class.getName());
    }
}
