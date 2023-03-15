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
import java.util.Locale;

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.graph.ui.gl.Scene;
import com.jogamp.graph.ui.gl.Shape;
import com.jogamp.graph.ui.gl.Shape.EventInfo;
import com.jogamp.graph.ui.gl.shapes.Button;
import com.jogamp.graph.ui.gl.shapes.GLButton;
import com.jogamp.graph.ui.gl.shapes.MediaButton;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.demos.es2.GearsES2;
import com.jogamp.opengl.demos.graph.MSAATool;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;

/**
 * Basic UIScene demo using a single UIShape.
 */
public class UISceneDemo01 implements GLEventListener {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static private final String defaultMediaPath = "http://archive.org/download/BigBuckBunny_328/BigBuckBunny_512kb.mp4";
    static private String filmPath = defaultMediaPath;

    public static void main(final String[] args) throws IOException {
        Font font = null;
        if( 0 != args.length ) {
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-font")) {
                    i++;
                    font = FontFactory.get(new File(args[i]));
                } else if(args[i].equals("-film")) {
                    i++;
                    filmPath = args[i];
                }
            }
        }
        if( null == font ) {
            font = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_SERIF);
        }
        System.err.println("Font: "+font.getFullFamilyName());

        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        System.out.println("Requested: " + caps);

        final GLWindow window = GLWindow.create(caps);
        // window.setPosition(10, 10);
        window.setSize(800, 400);
        window.setTitle(UISceneDemo01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        // final int renderModes = Region.COLORCHANNEL_RENDERING_BIT | Region.VBAA_RENDERING_BIT;
        final int renderModes = Region.COLORCHANNEL_RENDERING_BIT;
        window.setVisible(true);
        final UISceneDemo01 uiGLListener = new UISceneDemo01(font, renderModes, window.getSurfaceWidth(), window.getSurfaceHeight(), DEBUG, TRACE);
        window.addGLEventListener(uiGLListener);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(5*60, null);
        animator.add(window);

        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent arg0) {
                if(arg0.getKeyCode() == KeyEvent.VK_F4) {
                    new InterruptSource.Thread() {
                        @Override
                        public void run() {
                            window.destroy();
                        } }.start();
                }
            }
        });
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(final WindowEvent e) {
                animator.stop();
            }
        });

        animator.start();
    }

    @SuppressWarnings("unused")
    private final Font font;

    private final GLReadBufferUtil screenshot;
    private final int renderModes;
    private final boolean debug;
    private final boolean trace;

    private final Scene scene;
    private final Shape shape;

    private final float[] position = new float[] {0,0,0};

    private final float sceneDist = -1/5f;
    private final float zNear = 0.1f, zFar = 7000f;

    boolean ignoreInput = false;

    protected final AffineTransform tempT1 = new AffineTransform();
    protected final AffineTransform tempT2 = new AffineTransform();

    @SuppressWarnings("unused")
    public UISceneDemo01(final Font font, final int renderModes, final int width, final int height, final boolean debug, final boolean trace) {
        this.font = font;
        this.renderModes = renderModes;
        this.debug = debug;
        this.trace = trace;
        this.screenshot = new GLReadBufferUtil(false, false);

        scene = new Scene();
        final float[/*2*/] sceneSize = { 0f, 0f };
        scene.surfaceToObjSize(width, height, sceneSize);
        System.err.println("Scene "+width+" x "+height+" pixel -> "+sceneSize[0]+" x "+sceneSize[1]+" model");
        final float sw = 1/3f * sceneSize[0];
        final float sh = sw / 2f;
        System.err.println("Shape "+sw+" x "+sh);

        if( true ) {
            Uri filmUri;
            try {
                filmUri = Uri.cast( filmPath );
            } catch (final URISyntaxException e1) {
                throw new RuntimeException(e1);
            }
            final GLMediaPlayer mPlayer = GLMediaPlayerFactory.createDefault();
            // mPlayer.setTextureUnit(texUnitMediaPlayer);
            final MediaButton b = new MediaButton(SVertex.factory(), renderModes, sw, sh, mPlayer);
            b.setVerbose(false);
            b.addDefaultEventListener();
            b.setToggleable(true);
            b.setToggle(true);
            b.setToggleOffColorMod(0f, 1f, 0f, 1.0f);
            b.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    mPlayer.setAudioVolume( b.isToggleOn() ? 1f : 0f );
                } } );
            mPlayer.playStream(filmUri, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.TEXTURE_COUNT_DEFAULT);
            shape = b;
        } else if( true ) {
            final GLEventListener glel;
            {
                final GearsES2 gears = new GearsES2(0);
                gears.setVerbose(false);
                gears.setClearColor(new float[] { 0.9f, 0.9f, 0.9f, 1f } );
                glel = gears;
            }
            final int texUnit = 1;
            final GLButton b = new GLButton(scene.getVertexFactory(), renderModes,
                                            sw, sh, texUnit, glel, false /* useAlpha */);
            b.setToggleable(true);
            b.setToggle(true); // toggle == true -> animation
            b.setAnimate(true);
            b.addMouseListener(shapeGesture);
            b.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    b.setAnimate( b.isToggleOn() );
                } } );
            shape = b;
        } else {
            final Button b = new Button(SVertex.factory(), renderModes, font, "Click me!", sw, sh);
            b.setLabelColor(0.0f,0.0f,0.0f);
            b.addMouseListener(shapeGesture);
            shape = b;
        }
        shape.move((sceneSize[0] - sw)/2f, (sceneSize[1] - sh)/2f, 0f); // center
        scene.addShape(shape);
    }

    final Shape.MouseGestureAdapter shapeGesture = new Shape.MouseGestureAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
            System.err.println("Shape.EventInfo: "+shapeEvent);
            final Shape s = shapeEvent.shape;

            final RegionRenderer renderer = scene.getRenderer();
            final PMVMatrix pmv = new PMVMatrix();
            scene.setupMatrix(pmv, 0, 0);

            // flip to GL window coordinates, origin bottom-left
            final int[] viewport = renderer.getViewport(new int[4]);
            final int glWinX = e.getX();
            final int glWinY = viewport[3] - e.getY() - 1;
            {
                pmv.glPushMatrix();
                s.setTransform(pmv);

                final float[] objPos = new float[3];
                s.winToObjCoord(renderer, glWinX, glWinY, objPos);
                System.err.println("Button: Click: Win "+glWinX+"/"+glWinY+" -> Obj "+objPos[0]+"/"+objPos[1]+"/"+objPos[1]);

                final int[] surfaceSize = new int[2];
                s.getSurfaceSize(renderer, surfaceSize);
                System.err.println("Button: Size: Pixel "+surfaceSize[0]+" x "+surfaceSize[1]);

                pmv.glPopMatrix();
            }
        } };

    public final float[] getPosition() { return position; }

    @Override
    public void init(final GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(debug) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }

        if(drawable instanceof GLWindow) {
            final GLWindow glw = (GLWindow) drawable;
            scene.attachInputListenerTo(glw);
        }
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
        MSAATool.dump(drawable);

        System.err.println("Init: Shape "+shape);
        scene.init(drawable);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int xstart, final int ystart, final int width, final int height) {
        // Layout all shapes: Just stay centered
        //
        // shape.move(0f, 0f, 0f);
        System.err.println("Reshape: Shape "+shape);

        System.err.println("Reshape: Scene Plane.R "+scene.getBounds());

        scene.reshape(drawable, xstart, ystart, width, height);

        System.err.println("Reshape: Scene Plane.R "+scene.getBounds());

        if( drawable instanceof Window ) {
            ((Window)drawable).setTitle(UISceneDemo01.class.getSimpleName()+": "+drawable.getSurfaceWidth()+" x "+drawable.getSurfaceHeight());
        }
    }

    final int[] sampleCount = { 4 };

    @Override
    public void display(final GLAutoDrawable drawable) {
        scene.display(drawable);
    }
    static boolean once = true;

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        scene.dispose(drawable); // disposes all registered UIShapes

        screenshot.dispose(gl);
    }

    public void printScreen(final GL gl)  {
        final RegionRenderer renderer = scene.getRenderer();
        final String modeS = Region.getRenderModeString(renderModes);
        final String filename = String.format((Locale)null, "UISceneDemo01-shot%03d-%03dx%03d-S_%s_%02d.png",
                shotCount++, renderer.getWidth(), renderer.getHeight(),
                modeS, scene.getSampleCount());
        gl.glFinish(); // just make sure rendering finished ..
        if(screenshot.readPixels(gl, false)) {
            screenshot.write(new File(filename));
            System.err.println("Wrote: "+filename);
        }
    }
    private int shotCount = 0;

    public void setIgnoreInput(final boolean v) {
        ignoreInput = v;
    }
    public boolean getIgnoreInput() {
        return ignoreInput;
    }
}
