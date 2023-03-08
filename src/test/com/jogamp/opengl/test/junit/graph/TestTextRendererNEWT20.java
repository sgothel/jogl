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
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.graph.demos.GPUTextRendererListenerBase01;
import com.jogamp.opengl.test.junit.graph.demos.MSAATool;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.caps.NonFSAAGLCapsChooser;


/**
 * TestTextRendererNEWT20 Variant
 * - Using listener derived from fully features GPUTextRendererListenerBase01
 * - Renders multiple demo text with multiple fonts
 * - Used for validation against reference
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTextRendererNEWT20 extends UITestCase {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    static long duration = 100; // ms
    static int win_width = 1024;
    static int win_height = 640;

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
            } else if(args[i].equals("-width")) {
                i++;
                win_width = atoi(args[i]);
            } else if(args[i].equals("-height")) {
                i++;
                win_height = atoi(args[i]);
            }
        }
        final String tstname = TestTextRendererNEWT20.class.getName();
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

    @Test
    public void test00TextRendererVBAA01() throws InterruptedException, GLException, IOException {
        if(Platform.CPUFamily.X86 != PlatformPropsImpl.CPU_ARCH.family) { // FIXME
            // FIXME: Disabled for now - since it doesn't seem fit for mobile (performance wise).
            System.err.println("disabled on non desktop (x86) arch for now ..");
            return;
        }
        final GLProfile glp = GLProfile.getGL2ES2();

        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        System.err.println("Requested: "+caps);

        final GLWindow window = createWindow("TTRN20", caps, win_width, win_height);
        window.display();
        System.err.println(VersionUtil.getPlatformInfo());
        // System.err.println(JoglVersion.getAllAvailableCapabilitiesInfo(window.getScreen().getDisplay().getGraphicsDevice(), null).toString());
        System.err.println("Chosen: "+window.getChosenGLCapabilities());

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final TextGLListener textGLListener = new TextGLListener(glp, rs, Region.VBAA_RENDERING_BIT, 4 /* sampleCount */, DEBUG, TRACE);
        textGLListener.attachInputListenerTo(window);
        window.addGLEventListener(textGLListener);
        textGLListener.setHeadBox(2, true);
        window.display();
        // final AABBox headbox = textGLListener.getHeadBox();
        // GPUTextRendererListenerBase01.upsizeWindowSurface(window, false, (int)(headbox.getWidth()*1.5f), (int)(headbox.getHeight()*2f));

        final Runnable action_per_font = new Runnable() {
            @Override
            public void run() {
                textGLListener.setHeadBox(1, false);
                textGLListener.setSampleCount(4);
                window.display();
                textGLListener.printScreenOnGLThread(window, "./", window.getTitle(), "", false);
                sleep();

                textGLListener.setHeadBox(2, false);
                textGLListener.setSampleCount(4);
                window.display();
                textGLListener.printScreenOnGLThread(window, "./", window.getTitle(), "", false);
                sleep();
            } };

        final Font[] fonts = FontSet01.getSet01();
        for(final Font f : fonts) {
            if( textGLListener.setFont(f) ) {
                action_per_font.run();
            }
        }
        if(textGLListener.setFontSet(FontFactory.JAVA, 0, 0)) {
            action_per_font.run();
        }
        destroyWindow(window);
    }

    @Test
    public void test10TextRendererMSAA01() throws InterruptedException, GLException, IOException {
        if(Platform.CPUFamily.X86 != PlatformPropsImpl.CPU_ARCH.family) { // FIXME
            // FIXME: Disabled for now - since it doesn't seem fit for mobile (performance wise).
            System.err.println("disabled on non desktop (x86) arch for now ..");
            return;
        }
        final GLProfile glp = GLProfile.getGL2ES2();

        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        System.err.println("Requested: "+caps);

        final GLWindow window = createWindow("TTRN20", caps, win_width, win_height);
        window.display();
        System.err.println("Chosen: "+window.getChosenGLCapabilities());

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final TextGLListener textGLListener = new TextGLListener(glp, rs, Region.MSAA_RENDERING_BIT, 4 /* sampleCount */, DEBUG, TRACE);
        textGLListener.attachInputListenerTo(window);
        window.addGLEventListener(textGLListener);
        textGLListener.setHeadBox(2, true);
        window.display();
        // final AABBox headbox = textGLListener.getHeadBox();
        // GPUTextRendererListenerBase01.upsizeWindowSurface(window, false, (int)(headbox.getWidth()*1.5f), (int)(headbox.getHeight()*2f));

        final Runnable action_per_font = new Runnable() {
            @Override
            public void run() {
                textGLListener.setHeadBox(1, false);
                textGLListener.setSampleCount(4);
                window.display();
                textGLListener.printScreenOnGLThread(window, "./", window.getTitle(), "", false);
                sleep();

                textGLListener.setHeadBox(2, false);
                textGLListener.setSampleCount(4);
                window.display();
                textGLListener.printScreenOnGLThread(window, "./", window.getTitle(), "", false);
                sleep();
            } };

        final Font[] fonts = FontSet01.getSet01();
        for(final Font f : fonts) {
            if( textGLListener.setFont(f) ) {
                action_per_font.run();
            }
        }
        if(textGLListener.setFontSet(FontFactory.JAVA, 0, 0)) {
            action_per_font.run();
        }
        destroyWindow(window);
    }

    @Test
    public void test20TextRendererFSAA01() throws InterruptedException, GLException, IOException {
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        System.err.println("Requested: "+caps);

        final GLWindow window = createWindow("TTRN20", caps, 1024, 640);
        window.display();
        System.err.println("Chosen: "+window.getChosenGLCapabilities());

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final TextGLListener textGLListener = new TextGLListener(glp, rs, 0, 0 /* sampleCount */, DEBUG, TRACE);
        textGLListener.attachInputListenerTo(window);
        window.addGLEventListener(textGLListener);
        textGLListener.setHeadBox(2, true);
        window.display();

        final Runnable action_per_font = new Runnable() {
            @Override
            public void run() {
                textGLListener.setHeadBox(1, false);
                textGLListener.setSampleCount(0);
                window.display();
                textGLListener.printScreenOnGLThread(window, "./", window.getTitle(), "", false);
                sleep();

                textGLListener.setHeadBox(2, false);
                textGLListener.setSampleCount(0);
                window.display();
                textGLListener.printScreenOnGLThread(window, "./", window.getTitle(), "", false);
                sleep();
            } };

        final Font[] fonts = FontSet01.getSet01();
        for(final Font f : fonts) {
            if( textGLListener.setFont(f) ) {
                action_per_font.run();
            }
        }
        if(textGLListener.setFontSet(FontFactory.JAVA, 0, 0)) {
            action_per_font.run();
        }

        destroyWindow(window);
    }

    @Test
    public void test30TextRendererNoSampling() throws InterruptedException, GLException, IOException {
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        System.err.println("Requested: "+caps);

        final GLWindow window = createWindow("TTRN20", caps, 1024, 640);
        window.display();
        System.err.println("Chosen: "+window.getChosenGLCapabilities());

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final TextGLListener textGLListener = new TextGLListener(glp, rs, 0, 0 /* sampleCount */, DEBUG, TRACE);
        textGLListener.attachInputListenerTo(window);
        window.addGLEventListener(textGLListener);
        textGLListener.setHeadBox(2, true);
        window.display();

        final Runnable action_per_font = new Runnable() {
            @Override
            public void run() {
                textGLListener.setHeadBox(1, false);
                textGLListener.setSampleCount(0);
                window.display();
                textGLListener.printScreenOnGLThread(window, "./", window.getTitle(), "", false);
                sleep();

                textGLListener.setHeadBox(2, false);
                textGLListener.setSampleCount(0);
                window.display();
                textGLListener.printScreenOnGLThread(window, "./", window.getTitle(), "", false);
                sleep();
            } };

        final Font[] fonts = FontSet01.getSet01();
        for(final Font f : fonts) {
            if( textGLListener.setFont(f) ) {
                action_per_font.run();
            }
        }
        if(textGLListener.setFontSet(FontFactory.JAVA, 0, 0)) {
            action_per_font.run();
        }

        destroyWindow(window);
    }

    private static class TextGLListener extends GPUTextRendererListenerBase01 {
        public TextGLListener(final GLProfile glp, final RenderState rs, final int type, final int sampleCount, final boolean debug, final boolean trace) {
            super(glp, rs, type, sampleCount, true, debug, trace);
        }

        @Override
        public void attachInputListenerTo(final GLWindow window) {
            super.attachInputListenerTo(window);
        }
        public void setSampleCount(final int sampleCount){
            // setMatrix(xt, yt, zoom, angle, sampleCount);
            setMatrix(0, 0, 0, 0f, sampleCount);
        }

        @Override
        public void init(final GLAutoDrawable drawable) {
            super.init(drawable);

            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            gl.setSwapInterval(1);
            gl.glEnable(GL.GL_DEPTH_TEST);
            System.err.println(JoglVersion.getGLInfo(gl, null, false /* withCapsAndExts */).toString());
            MSAATool.dump(drawable);

            final RenderState rs = getRenderer().getRenderState();
            rs.setColorStatic(0.1f, 0.1f, 0.1f, 1.0f);
        }

        @Override
        public void display(final GLAutoDrawable drawable) {
            super.display(drawable);
        }
    }
}
