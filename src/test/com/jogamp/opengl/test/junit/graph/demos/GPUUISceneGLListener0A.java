package com.jogamp.opengl.test.junit.graph.demos;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLRunnable;

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.PinchToZoomGesture;
import com.jogamp.newt.event.GestureHandler.GestureEvent;
import com.jogamp.newt.event.MouseEvent.PointerClass;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.test.junit.graph.demos.ui.CrossHair;
import com.jogamp.opengl.test.junit.graph.demos.ui.GLEventListenerButton;
import com.jogamp.opengl.test.junit.graph.demos.ui.Label;
import com.jogamp.opengl.test.junit.graph.demos.ui.LabelButton;
import com.jogamp.opengl.test.junit.graph.demos.ui.MediaPlayerButton;
import com.jogamp.opengl.test.junit.graph.demos.ui.RoundButton;
import com.jogamp.opengl.test.junit.graph.demos.ui.SceneUIController;
import com.jogamp.opengl.test.junit.graph.demos.ui.ImageSeqButton;
import com.jogamp.opengl.test.junit.graph.demos.ui.UIShape;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.texture.ImageSequence;
import com.jogamp.opengl.util.texture.TextureIO;

public class GPUUISceneGLListener0A implements GLEventListener {

    private boolean debug = false;
    private boolean trace = false;

    private final float noAADPIThreshold;
    private final RenderState rs;
    private final SceneUIController sceneUIController;

    /** -1 == AUTO, TBD @ init(..) */
    private int renderModes;
    private RegionRenderer renderer;

    private final int fontSet = FontFactory.UBUNTU;
    private Font font;

    private final float sceneDist = 3000f;
    private final float zNear = 0.1f, zFar = 7000f;

    private final float relTop = 5f/6f;
    private final float relMiddle = 2f/6f;
    private final float relLeft = 1f/6f;

    /** Proportional Button Size to Window Height, per-vertical-pixels [PVP] */
    private final float buttonYSizePVP = 0.084f;
    private final float buttonXSizePVP = 0.105f;
    private final float fontSizePt = 10f;
    /** Proportional Font Size to Window Height  for Main Text, per-vertical-pixels [PVP] */
    private final float fontSizeFixedPVP = 0.046f;
    /** Proportional Font Size to Window Height for FPS Status Line, per-vertical-pixels [PVP] */
    private final float fontSizeFpsPVP = 0.038f;
    private float dpiH = 96;

    /**
     * Default DPI threshold value to disable {@link Region#VBAA_RENDERING_BIT VBAA}: {@value} dpi
     * @see #GPUUISceneGLListener0A(float)
     * @see #GPUUISceneGLListener0A(float, boolean, boolean)
     */
    public static final float DefaultNoAADPIThreshold = 200f;

    private int currentText = 0;

    private String actionText = null;
    private Label[] labels = null;
    private String[] strings = null;
    private final List<RoundButton> buttons = new ArrayList<RoundButton>();
    private int buttonsLeftCount = 0;
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

    /**
     * @param noAADPIThreshold see {@link #DefaultNoAADPIThreshold}
     */
    public GPUUISceneGLListener0A(final float noAADPIThreshold) {
        this(noAADPIThreshold, false, false);
    }

    /**
     * @param renderModes
     */
    public GPUUISceneGLListener0A(final int renderModes) {
      this(renderModes, false, false);
    }

    /**
     * @param renderModes
     * @param debug
     * @param trace
     */
    public GPUUISceneGLListener0A(final int renderModes, final boolean debug, final boolean trace) {
        this(0f, renderModes, debug, trace);
    }

    /**
     * @param noAADPIThreshold see {@link #DefaultNoAADPIThreshold}
     * @param debug
     * @param trace
     */
    public GPUUISceneGLListener0A(final float noAADPIThreshold, final boolean debug, final boolean trace) {
        this(noAADPIThreshold, 0, debug, trace);
    }

