/**
 * Copyright 2023-2024 JogAmp Community. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.jogamp.common.os.Clock;
import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.Font.Glyph;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.TooltipShape;
import com.jogamp.graph.ui.TooltipText;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.Gap;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.layout.Margin;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.GlyphShape;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.graph.ui.widgets.RangeSlider;
import com.jogamp.graph.ui.widgets.RangedGroup;
import com.jogamp.graph.ui.widgets.RangedGroup.SliderParam;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.newt.MonitorDevice;
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
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.graph.MSAATool;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.caps.NonFSAAGLCapsChooser;

/**
 * This may become a little font viewer application, having FontForge as its role model.
 *
 * This UISceneDemo* implements as a GLEventListener, capable to be used with any GLAutoDrawable.
 *
 * Notable: The actual {@link GlyphShape} created for the glyph-grid {@link Group}
 * is reused as-is in the top-right info-box as well as in the {@link TooltipShape}.
 *
 * This is possible only when not modifying the scale or position of the {@link GlyphShape},
 * achieved by simply wrapping it in a {@link Group}.
 * The latter gets scaled and translated when dropped
 * into each target {@link Group} with a {@link Group.Layout}.<br/>
 *
 * This is also good example using GraphUI with a Directed Acyclic Graph (DAG) arrangement.
 */
