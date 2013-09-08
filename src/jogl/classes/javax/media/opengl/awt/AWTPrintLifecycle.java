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
package javax.media.opengl.awt;

import javax.media.opengl.GLAutoDrawable;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;

import jogamp.nativewindow.awt.AWTMisc;

/**
 * Interface describing print lifecycle to support AWT printing
 * on AWT {@link GLAutoDrawable}s.
 * <a name="impl"><h5>Implementations</h5></a>
 * <p>
 * Implementing {@link GLAutoDrawable} classes based on AWT
 * supporting {@link Component#print(Graphics)} shall implement this interface.
 * </p>
 * <a name="usage"><h5>Usage</h5></a>
 * <p>
 * Users attempting to print an AWT {@link Container} containing {@link AWTPrintLifecycle} elements
 * shall consider decorating the {@link Container#printAll(Graphics)} call with<br>
 * {@link #setupPrint(Graphics2D, double, double) setupPrint(..)} and {@link #releasePrint()}
 * on all {@link AWTPrintLifecycle} elements in the {@link Container}.<br>
 * To minimize this burden, a user can use {@link Context#setupPrint(Container, Graphics2D, double, double) Context.setupPrint(..)}:
 * <pre>
 *  Graphics2D g2d;
 *  Frame frame;
 *  double scaleGLMatXY = 72.0/glDPI;
 *  ...
    final AWTPrintLifecycle.Context ctx = AWTPrintLifecycle.Context.setupPrint(frame, g2d, scaleGLMatXY, scaleGLMatXY);
    try {
       AWTEDTExecutor.singleton.invoke(true, new Runnable() {
            public void run() {
                frame.printAll(g2d);
           } });
    } finally {
       ctx.releasePrint();
    }
 * 
 * </pre>
 * </p>
 */
public interface AWTPrintLifecycle {

    public static final int DEFAULT_PRINT_TILE_SIZE = 512;
    
    
    /**
     * Shall be called before {@link Component#print(Graphics)}.
     * <p>
     * See <a href="#usage">Usage</a>.
     * </p>
     * @param g2d the {@link Graphics2D} instance, which will be used for printing.
     * @param scaleMatX {@link Graphics2D} {@link Graphics2D#scale(double, double) scaling factor}, i.e. rendering 1/scaleMatX * width pixels
     * @param scaleMatY {@link Graphics2D} {@link Graphics2D#scale(double, double) scaling factor}, i.e. rendering 1/scaleMatY * height pixels
     */
    void setupPrint(Graphics2D g2d, double scaleMatX, double scaleMatY);
    
    /**
     * Shall be called after very last {@link Component#print(Graphics)}.
     * <p>
     * See <a href="#usage">Usage</a>.
     * </p>
     */
    void releasePrint();

    /**
     * Convenient {@link AWTPrintLifecycle} context simplifying calling {@link AWTPrintLifecycle#setupPrint(Graphics2D, double, double) setupPrint(..)}
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
         * @param c container to be traversed through to perform {@link AWTPrintLifecycle#setupPrint(Graphics2D, double, double) setupPrint(..)} on all {@link AWTPrintLifecycle} elements.
         * @param g2d the {@link Graphics2D} instance, which will be used for printing.
         * @param scaleMatX {@link Graphics2D} {@link Graphics2D#scale(double, double) scaling factor}, i.e. rendering 1/scaleMatX * width pixels
         * @param scaleMatY {@link Graphics2D} {@link Graphics2D#scale(double, double) scaling factor}, i.e. rendering 1/scaleMatY * height pixels
         * @return the context
         */
        public static Context setupPrint(Container c, Graphics2D g2d, double scaleMatX, double scaleMatY) {
            final Context t = new Context(c, g2d, scaleMatX, scaleMatY);
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
         * @return count of performed actions of last {@link #setupPrint(Container, Graphics2D, double, double) setupPrint(..)} or {@link #releasePrint()}.
         */
        public int getCount() { return count; }
        
        private final Container cont;
        private final Graphics2D g2d;
        private final double scaleMatX;
        private final double scaleMatY;
        private int count;
        
        private final AWTMisc.ComponentAction setupAction = new AWTMisc.ComponentAction() {
            @Override
            public void run(Component c) {
                ((AWTPrintLifecycle)c).setupPrint(g2d, scaleMatX, scaleMatY);
            } };
        private final AWTMisc.ComponentAction releaseAction = new AWTMisc.ComponentAction() {
            @Override
            public void run(Component c) {
                ((AWTPrintLifecycle)c).releasePrint();
            } };

        private Context(Container c, Graphics2D g2d, double scaleMatX, double scaleMatY) {
            this.cont = c;
            this.g2d = g2d;
            this.scaleMatX = scaleMatX;
            this.scaleMatY = scaleMatY;
            this.count = 0;
        }
        private void setupPrint(Container c) {
            count = AWTMisc.performAction(c, AWTPrintLifecycle.class, setupAction);
        }
    }
}