    private GPUUISceneGLListener0A(final float noAADPIThreshold, final int renderModes, final boolean debug, final boolean trace) {
        this.noAADPIThreshold = noAADPIThreshold;
        this.rs = RenderState.createRenderState(SVertex.factory());
        this.debug = debug;
        this.trace = trace;

        this.renderModes = renderModes;

        try {
            font = FontFactory.get(FontFactory.UBUNTU).getDefault();
        } catch (final IOException ioe) {
            throw new RuntimeException(ioe);
        }
        sceneUIController = new SceneUIController(sceneDist, zNear, zFar);
        screenshot = new GLReadBufferUtil(false, false);
    }

    private void rotateButtons(float[] angdeg) {
        angdeg = VectorUtil.scaleVec3(angdeg, angdeg, FloatUtil.PI / 180.0f);
        for(int i=0; i<buttons.size(); i++) {
            buttons.get(i).getRotation().rotateByEuler( angdeg );
        }
    }
    private void translateButtons(final float tx, final float ty, final float tz) {
        for(int i=0; i<buttons.size(); i++) {
            buttons.get(i).translate(tx, ty, tz);
        }
    }

    private void setButtonsSpacing(final float dx, final float dy) {
        for(int i=0; i<buttons.size(); i++) {
            final RoundButton b = buttons.get(i);
            if( b instanceof LabelButton ) {
                final LabelButton lb = (LabelButton) b;
                final float sx = lb.getSpacingX()+dx, sy = lb.getSpacingY()+dy;
                System.err.println("Spacing: X "+sx+", Y "+sy);
                lb.setSpacing(sx, sy);
            }
        }
    }

    private void setButtonsCorner(final float dc) {
        for(int i=0; i<buttons.size(); i++) {
            final float c = buttons.get(i).getCorner()+dc;
            System.err.println("Corner: "+c);
            buttons.get(i).setCorner(c);
        }
    }

    private void resetButtons() {
        for(int i=0; i<buttons.size(); i++) {
            final RoundButton b = buttons.get(i);
            b.getRotation().setIdentity();
            b.setCorner(RoundButton.DEFAULT_CORNER);
            if( b instanceof LabelButton ) {
                ((LabelButton)b).setSpacing(LabelButton.DEFAULT_SPACING_X, LabelButton.DEFAULT_SPACING_Y);
            }
        }
    }

