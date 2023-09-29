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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontScale;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.Gap;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.shapes.MediaButton;
import com.jogamp.math.Vec2i;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.EventMask;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * MediaButtons in a grid, filled by media files from a directory in different aspect ratios
 */
public class UIMediaGrid00 {
    static CommandlineOptions options = new CommandlineOptions(1920, 1080, Region.VBAA_RENDERING_BIT);

    public static int aid = GLMediaPlayer.STREAM_ID_AUTO;

    public static void main(final String[] args) throws IOException {
        final List<Uri> mediaFiles = new ArrayList<Uri>();
        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if (args[idx[0]].equals("-file")) {
                    idx[0]++;
                    final Uri u = Uri.tryUriOrFile( args[idx[0]] );
                    if( null != u ) {
                        mediaFiles.add(u);
                    }
                } else if(args[idx[0]].equals("-aid")) {
                    idx[0]++;
                    aid = MiscUtils.atoi(args[idx[0]], aid);
                } else if(args[idx[0]].equals("-mute")) {
                    aid = GLMediaPlayer.STREAM_ID_NONE;
                }
            }
        }
        System.err.println(options);
        System.err.println("aid "+aid);

        if( 0 == mediaFiles.size() ) {
            System.err.println("No media files, exit.");
            return;
        }
        for(final Uri uri : mediaFiles) {
            System.err.println("- "+uri);
        }
        final Vec2i gridDim = new Vec2i(4, mediaFiles.size());

        // final Font fontStatus = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMono.ttf", FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        final Font fontInfo = FontFactory.get(FontFactory.UBUNTU).getDefault();

        final GLProfile reqGLP = GLProfile.get(options.glProfileName);
        System.err.println("GLProfile: "+reqGLP);

        final GLCapabilities caps = new GLCapabilities(reqGLP);
        caps.setAlphaBits(4);
        if( options.sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(options.sceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final Animator animator = new Animator(0 /* w/o AWT */);
        animator.setUpdateFPSFrames(1*60, null); // System.err);
        final GLWindow window = GLWindow.create(caps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UIMediaGrid00.class.getSimpleName());
        window.setVisible(true);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(FontView01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });
        animator.add(window);

        final Scene scene = new Scene(options.graphAASamples);
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setFrustumCullingEnabled(true);

        scene.attachInputListenerTo(window);
        window.addGLEventListener(scene);


        final float[] ppmm = window.getPixelsPerMM(new float[2]);
        {
            final float[] dpi = FontScale.ppmmToPPI( new float[] { ppmm[0], ppmm[1] } );
            System.err.println("DPI "+dpi[0]+" x "+dpi[1]+", "+ppmm[0]+" x "+ppmm[1]+" pixel/mm");

            final float[] hasSurfacePixelScale1 = window.getCurrentSurfaceScale(new float[2]);
            System.err.println("HiDPI PixelScale: "+hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
        }
        final Group mediaGrid;
        {
            final float cellWidth = 1f;
            final float cellHeight = 1f;
            mediaGrid = new Group(new GridLayout(gridDim.x(), cellWidth*0.9f, cellHeight*0.9f, Alignment.FillCenter, new Gap(cellHeight*0.1f, cellWidth*0.1f)));
            mediaGrid.setRelayoutOnDirtyShapes(false);
        }
        addMedia(reqGLP, fontInfo, mediaGrid, gridDim, mediaFiles, null);
        mediaGrid.validate(reqGLP);
        System.err.println("MediaGrid "+mediaGrid);
        System.err.println("MediaGrid "+mediaGrid.getLayout());

        final Group mainGrid = new Group(new GridLayout(1, 0f, 0f, Alignment.None));
        mainGrid.addShape(mediaGrid);
        scene.addShape(mainGrid);

        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                final short keySym = e.getKeySymbol();
                if( keySym == KeyEvent.VK_S ) {
                    printScreenOnGLThread(scene, window.getChosenGLCapabilities());
                } else if( keySym == KeyEvent.VK_DOWN ) {
                } else if( keySym == KeyEvent.VK_PAGE_DOWN ) {
                } else if( keySym == KeyEvent.VK_UP ) {
                } else if( keySym == KeyEvent.VK_PAGE_UP ) {
                } else if( keySym == KeyEvent.VK_F4 || keySym == KeyEvent.VK_ESCAPE || keySym == KeyEvent.VK_Q ) {
                    MiscUtils.destroyWindow(window);
                } else if( keySym == KeyEvent.VK_SPACE ) {
                    final Shape a = scene.getActiveShape();
                    if( a instanceof MediaButton ) {
                        final MediaButton b = (MediaButton)a;
                        final GLMediaPlayer mPlayer = b.getGLMediaPlayer();
                        if( GLMediaPlayer.State.Paused == mPlayer.getState() ) {
                            mPlayer.resume();
                        } else if(GLMediaPlayer.State.Uninitialized == mPlayer.getState()) {
                            mPlayer.playStream(mPlayer.getUri(), GLMediaPlayer.STREAM_ID_AUTO, aid, MediaTexCount);
                        } else if( e.isShiftDown() ) {
                            mPlayer.stop();
                        } else {
                            mPlayer.pause(false);
                        }
                    }
                }
            }
        });

        animator.start();
        scene.waitUntilDisplayed();
        {
            final AABBox sceneBox = scene.getBounds();
            final AABBox mainGridBox = mainGrid.getBounds();
            final float sx = sceneBox.getWidth() / mainGridBox.getWidth();
            final float sy = sceneBox.getHeight() / mainGridBox.getHeight();
            final float sxy = Math.min(sx, sy);
            System.err.println("SceneBox "+sceneBox);
            System.err.println("MainGridBox "+mainGridBox);
            System.err.println("scale sx "+sx+", sy "+sy+", sxy "+sxy);
            // scale, moveTo origin bottom-left, then move up to top-left corner.
            mainGrid.scale(sxy, sxy, 1f).moveTo(sceneBox.getLow()).move(0, sceneBox.getHeight()-mainGridBox.getHeight()*sxy, 0);
        }
        printScreenOnGLThread(scene, window.getChosenGLCapabilities());
        // stay open ..
    }
    static void printScreenOnGLThread(final Scene scene, final GLCapabilitiesImmutable caps) {
        scene.screenshot(true, scene.nextScreenshotFile(null, UIMediaGrid00.class.getSimpleName(), options.renderModes, caps, "media"));
    }

    public static final int MediaTexUnitMediaPlayer = 1;
    public static final int MediaTexCount = 3; // GLMediaPlayer.TEXTURE_COUNT_DEFAULT

    public static MediaButton createMediaButton(final Uri medium, final float aratio, final boolean letterBox, final Shape.MouseGestureListener mouseListener) {
        final float borderSz = 0.02f;
        final Vec4f borderToggleOff = new Vec4f(0, 0, 0, 0.7f);
        final Vec4f borderToggleOn = new Vec4f(0, 0, 1, 0.7f);
        final Vec4f borderActive = new Vec4f(1, 1, 0, 0.7f);

        final GLMediaPlayer mPlayer = GLMediaPlayerFactory.createDefault();
        mPlayer.setTextureUnit(MediaTexUnitMediaPlayer);
        // mPlayer.setAudioChannelLimit(1); // enforce mono to enjoy spatial 3D position effects
        final MediaButton button = new MediaButton(options.renderModes, aratio, 1, mPlayer);
        button.setVerbose(false).addDefaultEventListener().setTextureLetterbox(letterBox).setFixedARatioResize(true);
        if( aid == GLMediaPlayer.STREAM_ID_NONE ) {
            button.setToggle( true ); // toggle == false -> mute audio
        } else {
            button.setToggleable(true);
            button.setToggle(false); // toggle == false -> mute audio
        }
        mPlayer.setAudioVolume( 0f );
        if( null != mouseListener ) {
            button.addMouseListener(mouseListener);
        }
        button.onToggle( (final Shape s) -> {
            mPlayer.setAudioVolume( s.isToggleOn() ? 1f : 0f );
            if( s.isActive() ) {
                s.setBorderColor(borderActive);
            } else {
                if( s.isToggleOn() ) {
                    s.setBorderColor(borderToggleOn);
                } else {
                    s.setBorderColor(borderToggleOff);
                }
            }
        });
        button.onActivation( (final Shape s) -> {
            System.err.println("XXX "+s);
            if( s.isActive() ) {
                s.setBorderColor(borderActive);
            } else {
                if( s.isToggleOn() ) {
                    s.setBorderColor(borderToggleOn);
                } else {
                    s.setBorderColor(borderToggleOff);
                }
            }
        });
        mPlayer.addEventListener( new GLMediaEventListener() {
            @Override
            public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) { }

            @Override
            public void attributesChanged(final GLMediaPlayer mp, final EventMask eventMask, final long when) {
                // System.err.println("MediaButton AttributesChanges: "+eventMask+", when "+when);
                // System.err.println("MediaButton State: "+mp);
                if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Init) ) {
                    System.err.println(mp.toString());
                }
                if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.EOS) ) {
                    final StreamException err = mp.getStreamException();
                    if( null != err ) {
                        System.err.println("MovieSimple State: Exception: "+err.getMessage());
                    } else {
                        new InterruptSource.Thread() {
                            @Override
                            public void run() {
                                mp.setPlaySpeed(1f);
                                mp.seek(0);
                                mp.resume();
                            }
                        }.start();
                    }
                }
            }
        });
        button.setToggleOffColorMod(1f, 1f, 1f, 1f);
        button.setPressedColorMod(1f, 1f, 1f, 0.85f);
        button.setPerp().setBorderColor(borderToggleOff).setBorder(borderSz);
        mPlayer.playStream(medium, GLMediaPlayer.STREAM_ID_AUTO, aid, MediaTexCount);
        return button;
    }

    static void addMedia(final GLProfile glp, final Font font, final Group grid,
                         final Vec2i gridDim, final List<Uri> mediaFiles,
                         final Shape.MouseGestureListener mouseListener) {
        for(final Uri medium : mediaFiles) {
            grid.addShape( createMediaButton(medium, 16f/9f, false, mouseListener) );
            grid.addShape( createMediaButton(medium,  4f/3f, false, mouseListener) );
            grid.addShape( createMediaButton(medium, 16f/9f,  true, mouseListener) );
            grid.addShape( createMediaButton(medium,  4f/3f,  true, mouseListener) );
        }
    }
}
