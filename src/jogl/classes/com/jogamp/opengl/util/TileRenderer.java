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
 *
 * ---------------------
 *
 * Based on Brian Paul's tile rendering library, found
 * at <a href = "http://www.mesa3d.org/brianp/TR.html">http://www.mesa3d.org/brianp/TR.html</a>.
 *
 * Copyright (C) 1997-2005 Brian Paul.
 * Licensed under BSD-compatible terms with permission of the author.
 * See LICENSE.txt for license information.
 */
package com.jogamp.opengl.util;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLException;

import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;

/**
 * A fairly direct port of Brian Paul's tile rendering library, found
 * at <a href = "http://www.mesa3d.org/brianp/TR.html">
 * http://www.mesa3d.org/brianp/TR.html </a> . I've java-fied it, but
 * the functionality is the same.
 * <p>
 * Original code Copyright (C) 1997-2005 Brian Paul. Licensed under
 * BSD-compatible terms with permission of the author. See LICENSE.txt
 * for license information.
 * </p>
 * <p>
 * Enhanced for {@link GL2ES3}.
 * </p>
 * <p>
 * See {@link TileRendererBase} for details.
 * </p>
 *
 * @author ryanm, sgothel
 */
public class TileRenderer extends TileRendererBase {
    /**
     * The width of the final clipped image. See {@link #getParam(int)}.
     */
    public static final int TR_IMAGE_CLIPPING_WIDTH = 7;
    /**
     * The height of the final clipped image. See {@link #getParam(int)}.
     */
    public static final int TR_IMAGE_CLIPPING_HEIGHT = 8;
    /**
     * The width of the tiles. See {@link #getParam(int)}.
     */
    public static final int TR_TILE_WIDTH = 9;
    /**
     * The height of the tiles. See {@link #getParam(int)}.
     */
    public static final int TR_TILE_HEIGHT = 10;
    /**
     * The width of the border around the tiles. See {@link #getParam(int)}.
     */
    public static final int TR_TILE_BORDER = 11;
    /**
     * The tiles x-offset. See {@link #getParam(int)}.
     */
    public static final int TR_TILE_X_OFFSET = 12;
    /**
     * The tiles y-offset. See {@link #getParam(int)}.
     */
    public static final int TR_TILE_Y_OFFSET = 13;
    /**
     * The number of rows of tiles. See {@link #getParam(int)}.
     */
    public static final int TR_ROWS = 14;
    /**
     * The number of columns of tiles. See {@link #getParam(int)}.
     */
    public static final int TR_COLUMNS = 15;
    /**
     * The current tile number. Has value -1 if {@link #eot()}. See {@link #getParam(int)}.
     */
    public static final int TR_CURRENT_TILE_NUM = 16;
    /**
     * The current row number. See {@link #getParam(int)}.
     */
    public static final int TR_CURRENT_ROW = 17;
    /**
     * The current column number. See {@link #getParam(int)}.
     */
    public static final int TR_CURRENT_COLUMN = 18;
    /**
     * The order that the rows are traversed. See {@link #getParam(int)}.
     */
    public static final int TR_ROW_ORDER = 19;
    /**
     * Indicates we are traversing rows from the top to the bottom. See {@link #getParam(int)}.
     */
    public static final int TR_TOP_TO_BOTTOM = 20;
    /**
     * Indicates we are traversing rows from the bottom to the top (default). See {@link #getParam(int)}.
     */
    public static final int TR_BOTTOM_TO_TOP = 21;

    private static final int DEFAULT_TILE_WIDTH = 256;
    private static final int DEFAULT_TILE_HEIGHT = 256;
    private static final int DEFAULT_TILE_BORDER = 0;

    private final Dimension tileSize = new Dimension(DEFAULT_TILE_WIDTH, DEFAULT_TILE_HEIGHT);
    private final Dimension tileSizeNB = new Dimension(DEFAULT_TILE_WIDTH - 2 * DEFAULT_TILE_BORDER, DEFAULT_TILE_HEIGHT - 2 * DEFAULT_TILE_BORDER);

    private boolean isInit = false;
    private Dimension imageClippingDim = null; // not set - default
    private int tileBorder = DEFAULT_TILE_BORDER;
    private int rowOrder = TR_BOTTOM_TO_TOP;
    private int rows;
    private int columns;
    private int currentTile = 0;
    private int currentRow;
    private int currentColumn;
    private int offsetX;
    private int offsetY;

    @Override
    protected StringBuilder tileDetails(final StringBuilder sb) {
        sb.append("# "+currentTile+": ["+currentColumn+"]["+currentRow+"] / "+columns+"x"+rows+", ")
        .append("rowOrder "+rowOrder+", offset/size "+offsetX+"/"+offsetY+" "+tileSize.getWidth()+"x"+tileSize.getHeight()+" brd "+tileBorder+", ");
        return super.tileDetails(sb);
    }

