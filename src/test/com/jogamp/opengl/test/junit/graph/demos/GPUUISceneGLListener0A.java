package com.jogamp.opengl.test.junit.graph.demos;

import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPipelineFactory;
import javax.media.opengl.GLRunnable;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.graph.demos.ui.Label;
import com.jogamp.opengl.test.junit.graph.demos.ui.RIButton;
import com.jogamp.opengl.test.junit.graph.demos.ui.SceneUIController;
import com.jogamp.opengl.util.glsl.ShaderState;

public class GPUUISceneGLListener0A implements GLEventListener {

    private boolean debug = false;
    private boolean trace = false;

    private final int renderModes;
    private final int[] sampleCount = new int[1];
    private final int[] texSize2 = new int[1];
    private final RenderState rs;
    private final boolean useBlending;
    private final SceneUIController sceneUIController;
    protected final float zNear = 0.1f, zFar = 7000f;

    private RegionRenderer renderer;

    int fontSet = FontFactory.UBUNTU;
    Font font;
    final float fontSizeFixed = 6;
    float dpiH = 96;

    private float xTran = 0;
    private float yTran = 0;
    private float ang = 0f;
    private float zoom = -200f;
    private final float zoomText = 1f;
    private int currentText = 0;

    private Label[] labels = null;
    private String[] strings = null;
    private RIButton[] buttons = null;
    private Label jogampLabel = null;
    private Label fpsLabel = null;
    private final int numSelectable = 6;

    private MultiTouchListener multiTouchListener = null;
    private boolean showFPS = false;
    private GLAutoDrawable cDrawable;

    private final String jogamp = "JogAmp - Jogl Graph Module Demo";
    private final float angText = 0;

    public GPUUISceneGLListener0A() {
      this(0);
    }

    public GPUUISceneGLListener0A(int renderModes) {
      this(RenderState.createRenderState(new ShaderState(), SVertex.factory()), renderModes, false, false);
    }

    public GPUUISceneGLListener0A(RenderState rs, int renderModes, boolean debug, boolean trace) {
        this.rs = rs;
        this.renderModes = renderModes;
        this.sampleCount[0] = 4;
        this.texSize2[0] = 0;
        this.useBlending = true;

        this.debug = debug;
        this.trace = trace;
        try {
            font = FontFactory.get(FontFactory.UBUNTU).getDefault();
        } catch (IOException ioe) {
            System.err.println("Catched: "+ioe.getMessage());
            ioe.printStackTrace();
        }
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

        buttons[0].translate(xaxis,start);

        buttons[1] = new RIButton(SVertex.factory(), font, "Show FPS", xSize, ySize){
            public void onClick() {
                final GLAnimatorControl a = cDrawable.getAnimator();
                if( null != a ) {
                    a.resetFPSCounter();
                }
                showFPS = !showFPS;
            }
        };
        buttons[1].translate(xaxis,start - diff);
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
        };
        buttons[2].translate(xaxis,start-diff*2);
        buttons[2].setToggleable(true);

        buttons[3] = new RIButton(SVertex.factory(), font, "Tilt  +Y", xSize, ySize) {
            public void onClick() {
                ang+=10;
            }
        };
        buttons[3].translate(xaxis,start-diff*3);

        buttons[4] = new RIButton(SVertex.factory(), font, "Tilt  -Y", xSize, ySize){
            public void onClick() {
                ang-=10;
            }
        };
        buttons[4].translate(xaxis,start-diff*4);

        buttons[5] = new RIButton(SVertex.factory(), font, "Quit", xSize, ySize){
            public void onClick() {
                cDrawable.destroy();
            }
        };
        buttons[5].translate(xaxis,start-diff*5);
        buttons[5].setColor(0.8f, 0.0f, 0.0f);
        buttons[5].setLabelColor(1.0f, 1.0f, 1.0f);

