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
package jogamp.opengl.awt;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import com.jogamp.opengl.util.TileRenderer;
import com.jogamp.opengl.util.TileRendererBase;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.awt.AWTGLPixelBuffer;
import com.jogamp.opengl.util.awt.AWTGLPixelBuffer.AWTGLPixelBufferProvider;

/**
 * Implementing AWT {@link Graphics2D} based {@link TileRenderer} <i>painter</i>.
 * <p>
 * Maybe utilized for AWT printing.
 * </p>
 */
public class AWTTilePainter {
    public final TileRenderer renderer;
    public final int componentCount;
    public final double scaleMatX, scaleMatY;
    public final boolean verbose;
    
    private AWTGLPixelBuffer tBuffer = null;
    private BufferedImage vFlipImage = null;
    private Graphics2D g2d = null;
    private AffineTransform saveAT = null;    
    
    /** 
     * Assumes a configured {@link TileRenderer}, i.e.
     * an {@link TileRenderer#attachToAutoDrawable(GLAutoDrawable) attached}
     * {@link GLAutoDrawable} with {@link TileRenderer#setTileSize(int, int, int) set tile size}.
     * <p> 
     * Sets the renderer to {@link TileRenderer#TR_TOP_TO_BOTTOM} row order.
     * </p>
     * <p>
     * <code>componentCount</code> reflects opaque, i.e. 4 if non opaque.
     * </p>
     * @param renderer
     * @param componentCount
     * @param scaleMatX {@link Graphics2D} {@link Graphics2D#scale(double, double) scaling factor}, i.e. rendering 1/scaleMatX * width pixels
     * @param scaleMatY {@link Graphics2D} {@link Graphics2D#scale(double, double) scaling factor}, i.e. rendering 1/scaleMatY * height pixels
     * @param verbose
     */
    public AWTTilePainter(TileRenderer renderer, int componentCount, double scaleMatX, double scaleMatY, boolean verbose) {
        this.renderer = renderer;
        this.renderer.setGLEventListener(preTileGLEL, postTileGLEL);
        this.componentCount = componentCount;
        this.scaleMatX = scaleMatX;
        this.scaleMatY = scaleMatY;
        this.verbose = verbose;
        this.renderer.setRowOrder(TileRenderer.TR_TOP_TO_BOTTOM);
    }
    
    public String toString() { return renderer.toString(); }
    
    /**
     * Caches the {@link Graphics2D} instance for rendering.
     * <p>
     * Copies the current {@link Graphics2D} {@link AffineTransform}
     * and scales {@link Graphics2D} w/ <code>scaleMatX</code> x <code>scaleMatY</code>.<br>
     * After rendering, the {@link AffineTransform} should be reset via {@link #resetGraphics2D()}.
     * </p>
     * <p>
     * Sets the {@link TileRenderer}'s {@link TileRenderer#setImageSize(int, int) image size}
     * and {@link TileRenderer#setTileOffset(int, int) tile offset} according the
     * the {@link Graphics2D#getClipBounds() graphics clip bounds}.
     * </p>
     * @param g2d
     */
    public void setupGraphics2DAndClipBounds(Graphics2D g2d) {
        this.g2d = g2d;
        saveAT = g2d.getTransform();
        g2d.scale(scaleMatX, scaleMatY);
        
        final Rectangle gClipOrig = g2d.getClipBounds();
        final Rectangle gClip = new Rectangle(gClipOrig);
        if( 0 > gClip.x ) {
            gClip.width += gClip.x;
            gClip.x = 0;
        }
        if( 0 > gClip.y ) {
            gClip.height += gClip.y;
            gClip.y = 0;
        }
        if( verbose ) {
            System.err.println("AWT print.0: "+gClipOrig+" -> "+gClip);
        }
        renderer.setImageSize(gClip.width, gClip.height);
        renderer.setTileOffset(gClip.x, gClip.y);
    }
    
    /** See {@ #setupGraphics2DAndClipBounds(Graphics2D)}. */
    public void resetGraphics2D() {        
        g2d.setTransform(saveAT);
    }
    
