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

import javax.media.nativewindow.util.Dimension;
import javax.media.opengl.GL2ES3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

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
 * 
 * @author ryanm, sgothel
 */
public class TileRenderer extends TileRendererBase {
    /**
     * The width of a tile
     */
    public static final int TR_TILE_WIDTH = 7;
    /**
     * The height of a tile
     */
    public static final int TR_TILE_HEIGHT = 8;
    /**
     * The width of the border around the tiles
     */
    public static final int TR_TILE_BORDER = 9;
    /**
     * The number of rows of tiles
     */
    public static final int TR_ROWS = 10;
    /**
     * The number of columns of tiles
     */
    public static final int TR_COLUMNS = 11;
    /**
     * The current row number
     */
    public static final int TR_CURRENT_ROW = 12;
    /**
     * The current column number
     */
    public static final int TR_CURRENT_COLUMN = 13;
    /**
     * The order that the rows are traversed
     */
    public static final int TR_ROW_ORDER = 14;
    /**
     * Indicates we are traversing rows from the top to the bottom
     */
    public static final int TR_TOP_TO_BOTTOM = 15;
    /**
     * Indicates we are traversing rows from the bottom to the top
     */
    public static final int TR_BOTTOM_TO_TOP = 16;

    private static final boolean DEBUG = true;
    private static final int DEFAULT_TILE_WIDTH = 256;
    private static final int DEFAULT_TILE_HEIGHT = 256;
    private static final int DEFAULT_TILE_BORDER = 0;

    private final Dimension tileSize = new Dimension(DEFAULT_TILE_WIDTH, DEFAULT_TILE_HEIGHT);
    private final Dimension tileSizeNB = new Dimension(DEFAULT_TILE_WIDTH - 2 * DEFAULT_TILE_BORDER, DEFAULT_TILE_HEIGHT - 2 * DEFAULT_TILE_BORDER);

    private int tileBorder = DEFAULT_TILE_BORDER;
    private int rowOrder = TR_BOTTOM_TO_TOP;
    private int rows;
    private int columns;
    private int currentTile = -1;
    private int currentRow;
    private int currentColumn;

    /**
     * Creates a new TileRenderer object
     */
    public TileRenderer() {
        super();
    }

    /**
     * Sets the size of the tiles to use in rendering. The actual
     * effective size of the tile depends on the border size, ie (
     * width - 2*border ) * ( height - 2 * border )
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
    public final void setTileSize(int width, int height, int border) {
        if( 0 > border ) {
            throw new IllegalArgumentException("Tile border must be >= 0");        
        }
        if( 2 * border >= width || 2 * border >= height ) {
            throw new IllegalArgumentException("Tile size must be > 0x0 minus 2*border");        
        }
        tileBorder = border;
        tileSize.setWidth( width );
        tileSize.setHeight( height );
        tileSizeNB.setWidth( width - 2 * border );
        tileSizeNB.setHeight( height - 2 * border );
        setup();
    }

    /**
     * Sets up the number of rows and columns needed
     */
    private final void setup() throws IllegalStateException {
        columns = ( imageSize.getWidth() + tileSizeNB.getWidth() - 1 ) / tileSizeNB.getWidth();
        rows = ( imageSize.getHeight() + tileSizeNB.getHeight() - 1 ) / tileSizeNB.getHeight();
        currentTile = 0;
        currentTileXPos = 0;
        currentTileYPos = 0;
        currentTileWidth = 0;
        currentTileHeight = 0;
        currentRow = 0;
        currentColumn = 0;

        assert columns >= 0;
        assert rows >= 0;
    }

    /** 
     * Returns <code>true</code> if all tiles have been rendered or {@link #setup()}
     * has not been called, otherwise <code>false</code>.
     */
    public final boolean eot() { return 0 > currentTile; }

    @Override
    public final int getParam(int param) {
        switch (param) {
        case TR_TILE_WIDTH:
            return tileSize.getWidth();
        case TR_TILE_HEIGHT:
            return tileSize.getHeight();
        case TR_TILE_BORDER:
            return tileBorder;
        case TR_IMAGE_WIDTH:
            return imageSize.getWidth();
        case TR_IMAGE_HEIGHT:
            return imageSize.getHeight();
        case TR_ROWS:
            return rows;
        case TR_COLUMNS:
            return columns;
        case TR_CURRENT_ROW:
            if( currentTile < 0 ) {
                return -1;
            } else {
                return currentRow;
            }
        case TR_CURRENT_COLUMN:
            if( currentTile < 0 ) {
                return -1;
            } else {
                return currentColumn;
            }
        case TR_CURRENT_TILE_X_POS:
            return currentTileXPos;
        case TR_CURRENT_TILE_Y_POS:
            return currentTileYPos;
        case TR_CURRENT_TILE_WIDTH:
            return currentTileWidth;
        case TR_CURRENT_TILE_HEIGHT:
            return currentTileHeight;
        case TR_ROW_ORDER:
            return rowOrder;
        default:
            throw new IllegalArgumentException("Invalid enumerant as argument");
        }
    }

    /**
     * Sets the order of row traversal
     * 
     * @param order
     *           The row traversal order, must be
     *           eitherTR_TOP_TO_BOTTOM or TR_BOTTOM_TO_TOP
     */
    public final void setRowOrder(int order) {
        if (order == TR_TOP_TO_BOTTOM || order == TR_BOTTOM_TO_TOP) {
            rowOrder = order;
        } else {
            throw new IllegalArgumentException("Must pass TR_TOP_TO_BOTTOM or TR_BOTTOM_TO_TOP");
        }
    }

