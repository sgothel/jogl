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
package com.jogamp.opengl.test.junit.graph;

import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontScale;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.Gap;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.shapes.GlyphShape;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.geom.AABBox;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

/**
 * Glyph Grid using GraphUI
 */
public class FontViewListener01 implements GLEventListener {
    private float mmPerCell = 8.0f;
    private int pixelPerCell = 30; // 1280 x 720 pixel @ 94.268 dpi, 3.7113402 pixel/mm, 8mm -> 29.69 pixel
    private boolean useDPI = false;

    private final int renderModes;
    private final char startCharSymbol;
    private final Font font;
    private final Scene scene;
    private Group grid;

    public FontViewListener01(final int renderModes, final int graphSampleCount, final Font font, final char startCharSymbol) {
        this.renderModes = renderModes;
        this.startCharSymbol = startCharSymbol;
        this.font = font;

        scene = new Scene(graphSampleCount);
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setFrustumCullingEnabled(true);
    }

    public void setMMPerCell(final float mmPerCell) {
        this.mmPerCell = mmPerCell;
        this.useDPI = true;
    }
    public void setPixelPerCell(final int pixelPerCell) {
        this.pixelPerCell = pixelPerCell;
        this.useDPI = false;
    }

    public void attachInputListenerTo(final GLWindow window) {
        scene.attachInputListenerTo(window);
    }

    public void printScreenOnGLThread(final GLAutoDrawable drawable, final String dir, final String prefix, final String objName, final boolean exportAlpha) {
        final String fn = font.getFullFamilyName().replace(' ', '_').replace('-', '_');
        scene.screenshot(true, scene.nextScreenshotFile(null, prefix, renderModes, drawable.getChosenGLCapabilities(), fn));
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        scene.init(drawable);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        scene.dispose(drawable);
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        scene.display(drawable);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        scene.reshape(drawable, x, y, width, height);
        System.err.println("Reshape "+width+" x "+height+", "+scene.getViewport());

        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if( null != grid ) {
            scene.removeShape(gl, grid);
        }
        final int gridCols, gridRows;
        if( useDPI && drawable instanceof GLWindow ) {
            final GLWindow window = (GLWindow)drawable;
            final float[] ppmm = window.getPixelsPerMM(new float[2]);
            {
                final float[] dpi = FontScale.ppmmToPPI( new float[] { ppmm[0], ppmm[1] } );
                System.err.println("DPI "+dpi[0]+" x "+dpi[1]+", "+ppmm[0]+" x "+ppmm[1]+" pixel/mm");

                final float[] hasSurfacePixelScale1 = window.getCurrentSurfaceScale(new float[2]);
                System.err.println("HiDPI PixelScale: "+hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
                System.err.println("mmPerCell "+mmPerCell);
            }
            gridCols = (int)( 0.90f * ( width / ppmm[0] ) / mmPerCell );
            gridRows = (int)( 0.90f * ( height / ppmm[1] ) / mmPerCell );
        } else {
            gridCols = (int)( 0.90f * width / pixelPerCell );
            gridRows = (int)( 0.90f * height / pixelPerCell );
        }
        final int cellCount = gridCols * gridRows;
        final float netGridSize = gridCols > gridRows ? 1f/gridCols : 1f/gridRows;
        System.err.println("Reshape Grid "+gridCols+" x "+gridRows+", "+cellCount+" cells, netGridSize "+netGridSize);

        grid = new Group(new GridLayout(gridCols, netGridSize, netGridSize, Alignment.FillCenter, new Gap(netGridSize/0.90f*0.10f)));
        scene.addShape(grid);

        for(int idx=0; idx<Character.MAX_VALUE && grid.getShapeCount() < cellCount ; ++idx) {
            final char codepoint = (char)(startCharSymbol+idx);
            final Font.Glyph glyph = font.getGlyph( codepoint );
            if( glyph.isNonContour() ) {
                continue;
            }
            final GlyphShape glyphShape = new GlyphShape(renderModes, glyph, 0, 0);
            glyphShape.setColor(0.1f, 0.1f, 0.1f, 1);
            glyphShape.setDragAndResizeable(false);
            glyphShape.onClicked( new Shape.Listener() {
                @Override
                public void run(final Shape shape) {
                    System.err.println( ((GlyphShape)shape).getGlyph().toString() );
                }
            });
            glyphShape.validate(gl);

            // Group each GlyphShape with a border
            final Group c = new Group( new BoxLayout( 1f, 1f, Alignment.Center) );
            c.setBorder(0.02f).setBorderColor(0, 0, 0, 1).setInteractive(false);
            final AABBox gbox = glyphShape.getBounds();
            glyphShape.move( ( 1f - gbox.getWidth() ) / 2f, ( 1f - gbox.getHeight() ) / 2f, 0f ); // center
            glyphShape.move( gbox.getLow().mul(-1f) ); // remove bottom-left delta, here glyph underline
            c.addShape(glyphShape);
            grid.addShape(c);
        }

        grid.validate(gl);
        final AABBox sceneBox = scene.getBounds();
        final AABBox gridBox = grid.getBounds();
        final float sx = sceneBox.getWidth() / gridBox.getWidth();
        final float sy = sceneBox.getHeight() / gridBox.getHeight();
        final float sxy = Math.min(sx, sy);
        grid.scale(sxy, sxy, 1f).moveTo(sceneBox.getLow());
        System.err.println("SceneBox "+sceneBox);
        System.err.println("GridBox "+gridBox);
        System.err.println("scale sx "+sx+", sy "+sy+", sxy "+sxy);
        System.err.println("Grid "+grid);
        System.err.println("Grid "+grid.getLayout());
        System.err.println("Grid[0] "+grid.getShapes().get(0));
    }
}