public class FontView01 implements GLEventListener  {
    // static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.MSAA_RENDERING_BIT, Region.DEFAULT_AA_QUALITY, 4);
    // static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.NORM_RENDERING_BIT, 0, 0, 8);
    static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.VBAA_RENDERING_BIT);

    public static void main(final String[] args) throws IOException {
        boolean showUnderline = false;
        boolean showLabel = false;
        boolean perfanal = false;
        float mmPerCell = 8f;
        String fontFilename = null;
        int gridColumns = -1;

        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if(args[idx[0]].equals("-font")) {
                    idx[0]++;
                    fontFilename = args[idx[0]];
                } else if(args[idx[0]].equals("-mmPerCell")) {
                    idx[0]++;
                    mmPerCell = MiscUtils.atof(args[idx[0]], mmPerCell);
                } else if(args[idx[0]].equals("-gridCols")) {
                    idx[0]++;
                    gridColumns = MiscUtils.atoi(args[idx[0]], gridColumns);
                } else if(args[idx[0]].equals("-showUnderline")) {
                    showUnderline = true;
                } else if(args[idx[0]].equals("-showLabel")) {
                    showLabel = true;
                } else if(args[idx[0]].equals("-perf")) {
                    perfanal = true;
                } else if(args[idx[0]].equals("-max")) {
                    idx[0]++;
                    max_glyph_count = MiscUtils.atoi(args[idx[0]], max_glyph_count);
                }
            }
        }
        System.err.println(JoglVersion.getInstance().toString());
        System.err.println(options);

        final FontView01 demo = new FontView01(fontFilename, options.renderModes, false, false);
        demo.showUnderline = showUnderline;
        demo.showLabel = showLabel;
        demo.mmPerCell = mmPerCell;
        demo.gridColumns = gridColumns;

        final GLCapabilities caps = options.getGLCaps();
        System.out.println("Requested: " + caps);

        final GLWindow window = GLWindow.create(caps);
        if( 0 == options.sceneMSAASamples ) {
            window.setCapabilitiesChooser(new NonFSAAGLCapsChooser(false));
        }
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(FontView01.class.getSimpleName()+": "+demo.font.getFullFamilyName()+", "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());

        window.addGLEventListener(demo);

        final Animator animator = new Animator(0 /* w/o AWT */);
        animator.setUpdateFPSFrames(5*60, null);
        animator.add(window);
        animator.setExclusiveContext(options.exclusiveContext);

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

        if( options.wait_to_start ) {
            System.err.println("Press enter to continue");
            MiscUtils.waitForKey("Start");
        }

        window.setVisible(true);
        System.out.println("Chosen: " + window.getChosenGLCapabilities());

        animator.start();
    }

    private static final float GlyphGridWidth = 3/4f; // FBO AA: 3/4f = 0.75f dropped fine grid lines @ 0.2f thickness; 0.70f OK
    private static final float GlyphGridBorderThickness = 0.02f; // thickness 0.2f dropping
    private static final Vec4f GlyphGridBorderColorComplex = new Vec4f(0.2f, 0.2f, 0.2f, 1);
    private static final Vec4f GlyphGridBorderColorSimple = new Vec4f(0.2f, 0.2f, 0.7f, 1);

    private static int max_glyph_count = 10000;

    private static boolean VERBOSE_GLYPHS = false;
    private static boolean VERBOSE_UI = false;

    private boolean showUnderline = false;
    private boolean showLabel = false;
    private boolean debug = false;
    private boolean trace = false;
    private final Font font, fontStatus, fontInfo;
    private float dpiY = 96;
    private float pixPerMMY;

    private final Scene scene;

    private int maxGlyphCount = Integer.MAX_VALUE;
    private float mmPerCell = 8f;
    private int gridColumns = -1;
    private boolean firstReshape = true;

    Group mainView = null;
    Label infoLabel = null;
    int lastCodepoint = 0;

    /**
     * @param renderModes
     */
    public FontView01(final int renderModes) {
        this(null, renderModes, false, false);
    }

    private FontView01(final String fontFilename, final int renderModes, final boolean debug, final boolean trace) {
        this.debug = debug;
        this.trace = trace;

        FontView01.options.renderModes = renderModes;

        try {
            if( null == fontFilename ) {
                font = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",
                                       FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
            } else {
                font = FontFactory.get( new File( fontFilename ) );
            }
            System.err.println("Font "+font.getFullFamilyName());

            fontStatus = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMono.ttf", FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
            fontInfo = FontFactory.get(FontFactory.UBUNTU).getDefault();
            System.err.println("Status Font "+fontStatus.getFullFamilyName());
            System.err.println("Info Font "+fontInfo.getFullFamilyName());
        } catch (final IOException ioe) {
            throw new RuntimeException(ioe);
        }

        scene = new Scene(options.graphAASamples);
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setPMvCullingEnabled(true);
        scene.getRenderer().setHintBits(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
    }

    public float getMMPerCell() { return mmPerCell; }
    public void setMMPerCell(final float v) { mmPerCell = v; }

    public int getMaxGlyphCount() { return maxGlyphCount; }
    public void setMaxGlyphCount(final int v) { maxGlyphCount = v; }

    @Override
    public void init(final GLAutoDrawable glad) {
        // Resolution independent, no screen size
        //
        final int glyphGridRowsPerPage;

        final Object upObj = glad.getUpstreamWidget();
        final float[] pixPerMMXY, dpiXY;
        if( upObj instanceof Window ) {
            final Window win = (Window)upObj;
            win.addMouseListener( new Shape.MouseGestureAdapter() {
                @Override
                public void mouseWheelMoved(final MouseEvent e) {
                    if( null == mainView || e.getX() >= win.getSurfaceWidth() / 2f ) {
                        return;
                    }
                    if( e.isControlDown() ) {
                        // Scale and move back to center
                        final float[] rot = e.getRotation();
                        final float r = e.isShiftDown() ? rot[0] : rot[1];
                        final float s = 1f+r/200f;
                        final AABBox b0 = mainView.getBounds();
                        final AABBox bs = new AABBox(b0).scale(s, s, 1);
                        final float dw = b0.getWidth() - bs.getWidth();
                        final float dh = b0.getHeight() - bs.getHeight();
                        mainView.scale(s, s, 1);
                        final Vec3f s1 = mainView.getScale();
                        mainView.move(s1.x()*dw/2f, s1.y()*dh/2f, 0);
                        System.err.println("scale +"+s+" = "+s1);
                        e.setConsumed(true);
                    } else {
                        final Vec3f rot = new Vec3f(e.getRotation()).scale( FloatUtil.PI / 180.0f );
                        // swap axis for onscreen rotation matching natural feel
                        final float tmp = rot.x(); rot.setX( rot.y() ); rot.setY( tmp );
                        mainView.setRotation( mainView.getRotation().rotateByEuler( rot.scale( 2f ) ) );
                        e.setConsumed(true);
                    }
                } } );
            win.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(final KeyEvent e) {
                    final short keySym = e.getKeySymbol();
                    if( keySym == KeyEvent.VK_F4 || keySym == KeyEvent.VK_ESCAPE || keySym == KeyEvent.VK_Q ) {
                        MiscUtils.destroyWindow(glad);
                    } else if( keySym == KeyEvent.VK_S ) {
                        printScreenOnGLThread(scene, glad.getChosenGLCapabilities(), font, lastCodepoint);
                    }
                }
            });
            final MonitorDevice monitor = win.getMainMonitor();
            final float[] monitorDPI = MonitorDevice.mmToInch( monitor.getPixelsPerMM(new float[2]) );
            pixPerMMXY = win.getPixelsPerMM(new float[2]);
            pixPerMMY = pixPerMMXY[1]; // [px]/[mm]
            dpiXY = MonitorDevice.mmToInch(new float[2], pixPerMMXY);
            dpiY = dpiXY[1];
            final float[] hasSurfacePixelScale1 = win.getCurrentSurfaceScale(new float[2]);
            System.err.println("HiDPI PixelScale: "+hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
            System.err.println("Monitor detected: "+monitor);
            System.err.println("Monitor dpi: "+monitorDPI[0]+" x "+monitorDPI[1]);
            System.err.println("Surface scale: native "+Arrays.toString(win.getMaximumSurfaceScale(new float[2]))+", current "+Arrays.toString(win.getCurrentSurfaceScale(new float[2])));
            System.err.println("HiDPI PixelScale: "+hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
        } else {
            dpiY = 96;
            dpiXY = new float[] { dpiY, dpiY };
            pixPerMMY = MonitorDevice.inchToMM(dpiY); // [px]/[mm]
            pixPerMMXY = new float[] { pixPerMMY, pixPerMMY };
            System.err.println("Using default DPI of "+dpiY);
        }
        glyphGridRowsPerPage = (int)( ( glad.getSurfaceHeight() / pixPerMMXY[1] ) / mmPerCell );
        if( 0 >= gridColumns ) {
            gridColumns = (int)( ( glad.getSurfaceWidth() * GlyphGridWidth / pixPerMMXY[0] ) / mmPerCell );
        }

        System.err.println("Surface "+glad.getSurfaceWidth()+"x"+glad.getSurfaceHeight());
        System.err.println("Surface dpi "+dpiXY[0]+" x "+dpiXY[1]);
        System.err.println("Surface pixPerMM: "+pixPerMMXY[0]+" x "+pixPerMMXY[1]);
        System.err.println("mmPerCell "+mmPerCell+", glyphGridRowsPerPage "+glyphGridRowsPerPage+", gridColumns "+gridColumns);

        {
            final int o = options.fixDefaultAARenderModeWithDPIThreshold(dpiY);
            System.err.println("AUTO RenderMode: dpi "+dpiY+", threshold "+options.noAADPIThreshold+
                               ", mode "+Region.getRenderModeString(o)+" -> "+
                               Region.getRenderModeString(options.renderModes));
        }
        if(glad instanceof GLWindow) {
            System.err.println("FontView01: init (1.1)");
            final GLWindow glw = (GLWindow) glad;
            scene.attachInputListenerTo(glw);
        } else {
            System.err.println("FontView01: init (1.0)");
        }

        GL2ES2 gl = glad.getGL().getGL2ES2();
        if(debug) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }
        System.err.println(JoglVersion.getGLInfo(gl, null, false /* withCapsAndExts */).toString());
        System.err.println("VSync Swap Interval: "+gl.getSwapInterval());
        System.err.println("Chosen: "+glad.getChosenGLCapabilities());
        MSAATool.dump(glad);

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        // gl.glDepthFunc(GL.GL_LEQUAL);
        // gl.glEnable(GL.GL_BLEND);

        final float glyphGridCellSize = GlyphGridWidth / gridColumns;
        final Vec2f glyphGridSize = new Vec2f(GlyphGridWidth, glyphGridRowsPerPage * glyphGridCellSize);

        final long t0 = Clock.currentNanos();

        final GridDim gridDim = new GridDim(font, gridColumns, glyphGridRowsPerPage, 1, maxGlyphCount);
        final Vec2f glyphGridTotalSize = new Vec2f(glyphGridSize.x(), gridDim.rows * glyphGridCellSize);
        System.err.println(gridDim);
        System.err.println("GlyphGrid[pgsz "+glyphGridSize+", totsz "+glyphGridTotalSize+", cellSz "+glyphGridCellSize+"]");

        lastCodepoint = gridDim.contourChars.get(0);
        final Shape.PointerListener glyphHoverListener;
        {
            final Group glyphShapeBox = new Group( new BoxLayout( 1f, 1f, Alignment.FillCenter, new Margin(0.01f) ) );
            final Group glyphShapeHolder = new Group();
            glyphShapeHolder.setName("GlyphShapeHolderInfo");
            glyphShapeBox.addShape( glyphShapeHolder );

            final Group glyphInfoBox = new Group( new BoxLayout( 1f, 1f, Alignment.FillCenter, new Margin(0.025f, 0.025f, 0.025f, 0.025f) ) );
            final Label glyphInfo = new Label(options.renderModes, fontStatus, "Nothing there yet");
            setGlyphInfo(fontStatus, glyphInfo, font.getGlyph( 'A' ));
            glyphInfo.setColor(0.1f, 0.1f, 0.1f, 1.0f);
            glyphInfoBox.addShape(glyphInfo);
            glyphInfoBox.setRelayoutOnDirtyShapes(false); // avoid group re-validate on info text changes

            glyphHoverListener = (final Shape s, final Vec3f pos, final MouseEvent e) -> {
                System.err.println("XXX: Hover: "+s+", event "+e);
                final GlyphShape g0 = getGlyphShape(s);

                e.setConsumed(true);

                // Selected Glyph g0
                scene.invoke(false, (final GLAutoDrawable d) -> {
                    // Handle old one
                    if( 1 == glyphShapeHolder.getShapeCount() ) {
                        final GlyphShape old = (GlyphShape) glyphShapeHolder.getShapeByIdx(0);
                        if( null != old ) {
                            if( old.getGlyph().getCodepoint() == g0.getGlyph().getCodepoint() ) {
                                // System.err.println("GlyphShape Same: "+old);
                                return true; // abort - no change
                            } else {
                                glyphShapeHolder.removeShape(old);
                            }
                        } else {
                            // System.err.println("GlyphShape Old: Null");
                        }
                    } else {
                        // System.err.println("GlyphShape Old: None");
                    }
                    // New Glyph
                    glyphShapeHolder.addShape(g0);
                    setGlyphInfo(fontStatus, glyphInfo, g0.getGlyph());
                    lastCodepoint = g0.getGlyph().getCodepoint();
                    return true;
                });
            };

            final GLCapabilitiesImmutable reqCaps = glad.getRequestedGLCapabilities();
            final Group glyphInfoView = new Group(new GridLayout(2, 0f, 0f, Alignment.None));
            {
                // final float gapSizeX = ( gridDim.rawSize.x() - 1 ) * cellSize * 0.1f;
                final Group glyphGrid = new Group(new GridLayout(gridDim.columns, glyphGridCellSize*0.9f, glyphGridCellSize*0.9f, Alignment.FillCenter,
                                                  new Gap(glyphGridCellSize*0.1f)));
                glyphGrid.setInteractive(true).setDragAndResizable(false).setToggleable(false).setName("GlyphGrid");
                addGlyphs(reqCaps.getGLProfile(), font, glyphGrid, gridDim, showUnderline, showLabel, fontStatus, fontInfo, glyphHoverListener);
                glyphGrid.setRelayoutOnDirtyShapes(false); // avoid group re-validate to ease load in Group.isShapeDirty() w/ thousands of glyphs
                if( VERBOSE_UI ) {
                    glyphGrid.validate(reqCaps.getGLProfile());
                    System.err.println("GlyphGrid "+glyphGrid);
                    System.err.println("GlyphGrid "+glyphGrid.getLayout());
                }
                {
                    final GlyphShape gs = getGlyphShape( glyphGrid );
                    if( null != gs ) {
                        glyphShapeHolder.addShape(gs);
                    }
                }
                final RangedGroup glyphView = new RangedGroup( options.renderModes, glyphGrid, glyphGridSize,
                                                               null,
                                                               new SliderParam( new Vec2f(glyphGridCellSize/4f, glyphGridSize.y()), glyphGridCellSize/10f, true ) );
                glyphView.getVertSlider().setColor(0.3f, 0.3f, 0.3f, 0.7f).setName("GlyphView");
                if( VERBOSE_UI ) {
                    glyphView.getVertSlider().addChangeListener((final RangeSlider w, final float old_val, final float val, final float old_val_pct, final float val_pct, final Vec3f pos, final MouseEvent e) -> {
                        final Vec2f minmax = w.getMinMax();
                        final float row_f = val / glyphGridCellSize;
                        System.err.println("VertSlider: row["+row_f+".."+(row_f+gridDim.rowsPerPage-1)+"]/"+gridDim.rows+
                                           ", val["+old_val+" -> "+val+"]/"+minmax.y()+", pct["+(100*old_val_pct)+"% -> "+(100*val_pct)+"%], cellSz "+glyphGridCellSize);
                        System.err.println("VertSlider: "+w.getDescription());
                    });
                }
                glyphView.getVertSlider().receiveKeyEvents(glyphGrid);
                // glyphView.getVertSlider().receiveMouseEvents(glyphGrid);
                if( VERBOSE_UI ) {
                    glyphView.validate(reqCaps.getGLProfile());
                    System.err.println("GlyphView "+glyphView);
                }
                glyphInfoView.addShape(glyphView);
            }
            {
                final float infoCellWidth = ( 1f - glyphGridSize.x() ) * 1.15f; // FIXME: Layout issues to force 15% more width to use more size?
                final float infoCellHeight = glyphGridSize.y() * 0.5f;
                final Group infoGrid = new Group( new GridLayout(1, infoCellWidth, infoCellHeight * 1f, Alignment.FillCenter, new Gap(infoCellHeight*0.001f, 0)) );
                infoGrid.setPaddding( new Padding(0, 0, 0, 0.01f) );
                infoGrid.addShape(glyphShapeBox.setBorder(0.005f).setBorderColor(0, 0, 0, 1));
                infoGrid.addShape(glyphInfoBox.setBorder(0.005f).setBorderColor(0, 0, 0, 1));
                if( VERBOSE_UI ) {
                    infoGrid.validate(reqCaps.getGLProfile());
                    System.err.println("InfoGrid "+infoGrid);
                    System.err.println("InfoGrid "+infoGrid.getLayout());
                    System.err.println("GlyphShapeBox "+glyphShapeBox);
                }
                glyphInfoView.addShape(infoGrid);
            }
            glyphInfoView.setPaddding(new Padding(glyphGridCellSize/6f, 0, 0));
            if( VERBOSE_UI ) {
                glyphInfoView.validate(reqCaps.getGLProfile());
                System.err.println("GlyphInfoGrid "+glyphInfoView);
                System.err.println("GlyphInfoGrid "+glyphInfoView.getLayout());
            }

            mainView = new Group(new GridLayout(1, 0f, 0f, Alignment.None));
            mainView.addShape(glyphInfoView);
            {
                final String infoHelp = "Click on a Glyph for a big tooltip view.\n"+
                                        "Key-Up/Down or Slider-Mouse-Scroll to move through glyphs.\n"+
                                        "Page-Up/Down or Control + Slider-Mouse-Scroll to page faster.\n"+
                                        "Mouse-Scroll over left-half of Window rotates and holding control zooms.";
                infoLabel = new Label(options.renderModes, fontInfo, "Not yet");
                infoLabel.setColor(0.1f, 0.1f, 0.1f, 1f);
                infoLabel.setTooltip(new TooltipText(infoHelp, fontInfo, 8f));

                final float h = glyphGridCellSize * 0.4f;
                final Group labelBox = new Group(new BoxLayout(1.0f, h, new Alignment(Alignment.Bit.Fill.value | Alignment.Bit.CenterVert.value),
                                                               new Margin(0, 0.005f)));
                labelBox.addShape(infoLabel);
                mainView.addShape(labelBox);
            }
            if( VERBOSE_UI ) {
                mainView.validate(reqCaps.getGLProfile());
                System.err.println("MainView "+mainView);
                System.err.println("MainView "+mainView.getLayout());
            }
        }
        scene.addShape(mainView);
        scene.setAAQuality(options.graphAAQuality);
        scene.init(glad);

        firstReshape = true;

        {
            final long t1 = Clock.currentNanos();
            final long total = t1 - t0;
            final float nsPerGlyph = total / gridDim.glyphCount;
            System.err.println("PERF: Total took "+(total/1000000.0)+"ms, per-glyph "+(nsPerGlyph/1000000.0)+"ms, glyphs "+gridDim.glyphCount);
        }
        // printScreenOnGLThread(scene, glad.getChosenGLCapabilities(), font, lastCodepoint);

        // stay open ..
        OutlineShape.printPerf(System.err);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        mainView.destroy(drawable.getGL().getGL2ES2(), scene.getRenderer());
        mainView = null;
        infoLabel = null;

        scene.dispose(drawable);
    }

    @Override
    public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        scene.reshape(glad, x, y, width, height);
        if( firstReshape ) {
            firstReshape = false;
            final AABBox sceneBox = scene.getBounds();
            mainView.validate(glad.getGL().getGL2ES2());
            final AABBox mainViewBox = mainView.getBounds();
            final float sx = sceneBox.getWidth() / mainViewBox.getWidth();
            final float sy = sceneBox.getHeight() / mainViewBox.getHeight();
            final float sxy = Math.min(sx, sy);
            System.err.println("SceneBox "+sceneBox);
            System.err.println("MainViewBox "+mainViewBox);
            System.err.println("scale sx "+sx+", sy "+sy+", sxy "+sxy);
            mainView.scale(sxy, sxy, 1f).moveTo(sceneBox.getLow());
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        infoLabel.setText( scene.getStatusText(drawable, options.renderModes, dpiY) + " (Hover over 1s for help)" );
        scene.display(drawable);
    }

    static void printScreenOnGLThread(final Scene scene, final GLCapabilitiesImmutable caps, final Font font, final int codepoint) {
        final String fn = font.getFullFamilyName().replace(' ', '_').replace('-', '_');
        scene.screenshot(true, scene.nextScreenshotFile(null, FontView01.class.getSimpleName(), options.renderModes, caps, fn+"_cp"+Integer.toHexString(codepoint)));
    }

    static class GridDim {
        final List<Character> contourChars;
        final int glyphCount;
        final int columns;
        final int columnsNet;
        final int rows;
        final int rowsPerPage;
        final int elemCount;
        int complexGlyphCount;
        int maxNameLen;

        public GridDim(final Font font, final int columns, final int rowsPerPage, final int xReserved, final int maxGlyphCount) {
            this.contourChars = new ArrayList<Character>();
            this.glyphCount = Math.min(maxGlyphCount, scanContourGlyphs(font));
            this.columns = columns;
            this.columnsNet = columns - xReserved;
            this.rows = (int)Math.ceil((double)glyphCount / (double)columnsNet);
            this.rowsPerPage = rowsPerPage;
            this.elemCount = glyphCount + ( rows * xReserved );
            this.maxNameLen=10;
        }

        public int reserverColumns() { return columns - columnsNet; }

        private int scanContourGlyphs(final Font font) {
            final long t0 = Clock.currentNanos();
            contourChars.clear();
            complexGlyphCount = 0;
            maxNameLen = 1;
            final int[] max = { max_glyph_count };
            font.forAllGlyphs((final Glyph fg) -> {
                if( !fg.isNonContour() && max[0]-- > 0 ) {
                    contourChars.add( fg.getCodepoint() );
                    if( null != fg.getShape() && fg.getShape().isComplex() ) {
                        ++complexGlyphCount;
                    }
                    maxNameLen = Math.max(maxNameLen, fg.getName().length());
                }
            });
            final long t1 = Clock.currentNanos();
            final long total = t1 - t0;
            final float nsPerGlyph = total / contourChars.size();
            System.err.println("PERF: GlyphScan took "+(total/1000000.0)+"ms, per-glyph "+(nsPerGlyph/1000000.0)+"ms, glyphs "+contourChars.size());
            return contourChars.size();
        }
        @Override
        public String toString() { return "GridDim[contours "+glyphCount+", complex "+complexGlyphCount+" ("+((float)complexGlyphCount/(float)glyphCount)*100+"%), "+columns+"x"+rows+"="+(columns*rows)+">="+elemCount+", rows/pg "+rowsPerPage+"]"; }
    }

    static Group getGlyphShapeHolder(final Shape shape0) {
        if( !( shape0 instanceof Group ) ) {
            return null;
        }
        return (Group)((Group)shape0).getShapeByName("GlyphHolder");
    }
    static GlyphShape getGlyphShape(final Shape shape0) {
        final Group gsh = getGlyphShapeHolder(shape0);
        if( null != gsh && gsh.getShapeCount() > 0 ) {
            return (GlyphShape) gsh.getShapeByIdx(0);
        }
        return null;
    }

    /**
     * Fill given Group sink with glyph shapes wrapped as {@code Group2[Group1[GlyphShape]]},
     * with Group1 having the name 'GlyphHolder'.
     */
    static void addGlyphs(final GLProfile glp, final Font font, final Group sink,
                          final GridDim gridDim, final boolean showUnderline, final boolean showLabel,
                          final Font fontStatus, final Font fontInfo, final Shape.PointerListener hoverHandler) {
        final AABBox tmpBox = new AABBox();
        final long t0 = Clock.currentNanos();

        for(int idx = 0; idx < gridDim.glyphCount; ++idx) {
            final char codepoint = gridDim.contourChars.get(idx);
            final Font.Glyph fg = font.getGlyph(codepoint);
            final boolean isComplex = null != fg.getShape() ? fg.getShape().isComplex() : false;

            final GlyphShape g = new GlyphShape(options.renderModes, fg, 0, 0);
            g.setColor(0.1f, 0.1f, 0.1f, 1).setName("GlyphShape");
            g.setInteractive(false).setDragAndResizable(false);
            g.setName( "cp_0x"+Integer.toHexString(fg.getCodepoint()) );

            final Group c0 = new Group("GlyphHolder", null, null, g);
            c0.setInteractive(false).setDragAndResizable(false);

            // Group each GlyphShape with its bounding box Rectangle
            final AABBox gbox = fg.getBounds(tmpBox); // g.getBounds(glp);
            final boolean addUnderline = showUnderline && gbox.getMinY() < 0f;
            final Group c1 = new Group( new BoxLayout( 1f, 1f, addUnderline ? Alignment.None : Alignment.Center) );
            c1.setBorder(GlyphGridBorderThickness).setBorderColor(isComplex ? GlyphGridBorderColorComplex : GlyphGridBorderColorSimple)
              .setInteractive(true).setDragAndResizable(false).setName("GlyphHolder2");
            if( addUnderline ) {
                final Shape underline = new Rectangle(options.renderModes, 1f, gbox.getMinY(), 0.01f).setInteractive(false).setColor(0f, 0f, 1f, 0.25f);
                c1.addShape(underline);
            }

            c1.addShape( c0 );
            c1.onHover(hoverHandler);
            sink.receiveKeyEvents(c1);
            // sink.receiveMouseEvents(c1);
            c1.setTooltip( new TooltipShape(new Vec4f(1, 1, 1, 1), new Vec4f(0, 0, 0, 1), 0.01f,
                                            new Padding(0.05f), new Vec2f(14,14), 0, options.renderModes,
                                            g, TooltipShape.NoOpDtor) );
            c1.onClicked((final Shape s, final Vec3f pos, final MouseEvent e) -> {
                System.err.println("XXX: Clicked: "+s+", event "+e);
                if( e.getPointerType(0).getPointerClass() == MouseEvent.PointerClass.Onscreen ) {
                    hoverHandler.run(s, pos, e);
                } else {
                    c1.getTooltip().now();
                }
            });

            if( 0 < gridDim.reserverColumns() && 0 == idx % gridDim.columnsNet ) {
                addLabel(sink, fontStatus, String.format("%04x", (int)codepoint));
            }
            if( showLabel ) {
                final Group c2 = new Group( new GridLayout( 1, 0, 0, Alignment.None) ); //  Alignment(Alignment.Bit.CenterHoriz) ) );
                c2.addShape(c1.setName("GlyphHolder3"));
                {
                    final Label l = new Label(options.renderModes, fontInfo, fg.getName());
                    // final AABBox lbox = l.getUnscaledGlyphBounds();
                    final float sxy = 1f/7f; // gridDim.maxNameLen; // 0.10f; // Math.min(sx, sy);
                    c2.addShape( l.scale(sxy, sxy, 1).setColor(0, 0, 0, 1).setInteractive(false).setDragAndResizable(false) );
                }
                sink.addShape(c2);
                // System.err.println("Add.2: "+c2);
            } else {
                sink.addShape(c1);
                // System.err.println("Add.1: "+c1);
            }
        }
        final long t1 = Clock.currentNanos();
        final long total = t1 - t0;
        final float nsPerGlyph = total / gridDim.glyphCount;
        System.err.println("PERF: GlyphAdd took "+(total/1000000.0)+"ms, per-glyph "+(nsPerGlyph/1000000.0)+"ms, glyphs "+gridDim.glyphCount);
    }
    static void addLabel(final Group c, final Font font, final String text) {
        c.addShape( new Label(options.renderModes, font, text).setColor(0, 0, 0, 1).setInteractive(false).setDragAndResizable(false) );
    }

    static void setGlyphInfo(final Font font, final Label label, final Font.Glyph g) {
        label.setText( getGlyphInfo(g) );
        if( VERBOSE_GLYPHS ) {
            System.err.println( label.getText() );
        }
    }

    static String getGlyphInfo(final Font.Glyph g) {
        final OutlineShape os = g.getShape();
        final boolean isComplex = null != os ? os.isComplex() : false;
        final int osVertices = null != os ? os.getVertexCount() : 0;
        final String name_s = null != g.getName() ? g.getName() : "";
        final AABBox bounds = g.getBounds();
        final String box_s = String.format("Box %+.3f/%+.3f%n    %+.3f/%+.3f", bounds.getLow().x(), bounds.getLow().y(), bounds.getHigh().x(), bounds.getHigh().y());
        return String.format((Locale)null, "%s%nHeight: %1.3f%nLine Height: %1.3f%n%nSymbol: %04x, id %04x%nName: '%s'%nDim %1.3f x %1.3f%n%s%nAdvance %1.3f%nLS Bearings: %1.3f%nVertices: %03d%n%s",
                g.getFont().getFullFamilyName(),
                g.getFont().getMetrics().getAscent() - g.getFont().getMetrics().getDescent(), // font hhea table
                g.getFont().getLineHeight(), // font hhea table
                (int)g.getCodepoint(), g.getID(), name_s,
                bounds.getWidth(), bounds.getHeight(), box_s,
                g.getAdvanceWidth(),
                g.getLeftSideBearings(),
                osVertices, isComplex?"Complex Shape":"Simple Shape");
    }
}
