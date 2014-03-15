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
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.test.junit.graph.demos.ui.Label;
import com.jogamp.opengl.test.junit.graph.demos.ui.RIButton;
import com.jogamp.opengl.test.junit.graph.demos.ui.SceneUIController;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public class GPUUISceneGLListener0A implements GLEventListener {

    private boolean debug = false;
    private boolean trace = false;

    private final int renderModes;
    private final int[] sampleCount = new int[1];
    private final int[] texSize2 = new int[1];
    private final RenderState rs;
    private final SceneUIController sceneUIController;
    protected final float zNear = 0.1f, zFar = 7000f;
    /** Describing the bounding box in model-coordinates of the near-plane parallel at distance one. */
    protected final AABBox nearPlane1Box;

    private RegionRenderer renderer;

    int fontSet = FontFactory.UBUNTU;
    Font font;
    final float fontSizeFixed = 12f;
    final float fontSizeFPS = 10f;
    float dpiH = 96;

    private float xTran = 0f;
    private float yTran = 0;
    private float zTran = 0f;
    private float rotButtonsY = 0f;
    private float rotTextY = 0f;
    private final float zoomText = 1f;
    private int currentText = 0;

    private Label[] labels = null;
    private String[] strings = null;
    private RIButton[] buttons = null;
    private Label jogampLabel = null;
    private Label fpsLabel = null;
    private final int numSelectable = 6;

    private MultiTouchListener multiTouchListener = null;
    private boolean showFPS = true;
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
        this.sampleCount[0] = 4;
        this.texSize2[0] = 0;

        this.debug = debug;
        this.trace = trace;
        try {
            font = FontFactory.get(FontFactory.UBUNTU).getDefault();
        } catch (IOException ioe) {
            System.err.println("Catched: "+ioe.getMessage());
            ioe.printStackTrace();
        }
        sceneUIController = new SceneUIController();
        nearPlane1Box = new AABBox();
    }

    final float buttonXSize = 84f;
    final float buttonYSize = buttonXSize/2.5f;

    private void initButtons() {
        buttons = new RIButton[numSelectable];

        final float xstart = 0f;
        final float ystart = 0f;
        final float diff = 1.5f * buttonYSize;

        buttons[0] = new RIButton(SVertex.factory(), font, "Next Text", buttonXSize, buttonYSize){
            @Override
            public void onClick(MouseEvent e) {
                   currentText = (currentText+1)%3;
            }
        };
        buttons[0].translate(xstart,ystart);
        buttons[0].setLabelColor(1.0f, 1.0f, 1.0f);

        buttons[1] = new RIButton(SVertex.factory(), font, "Show FPS", buttonXSize, buttonYSize){
            @Override
            public void onClick(MouseEvent e) {
                final GLAnimatorControl a = cDrawable.getAnimator();
                if( null != a ) {
                    a.resetFPSCounter();
                }
                showFPS = !showFPS;
            }
        };
        buttons[1].translate(xstart,ystart - diff);
        buttons[1].setToggleable(true);
        buttons[1].setLabelColor(1.0f, 1.0f, 1.0f);

        buttons[2] = new RIButton(SVertex.factory(), font, "v-sync", buttonXSize, buttonYSize){
            @Override
            public void onClick(MouseEvent e) {
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
            }
        };
        buttons[2].translate(xstart,ystart-diff*2);
        buttons[2].setToggleable(true);
        buttons[2].setLabelColor(1.0f, 1.0f, 1.0f);

        buttons[3] = new RIButton(SVertex.factory(), font, "Tilt  +Y", buttonXSize, buttonYSize) {
            @Override
            public void onClick(MouseEvent e) {
                if( e.isShiftDown() ) {
                    rotButtonsY+=5;
                } else {
                    rotTextY+=5f;
                }
            }
        };
        buttons[3].translate(xstart,ystart-diff*3);
        buttons[3].setLabelColor(1.0f, 1.0f, 1.0f);

        buttons[4] = new RIButton(SVertex.factory(), font, "Tilt  -Y", buttonXSize, buttonYSize){
            @Override
            public void onClick(MouseEvent e) {
                if( e.isShiftDown() ) {
                    rotButtonsY-=5f;
                } else {
                    rotTextY-=5f;
                }
            }
        };
        buttons[4].translate(xstart,ystart-diff*4);
        buttons[4].setLabelColor(1.0f, 1.0f, 1.0f);

        buttons[5] = new RIButton(SVertex.factory(), font, "Quit", buttonXSize, buttonYSize){
            @Override
            public void onClick(MouseEvent e) {
                cDrawable.destroy();
            }
        };
        buttons[5].translate(xstart,ystart-diff*5);
        buttons[5].setColor(0.8f, 0.0f, 0.0f);
        buttons[5].setLabelColor(1.0f, 1.0f, 1.0f);

        buttons[5].setSelectedColor(0.8f, 0.8f, 0.8f);
        buttons[5].setLabelSelectedColor(0.8f, 0.0f, 0.0f);
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
        renderer.setColorStatic(gl, 0.0f, 0.0f, 0.0f);

        initTexts();
        initButtons();

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

    @Override
    public void display(GLAutoDrawable drawable) {
        // System.err.println("GPUUISceneGLListener0A: display");
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        final int width = drawable.getWidth();
        final int height = drawable.getHeight();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final float pixelSizeFixed = font.getPixelSize(fontSizeFixed, dpiH);

        float dx = width * 1f/6f;
        float dy = height - height/6f;

        renderer.resetModelview(null);
        // Keep Buttons static ..
        // sceneUIController.setTranslate(nearPlaneX0+xTran+(dx*nearPlaneSx), nearPlaneY0+yTran+((dy-buttonYSize)*nearPlaneSy), nearPlaneZ0+zTran);
        sceneUIController.setTranslate(nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+((dy-buttonYSize)*nearPlaneSy), nearPlaneZ0);
        sceneUIController.setScale(nearPlaneSx, nearPlaneSy, 1f);
        sceneUIController.setRotation(0, rotButtonsY, 0);
        sceneUIController.display(drawable);

        dx = width * 1f/3f;

        renderer.resetModelview(null);
        renderer.translate(null, nearPlaneX0+xTran+(dx*nearPlaneSx), nearPlaneY0+yTran+(dy*nearPlaneSy), nearPlaneZ0+zTran);
        renderer.rotate(null, rotTextY , 0, 1, 0);
        renderer.scale(null, nearPlaneSx*zoomText, nearPlaneSy*zoomText, 1f);
        renderer.updateMatrix(gl);
        renderer.setColorStatic(gl, 0.0f, 1.0f, 0.0f);
        jogampLabel.drawShape(gl, renderer, sampleCount, false);
        dy -= 3f * jogampLabel.getLineHeight();

        if(null == labels[currentText]) {
            labels[currentText] = new Label(SVertex.factory(), font, pixelSizeFixed, strings[currentText]);
            labels[currentText].setColor(0, 0, 0);
        }
        labels[currentText].validate(gl, renderer);

        renderer.resetModelview(null);
        renderer.translate(null, nearPlaneX0+xTran+(dx*nearPlaneSx), nearPlaneY0+yTran+(dy*nearPlaneSy), nearPlaneZ0+zTran);
        renderer.rotate(null, rotTextY, 0, 1, 0);
        renderer.scale(null, nearPlaneSx*zoomText, nearPlaneSy*zoomText, 1f);
        renderer.updateMatrix(gl);
        renderer.setColorStatic(gl, 0.0f, 0.0f, 0.0f);
        labels[currentText].drawShape(gl, renderer, sampleCount, false);

        if( showFPS ) {
            final float pixelSizeFPS = font.getPixelSize(fontSizeFPS, dpiH);
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
                    renderer.getRenderState().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED),
                    drawable.getChosenGLCapabilities().getAlphaBits());
            if(null != fpsLabel) {
                fpsLabel.clear(gl, renderer);
                fpsLabel.setText(text);
            } else {
                fpsLabel = new Label(renderer.getRenderState().getVertexFactory(), font, pixelSizeFPS, text);
            }
            renderer.resetModelview(null);
            renderer.translate(null, nearPlaneX0, nearPlaneY0+(nearPlaneSy * pixelSizeFPS / 2f), nearPlaneZ0);
            renderer.scale(null, nearPlaneSx, nearPlaneSy, 1f);
            renderer.updateMatrix(gl);
            fpsLabel.drawShape(gl, renderer, sampleCount, false);
        }
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
        System.err.println("GPUUISceneGLListener0A: reshape");
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        final PMVMatrix pmv = renderer.getMatrix();
        renderer.reshapePerspective(gl, 45.0f, width, height, zNear, zFar);
        renderer.resetModelview(null);
        renderer.updateMatrix(gl);
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
        sceneUIController.reshape(drawable, x, y, width, height);

        final float dist = 100f;
        nearPlaneX0 = nearPlane1Box.getMinX() * dist;
        nearPlaneY0 = nearPlane1Box.getMinY() * dist;
        nearPlaneZ0 = nearPlane1Box.getMinZ() * dist;
        final float xd = nearPlane1Box.getWidth() * dist;
        final float yd = nearPlane1Box.getHeight() * dist;
        nearPlaneSx = xd  / width;
        nearPlaneSy = yd / height;
        System.err.printf("Scale: [%f x %f] / [%d x %d] = [%f, %f]%n", xd, yd, width, height, nearPlaneSx, nearPlaneSy);
    }
    float nearPlaneX0, nearPlaneY0, nearPlaneZ0, nearPlaneSx, nearPlaneSy;

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

                zTran += 2 * Math.signum(dy);

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
                zTran += 2f*e.getRotation()[1]; // vertical: wheel
            }
        }
    }
}