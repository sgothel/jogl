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
package com.jogamp.opengl.test.junit.jogl.acore;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * Multiple GLJPanels in a JFrame
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class GLReadBuffer00Base extends UITestCase {

    public static abstract class TextRendererGLELBase implements GLEventListener {
        final float[] textPosition = new float[] {0,0,0};
        final int[] texSize = new int[] { 0 };
        final int fontSize = 24;

        final Font font;
        final RenderState rs;
        final TextRenderer renderer;

        boolean flipVerticalInGLOrientation = false;

        public TextRendererGLELBase() {
            {
                Font _font = null;
                try {
                    _font = FontFactory.get(FontFactory.UBUNTU).getDefault();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.font = _font;
            }
            if( null != font ) {
                this.rs = RenderState.createRenderState(new ShaderState(), SVertex.factory());
                this.renderer = TextRenderer.create(rs, Region.VBAA_RENDERING_BIT);
            } else {
                this.rs = null;
                this.renderer = null;
            }
        }

        public void setFlipVerticalInGLOrientation(boolean v) { flipVerticalInGLOrientation=v; }
        public final TextRenderer getRenderer() { return renderer; }

        public void init(GLAutoDrawable drawable) {
            if( null != renderer ) {
                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                renderer.init(gl);
                renderer.setAlpha(gl, 0.99f);
                renderer.setColorStatic(gl, 1.0f, 1.0f, 1.0f);
                final ShaderState st = rs.getShaderState();
                st.useProgram(gl, false);
            }
        }

        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            if( null != renderer ) {
                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                final ShaderState st = rs.getShaderState();
                st.useProgram(gl, true);
                // renderer.reshapePerspective(gl, 45.0f, width, height, 0.1f, 1000.0f);
                renderer.reshapeOrtho(gl, width, height, 0.1f, 1000.0f);
                st.useProgram(gl, false);
                texSize[0] = width * 2;
            }
        }

        public abstract void display(GLAutoDrawable drawable);

        public void dispose(GLAutoDrawable drawable) {
            if( null != renderer ) {
                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                renderer.destroy(gl);
            }
        }

        int lastRow = -1;

        public void renderString(GLAutoDrawable drawable, String text, int column, int row, int z0) {
            if( null != renderer ) {
                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                final int height = drawable.getHeight();

                int dx = 0;
                int dy = height;
                if(0>row) {
                    row = lastRow + 1;
                }
                AABBox textBox = font.getStringBounds(text, fontSize);
                dx += font.getAdvanceWidth('X', fontSize) * column;
                dy -= (int)textBox.getHeight() * ( row + 1 );

                final ShaderState st = rs.getShaderState();
                st.useProgram(gl, true);
                gl.glEnable(GL2ES2.GL_BLEND);
                renderer.resetModelview(null);
                renderer.translate(gl, dx, dy, z0);
                if( flipVerticalInGLOrientation && drawable.isGLOriented() ) {
                    renderer.scale(gl, 1f, -1f, 1f);
                }
                renderer.drawString3D(gl, font, text, textPosition, fontSize, texSize);
                st.useProgram(gl, false);
                gl.glDisable(GL2ES2.GL_BLEND);

                lastRow = row;
            }
        }
    }
    public static class TextRendererGLEL extends TextRendererGLELBase {
        int frameNo = 0;

        public TextRendererGLEL() {
            super();
        }

        @Override
        public void display(GLAutoDrawable drawable) {
            frameNo++;
            final String text = String.format("Frame %04d: %04dx%04d", frameNo, drawable.getWidth(), drawable.getHeight());
            if( null != renderer ) {
                renderString(drawable, text, 0,  0, -1);
            } else {
                System.err.println(text);
            }
        }
    }

    @BeforeClass
    public static void initClass() throws IOException {
        GLProfile.initSingleton();
    }

    protected abstract void test(final GLCapabilitiesImmutable caps, final boolean useSwingDoubleBuffer, final boolean skipGLOrientationVerticalFlip);

    @Test
    public void test00_MSAA0_DefFlip() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useSwingDoubleBuffer*/, false /* skipGLOrientationVerticalFlip */);
    }

    @Test
    public void test01_MSAA0_UsrFlip() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useSwingDoubleBuffer*/, true /* skipGLOrientationVerticalFlip */);
    }

    @Test
    public void test10_MSAA8_DefFlip() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(null);
        caps.setNumSamples(8);
        caps.setSampleBuffers(true);
        test(caps, false /*useSwingDoubleBuffer*/, false /* skipGLOrientationVerticalFlip */);
    }

    @Test
    public void test11_MSAA8_UsrFlip() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(null);
        caps.setNumSamples(8);
        caps.setSampleBuffers(true);
        test(caps, false /*useSwingDoubleBuffer*/, true /* skipGLOrientationVerticalFlip */);
    }

    static long duration = 500; // ms
}
