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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.es2.GearsES2;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.graph.MSAATool;
import com.jogamp.common.net.Uri;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontScale;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.graph.ui.gl.Scene;
import com.jogamp.graph.ui.gl.Shape;
import com.jogamp.graph.ui.gl.shapes.Button;
import com.jogamp.graph.ui.gl.shapes.GLButton;
import com.jogamp.graph.ui.gl.shapes.ImageButton;
import com.jogamp.graph.ui.gl.shapes.Label;
import com.jogamp.graph.ui.gl.shapes.MediaButton;
import com.jogamp.graph.ui.gl.shapes.RoundButton;
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
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.texture.ImageSequence;
import com.jogamp.opengl.util.texture.TextureIO;

public class GPUUISceneGLListener0A implements GLEventListener {
    static private final String defaultMediaURL = "http://archive.org/download/BigBuckBunny_328/BigBuckBunny_512kb.mp4";

    private boolean debug = false;
    private boolean trace = false;

    private final float noAADPIThreshold;
    private final Scene sceneUICntrl;

    /** -1 == AUTO, TBD @ init(..) */
    private int renderModes;

    private final Font font;
    private final Font fontFPS;
    private final Uri filmURL;

    private final float sceneDist = 3000f;
    private final float zNear = 0.1f, zFar = 7000f;

    private final float relTop = 80f/100f;
    private final float relMiddle = 22f/100f;
    private final float relLeft = 11f/100f;

    /** Proportional Button Size to Window Height, per-vertical-pixels [PVP] */
    private final float buttonYSizePVP = 0.084f;
    private final float buttonXSizePVP = 0.084f; // 0.105f;
    private final float fontSizePt = 10f;
    /** Proportional Font Size to Window Height  for Main Text, per-vertical-pixels [PVP] */
    private final float fontSizeFixedPVP = 0.04f;
    /** Proportional Font Size to Window Height for FPS Status Line, per-vertical-pixels [PVP] */
    private final float fontSizeFpsPVP = 0.03f;
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

    private final GLReadBufferUtil screenshot;

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
    public GPUUISceneGLListener0A(final int renderModes) {
        this(null, null, renderModes, false, false);
    }

    /**
     * @param filmURL TODO
     * @param renderModes
     * @param debug
     * @param trace
     */
    public GPUUISceneGLListener0A(final String fontfilename, final String filmURL, final int renderModes, final boolean debug, final boolean trace) {
        this(fontfilename, filmURL, 0f, renderModes, debug, trace);
    }

    /**
     * @param filmURL TODO
     * @param noAADPIThreshold see {@link #DefaultNoAADPIThreshold}
     * @param debug
     * @param trace
     */
    public GPUUISceneGLListener0A(final String fontfilename, final String filmURL, final float noAADPIThreshold, final boolean debug, final boolean trace) {
        this(fontfilename, filmURL, noAADPIThreshold, 0, debug, trace);
    }

    private GPUUISceneGLListener0A(final String fontfilename, final String filmURL, final float noAADPIThreshold, final int renderModes, final boolean debug, final boolean trace) {
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

            fontFPS = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMonoBold.ttf",
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
        {
            final RenderState rs = RenderState.createRenderState(SVertex.factory());
            final RegionRenderer renderer = RegionRenderer.create(rs, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
            rs.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
            // renderer = RegionRenderer.create(rs, null, null);

            sceneUICntrl = new Scene(renderer, sceneDist, zNear, zFar);
            // sceneUIController.setSampleCount(3); // easy on embedded devices w/ just 3 samples (default is 4)?
        }
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
            buttons.get(i).move(tx, ty, tz);
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
        return sceneUICntrl.getShapeByName(name);
    }

    private void initButtons(final GL2ES2 gl, final int width, final int height) {
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

        Button button = new Button(SVertex.factory(), renderModes, font, "Next Text", buttonXSize, buttonYSize);
        button.setName(BUTTON_NEXTTEXT);
        button.move(xStartLeft,yStartTop-diffY*buttons.size(), 0f);
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

        button = new Button(SVertex.factory(), renderModes, font, "Show FPS", buttonXSize, buttonYSize);
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

        button = new Button(SVertex.factory(), renderModes, font, "V-Sync", buttonXSize, buttonYSize);
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

        button = new Button(SVertex.factory(), renderModes, font, "< Tilt >", buttonXSize, buttonYSize);
        button.move(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
        button.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                if( shapeEvent.objPos[0] < shapeEvent.shape.getBounds().getCenter()[0] ) {
                    rotateButtons(new float[] { 0f, -5f, 0f}); // left-half pressed
                } else {
                    rotateButtons(new float[] { 0f,  5f, 0f}); // right-half pressed
                }
            }
            @Override
            public void mouseWheelMoved(final MouseEvent e) {
                rotateButtons(new float[] { 0f,  e.getRotation()[1], 0f});
            } } );
        buttons.add(button);

