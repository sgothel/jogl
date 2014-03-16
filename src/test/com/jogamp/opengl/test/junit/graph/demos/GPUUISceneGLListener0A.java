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
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.test.junit.graph.demos.ui.Label;
import com.jogamp.opengl.test.junit.graph.demos.ui.RIButton;
import com.jogamp.opengl.test.junit.graph.demos.ui.SceneUIController;
import com.jogamp.opengl.test.junit.graph.demos.ui.UIShape;
import com.jogamp.opengl.util.glsl.ShaderState;

public class GPUUISceneGLListener0A implements GLEventListener {

    private boolean debug = false;
    private boolean trace = false;

    private final int renderModes;
    private final RenderState rs;
    private final SceneUIController sceneUIController;

    private RegionRenderer renderer;

    int fontSet = FontFactory.UBUNTU;
    Font font;
    final float buttonXSize = 84f;
    final float buttonYSize = buttonXSize/2.5f;
    final float fontSizeFixed = 12f;
    final float fontSizeFPS = 10f;
    float dpiH = 96;

    private int currentText = 0;

    private Label[] labels = null;
    private String[] strings = null;
    private RIButton[] buttons = null;
    private Label jogampLabel = null;
    private Label fpsLabel = null;
    private final int numSelectable = 6;

    private boolean ioAttached = false;
    private GLAutoDrawable cDrawable;

    private final String jogamp = "JogAmp - Jogl Graph Module Demo";

    public GPUUISceneGLListener0A() {
      this(0);
    }

    public GPUUISceneGLListener0A(int renderModes) {
      this(RenderState.createRenderState(new ShaderState(), SVertex.factory()), renderModes, false, false);
    }

