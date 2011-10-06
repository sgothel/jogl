package com.jogamp.opengl.test.junit.graph.demos;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPipelineFactory;
import javax.media.opengl.GLRunnable;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.graph.demos.ui.Label;
import com.jogamp.opengl.test.junit.graph.demos.ui.RIButton;
import com.jogamp.opengl.test.junit.graph.demos.ui.SceneUIController;
import com.jogamp.opengl.test.junit.graph.demos.ui.opengl.UIRegion;
import com.jogamp.opengl.util.glsl.ShaderState;

public class GPUUISceneGLListener0A implements GLEventListener {

    private boolean debug = false;
    private boolean trace = false; 
    
    private final int renderModes;
    private final int texSize; 
    private final int renderModes2;
    private final int texSize2; 
    private RegionRenderer regionRenderer;
    private RenderState rs;
    
    int fontSet = FontFactory.UBUNTU;
    Font font;
    final int fontSizeFixed = 6;
    
    private float xTran = 0;
    private float yTran = 0;    
    private float ang = 0f;
    private float zoom = -200f;
    private float zoomText = 1f;
    private int currentText = 0;
    
    private Label[] labels = null;
    private String[] strings = null;
    private UIRegion[] labelRegions;
    private UIRegion fpsRegion = null;
    private UIRegion jogampRegion = null;
    private RIButton[] buttons = null;
    
    private int numSelectable = 6;
    
    private SceneUIController sceneUIController = null;
    private MultiTouchListener multiTouchListener = null;
    private boolean showFPS = false;
    private GLAutoDrawable cDrawable;
    private float fps = 0; 
    
    private String jogamp = "JogAmp - Jogl Graph Module Demo";
    private float angText = 0;
    
    public GPUUISceneGLListener0A() {
      this(0);
    }
    
    public GPUUISceneGLListener0A(int renderModes) {
      this(RenderState.createRenderState(new ShaderState(), SVertex.factory()), renderModes, false, false);
    }
    
    public GPUUISceneGLListener0A(RenderState rs, int renderModes, boolean debug, boolean trace) {
        this.rs = rs;
        this.renderModes = renderModes;
        this.texSize = Region.isVBAA(renderModes) ? 400 : 0;
        this.renderModes2 = 0;
        this.texSize2 = 0;
        
        this.debug = debug;
        this.trace = trace;
        font = FontFactory.get(FontFactory.UBUNTU).getDefault();
        labelRegions = new UIRegion[3];
        sceneUIController = new SceneUIController();
    }
    
    private void initButtons(int width, int height) {
        buttons = new RIButton[numSelectable];
        int xaxis = -110;
        float xSize = 40f;
        float ySize = 16f;
        
        int start = 50;
        int diff = (int)ySize + 5;
        
        buttons[0] = new RIButton(SVertex.factory(), font, "Next Text", xSize, ySize){
            public void onClick() {
                   currentText = (currentText+1)%3;
            }
            public void onPressed() { }
            public void onRelease() { }
        };
        
        buttons[0].setPosition(xaxis,start,0);
        
        buttons[1] = new RIButton(SVertex.factory(), font, "Show FPS", xSize, ySize){
            public void onClick() {
                final GLAnimatorControl a = cDrawable.getAnimator();
                if( null != a ) {
                    a.resetFPSCounter();
                }
                showFPS = !showFPS;
            }
            public void onPressed() { }
            public void onRelease() { }
        };
        buttons[1].setPosition(xaxis,start - diff,0);
        buttons[1].setToggleable(true);
        
        buttons[2] = new RIButton(SVertex.factory(), font, "v-sync", xSize, ySize){
            public void onClick() {
                cDrawable.invoke(false, new GLRunnable() {
                    public boolean run(GLAutoDrawable drawable) {
                        GL gl = drawable.getGL();
                        gl.setSwapInterval(gl.getSwapInterval()<=0?1:0);
                        final GLAnimatorControl a = drawable.getAnimator();
                        if( null != a ) {
                            a.resetFPSCounter();
                        }
                        return true;
                    }
                });
            }
            public void onPressed() { }
            public void onRelease() { }
        };
        buttons[2].setPosition(xaxis,start-diff*2,0);
        buttons[2].setToggleable(true);
        
        buttons[3] = new RIButton(SVertex.factory(), font, "Tilt  +Y", xSize, ySize) {
            public void onClick() {  
                ang+=10;
            }
            public void onPressed() { 

            }
            public void onRelease() { }
        };
        buttons[3].setPosition(xaxis,start-diff*3,0);
        
        buttons[4] = new RIButton(SVertex.factory(), font, "Tilt  -Y", xSize, ySize){
            public void onClick() {
                ang-=10;
            }
            public void onPressed() { }
            public void onRelease() { }
        };
        buttons[4].setPosition(xaxis,start-diff*4,0);
        
        buttons[5] = new RIButton(SVertex.factory(), font, "Quit", xSize, ySize){
            public void onClick() {
                cDrawable.destroy();
            }
            public void onPressed() { }
            public void onRelease() { }
        };
        buttons[5].setPosition(xaxis,start-diff*5,0);
        buttons[5].setButtonColor(0.8f, 0.0f, 0.0f);
        buttons[5].setLabelColor(1.0f, 1.0f, 1.0f);
        
        buttons[5].setButtonSelectedColor(0.8f, 0.8f, 0.8f);
        buttons[5].setLabelSelectedColor(0.8f, 0.0f, 0.0f);
    }
    
