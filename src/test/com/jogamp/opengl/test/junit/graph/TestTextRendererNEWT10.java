/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.PMVMatrix;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTextRendererNEWT10 extends UITestCase {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    static long duration = 100; // ms
    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static boolean mainRun = false;
    static boolean useMSAA = true;

    static final int[] texSize = new int[] { 0 };
    static final int fontSize = 24;
    static Font font;

    @BeforeClass
    public static void setup() throws IOException {
        font = FontFactory.get(FontFactory.UBUNTU).getDefault();
    }

    static int atoi(final String a) {
        try {
            return Integer.parseInt(a);
        } catch (final Exception ex) { throw new RuntimeException(ex); }
    }

    public static void main(final String args[]) throws IOException {
        mainRun = true;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = atoi(args[i]);
            } else if(args[i].equals("-noMSAA")) {
                useMSAA = false;
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            }
        }
        final String tstname = TestTextRendererNEWT10.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

    static void sleep() {
        try {
            System.err.println("** new frame ** (sleep: "+duration+"ms)");
            Thread.sleep(duration);
        } catch (final InterruptedException ie) {}
    }

    // @Test
    public void test00TextRendererNONE01() throws InterruptedException {
        testTextRendererImpl(0);
    }

    @Test
    public void testTextRendererMSAA01() throws InterruptedException {
        testTextRendererImpl(4);
    }

    void testTextRendererImpl(final int sampleCount) throws InterruptedException {
        final GLProfile glp;
        if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else {
            glp = GLProfile.getGL2ES2();
        }
        final GLCapabilities caps = new GLCapabilities( glp );
        caps.setAlphaBits(4);
        if( 0 < sampleCount ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(sampleCount);
        }
        System.err.println("Requested: "+caps);

        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createOnscreenWindow(caps, 800, 400, true);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2ES2 gl = winctx.context.getGL().getGL2ES2();

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        System.err.println("Chosen: "+winctx.window.getChosenCapabilities());

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final RegionRenderer renderer = RegionRenderer.create(rs, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        rs.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
        final TextRegionUtil textRenderUtil = new TextRegionUtil(0);

        // init
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        renderer.init(gl, 0);
        rs.setColorStatic(0.1f, 0.1f, 0.1f, 1.0f);

        // reshape
        gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        // renderer.reshapePerspective(gl, 45.0f, drawable.getWidth(), drawable.getHeight(), 0.1f, 1000.0f);
        renderer.reshapeOrtho(drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), 0.1f, 1000.0f);

        // display
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        renderString(drawable, gl, renderer, textRenderUtil, "012345678901234567890123456789", 0,  0, -1000);
        renderString(drawable, gl, renderer, textRenderUtil, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", 0, -1, -1000);
        renderString(drawable, gl, renderer, textRenderUtil, "Hello World", 0, -1, -1000);
        renderString(drawable, gl, renderer, textRenderUtil, "4567890123456", 4, -1, -1000);
        renderString(drawable, gl, renderer, textRenderUtil, "I like JogAmp", 4, -1, -1000);

        int c = 0;
        renderString(drawable, gl, renderer, textRenderUtil, "GlueGen", c++, -1, -1000);
        renderString(drawable, gl, renderer, textRenderUtil, "JOAL", c++, -1, -1000);
        renderString(drawable, gl, renderer, textRenderUtil, "JOGL", c++, -1, -1000);
        renderString(drawable, gl, renderer, textRenderUtil, "JOCL", c++, -1, -1000);

        drawable.swapBuffers();
        sleep();

        // dispose
        renderer.destroy(gl);

        NEWTGLContext.destroyWindow(winctx);
    }

    int lastRow = -1;

    void renderString(final GLDrawable drawable, final GL2ES2 gl, final RegionRenderer renderer, final TextRegionUtil textRenderUtil, final String text, final int column, int row, final int z0) {
        final int height = drawable.getSurfaceHeight();

        int dx = 0;
        int dy = height;
        if(0>row) {
            row = lastRow + 1;
        }
        final AABBox textBox = font.getMetricBounds(text, fontSize);
        dx += font.getAdvanceWidth('X', fontSize) * column;
        dy -= (int)textBox.getHeight() * ( row + 1 );

        final PMVMatrix pmv = renderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        pmv.glTranslatef(dx, dy, z0);
        textRenderUtil.drawString3D(gl, renderer, font, fontSize, text, null, texSize);

        lastRow = row;
    }
}
