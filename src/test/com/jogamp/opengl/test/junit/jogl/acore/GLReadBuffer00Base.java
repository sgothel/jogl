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

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLProfile;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.opengl.test.junit.graph.TextRendererGLELBase;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Multiple GLJPanels in a JFrame
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class GLReadBuffer00Base extends UITestCase {

    public static class TextRendererGLEL extends TextRendererGLELBase {
        final Font font = getFont(0, 0, 0);
        public int frameNo = 0;
        public int userCounter = 0;

        public TextRendererGLEL() {
            // FIXME: Graph TextRenderer does not AA well w/o MSAA and FBO
            super(Region.VBAA_RENDERING_BIT);
            texSizeScale = 2;

            staticRGBAColor[0] = 1.0f;
            staticRGBAColor[1] = 1.0f;
            staticRGBAColor[2] = 1.0f;
            staticRGBAColor[3] = 0.99f;
        }

        @Override
        public void display(GLAutoDrawable drawable) {
            final String text = String.format("Frame %04d (%03d): %04dx%04d", frameNo, userCounter, drawable.getWidth(), drawable.getHeight());
            System.err.println("TextRendererGLEL.display: "+text);
            if( null != renderer ) {
                renderString(drawable, font, 24f, text, 0 /* col */, 0 /* row */, 0, 0, -1, false);
            } else {
                System.err.println(text);
            }
            frameNo++;
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
