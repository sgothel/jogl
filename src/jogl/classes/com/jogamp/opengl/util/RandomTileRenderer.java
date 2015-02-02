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
package com.jogamp.opengl.util;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;

import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;

/**
 * Variation of {@link TileRenderer} w/o using fixed tiles but arbitrary rectangular regions.
 * <p>
 * See {@link TileRendererBase} for details.
 * </p>
 */
public class RandomTileRenderer extends TileRendererBase {
    private boolean tileRectSet = false;

    /**
     * Creates a new TileRenderer object
     */
    public RandomTileRenderer() {
        super();
    }

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
        default:
            throw new IllegalArgumentException("Invalid pname: "+pname);
        }
    }

    /**
     * Set the tile rectangle for the subsequent rendering calls.
     *
     * @throws IllegalArgumentException is tile x/y are < 0 or tile size is <= 0x0
     */
    public void setTileRect(final int tX, final int tY, final int tWidth, final int tHeight) throws IllegalStateException, IllegalArgumentException {
        if( 0 > tX || 0 > tY ) {
            throw new IllegalArgumentException("Tile pos must be >= 0/0");
        }
        if( 0 >= tWidth || 0 >= tHeight ) {
            throw new IllegalArgumentException("Tile size must be > 0x0");
        }
        this.currentTileXPos = tX;
        this.currentTileYPos = tY;
        this.currentTileWidth = tWidth;
        this.currentTileHeight = tHeight;
        tileRectSet = true;
    }

    @Override
    public final boolean isSetup() {
        return 0 < imageSize.getWidth() && 0 < imageSize.getHeight() && tileRectSet;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <i>end of tiling</i> is never reached w/ {@link RandomRileRenderer},
     * i.e. method always returns false.
     * </p>
     */
    @Override
    public final boolean eot() { return false; }

    /**
     * {@inheritDoc}
     *
     * Reset internal states of {@link RandomTileRenderer} are: <i>none</i>.
     */
    @Override
    public final void reset() { }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if {@link #setImageSize(int, int) image-size} has not been set or
     *         {@link #setTileRect(int, int, int, int) tile-rect} has not been set.
     */
    @Override
    public final void beginTile(final GL gl) throws IllegalStateException, GLException {
        if( 0 >= imageSize.getWidth() || 0 >= imageSize.getHeight() ) {
            throw new IllegalStateException("Image size has not been set");
        }
        if( !tileRectSet ) {
            throw new IllegalStateException("tileRect has not been set");
        }
        validateGL(gl);

        gl.glViewport( 0, 0, currentTileWidth, currentTileHeight );

        if( DEBUG ) {
            System.err.println("TileRenderer.begin.X: "+this.toString());
        }

        // Do not forget to issue:
        //    reshape( 0, 0, tW, tH );
        // which shall reflect tile renderer fileds: currentTileXPos, currentTileYPos and imageSize

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
            final int srcX = 0;
            final int srcY = 0;
            final int srcWidth = currentTileWidth;
            final int srcHeight = currentTileHeight;
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
            final int srcX = 0;
            final int srcY = 0;
            final int srcWidth = currentTileWidth;
            final int srcHeight = currentTileHeight;

            /* setup pixel store for glReadPixels */
            final int rowLength = imageSize.getWidth();
            psm.setPackRowLength(gl2es3, rowLength);

            /* read the tile into the final image */
            final int readPixelSize = GLBuffers.sizeof(gl, tmp, pixelAttribs.pfmt.comp.bytesPerPixel(), srcWidth, srcHeight, 1, true);

            final int ibPos = ( currentTileXPos + ( currentTileYPos * rowLength ) ) * pixelAttribs.pfmt.comp.bytesPerPixel(); // skipPixels + skipRows
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
    }

    /**
     * Rendering one tile, by simply calling {@link GLAutoDrawable#display()}.
     *
     * @throws IllegalStateException if no {@link GLAutoDrawable} is {@link #attachAutoDrawable(GLAutoDrawable) attached}
     *                               or imageSize is not set
     */
    public void display(final int tX, final int tY, final int tWidth, final int tHeight) throws IllegalStateException {
        setTileRect(tX, tY, tWidth, tHeight);
        display();
    }
}