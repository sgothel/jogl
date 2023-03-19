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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.graph.geom.Vertex;
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

    private static final boolean DEBUG = false;

    private final ArrayList<Shape> shapes = new ArrayList<Shape>();

    private final float sceneDist, zNear, zFar;
    private final float projAngle = DEFAULT_ANGLE;
    private float[] clearColor = null;
    private int clearMask;

    private final RegionRenderer renderer;

    private final int[] sampleCount = new int[1];

    /** Describing the bounding box in shape's object model-coordinates of the near-plane parallel at its scene-distance, pre {@link #translate(PMVMatrix)} to origin. */
    private final AABBox planeBoxCtr = new AABBox(0f, 0f, 0f, 0f, 0f, 0f);
    /** Describing the bounding box in shape's object model-coordinates of the near-plane parallel at its scene-distance, post {@link #translate(PMVMatrix)} */
    private final AABBox planBoxBL = new AABBox(0f, 0f, 0f, 0f, 0f, 0f);

    private volatile Shape activeShape = null;

    private SBCMouseListener sbcMouseListener = null;
    private SBCGestureListener sbcGestureListener = null;
    private PinchToZoomGesture pinchToZoomGesture = null;

    private GLAutoDrawable cDrawable = null;

    private static RegionRenderer createRenderer() {
        return RegionRenderer.create(SVertex.factory(), RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
    }

    /**
     * Create a new scene with an internally created RegionRenderer
     * and using default values {@link #DEFAULT_SCENE_DIST}, {@link #DEFAULT_ZNEAR} and {@link #DEFAULT_ZFAR}.
     */
    public Scene() {
        this(createRenderer(), DEFAULT_SCENE_DIST, DEFAULT_ZNEAR, DEFAULT_ZFAR);
    }

    /**
     * Create a new scene with given projection values and an internally created RegionRenderer.
     * @param sceneDist scene distance on z-axis to projection, consider using {@link #DEFAULT_SCENE_DIST}.
     * @param zNear projection z-near value, consider using {@link #DEFAULT_ZNEAR}
     * @param zFar projection z-far value, consider using {@link #DEFAULT_ZFAR}
     */
    public Scene(final float sceneDist, final float zNear, final float zFar) {
        this(createRenderer(), sceneDist, zNear, zFar);
    }

    /**
     * Create a new scene taking ownership of the given RegionRenderer
     * and using default values {@link #DEFAULT_SCENE_DIST}, {@link #DEFAULT_ZNEAR} and {@link #DEFAULT_ZFAR}.
     */
    public Scene(final RegionRenderer renderer) {
        this(renderer, DEFAULT_SCENE_DIST, DEFAULT_ZNEAR, DEFAULT_ZFAR);
    }

    /**
     * Create a new scene with given projection values and taking ownership of the given RegionRenderer.
     * @param renderer RegionRenderer to use and own
     * @param sceneDist scene distance on z-axis to projection, consider using {@link #DEFAULT_SCENE_DIST}.
     * @param zNear projection z-near value, consider using {@link #DEFAULT_ZNEAR}
     * @param zFar projection z-far value, consider using {@link #DEFAULT_ZFAR}
     */
    public Scene(final RegionRenderer renderer, final float sceneDist, final float zNear, final float zFar) {
        if( null == renderer ) {
            throw new IllegalArgumentException("Null RegionRenderer");
        }
        this.renderer = renderer;
        this.sceneDist = sceneDist;
        this.zFar = zFar;
        this.zNear = zNear;
        this.sampleCount[0] = 4;
    }

    /** Return z-axis distance of scene to projection, see {@link #DEFAULT_SCENE_DIST}. */
    public float getProjSceneDist() { return sceneDist; }

    /** Return projection angle in degrees, see {@link #DEFAULT_ANGLE}. */
    public float getProjAngle() { return projAngle; }

    /** Return projection z-near value, see {@link #DEFAULT_ZNEAR}. */
    public float getProjZNear() { return zNear; }

    /** Return projection z-far value, see {@link #DEFAULT_ZFAR}. */
    public float getProjZFar() { return zFar; }

    /** Returns the associated RegionRenderer */
    public RegionRenderer getRenderer() { return renderer; }

    /** Returns the associated RegionRenderer's RenderState, may be null. */
    public RenderState getRenderState() {
        if( null != renderer ) {
            return renderer.getRenderState();
        }
        return null;
    }
    public final Vertex.Factory<? extends Vertex> getVertexFactory() {
        if( null != renderer ) {
            return renderer.getRenderState().getVertexFactory();
        }
        return null;
    }
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
        System.err.println("SceneUIController: init");
        cDrawable = drawable;
        if( null != renderer ) {
            renderer.init(drawable.getGL().getGL2ES2());
        }
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

        renderer.enable(gl, false);
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
     * @param objPos storage for found object position in model-space of found {@link Shape}
     * @param shape storage for found {@link Shape} or null
     * @param runnable the action to perform if {@link Shape} was found
     */
    public void pickShape(final int glWinX, final int glWinY, final float[] objPos, final Shape[] shape, final Runnable runnable) {
        shape[0] = pickShapeImpl(glWinX, glWinY, objPos);
        if( null != shape[0] ) {
            runnable.run();
        }
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Shape pickShapeImpl(final int glWinX, final int glWinY, final float[] objPos) {
        final float winZ0 = 0f;
        final float winZ1 = 0.3f;
        /**
            final FloatBuffer winZRB = Buffers.newDirectFloatBuffer(1);
            gl.glReadPixels( x, y, 1, 1, GL2ES2.GL_DEPTH_COMPONENT, GL.GL_FLOAT, winZRB);
            winZ1 = winZRB.get(0); // dir
        */
        final PMVMatrix pmv = new PMVMatrix();
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
                pmv.glPopMatrix();
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
                    shape[0].winToObjCoord(getMatrix(), getViewport(), glWinX, glWinY, objPos);
                    pmv.glPopMatrix();

                    runnable.run();
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
     * Calling {@link Shape#winToObjCoord(Scene, int, int, float[])}, retrieving its object position.
     * @param shape
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param objPos resulting object position
     * @param runnable action
     */
    public void winToObjCoord(final Shape shape, final int glWinX, final int glWinY, final float[] objPos, final Runnable runnable) {
        if( null != shape && shape.winToObjCoord(this, glWinX, glWinY, objPos) ) {
            runnable.run();
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
        System.err.println("SceneUIController: dispose");
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
        if( null != renderer ) {
            renderer.destroy(gl);
        }
    }

    /**
     *
     * @param pmv
     * @param view
     * @param zNear
     * @param zFar
     * @param orthoX
     * @param orthoY
     * @param orthoDist
     * @param winZ
     * @param objPos float[3] storage for object coord result
     */
    public static void winToObjCoord(final PMVMatrix pmv, final int[] view,
                                       final float zNear, final float zFar,
                                       final float orthoX, final float orthoY, final float orthoDist,
                                       final float[] winZ, final float[] objPos) {
        winZ[0] = FloatUtil.getOrthoWinZ(orthoDist, zNear, zFar);
        pmv.gluUnProject(orthoX, orthoY, winZ[0], view, 0, objPos, 0);
    }

    /**
     * Map given window surface-size to object coordinates relative to this scene and {@link #getProjSceneDist() and projection settings.
     * @param width surface width in pixel
     * @param height surface height in pixel
     * @param objSceneSize float[2] storage for object surface size result
     */
    public void surfaceToObjSize(final int width, final int height, final float[/*2*/] objSceneSize) {
        final int[] viewport = { 0, 0, width, height };

        final PMVMatrix pmv = new PMVMatrix();
        setupMatrix(pmv, width, height);
        {
            final float orthoDist = -sceneDist;
            final float[] obj00Coord = new float[3];
            final float[] obj11Coord = new float[3];
            final float[] winZ = new float[1];

            winToObjCoord(pmv, viewport, zNear, zFar, 0f, 0f, orthoDist, winZ, obj00Coord);
            winToObjCoord(pmv, viewport, zNear, zFar, width, height, orthoDist, winZ, obj11Coord);
            objSceneSize[0] = obj11Coord[0] - obj00Coord[0];
            objSceneSize[1] = obj11Coord[1] - obj00Coord[1];
        }
    }

    /**
     * Reshape scene {@link #setupMatrix(PMVMatrix, int, int)}.
     * <p>
     * {@link GLMatrixFunc#GL_PROJECTION} is setup using perspective {@link #getProjAngle()} with {@link #getProjZNear()} and {@link #getProjZFar()}.
     * </p>
     * <p>
     * {@link GLMatrixFunc#GL_MODELVIEW} is translated to given {@link #getProjSceneDist()}
     * and and origin 0/0 becomes the bottom-left corner.
     * </p>
     * @see #setupMatrix(PMVMatrix, int, int)
     * @see #getBounds()
     * @see #getBoundsCenter()
     */
    @SuppressWarnings("unused")
    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        renderer.reshapeNotify(x, y, width, height);

        final PMVMatrix pmv = renderer.getMatrix();
        setupMatrix0(pmv, width, height);
        pmv.glTranslatef(0f, 0f, sceneDist);
        {
            final float orthoDist = -sceneDist;
            final float[] obj00Coord = new float[3];
            final float[] obj11Coord = new float[3];
            final float[] winZ = new float[1];

            winToObjCoord(pmv, getViewport(), zNear, zFar, 0f, 0f, orthoDist, winZ, obj00Coord);
            winToObjCoord(pmv, getViewport(), zNear, zFar, width, height, orthoDist, winZ, obj11Coord);

            pmv.glTranslatef(obj00Coord[0], obj00Coord[1], 0f); // bottom-left corder origin 0/0

            planeBoxCtr.setSize( obj00Coord[0],  // lx
                                 obj00Coord[1],  // ly
                                 obj00Coord[2],  // lz
                                 obj11Coord[0],  // hx
                                 obj11Coord[1],  // hy
                                 obj11Coord[2] );// hz

            planBoxBL.setSize(   0,                      // lx
                                 0,                      // ly
                                 0,                      // lz
                                 planeBoxCtr.getWidth(), // hx
                                 planeBoxCtr.getHeight(),// hy
                                 planeBoxCtr.getDepth());// hz

            if( true || DEBUG ) {
                System.err.printf("Reshape: zNear %f,  zFar %f, sceneDist %f%n", zNear, zFar, sceneDist);
                System.err.printf("Reshape: Frustum: %s%n", pmv.glGetFrustum());
                System.err.printf("Reshape: mapped.00: [%f, %f, %f], winZ %f -> [%f, %f, %f]%n", 0f, 0f, orthoDist, winZ[0], obj00Coord[0], obj00Coord[1], obj00Coord[2]);
                System.err.printf("Reshape: mapped.11: [%f, %f, %f], winZ %f -> [%f, %f, %f]%n", (float)width, (float)height, orthoDist, winZ[0], obj11Coord[0], obj11Coord[1], obj11Coord[2]);
                System.err.printf("Reshape: scenePlaneBox: %s%n", planeBoxCtr);
            }
        }

        if( false ) {
            final float[] sceneScale = new float[3];
            final float[] scenePlaneOrigin = new float[3];
            scenePlaneOrigin[0] = planeBoxCtr.getMinX() * sceneDist;
            scenePlaneOrigin[1] = planeBoxCtr.getMinY() * sceneDist;
            scenePlaneOrigin[2] = planeBoxCtr.getMinZ() * sceneDist;
            sceneScale[0] = ( planeBoxCtr.getWidth() * sceneDist ) / width;
            sceneScale[1] = ( planeBoxCtr.getHeight() * sceneDist  ) / height;
            sceneScale[2] = 1f;
            System.err.printf("Scene Origin [%f, %f, %f]%n", scenePlaneOrigin[0], scenePlaneOrigin[1], scenePlaneOrigin[2]);
            System.err.printf("Scene Scale  %f * [%f x %f] / [%d x %d] = [%f, %f, %f]%n",
                    sceneDist, planeBoxCtr.getWidth(), planeBoxCtr.getHeight(),
                    width, height,
                    sceneScale[0], sceneScale[1], sceneScale[2]);
        }
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

    /** Translate current matrix to {@link #getBounds()}'s origin (minx/miny) and {@link #getProjSceneDist()}, a convenience method. */
    private void translate(final PMVMatrix pmv) {
        pmv.glTranslatef(planeBoxCtr.getMinX(), planeBoxCtr.getMinY(), sceneDist);
    }

    /**
     * Setup {@link PMVMatrix} {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW}
     * using explicit surface width and height before {@link #reshape(GLAutoDrawable, int, int, int, int)} happened, a convenience method.
     * <p>
     * {@link GLMatrixFunc#GL_PROJECTION} is setup using perspective {@link #getProjAngle()} with {@link #getProjZNear()} and {@link #getProjZFar()}.
     * </p>
     * <p>
     * {@link GLMatrixFunc#GL_MODELVIEW} is translated to given {@link #getProjSceneDist()}
     * and and origin 0/0 becomes the bottom-left corner.
     * </p>
     * <p>
     * At the end of operations, the {@link GLMatrixFunc#GL_MODELVIEW} matrix is selected.
     * </p>
     * @param pmv the {@link PMVMatrix} to setup
     * @param surface_width explicit surface width
     * @param surface_height explicit surface height
     */
    public void setupMatrix(final PMVMatrix pmv, final int surface_width, final int surface_height) {
        setupMatrix0(pmv, surface_width, surface_height);
        translate(pmv);
    }
    private void setupMatrix0(final PMVMatrix pmv, final int surface_width, final int surface_height) {
        final float ratio = (float)surface_width/(float)surface_height;
        pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmv.glLoadIdentity();
        pmv.gluPerspective(projAngle, ratio, zNear, zFar);

        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
    }

    /**
     * Setup {@link PMVMatrix} {@link GLMatrixFunc#GL_PROJECTION} and {@link GLMatrixFunc#GL_MODELVIEW}
     * using implicit {@link #getViewport()} surface dimension after {@link #reshape(GLAutoDrawable, int, int, int, int)} happened, a convenience method.
     * <p>
     * {@link GLMatrixFunc#GL_PROJECTION} is setup using perspective {@link #getProjAngle()} with {@link #getProjZNear()} and {@link #getProjZFar()}.
     * </p>
     * <p>
     * {@link GLMatrixFunc#GL_MODELVIEW} is translated to given {@link #getProjSceneDist()}
     * and and origin 0/0 becomes the bottom-left corner.
     * </p>
     * <p>
     * At the end of operations, the {@link GLMatrixFunc#GL_MODELVIEW} matrix is selected.
     * </p>
     * @param pmv the {@link PMVMatrix} to setup
     * @param surface_width explicit surface width
     * @param surface_height explicit surface height
     */
    public void setupMatrix(final PMVMatrix pmv) {
        setupMatrix(pmv, getWidth(), getHeight());
    }

    /**
     * Describing the scene's object model-dimensions of the near-plane parallel at its scene-distance {@link #getProjSceneDist()}
     * having the origin 0/0 on the bottom-left corner.
     * <p>
     * The value is evaluated at {@link #reshape(GLAutoDrawable, int, int, int, int)}.
     * </p>
     * <p>
     * {@link AABBox#getWidth()} and {@link AABBox#getHeight()} define scene's dimension covered by surface size.
     * </p>
     */
    public AABBox getBounds() { return planBoxBL; }

    /**
     * Describing the scene's object model-dimensions of the near-plane parallel at its scene-distance {@link #getProjSceneDist()}
     * having the origin 0/0 in the center of the screen.
     * <p>
     * The value is evaluated at {@link #reshape(GLAutoDrawable, int, int, int, int)} before translating to the bottom-left origin 0/0,
     * i.e. its minimum values are negative of half dimension.
     * </p>
     * <p>
     * {@link AABBox#getWidth()} and {@link AABBox#getHeight()} define scene's dimension covered by surface size.
     * </p>
     */
    public AABBox getBoundsCenter() { return planeBoxCtr; }

    public final Shape getActiveShape() {
        return activeShape;
    }

    public void release() {
        setActiveShape(null);
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
                    final float[] objPos = new float[3];
                    final Shape shape = activeShape;
                    winToObjCoord(shape, glWinX, glWinY, objPos, () -> {
                        shape.dispatchGestureEvent(Scene.this, gh, glWinX, glWinY, objPos);
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
            dispatchMouseEventPickShape(e, glWinX, glWinY, true);
        } else {
            dispatchMouseEventForShape(activeShape, e, glWinX, glWinY);
        }
    }
    /**
     * Pick the shape using the event coordinates
     * @param e original Newt {@link MouseEvent}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param setActive
     */
    final void dispatchMouseEventPickShape(final MouseEvent e, final int glWinX, final int glWinY, final boolean setActive) {
        final float[] objPos = new float[3];
        final Shape[] shape = { null };
        pickShape(glWinX, glWinY, objPos, shape, () -> {
               if( setActive ) {
                   setActiveShape(shape[0]);
               }
               shape[0].dispatchMouseEvent(e, glWinX, glWinY, objPos);
           } );
    }
    /**
     * Dispatch event to shape
     * @param shape target active shape of event
     * @param e original Newt {@link MouseEvent}
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     */
    final void dispatchMouseEventForShape(final Shape shape, final MouseEvent e, final int glWinX, final int glWinY) {
        final float[] objPos = new float[3];
        winToObjCoord(shape, glWinX, glWinY, objPos, () -> { shape.dispatchMouseEvent(e, glWinX, glWinY, objPos); });
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
                release();
                clear();
            }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            // flip to GL window coordinates
            final int glWinX = e.getX();
            final int glWinY = getHeight() - e.getY() - 1;
            // activeId should have been released by mouseRelease() already!
            dispatchMouseEventPickShape(e, glWinX, glWinY, false);
            // Release active shape: last pointer has been lifted!
            release();
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
            dispatchMouseEventPickShape(e, glWinX, glWinY, true);
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            if( -1 == lId || e.getPointerId(0) == lId ) {
                lx = e.getX();
                ly = e.getY();
                lId = e.getPointerId(0);
            }
        }
        @Override
        public void mouseEntered(final MouseEvent e) { }
        @Override
        public void mouseExited(final MouseEvent e) {
            release();
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

}
