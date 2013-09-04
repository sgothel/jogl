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

import javax.media.nativewindow.util.Dimension;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;

/**
 * Variation of {@link TileRenderer} w/o using fixed tiles but arbitrary rectangular regions. 
 */
public class RandomTileRenderer {
    private final Dimension imageSize = new Dimension(0, 0);
    private final GLPixelStorageModes psm = new GLPixelStorageModes();

    private GLPixelBuffer imageBuffer;
    private GLPixelBuffer tileBuffer;
    private PMVMatrixCallback pmvMatrixCB = null;
    private int tX = 0;
    private int tY = 0;
    private int tWidth = 0;
    private int tHeight = 0;

    private GLAutoDrawable glad;
    private GLEventListener[] listeners;
    private boolean[] listenersInit;
    private GLEventListener glEventListenerPre = null;
    private GLEventListener glEventListenerPost = null;
    
    public static interface PMVMatrixCallback {
        void reshapePMVMatrix(GL gl, int tileX, int tileY, int tileWidth, int tileHeight, int imageWidth, int imageHeight);      
    }

    /**
     * Creates a new TileRenderer object
     */
    public RandomTileRenderer() {
    }

    public final void setPMVMatrixCallback(PMVMatrixCallback pmvMatrixCB) {
        assert ( null != pmvMatrixCB );
        this.pmvMatrixCB = pmvMatrixCB; 
    }

    /**
     * Specify a buffer the tiles to be copied to. This is not
     * necessary for the creation of the final image, but useful if you
     * want to inspect each tile in turn.
     * 
     * @param buffer The buffer itself. Must be large enough to contain a random tile
     */
    public final void setTileBuffer(GLPixelBuffer buffer) {
        tileBuffer = buffer;
    }

