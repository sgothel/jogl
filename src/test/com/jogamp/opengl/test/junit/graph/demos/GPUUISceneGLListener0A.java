package com.jogamp.opengl.test.junit.graph.demos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.test.junit.graph.demos.ui.CrossHair;
import com.jogamp.opengl.test.junit.graph.demos.ui.Label;
import com.jogamp.opengl.test.junit.graph.demos.ui.RIButton;
import com.jogamp.opengl.test.junit.graph.demos.ui.SceneUIController;
import com.jogamp.opengl.test.junit.graph.demos.ui.UIShape;
import com.jogamp.opengl.util.GLReadBufferUtil;
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

    final float relTop = 5f/6f;
    final float relRight = 2f/6f;
    final float relLeft = 1f/6f;

    final float buttonXSize = 84f;
    final float buttonYSize = buttonXSize/2.5f;
    final float fontSizePt = 10f;
    /** Proportional Window Height Font Size for Main Text, per-vertical-pixels [PVP] */
    final float fontSizeFixedPVP = 0.046f;
    /** Proportional Window Height Font Size for FPS Status Line, per-vertical-pixels [PVP] */
    final float fontSizeFpsPVP = 0.038f;
    float dpiH = 96;

    private int currentText = 0;

    private String actionText = null;
    private Label[] labels = null;
    private String[] strings = null;
    private final List<RIButton> buttons = new ArrayList<RIButton>();
    private Label truePtSizeLabel = null;
    private Label jogampLabel = null;
    private Label fpsLabel = null;
    private CrossHair crossHairCtr = null;

    private boolean ioAttached = false;
    private GLAutoDrawable cDrawable;

    private final GLReadBufferUtil screenshot;

    private final String jogamp = "JogAmp - Jogl Graph Module Demo";
    private final String truePtSize = fontSizePt+" pt font size label - true scale!";

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
        screenshot = new GLReadBufferUtil(false, false);
    }

    private void rotateButtons(float[] angdeg) {
        angdeg = VectorUtil.scaleVec3(angdeg, angdeg, FloatUtil.PI / 180.0f);
        for(int i=0; i<buttons.size(); i++) {
            buttons.get(i).getRotation().rotateByEuler( angdeg );
        }
    }
    private void translateButtons(float tx, float ty, float tz) {
        for(int i=0; i<buttons.size(); i++) {
            buttons.get(i).translate(tx, ty, tz);
        }
    }

    private void setButtonsSpacing(float dx, float dy) {
        for(int i=0; i<buttons.size(); i++) {
            final float sx = buttons.get(i).getSpacingX()+dx, sy = buttons.get(i).getSpacingY()+dy;
            System.err.println("Spacing: X "+sx+", Y "+sy);
            buttons.get(i).setSpacing(sx, sy);
        }
    }

    private void setButtonsCorner(float dc) {
        for(int i=0; i<buttons.size(); i++) {
            final float c = buttons.get(i).getCorner()+dc;
            System.err.println("Corner: "+c);
            buttons.get(i).setCorner(c);
        }
    }

    private void resetButtons() {
        for(int i=0; i<buttons.size(); i++) {
            buttons.get(i).getRotation().setIdentity();
            buttons.get(i).setCorner(RIButton.DEFAULT_CORNER);
            buttons.get(i).setSpacing(RIButton.DEFAULT_SPACING_X, RIButton.DEFAULT_SPACING_Y);
        }
    }

    private void initButtons(final GL gl, final RegionRenderer renderer) {
        final boolean pass2Mode = 0 != ( renderer.getRenderModes() & ( Region.VBAA_RENDERING_BIT | Region.MSAA_RENDERING_BIT ) ) ;
        buttons.clear();

        final float xstart = 0f;
        final float ystart = 0f;
        final float diffX = 1.2f * buttonXSize;
        final float diffY = 1.5f * buttonYSize;

        RIButton button = new RIButton(SVertex.factory(), font, "Next Text", buttonXSize, buttonYSize);
        button.translate(xstart,ystart-diffY*buttons.size(), 0f);
        button.setLabelColor(1.0f, 1.0f, 1.0f);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if( null != labels[currentText] ) {
                    labels[currentText].setEnabled(false);
                }
                currentText = (currentText+1)%labels.length;
                if( null != labels[currentText] ) {
                    labels[currentText].setEnabled(true);
                }
            } } );
        button.addMouseListener(dragZoomRotateListener);
        buttons.add(button);

        button = new RIButton(SVertex.factory(), font, "Show FPS", buttonXSize, buttonYSize);
        button.translate(xstart,ystart - diffY*buttons.size(), 0f);
        button.setToggleable(true);
        button.setPressed(fpsLabel.isEnabled());
        button.setLabelColor(1.0f, 1.0f, 1.0f);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final GLAnimatorControl a = cDrawable.getAnimator();
                if( null != a ) {
                    a.resetFPSCounter();
                }
                fpsLabel.setEnabled(!fpsLabel.isEnabled());
            } } );
        button.addMouseListener(dragZoomRotateListener);
        buttons.add(button);

        button = new RIButton(SVertex.factory(), font, "v-sync", buttonXSize, buttonYSize);
        button.translate(xstart,ystart - diffY*buttons.size(), 0f);
        button.setToggleable(true);
        button.setPressed(gl.getSwapInterval()>0);
        button.setLabelColor(1.0f, 1.0f, 1.0f);
        button.addMouseListener(new MouseAdapter() {
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
        button.addMouseListener(dragZoomRotateListener);
        buttons.add(button);

        button = new RIButton(SVertex.factory(), font, "< tilt >", buttonXSize, buttonYSize);
        button.translate(xstart,ystart - diffY*buttons.size(), 0f);
        button.setLabelColor(1.0f, 1.0f, 1.0f);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final Object attachment = e.getAttachment();
                if( attachment instanceof UIShape.EventDetails ) {
                    final UIShape.EventDetails shapeEvent = (UIShape.EventDetails)attachment;
                    if( shapeEvent.rotPosition[0] < shapeEvent.rotBounds.getCenter()[0] ) {
                        rotateButtons(new float[] { 0f, -5f, 0f}); // left-half pressed
                    } else {
                        rotateButtons(new float[] { 0f,  5f, 0f}); // right-half pressed
                    }
                }
            }
            @Override
            public void mouseWheelMoved(MouseEvent e) {
                rotateButtons(new float[] { 0f,  e.getRotation()[1], 0f});
            } } );
        buttons.add(button);

        if( pass2Mode ) { // second column to the left
            button = new RIButton(SVertex.factory(), font, "< samples >", buttonXSize, buttonYSize);
            button.translate(xstart,ystart - diffY*buttons.size(), 0f);
            button.setLabelColor(1.0f, 1.0f, 1.0f);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    final Object attachment = e.getAttachment();
                    if( attachment instanceof UIShape.EventDetails ) {
                        final UIShape.EventDetails shapeEvent = (UIShape.EventDetails)attachment;
                        int sampleCount = sceneUIController.getSampleCount();
                        if( shapeEvent.rotPosition[0] < shapeEvent.rotBounds.getCenter()[0] ) {
                            // left-half pressed
                            if( sampleCount > 0 ) {
                                sampleCount-=1;
                            }
                        } else {
                            // right-half pressed
                            if( sampleCount < 8 ) {
                                sampleCount+=1;
                            }
                        }
                        sceneUIController.setSampleCount(sampleCount);
                    }
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
        }

        button = new RIButton(SVertex.factory(), font, "Quit", buttonXSize, buttonYSize);
        button.translate(xstart,ystart - diffY*buttons.size(), 0f);
        button.setColor(0.8f, 0.0f, 0.0f);
        button.setLabelColor(1.0f, 1.0f, 1.0f);
        button.setSelectedColor(0.8f, 0.8f, 0.8f);
        button.setLabelSelectedColor(0.8f, 0.0f, 0.0f);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new Thread() {
                    public void run() {
                        if( null != cDrawable ) {
                            final GLAnimatorControl actrl = cDrawable.getAnimator();
                            if( null != actrl ) {
                                actrl.stop();
                            }
                            cDrawable.destroy();
                        }
                    } }.start();
            } } );
        button.addMouseListener(dragZoomRotateListener);
        buttons.add(button);

        // second column to the left
        {
            int j = 1; // column
            int k = 0; // row
            button = new RIButton(SVertex.factory(), font, "y flip", buttonXSize, buttonYSize);
            button.translate(xstart - diffX*j,ystart - diffY*k, 0f);
            button.setLabelColor(1.0f, 1.0f, 1.0f);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    rotateButtons(new float[] { 0f, 180f, 0f});
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);

            k++;
            button = new RIButton(SVertex.factory(), font, "x flip", buttonXSize, buttonYSize);
            button.translate(xstart - diffX*j,ystart - diffY*k, 0f);
            button.setLabelColor(1.0f, 1.0f, 1.0f);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    rotateButtons(new float[] { 180f, 0f, 0f});
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;

            button = new RIButton(SVertex.factory(), font, "+", buttonXSize, buttonYSize);
            button.translate(xstart - diffX*j,ystart - diffY*k, 0f);
            button.setLabelColor(1.0f, 1.0f, 1.0f);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    final Object attachment = e.getAttachment();
                    if( attachment instanceof UIShape.EventDetails ) {
                        final UIShape.EventDetails shapeEvent = (UIShape.EventDetails)attachment;
                        // rel position to center
                        final float dx = shapeEvent.rotPosition[0] - shapeEvent.rotBounds.getCenter()[0] ;
                        final float dy = shapeEvent.rotPosition[1] - shapeEvent.rotBounds.getCenter()[1] ;
                        // per-cent position to center (remove dependency on dimension)
                        final float awdx = Math.abs(dx)/shapeEvent.rotBounds.getWidth();
                        final float awdy = Math.abs(dy)/shapeEvent.rotBounds.getHeight();
                        float tx = 0, ty = 0;
                        if ( awdx > awdy  ) {
                            tx = dx < 0 ? -5 : 5;
                        } else {
                            ty = dy < 0 ? -5 : 5;
                        }
                        translateButtons(tx, ty, 0f);
                    }
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;

            button = new RIButton(SVertex.factory(), font, "< space >", buttonXSize, buttonYSize);
            button.translate(xstart - diffX*j,ystart - diffY*k, 0f);
            button.setLabelColor(1.0f, 1.0f, 1.0f);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    final Object attachment = e.getAttachment();
                    if( attachment instanceof UIShape.EventDetails ) {
                        final UIShape.EventDetails shapeEvent = (UIShape.EventDetails)attachment;
                        final float dx, dy;
                        if( shapeEvent.rotPosition[0] < shapeEvent.rotBounds.getCenter()[0] ) {
                            dx=-0.01f; dy=-0.005f;
                        } else {
                            dx=0.01f; dy=0.005f;
                        }
                        setButtonsSpacing(dx, dy);
                    }
                }
                @Override
                public void mouseWheelMoved(MouseEvent e) {
                    setButtonsSpacing(e.getRotation()[0]/100f, e.getRotation()[1]/200f);
                } } );
            buttons.add(button);
            k++;

            button = new RIButton(SVertex.factory(), font, "< corner >", buttonXSize, buttonYSize);
            button.translate(xstart - diffX*j,ystart - diffY*k, 0f);
            button.setLabelColor(1.0f, 1.0f, 1.0f);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    final Object attachment = e.getAttachment();
                    if( attachment instanceof UIShape.EventDetails ) {
                        final UIShape.EventDetails shapeEvent = (UIShape.EventDetails)attachment;
                        final float dc;
                        if( shapeEvent.rotPosition[0] < shapeEvent.rotBounds.getCenter()[0] ) {
                            dc=-0.1f;
                        } else {
                            dc=0.1f;
                        }
                        setButtonsCorner(dc);
                    }

                }
                @Override
                public void mouseWheelMoved(MouseEvent e) {
                    setButtonsCorner(e.getRotation()[1]/20f);
                } } );
            buttons.add(button);
            k++;

            button = new RIButton(SVertex.factory(), font, "reset", buttonXSize, buttonYSize);
            button.translate(xstart - diffX*j,ystart - diffY*k, 0f);
            button.setLabelColor(1.0f, 1.0f, 1.0f);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    resetButtons();
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;

            button = new RIButton(SVertex.factory(), font, "screenshot", buttonXSize, buttonYSize);
            button.translate(xstart - diffX*j,ystart - diffY*k, 0f);
            button.setLabelColor(1.0f, 1.0f, 1.0f);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    cDrawable.invoke(false, new GLRunnable() {
                        @Override
                        public boolean run(GLAutoDrawable drawable) {
                            printScreen(drawable.getGL());
                            return true;
                        }
                    });
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;
        }

    }

    private void initTexts() {
        strings = new String[4];
        int i = 0;

        strings[i++] =
                     "- Mouse Scroll Over Object\n"+
                     "   - General\n"+
                     "     - Z Translation\n"+
                     "     - Ctrl: Y-Rotation (Shift: X-Rotation)\n"+
                     "   - Tilt, Space and Corner\n"+
                     "     - Their respective action via wheel (shift = other value)\n"+
                     "\n"+
                     "- Mouse Drag On Object\n"+
                     "   - Click on Object and drag mouse\n"+
                     "   - Notice current postion in status line at bottom\n"+
                     "\n"+
                     "- Tilt Button Rotate Whole Button Group\n";

        strings[i++] = "abcdefghijklmn\nopqrstuvwxyz\n"+
                       "ABCDEFGHIJKL\n"+
                       "MNOPQRSTUVWXYZ\n"+
                       "0123456789.:,;(*!?/\\\")$%^&-+@~#<>{}[]";

        strings[i++] = "The quick brown fox\njumps over the lazy\ndog";

        strings[i++] =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec \n"+
            "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel\n"+
            "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices\n"+
            "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia\n"+
            "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat\n"+
            "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum\n"+
            "in lorem. Maecenas in ipsum ac justo scelerisque sollicitudin.\n";

        labels = new Label[i];
    }


    final boolean enableOthers = true;

    @Override
    public void init(GLAutoDrawable drawable) {
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            final float[] pixelsPerMM = new float[2];
            final MonitorDevice mm = ((Window)upObj).getMainMonitor();
            mm.getPixelsPerMM(pixelsPerMM);
            dpiH = pixelsPerMM[1]*25.4f;
            System.err.println("Monitor detected: "+mm);
            System.err.println("Using monitor's DPI of "+(pixelsPerMM[0]*25.4f)+" x "+dpiH+" -> "+dpiH);
        } else {
            System.err.println("Using default DPI of "+dpiH);
        }
        if(drawable instanceof GLWindow) {
            System.err.println("GPUUISceneGLListener0A: init (1)");
            final GLWindow glw = (GLWindow) drawable;
            attachInputListenerTo(glw);
        } else {
            System.err.println("GPUUISceneGLListener0A: init (0)");
        }
        System.err.println("Chosen: "+drawable.getChosenGLCapabilities());
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
        // renderer = RegionRenderer.create(rs, renderModes, null, null);

        gl.setSwapInterval(1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        gl.glEnable(GL2ES2.GL_BLEND);

        renderer.init(gl);
        renderer.setAlpha(gl, 1.0f);

        initTexts();

        sceneUIController.setRenderer(renderer);

        final float pixelSizeFixed = fontSizeFixedPVP * drawable.getHeight();
        jogampLabel = new Label(SVertex.factory(), font, pixelSizeFixed, jogamp);
        jogampLabel.addMouseListener(dragZoomRotateListener);
        sceneUIController.addShape(jogampLabel);
        jogampLabel.setEnabled(enableOthers);

        final float pixelSize10Pt = font.getPixelSize(fontSizePt, dpiH);
        System.err.println("10Pt PixelSize: Display "+dpiH+" dpi, fontSize "+fontSizePt+" ppi -> "+pixelSize10Pt+" pixel-size");
        truePtSizeLabel = new Label(SVertex.factory(), font, pixelSize10Pt, truePtSize);
        sceneUIController.addShape(truePtSizeLabel);
        truePtSizeLabel.setEnabled(enableOthers);
        truePtSizeLabel.translate(0, - 1.5f * jogampLabel.getLineHeight(), 0f);
        truePtSizeLabel.setColor(0.1f, 0.1f, 0.1f);

        /**
         *
         * [Label] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 18.814816
         * [FPS] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 15.679012
         */
        final float pixelSizeFPS = fontSizeFpsPVP * drawable.getHeight();
        fpsLabel = new Label(renderer.getRenderState().getVertexFactory(), font, pixelSizeFPS, "Nothing there yet");
        fpsLabel.addMouseListener(dragZoomRotateListener);
        sceneUIController.addShape(fpsLabel);
        fpsLabel.setEnabled(enableOthers);
        fpsLabel.setColor(0.3f, 0.3f, 0.3f);

        crossHairCtr = new CrossHair(renderer.getRenderState().getVertexFactory(), 100f, 100f, 2f);
        crossHairCtr.addMouseListener(dragZoomRotateListener);
        sceneUIController.addShape(crossHairCtr);
        crossHairCtr.setEnabled(true);
        crossHairCtr.translate(0f, 0f, -1f);

        initButtons(gl, renderer);
        for(int i=0; i<buttons.size(); i++) {
            sceneUIController.addShape(buttons.get(i));
        }

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
        screenshot.dispose(gl);
    }

    private int shotCount = 0;

    public void printScreen(final GL gl)  {
        final String modeS = Region.getRenderModeString(renderer.getRenderModes());
        final String filename = String.format("GraphUIDemo-shot%03d-%03dx%03d-S_%s_%02d.png",
                shotCount++, renderer.getWidth(), renderer.getHeight(),
                modeS, sceneUIController.getSampleCount());
        gl.glFinish(); // just make sure rendering finished ..
        if(screenshot.readPixels(gl, false)) {
            screenshot.write(new File(filename));
            System.err.println("Wrote: "+filename);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        // System.err.println("GPUUISceneGLListener0A: display");
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if(null == labels[currentText]) {
            final float pixelSizeFixed = fontSizeFixedPVP * drawable.getHeight();
            final float dyTop = drawable.getHeight() * relTop;
            final float dxRight = drawable.getWidth() * relRight;
            labels[currentText] = new Label(SVertex.factory(), font, pixelSizeFixed, strings[currentText]);
            labels[currentText].setColor(0.1f, 0.1f, 0.1f);
            labels[currentText].setEnabled(enableOthers);
            labels[currentText].translate(dxRight,
                                          dyTop - 1.5f * jogampLabel.getLineHeight()
                                                - 1.5f * truePtSizeLabel.getLineHeight(), 0f);
            labels[currentText].addMouseListener(dragZoomRotateListener);
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
            final String text;
            if( null == actionText ) {
                text = String.format("%03.1f/%03.1f fps, v-sync %d, fontSize %.1f, %s-samples %d, td %4.1f, blend %b, alpha-bits %d, msaa-bits %d",
                    lfps, tfps, gl.getSwapInterval(), fontSizeFixedPVP, modeS, sceneUIController.getSampleCount(), td,
                    renderer.getRenderState().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED),
                    drawable.getChosenGLCapabilities().getAlphaBits(),
                    drawable.getChosenGLCapabilities().getNumSamples());
            } else {
                text = String.format("%03.1f/%03.1f fps, v-sync %d, fontSize %.1f, %s",
                    lfps, tfps, gl.getSwapInterval(), fontSizeFixedPVP, actionText);
            }
            fpsLabel.setText(text);
        }
        sceneUIController.display(drawable);
    }


    float lastWidth = 0f, lastHeight = 0f;

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.err.println("GPUUISceneGLListener0A: reshape");

        //
        // Layout all shapes: Relational move regarding window coordinates
        //
        final float dw = width - lastWidth;
        final float dh = height - lastHeight;

        final float dz = 0f;
        final float dyTop = dh * relTop;
        final float dxRight = dw * relRight;
        final float dxLeft = dw * relLeft;

        for(int i=0; i<buttons.size(); i++) {
            buttons.get(i).translate(dxLeft, dyTop, dz);
        }
        jogampLabel.translate(dxRight, dyTop, dz);
        truePtSizeLabel.translate(dxRight, dyTop, dz);
        fpsLabel.translate(0f, 0f, 0f);
        if( null != labels[currentText] ) {
            labels[currentText].translate(dxRight, dyTop, 0f);
        }
        crossHairCtr.translate(dw/2f, dh/2f, 0f);

        sceneUIController.reshape(drawable, x, y, width, height);

        lastWidth = width;
        lastHeight = height;
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

    /**
     * We can share this instance w/ all UI elements,
     * since only mouse action / gesture is complete for a single one (press, drag, released and click).
     */
    private final MouseAdapter dragZoomRotateListener = new MouseAdapter() {
        int dragLastX=-1, dragLastY=-1;
        boolean dragFirst = false;

        @Override
        public void mousePressed(MouseEvent e) {
            dragFirst = true;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragFirst = false;
            actionText = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            final Object attachment = e.getAttachment();
            if( attachment instanceof UIShape.EventDetails ) {
                final UIShape.EventDetails shapeEvent = (UIShape.EventDetails)attachment;
                if(e.getPointerCount()==2) {
                    // 2 pointers zoom ..
                    if(dragFirst) {
                        dragLastX = Math.abs(e.getY(0)-e.getY(1));
                        dragFirst=false;
                        return;
                    }
                    int nv = Math.abs(e.getY(0)-e.getY(1));
                    int dy = nv - dragLastX;
                    dragLastX = nv;

                    shapeEvent.shape.translate(0f, 0f, 2 * Math.signum(dy));
                } else {
                    // 1 pointer drag
                    if(dragFirst) {
                        dragLastX = e.getX();
                        dragLastY = e.getY();
                        dragFirst=false;
                        return;
                    }
                    final int nx = e.getX();
                    final int ny = e.getY();
                    shapeEvent.shape.translate(nx-dragLastX, -(ny-dragLastY), 0f);
                    dragLastX = nx;
                    dragLastY = ny;
                    float[] tx = shapeEvent.shape.getTranslate();
                    actionText = String.format("Pos %6.2f / %6.2f / %6.2f", tx[0], tx[1], tx[2]);
                }
            }
        }

        @Override
        public void mouseWheelMoved(MouseEvent e) {
            final Object attachment = e.getAttachment();
            if( attachment instanceof UIShape.EventDetails ) {
                final UIShape.EventDetails shapeEvent = (UIShape.EventDetails)attachment;
                if( 0 == ( ~InputEvent.BUTTONALL_MASK & e.getModifiers() ) ) {
                    float tz = 2f*e.getRotation()[1]; // vertical: wheel
                    System.err.println("tz.4 "+tz);
                    shapeEvent.shape.translate(0f, 0f, tz);
                } else if( e.isControlDown() ) {
                    shapeEvent.shape.getRotation().rotateByEuler( VectorUtil.scaleVec3(e.getRotation(), e.getRotation(), FloatUtil.PI / 180.0f) );
                }
            }
       } };
}