    @Override
    public final void beginTile( GL2ES3 gl ) throws IllegalStateException {
        if( 0 >= imageSize.getWidth() || 0 >= imageSize.getHeight() ) {
            throw new IllegalStateException("Image size has not been set");        
        }
        if( null == this.pmvMatrixCB ) {
            throw new IllegalStateException("pmvMatrixCB has not been set");        
        }
        if (currentTile <= 0) {
            setup();
        }

        final int preRow = currentRow;
        final int preColumn = currentColumn;

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

        int border = tileBorder;

        int tH, tW;

        /* Compute actual size of this tile with border */
        if (currentRow < rows - 1) {
            tH = tileSize.getHeight();
        } else {
            tH = imageSize.getHeight() - ( rows - 1 ) * ( tileSizeNB.getHeight() ) + 2 * border;
        }

        if (currentColumn < columns - 1) {
            tW = tileSize.getWidth();
        } else {
            tW = imageSize.getWidth() - ( columns - 1 ) * ( tileSizeNB.getWidth()  ) + 2 * border;
        }

        currentTileXPos = currentColumn * tileSizeNB.getWidth();
        currentTileYPos = currentRow * tileSizeNB.getHeight();

        final int preTileWidth = currentTileWidth;
        final int preTileHeight = currentTileHeight;

        /* Save tile size, with border */
        currentTileWidth = tW;
        currentTileHeight = tH;

        if( DEBUG ) {
            System.err.println("Tile["+currentTile+"]: ["+preColumn+"]["+preRow+"] "+preTileWidth+"x"+preTileHeight+
                    " -> ["+currentColumn+"]["+currentRow+"] "+currentTileXPos+"/"+currentTileYPos+", "+tW+"x"+tH+", image "+imageSize.getWidth()+"x"+imageSize.getHeight());
        }

        gl.glViewport( 0, 0, tW, tH );
        pmvMatrixCB.reshapePMVMatrix(gl, currentTileXPos, currentTileYPos, tW, tH, imageSize.getWidth(), imageSize.getHeight());
        beginCalled = true;
    }

    @Override
    public void endTile( GL2ES3 gl ) throws IllegalStateException {
        if( !beginCalled ) {
            throw new IllegalStateException("beginTile(..) has not been called");
        }

        // be sure OpenGL rendering is finished
        gl.glFlush();

        // save current glPixelStore values
        psm.save(gl);

        final int tmp[] = new int[1];

        if( tileBuffer != null ) {
            final GLPixelAttributes pixelAttribs = tileBuffer.pixelAttributes;
            final int srcX = tileBorder;
            final int srcY = tileBorder;
            final int srcWidth = tileSizeNB.getWidth();
            final int srcHeight = tileSizeNB.getHeight();
            final int readPixelSize = GLBuffers.sizeof(gl, tmp, pixelAttribs.bytesPerPixel, srcWidth, srcHeight, 1, true);
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
            psm.setPackRowLength(gl, rowLength);
            psm.setPackAlignment(gl, 1);

            /* read the tile into the final image */
            final int readPixelSize = GLBuffers.sizeof(gl, tmp, pixelAttribs.bytesPerPixel, srcWidth, srcHeight, 1, true);

            final int skipPixels = currentColumn * tileSizeNB.getWidth();
            final int skipRows = currentRow * tileSizeNB.getHeight();
            final int ibPos = ( skipPixels + ( skipRows * rowLength ) ) * pixelAttribs.bytesPerPixel;
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
    
    /**
     * {@inheritDoc}
     * <p>
     * Sets the tile size of this renderer to the given {@link GLAutoDrawable} size
     * with zero tile border. 
     * </p>
     */
    @Override
    public void attachToAutoDrawable(GLAutoDrawable glad, PMVMatrixCallback pmvMatrixCB) throws IllegalStateException {
        super.attachToAutoDrawable(glad, pmvMatrixCB);
        setTileSize(glad.getWidth(), glad.getHeight(), 0);
    }
    
    protected final GLEventListener getTiledGLEL() { return tiledGLEL; }
    private final GLEventListener tiledGLEL = new GLEventListener() {
        @Override
        public void init(GLAutoDrawable drawable) {
            if( null != glEventListenerPre ) {
                glEventListenerPre.init(drawable);
            }
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                final GLEventListener l = listeners[i];
                l.init(drawable);
                listenersInit[i] = true;
            }
            if( null != glEventListenerPost ) {
                glEventListenerPost.init(drawable);
            }
        }
        @Override
        public void dispose(GLAutoDrawable drawable) {
            if( null != glEventListenerPre ) {
                glEventListenerPre.dispose(drawable);
            }
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                listeners[i].dispose(drawable);
            }
            if( null != glEventListenerPost ) {
                glEventListenerPost.dispose(drawable);
            }
        }
        @Override
        public void display(GLAutoDrawable drawable) {
            if( null != glEventListenerPre ) {
                glEventListenerPre.display(drawable);
            }
            final GL2ES3 gl = drawable.getGL().getGL2ES3();

            beginTile(gl);

            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                listeners[i].display(drawable);
            }

            endTile(gl);
            if( null != glEventListenerPost ) {
                glEventListenerPost.display(drawable);
            }
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            if( null != glEventListenerPre ) {
                glEventListenerPre.reshape(drawable, x, y, width, height);
            }
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                listeners[i].reshape(drawable, x, y, width, height);
            }
            if( null != glEventListenerPost ) {
                glEventListenerPost.reshape(drawable, x, y, width, height);
            }
        }
    };    
}