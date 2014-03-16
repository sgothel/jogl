package com.jogamp.opengl.test.junit.graph.demos.ui;

import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLRunnable;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.math.Ray;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;

public class SceneUIController implements GLEventListener{
    private final ArrayList<UIShape> shapes = new ArrayList<UIShape>();

    private float sceneStartX = 0f;
    private float sceneStartY = 0f;

    private RegionRenderer renderer;

    private final int[] sampleCount = new int[1];

    private final float zNear = 0.1f, zFar = 7000f;
    /** Describing the bounding box in model-coordinates of the near-plane parallel at distance one. */
    private final AABBox nearPlane1Box = new AABBox();
    private final int[] viewport = new int[] { 0, 0, 0, 0 };
    private float nearPlaneX0, nearPlaneY0, nearPlaneZ0, nearPlaneSx, nearPlaneSy;
    float globMvTx, globMvTy, globMvTz;

    private int activeId = -1;

    private SBCMouseListener sbcMouseListener = null;

    private GLAutoDrawable cDrawable = null;

    public SceneUIController() {
        this(null);
    }

    public SceneUIController(RegionRenderer renderer) {
        this.renderer = renderer;
        this.sampleCount[0] = 4;
    }

    public void setRenderer(RegionRenderer renderer) {
        this.renderer = renderer;
    }

    public void attachInputListenerTo(GLWindow window) {
        if(null == sbcMouseListener) {
            sbcMouseListener = new SBCMouseListener();
            window.addMouseListener(sbcMouseListener);
        }
    }

    public void detachInputListenerFrom(GLWindow window) {
        if(null != sbcMouseListener) {
            window.removeMouseListener(sbcMouseListener);
        }
    }

    public ArrayList<UIShape> getShapes() {
        return shapes;
    }
    public void addShape(UIShape b) {
        shapes.add(b);
    }

    public void removeShape(UIShape b) {
        shapes.remove(b);
    }

    public int getSampleCount() { return sampleCount[0]; }
    public void setSampleCount(int v) { sampleCount[0]=v; }

    @Override
    public void init(GLAutoDrawable drawable) {
        System.err.println("SceneUIController: init");
        cDrawable = drawable;
    }

