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

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;

import org.junit.Assert;

import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.awt.AWTPrintLifecycle;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Base unit test class implementing {@link Printable}.
 * 
 * <h5>Scaling of Frame and GL content</h5>
 * <p>
 * We fit the frame into the imageable area with for 72 dpi,
 * assuming that is the default AWT painting density.
 * </p>
 * <p>
 * The frame borders are considered.
 * </p>
 * <p>
 * The frame's scale factor is used for the graphics print matrix
 * of the overall print-job, hence no frame resize is required.
 * </p>
 * <p>
 * The GL scale factor 'scaleGLMatXY', 72dpi/glDPI, is passed to the GL object
 * which locally scales the print matrix and renders the scene with 1/scaleGLMatXY pixels.
 * </p>
 * <h5>Virtual printer driver</h5>
 * <p>
 * Note, on OSX you might need to setup a dummy printer, i.e. <i>print to file</i>.<br>
 * As root:
 * <pre>
   cupsctl FileDevice=Yes
   killall -HUP cupsd
   mkdir /data/lp
   chown USER /data/lp
   chmod ugo+rwx /data/lp
   lpadmin -p lprint -E -v file:/data/lp/out.ps -P /Library/Printers/PPDs/Contents/Resources/HP\ LaserJet\ 4\ Plus.gz
 * </pre>
 */
public abstract class TiledPrintingAWTBase extends UITestCase implements Printable {

    public static final double MM_PER_INCH = 25.4;
    /**
    public static final double A0_WIDTH_MM = 841.0;
    public static final double A0_HEIGHT_MM = 1189.0;
    public static final double A0_WIDTH_INCH = A0_WIDTH_MM / MM_PER_INCH;
    public static final double A0_HEIGHT_INCH = A0_WIDTH_MM / MM_PER_INCH; */
    
    /** Helper to pass desired Frame to print! **/    
    private Container printContainer;
    
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
             * See: 'Scaling of Frame and GL content' in Class description!
             */                
            final Insets frameInsets = printContainer.getInsets();
            final int frameWidth = printContainer.getWidth();
            final int frameHeight= printContainer.getHeight();
            final double scaleComp72;
            {
                final int frameBorderW = frameInsets.left + frameInsets.right;
                final int frameBorderH = frameInsets.top + frameInsets.bottom;
                final double sx = pf.getImageableWidth() / ( frameWidth + frameBorderW ); 
                final double sy = pf.getImageableHeight() / ( frameHeight + frameBorderH );
                scaleComp72 = Math.min(sx, sy);
            }
            
            System.err.println("PRINT thread "+Thread.currentThread().getName());
            System.err.println("PRINT DPI: scaleComp72 "+scaleComp72+
                               ", frame: border "+frameInsets+", size "+frameWidth+"x"+frameHeight);
            final Graphics2D g2d = (Graphics2D)g;
            System.err.println("PRINT at.pre: "+g2d.getTransform());
            g2d.translate(pf.getImageableX(), pf.getImageableY());
            g2d.scale(scaleComp72, scaleComp72);
            System.err.println("PRINT at.post: "+g2d.getTransform());

            AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                public void run() {
                    printContainer.printAll(g2d);
               }
            });
            
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

    /**
     * 
     * @param cont
     * @param pOrientation
     * @param paper
     * @param dpi
     * @param numSamples multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples 
     */
    public void doPrintAuto(Container cont, int pOrientation, Paper paper, int dpi, int numSamples) {
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
                final String fname = getPrintFilename(dpi, numSamples, "pdf");
                System.err.println("doPrint: dpi "+dpi+", "+fname);
                FileOutputStream outstream;
                try {
                    outstream = new FileOutputStream(fname);
                    Assert.assertTrue(doPrintAutoImpl(cont, pj, factories[0].getPrintService(outstream), pOrientation, paper, dpi, numSamples));
                } catch (FileNotFoundException e) {
                    Assert.assertNull("Unexpected exception", e);
                }
                return;
            }        
            System.err.println("No PDF");
            
            factories = PrinterJob.lookupStreamPrintServices(psMimeType);
            if (factories.length > 0) {
                final String fname = getPrintFilename(dpi, numSamples, "ps");
                System.err.println("doPrint: dpi "+dpi+", "+fname);
                FileOutputStream outstream;
                try {
                    outstream = new FileOutputStream(fname);
                    Assert.assertTrue(doPrintAutoImpl(cont, pj, factories[0].getPrintService(outstream), pOrientation, paper, dpi, numSamples));
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
    private String getPrintFilename(int dpi, int numSamples, String suffix) {
        final int maxSimpleTestNameLen = getMaxTestNameLen()+getClass().getSimpleName().length()+1;
        final String simpleTestName = getSimpleTestName(".");
        return String.format("%-"+maxSimpleTestNameLen+"s-n%04d-dpi%03d-aa%d.%s", simpleTestName, printCount, dpi, numSamples, suffix).replace(' ', '_');
    }
    private boolean doPrintAutoImpl(Container cont, PrinterJob job, 
                                    StreamPrintService ps, int pOrientation, Paper paper, int dpi,
                                    int numSamples) {
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
            job.setPrintable(this, pageFormat);
            doPrintImpl(cont, job, dpi, numSamples);
        } catch (PrinterException pe) {
            pe.printStackTrace();
            ok = false;
        }        
        return ok;
    }

    /**
     * 
     * @param cont
     * @param dpi
     * @param numSamples multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples 
     */
    public void doPrintManual(Container cont, int dpi, int numSamples) {
        lock.lock();
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(this);
            boolean ok = job.printDialog();
            if (ok) {
               doPrintImpl(cont, job, dpi, numSamples);
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 
     * @param cont
     * @param job
     * @param dpi
     * @param numSamples multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples 
     */
    private void doPrintImpl(final Container cont, final PrinterJob job, final int dpi, final int numSamples) {
       printContainer = cont;
       final double scaleGLMatXY = 72.0 / dpi;
       System.err.println("PRINT DPI: "+dpi+", AA "+numSamples+", scaleGL "+scaleGLMatXY);
       final AWTPrintLifecycle.Context ctx = AWTPrintLifecycle.Context.setupPrint(printContainer, scaleGLMatXY, scaleGLMatXY, numSamples);
       System.err.println("PRINT AWTPrintLifecycle.setup.count "+ctx.getCount());
       try {
           AWTEDTExecutor.singleton.invoke(true, new Runnable() {
            public void run() {
                try {
                    job.print();
                } catch (PrinterException ex) {
                    ex.printStackTrace();
                }
           } });
       } finally {
           ctx.releasePrint();
           System.err.println("PRINT AWTPrintLifecycle.release.count "+ctx.getCount());
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