/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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
import java.util.Locale;

import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.curve.OutlineShape;
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
import com.jogamp.graph.ui.shapes.GlyphShape;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.math.Vec2i;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;

/**
 * This may become a little font viewer application, having FontForge as its role model.
 */
public class FontView01 {
    private static float GlyphGridWidth = 2/3f;
    static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.VBAA_RENDERING_BIT);
    // static CommandlineOptions options = new CommandlineOptions(1920, 1080, Region.VBAA_RENDERING_BIT);

    private static boolean VERBOSE_GLYPHS = false;
    private static boolean VERBOSE_UI = false;

    public static void main(final String[] args) throws IOException {
        float mmPerCell = 8f;
        String fontfilename = null;
        final Vec2i gridDim = new Vec2i(-1, -1);
        final boolean[] showUnderline = { false };

        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if(args[idx[0]].equals("-font")) {
                    idx[0]++;
                    fontfilename = args[idx[0]];
                } else if(args[idx[0]].equals("-mmPerCell")) {
                    idx[0]++;
                    mmPerCell = MiscUtils.atof(args[idx[0]], mmPerCell);
                } else if(args[idx[0]].equals("-gridCols")) {
                    idx[0]++;
                    gridDim.setX( MiscUtils.atoi(args[idx[0]], gridDim.x()) );
                } else if(args[idx[0]].equals("-gridRows")) {
                    idx[0]++;
                    gridDim.setY( MiscUtils.atoi(args[idx[0]], gridDim.y()) );
                } else if(args[idx[0]].equals("-showUnderline")) {
                    showUnderline[0] = true;
                }
            }
        }
        System.err.println(options);

        Font font;
        if( null == fontfilename ) {
            font = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",
                                   FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        } else {
            font = FontFactory.get( new File( fontfilename ) );
        }
        System.err.println("Font "+font.getFullFamilyName());
        final Font fontStatus = FontFactory.get(FontFactory.UBUNTU).getDefault();

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
        window.setTitle(FontView01.class.getSimpleName()+": "+font.getFullFamilyName()+", "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
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

        final Scene scene = new Scene();
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
            System.err.println("mmPerCell "+mmPerCell);
        }
        if( 0 >= gridDim.x() ) {
            gridDim.setX( (int)( ( window.getSurfaceWidth() * GlyphGridWidth / ppmm[0] ) / mmPerCell ) );
        }
        if( 0 >= gridDim.y() ) {
            gridDim.setY( (int)( ( window.getSurfaceHeight() / ppmm[1] ) / mmPerCell ) );
        }
        final int cellCount = gridDim.x() * gridDim.y();

        final float gridSize = gridDim.x() > gridDim.y() ? 1f/gridDim.x() : 1f/gridDim.y();
        System.err.println("Grid "+gridDim+", "+cellCount+" cells, gridSize "+gridSize);
        final Group mainGrid = new Group(new GridLayout(gridDim.x(), gridSize, gridSize, Alignment.Fill, new Gap(gridSize*0.10f)));

        final Group glyphCont = new Group();
        {
            glyphCont.addShape(new Rectangle(options.renderModes, 1f, 1f, 0.005f).setInteractive(false));
        }
        final Group infoCont = new Group();
        final Label glyphInfo = new Label(options.renderModes, fontStatus, "Nothing there yet");
        infoCont.addShape(new Rectangle(options.renderModes, 1f, 1f, 0.005f).setInteractive(false));
        {
            setGlyphInfo(fontStatus, glyphInfo, 'A', font.getGlyph(font.getGlyphID('A')));
            glyphInfo.setColor(0.1f, 0.1f, 0.1f, 1.0f);
            infoCont.addShape(glyphInfo);
        }
        final Group infoGrid = new Group(new GridLayout(1/2f, 1/2f, Alignment.Fill, new Gap(1/2f*0.01f), 2));
        infoGrid.addShape(glyphCont);
        infoGrid.addShape(infoCont);

        final Shape.Listener glyphListener = new Shape.Listener() {
            @Override
            public void run(final Shape shape) {
                final Group c = (Group)shape;
                final GlyphShape gs = (GlyphShape)c.getShapes().get(1);
                if( 2 == glyphCont.getShapes().size() ) {
                    glyphCont.removeShape(1);
                }
                glyphCont.addShape(gs);
                setGlyphInfo(fontStatus, glyphInfo, gs.getSymbol(), gs.getGlyph());
                printScreenOnGLThread(scene, window.getChosenGLCapabilities(), font, gs.getGlyph().getID());
            }
        };
        final GlyphSymPos symPos = new GlyphSymPos(gridDim);
        addGlyphs(reqGLP, font, mainGrid, symPos, showUnderline[0], fontStatus, glyphListener);
        window.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseWheelMoved(final MouseEvent e) {
                if( VERBOSE_UI ) {
                    System.err.println("Scroll.0: "+symPos);
                }
                if( e.getRotation()[1] < 0f ) {
                    if( e.isControlDown() ) {
                        symPos.pageUp();
                    } else {
                        symPos.lineUp();
                    }
                } else {
                    if( e.isControlDown() ) {
                        symPos.pageDown();
                    } else {
                        symPos.lineDown();
                    }
                }
                window.invoke(false, new GLRunnable() {
                    @Override
                    public boolean run(final GLAutoDrawable drawable) {
                        mainGrid.removeAllShapes(drawable.getGL().getGL2ES2(), scene.getRenderer());
                        addGlyphs(reqGLP, font, mainGrid, symPos, showUnderline[0], fontStatus, glyphListener);
                        if( VERBOSE_UI ) {
                            System.err.println("Scroll.X: "+symPos);
                        }
                        return true;
                    }
                });
            }
        });

        animator.start();
        scene.waitUntilDisplayed();

        mainGrid.validate(reqGLP); // pre-validate to move & scale before display
        final AABBox sceneBox = scene.getBounds();
        System.err.println("SceneBox "+sceneBox);
        final AABBox mainGridBox = mainGrid.getBounds();
        final float sxy;
        {
            final float sx = sceneBox.getWidth() * GlyphGridWidth / mainGridBox.getWidth();
            final float sy = sceneBox.getHeight() / mainGridBox.getHeight();
            sxy = Math.min(sx, sy);
        }
        mainGrid.scale(sxy, sxy, 1f);
        mainGrid.moveTo(sceneBox.getMinX(), sceneBox.getMinY(), 0f);
        scene.addShape(mainGrid); // late add at correct position and size
        System.err.println("Grid "+mainGrid);
        System.err.println("Grid "+mainGrid.getLayout());
        System.err.println("Grid[0] "+mainGrid.getShapes().get(0));
        final float rightWidth = sceneBox.getWidth() * ( 1f - GlyphGridWidth );
        {
            final float icScale = sceneBox.getHeight();
            infoGrid.validate(reqGLP);
            final AABBox infoGridBox = infoGrid.getBounds();
            final float rightDiffH = ( rightWidth - infoGridBox.getWidth() * icScale ) * 0.5f;
            infoGrid.moveTo(sceneBox.getMaxX() - rightWidth + rightDiffH, sceneBox.getMinY(), 0f);
            infoGrid.setScale(icScale, icScale, 1f);
        }
        scene.addShape(infoGrid);

        printScreenOnGLThread(scene, window.getChosenGLCapabilities(), font, 0);
        // stay open ..
    }

    static void printScreenOnGLThread(final Scene scene, final GLCapabilitiesImmutable caps, final Font font, final int glyphID) {
        final String fn = font.getFullFamilyName().replace(' ', '_').replace('-', '_');
        scene.screenshot(true, scene.nextScreenshotFile(null, FontView01.class.getSimpleName(), options.renderModes, caps, fn+"_gid"+glyphID));
    }

    static class GlyphSymPos {
        final Vec2i gridDim;
        final int glyphsPerRow;
        final int glyphCount;
        int start;
        int nextLine;
        int nextPage;

        public GlyphSymPos(final Vec2i gridDim) {
            this.gridDim = gridDim;
            glyphsPerRow = gridDim.x() - 1;
            glyphCount = glyphsPerRow * gridDim.y();
            start = 0; nextLine = -1; nextPage = -1;
        }

        void pageUp() {
            if( nextPage < Character.MAX_VALUE - glyphCount ) {
                start = nextPage;
            } else {
                start = 0; // Character.MAX_VALUE - glyphCount;
            }
        }
        void pageDown() {
            if( start >= glyphCount ) {
                start -= glyphCount;
            } else {
                start = Character.MAX_VALUE - glyphCount;
            }
        }
        void lineUp() {
            if( nextLine < Character.MAX_VALUE - glyphsPerRow ) {
                start = nextLine;
            } else {
                start = 0; // Character.MAX_VALUE - glyphPerRow;
            }
        }
        void lineDown() {
            if( start >= glyphsPerRow ) {
                start -= glyphsPerRow;
            } else {
                start = Character.MAX_VALUE - glyphsPerRow;
            }
        }

        @Override
        public String toString() { return "SymPos[start "+start+", nextLine "+nextLine+", nextPage "+nextPage+", "+glyphsPerRow+"x"+gridDim.y()+"="+glyphCount+"]"; }
    }
    static void addGlyphs(final GLProfile glp, final Font font, final Group mainGrid,
                          final GlyphSymPos symPos, final boolean showUnderline,
                          final Font fontStatus, final Shape.Listener glyphListener) {
        int glyphID = -1; // startGlyphID;
        symPos.nextLine = -1;
        symPos.nextPage = symPos.start;
        // final int glyphCount = font.getGlyphCount();
        for(int i=0; i<symPos.glyphCount && symPos.nextPage <= Character.MAX_VALUE; ++i) {
            Font.Glyph fg = null;
            while( symPos.nextPage <= Character.MAX_VALUE ) {
                glyphID = font.getGlyphID((char)symPos.nextPage);
                fg = font.getGlyph(glyphID);
                if( VERBOSE_GLYPHS ) {
                    System.err.println("# Sym 0x"+Integer.toHexString(symPos.nextPage)+" -> ID 0x"+Integer.toHexString(glyphID)+": "+fg);
                }
                if( fg.isNonContour() ) {
                    ++symPos.nextPage;
                } else {
                    if( VERBOSE_GLYPHS ) {
                        System.err.println("First meaningful symbol: 0x"+Integer.toHexString(symPos.nextPage)+" -> 0x"+Integer.toHexString(glyphID));
                        System.err.println("# Sym 0x"+Integer.toHexString(symPos.nextPage)+" -> ID 0x"+Integer.toHexString(glyphID)+": "+getGlyphInfo((char)symPos.nextPage, fg));
                    }
                    break;
                }
            }
            if( symPos.nextPage > Character.MAX_VALUE ) {
                break;
            }
            final GlyphShape g = new GlyphShape(options.renderModes, (char)symPos.nextPage, fg, 0, 0);
            if( VERBOSE_GLYPHS ) {
                System.err.println("# Sym 0x"+Integer.toHexString(symPos.nextPage)+" -> ID 0x"+Integer.toHexString(glyphID)+": "+g);
            }
            g.setColor(0.1f, 0.1f, 0.1f, 1);
            g.setDragAndResizeable(false);

            // grid.addShape( g ); // GridLayout handles bottom-left offset and scale
            // Group each GlyphShape with its bounding box Rectangle
            final Group c = new Group();
            c.setInteractive(true).setDragAndResizeable(false);
            c.addShape(new Rectangle(options.renderModes, 1f, 1f, 0.02f).setInteractive(false));
            final AABBox gbox = g.getBounds(glp);
            if( showUnderline && gbox.getMinY() < 0f ) {
                final Shape underline = new Rectangle(options.renderModes, 1f, gbox.getHeight(), 0.02f).setInteractive(false);
                underline.move( 0f, ( 1f - gbox.getHeight() ) / 2f, 0f ); // vert-center
                underline.move( 0f, gbox.getMinY() * -1f, 0f );           // remove glyph underline
                underline.setColor(0f, 0f, 1f, 1f);
                c.addShape(underline);
            }
            g.move( ( 1f - gbox.getWidth() ) / 2f, ( 1f - gbox.getHeight() ) / 2f, 0f ); // center
            g.move( gbox.getLow().mul(-1f) ); // remove bottom-left delta, here glyph underline
            c.addShape(g);
            c.onClicked( glyphListener );
            if( 0 == i % symPos.glyphsPerRow ) {
                addLabel(mainGrid, fontStatus, String.format("%04x", symPos.nextPage));
                if( 0 > symPos.nextLine && i == symPos.glyphsPerRow ) {
                    symPos.nextLine = symPos.nextPage;
                }
            }
            mainGrid.addShape(c);
            ++symPos.nextPage;
        }
    }
    static void addLabel(final Group c, final Font font, final String text) {
        c.addShape( new Label(options.renderModes, font, 1f, text).setColor(0, 0, 0, 1).setInteractive(false).setDragAndResizeable(false) );
    }

    static void setGlyphInfo(final Font font, final Label label, final char symbol, final Font.Glyph g) {
        final String info = getGlyphInfo(symbol, g);
        final AABBox fbox = font.getGlyphBounds(info);
        final float spacing = 0.05f;
        final float slScale = ( 1f - spacing ) / Math.max(fbox.getWidth(), fbox.getHeight());
        if( VERBOSE_GLYPHS ) {
            System.err.println("Scale "+slScale+", "+fbox);
            System.err.println("Info "+g);
            if( g.getShape() != null ) {
                System.err.println("Shape Box "+g.getShape().getBounds());
            }
        }
        label.moveTo(spacing/2f, spacing/2f, 0);
        label.setText(info);
        label.setScale(slScale, slScale, 1f);
        if( VERBOSE_GLYPHS ) {
            System.err.println(info);
        }
    }

    static String getGlyphInfo(final char symbol, final Font.Glyph g) {
        final OutlineShape os = g.getShape();
        final int osVertices = null != os ? os.getVertexCount() : 0;
        final String contour_s;
        if( g.isNonContour() ) {
            final String ws_s = g.isWhitespace() ? "whitespace" : "";
            final String undef_s = g.isUndefined() ? "undefined" : "";
            contour_s = "non-cont("+ws_s+undef_s+")";
        } else {
            contour_s = "contour";
        }
        final String name_s = null != g.getName() ? g.getName() : "";
        return String.format((Locale)null, "%s%nHeight: %1.3f%nLine Height: %1.3f%n%nSymbol: 0x%s, id 0x%s%nName: '%s'%nDim %1.3f x %1.3f%nAdvance %1.3f%nLS Bearings: %1.3f%nVertices: %d, %s%n ",
                g.getFont().getFullFamilyName(),
                g.getFont().getMetrics().getAscent() - g.getFont().getMetrics().getDescent(), // font hhea table
                g.getFont().getLineHeight(), // font hhea table
                Integer.toHexString(symbol), Integer.toHexString(g.getID()), name_s,
                g.getBounds().getWidth(), g.getBounds().getHeight(),
                g.getAdvance(),
                g.getLeftSideBearings(),
                osVertices, contour_s);
    }
}
