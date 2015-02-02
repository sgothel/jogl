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
package com.jogamp.nativewindow.awt;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PrinterJob;

import jogamp.nativewindow.awt.AWTMisc;

/**
 * Interface describing print lifecycle to support AWT printing,
 * e.g. on AWT {@link com.jogamp.opengl.GLAutoDrawable GLAutoDrawable}s.
 * <a name="impl"><h5>Implementations</h5></a>
 * <p>
 * Implementing {@link com.jogamp.opengl.GLAutoDrawable GLAutoDrawable} classes based on AWT
 * supporting {@link Component#print(Graphics)} shall implement this interface.
 * </p>
 * <a name="usage"><h5>Usage</h5></a>
 * <p>
 * Users attempting to print an AWT {@link Container} containing {@link AWTPrintLifecycle} elements
 * shall consider decorating the {@link Container#printAll(Graphics)} call with<br>
 * {@link #setupPrint(double, double, int, int, int) setupPrint(..)} and {@link #releasePrint()}
 * on all {@link AWTPrintLifecycle} elements in the {@link Container}.<br>
 * To minimize this burden, a user can use {@link Context#setupPrint(Container, double, double, int, int, int) Context.setupPrint(..)}:
 * <pre>
 *  Container cont;
 *  double scaleGLMatXY = 72.0/glDPI;
 *  int numSamples = 0; // leave multisampling as-is
 *  PrinterJob job;
 *  ...
    final AWTPrintLifecycle.Context ctx = AWTPrintLifecycle.Context.setupPrint(cont, scaleGLMatXY, scaleGLMatXY, numSamples);
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
    }
 *
 * </pre>
 * </p>
 */
public interface AWTPrintLifecycle {

    public static final int DEFAULT_PRINT_TILE_SIZE = 1024;


    /**
     * Shall be called before {@link PrinterJob#print()}.
     * <p>
     * See <a href="#usage">Usage</a>.
     * </p>
     * @param scaleMatX {@link Graphics2D} {@link Graphics2D#scale(double, double) scaling factor}, i.e. rendering 1/scaleMatX * width pixels
     * @param scaleMatY {@link Graphics2D} {@link Graphics2D#scale(double, double) scaling factor}, i.e. rendering 1/scaleMatY * height pixels
     * @param numSamples multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples
     * @param tileWidth custom tile width for {@link com.jogamp.opengl.util.TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     * @param tileHeight custom tile height for {@link com.jogamp.opengl.util.TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     * FIXME: Add border size !
     */
    void setupPrint(double scaleMatX, double scaleMatY, int numSamples, int tileWidth, int tileHeight);

    /**
     * Shall be called after {@link PrinterJob#print()}.
     * <p>
     * See <a href="#usage">Usage</a>.
     * </p>
     */
    void releasePrint();

    /**
     * Convenient {@link AWTPrintLifecycle} context simplifying calling {@link AWTPrintLifecycle#setupPrint(double, double, int, int, int) setupPrint(..)}
     * and {@link AWTPrintLifecycle#releasePrint()} on all {@link AWTPrintLifecycle} elements of a {@link Container}.
     * <p>
     * See <a href="#usage">Usage</a>.
     * </p>
     */
    public static class Context {
        /**
         * <p>
         * See <a href="#usage">Usage</a>.
         * </p>
         *
         * @param c container to be traversed through to perform {@link AWTPrintLifecycle#setupPrint(double, double, int, int, int) setupPrint(..)} on all {@link AWTPrintLifecycle} elements.
         * @param scaleMatX {@link Graphics2D} {@link Graphics2D#scale(double, double) scaling factor}, i.e. rendering 1/scaleMatX * width pixels
         * @param scaleMatY {@link Graphics2D} {@link Graphics2D#scale(double, double) scaling factor}, i.e. rendering 1/scaleMatY * height pixels
         * @param numSamples multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples
         * @param tileWidth custom tile width for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
         * @param tileHeight custom tile height for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
         * @return the context
         */
        public static Context setupPrint(final Container c, final double scaleMatX, final double scaleMatY, final int numSamples, final int tileWidth, final int tileHeight) {
            final Context t = new Context(c, scaleMatX, scaleMatY, numSamples, tileWidth, tileHeight);
            t.setupPrint(c);
            return t;
        }

        /**
         * <p>
         * See <a href="#usage">Usage</a>.
         * </p>
         */
        public void releasePrint() {
            count = AWTMisc.performAction(cont, AWTPrintLifecycle.class, releaseAction);
        }

        /**
         * @return count of performed actions of last {@link #setupPrint(Container, double, double, int, int, int) setupPrint(..)} or {@link #releasePrint()}.
         */
        public int getCount() { return count; }

        private final Container cont;
        private final double scaleMatX;
        private final double scaleMatY;
        private final int numSamples;
        private final int tileWidth;
        private final int tileHeight;
        private int count;

        private final AWTMisc.ComponentAction setupAction = new AWTMisc.ComponentAction() {
            @Override
            public void run(final Component c) {
                ((AWTPrintLifecycle)c).setupPrint(scaleMatX, scaleMatY, numSamples, tileWidth, tileHeight);
            } };
        private final AWTMisc.ComponentAction releaseAction = new AWTMisc.ComponentAction() {
            @Override
            public void run(final Component c) {
                ((AWTPrintLifecycle)c).releasePrint();
            } };

        private Context(final Container c, final double scaleMatX, final double scaleMatY, final int numSamples, final int tileWidth, final int tileHeight) {
            this.cont = c;
            this.scaleMatX = scaleMatX;
            this.scaleMatY = scaleMatY;
            this.numSamples = numSamples;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.count = 0;
        }
        private void setupPrint(final Container c) {
            count = AWTMisc.performAction(c, AWTPrintLifecycle.class, setupAction);
        }
    }
}