    /**
     * Creates a new TileRenderer object
     */
    public TileRenderer() {
        super();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation {@link #reset()} internal states.
     * </p>
     */
    @Override
    public final void setImageSize(final int width, final int height) {
        super.setImageSize(width, height);
        reset();
    }

    /**
     * Clips the image-size this tile-renderer iterates through,
     * which can be retrieved via {@link #getClippedImageSize()}.
     * <p>
     * Original image-size stored in this tile-renderer is unmodified.
     * </p>
     * <p>
     * Implementation {@link #reset()} internal states.
     * </p>
     *
     * @param width The image-clipping.width
     * @param height The image-clipping.height
     * @see #getClippedImageSize()
     */
    public final void clipImageSize(final int width, final int height) {
        if( null == imageClippingDim ) {
            imageClippingDim = new Dimension(width, height);
        } else {
            imageClippingDim.set(width, height);
        }
        reset();
    }

    /**
     * Returns the clipped image-size.
     * <p>
     * If a image-size is clipped via {@link #clipImageSize(int, int)},
     * method returns:
     * <ul>
     *   <li><code>min( image-clipping, image-size )</code>, otherwise</li>
     *   <li><code> image-size </code></li>
     * </ul>
     * </p>
     * <p>
     * The clipping width and height can be retrieved via {@link #TR_IMAGE_CLIPPING_WIDTH}
     * {@link #TR_IMAGE_CLIPPING_HEIGHT}.
     * </p>
     */
    public final DimensionImmutable getClippedImageSize() {
        if( null != imageClippingDim ) {
            return new Dimension(Math.min(imageClippingDim.getWidth(), imageSize.getWidth()),
                                 Math.min(imageClippingDim.getHeight(), imageSize.getHeight()) );
        } else {
            return imageSize;
        }
    }

    /**
     * Sets the size of the tiles to use in rendering. The actual
     * effective size of the tile depends on the border size, ie (
     * width - 2*border ) * ( height - 2 * border )
     * <p>
     * Implementation {@link #reset()} internal states.
     * </p>
     *
     * @param width
     *           The width of the tiles. Must not be larger than the GL
     *           context
     * @param height
     *           The height of the tiles. Must not be larger than the
     *           GL context
     * @param border
     *           The width of the borders on each tile. This is needed
     *           to avoid artifacts when rendering lines or points with
     *           thickness > 1.
     */
    public final void setTileSize(final int width, final int height, final int border) {
        if( 0 > border ) {
            throw new IllegalArgumentException("Tile border must be >= 0");
        }
        if( 2 * border >= width || 2 * border >= height ) {
            throw new IllegalArgumentException("Tile size must be > 0x0 minus 2*border");
        }
        tileBorder = border;
        tileSize.set( width, height );
        tileSizeNB.set( width - 2 * border, height - 2 * border );
        reset();
    }

    /**
     * Sets an xy offset for the resulting tiles
     * {@link TileRendererBase#TR_CURRENT_TILE_X_POS x-pos} and {@link TileRendererBase#TR_CURRENT_TILE_Y_POS y-pos}.
     * @see #TR_TILE_X_OFFSET
     * @see #TR_TILE_Y_OFFSET
     **/
    public void setTileOffset(final int xoff, final int yoff) {
        offsetX = xoff;
        offsetY = yoff;
    }

    /**
     * {@inheritDoc}
     *
     * Reset internal states of {@link TileRenderer} are:
     * <ul>
     *  <li>{@link #TR_ROWS}</li>
     *  <li>{@link #TR_COLUMNS}</li>
     *  <li>{@link #TR_CURRENT_COLUMN}</li>
     *  <li>{@link #TR_CURRENT_ROW}</li>
     *  <li>{@link #TR_CURRENT_TILE_NUM}</li>
     *  <li>{@link #TR_CURRENT_TILE_X_POS}</li>
     *  <li>{@link #TR_CURRENT_TILE_Y_POS}</li>
     *  <li>{@link #TR_CURRENT_TILE_WIDTH}</li>
     *  <li>{@link #TR_CURRENT_TILE_HEIGHT}</li>
     *</ul>
     */
    @Override
    public final void reset() {
        final DimensionImmutable clippedImageSize = getClippedImageSize();
        columns = ( clippedImageSize.getWidth() + tileSizeNB.getWidth() - 1 ) / tileSizeNB.getWidth();
        rows = ( clippedImageSize.getHeight() + tileSizeNB.getHeight() - 1 ) / tileSizeNB.getHeight();
        currentRow = 0;
        currentColumn = 0;
        currentTile = 0;
        currentTileXPos = 0;
        currentTileYPos = 0;
        currentTileWidth = 0;
        currentTileHeight = 0;

        assert columns >= 0;
        assert rows >= 0;

        beginCalled = false;
        isInit = true;
    }

    /* pp */ final int getCurrentTile() { return currentTile; }

    @Override
    public final int getParam(final int pname) {
        switch (pname) {
        case TR_IMAGE_WIDTH:
            return imageSize.getWidth();
        case TR_IMAGE_HEIGHT:
            return imageSize.getHeight();
        case TR_CURRENT_TILE_X_POS:
            return currentTileXPos;
        case TR_CURRENT_TILE_Y_POS:
            return currentTileYPos;
        case TR_CURRENT_TILE_WIDTH:
            return currentTileWidth;
        case TR_CURRENT_TILE_HEIGHT:
            return currentTileHeight;
        case TR_IMAGE_CLIPPING_WIDTH:
            return null != imageClippingDim ? imageClippingDim.getWidth() : 0;
        case TR_IMAGE_CLIPPING_HEIGHT:
            return null != imageClippingDim ? imageClippingDim.getHeight() : 0;
        case TR_TILE_WIDTH:
            return tileSize.getWidth();
        case TR_TILE_HEIGHT:
            return tileSize.getHeight();
        case TR_TILE_BORDER:
            return tileBorder;
        case TR_TILE_X_OFFSET:
            return offsetX;
        case TR_TILE_Y_OFFSET:
            return offsetY;
        case TR_ROWS:
            return rows;
        case TR_COLUMNS:
            return columns;
        case TR_CURRENT_TILE_NUM:
            return currentTile;
        case TR_CURRENT_ROW:
            return currentRow;
        case TR_CURRENT_COLUMN:
            return currentColumn;
        case TR_ROW_ORDER:
            return rowOrder;
        default:
            throw new IllegalArgumentException("Invalid pname: "+pname);
        }
    }

    /**
     * Sets the order of row traversal, default is {@link #TR_BOTTOM_TO_TOP}.
     *
     * @param order The row traversal order, must be either {@link #TR_TOP_TO_BOTTOM} or {@link #TR_BOTTOM_TO_TOP}.
     */
    public final void setRowOrder(final int order) {
        if (order == TR_TOP_TO_BOTTOM || order == TR_BOTTOM_TO_TOP) {
            rowOrder = order;
        } else {
            throw new IllegalArgumentException("Must pass TR_TOP_TO_BOTTOM or TR_BOTTOM_TO_TOP");
        }
    }

    @Override
    public final boolean isSetup() {
        return 0 < imageSize.getWidth() && 0 < imageSize.getHeight();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <i>end of tiling</i> is reached w/ {@link TileRenderer}, if at least one of the following is true:
     * <ul>
     *   <li>all tiles have been rendered, i.e. {@link #TR_CURRENT_TILE_NUM} is -1</li>
     *   <li>no tiles to render, i.e. {@link #TR_COLUMNS} or {@link #TR_ROWS} is 0</li>
     * </ul>
     * </p>
     */
    @Override
    public final boolean eot() {
        if ( !isInit ) { // ensure at least one reset-call
            reset();
        }
        return 0 > currentTile || 0 >= columns*rows;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if {@link #setImageSize(int, int) image-size} has not been set or
     *         {@link #eot() end-of-tiling} has been reached.
     */
    @Override
    public final void beginTile( final GL gl ) throws IllegalStateException, GLException {
        if( !isSetup() ) {
            throw new IllegalStateException("Image size has not been set: "+this);
        }
        if ( eot() ) {
            throw new IllegalStateException("EOT reached: "+this);
        }
        validateGL(gl);

        /* which tile (by row and column) we're about to render */
        if (rowOrder == TR_BOTTOM_TO_TOP) {
            currentRow = currentTile / columns;
            currentColumn = currentTile % columns;
        } else {
            currentRow = rows - ( currentTile / columns ) - 1;
            currentColumn = currentTile % columns;
        }
        assert ( currentRow < rows );
        assert ( currentColumn < columns );

        final int border = tileBorder;

        final DimensionImmutable clippedImageSize = getClippedImageSize();
        int tH, tW;

        /* Compute actual size of this tile with border */
        if (currentRow < rows - 1) {
            tH = tileSize.getHeight();
        } else {
            tH = clippedImageSize.getHeight() - ( rows - 1 ) * ( tileSizeNB.getHeight() ) + 2 * border;
        }

        if (currentColumn < columns - 1) {
            tW = tileSize.getWidth();
        } else {
            tW = clippedImageSize.getWidth() - ( columns - 1 ) * ( tileSizeNB.getWidth()  ) + 2 * border;
        }

        currentTileXPos = currentColumn * tileSizeNB.getWidth() + offsetX;
        currentTileYPos = currentRow * tileSizeNB.getHeight() + offsetY;

        /* Save tile size, with border */
        currentTileWidth = tW;
        currentTileHeight = tH;

        gl.glViewport( 0, 0, tW, tH );

        if( DEBUG ) {
            System.err.println("TileRenderer.begin: "+this.toString());
        }

        // Do not forget to issue:
        //    reshape( 0, 0, tW, tH );
        // which shall reflect tile renderer tiles: currentTileXPos, currentTileYPos and imageSize
        beginCalled = true;
    }

    @Override
    public void endTile( final GL gl ) throws IllegalStateException, GLException {
        if( !beginCalled ) {
            throw new IllegalStateException("beginTile(..) has not been called");
        }
        validateGL(gl);

        // be sure OpenGL rendering is finished
        gl.glFlush();

        // implicitly save current glPixelStore values
        psm.setPackAlignment(gl, 1);
        final GL2ES3 gl2es3;
        final int readBuffer;
        if( gl.isGL2ES3() ) {
            gl2es3 = gl.getGL2ES3();
            readBuffer = gl2es3.getDefaultReadBuffer();
            gl2es3.glReadBuffer(readBuffer);
        } else {
            gl2es3 = null;
            readBuffer = 0; // undef. probably default: GL_FRONT (single buffering) GL_BACK (double buffering)
        }
        if( DEBUG ) {
            System.err.println("TileRenderer.end.0: readBuffer 0x"+Integer.toHexString(readBuffer)+", "+this.toString());
        }

        final int tmp[] = new int[1];

        if( tileBuffer != null ) {
            final GLPixelAttributes pixelAttribs = tileBuffer.pixelAttributes;
            final int srcX = tileBorder;
            final int srcY = tileBorder;
            final int srcWidth = tileSizeNB.getWidth();
            final int srcHeight = tileSizeNB.getHeight();
            final int readPixelSize = GLBuffers.sizeof(gl, tmp, pixelAttribs.pfmt.comp.bytesPerPixel(), srcWidth, srcHeight, 1, true);
            tileBuffer.clear();
            if( tileBuffer.requiresNewBuffer(gl, srcWidth, srcHeight, readPixelSize) ) {
                throw new IndexOutOfBoundsException("Required " + readPixelSize + " bytes of buffer, only had " + tileBuffer);
            }
            gl.glReadPixels( srcX, srcY, srcWidth, srcHeight, pixelAttribs.format, pixelAttribs.type, tileBuffer.buffer);
            // be sure OpenGL rendering is finished
            gl.glFlush();
            tileBuffer.position( readPixelSize );
            tileBuffer.flip();
        }

        if( imageBuffer != null ) {
            final GLPixelAttributes pixelAttribs = imageBuffer.pixelAttributes;
            final int srcX = tileBorder;
            final int srcY = tileBorder;
            final int srcWidth = currentTileWidth - 2 * tileBorder;
            final int srcHeight = currentTileHeight - 2 * tileBorder;

            /* setup pixel store for glReadPixels */
            final int rowLength = imageSize.getWidth();
            psm.setPackRowLength(gl2es3, rowLength);

            /* read the tile into the final image */
            final int readPixelSize = GLBuffers.sizeof(gl, tmp, pixelAttribs.pfmt.comp.bytesPerPixel(), srcWidth, srcHeight, 1, true);

            final int skipPixels = currentColumn * tileSizeNB.getWidth();
            final int skipRows = currentRow * tileSizeNB.getHeight();
            final int ibPos = ( skipPixels + ( skipRows * rowLength ) ) * pixelAttribs.pfmt.comp.bytesPerPixel();
            final int ibLim = ibPos + readPixelSize;
            imageBuffer.clear();
            if( imageBuffer.requiresNewBuffer(gl, srcWidth, srcHeight, readPixelSize) ) {
                throw new IndexOutOfBoundsException("Required " + ibLim + " bytes of buffer, only had " + imageBuffer);
            }
            imageBuffer.position(ibPos);

            gl.glReadPixels( srcX, srcY, srcWidth, srcHeight, pixelAttribs.format, pixelAttribs.type, imageBuffer.buffer);
            // be sure OpenGL rendering is finished
            gl.glFlush();
            imageBuffer.position( ibLim );
            imageBuffer.flip();
        }

        /* restore previous glPixelStore values */
        psm.restore(gl);

        beginCalled = false;

        /* increment tile counter, return 1 if more tiles left to render */
        currentTile++;
        if( currentTile >= rows * columns ) {
            currentTile = -1; /* all done */
        }
    }
}