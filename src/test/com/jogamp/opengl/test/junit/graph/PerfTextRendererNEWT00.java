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
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.common.os.Clock;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NEWTGLContext;
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
public class PerfTextRendererNEWT00 {
    static final Instant t0i = Clock.getMonotonicTime();
    static final long t0 = Clock.currentNanos();
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    static long duration = 100; // ms
    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static int win_width = 1280;
    static int win_height = 720;
    static int loop_count = 1;
    static boolean do_perf = false;
    static boolean do_snap = false;
    static boolean do_vsync = false;

    static Font font;
    static float fontSize = 20; // in pixel
    private final float[] fg_color = new float[] { 0, 0, 0, 1 };

    static {
        try {
            font = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSans.ttf",
                                   FontSet01.class.getClassLoader(), FontSet01.class).getInputStream(), true);
            // font = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_NONE);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(final String args[]) throws IOException, GLException, InterruptedException {
        String text = PerfTextRendererNEWT00.text_1;
        boolean wait = false;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-width")) {
                i++;
                win_width = MiscUtils.atoi(args[i], win_width);
            } else if(args[i].equals("-height")) {
                i++;
                win_height = MiscUtils.atoi(args[i], win_height);
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-font")) {
                i++;
                font = FontFactory.get(IOUtil.getResource(args[i], PerfTextRendererNEWT00.class.getClassLoader(), PerfTextRendererNEWT00.class).getInputStream(), true);
            } else if(args[i].equals("-fontSize")) {
                i++;
                fontSize = MiscUtils.atof(args[i], fontSize);
            } else if(args[i].equals("-wait")) {
                wait = true;
            } else if(args[i].equals("-loop")) {
                i++;
                loop_count = MiscUtils.atoi(args[i], loop_count);
                if( 0 >= loop_count ) {
                    loop_count = Integer.MAX_VALUE;
                }
            } else if(args[i].equals("-long_text")) {
                text = PerfTextRendererNEWT00.text_long;
            } else if(args[i].equals("-vsync")) {
                do_vsync = true;
            } else if(args[i].equals("-perf")) {
                do_perf = true;
            } else if(args[i].equals("-snap")) {
                do_snap = true;
            }
        }
        System.err.println("Excessuive performance test enabled: "+do_perf);
        System.err.println("VSync requested: "+do_vsync);
        if( wait ) {
            MiscUtils.waitForKey("Start");
        }
        final int renderModes = Region.VBAA_RENDERING_BIT /* | Region.COLORCHANNEL_RENDERING_BIT */;
        final int sampleCount = 4;

        final PerfTextRendererNEWT00 obj = new PerfTextRendererNEWT00();
        obj.test(renderModes, sampleCount, text);
    }

    static void sleep() {
        try {
            System.err.println("** new frame ** (sleep: "+duration+"ms)");
            Thread.sleep(duration);
        } catch (final InterruptedException ie) {}
    }

    static class Perf {
        // all td_ values are in [ns]
        public long td_graph = 0;
        public long td_txt = 0;
        public long td_draw = 0;
        public long td_txt_draw = 0;
        public long count = 0;

        public Perf() {
            final Instant startupMTime = Clock.getMonotonicStartupTime();
            final Instant currentMTime = Clock.getMonotonicTime();
            final Instant wallTime = Clock.getWallClockTime();
            final Duration elapsedSinceStartup = Duration.between(startupMTime, currentMTime);
            System.err.printf("Perf: Elapsed since startup: %,d [ms], %,d [ns]%n", elapsedSinceStartup.toMillis(), elapsedSinceStartup.toNanos());
            System.err.printf("- monotonic startup %s, %,d [ms]%n", startupMTime, startupMTime.toEpochMilli());
            System.err.printf("- monotonic current %s, %,d [ms]%n", currentMTime, currentMTime.toEpochMilli());
            System.err.printf("-      wall current %s%n", wallTime);
            final long td = Clock.currentNanos();
            System.err.printf("- currentNanos: Elapsed %,d [ns]%n", (td-t0));
            System.err.printf("  - test-startup %,13d [ns]%n", t0);
            System.err.printf("  - test-current %,13d [ns]%n", td);
        }

        public void clear() {
            td_graph = 0;
            td_txt = 0;
            td_draw = 0;
            td_txt_draw = 0;
            count = 0;
        }

        public void print(final PrintStream out, final long frame, final String msg) {
            out.printf("%3d / %3d: Perf %s:   Total: graph %,2d, txt %,2d, draw %,2d, txt+draw %,2d [ms]%n",
                    count, frame, msg,
                    TimeUnit.NANOSECONDS.toMillis(td_graph),
                    TimeUnit.NANOSECONDS.toMillis(td_txt),
                    TimeUnit.NANOSECONDS.toMillis(td_draw), TimeUnit.NANOSECONDS.toMillis(td_txt_draw));
            out.printf("%3d / %3d: Perf %s: PerLoop: graph %,4d, txt %,4d, draw %,4d, txt+draw %,4d [ns]%n",
                    count, frame, msg,
                    td_graph/count, td_txt/count, td_draw/count, td_txt_draw/count );
        }
    }

    public void test(final int renderModes, final int sampleCount, final String text) throws InterruptedException, GLException, IOException {
        final GLProfile glp;
        if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else {
            glp = GLProfile.getGL2ES2();
        }

        final Instant t1i = Clock.getMonotonicTime();
        final long t1 = Clock.currentNanos();

        final GLCapabilities caps = new GLCapabilities( glp );
        caps.setAlphaBits(4);

        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createWindow(caps, win_width, win_height, false); // true);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2ES2 gl = winctx.context.getGL().getGL2ES2();
        if( !do_vsync ) {
            gl.setSwapInterval(0);
        }
        {
            final int glerr = gl.glGetError();
            if( GL.GL_NO_ERROR != glerr ) {
                System.err.println("Initial GL Error: "+glerr);
            }
        }
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(JoglVersion.getInstance().toString(gl));
        System.err.println("VSync Swap Interval: "+gl.getSwapInterval());
        System.err.println("GLDrawable surface size: "+winctx.context.getGLDrawable().getSurfaceWidth()+" x "+winctx.context.getGLDrawable().getSurfaceHeight());

        System.err.println("Requested Caps: "+caps);
        System.err.println("Requested Region-RenderModes: "+Region.getRenderModeString(renderModes));
        System.err.println("Chosen Caps: "+winctx.window.getChosenCapabilities());
        System.err.println("Chosen Font: "+font.getFullFamilyName());

        final GLReadBufferUtil screenshot = new GLReadBufferUtil(false, false);

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final RegionRenderer renderer = RegionRenderer.create(rs, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        rs.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);

        // Since we know about the size ...
        // final GLRegion region = GLRegion.create(gl.getGLProfile(), renderModes, null);
        // region.growBufferSize(123000, 62000); // hack-me
        // FreeSans     ~ vertices  68/char, indices 36/char
        // Ubuntu Light ~ vertices 100/char, indices 50/char
        // FreeSerif    ~ vertices 115/char, indices 61/char
        // final int vertices_per_char = 68; // 100;
        // final int indices_per_char = 36; // 50;
        // final GLRegion region = GLRegion.create(gl.getGLProfile(), renderModes, null, text.length()*vertices_per_char, text.length()*indices_per_char);
        final GLRegion region = GLRegion.create(gl.getGLProfile(), renderModes, null);
        System.err.println("Region post ctor w/ default initial buffer size");
        region.printBufferStats(System.err);

        final int[] verticesIndicesCount = new int[] { 0, 0 };
        TextRegionUtil.countStringRegion(region, font, text, verticesIndicesCount);
        System.err.println("Region count: text "+text.length()+" chars -> vertices "+verticesIndicesCount[0]+", indices "+verticesIndicesCount[1]);
        region.setBufferCapacity(verticesIndicesCount[0], verticesIndicesCount[1]);
        System.err.println("Region post set-buffer-size w/ matching vertices "+verticesIndicesCount[0]+", indices "+verticesIndicesCount[1]);
        region.printBufferStats(System.err);

        final Perf perf = new Perf();
        if( do_perf ) {
            region.perfCounter().enable(true);
        }

        final AffineTransform translation = new AffineTransform();
        final AffineTransform tmp1 = new AffineTransform();
        final AffineTransform tmp2 = new AffineTransform();

        for(int loop_i=0; loop_i < loop_count; ++loop_i) {
            final long t2 = Clock.currentNanos(); // all initialized but graph
            if( null != perf ) {
                ++perf.count;
            }

            // init
            // final GLRegion region = GLRegion.create(gl.getGLProfile(), renderModes, null);
            // region.growBufferSize(123000, 62000); // hack-me
            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            renderer.init(gl, 0);
            rs.setColorStatic(0.1f, 0.1f, 0.1f, 1.0f);

            // reshape
            gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

            // renderer.reshapePerspective(gl, 45.0f, drawable.getWidth(), drawable.getHeight(), 0.1f, 1000.0f);
            renderer.reshapeOrtho(drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), 0.1f, 1000.0f);
            final int z0 = -1000;

            final int[] sampleCountIO = { sampleCount };
            // display
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            region.clear(gl);

            final long t3 = Clock.currentNanos(); // all initialized w/ graph

            final float dx = 0;
            final float dy = drawable.getSurfaceHeight() - 2 * fontSize * font.getLineHeight();
            final long t4;
            {
                // all sizes in em
                final float x_width = font.getAdvanceWidth( font.getGlyphID('X') );

                translation.setToTranslation(1*x_width, 0f);
                final AABBox tbox_1 = font.getGlyphBounds(text, tmp1, tmp2);
                final AABBox rbox_1 = TextRegionUtil.addStringToRegion(region, font, translation, text, fg_color, tmp1, tmp2);
                t4 = Clock.currentNanos(); // text added to region
                if( 0 == loop_i && !do_perf ) {
                    System.err.println("Text_1: tbox "+tbox_1);
                    System.err.println("Text_1: rbox "+rbox_1);
                }
            }

            final PMVMatrix pmv = renderer.getMatrix();
            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
            pmv.glTranslatef(dx, dy, z0);
            pmv.glScalef(fontSize, fontSize, 1f);
            region.draw(gl, renderer, sampleCountIO);
            final long t5 = Clock.currentNanos(); // text added to region
            if( null != perf ) {
                final long td_graph = t3 - t2;
                final long td_txt = t4 - t3;
                final long td_draw = t5 - t4;
                final long td_txt_draw = t5 - t3;
                perf.td_graph += td_graph;
                perf.td_txt += td_txt;
                perf.td_draw += td_draw;
                perf.td_txt_draw += td_txt_draw;
                if( 0 == loop_i ) {
                    final Duration td_launch0a = Duration.between(Clock.getMonotonicStartupTime(), t0i); // loading Gluegen - load test
                    final Duration td_launch0b = Duration.between(Clock.getMonotonicStartupTime(), t1i); // loading Gluegen - start test
                    final long td_launch1 = t1 - t0; // since loading this test
                    final long td_launch2 = t2 - t0; // gl
                    final long td_launch3 = t3 - t0; // w/ graph
                    final long td_launch_txt = t4 - t0;
                    final long td_launch_draw = t5 - t0;
                    System.err.printf("%n%n%3d: Perf Launch:%n"+
                                      "- loading GlueGen - loading test %,6d [ms]%n"+
                                      "- loading GlueGen - start test   %,6d [ms]%n"+
                                      "- loading test    - start test   %,6d [ms]%n"+
                                      "- loading test    - gl           %,6d [ms]%n"+
                                      "- loading test    - graph        %,6d [ms]%n"+
                                      "- loading test    - txt          %,6d [ms]%n"+
                                      "- loading test    - draw         %,6d [ms]%n",
                            loop_i+1,
                            td_launch0a.toMillis(), td_launch0b.toMillis(),
                            TimeUnit.NANOSECONDS.toMillis(td_launch1), TimeUnit.NANOSECONDS.toMillis(td_launch2),
                            TimeUnit.NANOSECONDS.toMillis(td_launch3), TimeUnit.NANOSECONDS.toMillis(td_launch_txt),
                            TimeUnit.NANOSECONDS.toMillis(td_launch_draw));
                    perf.print(System.err, loop_i+1, "Launch");
                }
            }
            if( loop_count - 1 == loop_i && do_snap ) {
                // print screen at end
                gl.glFinish();
                printScreen(screenshot, renderModes, drawable, gl, false, sampleCount);
            }
            drawable.swapBuffers();
            if( null != perf && loop_count/2-1 == loop_i ) {
                // print + reset counter @ 1/3 loops
                if( do_perf ) {
                    region.perfCounter().print(System.err);
                }
                perf.print(System.err, loop_i+1, "Frame"+(loop_i+1));
                perf.clear();
                if( do_perf ) {
                    region.perfCounter().clear();
                }
            }
            if( 0 == loop_i || loop_count - 1 == loop_i) {
                // print counter @ start and end
                System.err.println("GLRegion: for "+gl.getGLProfile()+" using int32_t indiced: "+region.usesI32Idx());
                System.err.println("GLRegion: "+region);
                System.err.println("Text length: "+text.length());
                region.printBufferStats(System.err);
                if( do_perf ) {
                    region.perfCounter().print(System.err);
                }
                perf.print(System.err, loop_i+1, "Frame"+(loop_i+1));
            }
            // region.destroy(gl);
        }

        region.destroy(gl);

        sleep();

        // dispose
        screenshot.dispose(gl);
        renderer.destroy(gl);

        NEWTGLContext.destroyWindow(winctx);
    }
    public static final String text_long =
        "JOGL: Java™ Binding for OpenGL®, providing hardware-accelerated 3D graphics.\n\n"+
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nec sapien tellus.\n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies\n"+
        "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices ultricies nec a elit.\n"+
        "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia pharetra at venenatis est.\n"+
        "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat nec, volutpat nec nisi.\n"+
        "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum id tempus quis, semper\n"+
        "Maecenas in ipsum ac justo scelerisque sollicitudin. Quisque sit amet neque lorem,\n" +
        "I “Ask Jeff” or ‘Ask Jeff’. Take the chef d’œuvre! Two of [of] (of) ‘of’ “of” of? of! of*.\n"+
        "Les Woëvres, the Fôret de Wœvres, the Voire and Vauvise. Yves is in heaven.\n"+
        "Lyford’s in Texas & L’Anse-aux-Griffons in Québec; the Łyna in Poland. Yriarte is in Yale.\n"+
        "Kyoto and Ryotsu are both in Japan, Kwikpak on the Yukon delta, Kvæven in Norway…\n"+
        "Von-Vincke-Straße in Münster, Vdovino in Russia, Ytterbium in the periodic table.\n"+
        "Miłosz and Wū Wŭ all in the library? 1510–1620, 11:00 pm, and the 1980s are over.\n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies \n"+
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

    public static final String text_1s = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nec sapien tellus.";
    public static final String text_1 =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nec sapien tellus. \n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies \n"+
        "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices ultricies nec a elit. \n"+
        "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia pharetra at venenatis est. \n"+
        "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat nec, volutpat nec nisi. \n"+
        "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum id tempus quis, semper \n"+
        "in lorem. Maecenas in ipsum ac justo scelerisque sollicitudin. Quisque sit amet neque lorem, \n" +
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