    /**
     * Disposes resources and {@link TileRenderer#detachFromAutoDrawable() detaches}
     * the {@link TileRenderer}'s {@link GLAutoDrawable}.
     */
    public void dispose() {
        renderer.detachFromAutoDrawable(); // tile-renderer -> printGLAD
        g2d = null;
        if( null != tBuffer ) {
            tBuffer.dispose();
            tBuffer = null;
        }
        if( null != vFlipImage ) {
            vFlipImage.flush();
            vFlipImage = null;
        }
    }
    
    final GLEventListener preTileGLEL = new GLEventListener() {
        @Override
        public void init(GLAutoDrawable drawable) {
        }
        @Override
        public void dispose(GLAutoDrawable drawable) {}
        @Override
        public void display(GLAutoDrawable drawable) {
            final GL gl = drawable.getGL();
            if( null == tBuffer ) {
                final int tWidth = renderer.getParam(TileRenderer.TR_TILE_WIDTH);
                final int tHeight = renderer.getParam(TileRenderer.TR_TILE_HEIGHT);
                final AWTGLPixelBufferProvider printBufferProvider = new AWTGLPixelBufferProvider( true /* allowRowStride */ );      
                final GLPixelAttributes pixelAttribs = printBufferProvider.getAttributes(gl, componentCount);
                tBuffer = printBufferProvider.allocate(gl, pixelAttribs, tWidth, tHeight, 1, true, 0);
                renderer.setTileBuffer(tBuffer);
                vFlipImage = new BufferedImage(tBuffer.width, tBuffer.height, tBuffer.image.getType());
            }
            if( verbose ) {
                System.err.println("XXX tile-pre "+renderer);
            }
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    };
    final GLEventListener postTileGLEL = new GLEventListener() {
        int tTopRowHeight = 0;
        @Override
        public void init(GLAutoDrawable drawable) {
            tTopRowHeight = 0;
        }
        @Override
        public void dispose(GLAutoDrawable drawable) {}
        @Override
        public void display(GLAutoDrawable drawable) {              
            // Copy temporary data into raster of BufferedImage for faster
            // blitting Note that we could avoid this copy in the cases
            // where !offscreenDrawable.isGLOriented(),
            // but that's the software rendering path which is very slow anyway.
            final int tWidth = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_WIDTH);
            final int tHeight = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_HEIGHT);
            // final BufferedImage dstImage = printBuffer.image;
            final BufferedImage srcImage = tBuffer.image;
            final BufferedImage dstImage = vFlipImage;
            final int[] src = ((DataBufferInt) srcImage.getRaster().getDataBuffer()).getData();
            final int[] dst = ((DataBufferInt) dstImage.getRaster().getDataBuffer()).getData();
            final int incr = tBuffer.width;
            int srcPos = 0;
            int destPos = (tHeight - 1) * tBuffer.width;
            for (; destPos >= 0; srcPos += incr, destPos -= incr) {
                System.arraycopy(src, srcPos, dst, destPos, incr);
            }
            // Draw resulting image in one shot
            final int tRows = renderer.getParam(TileRenderer.TR_ROWS);
            final int tRow = renderer.getParam(TileRenderer.TR_CURRENT_ROW);
            final int pX = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_X_POS); 
            final int pYf;
            if( tRow == tRows - 1 ) {
                tTopRowHeight = tHeight;
                pYf = 0;
            } else if( tRow == tRows - 2 ){
                pYf = tTopRowHeight;
            } else {
                pYf = ( tRows - 2 - tRow ) * tHeight + tTopRowHeight;
            }
            final Shape oClip = g2d.getClip();
            g2d.clipRect(pX, pYf, tWidth, tHeight);
            g2d.drawImage(dstImage, pX, pYf, dstImage.getWidth(), dstImage.getHeight(), null); // Null ImageObserver since image data is ready.
            if( verbose ) {
                System.err.println("XXX tile-post.X clip "+oClip+" -> "+g2d.getClip());
                g2d.setColor(Color.BLACK);
                g2d.drawRect(pX, pYf, tWidth, tHeight);
                final Rectangle r = oClip.getBounds();
                g2d.setColor(Color.YELLOW);
                g2d.drawRect(r.x, r.y, r.width, r.height);
                System.err.println("XXX tile-post.X "+renderer);
                System.err.println("XXX tile-post.X dst-img "+dstImage.getWidth()+"x"+dstImage.getHeight()+" -> "+pX+"/"+pYf);
            }
            g2d.setClip(oClip);
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    };
}
