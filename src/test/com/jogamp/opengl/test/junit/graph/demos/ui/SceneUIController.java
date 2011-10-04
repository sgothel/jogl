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

import com.jogamp.common.nio.Buffers;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;

public class SceneUIController implements GLEventListener{
    private ArrayList<UIShape> shapes = new ArrayList<UIShape>();

    private int count = 0;
    private int renderModes; 
    private int texSize; 
    private RegionRenderer renderer = null;
    private RenderState rs = null;

    private float[] translate = new float[3];
    private float[] scale = new float[3];
    private float[] rotation = new float[3];

    private float[] sceneClearColor = new float[]{0,0,0,1};
    
    private int activeId = -1;
    
    private SBCMouseListener sbcMouseListener = null;
    
    private GLAutoDrawable cDrawable = null;

    public SceneUIController() {
    }
    
    public void setRenderer(RegionRenderer renderer, RenderState rs, int renderModes, int texSize) {
        this.renderer = renderer;
        this.rs = rs;
        this.renderModes = renderModes;
        this.texSize = texSize;
    }
    
    public SceneUIController(RegionRenderer renderer, RenderState rs, int renderModes, int texSize) {
        this.renderer = renderer;
        this.rs = rs;
        this.renderModes = renderModes;
        this.texSize = texSize;
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
    
    public void init(GLAutoDrawable drawable) {
        System.err.println("SceneUIController: init");
        cDrawable = drawable;
    }
    public void display(GLAutoDrawable drawable) {
        // System.err.println("SceneUIController: display");
        final int width = drawable.getWidth();
        final int height = drawable.getHeight();
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        render(gl, width, height, renderModes, texSize, false);
    }
   
    public void dispose(GLAutoDrawable drawable) {
        System.err.println("SceneUIController: dispose");
        cDrawable = null;
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
            int height) {
        System.err.println("SceneUIController: reshape");
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        renderer.reshapePerspective(gl, 45.0f, width, height, 5f, 70.0f);        
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

        render(gl, width, height, 0, 0, true);
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

    private void render(GL2ES2 gl, int width, int height, int renderModes, int texSize, boolean select) {
        renderer.reshapePerspective(null, 45.0f, width, height, 0.1f, 7000.0f);
        
        for(int index=0; index < count;index++){
            if(select) {
                float color= index+1;
                renderer.setColorStatic(gl, color/(count+2), color/(count+2), color/(count+2));
            }
            float[] s = shapes.get(index).getScale();
            float[] p = shapes.get(index).getPosition();
            renderer.resetModelview(null);
            renderer.translate(null, translate[0]+p[0], translate[1]+p[1], translate[2]+p[2]);
            renderer.scale(gl, s[0], s[1], s[2]);
            renderer.rotate(gl, rotation[0], 1, 0, 0);
            renderer.rotate(gl, rotation[1], 0, 1, 0);
            renderer.rotate(gl, rotation[2], 0, 0, 1);
            
            shapes.get(index).render(gl, rs, renderer, renderModes, texSize, select);
            renderer.rotate(gl, -rotation[0], 1, 0, 0);
            renderer.rotate(gl, -rotation[1], 0, 1, 0);
            renderer.rotate(gl, -rotation[2], 0, 0, 1);
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
        this.rotation[0] = x;
        this.rotation[1] = y;
        this.rotation[2] = z;
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
        
        public void mouseClicked(MouseEvent e) {
            UIShape uiShape = getActiveUI();
            if(uiShape != null){
                uiShape.onClick();
            }
        }

        public void mousePressed(MouseEvent e) {
            if(null==cDrawable) {
                return;
            }
            mouseX = e.getX();
            mouseY = e.getY();
            
            GLRunnable runnable = new GLRunnable() {
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
                uiShape.onPressed();
            }
        }

        public void mouseReleased(MouseEvent e) { 
            UIShape uiShape = getActiveUI();
            if(uiShape != null){
                uiShape.setPressed(false);
                uiShape.onRelease();
            }
        }

        public void mouseMoved(MouseEvent e) { }
        public void mouseEntered(MouseEvent e) { }
        public void mouseExited(MouseEvent e) { }
        public void mouseDragged(MouseEvent e) { }
        public void mouseWheelMoved(MouseEvent e) { }
        
    }
}