    /** @see #setTileBuffer(GLPixelBuffer) */
    public final GLPixelBuffer getTileBuffer() { return tileBuffer; }
    
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
    }
    
    /** @see #setImageSize(int, int) */
    public final Dimension getImageSize() { return imageSize; }

    /**
     * Sets the buffer in which to store the final image
     * 
     * @param buffer the buffer itself, must be large enough to hold the final image
     */
    public final void setImageBuffer(GLPixelBuffer buffer) {
        imageBuffer = buffer;
    }

    /** @see #setImageBuffer(GLPixelBuffer) */
    public final GLPixelBuffer getImageBuffer() { return imageBuffer; }
    
    /**
     * Begins rendering a tile.
     * <p>
     * Methods modifies the viewport, hence user shall reset the viewport when finishing tile rendering.
     * </p>
     * <p> 
     * The projection matrix stack should be
     * left alone after calling this method!
     * </p>
     * 
     * @param gl The gl context
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public final void beginTile(GL2ES3 gl, int tX, int tY, int tWidth, int tHeight) throws IllegalStateException, IllegalArgumentException {
        if( 0 >= imageSize.getWidth() || 0 >= imageSize.getHeight() ) {
            throw new IllegalStateException("Image size has not been set");        
        }
        if( null == this.pmvMatrixCB ) {
            throw new IllegalStateException("pmvMatrixCB has not been set");        
        }
        if( 0 > tX || 0 > tX ) {
            throw new IllegalArgumentException("Tile pos must be >= 0/0");        
        }
        if( 0 >= tWidth || 0 >= tHeight ) {
            throw new IllegalArgumentException("Tile size must be > 0x0");        
        }
        
        gl.glViewport( 0, 0, tWidth, tHeight );
        pmvMatrixCB.reshapePMVMatrix(gl, tX, tY, tWidth, tHeight, imageSize.getWidth(), imageSize.getHeight());
        
        this.tX = tX;
        this.tY = tY;
        this.tWidth = tWidth;
        this.tHeight = tHeight;
    }
    
    /**
     * Must be called after rendering the scene
     * 
     * @param gl
     *           the gl context
     * @return true if there are more tiles to be rendered, false if
     *         the final image is complete
     */
    public void endTile( GL2ES3 gl )  {
        if( 0 >= tWidth || 0 >= tHeight ) {
            throw new IllegalStateException("beginTile(..) has not been called");
        }
        
        // be sure OpenGL rendering is finished
        gl.glFlush();

        // save current glPixelStore values
        psm.save(gl);

        final int tmp[] = new int[1];

        if( tileBuffer != null ) {
            final GLPixelAttributes pixelAttribs = tileBuffer.pixelAttributes;
            final int srcX = 0;
            final int srcY = 0;
            final int srcWidth = tWidth;
            final int srcHeight = tHeight;
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
            final int srcX = 0;
            final int srcY = 0;
            final int srcWidth = tWidth;
            final int srcHeight = tHeight;

            /* setup pixel store for glReadPixels */
            final int rowLength = imageSize.getWidth();
            psm.setPackRowLength(gl, rowLength);
            psm.setPackAlignment(gl, 1);

            /* read the tile into the final image */
            final int readPixelSize = GLBuffers.sizeof(gl, tmp, pixelAttribs.bytesPerPixel, srcWidth, srcHeight, 1, true);

            final int ibPos = ( tX + ( tY * rowLength ) ) * pixelAttribs.bytesPerPixel; // skipPixels + skipRows
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

        this.tX = 0;
        this.tY = 0;
        this.tWidth = 0;
        this.tHeight = 0;
    }
    
    /**
     * 
     * <p>
     * Sets the size of the tiles to use in rendering. The actual
     * effective size of the tile depends on the border size, ie (
     * width - 2*border ) * ( height - 2 * border )
     * </p>
     * @param glad
     * @throws IllegalStateException if an {@link GLAutoDrawable} is already attached
     */
    public void attachAutoDrawable(GLAutoDrawable glad, PMVMatrixCallback pmvMatrixCB) throws IllegalStateException {
        if( null != this.glad ) {
            throw new IllegalStateException("GLAutoDrawable already attached");
        }
        this.glad = glad;
        setPMVMatrixCallback(pmvMatrixCB);
        
        final int aSz = glad.getGLEventListenerCount();
        listeners = new GLEventListener[aSz];
        listenersInit = new boolean[aSz];
        for(int i=0; i<aSz; i++) {
            final GLEventListener l = glad.getGLEventListener(0);
            listenersInit[i] = glad.getGLEventListenerInitState(l);
            listeners[i] = glad.removeGLEventListener( l );
        }
        glad.addGLEventListener(tiledGLEL);
    }

    public void detachAutoDrawable() {
        if( null != glad ) {
            glad.removeGLEventListener(tiledGLEL);
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                final GLEventListener l = listeners[i];
                glad.addGLEventListener(l);
                glad.setGLEventListenerInitState(l, listenersInit[i]);
            }
            listeners = null;
            listenersInit = null;
            glad = null;
            pmvMatrixCB = null;
        }
    }

    /**
     * Set {@link GLEventListener} for pre- and post operations when used w/ 
     * {@link #attachAutoDrawable(GLAutoDrawable, int, PMVMatrixCallback)}
     * for each {@link GLEventListener} callback.
     * @param preTile the pre operations
     * @param postTile the post operations
     */
    public void setGLEventListener(GLEventListener preTile, GLEventListener postTile) {
        glEventListenerPre = preTile;
        glEventListenerPost = postTile;
    }
    
    /**
     * Rendering one tile, by simply calling {@link GLAutoDrawable#display()}.
     * 
     * @return true if there are more tiles to be rendered, false if the final image is complete
     * @throws IllegalStateException if no {@link GLAutoDrawable} is {@link #attachAutoDrawable(GLAutoDrawable, int) attached}
     *                               or imageSize is not set
     */
    public void display(int tX, int tY, int tWidth, int tHeight) throws IllegalStateException {
        if( null == glad ) {
            throw new IllegalStateException("No GLAutoDrawable attached");
        }
        this.tX = tX;
        this.tY = tY;
        this.tWidth = tWidth;
        this.tHeight = tHeight;
        glad.display();
    }

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

            beginTile(gl, tX, tY, tWidth, tHeight);

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