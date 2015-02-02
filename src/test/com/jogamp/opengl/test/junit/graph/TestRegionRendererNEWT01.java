/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.common.os.PlatformPropsImpl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.graph.demos.GPURegionGLListener01;
import com.jogamp.opengl.test.junit.graph.demos.GPURegionGLListener02;
import com.jogamp.opengl.test.junit.graph.demos.GPURendererListenerBase01;
import com.jogamp.opengl.test.junit.util.UITestCase;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRegionRendererNEWT01 extends UITestCase {

    public static void main(final String args[]) throws IOException {
        final String tstname = TestRegionRendererNEWT01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
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

    // @Test
    public void test00RegionRendererNONE01() throws InterruptedException {
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities caps = new GLCapabilities(glp);
    //    caps.setOnscreen(false);
        caps.setAlphaBits(4);

        final GLWindow window = createWindow("shape-vbaa0-msaa0", caps, 800, 400);
        final RenderState rs = RenderState.createRenderState(SVertex.factory());

        final GPURegionGLListener01 demo01Listener = new GPURegionGLListener01 (rs, 0, 0, false, false);
        demo01Listener.attachInputListenerTo(window);
        window.addGLEventListener(demo01Listener);

        final RegionGLListener listener = new RegionGLListener(demo01Listener, window.getTitle(), "GPURegion01");
        window.addGLEventListener(listener);

        listener.setTech(-20, 0, -300, 0f, 2);
        window.display();

        listener.setTech(-20, 0, -150, 0f, 3);
        window.display();

        listener.setTech(-20, 0,  -50, 0f, 4);
        window.display();

        destroyWindow(window);
    }

    // @Test
    public void test01RegionRendererNONE02() throws InterruptedException {
        if(Platform.CPUFamily.X86 != PlatformPropsImpl.CPU_ARCH.family) { // FIXME
            // FIXME: Disabled for now - since it doesn't seem fit for mobile (performance wise).
            // FIXME: Also the GLSL code for VARIABLE_CURVE is not fit for mobile yet!
            System.err.println("disabled on non desktop (x86) arch for now ..");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);

        final GLWindow window = createWindow("shape-vbaa0-msaa0", caps, 800, 400);
        final RenderState rs = RenderState.createRenderState(SVertex.factory());

        final GPURegionGLListener01 demo01Listener = new GPURegionGLListener01 (rs, Region.VARWEIGHT_RENDERING_BIT, 0, false, false);
        demo01Listener.attachInputListenerTo(window);
        window.addGLEventListener(demo01Listener);

        final RegionGLListener listener = new RegionGLListener(demo01Listener, window.getTitle(), "GPURegion02");
        window.addGLEventListener(listener);

        listener.setTech(-20, 0, -300, 0f, 2);
        window.display();

        listener.setTech(-20, 0, -150, 0f, 3);
        window.display();

        listener.setTech(-20, 0,  -50, 0f, 4);
        window.display();

        destroyWindow(window);
    }

    @Test
    public void test10RegionRendererMSAA01() throws InterruptedException {
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities caps = new GLCapabilities(glp);
    //    caps.setOnscreen(false);
        caps.setAlphaBits(4);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);

        final GLWindow window = createWindow("shape-vbaa0-msaa1", caps, 800, 400);
        final RenderState rs = RenderState.createRenderState(SVertex.factory());

        final GPURegionGLListener01 demo01Listener = new GPURegionGLListener01 (rs, 0, 0, false, false);
        demo01Listener.attachInputListenerTo(window);
        window.addGLEventListener(demo01Listener);

        final RegionGLListener listener = new RegionGLListener(demo01Listener, window.getTitle(), "GPURegion01");
        window.addGLEventListener(listener);

        listener.setTech(-20, 00, -300, 0f, 2);
        window.display();

        listener.setTech(-20, 00, -150, 0f, 3);
        window.display();

        listener.setTech(-20, 00,  -50, 0f, 4);
        window.display();

        destroyWindow(window);
    }

    // @Test
    public void test11RegionRendererMSAA02() throws InterruptedException {
        if(Platform.CPUFamily.X86 != PlatformPropsImpl.CPU_ARCH.family) { // FIXME
            // FIXME: Disabled for now - since it doesn't seem fit for mobile (performance wise).
            // FIXME: Also the GLSL code for VARIABLE_CURVE is not fit for mobile yet!
            System.err.println("disabled on non desktop (x86) arch for now ..");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);

        final GLWindow window = createWindow("shape-vbaa0-msaa1", caps, 800, 400);
        final RenderState rs = RenderState.createRenderState(SVertex.factory());

        final GPURegionGLListener01 demo01Listener = new GPURegionGLListener01 (rs, Region.VARWEIGHT_RENDERING_BIT, 0, false, false);
        demo01Listener.attachInputListenerTo(window);
        window.addGLEventListener(demo01Listener);

        final RegionGLListener listener = new RegionGLListener(demo01Listener, window.getTitle(), "GPURegion02");
        window.addGLEventListener(listener);

        listener.setTech(-20, 00, -300, 0f, 2);
        window.display();

        listener.setTech(-20, 00, -150, 0f, 3);
        window.display();

        listener.setTech(-20, 00,  -50, 0f, 4);
        window.display();

        destroyWindow(window);
    }

    @Test
    public void test20RegionRendererR2T01() throws InterruptedException {
        if(Platform.CPUFamily.X86 != PlatformPropsImpl.CPU_ARCH.family) { // FIXME
            // FIXME: Disabled for now - since it doesn't seem fit for mobile (performance wise).
            System.err.println("disabled on non desktop (x86) arch for now ..");
            return;
        }
        final GLProfile glp = GLProfile.getGL2ES2();

        final GLCapabilities caps = new GLCapabilities(glp);
        //caps.setOnscreen(false);
        caps.setAlphaBits(4);

        final GLWindow window = createWindow("shape-vbaa1-msaa0", caps, 800,400);
        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final GPURegionGLListener02  demo02Listener = new GPURegionGLListener02 (rs, Region.VBAA_RENDERING_BIT, 4, false, false);
        demo02Listener.attachInputListenerTo(window);
        window.addGLEventListener(demo02Listener);

        final RegionGLListener listener = new RegionGLListener(demo02Listener, window.getTitle(), "GPURegionNewtDemo02");
        window.addGLEventListener(listener);

        listener.setTech(-20, 00, -300, 0f, 2);
        window.display();

        listener.setTech(-20, 00, -150, 0f, 3);
        window.display();

        listener.setTech(-20, 00,  -50, 0f, 4);
        window.display();

        destroyWindow(window);
    }

    private static class RegionGLListener implements GLEventListener {
        String winTitle;
        String name;
        GPURendererListenerBase01 impl;

        public RegionGLListener(final GPURendererListenerBase01 impl, final String title, final String name) {
            this.impl = impl;
            this.winTitle = title;
            this.name = name;
        }

        public void setTech(final float xt, final float yt, final int zt, final float angle, final int sampleCount){
            impl.setMatrix(xt, yt, zt, angle, sampleCount);
        }

        public void init(final GLAutoDrawable drawable) {
            impl.init(drawable);
        }

        public void display(final GLAutoDrawable drawable) {
            impl.display(drawable);

            try {
                impl.printScreen(drawable, "./", winTitle, name, false);
            } catch (final GLException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        public void dispose(final GLAutoDrawable drawable) {
            impl.dispose(drawable);

        }

        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
            impl.reshape(drawable, x, y, width, height);

        }
    }
}
