package com.jogamp.opengl.util;

import java.nio.ByteBuffer;

import javax.media.nativewindow.util.Dimension;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES3;

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
public class TileRenderer {

    protected static final boolean DEBUG = true;
    protected static final int DEFAULT_TILE_WIDTH = 256;
    protected static final int DEFAULT_TILE_HEIGHT = 256;
    protected static final int DEFAULT_TILE_BORDER = 0;

    /**
     * The width of a tile
     */
    public static final int TR_TILE_WIDTH = 0;
    /**
     * The height of a tile
     */
    public static final int TR_TILE_HEIGHT = 1;
    /**
     * The width of the border around the tiles
     */
    public static final int TR_TILE_BORDER = 2;
    /**
     * The width of the final image
     */
    public static final int TR_IMAGE_WIDTH = 3;
    /**
     * The height of the final image
     */
    public static final int TR_IMAGE_HEIGHT = 4;
    /**
     * The number of rows of tiles
     */
    public static final int TR_ROWS = 5;
    /**
     * The number of columns of tiles
     */
    public static final int TR_COLUMNS = 6;
    /**
     * The current row number
     */
    public static final int TR_CURRENT_ROW = 7;
    /**
     * The current column number
     */
    public static final int TR_CURRENT_COLUMN = 8;
    /**
     * The width of the current tile
     */
    public static final int TR_CURRENT_TILE_WIDTH = 9;
    /**
     * The height of the current tile
     */
    public static final int TR_CURRENT_TILE_HEIGHT = 10;
    /**
     * The order that the rows are traversed
     */
    public static final int TR_ROW_ORDER = 11;
    /**
     * Indicates we are traversing rows from the top to the bottom
     */
    public static final int TR_TOP_TO_BOTTOM = 1;
    /**
     * Indicates we are traversing rows from the bottom to the top
     */
    public static final int TR_BOTTOM_TO_TOP = 2;

    protected final Dimension imageSize = new Dimension(0, 0);
    protected final Dimension tileSize = new Dimension(DEFAULT_TILE_WIDTH, DEFAULT_TILE_HEIGHT);
    protected final Dimension tileSizeNB = new Dimension(DEFAULT_TILE_WIDTH - 2 * DEFAULT_TILE_BORDER, DEFAULT_TILE_HEIGHT - 2 * DEFAULT_TILE_BORDER);
    protected final int[] userViewport = new int[ 4 ];
    protected final GLPixelStorageModes psm = new GLPixelStorageModes();

    protected int tileBorder = DEFAULT_TILE_BORDER;
    protected int imageFormat;
    protected int imageType;
    protected ByteBuffer imageBuffer;
    protected int tileFormat;
    protected int tileType;
    protected ByteBuffer tileBuffer;
    protected int rowOrder = TR_BOTTOM_TO_TOP;
    protected int rows;
    protected int columns;
    protected int currentTile = -1;
    protected int currentTileWidth;
    protected int currentTileHeight;
    protected int currentRow;
    protected int currentColumn;
    protected PMVMatrixCallback pmvMatrixCB = null;

    public static interface PMVMatrixCallback {
        void reshapePMVMatrix(GL gl, int tileNum, int tileColumn, int tileRow, int tileX, int tileY, int tileWidth, int tileHeight, int imageWidth, int imageHeight);      
    }

    /**
     * Creates a new TileRenderer object
     */
    public TileRenderer() {
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
        assert ( border >= 0 );
        assert ( width >= 1 );
        assert ( height >= 1 );
        assert ( width >= 2 * border );
        assert ( height >= 2 * border );

        tileBorder = border;
        tileSize.setWidth( width );
        tileSize.setHeight( height );
        tileSizeNB.setWidth( width - 2 * border );
        tileSizeNB.setHeight( height - 2 * border );
        setup();
    }

    public final void setPMVMatrixCallback(PMVMatrixCallback pmvMatrixCB) {
        assert ( null != pmvMatrixCB );
        this.pmvMatrixCB = pmvMatrixCB; 
    }

