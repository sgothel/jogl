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
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

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
 * Enhanced for {@link GL2ES3}, abstracted to suit {@link TileRenderer} and {@link RandomTileRenderer}.
 * </p>
 * 
 * @author ryanm, sgothel
 */
public abstract class TileRendererBase {
    /**
     * The width of the final image
     */
    public static final int TR_IMAGE_WIDTH = 1;
    /**
     * The height of the final image
     */
    public static final int TR_IMAGE_HEIGHT = 2;
    /**
     * The width of the current tile
     */
    public static final int TR_CURRENT_TILE_X_POS = 3;
    /**
     * The height of the current tile
     */
    public static final int TR_CURRENT_TILE_Y_POS = 4;
    /**
     * The width of the current tile
     */
    public static final int TR_CURRENT_TILE_WIDTH = 5;
    /**
     * The height of the current tile
     */
    public static final int TR_CURRENT_TILE_HEIGHT = 6;
    
    protected final Dimension imageSize = new Dimension(0, 0);
    protected final GLPixelStorageModes psm = new GLPixelStorageModes();
    protected GLPixelBuffer imageBuffer;
    protected GLPixelBuffer tileBuffer;
    protected PMVMatrixCallback pmvMatrixCB = null;
    protected boolean beginCalled = false;
    protected int currentTileXPos;
    protected int currentTileYPos;
    protected int currentTileWidth;
    protected int currentTileHeight;
    protected GLAutoDrawable glad;
    protected GLEventListener[] listeners;
    protected boolean[] listenersInit;
    protected GLEventListener glEventListenerPre = null;
    protected GLEventListener glEventListenerPost = null;

    public static interface PMVMatrixCallback {
        void reshapePMVMatrix(GL gl, int tileX, int tileY, int tileWidth, int tileHeight, int imageWidth, int imageHeight);      
    }

    protected TileRendererBase() {
    }

    /**
     * Gets the parameters of this TileRenderer object
     * 
     * @param param The parameter that is to be retrieved
     * @return the value of the parameter
     * @throws IllegalArgumentException if <code>param</code> is not handled
     */
    public abstract int getParam(int param) throws IllegalArgumentException;
    
    /**
     * @param pmvMatrixCB
     * @throws IllegalArgumentException if <code>pmvMatrixCB</code> is null
     */
    public final void setPMVMatrixCallback(PMVMatrixCallback pmvMatrixCB) throws IllegalArgumentException {
        if( null == pmvMatrixCB ) {
            throw new IllegalArgumentException("pmvMatrixCB is null");
        }
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
     * @param width The width of the final image
     * @param height The height of the final image
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
     * Methods modifies the viewport.
     * User shall reset the viewport when finishing tile rendering,
     * i.e. after last call of {@link #endTile(GL2ES3)}!
     * </p>
     * <p> 
     * The projection matrix stack should be
     * left alone after calling this method!
     * </p>
     * <p>
     * Use shall render the scene afterwards, concluded with a call to
     * this renderer {@link #endTile(GL2ES3)}. 
     * </p>
     * 
     * @param gl The gl context
     * @throws IllegalStateException if image-size or pmvMatrixCB has not been set
     */
    public abstract void beginTile(GL2ES3 gl) throws IllegalStateException;
    
    /**
     * Must be called after rendering the scene,
     * see {@link #beginTile(GL2ES3)}.
     * 
     * @param gl the gl context
     * @throws IllegalStateException if beginTile(gl) has not been called
     */
    public abstract void endTile( GL2ES3 gl ) throws IllegalStateException;
    
    /**
     * Attaches this renderer to the {@link GLAutoDrawable}.
     * <p>
     * The {@link GLAutoDrawable}'s {@link GLEventListener} are removed first and stored locally.
     * This renderer {@link GLEventListener} is then added to handle the tile rendering
     * for the original {@link GLEventListener}.
     * </p>
     * <p>
     * Call {@link #detachFromAutoDrawable()} to remove this renderer from the {@link GLAutoDrawable} 
     * and to restore it's original {@link GLEventListener}.
     * </p>
     * @param glad
     * @throws IllegalStateException if an {@link GLAutoDrawable} is already attached
     */
    public void attachToAutoDrawable(GLAutoDrawable glad, PMVMatrixCallback pmvMatrixCB) throws IllegalStateException {
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
        glad.addGLEventListener(getTiledGLEL());
    }

    /**
     * Detaches this renderer from the {@link GLAutoDrawable}.
     * <p>
     * See {@link #attachToAutoDrawable(GLAutoDrawable, PMVMatrixCallback)}.
     * </p>
     */
    public void detachFromAutoDrawable() {
        if( null != glad ) {
            glad.removeGLEventListener(getTiledGLEL());
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
    
    /** Return this rendere {@link GLEventListener} implementation. */
    protected abstract GLEventListener getTiledGLEL();

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
     * @throws IllegalStateException if no {@link GLAutoDrawable} is {@link #attachToAutoDrawable(GLAutoDrawable, int) attached}
     *                               or imageSize is not set
     */
    public void display() throws IllegalStateException {
        if( null == glad ) {
            throw new IllegalStateException("No GLAutoDrawable attached");
        }
        glad.display();
    }
}