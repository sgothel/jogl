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

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.jogamp.opengl.GLAutoDrawable;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;

import jogamp.nativewindow.awt.AWTMisc;

import org.junit.Assert;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.awt.AWTPrintLifecycle;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.TileRenderer;

/**
 * Base unit test class implementing
 * issuing {@link PrinterJob#print()} on a {@link Printable} implementation,
 * i.e. {@link OnscreenPrintable} or {@link OffscreenPrintable}.
 */
public abstract class TiledPrintingAWTBase extends UITestCase {

    private final RecursiveLock lock = LockFactory.createRecursiveLock();
    private int printCount = 0;

    public TiledPrintingAWTBase() {
        super();
    }

    /**
     *
     * @param cont
     * @param pOrientation
     * @param paper
     * @param offscrnImageType if < 0 onscreen, otherwise integer BufferedImage type
     * @param dpi
     * @param numSamples multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples
     * @param tileWidth custom tile width for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     * @param tileHeight custom tile height for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     * @param resizeWithinPrintTest TODO
     */
    public PrintableBase doPrintAuto(final Container cont, final int pOrientation, final Paper paper,
                                     final int offscrnImageType, final int dpi, final int numSamples, final int tileWidth, final int tileHeight, final boolean resizeWithinPrintTest) {
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
                final String fname = getPrintFilename(offscrnImageType, dpi, numSamples, tileWidth, tileHeight, "pdf", resizeWithinPrintTest);
                System.err.println("doPrint: dpi "+dpi+", "+fname);
                FileOutputStream outstream;
                try {
                    outstream = new FileOutputStream(fname);
                    return doPrintAutoImpl(cont, pj, factories[0].getPrintService(outstream), pOrientation, paper,
                            offscrnImageType, dpi, numSamples, tileWidth, tileHeight, resizeWithinPrintTest);
                } catch (final FileNotFoundException e) {
                    Assert.assertNull("Unexpected exception", e);
                }
            }
            System.err.println("No PDF");

