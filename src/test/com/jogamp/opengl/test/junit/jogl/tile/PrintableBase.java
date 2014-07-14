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
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.opengl.util.TileRenderer;

/**
 * Base {@link Printable} implementation class.
 *
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
public abstract class PrintableBase implements Printable {

    public static final double MM_PER_INCH = 25.4;

    public final PrinterJob job;
    public final Container cont;
    public final int dpi;
    public final int numSamples;
    public final int tileWidth, tileHeight;
    protected final RecursiveLock lockPrinting = LockFactory.createRecursiveLock();

    /**
     *
     * @param job
     * @param printContainer
     * @param printDPI
     * @param numSamples multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples
     * @param tileWidth custom tile width for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     * @param tileHeight custom tile height for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     */
    public PrintableBase(final PrinterJob job, final Container printContainer, final int printDPI, final int numSamples, final int tileWidth, final int tileHeight) {
        this.job = job;
        this.cont = printContainer;
        this.dpi = printDPI;
        this.numSamples = numSamples;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    /** Wait for idle .. simply acquiring all locks and releasing them. */
    public void waitUntilIdle() {
        lockPrinting.lock();
        lockPrinting.unlock();
    }
}