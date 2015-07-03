/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

import java.io.IOException;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.Window;
import com.jogamp.opengl.util.PMVMatrix;

public abstract class TextRendererGLELBase implements GLEventListener {
    public final int renderModes;

    protected final int[] vbaaSampleCount;
    protected final float[] staticRGBAColor = new float[] { 1f, 1f, 1f, 1f };

    private boolean exclusivePMVMatrix = true;
    private PMVMatrix sharedPMVMatrix = null;
    private RenderState rs = null;
    private RegionRenderer.GLCallback enableCallback=null, disableCallback=null;
    protected RegionRenderer renderer = null;
    protected TextRegionUtil textRenderUtil = null;

    /** scale pixel, default is 1f */
    protected float pixelScale = 1.0f;

    /** dpi display resolution, queried at {@link #init(GLAutoDrawable)} if NEWT, otherwise 96. */
    protected float dpiH = 96;

    boolean flipVerticalInGLOrientation = false;

    /**
     * @param fontSet e.g. default is {@link FontFactory#UBUNTU}
     * @param fontFamily e.g. default is {@link FontSet#FAMILY_REGULAR}
     * @param fontStylebits e.g. default is {@link FontSet#STYLE_NONE}
     * @return the resulting font.
     */
    public static Font getFont(final int fontSet, final int fontFamily, final int fontStylebits) {
        try {
            return FontFactory.get(fontSet).get(fontFamily, fontStylebits);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param renderModes
     * @param sampleCount desired multisampling sample count for msaa-rendering.
     * @see #setRendererCallbacks(com.jogamp.graph.curve.opengl.RegionRenderer.GLCallback, com.jogamp.graph.curve.opengl.RegionRenderer.GLCallback)
     */
    public TextRendererGLELBase(final int renderModes, final int[] sampleCount) {
        this.renderModes = renderModes;
        this.vbaaSampleCount = sampleCount;
    }

    /**
     * <p>
     * Must be called before {@link #init(GLAutoDrawable)}.
     * </p>
     * @param rs
     */
    public void setRenderState(final RenderState rs) { this.rs = rs; }

    /**
     * In exclusive mode, impl. uses a pixelScale of 1f and orthogonal PMV on window dimensions
     * and renderString uses 'height' for '1'.
     * <p>
     * In non-exclusive mode, i.e. shared w/ custom PMV (within another 3d scene),
     * it uses the custom pixelScale and renderString uses normalized 'height', i.e. '1'.
     * </p>
     * <p>
     * Must be called before {@link #init(GLAutoDrawable)}.
     * </p>
     */
    public void setSharedPMVMatrix(final PMVMatrix pmv) {
        this.sharedPMVMatrix = pmv;
    }

    /**
     * See {@link RegionRenderer#create(RenderState, com.jogamp.graph.curve.opengl.RegionRenderer.GLCallback, com.jogamp.graph.curve.opengl.RegionRenderer.GLCallback)}.
     * <p>
     * Must be called before {@link #init(GLAutoDrawable)}.
     * </p>
     */
    public void setRendererCallbacks(final RegionRenderer.GLCallback enable, final RegionRenderer.GLCallback disable) {
        this.enableCallback = enable;
        this.disableCallback = disable;
    }

    public void setFlipVerticalInGLOrientation(final boolean v) { flipVerticalInGLOrientation=v; }
    public final RegionRenderer getRenderer() { return renderer; }
    public final TextRegionUtil getTextRenderUtil() { return textRenderUtil; }

    @Override
    public void init(final GLAutoDrawable drawable) {
        if( null == this.rs ) {
            exclusivePMVMatrix = null == sharedPMVMatrix;
            this.rs = RenderState.createRenderState(SVertex.factory(), sharedPMVMatrix);
        }
        this.renderer = RegionRenderer.create(rs, enableCallback, disableCallback);
        rs.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
        this.textRenderUtil = new TextRegionUtil(renderModes);
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        renderer.init(gl, renderModes);
        rs.setColorStatic(staticRGBAColor[0], staticRGBAColor[1], staticRGBAColor[2], staticRGBAColor[3]);
        renderer.enable(gl, false);

        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            final float[] pixelsPerMM = ((Window)upObj).getPixelsPerMM(new float[2]);
            dpiH = pixelsPerMM[1]*25.4f;
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        if( null != renderer ) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            renderer.enable(gl, true);
            if( exclusivePMVMatrix ) {
                // renderer.reshapePerspective(gl, 45.0f, width, height, 0.1f, 1000.0f);
                renderer.reshapeOrtho(width, height, 0.1f, 1000.0f);
                pixelScale = 1.0f;
            } else {
                renderer.reshapeNotify(width, height);
            }
            renderer.enable(gl, false);
        }
    }

    @Override
    public abstract void display(GLAutoDrawable drawable);

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        if( null != renderer ) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            renderer.destroy(gl);
        }
    }

    int lastRow = -1;

    /**
     *
     * @param drawable
     * @param font
     * @param pixelSize Use {@link Font#getPixelSize(float, float)} for resolution correct pixel-size.
     * @param text
     * @param column
     * @param tx
     * @param ty
     * @param tz
     * @param cacheRegion
     */
    public void renderString(final GLAutoDrawable drawable,
                             final Font font, final float pixelSize, final String text,
                             final int column, final float tx, final float ty, final float tz, final boolean cacheRegion) {
        final int row = lastRow + 1;
        renderStringImpl(drawable, font, pixelSize, text, column, row, tx, ty, tz, cacheRegion, null);
    }

    public void renderString(final GLAutoDrawable drawable,
                             final Font font, final float pixelSize, final String text,
                             final int column, final float tx, final float ty, final float tz, final GLRegion region) {
        final int row = lastRow + 1;
        renderStringImpl(drawable, font, pixelSize, text, column, row, tx, ty, tz, false, region);
    }

    /**
     *
     * @param drawable
     * @param font
     * @param pixelSize Use {@link Font#getPixelSize(float, float)} for resolution correct pixel-size.
     * @param text
     * @param column
     * @param row
     * @param tx
     * @param ty
     * @param tz
     * @param cacheRegion
     */
    public void renderString(final GLAutoDrawable drawable,
                             final Font font, final float pixelSize, final String text,
                             final int column, final int row,
                             final float tx, final float ty, final float tz, final boolean cacheRegion) {
        renderStringImpl(drawable, font, pixelSize, text, column, row, tx, ty, tz, cacheRegion, null);
    }

    public void renderString(final GLAutoDrawable drawable,
                             final Font font, final float pixelSize, final String text,
                             final int column, final int row,
                             final float tx, final float ty, final float tz, final GLRegion region) {
        renderStringImpl(drawable, font, pixelSize, text, column, row, tx, ty, tz, false, region);
    }

    private void renderStringImpl(final GLAutoDrawable drawable,
                                  final Font font, final float pixelSize, final String text,
                                  final int column, final int row,
                                  final float tx, final float ty, final float tz, final boolean cacheRegion, final GLRegion region) {
        if( null != renderer ) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();

            float dx = tx;
            float dy;

            if( !exclusivePMVMatrix )  {
                dy = 1f-ty;
            } else {
                final int height = drawable.getSurfaceHeight();
                dy = height-ty;
            }
            final int newLineCount = TextRegionUtil.getCharCount(text, '\n');
            final float lineHeight = font.getLineHeight(pixelSize);
            dx += pixelScale * font.getAdvanceWidth('X', pixelSize) * column;
            dy -= pixelScale * lineHeight * ( row + 1 );

            final PMVMatrix pmvMatrix = rs.getMatrix();
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            if( !exclusivePMVMatrix )  {
                pmvMatrix.glPushMatrix();
            } else {
                pmvMatrix.glLoadIdentity();
            }
            pmvMatrix.glTranslatef(dx, dy, tz);
            if( flipVerticalInGLOrientation && drawable.isGLOriented() ) {
                pmvMatrix.glScalef(pixelScale, -1f*pixelScale, 1f);
            } else if( 1f != pixelScale ) {
                pmvMatrix.glScalef(pixelScale, pixelScale, 1f);
            }
            renderer.enable(gl, true);
            if( cacheRegion ) {
                textRenderUtil.drawString3D(gl, renderer, font, pixelSize, text, null, vbaaSampleCount);
            } else if( null != region ) {
                TextRegionUtil.drawString3D(gl, region, renderer, font, pixelSize, text, null, vbaaSampleCount,
                                            textRenderUtil.tempT1, textRenderUtil.tempT2);
            } else {
                TextRegionUtil.drawString3D(gl, renderModes, renderer, font, pixelSize, text, null, vbaaSampleCount,
                                            textRenderUtil.tempT1, textRenderUtil.tempT2);
            }
            renderer.enable(gl, false);

            if( !exclusivePMVMatrix )  {
                pmvMatrix.glPopMatrix();
            }
            lastRow = row + newLineCount;
        }
    }
}
