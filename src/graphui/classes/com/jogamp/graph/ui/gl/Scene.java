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
import com.jogamp.opengl.util.PMVMatrix;

/**
 * GraphUI Scene
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * GraphUI is intended to become an immediate- and retained-mode API.
 * </p>
 * @see Shape
 */
public class Scene implements GLEventListener{
    private final ArrayList<Shape> shapes = new ArrayList<Shape>();

    private final float sceneDist, zNear, zFar;

    private RegionRenderer renderer;

    private final int[] sampleCount = new int[1];

    /** Describing the bounding box in model-coordinates of the near-plane parallel at distance one. */
    private final AABBox nearPlane1Box = new AABBox();
    private final int[] viewport = new int[] { 0, 0, 0, 0 };
    private final float[] sceneScale = new float[3];
    private final float[] scenePlaneOrigin = new float[3];


    private volatile Shape activeShape = null;

    private SBCMouseListener sbcMouseListener = null;
    private SBCGestureListener sbcGestureListener = null;
    private PinchToZoomGesture pinchToZoomGesture = null;

    private GLAutoDrawable cDrawable = null;

    public Scene(final float sceneDist, final float zNear, final float zFar) {
        this(null, sceneDist, zNear, zFar);
    }

    public Scene(final RegionRenderer renderer, final float sceneDist, final float zNear, final float zFar) {
        this.renderer = renderer;
        this.sceneDist = sceneDist;
        this.zFar = zFar;
        this.zNear = zNear;
        this.sampleCount[0] = 4;
    }

    /** Returns the associated RegionRenderer */
    public RegionRenderer getRenderer() { return renderer; }
    /** Sets the associated RegionRenderer, may set to null to avoid its destruction when {@link #dispose(GLAutoDrawable)} this instance. */
    public void setRenderer(final RegionRenderer renderer) {
        this.renderer = renderer;
    }
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
    public final Shape getShapeByIdx(final int id) {
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
        sampleCount[0] = Math.min(8, Math.max(v, 1)); // clip
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

    public final float[] getSceneScale() { return sceneScale; }
    public final float[] getScenePlaneOrigin() { return scenePlaneOrigin; }

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
            final float s1Z = s1.getBounds().getMinZ()+s1.getTranslate()[2];
            final float s2Z = s2.getBounds().getMinZ()+s2.getTranslate()[2];
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
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final PMVMatrix pmv = renderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

        final Object[] shapesS = shapes.toArray();
        Arrays.sort(shapesS, (Comparator)shapeZAscComparator);

        renderer.enable(gl, true);

        //final int shapeCount = shapes.size();
        final int shapeCount = shapesS.length;
        for(int i=0; i<shapeCount; i++) {
            // final UIShape uiShape = shapes.get(i);
            final Shape uiShape = (Shape)shapesS[i];
            // System.err.println("Id "+i+": "+uiShape);
            if( uiShape.isEnabled() ) {
                uiShape.validate(gl, renderer);
                pmv.glPushMatrix();
                uiShape.setTransform(pmv);
                uiShape.drawShape(gl, renderer, sampleCount);
                pmv.glPopMatrix();
            }
        }

        renderer.enable(gl, false);
    }

