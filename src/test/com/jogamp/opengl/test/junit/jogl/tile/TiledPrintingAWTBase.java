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

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.media.opengl.awt.AWTPrintLifecycle;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;

import org.junit.Assert;

import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.opengl.test.junit.util.UITestCase;

public abstract class TiledPrintingAWTBase extends UITestCase implements Printable {

    public static final double MM_PER_INCH = 25.4;
    /**
    public static final double A0_WIDTH_MM = 841.0;
    public static final double A0_HEIGHT_MM = 1189.0;
    public static final double A0_WIDTH_INCH = A0_WIDTH_MM / MM_PER_INCH;
    public static final double A0_HEIGHT_INCH = A0_WIDTH_MM / MM_PER_INCH; */
    
    /** Helper to pass desired AWTPrintLifecycle ! **/
    private AWTPrintLifecycle awtPrintObject = null;    
    /** Helper to pass desired Frame to print! **/    
    private Frame frame;
    /** Helper to pass desired on- and offscreen mode ! **/
    private boolean printOffscreen = false;
    /** Helper to pass desired DPI value ! **/
    private int printDPI = 72;
    /** Helper to pass desired AA hint ! **/
    private boolean printAA = false;
    
    private RecursiveLock lockPrinting = LockFactory.createRecursiveLock();

    @Override
    public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
        if (page > 0) { // We have only one page, and 'page' is zero-based
            return NO_SUCH_PAGE;
        }
        