    private void initButtons(final GL2ES2 gl, final int width, final int height, final RegionRenderer renderer) {
        final boolean pass2Mode = Region.isTwoPass( renderModes ) ;
        buttons.clear();

        final float buttonXSize = buttonXSizePVP * width;
        // final float buttonYSize = buttonYSizePVP * height;
        final float buttonYSize = buttonXSize / 2.5f;
        System.err.println("Button Size: "+buttonXSizePVP+" x "+buttonYSizePVP+" * "+width+" x "+height+" -> "+buttonXSize+" x "+buttonYSize);
        final float xStartLeft = 0f; // aligned to left edge w/ space via reshape
        final float yStartTop = 0f;  // aligned to top edge w/ space via reshape
        final float diffX = 1.2f * buttonXSize;
        final float diffY = 1.5f * buttonYSize;

        LabelButton button = new LabelButton(SVertex.factory(), renderModes, font, "Next Text", buttonXSize, buttonYSize);
        button.translate(xStartLeft,yStartTop-diffY*buttons.size(), 0f);
        button.addMouseListener(new UIShape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
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

        button = new LabelButton(SVertex.factory(), renderModes, font, "Show FPS", buttonXSize, buttonYSize);
        button.setName(100); // FIXME: DEBUG tag
        button.translate(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
        button.setToggleable(true);
        button.setToggle(fpsLabel.isEnabled());
        button.addMouseListener(new UIShape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final GLAnimatorControl a = cDrawable.getAnimator();
                if( null != a ) {
                    a.resetFPSCounter();
                }
                fpsLabel.setEnabled(!fpsLabel.isEnabled());
            } } );
        button.addMouseListener(dragZoomRotateListener);
        buttons.add(button);

        button = new LabelButton(SVertex.factory(), renderModes, font, "v-sync", buttonXSize, buttonYSize);
        button.translate(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
        button.setToggleable(true);
        button.setToggle(gl.getSwapInterval()>0);
        button.addMouseListener(new UIShape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                cDrawable.invoke(false, new GLRunnable() {
                    @Override
                    public boolean run(final GLAutoDrawable drawable) {
                        final GL gl = drawable.getGL();
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

        button = new LabelButton(SVertex.factory(), renderModes, font, "< tilt >", buttonXSize, buttonYSize);
        button.translate(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
        button.addMouseListener(new UIShape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final Object attachment = e.getAttachment();
                if( attachment instanceof UIShape.PointerEventInfo ) {
                    final UIShape.PointerEventInfo shapeEvent = (UIShape.PointerEventInfo)attachment;
                    if( shapeEvent.objPos[0] < shapeEvent.shape.getBounds().getCenter()[0] ) {
                        rotateButtons(new float[] { 0f, -5f, 0f}); // left-half pressed
                    } else {
                        rotateButtons(new float[] { 0f,  5f, 0f}); // right-half pressed
                    }
                }
            }
            @Override
            public void mouseWheelMoved(final MouseEvent e) {
                rotateButtons(new float[] { 0f,  e.getRotation()[1], 0f});
            } } );
        buttons.add(button);

        if( pass2Mode ) { // second column to the left
            button = new LabelButton(SVertex.factory(), renderModes, font, "< samples >", buttonXSize, buttonYSize);
            button.translate(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
            button.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Object attachment = e.getAttachment();
                    if( attachment instanceof UIShape.PointerEventInfo ) {
                        final UIShape.PointerEventInfo shapeEvent = (UIShape.PointerEventInfo)attachment;
                        int sampleCount = sceneUIController.getSampleCount();
                        if( shapeEvent.objPos[0] < shapeEvent.shape.getBounds().getCenter()[0] ) {
                            // left-half pressed
                            sampleCount--;
                        } else {
                            // right-half pressed
                            sampleCount++;
                        }
                        sampleCount = sceneUIController.setSampleCount(sampleCount); // validated / clipped
                    }
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);

            button = new LabelButton(SVertex.factory(), renderModes, font, "< quality >", buttonXSize, buttonYSize);
            button.translate(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
            button.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Object attachment = e.getAttachment();
                    if( attachment instanceof UIShape.PointerEventInfo ) {
                        final UIShape.PointerEventInfo shapeEvent = (UIShape.PointerEventInfo)attachment;
                        int quality = shapeEvent.shape.getQuality();

                        if( shapeEvent.objPos[0] < shapeEvent.shape.getBounds().getCenter()[0] ) {
                            // left-half pressed
                            if( quality > 0 ) {
                                quality--;
                            }
                        } else {
                            // right-half pressed
                            if( quality < Region.MAX_QUALITY ) {
                                quality++;
                            }
                        }
                        sceneUIController.setAllShapesQuality(quality);
                    }
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
        }

        button = new LabelButton(SVertex.factory(), renderModes, font, "Quit", buttonXSize, buttonYSize);
        button.translate(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
        button.setColor(0.7f, 0.0f, 0.0f, 1.0f);
        button.setLabelColor(1.2f, 1.2f, 1.2f);
        button.setPressedColorMod(1.1f, 0.0f, 0.0f, 1.0f);
        button.addMouseListener(new UIShape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                new InterruptSource.Thread() {
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
            final int j = 1; // column
            int k = 0; // row
            button = new LabelButton(SVertex.factory(), renderModes, font, "y flip", buttonXSize, buttonYSize);
            button.translate(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    rotateButtons(new float[] { 0f, 180f, 0f});
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);

            k++;
            button = new LabelButton(SVertex.factory(), renderModes, font, "x flip", buttonXSize, buttonYSize);
            button.translate(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    rotateButtons(new float[] { 180f, 0f, 0f});
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;

            button = new LabelButton(SVertex.factory(), renderModes, font, "+", buttonXSize, buttonYSize);
            button.translate(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Object attachment = e.getAttachment();
                    if( attachment instanceof UIShape.PointerEventInfo ) {
                        final UIShape.PointerEventInfo shapeEvent = (UIShape.PointerEventInfo)attachment;
                        // rel position to center
                        final float dx = shapeEvent.objPos[0] - shapeEvent.shape.getBounds().getCenter()[0] ;
                        final float dy = shapeEvent.objPos[1] - shapeEvent.shape.getBounds().getCenter()[1] ;
                        // per-cent position to center (remove dependency on dimension)
                        final float awdx = Math.abs(dx)/shapeEvent.shape.getBounds().getWidth();
                        final float awdy = Math.abs(dy)/shapeEvent.shape.getBounds().getHeight();
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

            button = new LabelButton(SVertex.factory(), renderModes, font, "< space >", buttonXSize, buttonYSize);
            button.translate(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Object attachment = e.getAttachment();
                    if( attachment instanceof UIShape.PointerEventInfo ) {
                        final UIShape.PointerEventInfo shapeEvent = (UIShape.PointerEventInfo)attachment;
                        final float dx, dy;
                        if( shapeEvent.objPos[0] < shapeEvent.shape.getBounds().getCenter()[0] ) {
                            dx=-0.01f; dy=-0.005f;
                        } else {
                            dx=0.01f; dy=0.005f;
                        }
                        setButtonsSpacing(dx, dy);
                    }
                }
                @Override
                public void mouseWheelMoved(final MouseEvent e) {
                    setButtonsSpacing(e.getRotation()[0]/100f, e.getRotation()[1]/200f);
                } } );
            buttons.add(button);
            k++;

            button = new LabelButton(SVertex.factory(), renderModes, font, "< corner >", buttonXSize, buttonYSize);
            button.translate(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Object attachment = e.getAttachment();
                    if( attachment instanceof UIShape.PointerEventInfo ) {
                        final UIShape.PointerEventInfo shapeEvent = (UIShape.PointerEventInfo)attachment;
                        final float dc;
                        if( shapeEvent.objPos[0] < shapeEvent.shape.getBounds().getCenter()[0] ) {
                            dc=-0.1f;
                        } else {
                            dc=0.1f;
                        }
                        setButtonsCorner(dc);
                    }

                }
                @Override
                public void mouseWheelMoved(final MouseEvent e) {
                    setButtonsCorner(e.getRotation()[1]/20f);
                } } );
            buttons.add(button);
            k++;

            button = new LabelButton(SVertex.factory(), renderModes, font, "reset", buttonXSize, buttonYSize);
            button.translate(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    resetButtons();
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;

            button = new LabelButton(SVertex.factory(), renderModes, font, "screenshot", buttonXSize, buttonYSize);
            button.translate(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    cDrawable.invoke(false, new GLRunnable() {
                        @Override
                        public boolean run(final GLAutoDrawable drawable) {
                            printScreen(drawable.getGL());
                            return true;
                        }
                    });
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;
        }

        buttonsLeftCount = buttons.size();

        final float button2XSize = 2f*buttonXSize;
        final float button2YSize = 2f*buttonYSize;
        final float xStartRight = -button2XSize - 8f; // aligned to right edge via reshape
        final int texUnitMediaPlayer, texUnitImageButton, texUnitGLELButton;
        if( false ) {
            texUnitMediaPlayer=0;
            texUnitImageButton=0;
            texUnitGLELButton=0;
        } else {
            // works - but not required ..
            texUnitMediaPlayer=1;
            texUnitImageButton=2;
            texUnitGLELButton=3;
        }

        if( true ) {
            final GLMediaPlayer mPlayer = GLMediaPlayerFactory.createDefault();
            mPlayer.setTextureUnit(texUnitMediaPlayer);
            final MediaPlayerButton mPlayerButton = new MediaPlayerButton(renderer.getRenderState().getVertexFactory(), renderModes,
                                                                          button2XSize, button2YSize, mPlayer);
            mPlayerButton.setVerbose(true);
            mPlayerButton.addDefaultEventListener();
            mPlayerButton.translate(xStartRight, yStartTop - diffY*1, 0f);
            mPlayerButton.setToggleable(true);
            mPlayerButton.setToggle(false); // toggle == false -> mute audio
            mPlayerButton.setToggleOffColorMod(0f, 1f, 0f, 1.0f);
            mPlayerButton.addMouseListener(dragZoomRotateListener);
            mPlayerButton.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    mPlayer.setAudioVolume( mPlayerButton.isToggleOn() ? 1f : 0f );
                } } );
            buttons.add(mPlayerButton);
            try {
                final Uri streamLoc = Uri.cast("http://archive.org/download/BigBuckBunny_328/BigBuckBunny_512kb.mp4");
                mPlayer.initStream(streamLoc, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.TEXTURE_COUNT_DEFAULT);
            } catch (final URISyntaxException e1) {
                e1.printStackTrace();
            }
        }
        if( true ) {
            final ImageSequence imgSeq = new ImageSequence(texUnitImageButton, true);
            final ImageSeqButton imgButton = new ImageSeqButton(renderer.getRenderState().getVertexFactory(), renderModes,
                                                                button2XSize, button2YSize, imgSeq);
            try {
                imgSeq.addFrame(gl, GPUUISceneGLListener0A.class, "button-released-145x53.png", TextureIO.PNG);
                imgSeq.addFrame(gl, GPUUISceneGLListener0A.class, "button-pressed-145x53.png", TextureIO.PNG);
            } catch (final IOException e2) {
                e2.printStackTrace();
            }
            imgSeq.setManualStepping(true);
            imgButton.translate(xStartRight, yStartTop - diffY*2.5f, 0f);
            imgButton.addMouseListener(dragZoomRotateListener);
            imgButton.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    imgButton.setCurrentIdx(1);
                    System.err.println("XXX: "+imgButton);
                }
                @Override
                public void mouseReleased(final MouseEvent e) {
                    imgButton.setCurrentIdx(0);
                } } );
            buttons.add(imgButton);
        }
        if( true ) {
            // Issues w/ OSX and NewtCanvasAWT when rendering / animating
            // Probably related to CALayer - FBO - FBO* (of this button)
            final GLEventListener glel;
            if( true ) {
                final GearsES2 gears = new GearsES2(0);
                gears.setVerbose(false);
                gears.setClearColor(new float[] { 0.9f, 0.9f, 0.9f, 1f } );
                glel = gears;
            } else if( false ) {
                glel = new RedSquareES2(0);
            } else {
                glel = new GLEventListener() {
                    @Override
                    public void init(final GLAutoDrawable drawable) {
                    }

                    @Override
                    public void dispose(final GLAutoDrawable drawable) {
                    }

                    @Override
                    public void display(final GLAutoDrawable drawable) {
                        final GL2ES2 gl = drawable.getGL().getGL2ES2();
                        gl.glClearColor(0.0f, 0.0f, 1.0f, 0.0f);
                        // gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
                        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                    }

                    @Override
                    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                    }

                };
            }
            final GLEventListenerButton glelButton = new GLEventListenerButton(renderer.getRenderState().getVertexFactory(), renderModes,
                                                                       button2XSize, button2YSize,
                                                                       texUnitGLELButton, glel, false /* useAlpha */,
                                                                       (int)(button2XSize), (int)(button2YSize));
            glelButton.setToggleable(true);
            glelButton.setToggle(false); // toggle == true -> animation
            glelButton.setAnimate(false);
            glelButton.translate(xStartRight, yStartTop - diffY*4f, 0f);
            glelButton.addMouseListener(dragZoomRotateListener);
            glelButton.addMouseListener(new UIShape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    glelButton.setAnimate( glelButton.isToggleOn() );
                } } );
            buttons.add(glelButton);
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
                     "     - Their respective action via wheel\n"+
                     "       (shift = other value)\n"+
                     "\n"+
                     "- Mouse Drag On Object\n"+
                     "   - Click on Object and drag mouse\n"+
                     "   - Current postion in status line at bottom\n"+
                     "\n"+
                     "- Tilt Button Rotate Whole Button Group";

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
    public void init(final GLAutoDrawable drawable) {
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            final Window upWin = (Window)upObj;
            final MonitorDevice mm = upWin.getMainMonitor();
            final float[] monitorDPI = mm.getPixelsPerMM(new float[2]);
            monitorDPI[0] *= 25.4f;
            monitorDPI[1] *= 25.4f;
            final float[] sDPI = upWin.getPixelsPerMM(new float[2]);
            sDPI[0] *= 25.4f;
            sDPI[1] *= 25.4f;
            dpiH = sDPI[1];
            System.err.println("Monitor detected: "+mm);
            System.err.println("Monitor dpi: "+monitorDPI[0]+" x "+monitorDPI[1]);
            System.err.println("Surface scale: native "+Arrays.toString(upWin.getMaximumSurfaceScale(new float[2]))+", current "+Arrays.toString(upWin.getCurrentSurfaceScale(new float[2])));
            System.err.println("Surface dpi "+sDPI[0]+" x "+sDPI[1]);
        } else {
            System.err.println("Using default DPI of "+dpiH);
        }
        if( 0 == renderModes && !FloatUtil.isZero(noAADPIThreshold, FloatUtil.EPSILON)) {
            final boolean noAA = dpiH >= noAADPIThreshold;
            final String noAAs = noAA ? " >= " : " < ";
            System.err.println("AUTO RenderMode: dpi "+dpiH+noAAs+noAADPIThreshold+" -> noAA "+noAA);
            renderModes = noAA ? 0 : Region.VBAA_RENDERING_BIT;
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
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }

        try {
            font = FontFactory.get(fontSet).getDefault();
        } catch (final IOException ioe) {
            throw new RuntimeException(ioe);
        }

        renderer = RegionRenderer.create(rs, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        rs.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
        // renderer = RegionRenderer.create(rs, null, null);

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_BLEND);

        renderer.init(gl, renderModes);

        initTexts();

        sceneUIController.setRenderer(renderer);

        final float pixelSizeFixed = fontSizeFixedPVP * drawable.getSurfaceHeight();
        jogampLabel = new Label(renderer.getRenderState().getVertexFactory(), renderModes, font, pixelSizeFixed, jogamp);
        jogampLabel.addMouseListener(dragZoomRotateListener);
        sceneUIController.addShape(jogampLabel);
        jogampLabel.setEnabled(enableOthers);

        final float pixelSize10Pt = font.getPixelSize(fontSizePt, dpiH);
        System.err.println("10Pt PixelSize: Display "+dpiH+" dpi, fontSize "+fontSizePt+" ppi -> "+pixelSize10Pt+" pixel-size");
        truePtSizeLabel = new Label(renderer.getRenderState().getVertexFactory(), renderModes, font, pixelSize10Pt, truePtSize);
        sceneUIController.addShape(truePtSizeLabel);
        truePtSizeLabel.setEnabled(enableOthers);
        truePtSizeLabel.translate(0, - 1.5f * jogampLabel.getLineHeight(), 0f);
        truePtSizeLabel.setColor(0.1f, 0.1f, 0.1f, 1.0f);

        /**
         *
         * [Label] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 18.814816
         * [FPS] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 15.679012
         */
        final float pixelSizeFPS = fontSizeFpsPVP * drawable.getSurfaceHeight();
        fpsLabel = new Label(renderer.getRenderState().getVertexFactory(), renderModes, font, pixelSizeFPS, "Nothing there yet");
        fpsLabel.addMouseListener(dragZoomRotateListener);
        sceneUIController.addShape(fpsLabel);
        fpsLabel.setEnabled(enableOthers);
        fpsLabel.setColor(0.3f, 0.3f, 0.3f, 1.0f);

        crossHairCtr = new CrossHair(renderer.getRenderState().getVertexFactory(), 0, 100f, 100f, 2f);
        crossHairCtr.addMouseListener(dragZoomRotateListener);
        sceneUIController.addShape(crossHairCtr);
        crossHairCtr.setEnabled(true);
        crossHairCtr.translate(0f, 0f, -1f);

        initButtons(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), renderer);
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
    public void dispose(final GLAutoDrawable drawable) {
        if(drawable instanceof GLWindow) {
            System.err.println("GPUUISceneGLListener0A: dispose (1)");
            final GLWindow glw = (GLWindow) drawable;
            detachInputListenerFrom(glw);
        } else {
            System.err.println("GPUUISceneGLListener0A: dispose (0)");
        }

        sceneUIController.dispose(drawable); // disposes all registered UIShapes

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        renderer.destroy(gl);
        screenshot.dispose(gl);
    }

