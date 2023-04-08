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
package com.jogamp.opengl.demos.graph.ui;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontScale;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.Scene.PMVMatrixSetup;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.graph.ui.shapes.GLButton;
import com.jogamp.graph.ui.shapes.ImageButton;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.MediaButton;
import com.jogamp.graph.ui.shapes.RoundButton;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.es2.GearsES2;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.graph.MSAATool;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Recti;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.caps.NonFSAAGLCapsChooser;
import com.jogamp.opengl.util.texture.ImageSequence;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Complex interactive GraphUI Scene demo with different Button and Label Shapes layout on the screen.
 * <p>
 * This demo uses sets up an own {@link Scene.PMVMatrixSetup}, {@link MyPMVMatrixSetup},
 * with a plane origin bottom-left and keeping the perspective non-normalized object screen dimension of < 1.
 * </p>
 * <p>
 * Unlike {@link UISceneDemo00}, the {@link Scene}'s {@link GLEventListener} method are called directly
 * from this {@link GLEventListener} implementation, i.e. the {@link Scene} is not attached
 * to {@link GLAutoDrawable#addGLEventListener(GLEventListener)} itself.
 * </p>
 */
public class UISceneDemo20 implements GLEventListener {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static GraphUIDemoArgs options = new GraphUIDemoArgs(1280, 720, Region.VBAA_RENDERING_BIT);

    public static void main(final String[] args) {
        final float[] reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

        String fontfilename = null;
        String filmURL = null;

        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if(args[idx[0]].equals("-font")) {
                    idx[0]++;
                    fontfilename = args[idx[0]];
                } else if(args[idx[0]].equals("-pixelScale")) {
                    idx[0]++;
                    final float pS = MiscUtils.atof(args[idx[0]], reqSurfacePixelScale[0]);
                    reqSurfacePixelScale[0] = pS;
                    reqSurfacePixelScale[1] = pS;
                } else if(args[idx[0]].equals("-film")) {
                    idx[0]++;
                    filmURL = args[idx[0]];
                }
            }
        }
        System.err.println(options);

        final Display dpy = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(dpy, 0);
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(JoglVersion.getAllAvailableCapabilitiesInfo(dpy.getGraphicsDevice(), null).toString());

        final GLProfile glp = GLProfile.get(options.glProfileName);
        System.err.println("GLProfile: "+glp);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        if( options.sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(options.sceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final GLWindow window = GLWindow.create(screen, caps);
        if( 0 == options.sceneMSAASamples ) {
            window.setCapabilitiesChooser(new NonFSAAGLCapsChooser(true));
        }
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle("GraphUI Newt Demo: graph["+Region.getRenderModeString(options.renderModes)+"], msaa "+options.sceneMSAASamples);
        window.setSurfaceScale(reqSurfacePixelScale);
        // final float[] valReqSurfacePixelScale = window.getRequestedSurfaceScale(new float[2]);

        final UISceneDemo20 scene = new UISceneDemo20(fontfilename, filmURL, options.renderModes, DEBUG, TRACE);
        window.addGLEventListener(scene);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(5*60, null);
        animator.add(window);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(final WindowEvent e) {
                animator.stop();
            }
        });

        window.setVisible(true);
        animator.start();
    }

    static private final String defaultMediaURL = "http://archive.org/download/BigBuckBunny_328/BigBuckBunny_512kb.mp4";

    private boolean debug = false;
    private boolean trace = false;

    private final float noAADPIThreshold;
    private final Scene scene;

    /** -1 == AUTO, TBD @ init(..) */
    private int renderModes;

    private final Font font;
    private final Font fontButtons;
    private final Font fontFPS;
    private final Uri filmURL;

    private final float relTop = 80f/100f;
    private final float relMiddle = 22f/100f;
    private final float relLeft = 11f/100f;

    /** Relative Button Size to Window Height, normalized to 1. */
    private static final float buttonXSizeNorm = 0.084f;
    private static final float fontSizePt = 10f;
    /** Relative Font Size to Window Height  for Main Text, normalized to 1. */
    private static final float fontSizeFixedNorm = 0.04f;
    private float dpiV = 96;

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

    private GLAutoDrawable cDrawable;

    private final String jogamp = "JogAmp - Jogl Graph Module Demo";
    private final String truePtSize = fontSizePt+" pt font size label - true scale!";

    private final String longText = "JOGL: Java™ Binding for the OpenGL® API.\n\n"+
                                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec \n"+
                                    "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel\n"+
                                    "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices\n"+
                                    "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia\n"+
                                    "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat\n"+
                                    "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum\n"+
                                    "in lorem. Maecenas in ipsum ac justo scelerisque sollicitudin.\n"+
                                    "\n"+
                                    "Lyford’s in Texas & L’Anse-aux-Griffons in Québec;\n"+
                                    "Kwikpak on the Yukon delta, Kvæven in Norway, Kyulu in Kenya, not Rwanda.…\n"+
                                    "Ytterbium in the periodic table. Are Toussaint L’Ouverture, Wölfflin, Wolfe,\n"+
                                    "\n"+
                                    "The quick brown fox jumps over the lazy dog\n";

    /**
     * @param renderModes
     */
    public UISceneDemo20(final int renderModes) {
        this(null, null, renderModes, false, false);
    }

    /**
     * @param filmURL TODO
     * @param renderModes
     * @param debug
     * @param trace
     */
    public UISceneDemo20(final String fontfilename, final String filmURL, final int renderModes, final boolean debug, final boolean trace) {
        this(fontfilename, filmURL, 0f, renderModes, debug, trace);
    }

    /**
     * @param filmURL TODO
     * @param noAADPIThreshold see {@link #DefaultNoAADPIThreshold}
     * @param debug
     * @param trace
     */
    public UISceneDemo20(final String fontfilename, final String filmURL, final float noAADPIThreshold, final boolean debug, final boolean trace) {
        this(fontfilename, filmURL, noAADPIThreshold, 0, debug, trace);
    }

    private UISceneDemo20(final String fontfilename, final String filmURL, final float noAADPIThreshold, final int renderModes, final boolean debug, final boolean trace) {
        this.noAADPIThreshold = noAADPIThreshold;
        this.debug = debug;
        this.trace = trace;

        this.renderModes = renderModes;

        try {
            if( null == fontfilename ) {
                font = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",
                                       FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
            } else {
                font = FontFactory.get( new File( fontfilename ) );
            }
            System.err.println("Font "+font.getFullFamilyName());

            fontButtons = FontFactory.get(FontFactory.UBUNTU).getDefault();

            fontFPS = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMono.ttf",
                                      FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
            System.err.println("Font FPS "+fontFPS.getFullFamilyName());

        } catch (final IOException ioe) {
            throw new RuntimeException(ioe);
        }
        try {
            this.filmURL = Uri.cast( null != filmURL ? filmURL : defaultMediaURL );
        } catch (final URISyntaxException e1) {
            throw new RuntimeException(e1);
        }
        scene = new Scene();
        scene.setPMVMatrixSetup(new MyPMVMatrixSetup());
        scene.getRenderState().setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
        // scene.setSampleCount(3); // easy on embedded devices w/ just 3 samples (default is 4)?
        scene.setDebugBox(options.debugBoxThickness);
    }

    private void rotateButtons(final Vec3f angdeg) {
        angdeg.scale(FloatUtil.PI / 180.0f); // -> radians
        for(int i=0; i<buttons.size(); i++) {
            buttons.get(i).getRotation().rotateByEuler( angdeg );
        }
    }

    private void setButtonsSpacing(final float dx, final float dy) {
        for(int i=0; i<buttons.size(); i++) {
            final RoundButton b = buttons.get(i);
            if( b instanceof Button ) {
                final Button lb = (Button) b;
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
            if( b instanceof Button ) {
                ((Button)b).setSpacing(Button.DEFAULT_SPACING_X, Button.DEFAULT_SPACING_Y);
            }
        }
    }

    public static final int BUTTON_NEXTTEXT = 100;
    public static final int BUTTON_FPS = 101;
    public static final int BUTTON_VSYNC = 102;
    public static final int BUTTON_QUIT = 102;
    public static final int BUTTON_MOVIE = 200;
    public static final int BUTTON_GLEL = 200;

    public Shape getShapeByName(final int name) {
        return scene.getShapeByName(name);
    }

    private void initButtons(final GL2ES2 gl, final float scale) {
        final boolean pass2Mode = Region.isTwoPass( renderModes ) ;
        buttons.clear();

        final float buttonXSize = buttonXSizeNorm * scale;
        final float buttonYSize = buttonXSize / 2.5f;
        final float button2XSize = 2f*buttonXSize;
        final float button2YSize = 2f*buttonYSize;
        System.err.println("Button Size: "+buttonXSize+" x "+buttonYSize+", scale "+scale);
        final float xStartLeft = 0f; // aligned to left edge w/ space via reshape
        final float xStartRight = -button2XSize - button2XSize/8f; // aligned to right edge via reshape
        final float yStartTop =   0f; // aligned to top edge w/ space via reshape
        final float diffX = 1.2f * buttonXSize;
        final float diffY = 1.5f * buttonYSize;

        Button button = new Button(renderModes, fontButtons, " Next Text ", buttonXSize, buttonYSize);
        button.setName(BUTTON_NEXTTEXT);
        button.move(xStartLeft, yStartTop-diffY*buttons.size(), 0f);
        button.addMouseListener(new Shape.MouseGestureAdapter() {
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

        button = new Button(renderModes, fontButtons, "Show fps", buttonXSize, buttonYSize);
        button.setName(BUTTON_FPS);
        button.move(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
        button.setToggleable(true);
        button.setToggle(fpsLabel.isEnabled());
        button.addMouseListener(new Shape.MouseGestureAdapter() {
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

        button = new Button(renderModes, fontButtons, " V-Sync ", buttonXSize, buttonYSize);
        button.setName(BUTTON_VSYNC);
        button.move(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
        button.setToggleable(true);
        button.setToggle(gl.getSwapInterval()>0);
        button.addMouseListener(new Shape.MouseGestureAdapter() {
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

        button = new Button(renderModes, fontButtons, "< Tilt >", buttonXSize, buttonYSize);
        button.move(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
        button.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                if( shapeEvent.objPos.x() < shapeEvent.shape.getBounds().getCenter().x() ) {
                    rotateButtons(new Vec3f( 0f, -5f, 0f ) ); // left-half pressed
                } else {
                    rotateButtons(new Vec3f( 0f,  5f, 0f ) ); // right-half pressed
                }
            }
            @Override
            public void mouseWheelMoved(final MouseEvent e) {
                rotateButtons(new Vec3f( 0f,  e.getRotation()[1], 0f ) );
            } } );
        buttons.add(button);

        if( pass2Mode ) { // second column to the left
            button = new Button(renderModes, fontButtons, "< Samples >", buttonXSize, buttonYSize);
            button.move(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                    int sampleCount = scene.getSampleCount();
                    if( shapeEvent.objPos.x() < shapeEvent.shape.getBounds().getCenter().x() ) {
                        // left-half pressed
                        sampleCount--;
                    } else {
                        // right-half pressed
                        sampleCount++;
                    }
                    sampleCount = scene.setSampleCount(sampleCount); // validated / clipped
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);

            button = new Button(renderModes, fontButtons, "< Quality >", buttonXSize, buttonYSize);
            button.move(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                    if( shapeEvent.shape instanceof GraphShape ) {
                        int quality = ((GraphShape)shapeEvent.shape).getQuality();

                        if( shapeEvent.objPos.x() < shapeEvent.shape.getBounds().getCenter().x() ) {
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
                        scene.setAllShapesQuality(quality);
                    }
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
        }

        button = new Button(renderModes, fontButtons, "Quit", buttonXSize, buttonYSize);
        button.setName(BUTTON_QUIT);
        button.move(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
        button.setColor(0.7f, 0.0f, 0.0f, 1.0f);
        button.setLabelColor(1.2f, 1.2f, 1.2f);
        button.setPressedColorMod(1.1f, 0.0f, 0.0f, 1.0f);
        button.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                new InterruptSource.Thread() {
                    @Override
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
            button = new Button(renderModes, fontButtons, "Y Flip", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    rotateButtons(new Vec3f ( 0f, 180f, 0f ));
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);

            k++;
            button = new Button(renderModes, fontButtons, "X Flip", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    rotateButtons(new Vec3f ( 180f, 0f, 0f ));
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;

            button = new Button(renderModes, fontButtons, "< Space >", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                    final float dx, dy;
                    if( shapeEvent.objPos.x() < shapeEvent.shape.getBounds().getCenter().x() ) {
                        dx=-0.01f; dy=-0.005f;
                    } else {
                        dx=0.01f; dy=0.005f;
                    }
                    setButtonsSpacing(dx, dy);
                }
                @Override
                public void mouseWheelMoved(final MouseEvent e) {
                    setButtonsSpacing(e.getRotation()[0]/100f, e.getRotation()[1]/200f);
                } } );
            buttons.add(button);
            k++;

            button = new Button(renderModes, fontButtons, "< Corner >", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                    final float dc;
                    if( shapeEvent.objPos.x() < shapeEvent.shape.getBounds().getCenter().x() ) {
                        dc=-0.1f;
                    } else {
                        dc=0.1f;
                    }
                    setButtonsCorner(dc);
                }
                @Override
                public void mouseWheelMoved(final MouseEvent e) {
                    setButtonsCorner(e.getRotation()[1]/20f);
                } } );
            buttons.add(button);
            k++;

            button = new Button(renderModes, fontButtons, " Reset ", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    resetButtons();
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;

            button = new Button(renderModes, fontButtons, " Snapshot ", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    cDrawable.invoke(true, (drawable) -> {
                        scene.screenshot(drawable.getGL(), renderModes, UISceneDemo20.class.getSimpleName());
                        return true;
                    });
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;
        }

        buttonsLeftCount = buttons.size();

        final int texUnitMediaPlayer, texUnitImageButton, texUnitGLELButton;
        {
            // works - but not required ..
            texUnitMediaPlayer=1;
            texUnitImageButton=2;
            texUnitGLELButton=3;
        }

        if( true ) {
            final GLMediaPlayer mPlayer = GLMediaPlayerFactory.createDefault();
            mPlayer.setTextureUnit(texUnitMediaPlayer);
            final MediaButton mPlayerButton = new MediaButton(renderModes, button2XSize,
                    button2YSize, mPlayer);
            mPlayerButton.setName(BUTTON_MOVIE);
            mPlayerButton.setVerbose(false);
            mPlayerButton.addDefaultEventListener();
            mPlayerButton.move(xStartRight, yStartTop - diffY*1, 0f);
            mPlayerButton.setToggleable(true);
            mPlayerButton.setToggle(true); // toggle == false -> mute audio
            mPlayerButton.setToggleOffColorMod(0f, 1f, 0f, 1.0f);
            mPlayerButton.addMouseListener(dragZoomRotateListener);
            mPlayerButton.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    mPlayer.setAudioVolume( mPlayerButton.isToggleOn() ? 1f : 0f );
                } } );
            buttons.add(mPlayerButton);
            mPlayer.playStream(filmURL, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.TEXTURE_COUNT_DEFAULT);
        }
        if( true ) {
            final ImageSequence imgSeq = new ImageSequence(texUnitImageButton, true);
            final ImageButton imgButton = new ImageButton(renderModes, button2XSize, button2YSize, imgSeq);
            try {
                imgSeq.addFrame(gl, UISceneDemo20.class, "button-released-145x53.png", TextureIO.PNG);
                imgSeq.addFrame(gl, UISceneDemo20.class, "button-pressed-145x53.png", TextureIO.PNG);
            } catch (final IOException e2) {
                e2.printStackTrace();
            }
            imgSeq.setManualStepping(true);
            imgButton.move(xStartRight, yStartTop - diffY*2.5f, 0f);
            imgButton.addMouseListener(dragZoomRotateListener);
            imgButton.addMouseListener(new Shape.MouseGestureAdapter() {
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
            final GearsES2 gears = new GearsES2(0);
            gears.setVerbose(false);
            gears.setClearColor(new float[] { 0.9f, 0.9f, 0.9f, 1f } );
            final boolean[] animate = { true };
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.err.println("Gears Anim: Waiting");
                    try {
                        gears.waitForInit(true);
                    } catch (final InterruptedException e) { }
                    System.err.println("Gears Anim: Started");
                    while( gears.isInit() ) {
                        if( animate[0] ) {
                            final float ry = ( gears.getRotY() + 1 ) % 360;
                            gears.setRotY(ry);
                        }
                        try {
                            Thread.sleep(15);
                        } catch (final InterruptedException e) { }
                    }
                    System.err.println("Gears Anim: End");
                }
            }).start();
            final GLButton b = new GLButton(renderModes, button2XSize, button2YSize,
                                            texUnitGLELButton, gears, false /* useAlpha */);
            b.setName(BUTTON_GLEL);
            b.setToggleable(true);
            b.setToggle(false); // toggle == true -> animation
            b.setAnimate(false);
            b.move(xStartRight, yStartTop - diffY*4f, 0f);
            b.addMouseListener(dragZoomRotateListener);
            b.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    b.setAnimate( b.isToggleOn() );
                    animate[0] = b.getAnimate();
                } } );
            buttons.add(b);
        }
    }

    private void initTexts() {
        strings = new String[4];
        int i = 0;

        strings[i++] = "- Mouse Scroll Over Object\n"+
                       "   - General\n"+
                       "     - X-Rotation\n"+
                       "     - Shift: Y-Rotation\n"+
                       "   - Tilt, Space and Corner\n"+
                       "     - Their respective action via wheel\n"+
                       "       (shift = other value)\n"+
                       "\n"+
                       "- Mouse Drag On Object\n"+
                       "   - Click on Object and drag mouse\n"+
                       "   - Current postion in status line at bottom\n"+
                       "   - Resize when click on 1/4 bottom-left or bottom-right corner.\n"+
                       "\n"+
                       "- Tilt Button Rotate Whole Button Group";

        strings[i++] = "abcdefghijklmn\nopqrstuvwxyz\n"+
                       "ABCDEFGHIJKL\n"+
                       "MNOPQRSTUVWXYZ\n"+
                       "0123456789.:,;(*!?/\\\")$%^&-+@~#<>{}[]";

        strings[i++] = "The quick brown fox jumps over the lazy dog";

        strings[i++] = longText;

        labels = new Label[i];

        currentText = strings.length - 1;
    }


    private static final boolean enableOthers = true;


    private void initLabels(final GL2ES2 gl) {
        jogampLabel = new Label(renderModes, font, fontSizeFixedNorm, jogamp);
        jogampLabel.addMouseListener(dragZoomRotateListener);
        jogampLabel.setEnabled(enableOthers);
        scene.addShape(jogampLabel);

        truePtSizeLabel = new Label(renderModes, font, truePtSize);
        truePtSizeLabel.setEnabled(enableOthers);
        truePtSizeLabel.setColor(0.1f, 0.1f, 0.1f, 1.0f);
        scene.addShape(truePtSizeLabel);

        /**
         *
         * [Label] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 18.814816
         * [FPS] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 15.679012
         */
        fpsLabel = new Label(renderModes, fontFPS, "Nothing there yet");
        fpsLabel.addMouseListener(dragZoomRotateListener);
        fpsLabel.setEnabled(enableOthers);
        fpsLabel.setColor(0.1f, 0.1f, 0.1f, 1.0f);
        scene.addShape(fpsLabel);
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            final Window upWin = (Window)upObj;
            final MonitorDevice monitor = upWin.getMainMonitor();
            final float[] monitorDPI = MonitorDevice.perMMToPerInch( monitor.getPixelsPerMM(new float[2]) );
            final float[] sDPI = MonitorDevice.perMMToPerInch( upWin.getPixelsPerMM(new float[2]) );
            dpiV = sDPI[1];
            System.err.println("Monitor detected: "+monitor);
            System.err.println("Monitor dpi: "+monitorDPI[0]+" x "+monitorDPI[1]);
            System.err.println("Surface scale: native "+Arrays.toString(upWin.getMaximumSurfaceScale(new float[2]))+", current "+Arrays.toString(upWin.getCurrentSurfaceScale(new float[2])));
            System.err.println("Surface dpi "+sDPI[0]+" x "+sDPI[1]);
        } else {
            System.err.println("Using default DPI of "+dpiV);
        }
        if( 0 == renderModes && !FloatUtil.isZero(noAADPIThreshold, FloatUtil.EPSILON)) {
            final boolean noAA = dpiV >= noAADPIThreshold;
            final String noAAs = noAA ? " >= " : " < ";
            System.err.println("AUTO RenderMode: dpi "+dpiV+noAAs+noAADPIThreshold+" -> noAA "+noAA);
            renderModes = noAA ? 0 : Region.VBAA_RENDERING_BIT;
        }
        if(drawable instanceof GLWindow) {
            System.err.println("GPUUISceneGLListener0A: init (1)");
            final GLWindow glw = (GLWindow) drawable;
            scene.attachInputListenerTo(glw);
        } else {
            System.err.println("GPUUISceneGLListener0A: init (0)");
        }
        cDrawable = drawable;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(debug) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }
        System.err.println(JoglVersion.getGLInfo(gl, null, false /* withCapsAndExts */).toString());
        System.err.println("VSync Swap Interval: "+gl.getSwapInterval());
        System.err.println("Chosen: "+drawable.getChosenGLCapabilities());
        MSAATool.dump(drawable);

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_BLEND);

        initTexts();
        initLabels(gl);

        scene.init(drawable);

        final GLAnimatorControl a = drawable.getAnimator();
        if( null != a ) {
            a.resetFPSCounter();
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        System.err.println("GPUUISceneGLListener0A: reshape");

        //
        // Layout all shapes: Relational move regarding object coordinates
        //
        System.err.println("Reshape: Scene Plane.0 "+scene.getBounds());
        final float lastSceneWidth = scene.getBounds().getWidth();
        final float lastSceneHeight = scene.getBounds().getHeight();
        System.err.println("Reshape: Scene Plane.0 "+lastSceneWidth+" x "+lastSceneHeight);

        scene.reshape(drawable, x, y, width, height);
        final AABBox sceneBox = scene.getBounds();
        System.err.println("Reshape: Scene Plane.1 "+sceneBox);

        final float sceneWidth = sceneBox.getWidth();
        final float sceneHeight = sceneBox.getHeight();
        final float button_sxy = sceneWidth > sceneHeight ? sceneWidth : sceneHeight;
        if( buttons.isEmpty() ) {
            initButtons(drawable.getGL().getGL2ES2(), button_sxy);
            scene.addShapes(buttons);
        }

        final float dw = sceneWidth - lastSceneWidth;
        final float dh = sceneHeight - lastSceneHeight;

        final float dz = 0f;
        final float dyTop = dh * relTop;
        final float dxLeft = dw * relLeft;
        final float dxRight = dw;

        System.err.println("XXX: dw "+dw+", dh "+dh+", dxLeft "+dxLeft+", dxRight "+dxRight+", dyTop "+dyTop);

        for(int i=0; i<buttons.size() && i<buttonsLeftCount; i++) {
            // System.err.println("Button["+i+"].L0: "+buttons.get(i));
            buttons.get(i).move(dxLeft, dyTop, dz);
            // System.err.println("Button["+i+"].LM: "+buttons.get(i));
        }
        for(int i=buttonsLeftCount; i<buttons.size(); i++) {
            // System.err.println("Button["+i+"].R0: "+buttons.get(i));
            buttons.get(i).move(dxRight, dyTop, dz);
            // System.err.println("Button["+i+"].RM: "+buttons.get(i));
        }

        jogampLabel.setScale(sceneHeight, sceneHeight, 1f);

        final float dxMiddleAbs = sceneWidth * relMiddle;
        final float dyTopLabelAbs = sceneHeight - jogampLabel.getScaledLineHeight();
        jogampLabel.moveTo(dxMiddleAbs, dyTopLabelAbs - jogampLabel.getScaledLineHeight(), dz);
        {
            final float pixelSize10Pt = FontScale.toPixels(fontSizePt, dpiV);
            final float scale = pixelSize10Pt / height * sceneHeight; // normalize with dpi / surfaceHeight
            System.err.println("10Pt PixelSize: Display "+dpiV+" dpi, fontSize "+fontSizePt+" pt, "+FontScale.ptToMM(fontSizePt)+" mm -> "+pixelSize10Pt+" pixels, "+scale+" scene-size");
            truePtSizeLabel.setScale(scale, scale, 1f);
            truePtSizeLabel.moveTo(dxMiddleAbs, dyTopLabelAbs - jogampLabel.getScaledLineHeight() - truePtSizeLabel.getScaledLineHeight(), dz);
        }
        {
            final AABBox fbox = fontFPS.getGlyphBounds(scene.getStatusText(drawable, renderModes, fpsLabel.getQuality(), dpiV));
            final float scale = sceneWidth / ( 1.4f * fbox.getWidth() ); // add 40% width
            fpsLabel.setScale(scale, scale, 1f);
            fpsLabel.moveTo(sceneBox.getMinX(), sceneBox.getMinY() + scale * ( fontFPS.getMetrics().getLineGap() - fontFPS.getMetrics().getDescent() ), 0f);
            fpsLabel.validate(drawable.getGL().getGL2ES2());
            System.err.println("StatusLabel Scale: " + scale + " = " + sceneWidth + " / " + fbox.getWidth() + ", " + fbox);
            System.err.println("StatusLabel: " + fpsLabel);
        }
        if( null != labels[currentText] ) {
            labels[currentText].setScale(sceneHeight, sceneHeight, 1f);
            labels[currentText].moveTo(dxMiddleAbs,
                    dyTopLabelAbs - jogampLabel.getScaledLineHeight()
                                  - 1.5f * truePtSizeLabel.getScaledLineHeight()
                                  - labels[currentText].getScaledHeight(), 0f);
            System.err.println("Label["+currentText+"] MOVE: "+labels[currentText]);
            System.err.println("Label["+currentText+"] MOVE: "+labels[currentText].getPosition());
        }
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        System.err.println("GPUUISceneGLListener0A: dispose");

        scene.dispose(drawable); // disposes all registered UIShapes
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        // System.err.println("GPUUISceneGLListener0A: display");
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        gl.glClearColor(1f, 1f, 1f, 1f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        if(null == labels[currentText]) {
            final AABBox sbox = scene.getBounds();
            final float sceneHeight = sbox.getHeight();
            final float dyTop = sbox.getHeight() - jogampLabel.getScaledLineHeight();
            final float dxMiddle = sbox.getWidth() * relMiddle;
            labels[currentText] = new Label(renderModes, font, fontSizeFixedNorm, strings[currentText]);
            labels[currentText].setScale(sceneHeight, sceneHeight, 1f);
            labels[currentText].setColor(0.1f, 0.1f, 0.1f, 1.0f);
            labels[currentText].setEnabled(enableOthers);
            labels[currentText].validate(gl);
            labels[currentText].move(dxMiddle,
                    dyTop - jogampLabel.getScaledLineHeight()
                          - 1.5f * truePtSizeLabel.getScaledLineHeight()
                          - labels[currentText].getScaledHeight(), 0f);
            labels[currentText].addMouseListener(dragZoomRotateListener);
            scene.addShape(labels[currentText]);
            System.err.println("Label["+currentText+"] CTOR: "+labels[currentText]);
            System.err.println("Label["+currentText+"] CTOR: "+labels[currentText].getPosition());
        }
        if( fpsLabel.isEnabled() ) {
            final String text;
            if( null == actionText ) {
                text = scene.getStatusText(drawable, renderModes, fpsLabel.getQuality(), dpiV);
            } else if( null != drawable.getAnimator() ) {
                text = Scene.getStatusText(drawable.getAnimator())+", "+actionText;
            } else {
                text = actionText;
            }
            if( fpsLabel.setText(text) ) { // marks dirty only if text differs.
                // System.err.println(text);
            }
        }
        scene.display(drawable);
    }

    /**
     * We can share this instance w/ all UI elements,
     * since only mouse action / gesture is complete for a single one (press, drag, released and click).
     */
    private final Shape.MouseGestureAdapter dragZoomRotateListener = new Shape.MouseGestureAdapter() {
        @Override
        public void mouseReleased(final MouseEvent e) {
            actionText = null;
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
            if( e.getPointerCount() == 1 ) {
                final Vec3f tx = shapeEvent.shape.getPosition();
                actionText = String.format((Locale)null, "Pos %s", tx);
            }
        }

        @Override
        public void mouseWheelMoved(final MouseEvent e) {
            final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
            final Vec3f rot = new Vec3f(e.getRotation()).scale( FloatUtil.PI / 180.0f );
            // swap axis for onscreen rotation matching natural feel
            final float tmp = rot.x(); rot.setX( rot.y() ); rot.setY( tmp );
            shapeEvent.shape.getRotation().rotateByEuler( rot.scale( 2f ) );
        }
    };

    /**
     * Our PMVMatrixSetup:
     * - gluPerspective like Scene's default
     * - no normal scale to 1, keep distance to near plane for rotation effects. We scale Shapes
     * - translate origin to bottom-left
     */
    public static class MyPMVMatrixSetup implements PMVMatrixSetup {
        @Override
        public void set(final PMVMatrix pmv, final Recti viewport) {
            final float ratio = (float)viewport.width()/(float)viewport.height();
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            pmv.gluPerspective(Scene.DEFAULT_ANGLE, ratio, Scene.DEFAULT_ZNEAR, Scene.DEFAULT_ZFAR);
            pmv.glTranslatef(0f, 0f, Scene.DEFAULT_SCENE_DIST);

            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();

            // Translate origin to bottom-left
            final AABBox planeBox0 = new AABBox();
            setPlaneBox(planeBox0, pmv, viewport);
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glTranslatef(planeBox0.getMinX(), planeBox0.getMinY(), 0f);
            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        }

        @Override
        public void setPlaneBox(final AABBox planeBox, final PMVMatrix pmv, final Recti viewport) {
            Scene.getDefaultPMVMatrixSetup().setPlaneBox(planeBox, pmv, viewport);
        }
    };

}
