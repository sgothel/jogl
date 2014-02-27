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
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.glsl.ShaderState;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTextRendererNEWT00 extends UITestCase {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    static long duration = 100; // ms
    static boolean waitStartEnd = false;

    static final int[] texSize = new int[] { 0 };
    static final int fontSize = 24;
    static Font font;

    @BeforeClass
    public static void setup() throws IOException {
        font = FontFactory.get(FontFactory.UBUNTU).getDefault();
    }

    static int atoi(String a) {
        try {
            return Integer.parseInt(a);
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = atoi(args[i]);
            } else if(args[i].equals("-wait")) {
                waitStartEnd = true;
            }
        }
        String tstname = TestTextRendererNEWT00.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

    static void sleep() {
        try {
            System.err.println("** new frame ** (sleep: "+duration+"ms)");
            Thread.sleep(duration);
        } catch (InterruptedException ie) {}
    }

    static void destroyWindow(GLWindow window) {
        if(null!=window) {
            window.destroy();
        }
    }

    static GLWindow createWindow(String title, GLCapabilitiesImmutable caps, int width, int height) {
        Assert.assertNotNull(caps);

        GLWindow window = GLWindow.create(caps);
        window.setSize(width, height);
        window.setPosition(10, 10);
        window.setTitle(title);
        Assert.assertNotNull(window);
        window.setVisible(true);

        return window;
    }

    @Test
    public void testTextRendererMSAA01() throws InterruptedException {
        GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        System.err.println("Requested: "+caps);

        GLWindow window = createWindow("text-vbaa0-msaa1", caps, 1024, 640);
        window.display();
        System.err.println("Chosen: "+window.getChosenGLCapabilities());
        if( waitStartEnd ) {
            UITestCase.waitForKey("Start");
        }

        final RenderState rs = RenderState.createRenderState(new ShaderState(), SVertex.factory());
        final TextRendererGLEL textGLListener = new TextRendererGLEL(rs);
        System.err.println(textGLListener.getFontInfo());

        window.addGLEventListener(textGLListener);

        window.invoke(true, new GLRunnable() {
            @Override
            public boolean run(GLAutoDrawable drawable) {
                try {
                    textGLListener.printScreen(drawable, "./", "TestTextRendererNEWT00-snap"+screenshot_num, false);
                    screenshot_num++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        Animator anim = new Animator();
        anim.add(window);
        anim.start();
        anim.setUpdateFPSFrames(60, null);
        sleep();
        anim.stop();
        if( waitStartEnd ) {
            UITestCase.waitForKey("Stop");
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
        "------- End of Story ;-) ---------\n";

    private final class TextRendererGLEL extends TextRendererGLELBase {
        private final GLReadBufferUtil screenshot;
        private long t0;

        TextRendererGLEL(final RenderState rs) {
            super(rs, true /* exclusivePMV */, 0); // Region.VBAA_RENDERING_BIT);
            texSizeScale = 2;

            fontSize = 24;

            staticRGBAColor[0] = 0.0f;
            staticRGBAColor[1] = 0.0f;
            staticRGBAColor[2] = 0.0f;
            staticRGBAColor[3] = 1.0f;

            this.screenshot = new GLReadBufferUtil(false, false);
        }

        @Override
        public void init(GLAutoDrawable drawable) {
            super.init(drawable);
            drawable.getGL().setSwapInterval(0);
            t0 = Platform.currentTimeMillis();
        }
        public void dispose(GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            screenshot.dispose(gl);
            super.dispose(drawable);
        }

        public void printScreen(GLAutoDrawable drawable, String dir, String objName, boolean exportAlpha) throws GLException, IOException {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.printf("%s-%03dx%03d-T%04d", objName, drawable.getWidth(), drawable.getHeight(), texSize[0]);

            final String filename = dir + sw +".png";
            if(screenshot.readPixels(drawable.getGL(), false)) {
                screenshot.write(new File(filename));
            }
        }

        String getFontInfo() {
            final float unitsPerEM_Inv = font.getMetrics().getScale(1f);
            final float unitsPerEM = 1f / unitsPerEM_Inv;
            return String.format("Font %s, unitsPerEM %f", font.getName(Font.NAME_UNIQUNAME), unitsPerEM);
        }

        @Override
        public void display(GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();

            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            final GLAnimatorControl anim = drawable.getAnimator();
            final float lfps = null != anim ? anim.getLastFPS() : 0f;
            final float tfps = null != anim ? anim.getTotalFPS() : 0f;

            // Note: MODELVIEW is from [ 0 .. height ]

            final long t1 = Platform.currentTimeMillis();

            final String text1 = String.format("%03.1f/%03.1f fps, vsync %d, elapsed %4.1f s",
                    lfps, tfps, gl.getSwapInterval(), (t1-t0)/1000.0);

            if( false ) {
                renderString(drawable, textX2,      0, 0, 0, 0, -1000, true);
                // renderString(drawable, "0",         0, 0, 0, 0, -1000);
                // renderString(drawable, getFontInfo(),    0, 0, 0, -1000);
            } else {
                renderString(drawable, getFontInfo(),                    0, 0, 0, 0, -1000, true);
                renderString(drawable, "012345678901234567890123456789", 0, 0, 0, -1000, true);
                renderString(drawable, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", 0, 0, 0, -1000, true);
                renderString(drawable, "Hello World",                    0, 0, 0, -1000, true);
                renderString(drawable, "4567890123456",                  4, 0, 0, -1000, true);
                renderString(drawable, "I like JogAmp",                  4, 0, 0, -1000, true);
                renderString(drawable, "Hello World",                    0, 0, 0, -1000, true);
                renderString(drawable, textX2,                           0, 0, 0, -1000, true);
                renderString(drawable, text1,                            0, 0, 0, -1000, false); // no-cache
            }
        } };

}