    private int shotCount = 0;

    public void printScreen(final GL gl)  {
        final String modeS = Region.getRenderModeString(jogampLabel.getRenderModes());
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
    public void display(final GLAutoDrawable drawable) {
        // System.err.println("GPUUISceneGLListener0A: display");
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if(null == labels[currentText]) {
            final float pixelSizeFixed = fontSizeFixedPVP * drawable.getSurfaceHeight();
            final float dyTop = drawable.getSurfaceHeight() - 2f*jogampLabel.getLineHeight();
            final float dxMiddle = drawable.getSurfaceWidth() * relMiddle;
            labels[currentText] = new Label(renderer.getRenderState().getVertexFactory(), renderModes, font, pixelSizeFixed, strings[currentText]);
            labels[currentText].setColor(0.1f, 0.1f, 0.1f, 1.0f);
            labels[currentText].setEnabled(enableOthers);
            labels[currentText].translate(dxMiddle,
                                          dyTop - 1.5f * jogampLabel.getLineHeight()
                                                - 1.5f * truePtSizeLabel.getLineHeight(), 0f);
            labels[currentText].addMouseListener(dragZoomRotateListener);
            sceneUIController.addShape(labels[currentText]);
            System.err.println("Label["+currentText+"] CTOR: "+labels[currentText]);
            System.err.println("Label["+currentText+"] CTOR: "+Arrays.toString(labels[currentText].getTranslate()));
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
            final String modeS = Region.getRenderModeString(renderModes);
            final String text;
            if( null == actionText ) {
                final String timePrec = gl.isGLES() ? "4.0" : "4.1";
                text = String.format("%03.1f/%03.1f fps, v-sync %d, dpi %.1f, fontSize %.1f, %s-samples %d, q %d, td %"+timePrec+"f, blend %b, alpha %d, msaa %d",
                    lfps, tfps, gl.getSwapInterval(), dpiH, fontSizeFixedPVP, modeS, sceneUIController.getSampleCount(), fpsLabel.getQuality(), td,
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
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        System.err.println("GPUUISceneGLListener0A: reshape");

        //
        // Layout all shapes: Relational move regarding window coordinates
        //
        final float dw = width - lastWidth;
        final float dh = height - lastHeight;

        final float dz = 0f;
        final float dyTop = dh * relTop;
        final float dxLeft = dw * relLeft;
        final float dxRight = dw;

        for(int i=0; i<buttons.size() && i<buttonsLeftCount; i++) {
            buttons.get(i).translate(dxLeft, dyTop, dz);
        }
        for(int i=buttonsLeftCount; i<buttons.size(); i++) {
            buttons.get(i).translate(dxRight, dyTop, dz);
        }
        final float dxMiddleAbs = width * relMiddle;
        final float dyTopLabelAbs = drawable.getSurfaceHeight() - 2f*jogampLabel.getLineHeight();
        jogampLabel.setTranslate(dxMiddleAbs, dyTopLabelAbs, dz);
        truePtSizeLabel.setTranslate(dxMiddleAbs, dyTopLabelAbs, dz);
        truePtSizeLabel.setTranslate(dxMiddleAbs, dyTopLabelAbs - 1.5f * jogampLabel.getLineHeight(), 0f);
        fpsLabel.translate(0f, 0f, 0f);
        if( null != labels[currentText] ) {
            labels[currentText].setTranslate(dxMiddleAbs,
                                             dyTopLabelAbs - 1.5f * jogampLabel.getLineHeight()
                                             - 1.5f * truePtSizeLabel.getLineHeight(), 0f);
            System.err.println("Label["+currentText+"] MOVE: "+labels[currentText]);
            System.err.println("Label["+currentText+"] MOVE: "+Arrays.toString(labels[currentText].getTranslate()));
        }
        crossHairCtr.translate(dw/2f, dh/2f, 0f);

        sceneUIController.reshape(drawable, x, y, width, height);

        lastWidth = width;
        lastHeight = height;
   }

    public void attachInputListenerTo(final GLWindow window) {
        if ( !ioAttached ) {
            ioAttached = true;
            sceneUIController.attachInputListenerTo(window);
        }
    }

    public void detachInputListenerFrom(final GLWindow window) {
        if ( ioAttached ) {
            ioAttached = false;
            sceneUIController.detachInputListenerFrom(window);
        }
    }

    /**
     * We can share this instance w/ all UI elements,
     * since only mouse action / gesture is complete for a single one (press, drag, released and click).
     */
    private final UIShape.MouseGestureAdapter dragZoomRotateListener = new UIShape.MouseGestureAdapter() {
        float dragFirstX=-1f, dragFirstY=-1f;
        boolean dragFirst = false;

        @Override
        public void mousePressed(final MouseEvent e) {
            dragFirst = true;
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            dragFirst = false;
            actionText = null;
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            final Object attachment = e.getAttachment();
            if( attachment instanceof UIShape.PointerEventInfo ) {
                final UIShape.PointerEventInfo shapeEvent = (UIShape.PointerEventInfo)attachment;
                if( e.getPointerCount() == 1 ) {
                    // 1 pointer drag
                    if(dragFirst) {
                        dragFirstX = shapeEvent.objPos[0]; // e.getX();
                        dragFirstY = shapeEvent.objPos[1]; // e.getY();
                        dragFirst=false;
                        return;
                    }
                    final float nx = shapeEvent.objPos[0]; // e.getX();
                    final float ny = shapeEvent.objPos[1]; // e.getY();
                    final float dx = nx - dragFirstX;
                    final float dy = ny - dragFirstY;
                    // final float dy = -(ny - dragLastY);
                    shapeEvent.shape.translate(dx, dy, 0f);
                    final float[] tx = shapeEvent.shape.getTranslate();
                    actionText = String.format("Pos %6.2f / %6.2f / %6.2f", tx[0], tx[1], tx[2]);
                }
            }
        }

        @Override
        public void mouseWheelMoved(final MouseEvent e) {
            final Object attachment = e.getAttachment();
            if( attachment instanceof UIShape.PointerEventInfo ) {
                final UIShape.PointerEventInfo shapeEvent = (UIShape.PointerEventInfo)attachment;
                final boolean isOnscreen = PointerClass.Onscreen == e.getPointerType(0).getPointerClass();
                if( 0 == ( ~InputEvent.BUTTONALL_MASK & e.getModifiers() ) && !isOnscreen ) {
                    // offscreen vertical mouse wheel zoom
                    final float tz = 8f*e.getRotation()[1]; // vertical: wheel
                    System.err.println("Rotate.Zoom.W: "+tz);
                    shapeEvent.shape.translate(0f, 0f, tz);
                } else if( isOnscreen || e.isControlDown() ) {
                    final float[] rot =  VectorUtil.scaleVec3(e.getRotation(), e.getRotation(), FloatUtil.PI / 180.0f);
                    if( isOnscreen ) {
                        System.err.println("XXX: "+e);
                        // swap axis for onscreen rotation matching natural feel
                        final float tmp = rot[0]; rot[0] = rot[1]; rot[1] = tmp;
                        VectorUtil.scaleVec3(rot, rot, 2f);
                    }
                    shapeEvent.shape.getRotation().rotateByEuler( rot );
                }
            }
        }
        @Override
        public void gestureDetected(final GestureEvent e) {
            final Object attachment = e.getAttachment();
            if( attachment instanceof UIShape.PointerEventInfo ) {
                final UIShape.PointerEventInfo shapeEvent = (UIShape.PointerEventInfo)attachment;
                if( e instanceof PinchToZoomGesture.ZoomEvent ) {
                    final PinchToZoomGesture.ZoomEvent ze = (PinchToZoomGesture.ZoomEvent) e;
                    final float tz = ze.getDelta() * ze.getScale();
                    System.err.println("Rotate.Zoom.G: "+tz);
                    shapeEvent.shape.translate(0f, 0f, tz);
                }
            }
        } };
}
