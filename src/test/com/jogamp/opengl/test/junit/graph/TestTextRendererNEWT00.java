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
import java.net.URL;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTextRendererNEWT00 extends UITestCase {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    static long Duration = 2000; // ms
    static boolean WaitStartEnd = false;
    static boolean TextAnim = false;
    static int SceneMSAASamples = 0;
    static int GraphVBAASamples = 0;
    static int GraphMSAASamples = 0;
    static boolean ManualTest = false;
    static int SwapInterval = 1;

    static String fontFileName = null;
    static URL fontURL = null;
    static int fontSet = 0;
    static int fontFamily = 0;
    static int fontStylebits = 0;
    static float fontSizeFixed = 14f;

    static int atoi(final String a) {
        try {
            return Integer.parseInt(a);
        } catch (final Exception ex) { throw new RuntimeException(ex); }
    }

    public static void main(final String args[]) throws IOException {
        ManualTest = args.length > 0;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                Duration = atoi(args[i]);
            } else if(args[i].equals("-fontURL")) {
                i++;
                fontURL = new URL(args[i]);
            } else if(args[i].equals("-fontFile")) {
                i++;
                fontFileName = args[i];
            } else if(args[i].equals("-fontSet")) {
                i++;
                fontSet = atoi(args[i]);
            } else if(args[i].equals("-fontFamily")) {
                i++;
                fontFamily = atoi(args[i]);
            } else if(args[i].equals("-fontStyle")) {
                i++;
                fontStylebits = atoi(args[i]);
            } else if(args[i].equals("-fontSize")) {
                i++;
                fontSizeFixed = atoi(args[i]);
            } else if(args[i].equals("-smsaa")) {
                i++;
                SceneMSAASamples = atoi(args[i]);
            } else if(args[i].equals("-gmsaa")) {
                i++;
                GraphMSAASamples = atoi(args[i]);
            } else if(args[i].equals("-gvbaa")) {
                i++;
                GraphVBAASamples = atoi(args[i]);
            } else if(args[i].equals("-textAnim")) {
                TextAnim = true;
            } else if(args[i].equals("-vsync")) {
                i++;
                SwapInterval = MiscUtils.atoi(args[i], SwapInterval);
            } else if(args[i].equals("-wait")) {
                WaitStartEnd = true;
            }
        }
        System.err.println("Font [set "+fontSet+", family "+fontFamily+", style "+fontStylebits+", size "+fontSizeFixed+"], fontFileName "+fontFileName);
        System.err.println("Scene MSAA Samples "+SceneMSAASamples);
        System.err.println("Graph MSAA Samples "+GraphMSAASamples);
        System.err.println("Graph VBAA Samples "+GraphVBAASamples);
        System.err.println("swapInterval "+SwapInterval);
        final String tstname = TestTextRendererNEWT00.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

    static void sleep() {
        sleep(Duration);
    }
    static void sleep(final long d) {
        try {
            System.err.println("** new frame ** (sleep: "+d+"ms)");
            Thread.sleep(d);
        } catch (final InterruptedException ie) {}
    }

    static void destroyWindow(final GLWindow window) {
        if(null!=window) {
            window.destroy();
        }
    }

    static GLWindow createWindow(final String title, final GLCapabilitiesImmutable caps, final int width, final int height) {
        Assert.assertNotNull(caps);

        final GLWindow window = GLWindow.create(caps);
        window.setSize(width, height);
        window.setPosition(10, 10);
        window.setTitle(title);
        Assert.assertNotNull(window);
        window.setVisible(true);

        return window;
    }

    @Test
    public void test00Manual() throws InterruptedException {
        if( ManualTest ) {
            testImpl(SceneMSAASamples, GraphMSAASamples, GraphVBAASamples);
        }
    }
    @Test
    public void test00SceneNoAA() throws InterruptedException {
        if( !ManualTest ) {
            testImpl(0, 0, 0);
        }
    }
    @Test
    public void test01SceneMSAA04() throws InterruptedException {
        if( !ManualTest ) {
            testImpl(4, 0, 0);
        }
    }
    @Test
    public void test02GraphMSAA04() throws InterruptedException {
        if( !ManualTest ) {
            testImpl(0, 4, 0);
        }
    }
    @Test
    public void test03GraphVBAA04() throws InterruptedException {
        if( !ManualTest ) {
            testImpl(0, 0, 4);
        }
    }

    public void testImpl(final int sceneMSAASamples, final int graphMSAASamples, final int graphVBAASamples) throws InterruptedException {
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        if( 0 < sceneMSAASamples ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(sceneMSAASamples);
        }
        System.err.println("Requested: "+caps+", graph[msaaSamples "+graphMSAASamples+", vbaaSamples "+graphVBAASamples+"]");

        final GLWindow window = createWindow("text-gvbaa"+graphVBAASamples+"-gmsaa"+graphMSAASamples+"-smsaa"+sceneMSAASamples, caps, 1024, 640);
        window.display();
        System.err.println("Chosen: "+window.getChosenGLCapabilities());
        if( WaitStartEnd ) {
            JunitTracer.waitForKey("Start");
        }

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final int renderModes, sampleCount;
        if( graphVBAASamples > 0 ) {
            renderModes = Region.VBAA_RENDERING_BIT;
            sampleCount = graphVBAASamples;
        } else if ( graphMSAASamples > 0 ) {
            renderModes = Region.MSAA_RENDERING_BIT;
            sampleCount = graphMSAASamples;
        } else {
            renderModes = 0;
            sampleCount = 0;
        }
        final TextRendererGLEL textGLListener = new TextRendererGLEL(rs, renderModes, sampleCount);
        System.err.println(textGLListener.getFontInfo());

        window.addGLEventListener(textGLListener);

        final Animator anim = new Animator();
        anim.add(window);
        anim.start();
        anim.setUpdateFPSFrames(60, null);
        sleep();
        window.invoke(true, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable drawable) {
                try {
                    textGLListener.printScreen(renderModes, drawable, "./", "TestTextRendererNEWT00-snap"+screenshot_num, false);
                    screenshot_num++;
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        anim.stop();
        if( WaitStartEnd ) {
            JunitTracer.waitForKey("Stop");
        }
        destroyWindow(window);
    }
    int screenshot_num = 0;

    static final String textX2 =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nec sapien tellus. \n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies \n"+
        "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices ultricies nec a elit. \n"+
        "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia pharetra at venenatis est.\n"+
        "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat nec, volutpat nec nisi.\n"+
        "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum id tempus quis, semper \n"+
        "in lorem. Maecenas in ipsum ac justo scelerisque sollicitudin. Quisque sit amet neque lorem,\n" +
        "-------Press H to change text---------\n";

    private static final class TextRendererGLEL extends TextRendererGLELBase {
        private final GLReadBufferUtil screenshot;
        private final GLRegion regionFPS, regionFPSAnim;
        final Font font;
        final float fontSizeMin, fontSizeMax;
        private long t0;
        float fontSizeAnim, fontSizeDelta;
        float dpiH;

        TextRendererGLEL(final RenderState rs, final int renderModes, final int sampleCount) {
            super(renderModes, new int[] { sampleCount });
            setRendererCallbacks(RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
            setRenderState(rs);

            regionFPS = GLRegion.create(renderModes, null);
            regionFPSAnim = GLRegion.create(renderModes, null);
            if( null != fontURL ) {
                Font _font = null;
                try {
                    _font = FontFactory.get(fontURL.openStream(), true);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                font = _font;
            } else if( null != fontFileName ) {
                Font _font = null;
                try {
                    _font = FontFactory.get(getClass(), fontFileName, false);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                font = _font;
            } else {
                font = getFont(fontSet, fontFamily, fontStylebits);
            }

            staticRGBAColor[0] = 0.1f;
            staticRGBAColor[1] = 0.1f;
            staticRGBAColor[2] = 0.1f;
            staticRGBAColor[3] = 1.0f;

            this.screenshot = new GLReadBufferUtil(false, false);
            // fontSizeMin = Math.max(8, fontSizeFixed-5);
            fontSizeMin = fontSizeFixed;
            fontSizeMax = fontSizeFixed+8;
            fontSizeAnim = fontSizeFixed;
            fontSizeDelta = 0.01f;
        }

        @Override
        public void init(final GLAutoDrawable drawable) {
            super.init(drawable);
            drawable.getGL().setSwapInterval(SwapInterval);
            t0 = Platform.currentTimeMillis();

            final Window win = (Window)drawable.getUpstreamWidget();
            final float[] pixelsPerMM = win.getPixelsPerMM(new float[2]);
            final float[] dotsPerInch = new float[] { pixelsPerMM[0]*25.4f, pixelsPerMM[1]*25.4f };
            dpiH = dotsPerInch[1];
            System.err.println(getFontInfo());
            System.err.println("fontSize "+fontSizeFixed+", dotsPerMM "+pixelsPerMM[0]+"x"+pixelsPerMM[1]+", dpi "+dotsPerInch[0]+"x"+dotsPerInch[1]+", pixelSize "+font.getPixelSize(fontSizeFixed, dotsPerInch[1] /* dpi display */));
        }

        @Override
        public void dispose(final GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            screenshot.dispose(gl);
            regionFPS.destroy(gl);
            regionFPSAnim.destroy(gl);
            super.dispose(drawable);
        }

        public void printScreen(final int renderModes, final GLAutoDrawable drawable, final String dir, final String objName, final boolean exportAlpha) throws GLException, IOException {
            final String modeS = Region.getRenderModeString(renderModes);
            final String bname = String.format("%s-msaa%02d-fontsz%02.1f-%03dx%03d-%s%04d", objName,
                    drawable.getChosenGLCapabilities().getNumSamples(),
                    TestTextRendererNEWT00.fontSizeFixed, drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), modeS, vbaaSampleCount[0]);
            final String filename = dir + bname +".png";
            if(screenshot.readPixels(drawable.getGL(), false)) {
                screenshot.write(new File(filename));
            }
        }

        String getFontInfo() {
            final float unitsPerEM_Inv = font.getMetrics().getScale(1f);
            final float unitsPerEM = 1f / unitsPerEM_Inv;
            return String.format("Font %s%n %s%nunitsPerEM %f (upem)",
                    font.getFullFamilyName(null).toString(),
                    font.getName(Font.NAME_UNIQUNAME),
                    unitsPerEM);
        }

        @Override
        public void display(final GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();

            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            final GLAnimatorControl anim = drawable.getAnimator();
            final float lfps = null != anim ? anim.getLastFPS() : 0f;
            final float tfps = null != anim ? anim.getTotalFPS() : 0f;

            // Note: MODELVIEW is from [ 0 .. height ]

            final long t1 = Platform.currentTimeMillis();

            // final float fontSize = TestTextRendererNEWT00.fontSize;

            fontSizeAnim += fontSizeDelta;
            if( fontSizeMin >= fontSizeAnim || fontSizeAnim >= fontSizeMax ) {
                fontSizeDelta *= -1f;
            }

            final float pixelSize = font.getPixelSize(fontSizeFixed, dpiH);
            final float pixelSizeAnim = font.getPixelSize(fontSizeAnim, dpiH);

            final String modeS = Region.getRenderModeString(renderModes);

            if( false ) {
                // renderString(drawable, font, pixelSize, "I - / H P 7 0", 0, 0, 0, 0, -1000f, true);
                // renderString(drawable, font, pixelSize, "A M > } ] ", 0, 0, 0, 0, -1000f, true);
                // renderString(drawable, font, pixelSize, "M", 0, 0, 0, 0, -1000f, true);
                // renderString(drawable, font, pixelSize, "0 6 9 a b O Q A M > } ] ", 0, 0, 0, 0, -1000f, true);
                // renderString(drawable, font, pixelSize, "012345678901234567890123456789", 0, 0, 0, -1000, true);
                // renderString(drawable, font, pixelSize, textX2,        0, 0,   0, 0, -1000f, true);
                // renderString(drawable, font, pixelSize, text1,         0,    0, 0, -1000f, regionFPS); // no-cache
                final String text1 = lfps+" / "+tfps+" fps, vsync "+gl.getSwapInterval()+", elapsed "+(t1-t0)/1000.0+
                                     " s, fontSize "+fontSizeFixed+", msaa "+drawable.getChosenGLCapabilities().getNumSamples()+
                                     ", "+modeS+"-samples "+vbaaSampleCount[0];
                renderString(drawable, font, pixelSize, text1,              0, 0, 0, 0, -1000, regionFPS); // no-cache
            } else {
                final String text1 = String.format("%03.1f/%03.1f fps, vsync %d, elapsed %4.1f s, fontSize %2.2f, msaa %d, %s-samples %d",
                        lfps, tfps, gl.getSwapInterval(), (t1-t0)/1000.0, fontSizeFixed,
                        drawable.getChosenGLCapabilities().getNumSamples(), modeS, vbaaSampleCount[0]);
                renderString(drawable, font, pixelSize, getFontInfo(),                    0, 0, 0, 0, -1000, true);
                renderString(drawable, font, pixelSize, "012345678901234567890123456789", 0, 0, 0, -1000, true);
                renderString(drawable, font, pixelSize, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", 0, 0, 0, -1000, true);
                renderString(drawable, font, pixelSize, "Hello World",                    0, 0, 0, -1000, true);
                renderString(drawable, font, pixelSize, "4567890123456",                  4, 0, 0, -1000, true);
                renderString(drawable, font, pixelSize, "I like JogAmp",                  4, 0, 0, -1000, true);
                renderString(drawable, font, pixelSize, "Hello World",                    0, 0, 0, -1000, true);
                renderString(drawable, font, pixelSize, textX2,                           0, 0, 0, -1000, true);
                renderString(drawable, font, pixelSize, text1,                            0, 0, 0, -1000, regionFPS); // no-cache
                if( TextAnim ) {
                    renderString(drawable, font, pixelSizeAnim, text1,                   0, 0, 0, -1000, regionFPSAnim); // no-cache
                }
            }
        } };

}
