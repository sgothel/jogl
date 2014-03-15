package com.jogamp.opengl.test.junit.graph.demos.ui;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLRunnable;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.common.nio.Buffers;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.util.PMVMatrix;

public class SceneUIController implements GLEventListener{
    private final ArrayList<UIShape> shapes = new ArrayList<UIShape>();

    private int count = 0;
    private int renderModes;
    private int[] sampleCount;
    private RegionRenderer renderer;

    private final float[] translate = new float[3];
    private final float[] scale = new float[3];
    private final Quaternion quaternion = new Quaternion();

    private final float[] sceneClearColor = new float[]{0,0,0,0};

    private int activeId = -1;

    private SBCMouseListener sbcMouseListener = null;

    private GLAutoDrawable cDrawable = null;

    public SceneUIController() {
        this(null, 0, null);
    }

    public SceneUIController(RegionRenderer renderer, int renderModes, int[] sampleCount) {
        this.renderer = renderer;
        this.renderModes = renderModes;
        this.sampleCount = sampleCount;
        setScale(1f, 1f, 1f);
        setTranslate(0f, 0f, 0f);
        setRotation(0f, 0f, 0f);
    }

    public void setRenderer(RegionRenderer renderer, int renderModes, int[] sampleCount) {
        this.renderer = renderer;
        this.renderModes = renderModes;
        this.sampleCount = sampleCount;
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
        count++;
    }

    public void removeShape(UIShape b) {
        boolean found = shapes.remove(b);
        if(found) {
            count--;
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        System.err.println("SceneUIController: init");
        cDrawable = drawable;
    }
    @Override
    public void display(GLAutoDrawable drawable) {
        // System.err.println("SceneUIController: display");
        final int width = drawable.getWidth();
        final int height = drawable.getHeight();
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        render(gl, width, height, renderModes, sampleCount, false);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.err.println("SceneUIController: dispose");
        cDrawable = null;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public UIShape getShape(GLAutoDrawable drawable,int x, int y) {
        final int width = drawable.getWidth();
        final int height = drawable.getHeight();
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        int index = checkSelection(gl, x, y, width, height);
        if(index == -1)
            return null;
        return shapes.get(index);
    }

    public UIShape getActiveUI() {
        if(activeId == -1)
            return null;
        return shapes.get(activeId);
    }

    public void release() {
        activeId = -1;
    }

    private int checkSelection(GL2ES2 gl,int x, int y, int width, int height) {
        gl.glPixelStorei(GL2ES2.GL_PACK_ALIGNMENT, 4);
        gl.glPixelStorei(GL2ES2.GL_UNPACK_ALIGNMENT, 4);
        gl.glClearColor(sceneClearColor[0], sceneClearColor[1], sceneClearColor[2], sceneClearColor[3]);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        render(gl, width, height, 0, null, true);
        ByteBuffer pixel = Buffers.newDirectByteBuffer(4);
        pixel.order(ByteOrder.nativeOrder());
        IntBuffer viewport = IntBuffer.allocate(4);
        gl.glGetIntegerv(GL2ES2.GL_VIEWPORT, viewport);
        gl.glReadPixels(x, viewport.get(3) - y, 1, 1, GL2ES2.GL_RGBA,
                GL2ES2.GL_UNSIGNED_BYTE, pixel);

        int qp = pixel.get(0) & 0xFF;
        int index = Math.round(((qp/255.0f)*(count+2))-1);
        if(index < 0 || index >= count)
            return -1;
        return index;
    }

    private void render(GL2ES2 gl, int width, int height, int renderModes, int[/*1*/] sampleCount, boolean select) {
        final PMVMatrix pmv = renderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        // System.err.printf("SceneUICtrl.render.1.0: scale.0: %f, %f, %f%n", scale[0], scale[1], scale[2]);
        // System.err.printf("SceneUICtrl.render.1.0: translate.0: %f, %f, %f%n", translate[0], translate[1], translate[2]);
        pmv.glTranslatef(translate[0], translate[1], translate[2]);
        pmv.glRotate(quaternion);
        pmv.glScalef(scale[0], scale[1], scale[2]);

        for(int index=0; index < count;index++){
            if(select) {
                float color= index+1;
                renderer.setColorStatic(gl, color/(count+2), color/(count+2), color/(count+2));
            }
            final UIShape uiShape = shapes.get(index);
            uiShape.validate(gl, renderer);
            final float[] uiTranslate = uiShape.getTranslate();

            pmv.glPushMatrix();
            // System.err.printf("SceneUICtrl.render.1.0: translate.1: %f, %f%n", uiTranslate[0], uiTranslate[1]);
            pmv.glTranslatef(uiTranslate[0], uiTranslate[1], 0f);
            renderer.updateMatrix(gl);
            uiShape.drawShape(gl, renderer, sampleCount, select);
            pmv.glPopMatrix();
        }
    }

    public void setTranslate(float x, float y, float z) {
        this.translate[0] = x;
        this.translate[1] = y;
        this.translate[2] = z;
    }

    public void setScale(float x, float y, float z) {
        this.scale[0] = x;
        this.scale[1] = y;
        this.scale[2] = z;
    }

    public void setRotation(float x, float y, float z) {
        quaternion.setFromEuler(x * FloatUtil.PI / 180.0f,
                                y * FloatUtil.PI / 180.0f,
                                z * FloatUtil.PI / 180.0f);
    }
    public float[] getSceneClearColor() {
        return sceneClearColor;
    }

    public void setSceneClearColor(float r, float g, float b, float a) {
        this.sceneClearColor[0] = r;
        this.sceneClearColor[1] = g;
        this.sceneClearColor[2] = b;
        this.sceneClearColor[3] = a;
    }

    private class SBCMouseListener implements MouseListener {
        int mouseX = -1;
        int mouseY = -1;

        @Override
        public void mouseClicked(MouseEvent e) {
            UIShape uiShape = getActiveUI();
            if(uiShape != null){
                uiShape.onClick(e);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if(null==cDrawable) {
                return;
            }
            mouseX = e.getX();
            mouseY = e.getY();

            GLRunnable runnable = new GLRunnable() {
                @Override
                public boolean run(GLAutoDrawable drawable) {
                    UIShape s = getShape(drawable, mouseX, mouseY);
                    if(null != s) {
                        activeId = getShapes().indexOf(s);
                    }
                    else {
                        activeId = -1;
                    }
                    return false;
                }
            };
            cDrawable.invoke(true, runnable);

            UIShape uiShape = getActiveUI();

            if(uiShape != null) {
                uiShape.setPressed(true);
                uiShape.onPressed(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            UIShape uiShape = getActiveUI();
            if(uiShape != null){
                uiShape.setPressed(false);
                uiShape.onRelease(e);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) { }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) { }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseWheelMoved(MouseEvent e) { }

    }
}