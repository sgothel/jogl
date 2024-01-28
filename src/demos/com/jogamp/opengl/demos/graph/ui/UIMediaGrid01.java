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

import com.jogamp.common.net.Uri;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontScale;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.Gap;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.layout.Margin;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.MediaButton;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.graph.ui.widgets.MediaPlayer;
import com.jogamp.graph.ui.widgets.RangeSlider;
import com.jogamp.graph.ui.widgets.RangedGroup;
import com.jogamp.graph.ui.widgets.RangeSlider.SliderAdapter;
import com.jogamp.graph.ui.widgets.RangedGroup.SliderParam;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec2i;
import com.jogamp.math.geom.AABBox;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventAdapter;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;

/**
 * MediaButtons in a {@link RangedGroup} w/ vertical slider, filled by media files from a directory.
 */
public class UIMediaGrid01 {
    private static final float MediaGridWidth = 1f;

    static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.VBAA_RENDERING_BIT);

    private static final boolean VERBOSE_UI = true;
    private static final List<String> MEDIA_SUFFIXES = Arrays.asList("mp4", "mkv", "m2v", "avi");
    private static int aid = GLMediaPlayer.STREAM_ID_AUTO;
    private static float videoAspectRatio = 16f/9f;
    private static boolean letterBox = true;
    private static int texCount = GLMediaPlayer.TEXTURE_COUNT_DEFAULT;

    public static void main(final String[] args) throws IOException {
        float mmPerCellWidth = 50f;
        int maxMediaFiles = 10000; // Integer.MAX_VALUE;
        int gridColumns = -1;
        String mediaDir = null;
        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if (args[idx[0]].equals("-dir")) {
                    idx[0]++;
                    mediaDir = args[idx[0]];
                } else if(args[idx[0]].equals("-max")) {
                    idx[0]++;
                    maxMediaFiles = MiscUtils.atoi(args[idx[0]], maxMediaFiles);
                } else if(args[idx[0]].equals("-aid")) {
                    idx[0]++;
                    aid = MiscUtils.atoi(args[idx[0]], aid);
                } else if(args[idx[0]].equals("-ratio")) {
                    idx[0]++;
                    videoAspectRatio = MiscUtils.atof(args[idx[0]], videoAspectRatio);
                } else if(args[idx[0]].equals("-zoom")) {
                    letterBox = false;
                } else if(args[idx[0]].equals("-mmPerCell")) {
                    idx[0]++;
                    mmPerCellWidth = MiscUtils.atof(args[idx[0]], mmPerCellWidth);
                } else if(args[idx[0]].equals("-col")) {
                    idx[0]++;
                    gridColumns = MiscUtils.atoi(args[idx[0]], gridColumns);
                } else if(args[idx[0]].equals("-texCount")) {
                    idx[0]++;
                    texCount = MiscUtils.atoi(args[idx[0]], texCount);
                }
            }
        }
        System.err.println(options);
        System.err.println("mediaDir "+mediaDir);
        System.err.println("maxMediaFiles "+maxMediaFiles);
        System.err.println("aid "+aid);
        System.err.println("texCount "+texCount);
        System.err.println("boxRatio "+videoAspectRatio);
        System.err.println("letterBox "+letterBox);
        System.err.println("columns "+gridColumns);

        final List<Uri> mediaFiles = new ArrayList<Uri>();
        if( null != mediaDir && mediaDir.length() > 0 ) {
            final File dir = new File(mediaDir);
            final File[] files = dir.listFiles((final File pathname) -> {
                if( !pathname.canRead() || !pathname.isFile() ) {
                    System.err.println("Not a file or not readable: "+pathname);
                    return false;
                }
                final String name = pathname.getName();
                final int dot = name.lastIndexOf(".");
                if( 0 >= dot || dot == name.length() - 1 ) {
                    System.err.println("Not having a suffixe: "+pathname);
                    return false;
                }
                final String suffix = name.substring(dot+1);
                if( !MEDIA_SUFFIXES.contains(suffix) ) {
                    System.err.println("Not having a media suffix: "+pathname+", suffix '"+suffix+"'");
                    return false;
                }
                return true;
            });
            Arrays.sort(files, (final File f1, final File f2) -> {
                return f1.getAbsolutePath().compareTo(f2.getAbsolutePath());
            });
            for(final File f : files) {
                try {
                    final Uri uri = Uri.valueOf(f);
                    if( mediaFiles.size() < maxMediaFiles ) {
                        mediaFiles.add( uri );
                        System.err.println("Adding media file: "+uri);
                    } else {
                        System.err.println("Dropping media file: "+uri);
                    }
                } catch (final URISyntaxException e) {}
            }
        }
        if( 0 == mediaFiles.size() ) {
            System.err.println("No media files, exit.");
            return;
        }
        System.err.println("Media Files Count "+mediaFiles.size());
        final GLCapabilities reqCaps = options.getGLCaps();
        System.out.println("Requested: " + reqCaps);

        final Animator animator = new Animator(0 /* w/o AWT */);
        animator.setUpdateFPSFrames(1*60, null); // System.err);
        final GLWindow window = GLWindow.create(reqCaps);
        window.setSize(options.surface_width, options.surface_height);
        window.setVisible(true);
        System.out.println("Chosen: " + window.getChosenGLCapabilities());
        window.setTitle(UIMediaGrid01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UIMediaGrid01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });
        animator.add(window);

        final Scene scene = new Scene(options.graphAASamples);
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setPMvCullingEnabled(true);

        scene.attachInputListenerTo(window);
        window.addGLEventListener(scene);


        final float winAspectRatio, dpiV;
        final Vec2i gridDim;
        final int mediaRowsPerPage;
        {
            winAspectRatio = (float)window.getSurfaceWidth() / (float)window.getSurfaceHeight();
            final float[] ppmm = window.getPixelsPerMM(new float[2]);
            final float[] dpi = FontScale.ppmmToPPI( new float[] { ppmm[0], ppmm[1] } );
            System.err.println("DPI "+dpi[0]+" x "+dpi[1]+", "+ppmm[0]+" x "+ppmm[1]+" pixel/mm");
            dpiV = dpi[1];

            final float[] hasSurfacePixelScale1 = window.getCurrentSurfaceScale(new float[2]);
            System.err.println("HiDPI PixelScale: "+hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
            final float mmPerCellHeight = mmPerCellWidth / videoAspectRatio;
            int _mediaRowsPerPage = (int)( ( window.getSurfaceHeight() / ppmm[1] ) / mmPerCellHeight );
            if( 0 >= gridColumns ) {
                gridColumns = (int)( ( window.getSurfaceWidth() * MediaGridWidth / ppmm[0] ) / mmPerCellWidth );
            }
            if( mediaFiles.size() < gridColumns * _mediaRowsPerPage ) {
                gridColumns = (int)Math.floor( Math.sqrt ( mediaFiles.size() ) );
                _mediaRowsPerPage = gridColumns;
            }
            mediaRowsPerPage = _mediaRowsPerPage;
            gridDim = new Vec2i(gridColumns, mediaRowsPerPage);
        }
        final float mediaCellWidth = videoAspectRatio;
        final float mediaCellHeight = 1;
        final Vec2f mediaGridSize = new Vec2f(gridDim.x() * mediaCellWidth, mediaRowsPerPage * mediaCellHeight);
        System.err.println("GridDim "+gridDim);
        System.err.println("GridSize "+mediaGridSize);
        System.err.println("CellSize "+mediaCellWidth+" x "+mediaCellHeight+", vAspectRatio "+videoAspectRatio);
        System.err.println("Window "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight()+", wAspectRatio "+winAspectRatio);

        final RangedGroup mediaView;
        {
            final Group mediaGrid = new Group(new GridLayout(gridDim.x(), mediaCellWidth*0.9f, mediaCellHeight*0.9f, Alignment.FillCenter,
                                              new Gap(mediaCellHeight*0.1f, mediaCellWidth*0.1f)));
            mediaGrid.setInteractive(true).setDragAndResizeable(false).setToggleable(false).setName("MediaGrid");
            addMedia(scene, reqCaps.getGLProfile(), mediaGrid, mediaFiles, videoAspectRatio);
            mediaGrid.setRelayoutOnDirtyShapes(false); // avoid group re-validate to ease load in Group.isShapeDirty() w/ thousands of glyphs
            if( VERBOSE_UI ) {
                mediaGrid.validate(reqCaps.getGLProfile());
                System.err.println("MediaGrid "+mediaGrid);
                System.err.println("MediaGrid "+mediaGrid.getLayout());
            }
            mediaView = new RangedGroup(options.renderModes, mediaGrid, mediaGridSize,
                                        null,
                                        new SliderParam(new Vec2f(mediaCellWidth/20f, mediaGridSize.y()), mediaCellHeight/30f, true));
            mediaView.setPaddding(new Padding(mediaCellHeight/16));
            mediaView.getVertSlider().setColor(0.3f, 0.3f, 0.3f, 0.7f).setName("MediaView");
            // mediaView.setRelayoutOnDirtyShapes(false); // avoid group re-validate to ease load in Group.isShapeDirty() w/ thousands of glyphs
            if( VERBOSE_UI ) {
                mediaView.getVertSlider().addSliderListener(new SliderAdapter() {
                    @Override
                    public void dragged(final RangeSlider w, final float old_val, final float val, final float old_val_pct, final float val_pct) {
                        final Vec2f minmax = w.getMinMax();
                        final float row_f = val / mediaCellHeight;
                        System.err.println("VertSlider: row "+row_f+", val["+old_val+" -> "+val+"], pct["+(100*old_val_pct)+"% -> "+(100*val_pct)+"%], minmax "+minmax);
                    }
                });
            }
            if( VERBOSE_UI ) {
                mediaView.validate(reqCaps.getGLProfile());
                System.err.println("GlyphView "+mediaView);
            }
        }
        final Group mainGrid = new Group(new GridLayout(1, 0, 0, Alignment.None));
        mainGrid.setName("MainGrid");
        mainGrid.addShape(mediaView);
        {
            final Font fontInfo = Scene.getDefaultFont();
            final Label infoLabel = new Label(options.renderModes, fontInfo, "Not yet");
            infoLabel.setColor(0.1f, 0.1f, 0.1f, 1f);
            final Group labelBox = new Group(new BoxLayout(mediaGridSize.x(), mediaCellHeight / 10, new Alignment(Alignment.Bit.Fill.value | Alignment.Bit.CenterVert.value),
                                                           new Margin(0, 0.005f)));
            labelBox.addShape(infoLabel);
            scene.addGLEventListener(new GLEventAdapter() {
                @Override
                public void display(final GLAutoDrawable drawable) {
                    infoLabel.setText( scene.getStatusText(drawable, options.renderModes, dpiV) );
                }
            });
            mainGrid.addShape(labelBox);
        }
        scene.addShape(mainGrid);

        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                final short keySym = e.getKeySymbol();
                if( keySym == KeyEvent.VK_S ) {
                    printScreenOnGLThread(scene, window.getChosenGLCapabilities());
                } else if( keySym == KeyEvent.VK_F4 || keySym == KeyEvent.VK_ESCAPE || keySym == KeyEvent.VK_Q ) {
                    MiscUtils.destroyWindow(window);
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
        scene.screenshot(true, scene.nextScreenshotFile(null, UIMediaGrid01.class.getSimpleName(), options.renderModes, caps, "media"));
    }

    static void addMedia(final Scene scene, final GLProfile glp, final Group grid,
                         final List<Uri> mediaFiles, final float defRatio) {
        final float zoomSize = 1; // 0.95f;
        for(final Uri medium : mediaFiles) {
            final GLMediaPlayer glMPlayer = GLMediaPlayerFactory.createDefault();
            if( printNativeInfoOnce ) {
                glMPlayer.printNativeInfo(System.err);
                printNativeInfoOnce = false;
            }
            // mPlayer.setTextureMinMagFilter( new int[] { GL.GL_NEAREST, GL.GL_NEAREST } );
            glMPlayer.setTextureMinMagFilter( new int[] { GL.GL_LINEAR, GL.GL_LINEAR } );
            glMPlayer.setTextureUnit(1);

            final List<Shape> customCtrls = new ArrayList<Shape>();
            if( true ) {
                final Font fontSymbols = Scene.getSymbolsFont();
                if( null == fontSymbols ) {
                    grid.addShape( new Rectangle(options.renderModes, defRatio, 1, 0.10f) );
                    return;
                }
                final Button button = new Button(options.renderModes, fontSymbols,
                        fontSymbols.getUTF16String("reset_tv"), MediaPlayer.CtrlButtonWidth, MediaPlayer.CtrlButtonHeight, scene.getZEpsilon(16));
                button.setName("reset");
                button.setSpacing(MediaPlayer.SymSpacing, MediaPlayer.FixedSymSize).setPerp().setColor(MediaPlayer.CtrlCellCol);
                button.onClicked((final Shape s0) -> {
                    scene.forAll((final Shape s1) -> {
                       System.err.println("- "+s1.getName());
                       if( s1 instanceof MediaButton ) {
                           final MediaButton mb = (MediaButton)s1;
                           final GLMediaPlayer mp = mb.getGLMediaPlayer();
                           mp.seek(0);
                           mp.setPlaySpeed(1f);
                           mp.setAudioVolume( 0f );
                       }
                       if( s1.getName().equals("muteLabel") ) {
                           s1.setVisible(true);
                       }
                       if( s1.getName().equals("MediaGrid") ) {
                           s1.markShapeDirty();
                           System.err.println("Reset: "+s1);
                       }
                       return false;
                    });
                });
                customCtrls.add(button);
            }
            final MediaPlayer graphMPlayer = new MediaPlayer(options.renderModes, scene, glMPlayer, medium, defRatio, letterBox, zoomSize, customCtrls);
            // graphMPlayer.setSubtitleParams(MiscUtils.getSerifFont(), 0.1f);
            grid.addShape( graphMPlayer );
            glMPlayer.playStream(medium, GLMediaPlayer.STREAM_ID_AUTO, aid, GLMediaPlayer.STREAM_ID_NONE, texCount);
        }
    }
    private static boolean printNativeInfoOnce = true;
}
