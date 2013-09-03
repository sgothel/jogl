package com.jogamp.opengl.util;

import javax.media.opengl.GL2ES3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

/**
 * See {@link TileRenderer}.
 * <p>
 * Enhanced for {@link GLAutoDrawable} usage.
 * </p>
 */
public class TileRenderer2 extends TileRenderer {
    private GLAutoDrawable glad;
    private GLEventListener[] listeners;
    private boolean[] listenersInit;    

    /**
     * Creates a new TileRenderer object
     */
    public TileRenderer2() {
        glad = null;
        listeners = null;
        listenersInit = null;
    }

    /**
     * 
     * <p>
     * Sets the size of the tiles to use in rendering. The actual
     * effective size of the tile depends on the border size, ie (
     * width - 2*border ) * ( height - 2 * border )
     * </p>
     * @param glad
     * @param border
     *           The width of the borders on each tile. This is needed
     *           to avoid artifacts when rendering lines or points with
     *           thickness > 1.
     * @throws IllegalStateException if an {@link GLAutoDrawable} is already attached
     */
    public void attachAutoDrawable(GLAutoDrawable glad, int border, PMVMatrixCallback pmvMatrixCB) throws IllegalStateException {
        if( null != this.glad ) {
            throw new IllegalStateException("GLAutoDrawable already attached");
        }
        this.glad = glad;
        setTileSize(glad.getWidth(), glad.getHeight(), border);
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
     * Rendering one tile, by simply calling {@link GLAutoDrawable#display()}.
     * 
     * @return true if there are more tiles to be rendered, false if the final image is complete
     * @throws IllegalStateException if no {@link GLAutoDrawable} is {@link #attachAutoDrawable(GLAutoDrawable, int) attached}
     *                               or imageSize is not set
     */
    public boolean display() throws IllegalStateException {
        if( null == glad ) {
            throw new IllegalStateException("No GLAutoDrawable attached");
        }
        glad.display();
        return !eot();
    }

    private final GLEventListener tiledGLEL = new GLEventListener() {
        @Override
        public void init(GLAutoDrawable drawable) {
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                final GLEventListener l = listeners[i];
                l.init(drawable);
                listenersInit[i] = true;
            }
        }
        @Override
        public void dispose(GLAutoDrawable drawable) {
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                listeners[i].dispose(drawable);
            }
        }
        @Override
        public void display(GLAutoDrawable drawable) {
            final GL2ES3 gl = drawable.getGL().getGL2ES3();
            
            beginTile(gl);
            
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                listeners[i].display(drawable);
            }
            
            endTile(gl);
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            final int aSz = listenersInit.length;
            for(int i=0; i<aSz; i++) {
                listeners[i].reshape(drawable, x, y, width, height);
            }
        }
    };
}