        lockPrinting.lock();
        try {
            final Paper paper = pf.getPaper();
            final double paperWWidthInch = paper.getWidth() / 72.0;
            final double paperWHeightInch = paper.getHeight() / 72.0;
            final double paperIWidthInch = paper.getImageableWidth() / 72.0;
            final double paperIHeightInch = paper.getImageableHeight() / 72.0;
            final double paperWWidthMM = paperWWidthInch * MM_PER_INCH;
            final double paperWHeightMM = paperWHeightInch * MM_PER_INCH;
            final double paperIWidthMM = paperIWidthInch * MM_PER_INCH;
            final double paperIHeightMM = paperIHeightInch * MM_PER_INCH;
            
            final double pfWWidthInch = pf.getWidth() / 72.0; 
            final double pfWHeightInch = pf.getHeight() / 72.0;
            final double pfIWidthInch = pf.getImageableWidth() / 72.0; 
            final double pfIHeightInch = pf.getImageableHeight() / 72.0;
            final double pfWWidthMM = pfWWidthInch * MM_PER_INCH; 
            final double pfWHeightMM = pfWHeightInch * MM_PER_INCH;
            final double pfIWidthMM = pfIWidthInch * MM_PER_INCH; 
            final double pfIHeightMM = pfIHeightInch * MM_PER_INCH;
            
            System.err.println("PF: Paper whole size "+
                    Math.round(paperWWidthMM)+" x "+Math.round(paperWHeightMM)+" mm, "+
                    Math.round(paperWWidthInch)+" x "+Math.round(paperWHeightInch)+" inch");
            
            System.err.println("PF: Paper image size "+paper.getImageableX()+" / "+paper.getImageableY()+" "+
                    Math.round(paperIWidthMM)+" x "+Math.round(paperIHeightMM)+" mm, "+
                    Math.round(paperIWidthInch)+" x "+Math.round(paperIHeightInch)+" inch, "+
                    Math.round(paper.getImageableWidth())+"x"+Math.round(paper.getImageableHeight())+" 72dpi dots");
            
            System.err.println("PF: Page  whole size "+
                        Math.round(pfWWidthMM)+" x "+Math.round(pfWHeightMM)+" mm, "+
                        Math.round(pfWWidthInch)+" x "+Math.round(pfWHeightInch)+" inch");
            
            System.err.println("PF: Page  image size "+pf.getImageableX()+" / "+pf.getImageableY()+" "+
                    Math.round(pfIWidthMM)+" x "+Math.round(pfIHeightMM)+" mm, "+
                    Math.round(pfIWidthInch)+" x "+Math.round(pfIHeightInch)+" inch, "+
                    Math.round(pf.getImageableWidth())+"x"+Math.round(pf.getImageableHeight())+" 72dpi dots");
            
            System.err.println("PF: Page orientation "+pf.getOrientation());
            
            /**
             * We fit the frame into the imageable area with the desired DPI.
             * <p>
             * We assume AWT painting happens w/ 72 dpi!
             * </p> 
             * <p>
             * The frame borders are considered.
             * </p>
             */                
            final Insets frameInsets = frame.getInsets();
            final int frameWidth = frame.getWidth();
            final int frameHeight= frame.getHeight();
            final int frameWidthS;
            final int frameHeightS;
            final double scaleComp;
            {
                final int frameBorderW = frameInsets.left + frameInsets.right;
                final int frameBorderH = frameInsets.top + frameInsets.bottom;
                final double sx = pf.getImageableWidth() / ( frameWidth + frameBorderW ); 
                final double sy = pf.getImageableHeight() / ( frameHeight + frameBorderH );
                scaleComp = Math.min(sx, sy) * ( printDPI/72.0 );
                if( 0f < scaleComp ) {            
                    frameWidthS = (int) ( frameWidth*scaleComp ); // cut off FIXME
                    frameHeightS = (int) ( frameHeight*scaleComp ); // cut off FIXME
                } else {
                    frameWidthS = frameWidth;
                    frameHeightS = frameHeight;                        
                }
            }
            // Since we fit the frame size into the imageable size respecting the DPI,
            // we simply can scale the print graphics matrix to the DPI 
            // w/o the need to fiddle w/ the margins (matrix translation).
            final double scaleGraphics = 72.0 / printDPI;
            
            System.err.println("PRINT offscreen: "+printOffscreen+", thread "+Thread.currentThread().getName());
            System.err.println("PRINT DPI: "+printDPI+", AA "+printAA+", scaleGraphics "+scaleGraphics+", scaleComp "+scaleComp+
                               ", frame: border "+frameInsets+", size "+frameWidth+"x"+frameHeight+" -> "+frameWidthS+"x"+frameHeightS);
            
            final Graphics2D printG2D = (Graphics2D)g;
            final Graphics2D offscreenG2D;
            final BufferedImage offscreenImage;
            final Graphics2D g2d;
            if( printOffscreen ) {
                offscreenImage = new BufferedImage(frameWidthS, frameHeightS, BufferedImage.TYPE_4BYTE_ABGR);
                offscreenG2D = (Graphics2D) offscreenImage.getGraphics();
                offscreenG2D.setClip(0, 0, frameWidthS, frameHeightS);
                g2d = offscreenG2D;
            } else {
                offscreenG2D = null;
                offscreenImage = null;
                g2d = printG2D;
                
                g2d.translate(pf.getImageableX(), pf.getImageableY());
                g2d.scale(scaleGraphics, scaleGraphics);
            }
            if( printAA ) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
            awtPrintObject.setupPrint(g2d);
            try {
                AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                   public void run() {
                       frame.setSize(frameWidthS, frameHeightS);
                       frame.invalidate();
                       frame.validate();
                   }
                });
                                      
                AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                   public void run() {
                       frame.printAll(g2d);
                   } } );
                
                System.err.println("PRINT DPI: reset frame size "+frameWidth+"x"+frameHeight);
                AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                   public void run() {
                       frame.setSize(frameWidth, frameHeight);
                       frame.invalidate();
                       frame.validate();
                   }
                });
            } finally {
                awtPrintObject.releasePrint();
            }
            
            if( printOffscreen ) {
                printG2D.translate(pf.getImageableX(), pf.getImageableY());
                printG2D.scale(scaleGraphics, scaleGraphics);
                printG2D.drawImage(offscreenImage, 0, 0, offscreenImage.getWidth(), offscreenImage.getHeight(), null); // Null ImageObserver since image data is ready.
            }
            
            /* tell the caller that this page is part of the printed document */
            return PAGE_EXISTS;
        } finally {
            lockPrinting.unlock();
        }
    }

    private RecursiveLock lock = LockFactory.createRecursiveLock();
    private int printCount = 0;

    public TiledPrintingAWTBase() {
        super();
    }

    public void doPrintAuto(Frame frame, AWTPrintLifecycle awtPrintObject, 
                            Printable printable, int pOrientation, Paper paper, boolean offscreen, int dpi, boolean antialiasing) {
        lock.lock();
        try {
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
                final String fname = getPrintFilename(dpi, antialiasing, "pdf");
                System.err.println("doPrint: dpi "+dpi+", "+fname);
                FileOutputStream outstream;
                try {
                    outstream = new FileOutputStream(fname);
                    Assert.assertTrue(doPrintAutoImpl(frame, awtPrintObject, printable, pj, factories[0].getPrintService(outstream), pOrientation, paper, offscreen, dpi, antialiasing));
                } catch (FileNotFoundException e) {
                    Assert.assertNull("Unexpected exception", e);
                }
                return;
            }        
            System.err.println("No PDF");
            
            factories = PrinterJob.lookupStreamPrintServices(psMimeType);
            if (factories.length > 0) {
                final String fname = getPrintFilename(dpi, antialiasing, "ps");
                System.err.println("doPrint: dpi "+dpi+", "+fname);
                FileOutputStream outstream;
                try {
                    outstream = new FileOutputStream(fname);
                    Assert.assertTrue(doPrintAutoImpl(frame, awtPrintObject, printable, pj, factories[0].getPrintService(outstream), pOrientation, paper, offscreen, dpi, antialiasing));
                } catch (FileNotFoundException e) {
                    Assert.assertNull("Unexpected exception", e);
                }
                return;
            }        
            System.err.println("No PS");
        } finally {
            lock.unlock();
        }
    }
    private String getPrintFilename(int dpi, boolean antialiasing, String suffix) {
        final int maxSimpleTestNameLen = getMaxTestNameLen()+getClass().getSimpleName().length()+1;
        final String simpleTestName = getSimpleTestName(".");
        final String sAA = antialiasing ? "aa_" : "raw";
        return String.format("%-"+maxSimpleTestNameLen+"s-n%04d-dpi%03d-%s.%s", simpleTestName, printCount, dpi, sAA, suffix).replace(' ', '_');
    }
    private boolean doPrintAutoImpl(Frame frame, AWTPrintLifecycle awtPrintObject, 
                                    Printable printable, PrinterJob job, StreamPrintService ps, int pOrientation,
                                    Paper paper, boolean offscreen, int dpi, boolean antialiasing) {
        this.awtPrintObject = awtPrintObject; 
        this.frame = frame;
        printOffscreen = offscreen;
        printDPI = dpi;
        printAA = antialiasing;
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
            job.setPrintable(printable, pageFormat);
            job.print();
        } catch (PrinterException pe) {
            pe.printStackTrace();
            ok = false;
        }        
        return ok;
    }

    public void doPrintManual(Frame frame, AWTPrintLifecycle awtPrintObject, Printable printable, boolean offscreen, int dpi, boolean antialiasing) {
        lock.lock();
        try {
            this.awtPrintObject = awtPrintObject; 
            this.frame = frame;
            printOffscreen = offscreen;
            printDPI = dpi;
            printAA = antialiasing;
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(printable);
            boolean ok = job.printDialog();
            if (ok) {
                try {
                    job.print();
                } catch (PrinterException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** Wait for idle .. simply acquiring all locks and releasing them. */
    public void waitUntilPrintJobsIdle() {
        lock.lock();
        try {
            lockPrinting.lock();
            lockPrinting.unlock();
        } finally {
            lock.unlock();
        }
    }
}