    /**
     * Sets up the number of rows and columns needed
     */
    protected final void setup() throws IllegalStateException {
        columns = ( imageSize.getWidth() + tileSizeNB.getWidth() - 1 ) / tileSizeNB.getWidth();
        rows = ( imageSize.getHeight() + tileSizeNB.getHeight() - 1 ) / tileSizeNB.getHeight();
        currentTile = 0;
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

    /**
     * Specify a buffer the tiles to be copied to. This is not
     * necessary for the creation of the final image, but useful if you
     * want to inspect each tile in turn.
     * 
     * @param format
     *           Interpreted as in glReadPixels
     * @param type
     *           Interpreted as in glReadPixels
     * @param buffer
     *           The buffer itself. Must be large enough to contain a
     *           tile, minus any borders
     */
    public final void setTileBuffer(int format, int type, ByteBuffer buffer) {
        tileFormat = format;
        tileType = type;
        tileBuffer = buffer;
    }

    /**
     * Sets the desired size of the final image
     * 
     * @param width
     *           The width of the final image
     * @param height
     *           The height of the final image
     */
    public final void setImageSize(int width, int height) {
        imageSize.setWidth(width);
        imageSize.setHeight(height);
        setup();
    }

    /**
     * Sets the buffer in which to store the final image
     * 
     * @param format
     *           Interpreted as in glReadPixels
     * @param type
     *           Interpreted as in glReadPixels
     * @param image
     *           the buffer itself, must be large enough to hold the
     *           final image
     */
    public final void setImageBuffer(int format, int type, ByteBuffer buffer) {
        imageFormat = format;
        imageType = type;
        imageBuffer = buffer;
    }

    /**
     * Gets the parameters of this TileRenderer object
     * 
     * @param param
     *           The parameter that is to be retrieved
     * @return the value of the parameter
     */
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
            if( currentTile < 0 )
                return -1;
            else
                return currentRow;
        case TR_CURRENT_COLUMN:
            if( currentTile < 0 )
                return -1;
            else
                return currentColumn;
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

    /**
     * Begins rendering a tile.
     * <p> 
     * The projection matrix stack should be
     * left alone after calling this method!
     * </p>
     * 
     * @param gl The gl context
     */
    public final void beginTile( GL2ES3 gl ) {
        if( 0 >= imageSize.getWidth() || 0 >= imageSize.getHeight() ) {
            throw new IllegalStateException("Image size has not been set");        
        }
        if( null == this.pmvMatrixCB ) {
            throw new IllegalStateException("pmvMatrixCB has not been set");        
        }
        if (currentTile <= 0) {
            setup();
            /*
             * Save user's viewport, will be restored after last tile
             * rendered
             */
            gl.glGetIntegerv( GL.GL_VIEWPORT, userViewport, 0 );
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

        final int tX = currentColumn * tileSizeNB.getWidth() - border;
        final int tY = currentRow * tileSizeNB.getHeight() - border;

        final int preTileWidth = currentTileWidth;
        final int preTileHeight = currentTileHeight;

        /* Save tile size, with border */
        currentTileWidth = tW;
        currentTileHeight = tH;

        if( DEBUG ) {
            System.err.println("Tile["+currentTile+"]: ["+preColumn+"]["+preRow+"] "+preTileWidth+"x"+preTileHeight+
                    " -> ["+currentColumn+"]["+currentRow+"] "+tX+"/"+tY+", "+tW+"x"+tH+", image "+imageSize.getWidth()+"x"+imageSize.getHeight());
        }

        gl.glViewport( 0, 0, tW, tH );
        pmvMatrixCB.reshapePMVMatrix(gl, currentTile, currentColumn, currentRow, tX, tY, tW, tH, imageSize.getWidth(), imageSize.getHeight());
    }

    /**
     * Must be called after rendering the scene
     * 
     * @param gl
     *           the gl context
     * @return true if there are more tiles to be rendered, false if
     *         the final image is complete
     */
    public boolean endTile( GL2ES3 gl )  {
        assert ( currentTile >= 0 );

        // be sure OpenGL rendering is finished
        gl.glFlush();

        // save current glPixelStore values
        psm.save(gl);

        final int tmp[] = new int[1];

        if( tileBuffer != null ) {
            int srcX = tileBorder;
            int srcY = tileBorder;
            int srcWidth = tileSizeNB.getWidth();
            int srcHeight = tileSizeNB.getHeight();
            final int bytesPerPixel = GLBuffers.bytesPerPixel(tileFormat, tileType);
            final int readPixelSize = GLBuffers.sizeof(gl, tmp, bytesPerPixel, srcWidth, srcHeight, 1, true);
            tileBuffer.clear();
            if( tileBuffer.limit() < readPixelSize ) {
                throw new IndexOutOfBoundsException("Required " + readPixelSize + " bytes of buffer, only had " + tileBuffer.limit());
            }
            gl.glReadPixels( srcX, srcY, srcWidth, srcHeight, tileFormat, tileType, tileBuffer );
            // be sure OpenGL rendering is finished
            gl.glFlush();
            tileBuffer.position( readPixelSize );
            tileBuffer.flip();
        }

        if( imageBuffer != null ) {
            int srcX = tileBorder;
            int srcY = tileBorder;
            int srcWidth = currentTileWidth - 2 * tileBorder;
            int srcHeight = currentTileHeight - 2 * tileBorder;

            /* setup pixel store for glReadPixels */
            final int rowLength = imageSize.getWidth();
            psm.setPackRowLength(gl, rowLength);
            psm.setPackAlignment(gl, 1);

            /* read the tile into the final image */
            final int bytesPerPixel = GLBuffers.bytesPerPixel(imageFormat, imageType);
            final int readPixelSize = GLBuffers.sizeof(gl, tmp, bytesPerPixel, srcWidth, srcHeight, 1, true);

            final int skipPixels = tileSizeNB.getWidth() * currentColumn;
            final int skipRows = tileSizeNB.getHeight() * currentRow;
            final int ibPos = ( skipPixels + ( skipRows * rowLength ) ) * bytesPerPixel;
            final int ibLim = ibPos + readPixelSize;
            imageBuffer.clear();
            if( imageBuffer.limit() < ibLim ) {
                throw new IndexOutOfBoundsException("Required " + ibLim + " bytes of buffer, only had " + imageBuffer.limit());
            }
            imageBuffer.position(ibPos);

            gl.glReadPixels( srcX, srcY, srcWidth, srcHeight, imageFormat, imageType, imageBuffer);
            // be sure OpenGL rendering is finished
            gl.glFlush();
            imageBuffer.position( ibLim );
            imageBuffer.flip();
        }

        /* restore previous glPixelStore values */
        psm.restore(gl);

        /* increment tile counter, return 1 if more tiles left to render */
        currentTile++;
        if( currentTile >= rows * columns ) {
            /* restore user's viewport */
            gl.glViewport( userViewport[ 0 ], userViewport[ 1 ], userViewport[ 2 ], userViewport[ 3 ] );
            currentTile = -1; /* all done */
            return false;
        } else {
            return true;
        }
    }
}