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

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRenderUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public abstract class TextRendererGLELBase implements GLEventListener {
    public final Font font;
    public final int usrRenderModes;

    protected final int[] texSize = new int[] { 0 };
    protected final float[] staticRGBAColor = new float[] { 1f, 1f, 1f, 1f };

    /**
     * In exclusive mode, impl. uses a pixelScale of 1f and orthogonal PMV on window dimensions
     * and renderString uses 'height' for '1'.
     * <p>
     * In non-exclusive mode, i.e. shared w/ custom PMV (within another 3d scene),
     * it uses the custom pixelScale and renderString uses normalized 'height', i.e. '1'.
     * </p>
     */
    protected boolean exclusivePMVMatrix = true;
    protected PMVMatrix usrPMVMatrix = null;
    protected RenderState rs = null;
    protected RegionRenderer renderer = null;
    protected TextRenderUtil textRenderUtil = null;

    /** font size in pixels, default is 24 */
    protected int fontSize = 24;
    /** scale pixel, default is 1f */
    protected float pixelScale = 1.0f;
    protected int texSizeScale = 2;

    boolean flipVerticalInGLOrientation = false;

    public TextRendererGLELBase(final int renderModes) {
        usrRenderModes = renderModes;
        {
            Font _font = null;
            try {
                _font = FontFactory.get(FontFactory.UBUNTU).getDefault();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.font = _font;
        }
    }
    public TextRendererGLELBase(final RenderState rs, final boolean exclusivePMVMatrix, final int renderModes) {
        this(renderModes);
        this.rs = rs;
        this.exclusivePMVMatrix = exclusivePMVMatrix;
    }

    public void setFlipVerticalInGLOrientation(boolean v) { flipVerticalInGLOrientation=v; }
    public final RegionRenderer getRenderer() { return renderer; }
    public final TextRenderUtil getTextRenderUtil() { return textRenderUtil; }

    @Override
    public void init(GLAutoDrawable drawable) {
        if( null != font ) {
            if( null == this.rs ) {
                exclusivePMVMatrix = null == usrPMVMatrix;
                this.rs = RenderState.createRenderState(new ShaderState(), SVertex.factory(), usrPMVMatrix);
            }
            this.renderer = RegionRenderer.create(rs, usrRenderModes);
            this.textRenderUtil = new TextRenderUtil(renderer);
            if( 0 == usrRenderModes ) {
                texSizeScale = 0;
            }
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            renderer.init(gl);
            renderer.setAlpha(gl, staticRGBAColor[3]);
            renderer.setColorStatic(gl, staticRGBAColor[0], staticRGBAColor[1], staticRGBAColor[2]);
            final ShaderState st = rs.getShaderState();
            st.useProgram(gl, false);
        } else {
            this.renderer = null;
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        if( null != renderer ) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            final ShaderState st = rs.getShaderState();
            st.useProgram(gl, true);
            if( exclusivePMVMatrix ) {
                // renderer.reshapePerspective(gl, 45.0f, width, height, 0.1f, 1000.0f);
                renderer.reshapeOrtho(gl, width, height, 0.1f, 1000.0f);
                pixelScale = 1.0f;
            } else {
                renderer.reshapeNotify(gl, width, height);
            }
            st.useProgram(gl, false);
            texSize[0] = width * texSizeScale;
        }
    }

    @Override
    public abstract void display(GLAutoDrawable drawable);

    @Override
    public void dispose(GLAutoDrawable drawable) {
        if( null != renderer ) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            renderer.destroy(gl);
        }
    }

    int lastRow = -1;

    public void renderString(GLAutoDrawable drawable, String text, int column, float tx, float ty, float tz) {
        final int row = lastRow + 1;
        renderString(drawable, text, column, row, tx, ty, tz);
        lastRow = row;
    }

    public void renderString(GLAutoDrawable drawable, String text, int column, int row, float tx, float ty, float tz) {
        if( null != renderer ) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();

            float dx = tx;
            float dy;

            if( !exclusivePMVMatrix )  {
                dy = 1f-ty;
            } else {
                final int height = drawable.getHeight();
                dy = height-ty;
            }

            final AABBox textBox = font.getStringBounds(text, fontSize);
            dx += pixelScale * font.getAdvanceWidth('X', fontSize) * column;
            dy -= pixelScale * (int)textBox.getHeight() * ( row + 1 );

            final ShaderState st = rs.getShaderState();
            final PMVMatrix pmvMatrix = rs.pmvMatrix();
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            if( !exclusivePMVMatrix )  {
                pmvMatrix.glPushMatrix();
            } else {
                pmvMatrix.glLoadIdentity();
            }

            st.useProgram(gl, true);
            gl.glEnable(GL2ES2.GL_BLEND);
            pmvMatrix.glTranslatef(dx, dy, tz);
            if( flipVerticalInGLOrientation && drawable.isGLOriented() ) {
                pmvMatrix.glScalef(pixelScale, -1f*pixelScale, 1f);
            } else if( 1f != pixelScale ) {
                pmvMatrix.glScalef(pixelScale, pixelScale, 1f);
            }
            renderer.updateMatrix(gl);
            textRenderUtil.drawString3D(gl, font, text, fontSize, texSize);
            st.useProgram(gl, false);
            gl.glDisable(GL2ES2.GL_BLEND);

            if( !exclusivePMVMatrix )  {
                pmvMatrix.glPopMatrix();
            }
            lastRow = -1;
        }
    }
}
