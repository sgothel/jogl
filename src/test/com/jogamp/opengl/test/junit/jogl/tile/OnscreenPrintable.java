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
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.opengl.util.TileRenderer;

/**
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
 */
public class OnscreenPrintable extends PrintableBase implements Printable {

    /**
     *
     * @param job
     * @param printContainer
     * @param printDPI
     * @param numSamples multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples
     * @param tileWidth custom tile width for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     * @param tileHeight custom tile height for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     */
    public OnscreenPrintable(final PrinterJob job, final Container printContainer, final int printDPI, final int numSamples, final int tileWidth, final int tileHeight) {
        super(job, printContainer, printDPI, numSamples, tileWidth, tileHeight);
    }


    @Override
    public int print(final Graphics g, final PageFormat pf, final int page) throws PrinterException {
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
             * Note: Frame size contains the frame border (i.e. insets)!
             */
            final Insets frameInsets = cont.getInsets();
            final int frameWidth = cont.getWidth();
            final int frameHeight= cont.getHeight();
            final double scaleGraphics = dpi / 72.0;
            final int frameSWidth = (int) ( frameWidth * scaleGraphics );
            final int frameSHeight = (int) ( frameHeight * scaleGraphics );
            final double scaleComp72;
            {
                final double sx = pf.getImageableWidth() / frameWidth;
                final double sy = pf.getImageableHeight() / frameHeight;
                scaleComp72 = Math.min(sx, sy);
            }
            System.err.println("PRINT.onscrn thread "+Thread.currentThread().getName());
            System.err.println("PRINT.onscrn DPI: scaleGraphics "+scaleGraphics+", scaleComp72 "+scaleComp72);
            System.err.println("PRINT.onscrn DPI: frame: border "+frameInsets+", size "+frameWidth+"x"+frameHeight+
                    " -> scaled "+frameSWidth+ "x" + frameSHeight);

            final Graphics2D g2d = (Graphics2D)g;
            System.err.println("PRINT at.pre: "+g2d.getTransform());
            g2d.translate(pf.getImageableX(), pf.getImageableY());
            g2d.scale(scaleComp72, scaleComp72); // WARNING: Produces rounding artifacts due to diff scale-factor of AWT/GL comps !!!
            // g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                public void run() {
                    cont.printAll(g2d);
               }
            });

            /* tell the caller that this page is part of the printed document */
            return PAGE_EXISTS;
        } finally {
            lockPrinting.unlock();
        }
    }
}
