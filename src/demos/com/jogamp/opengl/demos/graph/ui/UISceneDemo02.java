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
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.graph.ui.gl.Scene;
import com.jogamp.graph.ui.gl.Shape;
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
import com.jogamp.opengl.demos.es2.GearsES2;
import com.jogamp.opengl.demos.graph.MSAATool;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;

/**
 * Basic UIScene demo using a single UIShape.
 */
public class UISceneDemo02 implements GLEventListener {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static private final String defaultMediaPath = "http://archive.org/download/BigBuckBunny_328/BigBuckBunny_512kb.mp4";
    static private String filmPath = defaultMediaPath;

    public static void main(final String[] args) throws IOException {
        int sceneMSAASamples = 0;
        boolean graphVBAAMode = true;
        boolean graphMSAAMode = false;

        Font font = null;

        final int width = 1280, height = 720;

        if( 0 != args.length ) {
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-gnone")) {
                    sceneMSAASamples = 0;
                    graphMSAAMode = false;
                    graphVBAAMode = false;
                } else if(args[i].equals("-smsaa")) {
                    i++;
                    sceneMSAASamples = MiscUtils.atoi(args[i], sceneMSAASamples);
                    graphMSAAMode = false;
                    graphVBAAMode = false;
                } else if(args[i].equals("-gmsaa")) {
                    sceneMSAASamples = 0;
                    graphMSAAMode = true;
                    graphVBAAMode = false;
                } else if(args[i].equals("-gvbaa")) {
                    sceneMSAASamples = 0;
                    graphMSAAMode = false;
                    graphVBAAMode = true;
                } else if(args[i].equals("-font")) {
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
        if( sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(sceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final int renderModes;
        if( graphVBAAMode ) {
            renderModes = Region.VBAA_RENDERING_BIT;
        } else if( graphMSAAMode ) {
            renderModes = Region.MSAA_RENDERING_BIT;
        } else {
            renderModes = 0;
        }

        final GLWindow window = GLWindow.create(caps);
        // window.setPosition(10, 10);
        window.setSize(width, height);
        window.setTitle(UISceneDemo02.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        // final int renderModes = Region.COLORCHANNEL_RENDERING_BIT | Region.VBAA_RENDERING_BIT;
        window.setVisible(true);

        final Scene scene = new Scene();

        final UISceneDemo02 demo = new UISceneDemo02(scene, font, renderModes, window.getSurfaceWidth(), window.getSurfaceHeight(), DEBUG, TRACE);
        window.addGLEventListener(demo);

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
        System.err.println("m1 "+demo.shape);

        scene.waitUntilDisplayed();

        final AABBox box = scene.getBounds();
        final float diag = (float)Math.sqrt(box.getHeight()*box.getHeight() + box.getWidth()*box.getWidth());
        System.err.println("box "+box+", diag "+diag);
        final float max = box.getWidth(); // diag;
        final float step = max/200f;
        System.err.println("m2 [0 .. "+max+"], step "+step+", "+box);
        demo.testProject();
        for(float x=0; x < max; x+=step) {
            demo.shape.move(-step, 0f, 0f);
            // System.err.println(demo.shape);
            final int[] glWinPos = new int[2];
            final boolean ok = demo.shape.objToWinCoord(scene.getPMVMatrixSetup(), scene.getViewport(), demo.shape.getBounds().getCenter(), new PMVMatrix(), glWinPos);
            System.err.printf("m3: objToWinCoord: ok "+ok+", winCoords %d / %d%n", glWinPos[0], glWinPos[1]);
            // demo.testProject(glWinPos[0], glWinPos[1]);
            window.warpPointer(glWinPos[0], window.getHeight() - glWinPos[1] - 1);
            System.err.println("mm x "+x+", [0 .. "+max+"], step "+step+", "+box);
            try { Thread.sleep(10); } catch (final InterruptedException e1) { }
        }
    }
    void testProject() {
        final int[] glWinPos = new int[2];
        final boolean ok = shape.objToWinCoord(scene.getPMVMatrixSetup(), scene.getViewport(), shape.getBounds().getCenter(), new PMVMatrix(), glWinPos);
        System.err.printf("m4: objToWinCoord: ok "+ok+", winCoords %d / %d%n", glWinPos[0], glWinPos[1]);
        testProject(glWinPos[0], glWinPos[1]);
    }
    void testProject(final int glWinX, final int glWinY) {
        final float[] objPos = new float[3];
        final int[] glWinPos = new int[2];
        final PMVMatrix pmv = new PMVMatrix();
        boolean ok = shape.winToObjCoord(scene.getPMVMatrixSetup(), scene.getViewport(), glWinX, glWinY, pmv, objPos);
        System.err.printf("m5: winToObjCoord: ok "+ok+", obj [%25.20ff, %25.20ff, %25.20ff]%n", objPos[0], objPos[1], objPos[2]);
        ok = shape.objToWinCoord(scene.getPMVMatrixSetup(), scene.getViewport(), objPos, pmv, glWinPos);
        final int windx = glWinPos[0]-glWinX;
        final int windy = glWinPos[1]-glWinY;
        if( windx > 10 || windy > 10 ) {
            System.err.printf("XX: objToWinCoord: ok "+ok+", winCoords %d / %d, diff %d x %d%n", glWinPos[0], glWinPos[1], windx, windy);
            // Thread.dumpStack();
            // System.exit(-1);
        } else {
            System.err.printf("m6: objToWinCoord: ok "+ok+", winCoords %d / %d, diff %d x %d%n", glWinPos[0], glWinPos[1], windx, windy);
        }
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

    boolean ignoreInput = false;

    protected final AffineTransform tempT1 = new AffineTransform();
    protected final AffineTransform tempT2 = new AffineTransform();

    @SuppressWarnings("unused")
    public UISceneDemo02(final Scene scene, final Font font, final int renderModes, final int width, final int height, final boolean debug, final boolean trace) {
        this.font = font;
        this.renderModes = renderModes;
        this.debug = debug;
        this.trace = trace;
        this.screenshot = new GLReadBufferUtil(false, false);
        this.scene = scene;

        final float[/*2*/] sceneSize = { 0f, 0f };
        scene.surfaceToPlaneSize(new int[] { 0, 0, width, height}, sceneSize);
        System.err.println("Scene "+width+" x "+height+" pixel -> "+sceneSize[0]+" x "+sceneSize[1]+" model");
        final float sw = sceneSize[0] * 0.2f; // 0.084f; // 1/3f * sceneSize[0];
        final float sh = sw / 2.5f;
        System.err.println("Shape "+sw+" x "+sh);

        if( false ) {
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
        } else if( false ) {
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
            final Button b = new Button(SVertex.factory(), renderModes, font, "+", sw, sh);
            b.addMouseListener(shapeGesture);
            b.setLabelColor(0.0f,0.0f,0.0f);
            b.setCorner(0.0f);
            b.scale(0.4f,  0.4f, 1f);
            // b.setLabelColor(1.0f,0.0f,0.0f);
            // b.setColor(0.0f,0.0f,1.0f, 1f);
            /** Button defaults !
                    button.setLabelColor(1.0f,1.0f,1.0f);
                    button.setButtonColor(0.6f,0.6f,0.6f);
                    button.setCorner(1.0f);
                    button.setSpacing(2.0f);
             */
            shape = b;
        }
        // shape.move((sceneSize[0] - sw)/2f, (sceneSize[1] - sh)/2f, 0f); // center
        // shape.move( 1/8f * sceneSize[0], (sceneSize[1] - sh)/2f, 0f); // left-center
        shape.move( sceneSize[0] - sw, sceneSize[1] - sh, 0f); // top-right

        scene.addShape(shape);
    }

    final Shape.MouseGestureAdapter shapeGesture = new Shape.MouseGestureAdapter() {
        @Override
        public void mouseMoved(final MouseEvent e) {
            final int[] viewport = scene.getViewport(new int[4]);
            // flip to GL window coordinates, origin bottom-left
            final int glWinX = e.getX();
            final int glWinY = viewport[3] - e.getY() - 1;
            testProject(glWinX, glWinY);
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
            System.err.println("Shape.EventInfo: "+shapeEvent);
            final Shape s = shapeEvent.shape;

            final int[] viewport = scene.getViewport(new int[4]);
            final PMVMatrix pmv = new PMVMatrix();
            scene.setupMatrix(pmv, 0, 0, viewport[2], viewport[3]);

            // flip to GL window coordinates, origin bottom-left
            final int glWinX = e.getX();
            final int glWinY = viewport[3] - e.getY() - 1;
            {
                pmv.glPushMatrix();
                s.setTransform(pmv);

                final float[] objPos = new float[3];
                boolean ok = s.winToObjCoord(pmv, viewport, glWinX, glWinY, objPos);
                System.err.println("Button: Click: win["+glWinX+", "+glWinY+"] -> ok "+ok+", obj["+objPos[0]+", "+objPos[1]+", "+objPos[1]+"]");

                final int[] surfaceSize = new int[2];
                ok = s.getSurfaceSize(pmv, viewport, surfaceSize);
                System.err.println("Button: Size: ok "+ok+", pixel "+surfaceSize[0]+" x "+surfaceSize[1]);

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
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        // gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
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
            ((Window)drawable).setTitle(UISceneDemo02.class.getSimpleName()+": "+drawable.getSurfaceWidth()+" x "+drawable.getSurfaceHeight());
        }
    }

    final int[] sampleCount = { 4 };

    @Override
    public void display(final GLAutoDrawable drawable) {
        scene.display(drawable);
        if( once ) {
            once = false;
            final RegionRenderer renderer = scene.getRenderer();
            final PMVMatrix pmv = renderer.getMatrix();
            pmv.glPushMatrix();
            shape.setTransform(pmv);

            final int[] winPosSize = { 0, 0 };
            System.err.println("draw.0: "+shape);
            boolean ok = shape.getSurfaceSize(pmv, renderer.getViewport(), winPosSize);
            System.err.println("draw.1: ok "+ok+", surfaceSize "+winPosSize[0]+" x "+winPosSize[1]);
            ok = shape.objToWinCoord(pmv, renderer.getViewport(), shape.getPosition(), winPosSize);
            System.err.println("draw.2: ok "+ok+",    winCoord "+winPosSize[0]+" x "+winPosSize[1]);

            pmv.glPopMatrix();
        }
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
