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
import java.awt.geom.NoninvertibleTransformException;
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
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;

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
    public final int customTileWidth, customTileHeight, customNumSamples;
    public final boolean verbose;

    /** Default for OpenGL: True */
    public boolean flipVertical;
    /** Default for OpenGL: True */
    public boolean originBottomLeft;
    private AWTGLPixelBuffer tBuffer = null;
    private BufferedImage vFlipImage = null;
    private Graphics2D g2d = null;
    private AffineTransform saveAT = null;

    public static void dumpHintsAndScale(final Graphics2D g2d) {
          final RenderingHints rHints = g2d.getRenderingHints();
          final Set<Entry<Object, Object>> rEntries = rHints.entrySet();
          int count = 0;
          for(final Iterator<Entry<Object, Object>> rEntryIter = rEntries.iterator(); rEntryIter.hasNext(); count++) {
              final Entry<Object, Object> rEntry = rEntryIter.next();
              System.err.println("Hint["+count+"]: "+rEntry.getKey()+" -> "+rEntry.getValue());
          }
          final AffineTransform aTrans = g2d.getTransform();
          if( null != aTrans ) {
              System.err.println(" type "+aTrans.getType());
              System.err.println(" scale "+aTrans.getScaleX()+" x "+aTrans.getScaleY());
              System.err.println(" move "+aTrans.getTranslateX()+" x "+aTrans.getTranslateY());
              System.err.println(" mat  "+aTrans);
          } else {
              System.err.println(" null transform");
          }
    }

    /**
     * @return resulting number of samples by comparing w/ {@link #customNumSamples} and the caps-config, 0 if disabled
     */
    public int getNumSamples(final GLCapabilitiesImmutable caps) {
          if( 0 > customNumSamples ) {
              return 0;
          } else if( 0 < customNumSamples ) {
              if ( !caps.getGLProfile().isGL2ES3() ) {
                  return 0;
              }
              return Math.max(caps.getNumSamples(), customNumSamples);
          } else {
              return caps.getNumSamples();
          }
    }

    /**
     * Assumes a configured {@link TileRenderer}, i.e.
     * an {@link TileRenderer#attachAutoDrawable(GLAutoDrawable) attached}
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
     * @param numSamples custom multisampling value: < 0 turns off, == 0 leaves as-is, > 0 enables using given num samples
     * @param tileWidth custom tile width for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     * @param tileHeight custom tile height for {@link TileRenderer#setTileSize(int, int, int) tile renderer}, pass -1 for default.
     * @param verbose
     */
    public AWTTilePainter(final TileRenderer renderer, final int componentCount, final double scaleMatX, final double scaleMatY, final int numSamples, final int tileWidth, final int tileHeight, final boolean verbose) {
        this.renderer = renderer;
        this.renderer.setGLEventListener(preTileGLEL, postTileGLEL);
        this.componentCount = componentCount;
        this.scaleMatX = scaleMatX;
        this.scaleMatY = scaleMatY;
        this.customNumSamples = numSamples;
        this.customTileWidth= tileWidth;
        this.customTileHeight = tileHeight;
        this.verbose = verbose;
        this.flipVertical = true;
    }

    @Override
    public String toString() {
        return "AWTTilePainter[flipVertical "+flipVertical+", startFromBottom "+originBottomLeft+", "+
                renderer.toString()+"]";
    }

    /**
     * @param flipVertical if <code>true</code>, the image will be flipped vertically (Default for OpenGL).
     * @param originBottomLeft if <code>true</code>, the image's origin is on the bottom left (Default for OpenGL).
     */
    public void setGLOrientation(final boolean flipVertical, final boolean originBottomLeft) {
        this.flipVertical = flipVertical;
        this.originBottomLeft = originBottomLeft;
    }

    private static Rectangle2D getClipBounds2D(final Graphics2D g) {
        final Shape shape = g.getClip();
        return null != shape ? shape.getBounds2D() : null;
    }
    private static Rectangle2D clipNegative(final Rectangle2D in) {
        if( null == in ) { return null; }
        double x=in.getX(), y=in.getY(), width=in.getWidth(), height=in.getHeight();
        if( 0 > x ) {
            width += x;
            x = 0;
        }
        if( 0 > y ) {
            height += y;
            y = 0;
        }
        return new Rectangle2D.Double(x, y, width, height);
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
     * @throws NoninvertibleTransformException if the {@link Graphics2D}'s {@link AffineTransform} {@link AffineTransform#invert() inversion} fails.
     *                                         Since inversion is tested before scaling the given {@link Graphics2D}, caller shall ignore the whole <i>term</i>.
     */
    public void setupGraphics2DAndClipBounds(final Graphics2D g2d, final int width, final int height) throws NoninvertibleTransformException {
        this.g2d = g2d;
        saveAT = g2d.getTransform();
        if( null == saveAT ) {
            saveAT = new AffineTransform(); // use identity
        }
        // We use double precision for scaling
        //
        // Setup original rectangles
        final Rectangle2D dClipOrigR = getClipBounds2D(g2d);
        final Rectangle2D dClipOrig = clipNegative(dClipOrigR);
        final Rectangle2D dImageSizeOrig = new Rectangle2D.Double(0, 0, width, height);

        // Retrieve scaled image-size and clip-bounds
        // Note: Clip bounds lie within image-size!
        final Rectangle2D dImageSizeScaled, dClipScaled;
        {
            final AffineTransform scaledATI;
            {
                final AffineTransform scaledAT = new AffineTransform(saveAT);
                scaledAT.scale(scaleMatX, scaleMatY);
                scaledATI = scaledAT.createInverse(); // -> NoninvertibleTransformException
            }
            Shape s0 = saveAT.createTransformedShape(dImageSizeOrig); // user in
            dImageSizeScaled = scaledATI.createTransformedShape(s0).getBounds2D(); // scaled out
            if( null == dClipOrig ) {
                dClipScaled = (Rectangle2D) dImageSizeScaled.clone();
            } else {
                s0 = saveAT.createTransformedShape(dClipOrig); // user in
                dClipScaled = scaledATI.createTransformedShape(s0).getBounds2D(); // scaled out
            }
        }
        final Rectangle iClipScaled = dClipScaled.getBounds();
        final Rectangle iImageSizeScaled = dImageSizeScaled.getBounds();
        renderer.setImageSize(iImageSizeScaled.width, iImageSizeScaled.height);
        renderer.clipImageSize(iClipScaled.width, iClipScaled.height);
        final int clipH = Math.min(iImageSizeScaled.height, iClipScaled.height);
        // Clip bounds lie within image-size!
        // GL y-offset is lower-left origin, AWT y-offset upper-left.
        scaledYOffset = iClipScaled.y;
        renderer.setTileOffset(iClipScaled.x, iImageSizeScaled.height - ( iClipScaled.y + clipH ));

        // Scale actual Grahics2D matrix
        g2d.scale(scaleMatX, scaleMatY);

        if( verbose ) {
            System.err.println("AWT print.0: image "+dImageSizeOrig + " -> " + dImageSizeScaled + " -> " + iImageSizeScaled);
            System.err.println("AWT print.0: clip  "+dClipOrigR + " -> " + dClipOrig + " -> " + dClipScaled + " -> " + iClipScaled);
            System.err.println("AWT print.0: "+renderer);
        }
    }
    private int scaledYOffset;

    /** See {@ #setupGraphics2DAndClipBounds(Graphics2D)}. */
    public void resetGraphics2D() {
        g2d.setTransform(saveAT);
    }

    /**
     * Disposes resources and {@link TileRenderer#detachAutoDrawable() detaches}
     * the {@link TileRenderer}'s {@link GLAutoDrawable}.
     */
    public void dispose() {
        renderer.detachAutoDrawable(); // tile-renderer -> printGLAD
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
        public void init(final GLAutoDrawable drawable) {}
        @Override
        public void dispose(final GLAutoDrawable drawable) {}
        @Override
        public void display(final GLAutoDrawable drawable) {
            final GL gl = drawable.getGL();
            if( null == tBuffer ) {
                final int tWidth = renderer.getParam(TileRenderer.TR_TILE_WIDTH);
                final int tHeight = renderer.getParam(TileRenderer.TR_TILE_HEIGHT);
                final AWTGLPixelBufferProvider printBufferProvider = new AWTGLPixelBufferProvider( true /* allowRowStride */ );
                final PixelFormat.Composition hostPixelComp = printBufferProvider.getHostPixelComp(gl.getGLProfile(), componentCount);
                final GLPixelAttributes pixelAttribs = printBufferProvider.getAttributes(gl, componentCount, true);
                tBuffer = printBufferProvider.allocate(gl, hostPixelComp, pixelAttribs, true, tWidth, tHeight, 1, 0);
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
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
    };
    static int _counter = 0;
    final GLEventListener postTileGLEL = new GLEventListener() {
        @Override
        public void init(final GLAutoDrawable drawable) {
        }
        @Override
        public void dispose(final GLAutoDrawable drawable) {}
        @Override
        public void display(final GLAutoDrawable drawable) {
            final DimensionImmutable cis = renderer.getClippedImageSize();
            final int tWidth = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_WIDTH);
            final int tHeight = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_HEIGHT);
            final int tY = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_Y_POS);
            final int tYOff = renderer.getParam(TileRenderer.TR_TILE_Y_OFFSET);
            final int imgYOff = originBottomLeft ? 0 : renderer.getParam(TileRenderer.TR_TILE_HEIGHT) - tHeight; // imgYOff will be cut-off via sub-image
            final int pX = renderer.getParam(TileRendererBase.TR_CURRENT_TILE_X_POS); // tileX == pX
            final int pY = cis.getHeight() - ( tY - tYOff + tHeight ) + scaledYOffset;

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
                        pX, tY, tYOff, pX, pY).replace(' ', '_');
                System.err.println("XXX file "+fname);
                final File fout = new File(fname);
                try {
                    ImageIO.write(tBuffer.image, "png", fout);
                } catch (final IOException e) {
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
                        pX, tY, tYOff, pX, pY).replace(' ', '_');
                System.err.println("XXX file "+fname);
                final File fout = new File(fname);
                try {
                    ImageIO.write(dstImage, "png", fout);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                _counter++;
            }
            // Draw resulting image in one shot
            final BufferedImage outImage = dstImage.getSubimage(0, imgYOff, tWidth, tHeight);
            final boolean drawDone = g2d.drawImage(outImage, pX, pY, null); // Null ImageObserver since image data is ready.
            if( verbose ) {
                final Shape oClip = g2d.getClip();
                System.err.println("XXX tile-post.X tile 0 / "+imgYOff+" "+tWidth+"x"+tHeight+", clippedImgSize "+cis);
                System.err.println("XXX tile-post.X pYf "+cis.getHeight()+" - ( "+tY+" - "+tYOff+" + "+tHeight+" ) "+scaledYOffset+" = "+ pY);
                System.err.println("XXX tile-post.X clip "+oClip+" + "+pX+" / [pY "+tY+", pYOff "+tYOff+", pYf "+pY+"] -> "+g2d.getClip());
                g2d.setColor(Color.BLACK);
                g2d.drawRect(pX, pY, tWidth, tHeight);
                if( null != oClip ) {
                    final Rectangle r = oClip.getBounds();
                    g2d.setColor(Color.YELLOW);
                    g2d.drawRect(r.x, r.y, r.width, r.height);
                }
                System.err.println("XXX tile-post.X "+renderer);
                System.err.println("XXX tile-post.X dst-img "+dstImage.getWidth()+"x"+dstImage.getHeight());
                System.err.println("XXX tile-post.X out-img "+outImage.getWidth()+"x"+outImage.getHeight());
                System.err.println("XXX tile-post.X y-flip "+flipVertical+", originBottomLeft "+originBottomLeft+" -> "+pX+"/"+pY+", drawDone "+drawDone);
            }
        }
        @Override
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
    };
}
