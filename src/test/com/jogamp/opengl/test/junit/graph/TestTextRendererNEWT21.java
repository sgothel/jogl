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

import java.io.IOException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.JoglVersion;

import jogamp.common.os.PlatformPropsImpl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.caps.NonFSAAGLCapsChooser;


/**
 * TestTextRendererNEWT21 Variant
 * - Using FontViewListener01, a full Glyph Grid using GraphUI
 * - Renders multiple demo text with multiple fonts
 * - Used for validation
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTextRendererNEWT21 extends UITestCase {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    static long duration = 100; // ms
    static int win_width = 1280;
    static int win_height = 720;
    static boolean onlyOne = false;

    static int atoi(final String a) {
        try {
            return Integer.parseInt(a);
        } catch (final Exception ex) { throw new RuntimeException(ex); }
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = atoi(args[i]);
            } else if(args[i].equals("-one")) {
                onlyOne = true;
            } else if(args[i].equals("-width")) {
                i++;
                win_width = atoi(args[i]);
            } else if(args[i].equals("-height")) {
                i++;
                win_height = atoi(args[i]);
            }
        }
        final String tstname = TestTextRendererNEWT21.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

    static void sleep() {
        try {
            System.err.println("** new frame ** (sleep: "+duration+"ms)");
            Thread.sleep(duration);
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
        if( !caps.getSampleBuffers() ) {
            // Make sure to not have FSAA if not requested
            // TODO: Implement in default chooser?
            window.setCapabilitiesChooser(new NonFSAAGLCapsChooser(true));
        }
        window.setSize(width, height);
        window.setPosition(10, 10);
        window.setTitle(title);
        Assert.assertNotNull(window);
        window.setVisible(true);

        return window;
    }

    class TestAction implements Runnable {
        private final GLWindow window;
        private final int renderModes;
        private final int graphSampleCount;
        private final Font font;
        private boolean keepAlive = false;

        public TestAction(final GLWindow window, final int renderModes, final int graphSampleCount, final Font font) {
            this.window = window;
            this.renderModes = renderModes;
            this.graphSampleCount = graphSampleCount;
            this.font = font;
        }
        public void setKeepAlive(final boolean v) { keepAlive = v; }

        @Override
        public void run() {
            final int fsaaSampleCount = window.getChosenGLCapabilities().getNumSamples();
            if( null != font ) {
                System.err.printf("Test Run: %s, %s%n",
                        Region.getRenderModeString(renderModes, graphSampleCount, fsaaSampleCount),
                        font.getFullFamilyName());
                final FontViewListener01 glel = new FontViewListener01(renderModes, graphSampleCount, font, 0 /* startGlyphID */);
                glel.attachInputListenerTo(window);
                window.addGLEventListener(glel);
                window.display();
                glel.printScreenOnGLThread(window, "./", window.getTitle(), "", false);
                sleep();
                if( !keepAlive ) {
                    window.disposeGLEventListener(glel, true);
                }
            } else {
                System.err.printf("Test Skipped: %s, %s, font not available%n",
                        Region.getRenderModeString(renderModes, graphSampleCount, fsaaSampleCount),
                        font.getFullFamilyName());
            }
        }
    }

    @Test
    public void test00() throws InterruptedException, GLException, IOException {
        if( !onlyOne ) {
            System.err.println("disabled !onlyOne");
            return;
        }
        final GLProfile glp = GLProfile.getGL2ES2();

        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        System.err.println("Requested: "+caps);

        final GLWindow window = createWindow("TTRN21", caps, win_width, win_height);
        window.display();
        System.err.println(VersionUtil.getPlatformInfo());
        // System.err.println(JoglVersion.getAllAvailableCapabilitiesInfo(window.getScreen().getDisplay().getGraphicsDevice(), null).toString());
        System.err.println("Chosen: "+window.getChosenGLCapabilities());

        final int graphSampleCount = 4;
        final TestAction ta = new TestAction(window, Region.VBAA_RENDERING_BIT, graphSampleCount, FontSet01.getSet01()[0]);
        ta.setKeepAlive(true);
        ta.run();
    }

    @Test
    public void test00TextRendererVBAA01() throws InterruptedException, GLException, IOException {
        if( onlyOne || Platform.CPUFamily.X86 != PlatformPropsImpl.CPU_ARCH.family ) { // FIXME
            // FIXME: Disabled for now - since it doesn't seem fit for mobile (performance wise).
            System.err.println("disabled on non desktop (x86) arch for now ..");
            return;
        }
        final GLProfile glp = GLProfile.getGL2ES2();

        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        System.err.println("Requested: "+caps);

        final GLWindow window = createWindow("TTRN21", caps, win_width, win_height);
        window.display();
        System.err.println(VersionUtil.getPlatformInfo());
        // System.err.println(JoglVersion.getAllAvailableCapabilitiesInfo(window.getScreen().getDisplay().getGraphicsDevice(), null).toString());
        System.err.println("Chosen: "+window.getChosenGLCapabilities());

        final int graphSampleCount = 4;
        final Font[] fonts = FontSet01.getSet01();
        for(final Font f : fonts) {
            new TestAction(window, Region.VBAA_RENDERING_BIT, graphSampleCount, f).run();
        }
        try {
            new TestAction(window, Region.VBAA_RENDERING_BIT, graphSampleCount, FontFactory.get(FontFactory.JAVA).get(0 /* family */, 0 /* stylebits */)).run();
        } catch(final IOException ioe) {
            System.err.println("Caught: "+ioe.getMessage());
        }
        destroyWindow(window);
    }

    @Test
    public void test10TextRendererMSAA01() throws InterruptedException, GLException, IOException {
        if( onlyOne || Platform.CPUFamily.X86 != PlatformPropsImpl.CPU_ARCH.family ) { // FIXME
            // FIXME: Disabled for now - since it doesn't seem fit for mobile (performance wise).
            System.err.println("disabled on non desktop (x86) arch for now ..");
            return;
        }
        final GLProfile glp = GLProfile.getGL2ES2();

        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        System.err.println("Requested: "+caps);

        final GLWindow window = createWindow("TTRN21", caps, win_width, win_height);
        window.display();
        System.err.println("Chosen: "+window.getChosenGLCapabilities());

        final int graphSampleCount = 4;
        final Font[] fonts = FontSet01.getSet01();
        for(final Font f : fonts) {
            new TestAction(window, Region.MSAA_RENDERING_BIT, graphSampleCount, f).run();
        }
        try {
            new TestAction(window, Region.MSAA_RENDERING_BIT, graphSampleCount, FontFactory.get(FontFactory.JAVA).get(0 /* family */, 0 /* stylebits */)).run();
        } catch(final IOException ioe) {
            System.err.println("Caught: "+ioe.getMessage());
        }
        destroyWindow(window);
    }

    @Test
    public void test20TextRendererFSAA01() throws InterruptedException, GLException, IOException {
        if( onlyOne ) {
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        System.err.println("Requested: "+caps);

        final GLWindow window = createWindow("TTRN21", caps, win_width, win_height);
        window.display();
        System.err.println("Chosen: "+window.getChosenGLCapabilities());

        final int graphSampleCount = 0;
        final Font[] fonts = FontSet01.getSet01();
        for(final Font f : fonts) {
            new TestAction(window, Region.NORM_RENDERING_BIT, graphSampleCount, f).run();
        }
        try {
            new TestAction(window, Region.NORM_RENDERING_BIT, graphSampleCount, FontFactory.get(FontFactory.JAVA).get(0 /* family */, 0 /* stylebits */)).run();
        } catch(final IOException ioe) {
            System.err.println("Caught: "+ioe.getMessage());
        }
        destroyWindow(window);
    }

    @Test
    public void test30TextRendererNoSampling() throws InterruptedException, GLException, IOException {
        if( onlyOne ) {
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        System.err.println("Requested: "+caps);

        final GLWindow window = createWindow("TTRN21", caps, win_width, win_height);
        window.display();
        System.err.println("Chosen: "+window.getChosenGLCapabilities());

        final int graphSampleCount = 0;
        final Font[] fonts = FontSet01.getSet01();
        for(final Font f : fonts) {
            new TestAction(window, Region.NORM_RENDERING_BIT, graphSampleCount, f).run();
        }
        try {
            new TestAction(window, Region.NORM_RENDERING_BIT, graphSampleCount, FontFactory.get(FontFactory.JAVA).get(0 /* family */, 0 /* stylebits */)).run();
        } catch(final IOException ioe) {
            System.err.println("Caught: "+ioe.getMessage());
        }
        destroyWindow(window);
    }
}