    private void initTexts() {
        strings = new String[3];
        
        strings[0] = "abcdefghijklmn\nopqrstuvwxyz\nABCDEFGHIJKL\nMNOPQRSTUVWXYZ\n0123456789.:,;(*!?/\\\")$%^&-+@~#<>{}[]";
        strings[1] = "The quick brown fox\njumps over the lazy\ndog";
        
        strings[2] = 
            "Lorem ipsum dolor sit amet, consectetur\n"+
            "Ut purus odio, rhoncus sit amet com\n"+
            "quam iaculis urna cursus ornare. Nullam\n"+
            "In hac habitasse platea dictumst. Vivam\n"+ 
            "Morbi quis bibendum nibh. Donec lectus\n"+
            "Donec ut dolor et nulla tristique variu\n"+
            "in lorem. Maecenas in ipsum ac justo sc\n";
        
        labels = new Label[3];
    }

    public void init(GLAutoDrawable drawable) {
        if(drawable instanceof GLWindow) {
            System.err.println("GPUUISceneGLListener0A: init (1)");
            final GLWindow glw = (GLWindow) drawable;
            attachInputListenerTo(glw);
        } else {
            System.err.println("GPUUISceneGLListener0A: init (0)");            
        }
        final int width = drawable.getWidth();
        final int height = drawable.getHeight();
        cDrawable = drawable;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(debug) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }
        
        this.font = FontFactory.get(fontSet).getDefault();
        regionRenderer = RegionRenderer.create(rs, renderModes); 
        
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        gl.glEnable(GL2ES2.GL_BLEND);
        
        regionRenderer.init(gl);
        regionRenderer.setAlpha(gl, 1.0f);
        regionRenderer.setColorStatic(gl, 0.0f, 0.0f, 0.0f);
        
        initTexts();
        initButtons(width, height);
        
        sceneUIController.setRenderer(regionRenderer, rs, renderModes, texSize);
        sceneUIController.addShape(buttons[0]);
        sceneUIController.addShape(buttons[1]);
        sceneUIController.addShape(buttons[2]);
        sceneUIController.addShape(buttons[3]);
        sceneUIController.addShape(buttons[4]);
        sceneUIController.addShape(buttons[5]);
        drawable.addGLEventListener(sceneUIController);
                
        Label jlabel = new Label(SVertex.factory(), font, fontSizeFixed, jogamp){
            public void onClick() { }
            public void onPressed() { }
            public void onRelease() { }
        };
        
