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

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.graph.ui.gl.Scene;
import com.jogamp.graph.ui.gl.Shape;
import com.jogamp.graph.ui.gl.shapes.Button;
import com.jogamp.graph.ui.gl.shapes.CrossHair;
import com.jogamp.graph.ui.gl.shapes.GLButton;
import com.jogamp.graph.ui.gl.shapes.MediaButton;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.es2.GearsES2;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;

/**
 * Basic UIScene demo using a single UIShape.
 */
public class UISceneDemo01 {
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

        final Scene scene = new Scene();
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final Shape shape = makeShape(font, renderModes);
        if( false ) {
        shape.onMove(new Shape.Listener() {
            @Override
            public void run(final Shape shape) {
                final float[] p = shape.getPosition();
                System.err.println("Shape moved: "+p[0]+", "+p[1]+", "+p[2]);
            }
        });
        shape.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                final int[] viewport = scene.getViewport(new int[4]);
                // flip to GL window coordinates, origin bottom-left
                final int glWinX = e.getX();
                final int glWinY = viewport[3] - e.getY() - 1;
                testProject(scene, shape, glWinX, glWinY);
            }
            @Override
            public void mouseDragged(final MouseEvent e) {
                final int[] viewport = scene.getViewport(new int[4]);
                // flip to GL window coordinates, origin bottom-left
                final int glWinX = e.getX();
                final int glWinY = viewport[3] - e.getY() - 1;
                testProject(scene, shape, glWinX, glWinY);
            }
        } );
        }
        scene.addShape(shape);

        final AABBox shapeBox = shape.getBounds(glp);
        System.err.println("m0 "+shape);


        // scene.surfaceToPlaneSize(width, height, null);

        final GLWindow window = GLWindow.create(caps);
        window.setSize(width, height);
        window.setTitle(UISceneDemo01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.setVisible(true);
        window.addGLEventListener(scene);
        scene.attachInputListenerTo(window);

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

        scene.waitUntilDisplayed();

        final AABBox sceneBox = scene.getBounds();
        final float[] surfaceSize = { 0f, 0f };
        scene.surfaceToPlaneSize(scene.getViewport(), surfaceSize);

        System.err.println("m1 scene "+surfaceSize[0]+" x "+surfaceSize[1]);
        System.err.println("m1 scene "+sceneBox);
        System.err.println("m1.0 "+shape);
        // shape.moveTo(-shapeBox.getWidth()/2f, -shapeBox.getHeight()/2f, 0f);
        shape.scale(sceneBox.getWidth(), sceneBox.getWidth(), 1f);
        System.err.println("m1.1 "+shape);
        shape.scale(0.4f,  0.4f, 1f);
        System.err.println("m1.2 "+shape);
        try { Thread.sleep(1000); } catch (final InterruptedException e1) { }

        final float min = sceneBox.getMinX();
        final float max = sceneBox.getMaxX(); // - shapeBox.getWidth(); //  * shape.getScaleX();
        // shape.moveTo(min, -shapeBox.getHeight()/2f, 0f);
        shape.moveTo(min, 0f, 0f);
        if( false ) {
        try { Thread.sleep(1000); } catch (final InterruptedException e1) { }
        final float step = (max-min)/200f;
        System.err.println("m2 ["+min+" .. "+max+"], step "+step);
        for(float x=min; x < max; x+=step) {
            shape.move(step, 0f, 0f);
            // System.err.println(shape);
            final int[] glWinPos = new int[2];
            final boolean ok = shape.objToWinCoord(scene.getPMVMatrixSetup(), scene.getViewport(), shape.getBounds().getCenter(), new PMVMatrix(), glWinPos);
            // System.err.printf("m3: objToWinCoord: ok "+ok+", winCoords %d / %d%n", glWinPos[0], glWinPos[1]);
            // demo.testProject(glWinPos[0], glWinPos[1]);
            // window.warpPointer(glWinPos[0], window.getHeight() - glWinPos[1] - 1);
            System.err.println("mm x "+x+", ["+min+" .. "+max+"], step "+step);
            try { Thread.sleep(10); } catch (final InterruptedException e1) { }
        }
        }
        System.err.println("X");
    }

    static void testProject(final Scene scene, final Shape shape, final int glWinX, final int glWinY) {
        final float[] objPos = new float[3];
        final int[] glWinPos = new int[2];
        final PMVMatrix pmv = new PMVMatrix();
        boolean ok = shape.winToObjCoord(scene.getPMVMatrixSetup(), scene.getViewport(), glWinX, glWinY, pmv, objPos);
        System.err.printf("MM1: winToObjCoord: ok "+ok+", obj [%25.20ff, %25.20ff, %25.20ff]%n", objPos[0], objPos[1], objPos[2]);
        ok = shape.objToWinCoord(scene.getPMVMatrixSetup(), scene.getViewport(), objPos, pmv, glWinPos);
        final int windx = glWinPos[0]-glWinX;
        final int windy = glWinPos[1]-glWinY;
        System.err.printf("MM2: objToWinCoord: ok "+ok+", winCoords %d / %d, diff %d x %d%n", glWinPos[0], glWinPos[1], windx, windy);
    }

    @SuppressWarnings("unused")
    static Shape makeShape(final Font font, final int renderModes) {
        final float sw = 0.2f;
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
            return b;
        } else if( false ) {
            final GLEventListener glel;
            {
                final GearsES2 gears = new GearsES2(0);
                gears.setVerbose(false);
                gears.setClearColor(new float[] { 0.9f, 0.9f, 0.9f, 1f } );
                glel = gears;
            }
            final int texUnit = 1;
            final GLButton b = new GLButton(SVertex.factory(), renderModes,
                                            sw, sh, texUnit, glel, false /* useAlpha */);
            b.setToggleable(true);
            b.setToggle(true); // toggle == true -> animation
            b.setAnimate(true);
            b.addMouseListener(new Shape.MouseGestureAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    b.setAnimate( b.isToggleOn() );
                } } );
            return b;
        } else if( true ){
            final Button b = new Button(SVertex.factory(), renderModes, font, "+", sw, sh);
            // b.setLabelColor(0.0f,0.0f,0.0f);
            b.setCorner(0.0f);
            return b;
        } else {
            final CrossHair b = new CrossHair(SVertex.factory(), renderModes, sw, sw, 1f/100f);
            return b;
        }
    }
}