        if( pass2Mode ) { // second column to the left
            button = new Button(SVertex.factory(), renderModes, font, "< Samples >", buttonXSize, buttonYSize);
            button.move(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                    int sampleCount = sceneUICntrl.getSampleCount();
                    if( shapeEvent.objPos[0] < shapeEvent.shape.getBounds().getCenter()[0] ) {
                        // left-half pressed
                        sampleCount--;
                    } else {
                        // right-half pressed
                        sampleCount++;
                    }
                    sampleCount = sceneUICntrl.setSampleCount(sampleCount); // validated / clipped
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);

            button = new Button(SVertex.factory(), renderModes, font, "< Quality >", buttonXSize, buttonYSize);
            button.move(xStartLeft,yStartTop - diffY*buttons.size(), 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
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
                    sceneUICntrl.setAllShapesQuality(quality);
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
        }

        button = new Button(SVertex.factory(), renderModes, font, "Quit", buttonXSize, buttonYSize);
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
            button = new Button(SVertex.factory(), renderModes, font, "Y Flip", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    rotateButtons(new float[] { 0f, 180f, 0f});
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);

            k++;
            button = new Button(SVertex.factory(), renderModes, font, "X Flip", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    rotateButtons(new float[] { 180f, 0f, 0f});
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;

            button = new Button(SVertex.factory(), renderModes, font, "+", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
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
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;

            button = new Button(SVertex.factory(), renderModes, font, "< Space >", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                    final float dx, dy;
                    if( shapeEvent.objPos[0] < shapeEvent.shape.getBounds().getCenter()[0] ) {
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

            button = new Button(SVertex.factory(), renderModes, font, "< Corner >", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                    final float dc;
                    if( shapeEvent.objPos[0] < shapeEvent.shape.getBounds().getCenter()[0] ) {
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

            button = new Button(SVertex.factory(), renderModes, font, "Reset", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    resetButtons();
                } } );
            button.addMouseListener(dragZoomRotateListener);
            buttons.add(button);
            k++;

            button = new Button(SVertex.factory(), renderModes, font, "Snapshot", buttonXSize, buttonYSize);
            button.move(xStartLeft - diffX*j,yStartTop - diffY*k, 0f);
            button.addMouseListener(new Shape.MouseGestureAdapter() {
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
        {
            // works - but not required ..
            texUnitMediaPlayer=1;
            texUnitImageButton=2;
            texUnitGLELButton=3;
        }

        if( true ) {
            final GLMediaPlayer mPlayer = GLMediaPlayerFactory.createDefault();
            mPlayer.setTextureUnit(texUnitMediaPlayer);
            final MediaButton mPlayerButton = new MediaButton(sceneUICntrl.getVertexFactory(), renderModes,
                    button2XSize, button2YSize, mPlayer);
            mPlayerButton.setName(BUTTON_MOVIE);
            mPlayerButton.setVerbose(true);
            mPlayerButton.addDefaultEventListener();
            mPlayerButton.move(xStartRight, yStartTop - diffY*1, 0f);
            mPlayerButton.setToggleable(true);
            mPlayerButton.setToggle(false); // toggle == false -> mute audio
            mPlayerButton.setToggleOffColorMod(0f, 1f, 0f, 1.0f);
            mPlayerButton.addMouseListener(dragZoomRotateListener);
            mPlayerButton.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    mPlayer.setAudioVolume( mPlayerButton.isToggleOn() ? 1f : 0f );
                } } );
            buttons.add(mPlayerButton);
            mPlayer.initStream(filmURL, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.TEXTURE_COUNT_DEFAULT);
        }
        if( true ) {
            final ImageSequence imgSeq = new ImageSequence(texUnitImageButton, true);
            final ImageButton imgButton = new ImageButton(sceneUICntrl.getVertexFactory(), renderModes,
                    button2XSize, button2YSize, imgSeq);
            try {
                imgSeq.addFrame(gl, GPUUISceneGLListener0A.class, "button-released-145x53.png", TextureIO.PNG);
                imgSeq.addFrame(gl, GPUUISceneGLListener0A.class, "button-pressed-145x53.png", TextureIO.PNG);
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
            final GLEventListener glel;
            {
                final GearsES2 gears = new GearsES2(0);
                gears.setVerbose(false);
                gears.setClearColor(new float[] { 0.9f, 0.9f, 0.9f, 1f } );
                glel = gears;
            }
            final GLButton glelButton = new GLButton(sceneUICntrl.getVertexFactory(), renderModes,
                    button2XSize, button2YSize,
                    texUnitGLELButton, glel, false /* useAlpha */,
                    (int)(button2XSize), (int)(button2YSize));
            glelButton.setName(BUTTON_GLEL);
            glelButton.setToggleable(true);
            glelButton.setToggle(false); // toggle == true -> animation
            glelButton.setAnimate(false);
            glelButton.move(xStartRight, yStartTop - diffY*4f, 0f);
            glelButton.addMouseListener(dragZoomRotateListener);
            glelButton.addMouseListener(new Shape.MouseGestureAdapter() {
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

        strings[i++] = "- Mouse Scroll Over Object\n"+
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

        strings[i++] = "The quick brown fox jumps over the lazy dog";

        strings[i++] = longText;

        labels = new Label[i];

        currentText = strings.length - 1;
    }


    private static final boolean enableOthers = true;
    private static final boolean reshape_ui = false; // incomplete: button positioning


    private void setupUI(final GLAutoDrawable drawable) {
        final float pixelSizeFixed = fontSizeFixedPVP * drawable.getSurfaceHeight();
        jogampLabel = new Label(sceneUICntrl.getVertexFactory(), renderModes, font, pixelSizeFixed, jogamp);
        jogampLabel.addMouseListener(dragZoomRotateListener);
        sceneUICntrl.addShape(jogampLabel);
        jogampLabel.setEnabled(enableOthers);

        final float pixelSize10Pt = FontScale.toPixels(fontSizePt, dpiV);
        System.err.println("10Pt PixelSize: Display "+dpiV+" dpi, fontSize "+fontSizePt+" ppi -> "+pixelSize10Pt+" pixel-size");
        truePtSizeLabel = new Label(sceneUICntrl.getVertexFactory(), renderModes, font, pixelSize10Pt, truePtSize);
        sceneUICntrl.addShape(truePtSizeLabel);
        truePtSizeLabel.setEnabled(enableOthers);
        truePtSizeLabel.move(0, - 1.5f * jogampLabel.getLineHeight(), 0f);
        truePtSizeLabel.setColor(0.1f, 0.1f, 0.1f, 1.0f);

        /**
         *
         * [Label] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 18.814816
         * [FPS] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 15.679012
         */
        final float pixelSizeFPS = fontSizeFpsPVP * drawable.getSurfaceHeight();
        fpsLabel = new Label(sceneUICntrl.getVertexFactory(), renderModes, fontFPS, pixelSizeFPS, "Nothing there yet");
        fpsLabel.addMouseListener(dragZoomRotateListener);
        sceneUICntrl.addShape(fpsLabel);
        fpsLabel.setEnabled(enableOthers);
        fpsLabel.setColor(0.1f, 0.1f, 0.1f, 1.0f);
        fpsLabel.move(0f, pixelSizeFPS * (fontFPS.getMetrics().getLineGap() - fontFPS.getMetrics().getDescent()), 0f);

        initButtons(drawable.getGL().getGL2ES2(), drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        for(int i=0; i<buttons.size(); i++) {
            sceneUICntrl.addShape(buttons.get(i));
        }
    }

    private void reshapeUI(final GLAutoDrawable drawable) {
        final float pixelSizeFixed = fontSizeFixedPVP * drawable.getSurfaceHeight();
        jogampLabel.setPixelSize(pixelSizeFixed);

        final float pixelSize10Pt = FontScale.toPixels(fontSizePt, dpiV);
        System.err.println("10Pt PixelSize: Display "+dpiV+" dpi, fontSize "+fontSizePt+" ppi -> "+pixelSize10Pt+" pixel-size");
        truePtSizeLabel.setPixelSize(pixelSize10Pt);

        /**
         *
         * [Label] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 18.814816
         * [FPS] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 15.679012
         */
        final float pixelSizeFPS = fontSizeFpsPVP * drawable.getSurfaceHeight();
        fpsLabel.setPixelSize(pixelSizeFPS);

        final float buttonXSize = buttonXSizePVP * drawable.getSurfaceWidth();
        // final float buttonYSize = buttonYSizePVP * height;
        final float buttonYSize = buttonXSize / 2.5f;
        final float button2XSize = 2f*buttonXSize;
        final float button2YSize = 2f*buttonYSize;

        for(int i=0; i<buttons.size() && i<buttonsLeftCount; i++) {
            buttons.get(i).setSize(buttonXSize, buttonYSize);
        }
        for(int i=buttonsLeftCount; i<buttons.size(); i++) {
            buttons.get(i).setSize(button2XSize, button2YSize);
        }

        for(int i=0; i<labels.length; i++) {
            final Label l = labels[i];
            if( null != l ) {
                l.setPixelSize(pixelSizeFixed);
            }
        }
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
            sceneUICntrl.attachInputListenerTo(glw);
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

        sceneUICntrl.init(drawable);

        final GLAnimatorControl a = drawable.getAnimator();
        if( null != a ) {
            a.resetFPSCounter();
        }

        setupUI(drawable);
    }

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

        if( reshape_ui ) {
            reshapeUI(drawable);
        }
        for(int i=0; i<buttons.size() && i<buttonsLeftCount; i++) {
            buttons.get(i).move(dxLeft, dyTop, dz);
        }
        for(int i=buttonsLeftCount; i<buttons.size(); i++) {
            buttons.get(i).move(dxRight, dyTop, dz);
        }

        final float dxMiddleAbs = width * relMiddle;
        final float dyTopLabelAbs = drawable.getSurfaceHeight() - 2f*jogampLabel.getLineHeight();
        jogampLabel.setPosition(dxMiddleAbs, dyTopLabelAbs, dz);
        truePtSizeLabel.setPosition(dxMiddleAbs, dyTopLabelAbs, dz);
        truePtSizeLabel.setPosition(dxMiddleAbs, dyTopLabelAbs - 1.5f * jogampLabel.getLineHeight(), 0f);
        fpsLabel.move(0f, 0f, 0f);
        if( null != labels[currentText] ) {
            labels[currentText].setPosition(dxMiddleAbs,
                    dyTopLabelAbs - 1.5f * jogampLabel.getLineHeight()
                    - 1.5f * truePtSizeLabel.getLineHeight(), 0f);
            System.err.println("Label["+currentText+"] MOVE: "+labels[currentText]);
            System.err.println("Label["+currentText+"] MOVE: "+Arrays.toString(labels[currentText].getPosition()));
        }

        sceneUICntrl.reshape(drawable, x, y, width, height);

        lastWidth = width;
        lastHeight = height;
    }
    float lastWidth = 0f, lastHeight = 0f;

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        System.err.println("GPUUISceneGLListener0A: dispose");

        sceneUICntrl.dispose(drawable); // disposes all registered UIShapes

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        screenshot.dispose(gl);
    }

    private int shotCount = 0;

    public void printScreen(final GL gl)  {
        final RegionRenderer renderer = sceneUICntrl.getRenderer();
        final String modeS = Region.getRenderModeString(jogampLabel.getRenderModes());
        final String filename = String.format((Locale)null, "GraphUIDemo-shot%03d-%03dx%03d-S_%s_%02d.png",
                shotCount++, renderer.getWidth(), renderer.getHeight(),
                modeS, sceneUICntrl.getSampleCount());
        gl.glFinish(); // just make sure rendering finished ..
        if(screenshot.readPixels(gl, false)) {
            screenshot.write(new File(filename));
            System.err.println("Wrote: "+filename);
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        // System.err.println("GPUUISceneGLListener0A: display");

        if(null == labels[currentText]) {
            final float pixelSizeFixed = fontSizeFixedPVP * drawable.getSurfaceHeight();
            final float dyTop = drawable.getSurfaceHeight() - 2f*jogampLabel.getLineHeight();
            final float dxMiddle = drawable.getSurfaceWidth() * relMiddle;
            labels[currentText] = new Label(sceneUICntrl.getVertexFactory(), renderModes, font, pixelSizeFixed, strings[currentText]);
            labels[currentText].setColor(0.1f, 0.1f, 0.1f, 1.0f);
            labels[currentText].setEnabled(enableOthers);
            labels[currentText].move(dxMiddle,
                    dyTop - 1.5f * jogampLabel.getLineHeight()
                    - 1.5f * truePtSizeLabel.getLineHeight(), 0f);
            labels[currentText].addMouseListener(dragZoomRotateListener);
            sceneUICntrl.addShape(labels[currentText]);
            System.err.println("Label["+currentText+"] CTOR: "+labels[currentText]);
            System.err.println("Label["+currentText+"] CTOR: "+Arrays.toString(labels[currentText].getPosition()));
        }
        if( fpsLabel.isEnabled() ) {
            final String text;
            if( null == actionText ) {
                text = sceneUICntrl.getStatusText(drawable, renderModes, fpsLabel.getQuality(), dpiV);
            } else if( null != drawable.getAnimator() ) {
                text = Scene.getStatusText(drawable.getAnimator())+", "+actionText;
            } else {
                text = actionText;
            }
            if( fpsLabel.setText(text) ) { // marks dirty only if text differs.
                // System.err.println(text);
            }
        }
        sceneUICntrl.display(drawable);
    }

    public void attachInputListenerTo(final GLWindow window) {
        sceneUICntrl.attachInputListenerTo(window);
    }

    public void detachInputListenerFrom(final GLWindow window) {
        sceneUICntrl.detachInputListenerFrom(window);
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
                final float[] tx = shapeEvent.shape.getPosition();
                actionText = String.format((Locale)null, "Pos %6.2f / %6.2f / %6.2f", tx[0], tx[1], tx[2]);
            }
        }

        @Override
        public void mouseWheelMoved(final MouseEvent e) {
            final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
            final boolean isOnscreen = PointerClass.Onscreen == e.getPointerType(0).getPointerClass();
            if( 0 == ( ~InputEvent.BUTTONALL_MASK & e.getModifiers() ) && !isOnscreen ) {
                // offscreen vertical mouse wheel zoom
                final float tz = 100f*e.getRotation()[1]; // vertical: wheel
                System.err.println("Rotate.Zoom.W: "+tz);
                shapeEvent.shape.move(0f, 0f, tz);
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
        @Override
        public void gestureDetected(final GestureEvent e) {
            final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
            if( e instanceof PinchToZoomGesture.ZoomEvent ) {
                final PinchToZoomGesture.ZoomEvent ze = (PinchToZoomGesture.ZoomEvent) e;
                final float tz = ze.getDelta() * ze.getScale();
                System.err.println("Rotate.Zoom.G: "+tz);
                shapeEvent.shape.move(0f, 0f, tz);
            }
        } };
}
