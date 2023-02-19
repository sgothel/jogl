/**
 * Copyright 2011-2023 JogAmp Community. All rights reserved.
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
import java.util.Locale;

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
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.PMVMatrix;


/**
 * TestTextRendererNEWT00 Variant
 * - Testing GLRegion properties, i.e. overflow bug
 * - No listener, all straight forward
 * - Type Rendering vanilla via TextRegionUtil.addStringToRegion(..)
 *   - GLRegion.addOutlineShape( Font.processString(..) )
 *   - Using a single GLRegion instantiation
 *   - Single GLRegion is filled once with shapes from text
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTextRendererNEWT00 extends UITestCase {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    static long duration = 100; // ms
    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static boolean mainRun = false;
    static boolean useMSAA = true;
    static int win_width = 1280;
    static int win_height = 720;

    static Font font;
    static float fontSize = 24; // in pixel
    private final float[] fg_color = new float[] { 0, 0, 0, 1 };

    @BeforeClass
    public static void setup() throws IOException {
        if( null == font ) {
            font = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_NONE);
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
            } else if(args[i].equals("-width")) {
                i++;
                win_width = atoi(args[i]);
            } else if(args[i].equals("-height")) {
                i++;
                win_height = atoi(args[i]);
            } else if(args[i].equals("-noMSAA")) {
                useMSAA = false;
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-font")) {
                i++;
                font = FontFactory.get(IOUtil.getResource(args[i], TestTextRendererNEWT00.class.getClassLoader(), TestTextRendererNEWT00.class).getInputStream(), true);
            } else if(args[i].equals("-fontSize")) {
                i++;
                fontSize = MiscUtils.atof(args[i], fontSize);
            }
        }
        final String tstname = TestTextRendererNEWT00.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

    static void sleep() {
        try {
            System.err.println("** new frame ** (sleep: "+duration+"ms)");
            Thread.sleep(duration);
        } catch (final InterruptedException ie) {}
    }

    @Test
    public void test02TextRendererVBAA04() throws InterruptedException, GLException, IOException {
        final int renderModes = Region.VBAA_RENDERING_BIT | Region.COLORCHANNEL_RENDERING_BIT;
        final int sampleCount = 4;
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
        System.err.println("Requested: "+caps);
        System.err.println("Requested: "+Region.getRenderModeString(renderModes));

        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createWindow(caps, win_width, win_height, true);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2ES2 gl = winctx.context.getGL().getGL2ES2();

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        System.err.println("Chosen: "+winctx.window.getChosenCapabilities());

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final RegionRenderer renderer = RegionRenderer.create(rs, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        rs.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);

        // init
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        renderer.init(gl, 0);
        rs.setColorStatic(0.1f, 0.1f, 0.1f, 1.0f);
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(false, false);

        // reshape
        gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        // renderer.reshapePerspective(gl, 45.0f, drawable.getWidth(), drawable.getHeight(), 0.1f, 1000.0f);
        renderer.reshapeOrtho(drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), 0.1f, 1000.0f);
        final int z0 = -1000;

        final int[] sampleCountIO = { sampleCount };
        // display
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final GLRegion region = GLRegion.create(renderModes, null);
        final float dx = 0;
        final float dy = drawable.getSurfaceHeight() - 3 * fontSize * font.getLineHeight();
        {
            // all sizes in em
            final float x_width = font.getAdvanceWidth( font.getGlyphID('X') );
            final AffineTransform t = new AffineTransform();

            t.setToTranslation(3*x_width, 0f);
            final AABBox tbox_1 = font.getGlyphBounds(text_1);
            final AABBox rbox_1 = TextRegionUtil.addStringToRegion(region, font, t, text_1, fg_color);
            System.err.println("Text_1: tbox "+tbox_1);
            System.err.println("Text_1: rbox "+rbox_1);

            if( true ) {
                t.setToTranslation(3*x_width, -1f*(rbox_1.getHeight()+font.getLineHeight()));
                final AABBox tbox_2 = font.getGlyphBounds(text_2);
                final AABBox rbox_2 = TextRegionUtil.addStringToRegion(region, font, t, text_2, fg_color);
                System.err.println("Text_1: tbox "+tbox_2);
                System.err.println("Text_1: rbox "+rbox_2);
            }
        }

        final PMVMatrix pmv = renderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        pmv.glTranslatef(dx, dy, z0);
        pmv.glScalef(fontSize, fontSize, 1f);
        region.draw(gl, renderer, sampleCountIO);
        gl.glFinish();
        printScreen(screenshot, renderModes, drawable, gl, false, sampleCount);
        drawable.swapBuffers();

        sleep();

        // dispose
        region.destroy(gl);;
        screenshot.dispose(gl);
        renderer.destroy(gl);

        NEWTGLContext.destroyWindow(winctx);
    }
    public static final String text_1 =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nec sapien tellus. \n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies \n"+
        "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices ultricies nec a elit. \n"+
        "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia pharetra at venenatis est. \n"+
        "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat nec, volutpat nec nisi. \n"+
        "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum id tempus quis, semper \n"+
        "in lorem. Maecenas in ipsum ac justo scelerisque sollicitudin. Quisque sit amet neque lorem, \n" +
        "I “Ask Jeff” or ‘Ask Jeff’. Take the chef d’œuvre! Two of [of] (of) ‘of’ “of” of? of! of*. X\n"+
        "Les Woëvres, the Fôret de Wœvres, the Voire and Vauvise. Yves is in heaven; D’Amboise is in jail. X\n"+
        "Lyford’s in Texas & L’Anse-aux-Griffons in Québec; the Łyna in Poland. Yriarte, Yciar and Ysaÿe are at Yale. X\n"+
        "Kyoto and Ryotsu are both in Japan, Kwikpak on the Yukon delta, Kvæven in Norway, Kyulu in Kenya, not in Rwanda.… X\n"+
        "Von-Vincke-Straße in Münster, Vdovino in Russia, Ytterbium in the periodic table. Are Toussaint L’Ouverture, Wölfflin, Wolfe, X\n"+
        "Miłosz and Wū Wŭ all in the library? 1510–1620, 11:00 pm, and the 1980s are over. X\n"+
        "-------Press H to change text---------";

    public static final String text_1b =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nec sapien tellus. \n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies \n"+
        "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices ultricies nec a elit. \n"+
        "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia pharetra at venenatis est. \n"+
        "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat nec, volutpat nec nisi. \n"+
        "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum id tempus quis, semper \n"+
        "in lorem. Maecenas in ipsum ac justo scelerisque sollicitudin. Quisque sit amet neque lorem, \n" +
        "I “Ask Jeff” or ‘Ask Jeff’. Take the chef d’œuvre! Two of"+
        "abcdefh";
        //     ^
        //     |
        //"abcdefgh";
        //       ^
        //       |

    public static final String text_1o =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nec sapien tellus. \n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies \n"+
        "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices ultricies nec a elit. \n"+
        "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia pharetra at venenatis est. \n"+
        "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat nec, volutpat nec nisi. \n"+
        "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum id tempus quis, semper \n"+
        "in lorem. Maecenas in ipsum ac justo scelerisque sollicitudin. Quisque sit amet neque lorem, \n" +
        "-------Press H to change text---------";

    public static final String text_2 =
        "I “Ask Jeff” or ‘Ask Jeff’. Take the chef d’œuvre! Two of [of] (of) ‘of’ “of” of? of! of*. X\n"+
        "Les Woëvres, the Fôret de Wœvres, the Voire and Vauvise. Yves is in heaven; D’Amboise is in jail. X\n"+
        "Lyford’s in Texas & L’Anse-aux-Griffons in Québec; the Łyna in Poland. Yriarte, Yciar and Ysaÿe are at Yale. X\n"+
        "Kyoto and Ryotsu are both in Japan, Kwikpak on the Yukon delta, Kvæven in Norway, Kyulu in Kenya, not in Rwanda.… X\n"+
        "Von-Vincke-Straße in Münster, Vdovino in Russia, Ytterbium in the periodic table. Are Toussaint L’Ouverture, Wölfflin, Wolfe, X\n"+
        "Miłosz and Wū Wŭ all in the library? 1510–1620, 11:00 pm, and the 1980s are over. X\n"+
        "-------Press H to change text---------";

    public static void printScreen(final GLReadBufferUtil screenshot, final int renderModes, final GLDrawable drawable, final GL gl, final boolean exportAlpha, final int sampleCount) throws GLException, IOException {
        final int screenshot_num = 0;
        final String dir = "./";
        final String objName = "TestTextRendererNEWT00-snap"+screenshot_num;
        // screenshot_num++;
        final String modeS = Region.getRenderModeString(renderModes);
        final String bname = String.format((Locale)null, "%s-msaa%02d-fontsz%02.1f-%03dx%03d-%s%04d", objName,
                drawable.getChosenGLCapabilities().getNumSamples(),
                fontSize, drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), modeS, sampleCount);
        final String filename = dir + bname +".png";
        if(screenshot.readPixels(gl, false)) {
            screenshot.write(new File(filename));
        }
    }

}