    public GPUUISceneGLListener0A(RenderState rs, int renderModes, boolean debug, boolean trace) {
        this.rs = rs;
        this.renderModes = renderModes;

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

    private void rotateButtons(MouseEvent e, float angdeg) {
        for(int i=0; i<buttons.length; i++) {
            rotateInstance(e, buttons[i].getRotation(), angdeg);
        }
    }
    private void rotateInstance(MouseEvent e, Quaternion quat, float angdeg) {
        final float angrad = angdeg * FloatUtil.PI / 180.0f;
        if( e.isControlDown() ) {
            quat.rotateByAngleZ(angrad);
        } else if( e.isShiftDown() ) {
            quat.rotateByAngleX(angrad);
        } else {
            quat.rotateByAngleY(angrad);
        }
    }

    private void initButtons() {
        buttons = new RIButton[numSelectable];

        final float xstart = 0f;
        final float ystart = 0f;
        final float diff = 1.5f * buttonYSize;

        buttons[0] = new RIButton(SVertex.factory(), font, "Next Text", buttonXSize, buttonYSize);
        buttons[0].translate(xstart,ystart, 0f);
        buttons[0].setLabelColor(1.0f, 1.0f, 1.0f);
        buttons[0].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if( null != labels[currentText] ) {
                    labels[currentText].setEnabled(false);
                }
                currentText = (currentText+1)%3;
                if( null != labels[currentText] ) {
                    labels[currentText].setEnabled(true);
                }
            } } );
        buttons[0].addMouseListener(new DragAndZoomListener(buttons[0]));

        buttons[1] = new RIButton(SVertex.factory(), font, "Show FPS", buttonXSize, buttonYSize);
        buttons[1].translate(xstart,ystart - diff, 0f);
        buttons[1].setToggleable(true);
        buttons[1].setLabelColor(1.0f, 1.0f, 1.0f);
        buttons[1].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final GLAnimatorControl a = cDrawable.getAnimator();
                if( null != a ) {
                    a.resetFPSCounter();
                }
                fpsLabel.setEnabled(!fpsLabel.isEnabled());
            } } );
        buttons[1].addMouseListener(new DragAndZoomListener(buttons[1]));

        buttons[2] = new RIButton(SVertex.factory(), font, "v-sync", buttonXSize, buttonYSize);
        buttons[2].translate(xstart,ystart-diff*2, 0f);
        buttons[2].setToggleable(true);
        buttons[2].setLabelColor(1.0f, 1.0f, 1.0f);
        buttons[2].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cDrawable.invoke(false, new GLRunnable() {
                    @Override
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
            } } );
        buttons[2].addMouseListener(new DragAndZoomListener(buttons[2]));

        buttons[3] = new RIButton(SVertex.factory(), font, "Tilt  +Y", buttonXSize, buttonYSize);
        buttons[3].translate(xstart,ystart-diff*3, 0f);
        buttons[3].setLabelColor(1.0f, 1.0f, 1.0f);
        buttons[3].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                rotateButtons(e, 5f);
            } } );
        buttons[3].addMouseListener(new DragAndZoomListener(buttons[3]));

        buttons[4] = new RIButton(SVertex.factory(), font, "Tilt  -Y", buttonXSize, buttonYSize);
        buttons[4].translate(xstart,ystart-diff*4, 0f);
        buttons[4].setLabelColor(1.0f, 1.0f, 1.0f);
        buttons[4].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                rotateButtons(e, -5f);
            } } );
        buttons[4].addMouseListener(new DragAndZoomListener(buttons[4]));

        buttons[5] = new RIButton(SVertex.factory(), font, "Quit", buttonXSize, buttonYSize);
        buttons[5].translate(xstart,ystart-diff*5, 0f);
        buttons[5].setColor(0.8f, 0.0f, 0.0f);
        buttons[5].setLabelColor(1.0f, 1.0f, 1.0f);

        buttons[5].setSelectedColor(0.8f, 0.8f, 0.8f);
        buttons[5].setLabelSelectedColor(0.8f, 0.0f, 0.0f);
        buttons[5].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cDrawable.destroy();
            } } );
        buttons[5].addMouseListener(new DragAndZoomListener(buttons[5]));
    }

    private void initTexts() {
        strings = new String[3];

        strings[0] = "Next Text\n"+
                     "Show FPS\n"+
                     "abcdefghijklmn\nopqrstuvwxyz\n"+
                     "ABCDEFGHIJKL\n"+
                     "MNOPQRSTUVWXYZ\n"+
                     "0123456789.:,;(*!?/\\\")$%^&-+@~#<>{}[]";

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

    @Override
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

        initTexts();
        initButtons();

        sceneUIController.setRenderer(renderer);
        sceneUIController.addShape(buttons[0]);
        sceneUIController.addShape(buttons[1]);
        sceneUIController.addShape(buttons[2]);
        sceneUIController.addShape(buttons[3]);
        sceneUIController.addShape(buttons[4]);
        sceneUIController.addShape(buttons[5]);

        final float pixelSizeFixed = font.getPixelSize(fontSizeFixed, dpiH);
        jogampLabel = new Label(SVertex.factory(), font, pixelSizeFixed, jogamp);
        jogampLabel.addMouseListener(new DragAndZoomListener(jogampLabel));
        sceneUIController.addShape(jogampLabel);

        final float pixelSizeFPS = font.getPixelSize(fontSizeFPS, dpiH);
        fpsLabel = new Label(renderer.getRenderState().getVertexFactory(), font, pixelSizeFPS, "Nothing there yet");
        fpsLabel.translate(0f, 0f, 0f); // FIXME
        fpsLabel.addMouseListener(new DragAndZoomListener(fpsLabel));
        fpsLabel.addMouseListener(new DragAndZoomListener(fpsLabel));
        sceneUIController.addShape(fpsLabel);

        sceneUIController.init(drawable);

        final GLAnimatorControl a = drawable.getAnimator();
        if( null != a ) {
            a.resetFPSCounter();
        }
    }

    @Override
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

    protected void setupPMV(GLAutoDrawable drawable) {

    }

    @Override
    public void display(GLAutoDrawable drawable) {
        // System.err.println("GPUUISceneGLListener0A: display");
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if(null == labels[currentText]) {
            final float pixelSizeFixed = font.getPixelSize(fontSizeFixed, dpiH);
            float dx = drawable.getWidth() * 1f/3f;
            labels[currentText] = new Label(SVertex.factory(), font, pixelSizeFixed, strings[currentText]);
            labels[currentText].setColor(0, 0, 0);
            labels[currentText].translate(dx, -3f * jogampLabel.getLineHeight(), 0f);
            labels[currentText].setEnabled(true);
            labels[currentText].addMouseListener(new DragAndZoomListener(labels[currentText]));
            sceneUIController.addShape(labels[currentText]);
        }
        if( fpsLabel.isEnabled() ) {
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
                    lfps, tfps, gl.getSwapInterval(), fontSizeFixed, modeS, sceneUIController.getSampleCount(), td,
                    renderer.getRenderState().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED),
                    drawable.getChosenGLCapabilities().getAlphaBits());
            fpsLabel.clear(gl, renderer);
            fpsLabel.setText(text);
        }
        sceneUIController.display(drawable);
    }


    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.err.println("GPUUISceneGLListener0A: reshape");

        float dx = width * 1f/3f;
        jogampLabel.setTranslate(dx, 0f, 0f);
        fpsLabel.setTranslate(0f, 0f, 0f); // FIXME
        if( null != labels[currentText] ) {
            labels[currentText].setTranslate(dx, -3f * jogampLabel.getLineHeight(), 0f);
        }

        sceneUIController.reshape(drawable, x, y, width, height);
   }

    public void attachInputListenerTo(GLWindow window) {
        if ( !ioAttached ) {
            ioAttached = true;
            sceneUIController.attachInputListenerTo(window);
        }
    }

    public void detachInputListenerFrom(GLWindow window) {
        if ( ioAttached ) {
            ioAttached = false;
            sceneUIController.detachInputListenerFrom(window);
        }
    }

    private class DragAndZoomListener extends MouseAdapter {
        final UIShape shape;
        int lx=-1, ly=-1;
        boolean first = false;

        public DragAndZoomListener(UIShape shape) {
            this.shape = shape;
        }

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
            if(e.getPointerCount()==2) {
                // 2 pointers zoom ..
                if(first) {
                    lx = Math.abs(e.getY(0)-e.getY(1));
                    first=false;
                    return;
                }
                int nv = Math.abs(e.getY(0)-e.getY(1));
                int dy = nv - lx;
                lx = nv;

                shape.translate(0f, 0f, 2 * Math.signum(dy));
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
                    shape.translate(Math.signum(dx), 0f, 0f);
                } else {
                    shape.translate(0f, -Math.signum(dy), 0f);
                }
                lx = nx;
                ly = ny;
            }
        }

        @Override
        public void mouseWheelMoved(MouseEvent e) {
            if( !e.isShiftDown() ) {
                float tz = 2f*e.getRotation()[1]; // vertical: wheel
                System.err.println("tz.4 "+tz);
                shape.translate(0f, 0f, tz);
            }
        }
    }
}