        jogampRegion = new UIRegion(jlabel);
        final GLAnimatorControl a = drawable.getAnimator();
        if( null != a ) {
            a.resetFPSCounter();
        }        
    }

    public void dispose(GLAutoDrawable drawable) {
        if(drawable instanceof GLWindow) {
            System.err.println("GPUUISceneGLListener0A: dispose (1)");
            final GLWindow glw = (GLWindow) drawable;
            detachInputListenerFrom(glw);
        } else {
            System.err.println("GPUUISceneGLListener0A: dispose (0)");            
        }
        
        drawable.removeGLEventListener(sceneUIController);
        sceneUIController.dispose(drawable);
        
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        regionRenderer.destroy(gl);
    }

    public void display(GLAutoDrawable drawable) {
        // System.err.println("GPUUISceneGLListener0A: display");
        final int width = drawable.getWidth();
        final int height = drawable.getHeight();
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        
        regionRenderer.reshapePerspective(null, 45.0f, width, height, 0.1f, 7000.0f);
        sceneUIController.setTranslate(xTran, yTran, zoom);
        sceneUIController.setRotation(0, ang, 0);
        
        renderScene(drawable);
  }
    
    private void renderScene(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        regionRenderer.resetModelview(null);
        regionRenderer.translate(null, xTran-50, yTran+43, zoom);
        regionRenderer.translate(gl, 0, 30, 0);
        regionRenderer.scale(null, zoomText, zoomText, 1);
        regionRenderer.scale(gl, 1.5f, 1.5f, 1.0f);
        regionRenderer.rotate(gl, angText , 0, 1, 0);
        regionRenderer.setColorStatic(gl, 0.0f, 1.0f, 0.0f);
        regionRenderer.draw(gl, jogampRegion.getRegion(gl, rs, 0), new float[]{0,0,0}, 0);
        
        if(null == labelRegions[currentText]) {
            if( null == labels[currentText]) {
                labels[currentText] = new Label(SVertex.factory(), font, fontSizeFixed, strings[currentText]){
                    public void onClick() { }
                    public void onPressed() { }
                    public void onRelease() { }
                };
            }
            labelRegions[currentText] = new UIRegion(labels[currentText]);
        }
        
        regionRenderer.resetModelview(null);
        regionRenderer.translate(null, xTran-50, yTran, zoom);
        regionRenderer.translate(gl, 0, 30, 0);
        regionRenderer.scale(null, zoomText, zoomText, 1);
        regionRenderer.scale(gl, 1.5f, 1.5f, 1.0f);
        regionRenderer.rotate(gl, zoomText, 0, 1, 0);
        
        regionRenderer.setColorStatic(gl, 0.0f, 0.0f, 0.0f);
        regionRenderer.draw(gl, labelRegions[currentText].getRegion(gl, rs, renderModes2), new float[]{0,0,0}, texSize2);
        
        final GLAnimatorControl animator = drawable.getAnimator();
        final boolean _drawFPS = showFPS && null != animator;
        
        if(_drawFPS && fps != animator.getTotalFPS()) {
            if(null != fpsRegion) {
                fpsRegion.destroy(gl, rs);
            }
            fps = animator.getTotalFPS();
            final String fpsS = String.valueOf(fps);
            final int fpsSp = fpsS.indexOf('.');
            
            Label fpsLabel = new Label(SVertex.factory(), font, fontSizeFixed, fpsS.substring(0, fpsSp+2)+" fps"){
                public void onClick() { }
                public void onPressed() { }
                public void onRelease() { }
            };
            fpsRegion = new UIRegion(fpsLabel);
        }    
        if(showFPS && null != fpsRegion) {
            regionRenderer.translate(gl, 0, -60, 0);
            regionRenderer.scale(null, zoomText, zoomText, 1);
            regionRenderer.draw(gl, fpsRegion.getRegion(gl, rs, 0), new float[]{0,0,0}, 0);
        }
    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.err.println("GPUUISceneGLListener0A: reshape");
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        gl.glViewport(x, y, width, height);        
        regionRenderer.reshapePerspective(gl, 45.0f, width, height, 5f, 70.0f);
    }

    public void attachInputListenerTo(GLWindow window) {
        if ( null == multiTouchListener ) {
            multiTouchListener = new MultiTouchListener();
            window.addMouseListener(multiTouchListener);
            sceneUIController.attachInputListenerTo(window);
        }
    }
    
    public void detachInputListenerFrom(GLWindow window) {
        if ( null != multiTouchListener ) {
            window.removeMouseListener(multiTouchListener);
            sceneUIController.detachInputListenerFrom(window);
        }
    }
    
    private class MultiTouchListener extends MouseAdapter {
        int lx = 0;
        int ly = 0;
        
        boolean first = false;
        
        @Override
        public void mousePressed(MouseEvent e) {
            first = true;  
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            first = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            System.err.println("demo:mousedragged "+e);
            if(e.getPointerCount()==2) {
                // 2 pointers zoom ..
                if(first) {
                    lx = Math.abs(e.getY(0)-e.getY(1));
                    first=false;
                    return;
                }
                int nv = Math.abs(e.getY(0)-e.getY(1));
                int dy = nv - lx;
                
                zoom += 2 * Math.signum(dy);
                
                lx = nv;
            } else {
                // 1 pointer drag
                if(first) {
                    lx = e.getX();
                    ly = e.getY();
                    first=false;
                    return;
                }
                int nx = e.getX();
                int ny = e.getY();
                int dx = nx - lx;       
                int dy = ny - ly;
                if(Math.abs(dx) > Math.abs(dy)){
                    xTran += Math.signum(dx);
                }
                else {
                    yTran -= Math.signum(dy);
                }
                lx = nx;
                ly = ny;
            }
        }
    }
}      