            factories = PrinterJob.lookupStreamPrintServices(psMimeType);
            if (factories.length > 0) {
                final String fname = getPrintFilename(offscrnImageType, dpi, numSamples, tileWidth, tileHeight, "ps", resizeWithinPrintTest);
                System.err.println("doPrint: dpi "+dpi+", "+fname);
                FileOutputStream outstream;
                try {
                    outstream = new FileOutputStream(fname);
                    return doPrintAutoImpl(cont, pj, factories[0].getPrintService(outstream), pOrientation, paper, offscrnImageType, dpi, numSamples, tileWidth, tileHeight, resizeWithinPrintTest);
                } catch (final FileNotFoundException e) {
                    Assert.assertNull("Unexpected exception", e);
                }
            }
            System.err.println("No PS");
            return null;
        } finally {
            lock.unlock();
        }
    }
    private String getPrintFilename(final int offscrnImageType, final int dpi, final int numSamples, final int tileWidth, final int tileHeight, final String suffix, final boolean resizeWithinPrintTest) {
        final int maxSimpleTestNameLen = getMaxTestNameLen()+getClass().getSimpleName().length()+1;
        final String simpleTestName = getSimpleTestName(".");
        final String onoffscrn = 0 > offscrnImageType ? "on_screen" : "offscrn_"+offscrnImageType;
        final String aa = 0 <= numSamples ? "aa"+numSamples : "aaN";
        return String.format("%-"+maxSimpleTestNameLen+"s-n%04d-%s-dpi%03d-%s-tSz%04dx%04d-resize%d.%s",
                simpleTestName, printCount, onoffscrn, dpi, aa, tileWidth, tileHeight, resizeWithinPrintTest?1:0, suffix).replace(' ', '_');
    }
    private PrintableBase doPrintAutoImpl(final Container cont, final PrinterJob job,
                                          final StreamPrintService ps, final int pOrientation, final Paper paper,
                                          final int offscrnImageType, final int dpi, final int numSamples, final int tileWidth, final int tileHeight, final boolean resizeWithinPrintTest) {
        try {
            final PageFormat pageFormat = job.defaultPage();
            if( null != paper ) {
                /**
                Paper paper = new Paper();
                paper.setSize(500,500); // Large Address Dimension
                paper.setImageableArea(20, 20, 450, 420); */
                pageFormat.setPaper(paper);
            }
            pageFormat.setOrientation(pOrientation); // PageFormat.LANDSCAPE or PageFormat.PORTRAIT
            job.setPrintService(ps);
            final PrintableBase printable;
            if( 0 < offscrnImageType ) {
                printable = new OffscreenPrintable(job, cont, dpi, numSamples, tileWidth, tileHeight, offscrnImageType, getPrintFilename(offscrnImageType, dpi, numSamples, tileWidth, tileHeight, "png", resizeWithinPrintTest));
            } else {
                printable = new OnscreenPrintable(job, cont, dpi, numSamples, tileWidth, tileHeight);
            }
            printable.job.setPrintable(printable, pageFormat);
            doPrintImpl(printable, resizeWithinPrintTest);
            return printable;
        } catch (final PrinterException pe) {
            pe.printStackTrace();
            return null;
        }
    }

    /**
     * @param cont
     * @param dpi
     * @param numSamples multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples
     * @param tileWidth custom tile width for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     * @param tileHeight custom tile height for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     */
    public PrintableBase doPrintManual(final Container cont, final int dpi, final int numSamples, final int tileWidth, final int tileHeight) {
        lock.lock();
        try {
            final OnscreenPrintable printable = new OnscreenPrintable(PrinterJob.getPrinterJob(), cont, dpi, numSamples, tileWidth, tileHeight);
            printable.job.setPrintable(printable);
            final boolean ok = printable.job.printDialog();
            if (ok) {
               doPrintImpl(printable, false);
            }
            return printable;
        } finally {
            lock.unlock();
        }
    }

    private final AWTMisc.ComponentAction resizePlusAction = new AWTMisc.ComponentAction() {
        @Override
        public void run(final Component c) {
            final Rectangle r = c.getBounds();
            r.width += 64;
            r.height += 64;
            c.setBounds(r);
        } };
    private final AWTMisc.ComponentAction resizeMinusAction = new AWTMisc.ComponentAction() {
        @Override
        public void run(final Component c) {
            final Rectangle r = c.getBounds();
            r.width -= 64;
            r.height -= 64;
            c.setBounds(r);
        } };

    private void doPrintImpl(final PrintableBase printable, final boolean resizeWithinPrintTest) {
       final double scaleGLMatXY = 72.0 / printable.dpi;
       System.err.println("PRINTable: "+printable.getClass().getSimpleName());
       System.err.println("PRINT DPI: "+printable.dpi+", AA "+printable.numSamples+", scaleGL "+scaleGLMatXY);
       final AWTPrintLifecycle.Context ctx =
               AWTPrintLifecycle.Context.setupPrint(printable.cont, scaleGLMatXY, scaleGLMatXY,
                                                    printable.numSamples, printable.tileWidth, printable.tileHeight);
       System.err.println("PRINT AWTPrintLifecycle.setup.count "+ctx.getCount());
       final int w = printable.cont.getWidth();
       final int h = printable.cont.getHeight();
       final long t0 = Platform.currentTimeMillis();
       try {
           AWTEDTExecutor.singleton.invoke(true, new Runnable() {
            public void run() {
                try {
                    if( resizeWithinPrintTest ) {
                        System.err.println("PRINT resizeWithinPrint size+ "+(w+64)+"x"+(h+64));
                        AWTMisc.performAction(printable.cont, GLAutoDrawable.class, resizePlusAction);
                        printable.cont.validate();
                        if( printable.cont instanceof Window ) {
                            ((Window)printable.cont).pack();
                        }
                    }
                    printable.job.print();
                } catch (final PrinterException ex) {
                    ex.printStackTrace();
                }
           } });
       } finally {
           ctx.releasePrint();
           final long td = Platform.currentTimeMillis() - t0;
           System.err.println("PRINT Duration "+td+" ms");
           if( resizeWithinPrintTest ) {
               AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                public void run() {
                   System.err.println("PRINT resizeWithinPrint repaint");
                   printable.cont.repaint();
                   System.err.println("PRINT resizeWithinPrint size- "+w+"x"+h);
                   AWTMisc.performAction(printable.cont, GLAutoDrawable.class, resizeMinusAction);
                   printable.cont.validate();
                   if( printable.cont instanceof Window ) {
                       ((Window)printable.cont).pack();
                   }
               } });
           }
           System.err.println("PRINT AWTPrintLifecycle.release.count "+ctx.getCount());
       }
    }

    /** Wait for idle .. simply acquiring all locks and releasing them. */
    public void waitUntilPrintJobsIdle(final PrintableBase p) {
        lock.lock();
        try {
            if( null != p ) {
                p.waitUntilIdle();
            }
        } finally {
            lock.unlock();
        }
    }
}