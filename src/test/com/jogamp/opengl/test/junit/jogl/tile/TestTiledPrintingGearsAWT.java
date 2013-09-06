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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTiledPrintingGearsAWT extends UITestCase implements Printable {
    static GLProfile glp;
    static int width, height;
    static boolean waitForKey = false;

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
    
    final double mmPerInch = 25.4;
    final double a0WidthMM = 841.0;
    final double a0HeightMM = 1189.0;
    final double a0WidthInch = a0WidthMM / mmPerInch;
    final double a0HeightInch = a0WidthMM / mmPerInch;
    
    /** Helper to pass desired on- and offscreen mode ! **/
    boolean printOffscreen = false;
    /** Helper to pass desired DPI value ! **/
    int printDPI = 72;
    
    @Override
    public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
        if (page > 0) { // We have only one page, and 'page' is zero-based
            return NO_SUCH_PAGE;
        }
        
        final Paper paper = pf.getPaper();
        final double paperWWidthInch = paper.getWidth() / 72.0;
        final double paperWHeightInch = paper.getHeight() / 72.0;
        final double paperIWidthInch = paper.getImageableWidth() / 72.0;
        final double paperIHeightInch = paper.getImageableHeight() / 72.0;
        final double paperWWidthMM = paperWWidthInch * mmPerInch;
        final double paperWHeightMM = paperWHeightInch * mmPerInch;
        final double paperIWidthMM = paperIWidthInch * mmPerInch;
        final double paperIHeightMM = paperIHeightInch * mmPerInch;
        
        final double pfWWidthInch = pf.getWidth() / 72.0; 
        final double pfWHeightInch = pf.getHeight() / 72.0;
        final double pfIWidthInch = pf.getImageableWidth() / 72.0;; 
        final double pfIHeightInch = pf.getImageableHeight() / 72.0;
        final double pfWWidthMM = pfWWidthInch * mmPerInch; 
        final double pfWHeightMM = pfWHeightInch * mmPerInch;
        final double pfIWidthMM = pfIWidthInch * mmPerInch; 
        final double pfIHeightMM = pfIHeightInch * mmPerInch;
        
        System.err.println("PF: Paper whole size "+Math.round(paperWWidthMM)+" x "+Math.round(paperWHeightMM)+" mm, "+Math.round(paperWWidthInch)+" x "+Math.round(paperWHeightInch)+" inch");
        System.err.println("PF: Paper image size "+paper.getImageableX()+" / "+paper.getImageableY()+" "+Math.round(paperIWidthMM)+" x "+Math.round(paperIHeightMM)+" mm, "+Math.round(paperIWidthInch)+" x "+Math.round(paperIHeightInch)+" inch");
        System.err.println("PF: Page  whole size "+Math.round(pfWWidthMM)+" x "+Math.round(pfWHeightMM)+" mm, "+Math.round(pfWWidthInch)+" x "+Math.round(pfWHeightInch)+" inch");
        System.err.println("PF: Page  image size "+pf.getImageableX()+" / "+pf.getImageableY()+" "+Math.round(pfIWidthMM)+" x "+Math.round(pfIHeightMM)+" mm, "+Math.round(pfIWidthInch)+" x "+Math.round(pfIHeightInch)+" inch");
        System.err.println("PF: Page orientation "+pf.getOrientation());
        
        /**
         * User (0,0) is typically outside the imageable area, so we must
         * translate by the X and Y values in the PageFormat to avoid clipping
         */
        
        final int scaleComp;
        {
            final int xScaleComp = (int) Math.round(printDPI/72.0);
            final int yScaleComp = (int) Math.round(printDPI/72.0);
            scaleComp = Math.min(xScaleComp, yScaleComp);
        }
        final double scale;
        {
            final double xScale = 72.0/printDPI;
            final double yScale = 72.0/printDPI;
            scale = Math.min(xScale, yScale);
        }

        System.err.println("PRINT offscreen: "+printOffscreen);
        System.err.println("PRINT DPI: "+printDPI+", scaleComp "+scaleComp);
        glCanvas.setupPrint();
        
        final int frameWidth = frame.getWidth();
        final int frameHeight= frame.getHeight();
        
        final double moveX, moveY;
        
        if( scaleComp != 1 ) {            
            final int frameWidthS = frameWidth*(scaleComp-1);
            final int frameHeightS = frameHeight*(scaleComp-1);
            
            double xMargin = (pf.getImageableWidth() - frameWidthS*scale)/2;
            double yMargin = (pf.getImageableHeight() - frameHeightS*scale)/2;
            moveX = pf.getImageableX() + xMargin;
            moveY = pf.getImageableY() + yMargin;
            System.err.println("PRINT DPI: "+printDPI+", scale "+scale+", margin "+xMargin+"/"+yMargin+", move "+moveX+"/"+moveY+
                               ", frame: "+frameWidth+"x"+frameHeight+" -> "+frameWidthS+"x"+frameHeightS);
                        
            frame.setSize(frameWidthS, frameHeightS);
        } else {
            moveX = pf.getImageableX();
            moveY = pf.getImageableY();
            System.err.println("PRINT DPI: "+printDPI+", scale "+scale+", move "+moveX+"/"+moveY+
                               ", frame: "+frameWidth+"x"+frameHeight);
        }
        
        final Graphics2D printG2D = (Graphics2D)g;
        
        final Graphics2D offscreenG2D;
        final BufferedImage offscreenImage;
        if( printOffscreen ) {            
            final int w = frame.getWidth();
            final int h = frame.getHeight();
            offscreenImage = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
            offscreenG2D = (Graphics2D) offscreenImage.getGraphics();
            offscreenG2D.setClip(0, 0, w, h);
        } else {
            offscreenG2D = null;
            offscreenImage = null;
        }
        
        final Graphics2D g2d = null != offscreenG2D ? offscreenG2D : printG2D; 
              
        if( g2d != offscreenG2D ) {
            g2d.translate(moveX, moveY);
            if( scaleComp != 1 ) {
                g2d.scale(scale , scale );
            }
        }
        
        frame.printAll(g2d);
        glCanvas.releasePrint();
        
        if( scaleComp != 1 ) {
            System.err.println("PRINT DPI: reset frame size "+frameWidth+"x"+frameHeight);
            frame.setSize(frameWidth, frameHeight);
        }
        
        if( g2d == offscreenG2D ) {
            printG2D.translate(moveX, moveY);
            if( scaleComp != 1 ) {
                printG2D.scale(scale , scale );
            }
            printG2D.drawImage(offscreenImage, 0, 0, offscreenImage.getWidth(), offscreenImage.getHeight(), null); // Null ImageObserver since image data is ready.
        }

        /* tell the caller that this page is part of the printed document */
        return PAGE_EXISTS;
    }

    private Frame frame;
    private GLCanvas glCanvas;

    private int printCount = 0;    
    protected void doPrintAuto(int pOrientation, Paper paper, boolean offscreen, int dpi) {
        final PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(MediaSizeName.ISO_A1); // 594 × 841 mm
        aset.add(MediaSizeName.ISO_A2); // 420 × 594 mm
        aset.add(MediaSizeName.ISO_A3); // 297 × 420 mm
        aset.add(MediaSizeName.ISO_A4); // 210 × 297 mm
        
        printCount++;
        
        final String psMimeType = "application/postscript";
        final String pdfMimeType = "application/pdf";
        final PrinterJob pj = PrinterJob.getPrinterJob();

        StreamPrintServiceFactory[] factories = PrinterJob.lookupStreamPrintServices(pdfMimeType);
        if (factories.length > 0) {
            final String fname = getPrintFilename(dpi, "pdf");
            System.err.println("doPrint: dpi "+dpi+", "+fname);
            FileOutputStream outstream;
            try {
                outstream = new FileOutputStream(fname);
                Assert.assertTrue(doPrintAutoImpl(pj, factories[0].getPrintService(outstream), pOrientation, paper, offscreen, dpi));
            } catch (FileNotFoundException e) {
                Assert.assertNull("Unexpected exception", e);
            }
            return;
        }        
        System.err.println("No PDF");
        
        factories = PrinterJob.lookupStreamPrintServices(psMimeType);
        if (factories.length > 0) {
            final String fname = getPrintFilename(dpi, "ps");
            System.err.println("doPrint: dpi "+dpi+", "+fname);
            FileOutputStream outstream;
            try {
                outstream = new FileOutputStream(fname);
                Assert.assertTrue(doPrintAutoImpl(pj, factories[0].getPrintService(outstream), pOrientation, paper, offscreen, dpi));
            } catch (FileNotFoundException e) {
                Assert.assertNull("Unexpected exception", e);
            }
        }        
        System.err.println("No PS");
    }
    private String getPrintFilename(int dpi, String suffix) {
        final int maxSimpleTestNameLen = getMaxTestNameLen()+getClass().getSimpleName().length()+1;
        final String simpleTestName = getSimpleTestName(".");
        return String.format("%-"+maxSimpleTestNameLen+"s-n%04d-dpi%03d.%s", simpleTestName, printCount, dpi, suffix).replace(' ', '_');
    }
    private boolean doPrintAutoImpl(PrinterJob job, StreamPrintService ps, int pOrientation, Paper paper, boolean offscreen, int dpi) {
        printOffscreen = offscreen;
        printDPI = dpi;
        boolean ok = true;
        try {            
            PageFormat pageFormat = job.defaultPage();
            if( null != paper ) {
                /**
                Paper paper = new Paper();
                paper.setSize(500,500); // Large Address Dimension
                paper.setImageableArea(20, 20, 450, 420); */
                pageFormat.setPaper(paper);
            }
            pageFormat.setOrientation(pOrientation); // PageFormat.LANDSCAPE or PageFormat.PORTRAIT
            job.setPrintService(ps);
            job.setPrintable(TestTiledPrintingGearsAWT.this, pageFormat);
            job.print();
        } catch (PrinterException pe) {
            pe.printStackTrace();
            ok = false;
        }        
        return ok;
    }    
    protected void doPrintManual(int dpi) {
        printDPI = dpi;
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(TestTiledPrintingGearsAWT.this);
        boolean ok = job.printDialog();
        if (ok) {
            try {
                job.print();
            } catch (PrinterException ex) {
                ex.printStackTrace();
            }
        }        
    }
    
    protected void runTestGL(GLCapabilities caps, boolean offscreenPrinting) throws InterruptedException, InvocationTargetException {
        glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);        
        Dimension glc_sz = new Dimension(width, height);
        glCanvas.setMinimumSize(glc_sz);
        glCanvas.setPreferredSize(glc_sz);
        glCanvas.setSize(glc_sz);
        
        final Gears gears = new Gears();
        glCanvas.addGLEventListener(gears);
        
        final ActionListener print72DPIAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doPrintManual(72);
            } };
        final ActionListener print300DPIAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doPrintManual(300);
            } };
        final Button print72DPIButton = new Button("72dpi");
        print72DPIButton.addActionListener(print72DPIAction);
        final Button print300DPIButton = new Button("300dpi");
        print300DPIButton.addActionListener(print300DPIAction);
            
        frame = new Frame("Gears AWT Test");
        Assert.assertNotNull(frame);
        frame.setLayout(new BorderLayout());
        Panel printPanel = new Panel();
        printPanel.add(print72DPIButton);
        printPanel.add(print300DPIButton);
        Panel southPanel = new Panel();
        southPanel.add(new Label("South"));
        Panel eastPanel = new Panel();
        eastPanel.add(new Label("East"));
        Panel westPanel = new Panel();
        westPanel.add(new Label("West"));
        frame.add(printPanel, BorderLayout.NORTH);
        frame.add(glCanvas, BorderLayout.CENTER);
        frame.add(southPanel, BorderLayout.SOUTH);
        frame.add(eastPanel, BorderLayout.EAST);
        frame.add(westPanel, BorderLayout.WEST);
        frame.setTitle("Tiles AWT Print Test");
        
        Animator animator = new Animator(glCanvas);
        QuitAdapter quitAdapter = new QuitAdapter();

        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter)).addTo(glCanvas);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter)).addTo(frame);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.pack();
                frame.setVisible(true);
            }});
        animator.setUpdateFPSFrames(60, System.err);        
        animator.start();

        boolean dpi72Done = false;
        boolean dpi300Done = false;
        while(!quitAdapter.shouldQuit() && animator.isAnimating() && ( 0 == duration || animator.getTotalFPSDuration()<duration )) {
            Thread.sleep(100);
            if( !dpi72Done ) {
                dpi72Done = true;
                doPrintAuto(PageFormat.LANDSCAPE, null, offscreenPrinting, 72);
            } else if( !dpi300Done ) {
                dpi300Done = true;
                doPrintAuto(PageFormat.LANDSCAPE, null, offscreenPrinting, 300);
            }
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glCanvas);
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
                frame = null;
                _frame.remove(glCanvas);
                glCanvas = null;
                _frame.dispose();
            }});
    }

    @Test
    public void test01_Onscreen() throws InterruptedException, InvocationTargetException {
        GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, false);
    }
    @Test
    public void test02_Offscreen() throws InterruptedException, InvocationTargetException {
        GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, true);
    }

    static long duration = 500; // ms

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            }
        }
        if(waitForKey) {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (IOException e) { }
        }
        org.junit.runner.JUnitCore.main(TestTiledPrintingGearsAWT.class.getName());
    }
}
