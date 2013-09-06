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
import com.jogamp.opengl.test.junit.util.UITestCase;

public abstract class TiledPrintingAWTBase extends UITestCase implements Printable {

    final double mmPerInch = 25.4;
    final double a0WidthMM = 841.0;
    final double a0HeightMM = 1189.0;
    final double a0WidthInch = a0WidthMM / mmPerInch;
    final double a0HeightInch = a0WidthMM / mmPerInch;
    
    /** Helper to pass desired AWTPrintLifecycle ! **/
    private AWTPrintLifecycle awtPrintObject = null;    
    /** Helper to pass desired Frame to print! **/    
    private Frame frame;
    /** Helper to pass desired on- and offscreen mode ! **/
    private boolean printOffscreen = false;
    /** Helper to pass desired DPI value ! **/
    private int printDPI = 72;

    @Override
    public int print(Graphics g, PageFormat pf, int page)
            throws PrinterException {
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
            
                System.err.println("PRINT offscreen: "+printOffscreen+", thread "+Thread.currentThread().getName());
                System.err.println("PRINT DPI: "+printDPI+", scaleComp "+scaleComp);
                awtPrintObject.setupPrint();
                
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
                                
                    AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                       public void run() {
                           frame.setSize(frameWidthS, frameHeightS);
                           frame.invalidate();
                           frame.validate();
                       }
                    });
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
                    g2d.scale(scale , scale );
                }
                
                frame.printAll(g2d);
                awtPrintObject.releasePrint();
                
                if( scaleComp != 1 ) {
                    System.err.println("PRINT DPI: reset frame size "+frameWidth+"x"+frameHeight);
                    AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                       public void run() {
                           frame.setSize(frameWidth, frameHeight);
                           frame.invalidate();
                           frame.validate();
                       }
                    });
                }
                
                if( g2d == offscreenG2D ) {
                    printG2D.translate(moveX, moveY);
                    printG2D.scale(scale , scale );
                    printG2D.drawImage(offscreenImage, 0, 0, offscreenImage.getWidth(), offscreenImage.getHeight(), null); // Null ImageObserver since image data is ready.
                }
            
                /* tell the caller that this page is part of the printed document */
                return PAGE_EXISTS;
            }

    private int printCount = 0;

    public TiledPrintingAWTBase() {
        super();
    }

    protected void doPrintAuto(Frame frame, AWTPrintLifecycle awtPrintObject, 
                               Printable printable, int pOrientation, Paper paper, boolean offscreen, int dpi) {
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
                Assert.assertTrue(doPrintAutoImpl(frame, awtPrintObject, printable, pj, factories[0].getPrintService(outstream), pOrientation, paper, offscreen, dpi));
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
                Assert.assertTrue(doPrintAutoImpl(frame, awtPrintObject, printable, pj, factories[0].getPrintService(outstream), pOrientation, paper, offscreen, dpi));
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

    private boolean doPrintAutoImpl(Frame frame, AWTPrintLifecycle awtPrintObject, 
                                    Printable printable, PrinterJob job, StreamPrintService ps, int pOrientation,
                                    Paper paper, boolean offscreen, int dpi) {
        this.awtPrintObject = awtPrintObject; 
        this.frame = frame;
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
            job.setPrintable(printable, pageFormat);
            job.print();
        } catch (PrinterException pe) {
            pe.printStackTrace();
            ok = false;
        }        
        return ok;
    }

    protected void doPrintManual(Frame frame, AWTPrintLifecycle awtPrintObject, Printable printable, boolean offscreen, int dpi) {
        this.awtPrintObject = awtPrintObject; 
        this.frame = frame;
        printOffscreen = offscreen;
        printDPI = dpi;
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
    }

}