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
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import jogamp.opengl.Debug;

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
 * Enhanced for {@link GL} and {@link GL2ES3}, abstracted to suit {@link TileRenderer} and {@link RandomTileRenderer}.
 * </p>
 * <a name="pmvmatrix"><h5>PMV Matrix Considerations</h5></a>
 * <p>
 * The PMV matrix needs to be reshaped in user code
 * after calling {@link #beginTile(GL)}, See {@link #beginTile(GL)}.
 * </p>
 * <p>
 * If {@link #attachAutoDrawable(GLAutoDrawable) attaching to} an {@link GLAutoDrawable},
 * the {@link TileRendererListener#reshapeTile(TileRendererBase, int, int, int, int, int, int)} method
 * is being called after {@link #beginTile(GL)} for each rendered tile.
 * It's implementation shall reshape the PMV matrix according to {@link #beginTile(GL)}.
 * </p>
 * <a name="glprequirement"><h5>GL Profile Requirement</h5></a>
 * <p>
 * Note that {@link #setImageBuffer(GLPixelBuffer) image buffer} can only be used
 * in conjunction w/ a {@link GL} instance &ge; {@link GL2ES3} passed to {@link #beginTile(GL)} and {@link #endTile(GL)}.<br>
 * This is due to setting up the {@link GL2ES3#GL_PACK_ROW_LENGTH pack row length}
 * for an {@link #setImageSize(int, int) image width} != tile-width, which usually is the case.<br>
 * Hence a {@link GLException} is thrown in both methods,
 * if using an {@link #setImageBuffer(GLPixelBuffer) image buffer}
 * and passing a {@link GL} instance &lt; {@link GL2ES3}.
 * </p>
 * <p>
 * Further more, reading back of MSAA buffers is only supported since {@link GL2ES3}
 * since it requires to set the {@link GL2ES3#glReadBuffer(int) read-buffer}.
 * </p>
 *
 * @author ryanm, sgothel
 */
public abstract class TileRendererBase {
    /**
     * The width of the final image. See {@link #getParam(int)}.
     */
    public static final int TR_IMAGE_WIDTH = 1;
    /**
     * The height of the final image. See {@link #getParam(int)}.
     */
    public static final int TR_IMAGE_HEIGHT = 2;
    /**
     * The x-pos of the current tile. See {@link #getParam(int)}.
     */
    public static final int TR_CURRENT_TILE_X_POS = 3;
    /**
     * The y-pos of the current tile. See {@link #getParam(int)}.
     */
    public static final int TR_CURRENT_TILE_Y_POS = 4;
    /**
     * The width of the current tile. See {@link #getParam(int)}.
     */
    public static final int TR_CURRENT_TILE_WIDTH = 5;
    /**
     * The height of the current tile. See {@link #getParam(int)}.
     */
    public static final int TR_CURRENT_TILE_HEIGHT = 6;

    /* pp */ static final boolean DEBUG = Debug.debug("TileRenderer");

    /**
     * Listener for tile renderer events, intended to extend {@link GLEventListener} implementations,
     * enabling tile rendering via {@link TileRendererBase#attachAutoDrawable(GLAutoDrawable)}.
     */
    public static interface TileRendererListener {
        /**
         * The owning {@link GLAutoDrawable} is {@link TileRendererBase#attachAutoDrawable(GLAutoDrawable) attached}
         * to the given {@link TileRendererBase} instance.
         * <p>
         * The {@link GLContext} of the {@link TileRendererBase}'s {@link TileRendererBase#getAttachedDrawable() attached} {@link GLAutoDrawable}
         * <i>is not</i> current.
         * </p>
         * @param tr the associated {@link TileRendererBase}
         * @see TileRendererBase#getAttachedDrawable()
         */
        public void addTileRendererNotify(TileRendererBase tr);

        /**
         * The owning {@link GLAutoDrawable} is {@link TileRendererBase#detachAutoDrawable() detached}
         * from the given {@link TileRendererBase} instance.
         * <p>
         * The {@link GLContext} of the {@link TileRendererBase}'s {@link TileRendererBase#getAttachedDrawable() attached} {@link GLAutoDrawable}
         * <i>is not</i> current.
         * </p>
         * @param tr the disassociated {@link TileRendererBase}
         * @see TileRendererBase#getAttachedDrawable()
         */
        public void removeTileRendererNotify(TileRendererBase tr);

        /**
         * Called by the {@link TileRendererBase} during tile-rendering via an
         * {@link TileRendererBase#getAttachedDrawable() attached} {@link GLAutoDrawable}'s
         * {@link GLAutoDrawable#display()} call for each tile before {@link #display(GLAutoDrawable)}.
         * <p>
         * The <a href="TileRendererBase#pmvmatrix">PMV Matrix</a> shall be reshaped
         * according to the given
         * <ul>
         *   <li>current tile-position</li>
         *   <li>current tile-size</li>
         *   <li>final image-size</li>
         * </ul>
         * The GL viewport is already set to origin 0/0 and the current tile-size.<br>
         * See details in {@link TileRendererBase#beginTile(GL)}.<br>
         * </p>
         * <p>
         * The {@link GLContext} of the {@link TileRendererBase}'s {@link TileRendererBase#getAttachedDrawable() attached} {@link GLAutoDrawable}
         * <i>is</i> current.
         * </p>
         * @param tr the issuing {@link TileRendererBase}
         * @param tileX the {@link TileRendererBase#TR_CURRENT_TILE_X_POS current tile's x-pos}
         * @param tileY the {@link TileRendererBase#TR_CURRENT_TILE_Y_POS current tile's y-pos}
         * @param tileWidth the {@link TileRendererBase#TR_CURRENT_TILE_WIDTH current tile's width}
         * @param tileHeight the {@link TileRendererBase#TR_CURRENT_TILE_HEIGHT current tile's height}
         * @param imageWidth the {@link TileRendererBase#TR_IMAGE_WIDTH final image width}
         * @param imageHeight the {@link TileRendererBase#TR_IMAGE_HEIGHT final image height}
         * @see TileRendererBase#getAttachedDrawable()
         */
        public void reshapeTile(TileRendererBase tr,
                                int tileX, int tileY, int tileWidth, int tileHeight,
                                int imageWidth, int imageHeight);

        /**
         * Called by the {@link TileRendererBase} during tile-rendering
         * after {@link TileRendererBase#beginTile(GL)} and before {@link #reshapeTile(TileRendererBase, int, int, int, int, int, int) reshapeTile(..)}.
         * <p>
         * If {@link TileRendererBase} is of type {@link TileRenderer},
         * method is called for the first tile of all tiles.<br>
         * Otherwise, i.e. {@link RandomTileRenderer}, method is called for each particular tile.
         * </p>
         * <p>
         * The {@link GLContext} of the {@link TileRenderer}'s {@link TileRenderer#getAttachedDrawable() attached} {@link GLAutoDrawable}
         * <i>is</i> current.
         * </p>
         * @param tr the issuing {@link TileRendererBase}
         */
        public void startTileRendering(TileRendererBase tr);

        /**
         * Called by the {@link TileRenderer} during tile-rendering
         * after {@link TileRendererBase#endTile(GL)} and {@link GLAutoDrawable#swapBuffers()}.
         * <p>
         * If {@link TileRendererBase} is of type {@link TileRenderer},
         * method is called for the last tile of all tiles.<br>
         * Otherwise, i.e. {@link RandomTileRenderer}, method is called for each particular tile.
         * </p>
         * <p>
         * The {@link GLContext} of the {@link TileRenderer}'s {@link TileRenderer#getAttachedDrawable() attached} {@link GLAutoDrawable}
         * <i>is</i> current.
         * </p>
         * @param tr the issuing {@link TileRendererBase}
         */
        public void endTileRendering(TileRendererBase tr);
    }

    protected final Dimension imageSize = new Dimension(0, 0);
    protected final GLPixelStorageModes psm = new GLPixelStorageModes();
    protected GLPixelBuffer imageBuffer;
    protected GLPixelBuffer tileBuffer;
    protected boolean beginCalled = false;
    protected int currentTileXPos;
    protected int currentTileYPos;
    protected int currentTileWidth;
    protected int currentTileHeight;
    protected GLAutoDrawable glad;
    protected boolean gladRequiresPreSwap;
    protected boolean gladAutoSwapBufferMode = true;
    protected GLEventListener[] listeners;
    protected boolean[] listenersInit;
    protected GLEventListener glEventListenerPre = null;
    protected GLEventListener glEventListenerPost = null;

    private final String hashStr(final Object o) {
        final int h = null != o ? o.hashCode() : 0;
        return "0x"+Integer.toHexString(h);
    }
    protected StringBuilder tileDetails(final StringBuilder sb) {
        return sb.append("cur "+currentTileXPos+"/"+currentTileYPos+" "+currentTileWidth+"x"+currentTileHeight+", buffer "+hashStr(tileBuffer));
    }
    public StringBuilder toString(final StringBuilder sb) {
        final int gladListenerCount = null != listeners ? listeners.length : 0;
        sb.append("tile[");
        tileDetails(sb);
        sb.append("], image[size "+imageSize+", buffer "+hashStr(imageBuffer)+"], glad["+
                gladListenerCount+" listener, pre "+(null!=glEventListenerPre)+", post "+(null!=glEventListenerPost)+", preSwap "+gladRequiresPreSwap+"]");
        sb.append(", isSetup "+isSetup());
        return sb;
    }
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        return getClass().getSimpleName()+
                "["+toString(sb).toString()+"]";
    }

    protected TileRendererBase() {
    }

    /**
     * Gets the parameters of this TileRenderer object
     *
     * @param pname The parameter name that is to be retrieved
     * @return the value of the parameter
     * @throws IllegalArgumentException if <code>pname</code> is not handled
     */
    public abstract int getParam(int pname) throws IllegalArgumentException;

    /**
     * Specify a buffer the tiles to be copied to. This is not
     * necessary for the creation of the final image, but useful if you
     * want to inspect each tile in turn.
     *
     * @param buffer The buffer itself. Must be large enough to contain a random tile
     */
    public final void setTileBuffer(final GLPixelBuffer buffer) {
        tileBuffer = buffer;
        if( DEBUG ) {
            System.err.println("TileRenderer: tile-buffer "+tileBuffer);
        }
    }

    /** @see #setTileBuffer(GLPixelBuffer) */
    public final GLPixelBuffer getTileBuffer() { return tileBuffer; }

    /**
     * Sets the desired size of the final image
     *
     * @param width The width of the final image
     * @param height The height of the final image
     */
    public void setImageSize(final int width, final int height) {
        imageSize.set(width, height);
    }

    /** @see #setImageSize(int, int) */
    public final DimensionImmutable getImageSize() { return imageSize; }

    /**
     * Sets the buffer in which to store the final image
     *
     * @param buffer the buffer itself, must be large enough to hold the final image
     */
    public final void setImageBuffer(final GLPixelBuffer buffer) {
        imageBuffer = buffer;
        if( DEBUG ) {
            System.err.println("TileRenderer: image-buffer "+imageBuffer);
        }
    }

    /** @see #setImageBuffer(GLPixelBuffer) */
    public final GLPixelBuffer getImageBuffer() { return imageBuffer; }

    /* pp */ final void validateGL(final GL gl) throws GLException {
        if( imageBuffer != null && !gl.isGL2ES3()) {
            throw new GLException("Using image-buffer w/ inssufficient GL context: "+gl.getContext().getGLVersion()+", "+gl.getGLProfile());
        }
    }

    /**
     * Returns true if this instance is setup properly, i.e. {@link #setImageSize(int, int)} ..,
     * and ready for {@link #beginTile(GL)}.
     * Otherwise returns false.
     */
    public abstract boolean isSetup();

    /**
     * Returns true if <i>end of tiling</i> has been reached, otherwise false.
     * <p>
     * <i>end of tiling</i> criteria is implementation specific and may never be reached.
     * </p>
     * <p>
     * User needs to {@link #reset()} tiling after reaching <i>end of tiling</i>
     * before calling {@link #beginTile(GL)} again.
     * </p>
     */
    public abstract boolean eot();

    /**
     * Method resets implementation's internal state to <i>start of tiling</i>
     * as required for {@link #beginTile(GL)} if {@link #eot() end of tiling} has been reached.
     * <p>
     * Implementation is a <i>nop</i> where {@link #eot() end of tiling} is never reached.
     * </p>
     */
    public abstract void reset();

    /**
     * Begins rendering a tile.
     * <p>
     * This method modifies the viewport, see below.
     * User shall reset the viewport when finishing all tile rendering,
     * i.e. after very last call of {@link #endTile(GL)}!
     * </p>
     * <p>
     * The <a href="TileRendererBase.html#pmvmatrix">PMV Matrix</a>
     * must be reshaped after this call using:
     * <ul>
     *   <li>Current Viewport
     *   <ul>
     *      <li>x 0</li>
     *      <li>y 0</li>
     *      <li>{@link #TR_CURRENT_TILE_WIDTH current tile's width}</li>
     *      <li>{@link #TR_CURRENT_TILE_HEIGHT current tile's height}</li>
     *   </ul></li>
     *   <li>{@link #TR_CURRENT_TILE_X_POS current tile's x-pos}</li>
     *   <li>{@link #TR_CURRENT_TILE_Y_POS current tile's y-pos}</li>
     *   <li>{@link #TR_IMAGE_WIDTH final image width}</li>
     *   <li>{@link #TR_IMAGE_HEIGHT final image height}</li>
     * </ul>
     * </p>
     * <p>
     * Use shall render the scene afterwards, concluded with a call to
     * this renderer {@link #endTile(GL)}.
     * </p>
     * <p>
     * User has to comply with the <a href="TileRendererBase.html#glprequirement">GL profile requirement</a>.
     * </p>
     * <p>
     * If {@link #eot() end of tiling} has been reached,
     * user needs to {@link #reset()} tiling before calling this method.
     * </p>
     *
     * @param gl The gl context
     * @throws IllegalStateException if {@link #setImageSize(int, int) image-size} is undefined,
     *         an {@link #isSetup() implementation related setup} has not be performed
     *         or {@ link #eot()} has been reached. See implementing classes.
     * @throws GLException if {@link #setImageBuffer(GLPixelBuffer) image buffer} is used but <code>gl</code> instance is &lt; {@link GL2ES3}
     * @see #isSetup()
     * @see #eot()
     * @see #reset()
     */
    public abstract void beginTile(GL gl) throws IllegalStateException, GLException;

    /**
     * Must be called after rendering the scene,
     * see {@link #beginTile(GL)}.
     * <p>
     * Please consider {@link #reqPreSwapBuffers(GLCapabilitiesImmutable)} to determine
     * whether you need to perform {@link GLDrawable#swapBuffers() swap-buffers} before or after
     * calling this method!
     * </p>
     * <p>
     * User has to comply with the <a href="TileRendererBase.html#glprequirement">GL profile requirement</a>.
     * </p>
     *
     * @param gl the gl context
     * @throws IllegalStateException if beginTile(gl) has not been called
     * @throws GLException if {@link #setImageBuffer(GLPixelBuffer) image buffer} is used but <code>gl</code> instance is &lt; {@link GL2ES3}
     */
    public abstract void endTile( GL gl ) throws IllegalStateException, GLException;

    /**
     * Determines whether the chosen {@link GLCapabilitiesImmutable}
     * requires a <i>pre-{@link GLDrawable#swapBuffers() swap-buffers}</i>
     * before accessing the results, i.e. before {@link #endTile(GL)}.
     * <p>
     * See {@link GLDrawableUtil#swapBuffersBeforeRead(GLCapabilitiesImmutable)}.
     * </p>
     */
    public final boolean reqPreSwapBuffers(final GLCapabilitiesImmutable chosenCaps) {
        return GLDrawableUtil.swapBuffersBeforeRead(chosenCaps);
    }

    /**
     * Attaches the given {@link GLAutoDrawable} to this tile renderer.
     * <p>
     * The {@link GLAutoDrawable}'s original {@link GLEventListener} are moved to this tile renderer.
     * </p>
     * <p>
     * {@link GLEventListeners} not implementing {@link TileRendererListener} are ignored while tile rendering.
     * </p>
     * <p>
     * The {@link GLAutoDrawable}'s {@link GLAutoDrawable#getAutoSwapBufferMode() auto-swap mode} is cached
     * and set to <code>false</code>, since {@link GLAutoDrawable#swapBuffers() swapBuffers()} maybe issued before {@link #endTile(GL)},
     * see {@link #reqPreSwapBuffers(GLCapabilitiesImmutable)}.
     * </p>
     * <p>
     * This tile renderer's internal {@link GLEventListener} is then added to the attached {@link GLAutoDrawable}
     * to handle the tile rendering, replacing the original {@link GLEventListener}.<br>
     * It's {@link GLEventListener#display(GLAutoDrawable) display} implementations issues:
     * <ul>
     *   <li>Optional {@link #setGLEventListener(GLEventListener, GLEventListener) pre-glel}.{@link GLEventListener#display(GLAutoDrawable) display(..)}</li>
     *   <li>{@link #beginTile(GL)}</li>
     *   <li>for all original {@link TileRendererListener}:
     *   <ul>
     *     <li>{@link TileRendererListener#reshapeTile(TileRendererBase, int, int, int, int, int, int) reshapeTile(tileX, tileY, tileWidth, tileHeight, imageWidth, imageHeight)}</li>
     *     <li>{@link GLEventListener#display(GLAutoDrawable) display(autoDrawable)}</li>
     *   </ul></li>
     *   <li>if ( {@link #reqPreSwapBuffers(GLCapabilitiesImmutable) pre-swap} ) { {@link GLAutoDrawable#swapBuffers() swapBuffers()} }</li>
     *   <li>{@link #endTile(GL)}</li>
     *   <li>if ( !{@link #reqPreSwapBuffers(GLCapabilitiesImmutable) pre-swap} ) { {@link GLAutoDrawable#swapBuffers() swapBuffers()} }</li>
     *   <li>Optional {@link #setGLEventListener(GLEventListener, GLEventListener) post-glel}.{@link GLEventListener#display(GLAutoDrawable) display(..)}</li>
     * </ul>
     * </p>
     * <p>
     * Consider using {@link #setGLEventListener(GLEventListener, GLEventListener)} to add pre- and post
     * hooks to be performed on this renderer {@link GLEventListener}.<br>
     * The pre-hook is able to allocate memory and setup parameters, since it's called before {@link #beginTile(GL)}.<br>
     * The post-hook is able to use the rendering result and can even shutdown tile-rendering,
     * since it's called after {@link #endTile(GL)}.
     * </p>
     * <p>
     * Call {@link #detachAutoDrawable()} to remove the attached {@link GLAutoDrawable} from this tile renderer
     * and to restore it's original {@link GLEventListener}.
     * </p>
     * @param glad the {@link GLAutoDrawable} to attach.
     * @throws IllegalStateException if an {@link GLAutoDrawable} is already attached
     * @see #getAttachedDrawable()
     * @see #detachAutoDrawable()
     */
    public final void attachAutoDrawable(final GLAutoDrawable glad) throws IllegalStateException {
        if( null != this.glad ) {
            throw new IllegalStateException("GLAutoDrawable already attached");
        }
        this.glad = glad;

        final int aSz = glad.getGLEventListenerCount();
        listeners = new GLEventListener[aSz];
        listenersInit = new boolean[aSz];
        for(int i=0; i<aSz; i++) {
            final GLEventListener l = glad.getGLEventListener(0);
            listenersInit[i] = glad.getGLEventListenerInitState(l);
            listeners[i] = glad.removeGLEventListener( l );
            final boolean trn;
            if( listeners[i] instanceof TileRendererListener ) {
                trn = true;
                ((TileRendererListener)listeners[i]).addTileRendererNotify(this);
            } else {
                trn = false;
            }
            if( DEBUG ) {
                System.err.println("TileRenderer.attach["+i+"]: isInit "+listenersInit[i]+", isTRN "+trn+", "+listeners[i].getClass().getName());
            }
        }
        glad.addGLEventListener(tiledGLEL);
        gladAutoSwapBufferMode = glad.getAutoSwapBufferMode();
        gladRequiresPreSwap = this.reqPreSwapBuffers(glad.getChosenGLCapabilities());
        glad.setAutoSwapBufferMode(false);
        if( DEBUG ) {
            System.err.println("TileRenderer: attached: "+glad);
            System.err.println("TileRenderer: preSwap "+gladRequiresPreSwap+", "+glad.getChosenGLCapabilities()+", cached "+listeners.length+" listener");
        }
    }

    /**
     * Returns a previously {@link #attachAutoDrawable(GLAutoDrawable) attached} {@link GLAutoDrawable},
     * <code>null</code> if none is attached.
     * <p>
     * If called from {@link TileRendererListener#addTileRendererNotify(TileRendererBase)}
     * or {@link TileRendererListener#removeTileRendererNotify(TileRendererBase)}, method returns the
     * just attached or soon to be detached {@link GLAutoDrawable}.
     * </p>
     * @see #attachAutoDrawable(GLAutoDrawable)
     * @see #detachAutoDrawable()
     */
    public final GLAutoDrawable getAttachedDrawable() {
        return glad;
    }

    /**
     * Detaches the given {@link GLAutoDrawable} from this tile renderer.
     * @see #attachAutoDrawable(GLAutoDrawable)
     * @see #getAttachedDrawable()
     */
    public final void detachAutoDrawable() {
        if( null != glad ) {
            glad.removeGLEventListener(tiledGLEL);
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                final GLEventListener l = listeners[i];
                if( l instanceof TileRendererListener ) {
                    ((TileRendererListener)l).removeTileRendererNotify(this);
                }
                glad.addGLEventListener(l);
                glad.setGLEventListenerInitState(l, listenersInit[i]);
            }
            glad.setAutoSwapBufferMode(gladAutoSwapBufferMode);
            if( DEBUG ) {
                System.err.println("TileRenderer: detached: "+glad);
                System.err.println("TileRenderer: "+glad.getChosenGLCapabilities());
            }

            listeners = null;
            listenersInit = null;
            glad = null;
        }
    }

    /**
     * Set {@link GLEventListener} for pre- and post operations when used w/
     * {@link #attachAutoDrawable(GLAutoDrawable)}
     * for each {@link GLEventListener} callback.
     * @param preTile the pre operations
     * @param postTile the post operations
     */
    public final void setGLEventListener(final GLEventListener preTile, final GLEventListener postTile) {
        glEventListenerPre = preTile;
        glEventListenerPost = postTile;
    }

    /**
     * Rendering one tile, by simply calling {@link GLAutoDrawable#display()}.
     *
     * @throws IllegalStateException if no {@link GLAutoDrawable} is {@link #attachAutoDrawable(GLAutoDrawable) attached}
     *                               or imageSize is not set
     */
    public final void display() throws IllegalStateException {
        if( null == glad ) {
            throw new IllegalStateException("No GLAutoDrawable attached");
        }
        glad.display();
    }

    private final GLEventListener tiledGLEL = new GLEventListener() {
        final TileRenderer tileRenderer = TileRendererBase.this instanceof TileRenderer ? (TileRenderer) TileRendererBase.this : null;

        @Override
        public void init(final GLAutoDrawable drawable) {
            if( null != glEventListenerPre ) {
                glEventListenerPre.init(drawable);
            }
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                final GLEventListener l = listeners[i];
                if( !listenersInit[i] && l instanceof TileRendererListener ) {
                    l.init(drawable);
                    listenersInit[i] = true;
                }
            }
            if( null != glEventListenerPost ) {
                glEventListenerPost.init(drawable);
            }
        }
        @Override
        public void dispose(final GLAutoDrawable drawable) {
            if( null != glEventListenerPre ) {
                glEventListenerPre.dispose(drawable);
            }
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) { // dispose all GLEventListener, last chance!
                listeners[i].dispose(drawable);
            }
            if( null != glEventListenerPost ) {
                glEventListenerPost.dispose(drawable);
            }
        }
        @Override
        public void display(final GLAutoDrawable drawable) {
            if( null != glEventListenerPre ) {
                glEventListenerPre.reshape(drawable, 0, 0, currentTileWidth, currentTileHeight);
                glEventListenerPre.display(drawable);
            }
            if( !isSetup() ) {
                if( DEBUG ) {
                    System.err.println("TileRenderer.glel.display: !setup: "+TileRendererBase.this);
                }
                return;
            }
            if( eot() ) {
                if( DEBUG ) {
                    System.err.println("TileRenderer.glel.display: EOT: "+TileRendererBase.this);
                }
                return;
            }
            final GL gl = drawable.getGL();

            beginTile(gl);

            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                final GLEventListener l = listeners[i];
                if( l instanceof TileRendererListener ) {
                    final TileRendererListener tl = (TileRendererListener)l;
                    if( null == tileRenderer || 0 == tileRenderer.getCurrentTile() ) {
                        tl.startTileRendering(TileRendererBase.this);
                    }
                    tl.reshapeTile(TileRendererBase.this,
                                   currentTileXPos, currentTileYPos, currentTileWidth, currentTileHeight,
                                   imageSize.getWidth(), imageSize.getHeight());
                    l.display(drawable);
                }
            }

            if( gladRequiresPreSwap ) {
                glad.swapBuffers();
                endTile(gl);
            } else {
                endTile(gl);
                glad.swapBuffers();
            }
            if( null == tileRenderer || tileRenderer.eot() ) {
                for(int i=0; i<aSz; i++) {
                    final GLEventListener l = listeners[i];
                    if( l instanceof TileRendererListener ) {
                        ((TileRendererListener)l).endTileRendering(TileRendererBase.this);
                    }
                }
            }
            if( null != glEventListenerPost ) {
                glEventListenerPost.reshape(drawable, 0, 0, currentTileWidth, currentTileHeight);
                glEventListenerPost.display(drawable);
            }
        }
        @Override
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
    };
}