        buttons[5].setSelectedColor(0.8f, 0.8f, 0.8f);
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
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            final float[] pixelsPerMM = new float[2];
            ((Window)upObj).getMainMonitor().getPixelsPerMM(pixelsPerMM);
            dpiH = pixelsPerMM[1]*25.4f;
        }
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

        try {
            font = FontFactory.get(fontSet).getDefault();
        } catch (IOException ioe) {
            System.err.println("Catched: "+ioe.getMessage());
            ioe.printStackTrace();
        }

        renderer = RegionRenderer.create(rs, renderModes, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);

        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        gl.glEnable(GL2ES2.GL_BLEND);

        renderer.init(gl);
        renderer.setAlpha(gl, 1.0f);
        renderer.setColorStatic(gl, 0.0f, 0.0f, 0.0f);

        initTexts();
        initButtons(width, height);

        sceneUIController.setRenderer(renderer, renderModes, sampleCount);
        sceneUIController.addShape(buttons[0]);
        sceneUIController.addShape(buttons[1]);
        sceneUIController.addShape(buttons[2]);
        sceneUIController.addShape(buttons[3]);
        sceneUIController.addShape(buttons[4]);
        sceneUIController.addShape(buttons[5]);
        sceneUIController.init(drawable);

        final float pixelSizeFixed = font.getPixelSize(fontSizeFixed, dpiH);
        jogampLabel = new Label(SVertex.factory(), font, pixelSizeFixed, jogamp);

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

        sceneUIController.dispose(drawable);

        GL2ES2 gl = drawable.getGL().getGL2ES2();
        renderer.destroy(gl);
    }

    public void display(GLAutoDrawable drawable) {
        // System.err.println("GPUUISceneGLListener0A: display");
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        renderer.resetModelview(null);
        sceneUIController.setTranslate(xTran, yTran, zoom);
        sceneUIController.setRotation(0, ang, 0);
        sceneUIController.display(drawable);

        final float pixelSizeFixed = font.getPixelSize(fontSizeFixed, dpiH);

        renderer.resetModelview(null);
        renderer.translate(null, xTran-50, yTran+43, zoom);
        renderer.translate(gl, 0, 30, 0);
        renderer.scale(null, zoomText, zoomText, 1);
        renderer.scale(gl, 1.5f, 1.5f, 1.0f);
        renderer.rotate(gl, angText , 0, 1, 0);
        renderer.setColorStatic(gl, 0.0f, 1.0f, 0.0f);
        jogampLabel.drawShape(gl, renderer, sampleCount, false);
        if(null == labels[currentText]) {
            labels[currentText] = new Label(SVertex.factory(), font, pixelSizeFixed, strings[currentText]);
        }

        renderer.resetModelview(null);
        renderer.translate(null, xTran-50, yTran, zoom);
        renderer.translate(gl, 0, 30, 0);
        renderer.scale(null, zoomText, zoomText, 1);
        renderer.scale(gl, 1.5f, 1.5f, 1.0f);
        renderer.rotate(gl, zoomText, 0, 1, 0);

        renderer.setColorStatic(gl, 0.0f, 0.0f, 0.0f);
        labels[currentText].drawShape(gl, renderer, sampleCount, false);

        if( showFPS ) {
            final float lfps, tfps, td;
            final GLAnimatorControl animator = drawable.getAnimator();
            if( null != animator ) {
                lfps = animator.getLastFPS();
                tfps = animator.getTotalFPS();
                td = animator.getTotalFPSDuration()/1000f;
            } else {
                lfps = 0f;
                tfps = 0f;
                td = 0f;
            }
            final String modeS = Region.getRenderModeString(renderer.getRenderModes());
            final String text = String.format("%03.1f/%03.1f fps, v-sync %d, fontSize %.1f, %s-samples %d, td %4.1f, blend %b, alpha-bits %d",
                    lfps, tfps, gl.getSwapInterval(), fontSizeFixed, modeS, sampleCount[0], td,
                    useBlending, drawable.getChosenGLCapabilities().getAlphaBits());
            if(null != fpsLabel) {
                fpsLabel.clear(gl, renderer);
                fpsLabel.setText(text);
                fpsLabel.setPixelSize(pixelSizeFixed);
            } else {
                fpsLabel = new Label(renderer.getRenderState().getVertexFactory(), font, pixelSizeFixed, text);
            }
            renderer.translate(gl, 0, -60, 0);
            renderer.scale(null, zoomText, zoomText, 1);
            fpsLabel.drawShape(gl, renderer, sampleCount, false);
        }
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.err.println("GPUUISceneGLListener0A: reshape");
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glViewport(x, y, width, height);
        renderer.reshapePerspective(gl, 45.0f, width, height, zNear, zFar);
        sceneUIController.reshape(drawable, x, y, width, height);
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

        @Override
        public void mouseWheelMoved(MouseEvent e) {
            if( !e.isShiftDown() ) {
                zoom += 2f*e.getRotation()[1]; // vertical: wheel
            }
        }
    }
}