    public int pickShape(final int winX, int winY) {
        final float winZ0 = 0f;
        final float winZ1 = 0.3f;
        /**
            final FloatBuffer winZRB = Buffers.newDirectFloatBuffer(1);
            gl.glReadPixels( x, y, 1, 1, GL2ES2.GL_DEPTH_COMPONENT, GL.GL_FLOAT, winZRB);
            winZ1 = winZRB.get(0); // dir
        */

        // flip to GL window coordinates
        winY = viewport[3] - winY;

        final PMVMatrix pmv = renderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

        final Ray ray = new Ray();
        final int shapeCount = shapes.size();
        for(int i=0; i<shapeCount; i++) {
            final UIShape uiShape = shapes.get(i);
            if( uiShape.isEnabled() ) {
                pmv.glPushMatrix();
                transformShape(pmv, uiShape);

                pmv.gluUnProjectRay(winX, winY, winZ0, winZ1, viewport, 0, ray);
                System.err.printf("Pick: mapped.0: [%d, %d, %f/%f] -> %s%n", winX, winY, winZ0, winZ1, ray);

                pmv.glPopMatrix();
                final AABBox box = shapes.get(i).getBounds();
                final boolean hit = box.intersectsRay(ray);
                System.err.println("Test: "+box+" -> hit "+hit+", shape: "+uiShape);
                if( hit ) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void transformShape(final PMVMatrix pmv, final UIShape uiShape) {
        final float[] uiTranslate = uiShape.getTranslate();
        float[] uiScale = uiShape.getScale();
        // System.err.printf("SceneUICtrl.render.1.0: scale.0: %f, %f, %f%n", uiScale[0], uiScale[1], uiScale[2]);
        // System.err.printf("SceneUICtrl.render.1.0: translate.0: %f, %f, %f%n", uiTranslate[0], uiTranslate[1], uiTranslate[2]);
        pmv.glRotate(uiShape.getRotation());
        pmv.glScalef(uiScale[0], uiScale[1], uiScale[2]);
        pmv.glTranslatef(uiTranslate[0], uiTranslate[1], uiTranslate[2]);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final PMVMatrix pmv = renderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

        final int shapeCount = shapes.size();
        for(int i=0; i<shapeCount; i++) {
            final UIShape uiShape = shapes.get(i);
            if( uiShape.isEnabled() ) {
                uiShape.validate(gl, renderer);
                pmv.glPushMatrix();
                transformShape(pmv, uiShape);
                renderer.updateMatrix(gl);
                uiShape.drawShape(gl, renderer, sampleCount);
                pmv.glPopMatrix();
                // break;
            }
        }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.err.println("SceneUIController: dispose");
        cDrawable = null;
    }

    public static void mapWin2ObjectCoords(final PMVMatrix pmv, final int[] view,
                                           final float zNear, final float zFar,
                                           float orthoX, float orthoY, float orthoDist,
                                           final float[] winZ, final float[] objPos) {
        winZ[0] = (1f/zNear-1f/orthoDist)/(1f/zNear-1f/zFar);
        pmv.gluUnProject(orthoX, orthoY, winZ[0], view, 0, objPos, 0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        viewport[0] = x;
        viewport[1] = y;
        viewport[2] = width;
        viewport[3] = height;

        sceneStartX = width * 1f/6f;
        sceneStartY = height - height/6f;

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        final PMVMatrix pmv = renderer.getMatrix();
        renderer.reshapePerspective(gl, 45.0f, width, height, zNear, zFar);
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();

        System.err.printf("Reshape: zNear %f,  zFar %f%n", zNear, zFar);
        System.err.printf("Reshape: Frustum: %s%n", pmv.glGetFrustum());
        {
            final float orthoDist = 1f;
            final float[] obj00Coord = new float[3];
            final float[] obj11Coord = new float[3];
            final float[] winZ = new float[1];
            final int[] view = new int[] { 0, 0, width, height };

            mapWin2ObjectCoords(pmv, view, zNear, zFar, 0f, 0f, orthoDist, winZ, obj00Coord);
            System.err.printf("Reshape: mapped.00: [%f, %f, %f], winZ %f -> [%f, %f, %f]%n", 0f, 0f, orthoDist, winZ[0], obj00Coord[0], obj00Coord[1], obj00Coord[2]);

            mapWin2ObjectCoords(pmv, view, zNear, zFar, width, height, orthoDist, winZ, obj11Coord);
            System.err.printf("Reshape: mapped.11: [%f, %f, %f], winZ %f -> [%f, %f, %f]%n", (float)width, (float)height, orthoDist, winZ[0], obj11Coord[0], obj11Coord[1], obj11Coord[2]);

            nearPlane1Box.setSize( obj00Coord[0],  // lx
                                   obj00Coord[1],  // ly
                                   obj00Coord[2],  // lz
                                   obj11Coord[0],  // hx
                                   obj11Coord[1],  // hy
                                   obj11Coord[2] );// hz
            System.err.printf("Reshape: dist1Box: %s%n", nearPlane1Box);
        }
        final float dist = 100f;
        nearPlaneX0 = nearPlane1Box.getMinX() * dist;
        nearPlaneY0 = nearPlane1Box.getMinY() * dist;
        nearPlaneZ0 = nearPlane1Box.getMinZ() * dist;
        final float xd = nearPlane1Box.getWidth() * dist;
        final float yd = nearPlane1Box.getHeight() * dist;
        nearPlaneSx = xd  / width;
        nearPlaneSy = yd / height;
        System.err.printf("Scale: [%f x %f] / [%d x %d] = [%f, %f]%n", xd, yd, width, height, nearPlaneSx, nearPlaneSy);

        // globMvTx=nearPlaneX0+(sceneStartX*nearPlaneSx);
        // globMvTy=nearPlaneY0+(sceneStartY*nearPlaneSy);
        globMvTx=nearPlaneX0;
        globMvTy=nearPlaneY0;
        globMvTz=nearPlaneZ0;
        pmv.glTranslatef(globMvTx, globMvTy, globMvTz);
        pmv.glScalef(nearPlaneSx, nearPlaneSy, 1f);
        pmv.glTranslatef(sceneStartX, sceneStartY, 0f);
        renderer.updateMatrix(gl);
    }

    public UIShape getActiveUI() {
        if( 0 > activeId ) {
            return null;
        }
        return shapes.get(activeId);
    }

    public void release() {
        activeId = -1;
    }

    private class SBCMouseListener implements MouseListener {
        int lx=-1, ly=-1;

        void clear() {
            lx = -1; ly = -1;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            UIShape uiShape = getActiveUI();
            if(uiShape != null){
                uiShape.dispatchMouseEvent(e);
            }
            clear();
            release();
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if(null==cDrawable) {
                return;
            }

            // Avoid race condition w/ matrix instance,
            // even thought we do not require a GL operation!
            cDrawable.invoke(true, new GLRunnable() {
                @Override
                public boolean run(GLAutoDrawable drawable) {
                    activeId = pickShape(e.getX(), e.getY());
                    final UIShape uiShape = getActiveUI();
                    if(uiShape != null) {
                        uiShape.setPressed(true);
                        uiShape.dispatchMouseEvent(e);
                    }
                    return false;
                } } );
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            final UIShape uiShape = getActiveUI();
            if(uiShape != null){
                uiShape.setPressed(false);
                uiShape.dispatchMouseEvent(e);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            final UIShape uiShape = getActiveUI();
            if(uiShape != null) {
                uiShape.dispatchMouseEvent(e);
            }
        }

        @Override
        public void mouseWheelMoved(final MouseEvent e) {
            cDrawable.invoke(true, new GLRunnable() {
                @Override
                public boolean run(GLAutoDrawable drawable) {
                    activeId = pickShape(lx, ly);
                    final UIShape uiShape = getActiveUI();
                    if(uiShape != null) {
                        uiShape.dispatchMouseEvent(e);
                    }
                    return false;
                } } );
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            lx = e.getX();
            ly = e.getY();
        }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) {
            release();
            clear();
        }
    }
}