    public void pickShape(final int glWinX, final int glWinY, final float[] objPos, final Shape[] shape, final Runnable runnable) {
        if( null == cDrawable ) {
            return;
        }
        cDrawable.invoke(false, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable drawable) {
                shape[0] = pickShapeImpl(glWinX, glWinY, objPos);
                if( null != shape[0] ) {
                    runnable.run();
                }
                return true;
            } } );
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
        final PMVMatrix pmv = renderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

        final Ray ray = new Ray();

        final Object[] shapesS = shapes.toArray();
        Arrays.sort(shapesS, (Comparator)shapeZAscComparator);

        for(int i=shapesS.length-1; i>=0; i--) {
            final Shape uiShape = (Shape)shapesS[i];

            if( uiShape.isEnabled() ) {
                pmv.glPushMatrix();
                uiShape.setTransform(pmv);
                final boolean ok = pmv.gluUnProjectRay(glWinX, glWinY, winZ0, winZ1, viewport, 0, ray);
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
     * Calling {@link Shape#winToObjCoord(RegionRenderer, int, int, float[])}, retrieving its object position.
     * @param activeShape
     * @param glWinX in GL window coordinates, origin bottom-left
     * @param glWinY in GL window coordinates, origin bottom-left
     * @param objPos resulting object position
     * @param runnable action
     */
    public void windowToShapeCoords(final Shape activeShape, final int glWinX, final int glWinY, final float[] objPos, final Runnable runnable) {
        if( null == cDrawable || null == activeShape ) {
            return;
        }
        cDrawable.invoke(false, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable drawable) {
                final boolean ok;
                {
                    final PMVMatrix pmv = renderer.getMatrix();
                    pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
                    pmv.glPushMatrix();
                    activeShape.setTransform(pmv);
                    ok = activeShape.winToObjCoord(renderer, glWinX, glWinY, objPos);
                    pmv.glPopMatrix();
                }
                if( ok ) {
                    runnable.run();
                }
                return true;
            } } );
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

    public static void mapWin2ObjectCoords(final PMVMatrix pmv, final int[] view,
                                           final float zNear, final float zFar,
                                           final float orthoX, final float orthoY, final float orthoDist,
                                           final float[] winZ, final float[] objPos) {
        winZ[0] = FloatUtil.getOrthoWinZ(orthoDist, zNear, zFar);
        pmv.gluUnProject(orthoX, orthoY, winZ[0], view, 0, objPos, 0);
    }

   @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        viewport[0] = x;
        viewport[1] = y;
        viewport[2] = width;
        viewport[3] = height;

        final PMVMatrix pmv = renderer.getMatrix();
        renderer.reshapePerspective(45.0f, width, height, zNear, zFar);
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();

        System.err.printf("Reshape: zNear %f,  zFar %f%n", zNear, zFar);
        System.err.printf("Reshape: Frustum: %s%n", pmv.glGetFrustum());
        {
            final float orthoDist = 1f;
            final float[] obj00Coord = new float[3];
            final float[] obj11Coord = new float[3];
            final float[] winZ = new float[1];

            mapWin2ObjectCoords(pmv, viewport, zNear, zFar, 0f, 0f, orthoDist, winZ, obj00Coord);
            System.err.printf("Reshape: mapped.00: [%f, %f, %f], winZ %f -> [%f, %f, %f]%n", 0f, 0f, orthoDist, winZ[0], obj00Coord[0], obj00Coord[1], obj00Coord[2]);

            mapWin2ObjectCoords(pmv, viewport, zNear, zFar, width, height, orthoDist, winZ, obj11Coord);
            System.err.printf("Reshape: mapped.11: [%f, %f, %f], winZ %f -> [%f, %f, %f]%n", (float)width, (float)height, orthoDist, winZ[0], obj11Coord[0], obj11Coord[1], obj11Coord[2]);

            nearPlane1Box.setSize( obj00Coord[0],  // lx
                                   obj00Coord[1],  // ly
                                   obj00Coord[2],  // lz
                                   obj11Coord[0],  // hx
                                   obj11Coord[1],  // hy
                                   obj11Coord[2] );// hz
            System.err.printf("Reshape: dist1Box: %s%n", nearPlane1Box);
        }
        scenePlaneOrigin[0] = nearPlane1Box.getMinX() * sceneDist;
        scenePlaneOrigin[1] = nearPlane1Box.getMinY() * sceneDist;
        scenePlaneOrigin[2] = nearPlane1Box.getMinZ() * sceneDist;
        sceneScale[0] = ( nearPlane1Box.getWidth() * sceneDist ) / width;
        sceneScale[1] = ( nearPlane1Box.getHeight() * sceneDist  ) / height;
        sceneScale[2] = 1f;
        System.err.printf("Scene Origin [%f, %f, %f]%n", scenePlaneOrigin[0], scenePlaneOrigin[1], scenePlaneOrigin[2]);
        System.err.printf("Scene Scale  %f * [%f x %f] / [%d x %d] = [%f, %f, %f]%n",
                sceneDist, nearPlane1Box.getWidth(), nearPlane1Box.getHeight(),
                width, height,
                sceneScale[0], sceneScale[1], sceneScale[2]);

        pmv.glTranslatef(scenePlaneOrigin[0], scenePlaneOrigin[1], scenePlaneOrigin[2]);
        pmv.glScalef(sceneScale[0], sceneScale[1], sceneScale[2]);
    }

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
                    final int glWinY = viewport[3] - e.getY() - 1;
                    final float[] objPos = new float[3];
                    final Shape shape = activeShape;
                    windowToShapeCoords(shape, glWinX, glWinY, objPos, new Runnable() {
                        @Override
                        public void run() {
                            shape.dispatchGestureEvent(gh, glWinX, glWinY, objPos);
                        } } );
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
        pickShape(glWinX, glWinY, objPos, shape, new Runnable() {
           @Override
        public void run() {
               if( setActive ) {
                   setActiveShape(shape[0]);
               }
               shape[0].dispatchMouseEvent(e, glWinX, glWinY, objPos);
           } } );
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
        windowToShapeCoords(shape, glWinX, glWinY, objPos, new Runnable() {
            @Override
            public void run() {
                shape.dispatchMouseEvent(e, glWinX, glWinY, objPos);
            } } );
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
            final int glWinY = viewport[3] - e.getY() - 1;
            dispatchMouseEvent(e, glWinX, glWinY);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            // flip to GL window coordinates, origin bottom-left
            final int glWinX = e.getX();
            final int glWinY = viewport[3] - e.getY() - 1;
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
            final int glWinY = viewport[3] - e.getY() - 1;
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
                final int glWinY = viewport[3] - ly - 1;
                dispatchMouseEventForShape(activeShape, e, glWinX, glWinY);
            }
        }

        @Override
        public void mouseWheelMoved(final MouseEvent e) {
            // flip to GL window coordinates
            final int glWinX = lx;
            final int glWinY = viewport[3] - ly - 1;
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
