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

import java.io.File;
import java.io.IOException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLReadBufferUtil;
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

    static Font font;
    static float fontSize = 24;
    static String customStr = null;

    @BeforeClass
    public static void setup() throws IOException {
        if( null == font ) {
            font = FontFactory.get(FontFactory.UBUNTU).getDefault();
            // font = FontFactory.get(FontFactory.JAVA).getDefault();
        }
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
            } else if(args[i].equals("-font")) {
                i++;
                font = FontFactory.get(IOUtil.getResource(args[i], TestTextRendererNEWT10.class.getClassLoader(), TestTextRendererNEWT10.class).getInputStream(), true);
            } else if(args[i].equals("-fontSize")) {
                i++;
                fontSize = MiscUtils.atof(args[i], fontSize);
            } else if(args[i].equals("-text")) {
                i++;
                customStr = args[i];
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

    @Test
    public void test00TextRendererNONE00() throws InterruptedException, GLException, IOException {
        testTextRendererImpl(0, 0);
    }

    @Test
    public void test01TextRendererMSAA04() throws InterruptedException, GLException, IOException {
        testTextRendererImpl(0, 4);
    }

    @Test
    public void test02TextRendererVBAA04() throws InterruptedException, GLException, IOException {
        testTextRendererImpl(Region.VBAA_RENDERING_BIT, 4);
    }

    void testTextRendererImpl(final int renderModes, final int sampleCount) throws InterruptedException, GLException, IOException {
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
        if( 0 < sampleCount && !Region.isVBAA(renderModes) ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(sampleCount);
        }
        System.err.println("Requested: "+caps);
        System.err.println("Requested: "+Region.getRenderModeString(renderModes));

        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createWindow(caps, 800, 400, true);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2ES2 gl = winctx.context.getGL().getGL2ES2();

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        System.err.println("Chosen: "+winctx.window.getChosenCapabilities());

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final RegionRenderer renderer = RegionRenderer.create(rs, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        rs.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
        final TextRegionUtil textRenderUtil = new TextRegionUtil(renderModes);

        // init
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        renderer.init(gl, 0);
        rs.setColorStatic(0.1f, 0.1f, 0.1f, 1.0f);
        screenshot = new GLReadBufferUtil(false, false);

        // reshape
        gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        // renderer.reshapePerspective(gl, 45.0f, drawable.getWidth(), drawable.getHeight(), 0.1f, 1000.0f);
        renderer.reshapeOrtho(drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), 0.1f, 1000.0f);

        final int[] sampleCountIO = { sampleCount };
        // display
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        if( null == customStr ) {
            renderString(drawable, gl, renderer, textRenderUtil, "012345678901234567890123456789", 0,  0, -1000, sampleCountIO);
            renderString(drawable, gl, renderer, textRenderUtil, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", 0, -1, -1000, sampleCountIO);
            renderString(drawable, gl, renderer, textRenderUtil, "Hello World", 0, -1, -1000, sampleCountIO);
            renderString(drawable, gl, renderer, textRenderUtil, "4567890123456", 4, -1, -1000,sampleCountIO);
            renderString(drawable, gl, renderer, textRenderUtil, "I like JogAmp", 4, -1, -1000, sampleCountIO);

            int c = 0;
            renderString(drawable, gl, renderer, textRenderUtil, "GlueGen", c++, -1, -1000, sampleCountIO);
            renderString(drawable, gl, renderer, textRenderUtil, "JOAL", c++, -1, -1000, sampleCountIO);
            renderString(drawable, gl, renderer, textRenderUtil, "JOGL", c++, -1, -1000, sampleCountIO);
            renderString(drawable, gl, renderer, textRenderUtil, "JOCL", c++, -1, -1000, sampleCountIO);
        } else {
            renderString(drawable, gl, renderer, textRenderUtil, customStr, 0,  0, -1000, sampleCountIO);
        }
        drawable.swapBuffers();
        printScreen(renderModes, drawable, gl, false, sampleCount);

        sleep();

        // dispose
        screenshot.dispose(gl);
        renderer.destroy(gl);

        NEWTGLContext.destroyWindow(winctx);
    }

    private GLReadBufferUtil screenshot;
    int lastRow = -1;

    void renderString(final GLDrawable drawable, final GL2ES2 gl, final RegionRenderer renderer, final TextRegionUtil textRenderUtil, final String text, final int column, int row, final int z0, final int[] sampleCount) {
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
        textRenderUtil.drawString3D(gl, renderer, font, fontSize, text, null, sampleCount);

        lastRow = row;
    }

    private int screenshot_num = 0;

    public void printScreen(final int renderModes, final GLDrawable drawable, final GL gl, final boolean exportAlpha, final int sampleCount) throws GLException, IOException {
        final String dir = "./";
        final String objName = getSimpleTestName(".")+"-snap"+screenshot_num;
        screenshot_num++;
        final String modeS = Region.getRenderModeString(renderModes);
        final String bname = String.format("%s-msaa%02d-fontsz%02.1f-%03dx%03d-%s%04d", objName,
                drawable.getChosenGLCapabilities().getNumSamples(),
                TestTextRendererNEWT10.fontSize, drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), modeS, sampleCount);
        final String filename = dir + bname +".png";
        if(screenshot.readPixels(gl, false)) {
            screenshot.write(new File(filename));
        }
    }

}
