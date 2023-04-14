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
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.GlyphShape;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;

/**
 * This may become a little font viewer application, having FontForge as its role model.
 */
public class FontView01 {
    private static float gridWidth = 2/3f;
    static GraphUIDemoArgs options = new GraphUIDemoArgs(1280, 720, Region.VBAA_RENDERING_BIT);

    public static void main(final String[] args) throws IOException {
        float mmPerCell = 10f;
        String fontfilename = null;
        int gridCols = -1;
        int gridRows = -1;
        final int startGlyphID = 0;
        boolean showUnderline = false;

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
                    gridCols = MiscUtils.atoi(args[idx[0]], gridCols);
                } else if(args[idx[0]].equals("-gridRows")) {
                    idx[0]++;
                    gridRows = MiscUtils.atoi(args[idx[0]], gridRows);
                } else if(args[idx[0]].equals("-showUnderline")) {
                    showUnderline = true;
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
        final Font fontStatus = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMono.ttf", FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);

        final GLProfile reqGLP = GLProfile.get(options.glProfileName);
        System.err.println("GLProfile: "+reqGLP);

        final GLCapabilities caps = new GLCapabilities(reqGLP);
        caps.setAlphaBits(4);
        if( options.sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(options.sceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(1*60, null); // System.err);
        final GLWindow window = GLWindow.create(caps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(FontView01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
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
        if( 0 >= gridCols ) {
            gridCols = (int)( ( window.getSurfaceWidth() * gridWidth / ppmm[0] ) / mmPerCell );
        }
        if( 0 >= gridRows ) {
            gridRows = (int)( ( window.getSurfaceHeight() / ppmm[1] ) / mmPerCell );
        }
        final int cellCount = gridCols * gridRows;
        final float gridSize = gridCols > gridRows ? 1f/gridCols : 1f/gridRows;
        System.err.println("Grid "+gridCols+" x "+gridRows+", "+cellCount+" cells, gridSize "+gridSize);
        final Group mainGrid = new Group(new GridLayout(gridCols, gridSize, gridSize, new Padding(gridSize*0.1f, gridSize*0.1f)));

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
        final Group infoGrid = new Group(new GridLayout(1/2f, 1/2f, new Padding(1/2f*0.01f, 1/2f*0.01f), 2));
        infoGrid.addShape(glyphCont);
        infoGrid.addShape(infoCont);

        char glyphSymbol = 0;
        int glyphID; // startGlyphID;
        // final int glyphCount = font.getGlyphCount();
        for(int i=0; i<cellCount; ++i) {
            Font.Glyph fg;
            while( true ) {
                glyphID = font.getGlyphID(glyphSymbol);
                fg = font.getGlyph(glyphID);
                System.err.println("# 0x"+Integer.toHexString(glyphSymbol)+" -> 0x"+Integer.toHexString(glyphID)+": "+fg);
                System.err.println("# 0x"+Integer.toHexString(glyphSymbol)+" -> 0x"+Integer.toHexString(glyphID)+": "+getGlyphInfo(glyphSymbol, fg));
                if( !fg.isWhiteSpace() && !fg.isUndefined() ) {
                    break;
                } else {
                    ++glyphSymbol;
                }
            }
            final GlyphShape g = new GlyphShape(options.renderModes, glyphSymbol, fg, 0, 0);
            System.err.println("# 0x"+Integer.toHexString(glyphSymbol)+" -> 0x"+Integer.toHexString(glyphID)+": "+g);
            ++glyphSymbol;
            g.setColor(0.1f, 0.1f, 0.1f, 1);
            g.setDragAndResizeable(false);

            // grid.addShape( g ); // GridLayout handles bottom-left offset and scale
            // Group each GlyphShape with its bounding box Rectangle
            final Group c = new Group();
            c.setInteractive(true).setDragAndResizeable(false);
            c.addShape(new Rectangle(options.renderModes, 1f, 1f, 0.02f).setInteractive(false));
            final AABBox gbox = g.getBounds(reqGLP);
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
            c.onClicked( new Shape.Listener() {
                @Override
                public void run(final Shape shape) {
                    final Group c = (Group)shape;
                    final GlyphShape gs = (GlyphShape)c.getShapes().get(1);
                    if( 2 == glyphCont.getShapes().size() ) {
                        glyphCont.removeShape(1);
                    }
                    glyphCont.addShape(gs);
                    setGlyphInfo(fontStatus, glyphInfo, gs.getSymbol(), gs.getGlyph());
                    // glyphCont.markShapeDirty();
                    // grid.markShapeDirty();
                    System.err.println();
                    printScreenOnGLThread(scene, window.getChosenGLCapabilities(), font, gs.getGlyph().getID());
                }
            });
            mainGrid.addShape(c);
        }

        animator.start();
        scene.waitUntilDisplayed();

        mainGrid.validate(reqGLP); // pre-validate to move & scale before display
        final AABBox sceneBox = scene.getBounds();
        System.err.println("SceneBox "+sceneBox);
        final AABBox mainGridBox = mainGrid.getBounds();
        final float sgxy;
        if( mainGridBox.getWidth() > mainGridBox.getHeight() ) {
            sgxy = sceneBox.getWidth() * gridWidth / mainGridBox.getWidth();
        } else {
            sgxy = sceneBox.getHeight() / mainGridBox.getHeight();
        }
        mainGrid.scale(sgxy, sgxy, 1f);
        mainGrid.moveTo(sceneBox.getMinX(), sceneBox.getMinY(), 0f);
        scene.addShape(mainGrid); // late add at correct position and size
        System.err.println("Grid "+mainGrid);
        System.err.println("Grid "+mainGrid.getLayout());
        System.err.println("Grid[0] "+mainGrid.getShapes().get(0));
        final float rightWidth = sceneBox.getWidth() * ( 1f - gridWidth );
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

    static void setGlyphInfo(final Font font, final Label label, final char symbol, final Font.Glyph g) {
        final String info = getGlyphInfo(symbol, g);
        final AABBox fbox = font.getGlyphBounds(info);
        final float slScale = 1f / fbox.getWidth();
        System.err.println("Scale "+slScale+", "+fbox);
        System.err.println("Info "+g);
        label.setText(info);
        label.setScale(slScale, slScale, 1f);
        System.err.println(info);
    }

    static String getGlyphInfo(final char symbol, final Font.Glyph g) {
        final OutlineShape os = g.getShape();
        final int osVertices = null != os ? os.getOutlineVectexCount() : 0;
        final String ws = g.isWhiteSpace() ? ", ws" : "";
        return String.format((Locale)null, "%s%nHeight: %1.3f%nLine Height: %1.3f%n%nSymbol: 0x%s, id 0x%s%s%nName: %s%nDim %1.3f x %1.3f%nAdvance %1.3f%nLS Bearings: %1.3f%nVertices: %d%n ",
                g.getFont().getFullFamilyName(),
                g.getFont().getMetrics().getAscent() - g.getFont().getMetrics().getDescent(), // font hhea table
                g.getFont().getLineHeight(), // font hhea table
                Integer.toHexString(symbol), Integer.toHexString(g.getID()), ws, g.getName(),
                g.getBounds().getWidth(), g.getBounds().getHeight(),
                g.getAdvance(),
                g.getLeftSideBearings(),
                osVertices );
    }
}
