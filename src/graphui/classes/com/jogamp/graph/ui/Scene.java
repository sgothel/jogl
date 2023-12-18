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
package com.jogamp.graph.ui;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.common.nio.Buffers;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.ui.Shape.Visitor2;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Ray;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.graph.ui.Shape.Visitor1;
import com.jogamp.newt.event.GestureHandler;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.PinchToZoomGesture;
import com.jogamp.newt.event.GestureHandler.GestureEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureSequence;

import jogamp.graph.ui.TreeTool;

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
 * {@link #setPMVMatrixSetup(PMVMatrixSetup)} maybe used to provide a custom {@link PMVMatrix4f} setup.
 * </p>
 * @see Shape
 */
public final class Scene implements Container, GLEventListener {
    /** Default scene distance on z-axis to projection is -1/5f. */
    public static final float DEFAULT_SCENE_DIST = -1/5f;
    /** Default projection angle in radians is PI/4, i.e. 45.0 degrees. */
    public static final float DEFAULT_ANGLE = FloatUtil.QUARTER_PI;
    /** Default projection z-near value is {@value}. */
    public static final float DEFAULT_ZNEAR = 0.1f;
    /** Default projection z-far value is {@value}. */
    public static final float DEFAULT_ZFAR = 7000.0f;
    /** Default Z precision on 16-bit depth buffer using {@link #DEFAULT_SCENE_DIST} z-position and {@link #DEFAULT_ZNEAR}. Value is {@code 6.1033297E-6}. */
    public static final float DEFAULT_Z16_EPSILON = FloatUtil.getZBufferEpsilon(16 /* zBits */, DEFAULT_SCENE_DIST, DEFAULT_ZNEAR);
    /** Default Z precision on 16-bit depth buffer using {@code -1} z-position and {@link #DEFAULT_ZNEAR}. Value is {@code 1.5256461E-4}. */
    // public static final float DIST1_Z16_EPSILON = FloatUtil.getZBufferEpsilon(16 /* zBits */, -1, DEFAULT_ZNEAR);

    /**
     * Return Z precision on using {@link PMVMatrixSetup#getSceneDist()} z-position and {@link PMVMatrixSetup#getZNear()}.
     * @param zBits depth buffer bit-depth, minimum 16-bit
     * @param setup {@link PMVMatrixSetup} for scene-distance as z-position and zNear
     * @return the Z precision
     */
    public static float getZEpsilon(final int zBits, final PMVMatrixSetup setup) {
        return FloatUtil.getZBufferEpsilon(zBits, setup.getSceneDist(), setup.getZNear());
    }

    /** Minimum sample count {@value} for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link Region#VBAA_RENDERING_BIT} or {@link Region#MSAA_RENDERING_BIT}. */
    public static final int MIN_SAMPLE_COUNT = 1;

    /** Maximum sample count {@value} for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link Region#VBAA_RENDERING_BIT} or {@link Region#MSAA_RENDERING_BIT}. */
    public static final int MAX_SAMPLE_COUNT = 8;

    @SuppressWarnings("unused")
    private static final boolean DEBUG = false;

    private final List<Shape> shapes = new CopyOnWriteArrayList<Shape>();
    private float dbgBorderThickness = 0f;
    private boolean doFrustumCulling = false;

    private float[] clearColor = null;
    private int clearMask;

    private final RegionRenderer renderer;

    private final int[] sampleCount = new int[1];

    /** Describing the bounding box in shape's object model-coordinates of the near-plane parallel at its scene-distance, post {@link #translate(PMVMatrix4f)} */
    private final AABBox planeBox = new AABBox(0f, 0f, 0f, 0f, 0f, 0f);

    private volatile Shape activeShape = null;

    private SBCMouseListener sbcMouseListener = null;
    private SBCGestureListener sbcGestureListener = null;
    private PinchToZoomGesture pinchToZoomGesture = null;

    final GLReadBufferUtil screenshot;

    private GLAutoDrawable cDrawable = null;

    private static RegionRenderer createRenderer() {
        return RegionRenderer.create(RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
    }

    /**
     * Create a new scene with an internally created {@link RegionRenderer}, a graph AA sample-count 4 and using {@link DefaultPMVMatrixSetup#DefaultPMVMatrixSetup()}.
     * @see #Scene(RegionRenderer, int)
     * @see #setSampleCount(int)
     */
    public Scene() {
        this(createRenderer(), 4);
    }

    /**
     * Create a new scene with an internally created {@link RegionRenderer}, using {@link DefaultPMVMatrixSetup#DefaultPMVMatrixSetup()}.
     * @param sampleCount sample count for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link Region#VBAA_RENDERING_BIT} or {@link Region#MSAA_RENDERING_BIT},
     *                    clipped to [{@link #MIN_SAMPLE_COUNT}..{@link #MAX_SAMPLE_COUNT}]
     * @see #Scene(RegionRenderer, int)
     * @see #setSampleCount(int)
     */
    public Scene(final int sampleCount) {
        this(createRenderer(), sampleCount);
    }

    /**
     * Create a new scene taking ownership of the given RegionRenderer, using {@link DefaultPMVMatrixSetup#DefaultPMVMatrixSetup()}.
     * @param renderer {@link RegionRenderer} to be owned
     * @param sampleCount sample count for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link Region#VBAA_RENDERING_BIT} or {@link Region#MSAA_RENDERING_BIT},
     *                    clipped to [{@link #MIN_SAMPLE_COUNT}..{@link #MAX_SAMPLE_COUNT}]
     * @see #setSampleCount(int)
     */
    public Scene(final RegionRenderer renderer, final int sampleCount) {
        if( null == renderer ) {
            throw new IllegalArgumentException("Null RegionRenderer");
        }
        this.renderer = renderer;
        this.sampleCount[0] = Math.min(MAX_SAMPLE_COUNT, Math.max(sampleCount, MIN_SAMPLE_COUNT)); // clip
        this.screenshot = new GLReadBufferUtil(false, false);
    }

    /** Returns the associated RegionRenderer */
    public RegionRenderer getRenderer() { return renderer; }

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

    @Override
    public final void setFrustumCullingEnabled(final boolean v) { doFrustumCulling = v; }

    @Override
    public final boolean isFrustumCullingEnabled() { return doFrustumCulling; }

    public void attachGLAutoDrawable(final GLAutoDrawable drawable) {
        cDrawable = drawable;
    }
    public void detachGLAutoDrawable(final GLAutoDrawable drawable) {
        if( cDrawable == drawable ) {
            cDrawable = null;
        }
    }
    public void attachInputListenerTo(final GLWindow window) {
        cDrawable = window;
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

    @Override
    public int getShapeCount() { return shapes.size(); }

    @Override
    public List<Shape> getShapes() { return shapes; }

    @Override
    public void addShape(final Shape s) {
        if( !s.hasBorder() && !FloatUtil.isZero(dbgBorderThickness) ) {
            s.setBorder(dbgBorderThickness);
        }
        shapes.add(s);
    }
    @Override
    public Shape removeShape(final Shape s) {
        if( shapes.remove(s) ) {
            s.setBorder(0f);
            return s;
        } else {
            return null;
        }
    }
    @Override
    public Shape removeShape(final int idx) {
        final Shape r = shapes.remove(idx);
        if( null != r ) {
            r.setBorder(0f);
        }
        return r;
    }

    /**
     * Removes given shape and destroy it, if contained.
     * @param gl GL2ES2 context
     * @param s the shape to be removed
     * @return true if given Shape is removed and destroyed
     */
    public boolean removeShape(final GL2ES2 gl, final Shape s) {
        if( shapes.remove(s) ) {
            s.setBorder(0f);
            s.destroy(gl, renderer);
            return true;
        } else {
            return false;
        }
    }
    @Override
    public void addShapes(final Collection<? extends Shape> shapes) {
        for(final Shape s : shapes) {
            addShape(s);
        }
    }
    @Override
    public void removeShapes(final Collection<? extends Shape> shapes) {
        for(final Shape s : shapes) {
            removeShape(s);
        }
    }
    /** Removes all given shapes and destroys them. */
    public void removeShapes(final GL2ES2 gl, final Collection<? extends Shape> shapes) {
        for(final Shape s : shapes) {
            removeShape(gl, s);
        }
    }
    @Override
    public void removeAllShapes() {
        final int count = shapes.size();
        for(int i=count-1; i>=0; --i) {
            final Shape s = shapes.get(i);
            s.setBorder(0f);
            shapes.remove(s);
        }
    }
    /** Removes all given shapes and destroys them. */
    public void removeAllShapes(final GL2ES2 gl) {
        final int count = shapes.size();
        for(int i=count-1; i>=0; --i) {
            removeShape(gl, shapes.get(i));
        }
    }

    @Override
    public boolean contains(final Shape s) {
        return false;
    }
    @Override
    public Shape getShapeByIdx(final int id) {
        if( 0 > id ) {
            return null;
        }
        return shapes.get(id);
    }
    @Override
    public Shape getShapeByID(final int id) {
        for(final Shape b : shapes) {
            if(b.getID() == id ) {
                return b;
            }
        }
        return null;
    }
    @Override
    public Shape getShapeByName(final String name) {
        for(final Shape b : shapes) {
            if( b.getName().equals(name) ) {
                return b;
            }
        }
        return null;
    }

    /** Returns sample count for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link Region#VBAA_RENDERING_BIT} or {@link Region#MSAA_RENDERING_BIT} */
    public int getSampleCount() { return sampleCount[0]; }

    /**
     * Sets sample count for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link Region#VBAA_RENDERING_BIT} or {@link Region#MSAA_RENDERING_BIT}
     * @param v sample count, clipped to [{@link #MIN_SAMPLE_COUNT}..{@link #MAX_SAMPLE_COUNT}]
     * @return clipped and set value
     */
    public int setSampleCount(final int v) {
        sampleCount[0] = Math.min(MAX_SAMPLE_COUNT, Math.max(v, MIN_SAMPLE_COUNT)); // clip
        markAllShapesDirty();
        return sampleCount[0];
    }

    public void setAllShapesQuality(final int q) {
        for(int i=0; i<shapes.size(); i++) {
            final Shape shape = shapes.get(i);
            if( shape instanceof GraphShape ) {
                ((GraphShape)shape).setQuality(q);
            }
        }
    }
    public void setAllShapesSharpness(final float sharpness) {
        for(int i=0; i<shapes.size(); i++) {
            final Shape shape = shapes.get(i);
            if( shape instanceof GraphShape ) {
                ((GraphShape)shape).setSharpness(sharpness);
            }
        }
    }
    public void markAllShapesDirty() {
        for(int i=0; i<shapes.size(); i++) {
            shapes.get(i).markShapeDirty();
        }
    }

    /**
     * Sets the debug {@link Shape#setBorder(float) border} thickness for all existing or added shapes, zero for no debug border (default).
     * @param v thickness debug border, zero for no border
     */
    public final void setDebugBorderBox(final float v) {
        dbgBorderThickness = v;
        for(int i=0; i<shapes.size(); i++) {
            shapes.get(i).setBorder(v);
        }
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        if( null == cDrawable ) {
            cDrawable = drawable;
        }
        renderer.init(drawable.getGL().getGL2ES2());
    }

    /**
     * Enqueues a one-shot {@link GLRunnable},
     * which will be executed within the next {@link GLAutoDrawable#display()} call
     * if this {@link Scene} has been added as a {@link GLEventListener} and {@link #init(GLAutoDrawable)} has been called.
     * <p>
     * See {@link GLAutoDrawable#invoke(boolean, GLRunnable)}.
     * </p>
     *
     * @param wait if <code>true</code> block until execution of <code>glRunnable</code> is finished, otherwise return immediately w/o waiting
     * @param glRunnable the {@link GLRunnable} to execute within {@link #display()}
     * @return <code>true</code> if the {@link GLRunnable} has been processed or queued, otherwise <code>false</code>.
     * @throws IllegalStateException in case of a detected deadlock situation ahead, see above.
     * @see GLAutoDrawable#invoke(boolean, GLRunnable)
     */
    public boolean invoke(final boolean wait, final GLRunnable glRunnable) throws IllegalStateException  {
        if( null != cDrawable ) {
            return cDrawable.invoke(wait, glRunnable);
        }
        return false;
    }

    public void addGLEventListener(final GLEventListener listener) {
        if( null != cDrawable ) {
            cDrawable.addGLEventListener(listener);
        }
    }
    public void removeGLEventListener(final GLEventListener listener) {
        if( null != cDrawable ) {
            cDrawable.removeGLEventListener(listener);
        }
    }

    /**
     * Reshape scene using {@link #setupMatrix(PMVMatrix4f, int, int, int, int)} using {@link PMVMatrixSetup}.
     * <p>
     * {@inheritDoc}
     * </p>
     * @see PMVMatrixSetup
     * @see #setPMVMatrixSetup(PMVMatrixSetup)
     * @see #setupMatrix(PMVMatrix4f, int, int, int, int)
     * @see #getBounds()
     * @see #getBoundsCenter()
     */
    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        renderer.reshapeNotify(x, y, width, height);

        setupMatrix(renderer.getMatrix(), renderer.getViewport());
        pmvMatrixSetup.setPlaneBox(planeBox, renderer.getMatrix(), renderer.getViewport());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void display(final GLAutoDrawable drawable) {
        final Object[] shapesS = shapes.toArray();
        Arrays.sort(shapesS, (Comparator)Shape.ZAscendingComparator);

        display(drawable, shapesS, false);
    }

    private static final int[] sampleCountGLSelect = { -1 };

    private void display(final GLAutoDrawable drawable, final Object[] shapes, final boolean glSelect) {
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

        final PMVMatrix4f pmv = renderer.getMatrix();

        if( glSelect ) {
            renderer.enable(gl, true, RegionRenderer.defaultBlendDisable, RegionRenderer.defaultBlendDisable);
        } else {
            renderer.enable(gl, true);
        }

        //final int shapeCount = shapes.size();
        final int shapeCount = shapes.length;
        for(int i=0; i<shapeCount; i++) {
            // final Shape shape = shapes.get(i);
            final Shape shape = (Shape)shapes[i];
            // System.err.println("Id "+i+": "+uiShape);
            if( shape.isVisible() ) {
                pmv.pushMv();
                shape.setTransformMv(pmv);

                if( !doFrustumCulling || !pmv.getFrustum().isAABBoxOutside( shape.getBounds() ) ) {
                    if( glSelect ) {
                        final float color = ( i + 1f ) / ( shapeCount + 2f );
                        // FIXME
                        // System.err.printf("drawGL: color %f, index %d of [0..%d[%n", color, i, shapeCount);
                        renderer.setColorStatic(color, color, color, 1f);
                        shape.drawToSelect(gl, renderer, sampleCount0);
                    } else {
                        shape.draw(gl, renderer, sampleCount0);
                    }
                }
                pmv.popMv();
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
        for(int i=0; i<disposeActions.size(); i++) {
            try {
                disposeActions.get(i).run(drawable);
            } catch(final Throwable t) {
                System.err.println("Scene.dispose: Caught Exception @ User Disposable["+i+"]: "+t.getMessage());
                t.printStackTrace();
            }
        }
        shapes.clear();
        disposeActions.clear();
        if( drawable == cDrawable ) {
            cDrawable = null;
        }
        renderer.destroy(gl);
        screenshot.dispose(gl);
    }
    private final List<GLRunnable> disposeActions = new ArrayList<GLRunnable>();
    /**
     * Add a user one-time {@link GLRunnable} disposal action to an internal list, all invoked at {@Link #dispose(GLAutoDrawable)}
     * where the list is cleared afterwards similar to all shapes.
     * <p>
     * This allows proper take-down of custom user resources at exit.
     * </p>
     * @param action the custom {@link GLRunnable} disposal action
     */
    public void addDisposeAction(final GLRunnable action) { disposeActions.add(action); }

    /**
     * Attempt to pick a {@link Shape} using the window coordinates and contained {@ling Shape}'s {@link AABBox} {@link Shape#getBounds() bounds}
     * using a ray-intersection algorithm.
     * <p>
     * If {@link Shape} was found the given action is performed.
     * </p>
     * <p>
     * Method performs on current thread and returns after probing every {@link Shape}.
     * </p>
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link Shape#setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller and runnable.
     * @param glWinX window X coordinate, bottom-left origin
     * @param glWinY window Y coordinate, bottom-left origin
     * @param objPos storage for found object position in model-space of found {@link Shape}
     * @param runnable the action to perform if {@link Shape} was found
     * @return last picked (inner) Shape if any or null
     */
    public Shape pickShape(final PMVMatrix4f pmv, final int glWinX, final int glWinY, final Vec3f objPos, final Shape.Visitor1 visitor) {
        setupMatrix(pmv);

        final float winZ0 = 0f;
        final float winZ1 = 0.3f;
        /**
            final FloatBuffer winZRB = Buffers.newDirectFloatBuffer(1);
            gl.glReadPixels( x, y, 1, 1, GL2ES2.GL_DEPTH_COMPONENT, GL.GL_FLOAT, winZRB);
            winZ1 = winZRB.get(0); // dir
        */
        final Recti viewport = getViewport();
        final Ray ray = new Ray();
        final Shape[] shape = { null };
        final int[] shapeIdx = { -1 };
        forSortedAll(Shape.ZDescendingComparator, pmv, (final Shape s, final PMVMatrix4f pmv2) -> {
            shapeIdx[0]++;
            final boolean ok = s.isInteractive() && pmv.mapWinToRay(glWinX, glWinY, winZ0, winZ1, viewport, ray);
            if( ok ) {
                final AABBox sbox = s.getBounds();
                if( sbox.intersectsRay(ray) ) {
                    if( DEBUG ) {
                        System.err.printf("Pick.0: shape %d/%s/%s, [%d, %d, %f/%f] -> %s%n", shapeIdx[0], s.getClass().getSimpleName(), s.getName(), glWinX, glWinY, winZ0, winZ1, ray);
                    }
                    if( null == sbox.getRayIntersection(objPos, ray, FloatUtil.EPSILON, true) ) {
                        throw new InternalError("Ray "+ray+", box "+sbox);
                    }
                    if( visitor.visit(s) ) {
                        if( DEBUG ) {
                            System.err.printf("Pick.S: shape %d/%s/%s @ %s, %s%n", shapeIdx[0], s.getClass().getSimpleName(), s.getName(), objPos, s);
                        }
                        shape[0] = s;
                    } else if( DEBUG ) {
                        System.err.printf("Pick.1: shape %d/%s/%s @ %s, %s%n", shapeIdx[0], s.getClass().getSimpleName(), s.getName(), objPos, s);
                    }
                }
            }
            return false; // continue traversing for most inner interactive shape
        });
        if( DEBUG ) {
            if( null != shape[0] ) {
                System.err.printf("Pick.X: shape %s/%s%n%n", shape[0].getClass().getSimpleName(), shape[0].getName());
            } else {
                System.err.printf("Pick.X: shape null%n%n");
            }
        }
        return shape[0];
    }

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
    public void pickShapeGL(final int glWinX, final int glWinY, final Vec3f objPos, final Shape[] shape, final Runnable runnable) {
        if( null == cDrawable ) {
            return;
        }
        cDrawable.invoke(false, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable drawable) {
                final Shape s = pickShapeGLImpl(drawable, glWinX, glWinY);
                shape[0] = s;
                if( null != s ) {
                    final PMVMatrix4f pmv = renderer.getMatrix();
                    pmv.pushMv();
                    s.setTransformMv(pmv);
                    final boolean ok = null != shape[0].winToShapeCoord(getMatrix(), getViewport(), glWinX, glWinY, objPos);
                    pmv.popMv();
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
        Arrays.sort(shapesS, (Comparator)Shape.ZAscendingComparator);

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
     * @param pmv a new {@link PMVMatrix4f} which will {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti) be setup},
     *            {@link Shape#setTransformMv(PMVMatrix4f) shape-transformed} and can be reused by the caller and runnable.
     * @param objPos resulting object position
     * @param runnable action
     */
    public void winToShapeCoord(final Shape shape, final int glWinX, final int glWinY, final PMVMatrix4f pmv, final Vec3f objPos, final Runnable runnable) {
        if( null == shape ) {
            return;
        }
        final Recti viewport = getViewport();
        setupMatrix(pmv);
        forOne(pmv, shape, () -> {
            if( null != shape.winToShapeCoord(pmv, viewport, glWinX, glWinY, objPos) ) {
                runnable.run();
            }
        });
    }

    @Override
    public AABBox getBounds(final PMVMatrix4f pmv, final Shape shape) {
        final AABBox res = new AABBox();
        if( null == shape ) {
            return res;
        }
        setupMatrix(pmv);
        forOne(pmv, shape, () -> {
            shape.getBounds().transform(pmv.getMv(), res);
        });
        return res;
    }

    /**
     * Traverses through the graph up until {@code shape} and apply {@code action} on it.
     * @param pmv {@link PMVMatrix4f}, which shall be properly initialized, e.g. via {@link Scene#setupMatrix(PMVMatrix4f)}
     * @param shape
     * @param action
     * @return true to signal operation complete, i.e. {@code shape} found, otherwise false
     */
    @Override
    public boolean forOne(final PMVMatrix4f pmv, final Shape shape, final Runnable action) {
        return TreeTool.forOne(shapes, pmv, shape, action);
    }

    /**
     * Traverses through the graph and apply {@link Visitor2#visit(Shape, PMVMatrix4f)} for each, stop if it returns true.
     * @param pmv {@link PMVMatrix4f}, which shall be properly initialized, e.g. via {@link Scene#setupMatrix(PMVMatrix4f)}
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor2#visit(Shape, PMVMatrix4f)} returned true, otherwise false
     */
    @Override
    public boolean forAll(final PMVMatrix4f pmv, final Visitor2 v) {
        return TreeTool.forAll(shapes, pmv, v);
    }

    /**
     * Traverses through the graph and apply {@link Visitor1#visit(Shape)} for each, stop if it returns true.
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor1#visit(Shape)} returned true, otherwise false
     */
    @Override
    public boolean forAll(final Visitor1 v) {
        return TreeTool.forAll(shapes, v);
    }

    /**
     * Traverses through the graph and apply {@link Visitor2#visit(Shape, PMVMatrix4f)} for each, stop if it returns true.
     *
     * Each {@link Container} level is sorted using {@code sortComp}
     * @param sortComp
     * @param pmv
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor2#visit(Shape, PMVMatrix4f)} returned true, otherwise false
     */
    @Override
    public boolean forSortedAll(final Comparator<Shape> sortComp, final PMVMatrix4f pmv, final Visitor2 v) {
        return TreeTool.forSortedAll(sortComp, shapes, pmv, v);
    }

    /**
     * Interface providing {@link #set(PMVMatrix4f, Recti) a method} to
     * setup {@link PMVMatrix4f}'s {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW}.
     * <p>
     * At the end of operations, the {@link GLMatrixFunc#GL_MODELVIEW} matrix has to be selected.
     * </p>
     * <p>
     * Implementation is being called by {@link Scene#setupMatrix(PMVMatrix4f, int, int, int, int)}
     * and hence {@link Scene#reshape(GLAutoDrawable, int, int, int, int)}.
     * </p>
     * <p>
     * Custom implementations can be set via {@link Scene#setPMVMatrixSetup(PMVMatrixSetup)}.
     * </p>
     * <p>
     * The default implementation is {@link Scene.DefaultPMVMatrixSetup}.
     * @see DefaultPMVMatrixSetup
     * @see Scene#setPMVMatrixSetup(PMVMatrixSetup)
     */
    public static interface PMVMatrixSetup {
        /** Returns scene distance on z-axis to projection. */
        float getSceneDist();
        /** Returns fov projection angle in radians, shall be {@code 0} for orthogonal projection. */
        float getAngle();
        /** Returns projection z-near value. */
        float getZNear();
        /** Returns projection z-far value. */
        float getZFar();

        /**
         * Setup {@link PMVMatrix4f}'s {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW}.
         * <p>
         * See {@link PMVMatrixSetup} for details.
         * </p>
         * <p>
         * At the end of operations, the {@link GLMatrixFunc#GL_MODELVIEW} matrix is selected.
         * </p>
         * @param pmv the {@link PMVMatrix4f} to setup
         * @param viewport Rect4i viewport
         */
        void set(PMVMatrix4f pmv, Recti viewport);

        /**
         * Optional method to set the {@link Scene#getBounds()} {@link AABBox}, maybe a {@code nop} if not desired.
         * <p>
         * Will be called by {@link Scene#reshape(GLAutoDrawable, int, int, int, int)} after {@link #set(PMVMatrix4f, Recti)}.
         * </p>
         * @param planeBox the {@link AABBox} to define
         * @param pmv the {@link PMVMatrix4f}, already setup via {@link #set(PMVMatrix4f, Recti)}.
         * @param viewport Rect4i viewport
         */
        void setPlaneBox(final AABBox planeBox, final PMVMatrix4f pmv, Recti viewport);
    }

    /** Return the default or {@link #setPMVMatrixSetup(PMVMatrixSetup)} {@link PMVMatrixSetup}. */
    public final PMVMatrixSetup getPMVMatrixSetup() { return pmvMatrixSetup; }

    /** Set a custom {@link PMVMatrixSetup}. */
    public final void setPMVMatrixSetup(final PMVMatrixSetup setup) { pmvMatrixSetup = setup; }

    /**
     * Setup {@link PMVMatrix4f} {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW}
     * by calling {@link #getPMVMatrixSetup()}'s {@link PMVMatrixSetup#set(PMVMatrix4f, Recti)}.
     * @param pmv the {@link PMVMatrix4f} to setup
     * @param Recti viewport
     */
    public void setupMatrix(final PMVMatrix4f pmv, final Recti viewport) {
        pmvMatrixSetup.set(pmv, viewport);
    }

    /**
     * Setup {@link PMVMatrix4f} {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW}
     * using implicit {@link #getViewport()} surface dimension by calling {@link #getPMVMatrixSetup()}'s {@link PMVMatrixSetup#set(PMVMatrix4f, Recti)}.
     * @param pmv the {@link PMVMatrix4f} to setup
     */
    public void setupMatrix(final PMVMatrix4f pmv) {
        final Recti viewport = renderer.getViewport();
        setupMatrix(pmv, viewport);
    }

    /** Copies the current int[4] viewport in given target and returns it for chaining. It is set after initial {@link #reshape(GLAutoDrawable, int, int, int, int)}. */
    public final Recti getViewport(final Recti target) { return renderer.getViewport(target); }

    /** Borrows the current int[4] viewport w/o copying. It is set after initial {@link #reshape(GLAutoDrawable, int, int, int, int)}. */
    public Recti getViewport() { return renderer.getViewport(); }

    /** Returns the {@link #getViewport()}'s width, set after initial {@link #reshape(GLAutoDrawable, int, int, int, int)}. */
    public int getWidth() { return renderer.getWidth(); }
    /** Returns the {@link #getViewport()}'s height, set after initial {@link #reshape(GLAutoDrawable, int, int, int, int)}. */
    public int getHeight() { return renderer.getHeight(); }

    /** Borrow the current {@link PMVMatrix4f}. */
    public PMVMatrix4f getMatrix() { return renderer.getMatrix(); }

    /**
     * Describing the scene's object model-dimensions of the plane at scene-distance covering the visible viewport rectangle.
     * <p>
     * The value is evaluated at {@link #reshape(GLAutoDrawable, int, int, int, int)} via {@link }
     * </p>
     * <p>
     * {@link AABBox#getWidth()} and {@link AABBox#getHeight()} define scene's dimension covered by surface size.
     * </p>
     * <p>
     * {@link AABBox} is setup via {@link #getPMVMatrixSetup()}'s {@link PMVMatrixSetup#setPlaneBox(AABBox, PMVMatrix4f, Recti)}.
     * </p>
     * <p>
     * The default {@link PMVMatrixSetup} implementation scales to normalized plane dimensions, 1 for the greater of width and height.
     * </p>
     */
    public AABBox getBounds() { return planeBox; }

    /**
     * Return Z precision on using current {@link #getPMVMatrixSetup()}'s {@link PMVMatrixSetup#getSceneDist()} z-position and {@link PMVMatrixSetup#getZNear()}.
     * @param zBits depth buffer bit-depth, minimum 16-bit
     * @return the Z precision
     */
    public float getZEpsilon(final int zBits) {
        return FloatUtil.getZBufferEpsilon(zBits, pmvMatrixSetup.getSceneDist(), pmvMatrixSetup.getZNear());
    }

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
    public static void winToPlaneCoord(final PMVMatrix4f pmv, final Recti viewport,
                                       final float zNear, final float zFar,
                                       final float winX, final float winY, final float objOrthoZ,
                                       final Vec3f objPos) {
        final float winZ = FloatUtil.getOrthoWinZ(objOrthoZ, zNear, zFar);
        pmv.mapWinToObj(winX, winY, winZ, viewport, objPos);
    }

    /**
     * Map given window surface-size to object coordinates relative to this scene using
     * the give projection parameters.
     * @param viewport viewport rectangle
     * @param zNear custom {@link #DEFAULT_ZNEAR}
     * @param zFar custom {@link #DEFAULT_ZFAR}
     * @param objOrthoDist custom {@link #DEFAULT_SCENE_DIST}
     * @param objSceneSize Vec2f storage for object surface size result
     */
    public void surfaceToPlaneSize(final Recti viewport, final float zNear, final float zFar, final float objOrthoDist, final Vec2f objSceneSize) {
        final PMVMatrix4f pmv = new PMVMatrix4f();
        setupMatrix(pmv, viewport);
        {
            final Vec3f obj00Coord = new Vec3f();
            final Vec3f obj11Coord = new Vec3f();

            winToPlaneCoord(pmv, viewport, DEFAULT_ZNEAR, DEFAULT_ZFAR, viewport.x(), viewport.y(), objOrthoDist, obj00Coord);
            winToPlaneCoord(pmv, viewport, DEFAULT_ZNEAR, DEFAULT_ZFAR, viewport.width(), viewport.height(), objOrthoDist, obj11Coord);
            objSceneSize.set( obj11Coord.x() - obj00Coord.x(),
                              obj11Coord.y() - obj00Coord.y() );
        }
    }

    /**
     * Map given window surface-size to object coordinates relative to this scene using
     * the default {@link PMVMatrixSetup}, i.e. {@link #DEFAULT_ZNEAR}, {@link #DEFAULT_ZFAR} and {@link #DEFAULT_SCENE_DIST}
     * @param viewport viewport rectangle
     * @param objSceneSize Vec2f storage for object surface size result
     */
    public void surfaceToPlaneSize(final Recti viewport, final Vec2f objSceneSize) {
        surfaceToPlaneSize(viewport, DEFAULT_ZNEAR, DEFAULT_ZFAR, -DEFAULT_SCENE_DIST, objSceneSize);
    }

    public final Shape getActiveShape() {
        return activeShape;
    }

    public void releaseActiveShape() {
        if( null != activeShape ) {
            activeShape.setActive(false, 0);
            activeShape = null;
        }
    }
    private void setActiveShape(final Shape shape) {
        if( activeShape != shape && null != shape &&
            shape.setActive(true, activeZOffsetScale * getZEpsilon(16)) )
        {
            if( null != activeShape ) {
                activeShape.setActive(false, 0);
            }
            activeShape = shape;
        }
    }
    private float activeZOffsetScale = 10f;

    /** Returns the active {@link Shape} Z-Offset scale, defaults to {@code 10.0}. */
    public float getActiveShapeZOffsetScale() { return activeZOffsetScale; }
    /** Sets the active {@link Shape} Z-Offset scale, defaults to {@code 10.0}. */
    public void setActiveShapeZOffsetScale(final float v) { activeZOffsetScale = v; }

    private final class SBCGestureListener implements GestureHandler.GestureListener {
        @Override
        public void gestureDetected(final GestureEvent gh) {
            if( null != activeShape ) {
                // gesture .. delegate to active shape!
                final InputEvent orig = gh.getTrigger();
                if( orig instanceof MouseEvent ) {
                    final Shape shape = activeShape;
                    if( shape.isInteractive() ) {
                        final MouseEvent e = (MouseEvent) orig;
                        // flip to GL window coordinates
                        final int glWinX = e.getX();
                        final int glWinY = getHeight() - e.getY() - 1;
                        final PMVMatrix4f pmv = new PMVMatrix4f();
                        final Vec3f objPos = new Vec3f();
                        winToShapeCoord(shape, glWinX, glWinY, pmv, objPos, () -> {
                            shape.dispatchGestureEvent(gh, glWinX, glWinY, pmv, renderer.getViewport(), objPos);
                        });
                    }
                }
            }
        }
    }

    /**
     * Pick the shape using the event coordinates
     * @param e original Newt {@link MouseEvent}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     */
    private final boolean dispatchMouseEventPickShape(final MouseEvent e, final int glWinX, final int glWinY) {
        final PMVMatrix4f pmv = new PMVMatrix4f();
        final Vec3f objPos = new Vec3f();
        final Shape shape = pickShape(pmv, glWinX, glWinY, objPos, (final Shape s) -> {
               return s.isInteractive() && ( s.dispatchMouseEvent(e, glWinX, glWinY, objPos) || true );
           });
        if( null != shape ) {
           setActiveShape(shape);
           return true;
        } else {
           releaseActiveShape();
           return false;
        }
    }
    /**
     * Dispatch event to shape
     * @param shape target active shape of event
     * @param e original Newt {@link MouseEvent}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     */
    private final void dispatchMouseEventForShape(final Shape shape, final MouseEvent e, final int glWinX, final int glWinY) {
        final PMVMatrix4f pmv = new PMVMatrix4f();
        final Vec3f objPos = new Vec3f();
        winToShapeCoord(shape, glWinX, glWinY, pmv, objPos, () -> { shape.dispatchMouseEvent(e, glWinX, glWinY, objPos); });
    }

    private final class SBCMouseListener implements MouseListener {
        private int lx, ly, lId;
        private boolean mouseOver;

        private SBCMouseListener() {
            clear();
        }
        private final void clear() {
            lx = -1; ly = -1; lId = -1; mouseOver = false;
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
            dispatchMouseEventPickShape(e, glWinX, glWinY);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            // flip to GL window coordinates, origin bottom-left
            final int glWinX = e.getX();
            final int glWinY = getHeight() - e.getY() - 1;
            dispatchMouseEventPickShape(e, glWinX, glWinY);
            if( !mouseOver ) {
                if( 1 == e.getPointerCount() ) {
                    // Release active shape: last pointer has been lifted!
                    releaseActiveShape();
                    clear();
                }
            }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            // flip to GL window coordinates
            final int glWinX = e.getX();
            final int glWinY = getHeight() - e.getY() - 1;
            if( mouseOver ) {
                dispatchMouseEventPickShape(e, glWinX, glWinY);
            } else {
                // activeId should have been released by mouseRelease() already!
                dispatchMouseEventPickShape(e, glWinX, glWinY);
                // Release active shape: last pointer has been lifted!
                releaseActiveShape();
                clear();
            }
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            // drag activeShape, if no gesture-activity, only on 1st pointer
            if( null != activeShape && activeShape.isInteractive() && !pinchToZoomGesture.isWithinGesture() && e.getPointerId(0) == lId ) {
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
            dispatchMouseEventPickShape(e, glWinX, glWinY);
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
            mouseOver = dispatchMouseEventPickShape(e, glWinX, glWinY);
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
     * @param renderModes render modes for {@link Region#getRenderModeString(int, int, int)}
     * @param quality the Graph-Curve quality setting or -1 to be ignored
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
            final GLCapabilitiesImmutable caps = glad.getChosenGLCapabilities();
            final String modeS = Region.getRenderModeString(renderModes, getSampleCount(), caps.getNumSamples());
            final String qualityStr, blendStr;
            if( 0 <= quality ) {
                qualityStr = ", q "+quality;
            } else {
                qualityStr = "";
            }
            if( getRenderer().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED) ) {
                blendStr = ", blend";
            } else {
                blendStr = "";
            }
            return String.format("%03.1f/%03.1f fps, %.1f ms/f, vsync %d, dpi %.1f, %s%s%s, a %d",
                        lfps, tfps, td, glad.getGL().getSwapInterval(), dpi, modeS, qualityStr, blendStr, caps.getAlphaBits());
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

    /**
     * Return the unique next technical screenshot PNG {@link File} instance as follows:
     * <pre>
     *    filename = [{dir}][{prefix}-]{@link Region#getRenderModeString(int, int, int)}[-{contentDetails}]-snap{screenShotCount}-{resolution}.png
     * </pre>
     * Implementation increments {@link #getScreenshotCount()}.
     *
     * @param dir the target directory, may be `null` or an empty string
     * @param prefix the prefix, may be `null` or an empty string
     * @param renderModes the used Graph renderModes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}
     * @param caps the used {@link GLCapabilitiesImmutable} used to retrieved the full-screen AA (fsaa) {@link GLCapabilitiesImmutable#getNumSamples()}
     * @param contentDetail user content details to be added at the end but before {@link #getScreenshotCount()}, may be `null` or an empty string
     * @return a unique descriptive screenshot filename
     * @see #screenshot(GL, File)
     * @see #screenshot(boolean, File)
     * @see #getScreenshotCount()
     */
    public File nextScreenshotFile(final String dir, final String prefix, final int renderModes, final GLCapabilitiesImmutable caps, final String contentDetail) {
        final String dir2 = ( null != dir && dir.length() > 0 ) ? dir : "";
        final String prefix2 = ( null != prefix && prefix.length() > 0 ) ? prefix+"-" : "";
        final RegionRenderer renderer = getRenderer();
        final String modeS = Region.getRenderModeString(renderModes, getSampleCount(), caps.getNumSamples());
        final String contentDetail2 = ( null != contentDetail && contentDetail.length() > 0 ) ? contentDetail+"-" : "";
        return new File( String.format((Locale)null, "%s%s%s-%ssnap%02d-%04dx%04d.png",
                                       dir2, prefix2, modeS, contentDetail2,
                                       screenShotCount++, renderer.getWidth(), renderer.getHeight() ) );
    }
    private int screenShotCount = 0;

    /** Return the number of {@link #nextScreenshotFile(String, String, int, GLCapabilitiesImmutable, String)} calls. */
    public int getScreenshotCount() { return screenShotCount; }

    /**
     * Write current read drawable (screen) to a file.
     * <p>
     * Best to be {@link GLAutoDrawable#invoke(boolean, GLRunnable) invoked on the display call},
     * see {@link #screenshot(boolean, String)}.
     * </p>
     * @param gl current GL object
     * @param file the target file to be used, consider using {@link #nextScreenshotFile(String, String, int, GLCapabilitiesImmutable, String)}
     * @see #nextScreenshotFile(String, String, int, GLCapabilitiesImmutable, String)
     * @see #getScreenshotCount()
     * @see #screenshot(boolean, File)
     */
    public void screenshot(final GL gl, final File file)  {
        if(screenshot.readPixels(gl, false)) {
            screenshot.write(file);
            System.err.println("Wrote: "+file);
        }
    }

    /**
     * Write current read drawable (screen) to a file on {@link GLAutoDrawable#invoke(boolean, GLRunnable) on the display call}.
     *
     * @param wait if true block until execution of screenshot {@link GLRunnable} is finished, otherwise return immediately w/o waiting
     * @param file the target file to be used, consider using {@link #nextScreenshotFile(String, String, int, GLCapabilitiesImmutable, String)}
     * @see #nextScreenshotFile(String, String, int, GLCapabilitiesImmutable, String)
     * @see #getScreenshotCount()
     * @see #screenshot(GL, File)
     */
    public void screenshot(final boolean wait, final File file)  {
        if( null != cDrawable ) {
            cDrawable.invoke(wait, (drawable) -> {
                screenshot(drawable.getGL(), file);
                return true;
            });
        }
    }

    /**
     * Default implementation of {@link Scene.PMVMatrixSetup},
     * implementing {@link Scene.PMVMatrixSetup#set(PMVMatrix4f, Recti)} as follows:
     * <ul>
     *   <li>{@link GLMatrixFunc#GL_PROJECTION} Matrix
     *   <ul>
     *     <li>Identity</li>
     *     <li>Perspective {@link #getAngle()} with {@link #getZNear()} and {@link #getZFar()}</li>
     *     <li>Translated to given {@link #getSceneDist()}</li>
     *   </ul></li>
     *   <li>{@link GLMatrixFunc#GL_MODELVIEW} Matrix
     *   <ul>
     *     <li>identity</li>
     *   </ul></li>
     * </ul>
     * </p>
     * @see DefaultPMVMatrixSetup#DefaultPMVMatrixSetup()
     * @see Scene#setPMVMatrixSetup(PMVMatrixSetup)
     * @see Scene.PMVMatrixSetup
     */
    public static class DefaultPMVMatrixSetup implements PMVMatrixSetup {
        /** Scene distance on z-axis to projection. */
        private final float scene_dist;
        /** Projection angle in radians. */
        private final float angle;
        /** Projection z-near value. */
        private final float zNear;
        /** Projection z-far value. */
        private final float zFar;

        /**
         * Custom {@link DefaultPMVMatrixSetup} instance
         * @param scene_dist scene distance on z-axix
         * @param zNear projection z-near value
         * @param zFar projection z-far value
         * @param angle projection angle in radians
         * @see DefaultPMVMatrixSetup
         * @see Scene#setPMVMatrixSetup(PMVMatrixSetup)
         * @see Scene.PMVMatrixSetup
         */
        public DefaultPMVMatrixSetup(final float scene_dist, final float zNear, final float zFar, final float angle) {
            if( !( zNear > 0 && zFar > zNear ) ) {
                throw new IllegalArgumentException("zNear is "+zNear+", but must be > 0 and < zFar, zFar "+zFar);
            }
            this.scene_dist = scene_dist;
            this.zNear = zNear;
            this.zFar = zFar;
            this.angle = angle;
        }
        /**
         * Custom {@link DefaultPMVMatrixSetup} instance using given {@code scene_dist}, {@code zNear}, {@code zFar} and {@link Scene#DEFAULT_ANGLE}.
         * @param scene_dist scene distance on z-axix
         * @param zNear projection z-near value
         * @param zFar projection z-far value
         * @see DefaultPMVMatrixSetup
         * @see Scene#setPMVMatrixSetup(PMVMatrixSetup)
         * @see Scene.PMVMatrixSetup
         */
        public DefaultPMVMatrixSetup(final float scene_dist, final float zNear, final float zFar) {
            this(scene_dist, zNear, zFar, Scene.DEFAULT_ANGLE);
        }
        /**
         * Custom {@link DefaultPMVMatrixSetup} instance using given {@code scene_dist} and {@link Scene#DEFAULT_ZNEAR}, {@link Scene#DEFAULT_ZFAR}, {@link Scene#DEFAULT_ANGLE}.
         * @param scene_dist scene distance on z-axix
         * @see DefaultPMVMatrixSetup
         * @see Scene#setPMVMatrixSetup(PMVMatrixSetup)
         * @see Scene.PMVMatrixSetup
         */
        public DefaultPMVMatrixSetup(final float scene_dist) {
            this(scene_dist, Scene.DEFAULT_ZNEAR, Scene.DEFAULT_ZFAR, Scene.DEFAULT_ANGLE);
        }
        /**
         * Default {@link DefaultPMVMatrixSetup} instance using {@link Scene#DEFAULT_SCENE_DIST}, {@link Scene#DEFAULT_ZNEAR}, {@link Scene#DEFAULT_ZFAR}, {@link Scene#DEFAULT_ANGLE}.
         * @see DefaultPMVMatrixSetup
         * @see Scene#setPMVMatrixSetup(PMVMatrixSetup)
         * @see Scene.PMVMatrixSetup
         */
        public DefaultPMVMatrixSetup() {
            this(Scene.DEFAULT_SCENE_DIST, Scene.DEFAULT_ZNEAR, Scene.DEFAULT_ZFAR, Scene.DEFAULT_ANGLE);
        }

        @Override
        public void set(final PMVMatrix4f pmv, final Recti viewport) {
            final float ratio = (float) viewport.width() / (float) viewport.height();
            pmv.loadPIdentity();
            pmv.perspectiveP(angle, ratio, zNear, zFar);
            pmv.translateP(0f, 0f, scene_dist);

            pmv.loadMvIdentity();
        }

        @Override
        public void setPlaneBox(final AABBox planeBox, final PMVMatrix4f pmv, final Recti viewport) {
            final float orthoDist = -scene_dist;
            final Vec3f obj00Coord = new Vec3f();
            final Vec3f obj11Coord = new Vec3f();

            winToPlaneCoord(pmv, viewport, zNear, zFar, viewport.x(), viewport.y(), orthoDist, obj00Coord);
            winToPlaneCoord(pmv, viewport, zNear, zFar, viewport.width(), viewport.height(), orthoDist, obj11Coord);

            planeBox.setSize( obj00Coord, obj11Coord );
        }

        @Override
        public float getSceneDist() { return scene_dist; }
        @Override
        public float getAngle() { return angle; }
        @Override
        public float getZNear() { return zNear; }
        @Override
        public float getZFar() { return zFar; }
    };
    private PMVMatrixSetup pmvMatrixSetup = new DefaultPMVMatrixSetup();

}
