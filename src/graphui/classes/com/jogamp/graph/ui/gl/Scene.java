/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui.gl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.common.nio.Buffers;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.newt.event.GestureHandler;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.PinchToZoomGesture;
import com.jogamp.newt.event.GestureHandler.GestureEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Ray;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * GraphUI Scene
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * GraphUI is intended to become an immediate- and retained-mode API.
 * </p>
 * <p>
 * To utilize a Scene instance directly as a {@link GLEventListener},
 * user needs to {@link #setClearParams(float[], int)}.
 *
 * Otherwise user may just call provided {@link GLEventListener} from within their own workflow
 * - {@link GLEventListener#init(GLAutoDrawable)}
 * - {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int)}
 * - {@link GLEventListener#display(GLAutoDrawable)}
 * - {@link GLEventListener#dispose(GLAutoDrawable)}
 * </p>
 * <p>
 * {@link #setPMVMatrixSetup(PMVMatrixSetup)} maybe used to provide a custom {@link PMVMatrix} setup.
 * </p>
 * @see Shape
 */
public final class Scene implements GLEventListener {
    /** Default scene distance on z-axis to projection is -1/5f. */
    public static final float DEFAULT_SCENE_DIST = -1/5f;
    /** Default projection angle in degrees value is 45.0. */
    public static final float DEFAULT_ANGLE = 45.0f;
    /** Default projection z-near value is 0.1. */
    public static final float DEFAULT_ZNEAR = 0.1f;
    /** Default projection z-far value is 7000. */
    public static final float DEFAULT_ZFAR = 7000.0f;

    @SuppressWarnings("unused")
    private static final boolean DEBUG = false;

    private final ArrayList<Shape> shapes = new ArrayList<Shape>();

    private float[] clearColor = null;
    private int clearMask;

    private final RegionRenderer renderer;

    private final int[] sampleCount = new int[1];

    /** Describing the bounding box in shape's object model-coordinates of the near-plane parallel at its scene-distance, post {@link #translate(PMVMatrix)} */
    private final AABBox planeBox = new AABBox(0f, 0f, 0f, 0f, 0f, 0f);

    private volatile Shape activeShape = null;

    private SBCMouseListener sbcMouseListener = null;
    private SBCGestureListener sbcGestureListener = null;
    private PinchToZoomGesture pinchToZoomGesture = null;

    private GLAutoDrawable cDrawable = null;

    private static RegionRenderer createRenderer() {
        return RegionRenderer.create(RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
    }

    /**
     * Create a new scene with an internally created RegionRenderer
     * and using default values {@link #DEFAULT_SCENE_DIST}, {@link #DEFAULT_ANGLE}, {@link #DEFAULT_ZNEAR} and {@link #DEFAULT_ZFAR}.
     */
    public Scene() {
        this(createRenderer());
    }

    /**
     * Create a new scene taking ownership of the given RegionRenderer
     * and using default values {@link #DEFAULT_SCENE_DIST}, {@link #DEFAULT_ANGLE}, {@link #DEFAULT_ZNEAR} and {@link #DEFAULT_ZFAR}.
     */
    public Scene(final RegionRenderer renderer) {
        if( null == renderer ) {
            throw new IllegalArgumentException("Null RegionRenderer");
        }
        this.renderer = renderer;
        this.sampleCount[0] = 4;
    }

    /** Returns the associated RegionRenderer */
    public RegionRenderer getRenderer() { return renderer; }

    /** Returns the associated RegionRenderer's RenderState. */
    public RenderState getRenderState() { return renderer.getRenderState(); }

    /**
     * Sets the clear parameter for {@link GL#glClearColor(float, float, float, float) glClearColor(..)} and {@link GL#glClear(int) glClear(..)}
     * to be issued at {@link #display(GLAutoDrawable)}.
     *
     * Without setting these parameter, user has to issue
     * {@link GL#glClearColor(float, float, float, float) glClearColor(..)} and {@link GL#glClear(int) glClear(..)}
     * before calling {@link #display(GLAutoDrawable)}.
     *
     * @param clearColor {@link GL#glClearColor(float, float, float, float) glClearColor(..)} arguments
     * @param clearMask {@link GL#glClear(int) glClear(..)} mask, default is {@link GL#GL_COLOR_BUFFER_BIT} | {@link GL#GL_DEPTH_BUFFER_BIT}
     */
    public final void setClearParams(final float[] clearColor, final int clearMask) { this.clearColor = clearColor; this.clearMask = clearMask; }

    /** Returns the {@link GL#glClearColor(float, float, float, float) glClearColor(..)} arguments, see {@link #setClearParams(float[], int)}. */
    public final float[] getClearColor() { return clearColor; }

    /** Returns the {@link GL#glClear(int) glClear(..)} mask, see {@link #setClearParams(float[], int)}. */
    public final int getClearMask() { return clearMask; }

    public void attachInputListenerTo(final GLWindow window) {
        if(null == sbcMouseListener) {
            sbcMouseListener = new SBCMouseListener();
            window.addMouseListener(sbcMouseListener);
            sbcGestureListener = new SBCGestureListener();
            window.addGestureListener(sbcGestureListener);
            pinchToZoomGesture = new PinchToZoomGesture(window.getNativeSurface(), false);
            window.addGestureHandler(pinchToZoomGesture);
        }
    }

    public void detachInputListenerFrom(final GLWindow window) {
        if(null != sbcMouseListener) {
            window.removeMouseListener(sbcMouseListener);
            sbcMouseListener = null;
            window.removeGestureListener(sbcGestureListener);
            sbcGestureListener = null;
            window.removeGestureHandler(pinchToZoomGesture);
            pinchToZoomGesture = null;
        }
    }

    public ArrayList<Shape> getShapes() {
        return shapes;
    }
    public void addShape(final Shape b) {
        shapes.add(b);
    }
    public void removeShape(final Shape b) {
        shapes.remove(b);
    }
    public void addShapes(final Collection<? extends Shape> shapes) {
        this.shapes.addAll(shapes);
    }
    public void removeShapes(final Collection<? extends Shape> shapes) {
        this.shapes.removeAll(shapes);
    }
    public Shape getShapeByIdx(final int id) {
        if( 0 > id ) {
            return null;
        }
        return shapes.get(id);
    }
    public Shape getShapeByName(final int name) {
        for(final Shape b : shapes) {
            if(b.getName() == name ) {
                return b;
            }
        }
        return null;
    }

    public int getSampleCount() { return sampleCount[0]; }
    public int setSampleCount(final int v) {
        sampleCount[0] = Math.min(8, Math.max(v, 0)); // clip
        markAllShapesDirty();
        return sampleCount[0];
    }

    public void setAllShapesQuality(final int q) {
        for(int i=0; i<shapes.size(); i++) {
            shapes.get(i).setQuality(q);
        }
    }
    public void setAllShapesSharpness(final float sharpness) {
        for(int i=0; i<shapes.size(); i++) {
            shapes.get(i).setSharpness(sharpness);
        }
    }
    public void markAllShapesDirty() {
        for(int i=0; i<shapes.size(); i++) {
            shapes.get(i).markShapeDirty();
        }
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        cDrawable = drawable;
        renderer.init(drawable.getGL().getGL2ES2());
    }

    /**
     * Reshape scene using {@link #setupMatrix(PMVMatrix, int, int, int, int)} using {@link PMVMatrixSetup}.
     * <p>
     * {@inheritDoc}
     * </p>
     * @see PMVMatrixSetup
     * @see #setPMVMatrixSetup(PMVMatrixSetup)
     * @see #setupMatrix(PMVMatrix, int, int, int, int)
     * @see #getBounds()
     * @see #getBoundsCenter()
     */
    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        renderer.reshapeNotify(x, y, width, height);

        setupMatrix(renderer.getMatrix(), x, y, width, height);
        pmvMatrixSetup.setPlaneBox(planeBox, renderer.getMatrix(), x, y, width, height);
    }

    private static Comparator<Shape> shapeZAscComparator = new Comparator<Shape>() {
        @Override
        public int compare(final Shape s1, final Shape s2) {
            final float s1Z = s1.getBounds().getMinZ()+s1.getPosition()[2];
            final float s2Z = s2.getBounds().getMinZ()+s2.getPosition()[2];
            if( FloatUtil.isEqual(s1Z, s2Z, FloatUtil.EPSILON) ) {
                return 0;
            } else if( s1Z < s2Z ){
                return -1;
            } else {
                return 1;
            }
        } };

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void display(final GLAutoDrawable drawable) {
        final Object[] shapesS = shapes.toArray();
        Arrays.sort(shapesS, (Comparator)shapeZAscComparator);

        display(drawable, shapesS, false);
    }

    private static final int[] sampleCountGLSelect = { -1 };

    private void display(final GLAutoDrawable drawable, final Object[] shapesS, final boolean glSelect) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        final int[] sampleCount0;
        if( glSelect ) {
            gl.glClearColor(0f, 0f, 0f, 1f);
            sampleCount0 = sampleCountGLSelect;
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        } else {
            if( null != clearColor ) {
                gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
                gl.glClear(clearMask);
            }
            sampleCount0 = sampleCount;
        }

        final PMVMatrix pmv = renderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

        if( glSelect ) {
            renderer.enable(gl, true, RegionRenderer.defaultBlendDisable, RegionRenderer.defaultBlendDisable);
        } else {
            renderer.enable(gl, true);
        }

        //final int shapeCount = shapes.size();
        final int shapeCount = shapesS.length;
        for(int i=0; i<shapeCount; i++) {
            // final UIShape uiShape = shapes.get(i);
            final Shape uiShape = (Shape)shapesS[i];
            // System.err.println("Id "+i+": "+uiShape);
            if( uiShape.isEnabled() ) {
                pmv.glPushMatrix();
                uiShape.setTransform(pmv);
                if( glSelect ) {
                    final float color = ( i + 1f ) / ( shapeCount + 2f );
                    // FIXME
                    // System.err.printf("drawGL: color %f, index %d of [0..%d[%n", color, i, shapeCount);
                    renderer.getRenderState().setColorStatic(color, color, color, 1f);
                    uiShape.drawGLSelect(gl, renderer, sampleCount0);
                } else {
                    uiShape.draw(gl, renderer, sampleCount0);
                }
                pmv.glPopMatrix();
            }
        }
        if( glSelect ) {
            renderer.enable(gl, false, RegionRenderer.defaultBlendDisable, RegionRenderer.defaultBlendDisable);
        } else {
            renderer.enable(gl, false);
        }
        synchronized ( syncDisplayedOnce ) {
            displayedOnce = true;
            syncDisplayedOnce.notifyAll();
        }
    }

    private volatile boolean displayedOnce = false;
    private final Object syncDisplayedOnce = new Object();

    /** Blocks until first {@link #display(GLAutoDrawable)} has completed after construction or {@link #dispose(GLAutoDrawable). */
    public void waitUntilDisplayed() {
        synchronized( syncDisplayedOnce ) {
            while( !displayedOnce ) {
                try {
                    syncDisplayedOnce.wait();
                } catch (final InterruptedException e) { }
            }
        }
    }

    /**
     * Disposes all {@link #addShape(Shape) added} {@link Shape}s.
     * <p>
     * Implementation also issues {@link RegionRenderer#destroy(GL2ES2)} if set
     * and {@link #detachInputListenerFrom(GLWindow)} in case the drawable is of type {@link GLWindow}.
     * </p>
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void dispose(final GLAutoDrawable drawable) {
        synchronized ( syncDisplayedOnce ) {
            displayedOnce = false;
            syncDisplayedOnce.notifyAll();
        }
        if( drawable instanceof GLWindow ) {
            final GLWindow glw = (GLWindow) drawable;
            detachInputListenerFrom(glw);
        }
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        for(int i=0; i<shapes.size(); i++) {
            shapes.get(i).destroy(gl, renderer);
        }
        shapes.clear();
        cDrawable = null;
        renderer.destroy(gl);
    }

    /**
     * Attempt to pick a {@link Shape} using the window coordinates and contained {@ling Shape}'s {@link AABBox} {@link Shape#getBounds() bounds}
     * using a ray-intersection algorithm.
     * <p>
     * If {@link Shape} was found the given action is performed.
     * </p>
     * <p>
     * Method performs on current thread and returns after probing every {@link Shape}.
     * </p>
     * @param glWinX window X coordinate, bottom-left origin
     * @param glWinY window Y coordinate, bottom-left origin
     * @param pmv a new {@link PMVMatrix} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix, int, int, int, int) be setup},
     *            {@link Shape#setTransform(PMVMatrix) shape-transformed} and can be reused by the caller and runnable.
     * @param objPos storage for found object position in model-space of found {@link Shape}
     * @param shape storage for found {@link Shape} or null
     * @param runnable the action to perform if {@link Shape} was found
     */
    public Shape pickShape(final int glWinX, final int glWinY, final PMVMatrix pmv, final float[] objPos, final Shape[] shape, final Runnable runnable) {
        shape[0] = pickShapeImpl(glWinX, glWinY, pmv, objPos);
        if( null != shape[0] ) {
            runnable.run();
        }
        return shape[0];
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Shape pickShapeImpl(final int glWinX, final int glWinY, final PMVMatrix pmv, final float[] objPos) {
        final float winZ0 = 0f;
        final float winZ1 = 0.3f;
        /**
            final FloatBuffer winZRB = Buffers.newDirectFloatBuffer(1);
            gl.glReadPixels( x, y, 1, 1, GL2ES2.GL_DEPTH_COMPONENT, GL.GL_FLOAT, winZRB);
            winZ1 = winZRB.get(0); // dir
        */
        setupMatrix(pmv);

        final Ray ray = new Ray();

        final Object[] shapesS = shapes.toArray();
        Arrays.sort(shapesS, (Comparator)shapeZAscComparator);

        for(int i=shapesS.length-1; i>=0; i--) {
            final Shape uiShape = (Shape)shapesS[i];

            if( uiShape.isEnabled() ) {
                pmv.glPushMatrix();
                uiShape.setTransform(pmv);
                final boolean ok = pmv.gluUnProjectRay(glWinX, glWinY, winZ0, winZ1, getViewport(), 0, ray);
                if( ok ) {
                    final AABBox sbox = uiShape.getBounds();
                    if( sbox.intersectsRay(ray) ) {
                        // System.err.printf("Pick.0: shape %d, [%d, %d, %f/%f] -> %s%n", i, glWinX, glWinY, winZ0, winZ1, ray);
                        if( null == sbox.getRayIntersection(objPos, ray, FloatUtil.EPSILON, true, dpyTmp1V3, dpyTmp2V3, dpyTmp3V3) ) {
                            throw new InternalError("Ray "+ray+", box "+sbox);
                        }
                        // System.err.printf("Pick.1: shape %d @ [%f, %f, %f], within %s%n", i, objPos[0], objPos[1], objPos[2], uiShape.getBounds());
                        return uiShape;
                    }
                }
                pmv.glPopMatrix(); // we leave the stack open if picked above, allowing the modelview shape transform to be reused
            }
        }
        return null;
    }
    private final float[] dpyTmp1V3 = new float[3];
    private final float[] dpyTmp2V3 = new float[3];
    private final float[] dpyTmp3V3 = new float[3];

    /**
     * Attempt to pick a {@link Shape} using the OpenGL false color rendering.
     * <p>
     * If {@link Shape} was found the given action is performed on the rendering thread.
     * </p>
     * <p>
     * Method is non blocking and performs on rendering-thread, it returns immediately.
     * </p>
     * @param glWinX window X coordinate, bottom-left origin
     * @param glWinY window Y coordinate, bottom-left origin
     * @param objPos storage for found object position in model-space of found {@link Shape}
     * @param shape storage for found {@link Shape} or null
     * @param runnable the action to perform if {@link Shape} was found
     */
    public void pickShapeGL(final int glWinX, final int glWinY, final float[] objPos, final Shape[] shape, final Runnable runnable) {
        if( null == cDrawable ) {
            return;
        }
        cDrawable.invoke(false, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable drawable) {
                final Shape s = pickShapeGLImpl(drawable, glWinX, glWinY);
                shape[0] = s;
                if( null != s ) {
                    final PMVMatrix pmv = renderer.getMatrix();
                    pmv.glPushMatrix();
                    s.setTransform(pmv);
                    final boolean ok = null != shape[0].winToShapeCoord(getMatrix(), getViewport(), glWinX, glWinY, objPos);
                    pmv.glPopMatrix();
                    if( ok ) {
                        runnable.run();
                    }
                }
                return false; // needs to re-render to wash away our false-color glSelect
            } } );
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Shape pickShapeGLImpl(final GLAutoDrawable drawable, final int glWinX, final int glWinY) {
        final Object[] shapesS = shapes.toArray();
        Arrays.sort(shapesS, (Comparator)shapeZAscComparator);

        final GLPixelStorageModes psm = new GLPixelStorageModes();
        final ByteBuffer pixel = Buffers.newDirectByteBuffer(4);

        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        display(drawable, shapesS, true);

        psm.setPackAlignment(gl, 4);
        // psm.setUnpackAlignment(gl, 4);
        try {
            // gl.glReadPixels(glWinX, getHeight() - glWinY, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixel);
            gl.glReadPixels(glWinX, glWinY, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixel);
        } catch(final GLException gle) {
            gle.printStackTrace();
            return null;
        }
        psm.restore(gl);

        // final float color = ( i + 1f ) / ( shapeCount + 2f );
        final int shapeCount = shapes.size();
        final int qp = pixel.get(0) & 0xFF;
        final float color = qp / 255.0f;
        final int index = Math.round( ( color * ( shapeCount + 2f) ) - 1f );

        // FIXME drawGL: color 0.333333, index 0 of [0..1[
        System.err.printf("pickGL: glWin %d / %d, byte %d, color %f, index %d of [0..%d[%n",
                glWinX, glWinY, qp, color, index, shapeCount);

        if( 0 <= index && index < shapeCount ) {
            return (Shape)shapesS[index];
        } else {
            return null;
        }
    }

    /**
     * Calling {@link Shape#winToObjCoord(Scene, int, int, float[])}, retrieving its Shape object position.
     * @param shape
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param pmv a new {@link PMVMatrix} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix, int, int, int, int) be setup},
     *            {@link Shape#setTransform(PMVMatrix) shape-transformed} and can be reused by the caller and runnable.
     * @param objPos resulting object position
     * @param runnable action
     */
    public void winToShapeCoord(final Shape shape, final int glWinX, final int glWinY, final PMVMatrix pmv, final float[] objPos, final Runnable runnable) {
        if( null != shape && null != shape.winToShapeCoord(pmvMatrixSetup, renderer.getViewport(), glWinX, glWinY, pmv, objPos) ) {
            runnable.run();
        }
    }

    /**
     * Interface providing {@link #set(PMVMatrix, int, int, int, int) a method} to
     * setup {@link PMVMatrix}'s {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW}.
     * <p>
     * At the end of operations, the {@link GLMatrixFunc#GL_MODELVIEW} matrix has to be selected.
     * </p>
     * <p>
     * Implementation is being called by {@link Scene#setupMatrix(PMVMatrix, int, int, int, int)}
     * and hence {@link Scene#reshape(GLAutoDrawable, int, int, int, int)}.
     * </p>
     * <p>
     * Custom implementations can be set via {@link Scene#setPMVMatrixSetup(PMVMatrixSetup)}.
     * </p>
     * <p>
     * The default implementation is described below:
     * <ul>
     *   <li>{@link GLMatrixFunc#GL_PROJECTION} Matrix
     *   <ul>
     *     <li>Identity</li>
     *     <li>Perspective {@link Scene#DEFAULT_ANGLE} with {@link Scene#DEFAULT_ZNEAR} and {@link Scene#DEFAULT_ZFAR}</li>
     *     <li>Translated to given {@link Scene#DEFAULT_SCENE_DIST}</li>
     *     <li>Scale (back) to have normalized {@link Scene#getBounds() plane dimensions}, 1 for the greater of width and height.</li>
     *   </ul></li>
     *   <li>{@link GLMatrixFunc#GL_MODELVIEW} Matrix
     *   <ul>
     *     <li>identity</li>
     *   </ul></li>
     * </ul>
     * </p>
     */
    public static interface PMVMatrixSetup {
        /**
         * Setup {@link PMVMatrix}'s {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW}.
         * <p>
         * See {@link PMVMatrixSetup} for details.
         * </p>
         * <p>
         * At the end of operations, the {@link GLMatrixFunc#GL_MODELVIEW} matrix is selected.
         * </p>
         * @param pmv the {@link PMVMatrix} to setup
         * @param x lower left corner of the viewport rectangle
         * @param y lower left corner of the viewport rectangle
         * @param width width of the viewport rectangle
         * @param height height of the viewport rectangle
         */
        void set(PMVMatrix pmv, final int x, final int y, final int width, final int height);

        /**
         * Optional method to set the {@link Scene#getBounds()} {@link AABBox}, maybe a {@code nop} if not desired.
         * <p>
         * Will be called by {@link Scene#reshape(GLAutoDrawable, int, int, int, int)} after {@link #set(PMVMatrix, int, int, int, int)}.
         * </p>
         * @param planeBox the {@link AABBox} to define
         * @param pmv the {@link PMVMatrix}, already setup via {@link #set(PMVMatrix, int, int, int, int)}.
         * @param x lower left corner of the viewport rectangle
         * @param y lower left corner of the viewport rectangle
         * @param width width of the viewport rectangle
         * @param height height of the viewport rectangle
         */
        void setPlaneBox(final AABBox planeBox, final PMVMatrix pmv, int x, int y, final int width, final int height);
    }

    /** Return the default or {@link #setPMVMatrixSetup(PMVMatrixSetup)} {@link PMVMatrixSetup}. */
    public final PMVMatrixSetup getPMVMatrixSetup() { return pmvMatrixSetup; }

    /** Set a custom {@link PMVMatrixSetup}. */
    public final void setPMVMatrixSetup(final PMVMatrixSetup setup) { pmvMatrixSetup = setup; }

    /** Return the default {@link PMVMatrixSetup}. */
    public static PMVMatrixSetup getDefaultPMVMatrixSetup() { return defaultPMVMatrixSetup; }

    /**
     * Setup {@link PMVMatrix} {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW}
     * by calling {@link #getPMVMatrixSetup()}'s {@link PMVMatrixSetup#set(PMVMatrix, int, int, int, int)}.
     * @param pmv the {@link PMVMatrix} to setup
     * @param x lower left corner of the viewport rectangle
     * @param y lower left corner of the viewport rectangle
     * @param width width of the viewport rectangle
     * @param height height of the viewport rectangle
     */
    public void setupMatrix(final PMVMatrix pmv, final int x, final int y, final int width, final int height) {
        pmvMatrixSetup.set(pmv, x, y, width, height);
    }

    /**
     * Setup {@link PMVMatrix} {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW}
     * using implicit {@link #getViewport()} surface dimension by calling {@link #getPMVMatrixSetup()}'s {@link PMVMatrixSetup#set(PMVMatrix, int, int, int, int)}.
     * @param pmv the {@link PMVMatrix} to setup
     */
    public void setupMatrix(final PMVMatrix pmv) {
        final int[] viewport = renderer.getViewport();
        setupMatrix(pmv, viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    /** Copies the current int[4] viewport in given target and returns it for chaining. It is set after initial {@link #reshape(GLAutoDrawable, int, int, int, int)}. */
    public final int[/*4*/] getViewport(final int[/*4*/] target) { return renderer.getViewport(target); }

    /** Borrows the current int[4] viewport w/o copying. It is set after initial {@link #reshape(GLAutoDrawable, int, int, int, int)}. */
    public int[/*4*/] getViewport() { return renderer.getViewport(); }

    /** Returns the {@link #getViewport()}'s width, set after initial {@link #reshape(GLAutoDrawable, int, int, int, int)}. */
    public int getWidth() { return renderer.getWidth(); }
    /** Returns the {@link #getViewport()}'s height, set after initial {@link #reshape(GLAutoDrawable, int, int, int, int)}. */
    public int getHeight() { return renderer.getHeight(); }

    /** Borrow the current {@link PMVMatrix}. */
    public PMVMatrix getMatrix() { return renderer.getMatrix(); }

    /**
     * Describing the scene's object model-dimensions of the plane at scene-distance covering the visible viewport rectangle.
     * <p>
     * The value is evaluated at {@link #reshape(GLAutoDrawable, int, int, int, int)} via {@link }
     * </p>
     * <p>
     * {@link AABBox#getWidth()} and {@link AABBox#getHeight()} define scene's dimension covered by surface size.
     * </p>
     * <p>
     * {@link AABBox} is setup via {@link #getPMVMatrixSetup()}'s {@link PMVMatrixSetup#setPlaneBox(AABBox, PMVMatrix, int, int, int, int)}.
     * </p>
     * <p>
     * The default {@link PMVMatrixSetup} implementation scales to normalized plane dimensions, 1 for the greater of width and height.
     * </p>
     */
    public AABBox getBounds() { return planeBox; }

    /**
     *
     * @param pmv
     * @param viewport
     * @param zNear
     * @param zFar
     * @param winX
     * @param winY
     * @param objOrthoZ
     * @param objPos float[3] storage for object coord result
     * @param winZ
     */
    public static void winToPlaneCoord(final PMVMatrix pmv, final int[] viewport,
                                       final float zNear, final float zFar,
                                       final float winX, final float winY, final float objOrthoZ,
                                       final float[] objPos) {
        final float winZ = FloatUtil.getOrthoWinZ(objOrthoZ, zNear, zFar);
        pmv.gluUnProject(winX, winY, winZ, viewport, 0, objPos, 0);
    }

    /**
     * Map given window surface-size to object coordinates relative to this scene using
     * the give projection parameters.
     * @param viewport
     * @param zNear
     * @param zFar
     * @param objOrthoDist
     * @param objSceneSize float[2] storage for object surface size result
     */
    public void surfaceToPlaneSize(final int[] viewport, final float zNear, final float zFar, final float objOrthoDist, final float[/*2*/] objSceneSize) {
        final PMVMatrix pmv = new PMVMatrix();
        setupMatrix(pmv, viewport[0], viewport[1], viewport[2], viewport[3]);
        {
            final float[] obj00Coord = new float[3];
            final float[] obj11Coord = new float[3];

            winToPlaneCoord(pmv, viewport, DEFAULT_ZNEAR, DEFAULT_ZFAR, viewport[0], viewport[1], objOrthoDist, obj00Coord);
            winToPlaneCoord(pmv, viewport, DEFAULT_ZNEAR, DEFAULT_ZFAR, viewport[2], viewport[3], objOrthoDist, obj11Coord);
            objSceneSize[0] = obj11Coord[0] - obj00Coord[0];
            objSceneSize[1] = obj11Coord[1] - obj00Coord[1];
        }
    }

    /**
     * Map given window surface-size to object coordinates relative to this scene using
     * the default {@link PMVMatrixSetup}.
     * @param viewport
     * @param objSceneSize float[2] storage for object surface size result
     */
    public void surfaceToPlaneSize(final int[] viewport, final float[/*2*/] objSceneSize) {
        surfaceToPlaneSize(viewport, DEFAULT_ZNEAR, DEFAULT_ZFAR, -DEFAULT_SCENE_DIST, objSceneSize);
    }

    public final Shape getActiveShape() {
        return activeShape;
    }

    public void releaseActiveShape() {
        activeShape = null;
    }
    private void setActiveShape(final Shape shape) {
        activeShape = shape;
    }

    private final class SBCGestureListener implements GestureHandler.GestureListener {
        @Override
        public void gestureDetected(final GestureEvent gh) {
            if( null != activeShape ) {
                // gesture .. delegate to active shape!
                final InputEvent orig = gh.getTrigger();
                if( orig instanceof MouseEvent ) {
                    final MouseEvent e = (MouseEvent) orig;
                    // flip to GL window coordinates
                    final int glWinX = e.getX();
                    final int glWinY = getHeight() - e.getY() - 1;
                    final PMVMatrix pmv = new PMVMatrix();
                    final float[] objPos = new float[3];
                    final Shape shape = activeShape;
                    winToShapeCoord(shape, glWinX, glWinY, pmv, objPos, () -> {
                        shape.dispatchGestureEvent(gh, glWinX, glWinY, pmv, renderer.getViewport(), objPos);
                    });
                }
            }
        }
    }

    /**
     * Dispatch mouse event, either directly sending to activeShape or picking one
     * @param e original Newt {@link MouseEvent}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     */
    final void dispatchMouseEvent(final MouseEvent e, final int glWinX, final int glWinY) {
        if( null == activeShape ) {
            dispatchMouseEventPickShape(e, glWinX, glWinY);
        } else {
            dispatchMouseEventForShape(activeShape, e, glWinX, glWinY);
        }
    }
    /**
     * Pick the shape using the event coordinates
     * @param e original Newt {@link MouseEvent}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     */
    final void dispatchMouseEventPickShape(final MouseEvent e, final int glWinX, final int glWinY) {
        final PMVMatrix pmv = new PMVMatrix();
        final float[] objPos = new float[3];
        final Shape[] shape = { null };
        if( null == pickShape(glWinX, glWinY, pmv, objPos, shape, () -> {
               setActiveShape(shape[0]);
               shape[0].dispatchMouseEvent(e, glWinX, glWinY, objPos);
           } ) )
        {
           releaseActiveShape();
        }
    }
    /**
     * Dispatch event to shape
     * @param shape target active shape of event
     * @param e original Newt {@link MouseEvent}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     */
    final void dispatchMouseEventForShape(final Shape shape, final MouseEvent e, final int glWinX, final int glWinY) {
        final PMVMatrix pmv = new PMVMatrix();
        final float[] objPos = new float[3];
        winToShapeCoord(shape, glWinX, glWinY, pmv, objPos, () -> { shape.dispatchMouseEvent(e, glWinX, glWinY, objPos); });
    }

    private class SBCMouseListener implements MouseListener {
        int lx=-1, ly=-1, lId=-1;

        void clear() {
            lx = -1; ly = -1; lId = -1;
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if( -1 == lId || e.getPointerId(0) == lId ) {
                lx = e.getX();
                ly = e.getY();
                lId = e.getPointerId(0);
            }
            // flip to GL window coordinates, origin bottom-left
            final int glWinX = e.getX();
            final int glWinY = getHeight() - e.getY() - 1;
            dispatchMouseEvent(e, glWinX, glWinY);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            // flip to GL window coordinates, origin bottom-left
            final int glWinX = e.getX();
            final int glWinY = getHeight() - e.getY() - 1;
            dispatchMouseEvent(e, glWinX, glWinY);
            if( 1 == e.getPointerCount() ) {
                // Release active shape: last pointer has been lifted!
                releaseActiveShape();
                clear();
            }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            // flip to GL window coordinates
            final int glWinX = e.getX();
            final int glWinY = getHeight() - e.getY() - 1;
            // activeId should have been released by mouseRelease() already!
            dispatchMouseEventPickShape(e, glWinX, glWinY);
            // Release active shape: last pointer has been lifted!
            releaseActiveShape();
            clear();
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            // drag activeShape, if no gesture-activity, only on 1st pointer
            if( null != activeShape && !pinchToZoomGesture.isWithinGesture() && e.getPointerId(0) == lId ) {
                lx = e.getX();
                ly = e.getY();

                // dragged .. delegate to active shape!
                // flip to GL window coordinates, origin bottom-left
                final int glWinX = lx;
                final int glWinY = getHeight() - ly - 1;
                dispatchMouseEventForShape(activeShape, e, glWinX, glWinY);
            }
        }

        @Override
        public void mouseWheelMoved(final MouseEvent e) {
            // flip to GL window coordinates
            final int glWinX = lx;
            final int glWinY = getHeight() - ly - 1;
            dispatchMouseEvent(e, glWinX, glWinY);
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            if( -1 == lId || e.getPointerId(0) == lId ) {
                lx = e.getX();
                ly = e.getY();
                lId = e.getPointerId(0);
            }
            final int glWinX = lx;
            final int glWinY = getHeight() - ly - 1;
            // dispatchMouseEvent(e, glWinX, glWinY);
            dispatchMouseEventPickShape(e, glWinX, glWinY);
        }
        @Override
        public void mouseEntered(final MouseEvent e) { }
        @Override
        public void mouseExited(final MouseEvent e) {
            releaseActiveShape();
            clear();
        }
    }

    /**
     * Return a formatted status string containing avg fps and avg frame duration.
     * @param glad GLAutoDrawable instance for FPSCounter, its chosen GLCapabilities and its GL's swap-interval
     * @param renderModes render modes for {@link Region#getRenderModeString(int)}
     * @param quality the Graph-Curve quality setting
     * @param dpi the monitor's DPI (vertical preferred)
     * @return formatted status string
     */
    public String getStatusText(final GLAutoDrawable glad, final int renderModes, final int quality, final float dpi) {
            final FPSCounter fpsCounter = glad.getAnimator();
            final float lfps, tfps, td;
            if( null != fpsCounter ) {
                lfps = fpsCounter.getLastFPS();
                tfps = fpsCounter.getTotalFPS();
                td = (float)fpsCounter.getLastFPSPeriod() / (float)fpsCounter.getUpdateFPSFrames();
            } else {
                lfps = 0f;
                tfps = 0f;
                td = 0f;
            }
            final String modeS = Region.getRenderModeString(renderModes);
            final GLCapabilitiesImmutable caps = glad.getChosenGLCapabilities();
            return String.format("%03.1f/%03.1f fps, %.1f ms/f, v-sync %d, dpi %.1f, %s-samples %d, q %d, msaa %d, blend %b, alpha %d",
                        lfps, tfps, td, glad.getGL().getSwapInterval(), dpi, modeS, getSampleCount(), quality,
                        caps.getNumSamples(),
                        getRenderState().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED),
                        caps.getAlphaBits());
    }

    /**
     * Return a formatted status string containing avg fps and avg frame duration.
     * @param fpsCounter the counter, must not be null
     * @return formatted status string
     */
    public static String getStatusText(final FPSCounter fpsCounter) {
            final float lfps = fpsCounter.getLastFPS();
            final float tfps = fpsCounter.getTotalFPS();
            final float td = (float)fpsCounter.getLastFPSPeriod() / (float)fpsCounter.getUpdateFPSFrames();
            return String.format("%03.1f/%03.1f fps, %.1f ms/f", lfps, tfps, td);
    }

    private static final PMVMatrixSetup defaultPMVMatrixSetup = new PMVMatrixSetup() {
        @Override
        public void set(final PMVMatrix pmv, final int x, final int y, final int width, final int height) {
            final float ratio = (float)width/(float)height;
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            pmv.gluPerspective(DEFAULT_ANGLE, ratio, DEFAULT_ZNEAR, DEFAULT_ZFAR);
            pmv.glTranslatef(0f, 0f, DEFAULT_SCENE_DIST);

            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();

            // Scale (back) to have normalized plane dimensions, 1 for the greater of width and height.
            final AABBox planeBox0 = new AABBox();
            setPlaneBox(planeBox0, pmv, x, y, width, height);
            final float sx = planeBox0.getWidth();
            final float sy = planeBox0.getHeight();
            final float sxy = sx > sy ? sx : sy;
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glScalef(sxy, sxy, 1f);
            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        }

        @Override
        public void setPlaneBox(final AABBox planeBox, final PMVMatrix pmv, final int x, final int y, final int width, final int height) {
                final float orthoDist = -DEFAULT_SCENE_DIST;
                final float[] obj00Coord = new float[3];
                final float[] obj11Coord = new float[3];
                final int[] viewport = { x, y, width, height };

                winToPlaneCoord(pmv, viewport, DEFAULT_ZNEAR, DEFAULT_ZFAR, x, y, orthoDist, obj00Coord);
                winToPlaneCoord(pmv, viewport, DEFAULT_ZNEAR, DEFAULT_ZFAR, width, height, orthoDist, obj11Coord);

                planeBox.setSize( obj00Coord[0],  // lx
                                  obj00Coord[1],  // ly
                                  obj00Coord[2],  // lz
                                  obj11Coord[0],  // hx
                                  obj11Coord[1],  // hy
                                  obj11Coord[2] );// hz            }
        }
    };
    private PMVMatrixSetup pmvMatrixSetup = defaultPMVMatrixSetup;
}
