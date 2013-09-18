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
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;

import jogamp.opengl.Debug;

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
    private static final boolean DEBUG_TILES = Debug.debug("TileRenderer.PNG");
    
    public final TileRenderer renderer;
    public final int componentCount;
    public final double scaleMatX, scaleMatY;
    public final boolean verbose;
    
    public boolean flipVertical;
    private AWTGLPixelBuffer tBuffer = null;
    private BufferedImage vFlipImage = null;
    private Graphics2D g2d = null;
    private AffineTransform saveAT = null;    
    
    public static void dumpHintsAndScale(Graphics2D g2d) {
          final RenderingHints rHints = g2d.getRenderingHints();
          final Set<Entry<Object, Object>> rEntries = rHints.entrySet();
          int count = 0;
          for(Iterator<Entry<Object, Object>> rEntryIter = rEntries.iterator(); rEntryIter.hasNext(); count++) {
              final Entry<Object, Object> rEntry = rEntryIter.next();
              System.err.println("Hint["+count+"]: "+rEntry.getKey()+" -> "+rEntry.getValue());
          }
          final AffineTransform aTrans = g2d.getTransform();
          System.err.println(" type "+aTrans.getType());
          System.err.println(" scale "+aTrans.getScaleX()+" x "+aTrans.getScaleY());
          System.err.println(" move "+aTrans.getTranslateX()+" x "+aTrans.getTranslateY());        
          System.err.println(" mat  "+aTrans);
    }
    
    /**
     * @param numSamples multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples 
     * @param caps used capabilties
     * @return resulting number of samples, 0 if disabled
     */
    public static int getNumSamples(int numSamples, GLCapabilitiesImmutable caps) {
          if( 0 > numSamples ) {
              return 0;
          } else if( 0 < numSamples ) {
              if ( !caps.getGLProfile().isGL2ES3() ) {
                  return 0;
              }
              return Math.max(caps.getNumSamples(), numSamples);
          } else {
              return caps.getNumSamples();
          }
    }
    
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
        this.flipVertical = true;
    }
    
    public String toString() { return renderer.toString(); }
    
    public void setIsGLOriented(boolean v) {
        flipVertical = v;
    }
    
    private static Rectangle getRoundedRect(Rectangle2D r) {
        if( null == r ) { return null; }
        return new Rectangle((int)Math.round(r.getX()), (int)Math.round(r.getY()), 
                             (int)Math.round(r.getWidth()), (int)Math.round(r.getHeight()));
    }
    private static Rectangle clipNegative(Rectangle in) {
        if( null == in ) { return null; }
        final Rectangle out = new Rectangle(in);
        if( 0 > out.x ) {
            out.width += out.x;
            out.x = 0;
        }
        if( 0 > out.y ) {
            out.height += out.y;
            out.y = 0;
        }
        return out;
    }
    
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
     * @param g2d Graphics2D instance used for transform and clipping
     * @param width width of the AWT component in case clipping is null
     * @param height height of the AWT component in case clipping is null
     */
    public void setupGraphics2DAndClipBounds(Graphics2D g2d, int width, int height) {
        this.g2d = g2d;
        saveAT = g2d.getTransform();
        final Rectangle gClipOrigR;
        final Rectangle2D dClipOrig, dImageSizeOrig; // double precision for scaling
        // setup original rectangles
        {
            gClipOrigR = g2d.getClipBounds();
            final Rectangle gClipOrig = clipNegative(gClipOrigR);
            dClipOrig = null != gClipOrig ? new Rectangle2D.Double(gClipOrig.getX(), gClipOrig.getY(), gClipOrig.getWidth(), gClipOrig.getHeight()) : null;
            dImageSizeOrig = new Rectangle2D.Double(0, 0, width, height);
        }
        final Rectangle2D dClipScaled, dImageSizeScaled; // double precision for scaling
        // retrieve scaled image-size and clip-bounds
        {
            g2d.setClip(dImageSizeOrig);
            g2d.scale(scaleMatX, scaleMatY);
            dImageSizeScaled = (Rectangle2D) g2d.getClip();
            if( null == dClipOrig ) {
                g2d.setClip(null);
                dClipScaled = (Rectangle2D) dImageSizeScaled.clone();
            } else {
                g2d.setTransform(saveAT); // reset
                g2d.setClip(dClipOrig);
                g2d.scale(scaleMatX, scaleMatY);
                dClipScaled = (Rectangle2D) g2d.getClip();
            }
        }
        final Rectangle iClipScaled = getRoundedRect(dClipScaled);
        final Rectangle iImageSizeScaled = getRoundedRect(dImageSizeScaled);
        scaledYOffset = iClipScaled.y;
        renderer.setImageSize(iImageSizeScaled.width, iImageSizeScaled.height);
        renderer.clipImageSize(iClipScaled.width, iClipScaled.height);
        final int clipH = Math.min(iImageSizeScaled.height, iClipScaled.height);
        renderer.setTileOffset(iClipScaled.x, iImageSizeScaled.height - ( iClipScaled.y + clipH ));
        if( verbose ) {
            System.err.println("AWT print.0: image "+dImageSizeOrig + " -> " + dImageSizeScaled + " -> " + iImageSizeScaled);
            System.err.println("AWT print.0: clip  "+gClipOrigR + " -> " + dClipOrig + " -> " + dClipScaled + " -> " + iClipScaled);
            System.err.println("AWT print.0: "+renderer);
        }
    }
    private int scaledYOffset;
    
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
        public void init(GLAutoDrawable drawable) {}
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
                if( flipVertical ) {
                    vFlipImage = new BufferedImage(tBuffer.width, tBuffer.height, tBuffer.image.getType());
                } else {
                    vFlipImage = null;
                }
            }
            if( verbose ) {
                System.err.println("XXX tile-pre "+renderer);
            }
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    };
    static int _counter = 0;
    final GLEventListener postTileGLEL = new GLEventListener() {
        @Override
        public void init(GLAutoDrawable drawable) {
        }
        @Override
        public void dispose(GLAutoDrawable drawable) {}
        @Override
        public void display(GLAutoDrawable drawable) {              
            final DimensionImmutable cis = renderer.getClippedImageSize();
            final int tWidth = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_WIDTH);
            final int tHeight = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_HEIGHT);
            final int pX = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_X_POS);
            final int pY = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_Y_POS);
            final int pYOff = renderer.getParam(TileRenderer.TR_TILE_Y_OFFSET);
            final int pYf = cis.getHeight() - ( pY - pYOff + tHeight ) + scaledYOffset;
            
            // Copy temporary data into raster of BufferedImage for faster
            // blitting Note that we could avoid this copy in the cases
            // where !offscreenDrawable.isGLOriented(),
            // but that's the software rendering path which is very slow anyway.
            final BufferedImage dstImage;
            if( DEBUG_TILES ) {
                final String fname = String.format("file_%03d_0_tile_[%02d][%02d]_sz_%03dx%03d_pos0_%03d_%03d_yOff_%03d_pos1_%03d_%03d.png", 
                        _counter, 
                        renderer.getParam(TileRenderer.TR_CURRENT_COLUMN), renderer.getParam(TileRenderer.TR_CURRENT_ROW),
                        tWidth, tHeight,
                        pX, pY, pYOff, pX, pYf).replace(' ', '_');
                System.err.println("XXX file "+fname);
                final File fout = new File(fname); 
                try {
                    ImageIO.write(tBuffer.image, "png", fout);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }            
            if( flipVertical ) {
                final BufferedImage srcImage = tBuffer.image;
                dstImage = vFlipImage;
                final int[] src = ((DataBufferInt) srcImage.getRaster().getDataBuffer()).getData();
                final int[] dst = ((DataBufferInt) dstImage.getRaster().getDataBuffer()).getData();
                if( DEBUG_TILES ) {
                    Arrays.fill(dst, 0x55);
                }
                final int incr = tBuffer.width;
                int srcPos = 0;
                int destPos = (tHeight - 1) * tBuffer.width;
                for (; destPos >= 0; srcPos += incr, destPos -= incr) {
                    System.arraycopy(src, srcPos, dst, destPos, incr);
                }
            } else {
                dstImage = tBuffer.image;
            }
            if( DEBUG_TILES ) {
                final String fname = String.format("file_%03d_1_tile_[%02d][%02d]_sz_%03dx%03d_pos0_%03d_%03d_yOff_%03d_pos1_%03d_%03d.png", 
                        _counter, 
                        renderer.getParam(TileRenderer.TR_CURRENT_COLUMN), renderer.getParam(TileRenderer.TR_CURRENT_ROW),
                        tWidth, tHeight,
                        pX, pY, pYOff, pX, pYf).replace(' ', '_');
                System.err.println("XXX file "+fname);
                final File fout = new File(fname); 
                try {
                    ImageIO.write(dstImage, "png", fout);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                _counter++;
            }            
            // Draw resulting image in one shot
            final Shape oClip = g2d.getClip();
            // g2d.clipRect(pX, pYf, tWidth, tHeight);
            final BufferedImage outImage = dstImage.getSubimage(0, 0, tWidth, tHeight); // instead of clipping
            final boolean drawDone = g2d.drawImage(outImage, pX, pYf, null); // Null ImageObserver since image data is ready.
            // final boolean drawDone = g2d.drawImage(dstImage, pX, pYf, dstImage.getWidth(), dstImage.getHeight(), null); // Null ImageObserver since image data is ready.
            if( verbose ) {
                System.err.println("XXX tile-post.X clippedImageSize "+cis);
                System.err.println("XXX tile-post.X pYf "+cis.getHeight()+" - ( "+pY+" - "+pYOff+" + "+tHeight+" ) "+scaledYOffset+" = "+ pYf);
                System.err.println("XXX tile-post.X clip "+oClip+" + "+pX+" / [pY "+pY+", pYOff "+pYOff+", pYf "+pYf+"] "+tWidth+"x"+tHeight+" -> "+g2d.getClip());
                g2d.setColor(Color.BLACK);
                g2d.drawRect(pX, pYf, tWidth, tHeight);
                if( null != oClip ) {
                    final Rectangle r = oClip.getBounds();
                    g2d.setColor(Color.YELLOW);
                    g2d.drawRect(r.x, r.y, r.width, r.height);
                }
                System.err.println("XXX tile-post.X "+renderer);
                System.err.println("XXX tile-post.X dst-img "+dstImage.getWidth()+"x"+dstImage.getHeight());
                System.err.println("XXX tile-post.X out-img "+outImage.getWidth()+"x"+outImage.getHeight());
                System.err.println("XXX tile-post.X y-flip "+flipVertical+" -> "+pX+"/"+pYf+", drawDone "+drawDone);
            }
            // g2d.setClip(oClip);
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    };
}
