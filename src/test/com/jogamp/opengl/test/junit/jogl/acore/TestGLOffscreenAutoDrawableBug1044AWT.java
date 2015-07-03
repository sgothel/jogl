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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLOffscreenAutoDrawableBug1044AWT extends UITestCase {

    @Test
    public void test01GLOffscreenDrawable() throws InterruptedException {
        final GLReadBufferUtil readBufferUtilRGB888 = new GLReadBufferUtil(false, false);
        final GLReadBufferUtil readBufferUtilRGBA8888 = new GLReadBufferUtil(true, false);
        final GLDrawableFactory fac = GLDrawableFactory.getFactory(GLProfile.getDefault());
        final GLCapabilities glCap = new GLCapabilities(GLProfile.getMaxFixedFunc(true));
        // Without line below, there is an error on Windows.
        glCap.setDoubleBuffered(false);
        //makes a new buffer 100x100
        final GLDrawable glad = fac.createOffscreenDrawable(null, glCap, null, 100, 100);
        glad.setRealized(true);
        final GLContext context =  glad.createContext(null);
        context.makeCurrent();

        System.err.println("Chosen: "+glad.getChosenGLCapabilities());

        final GL2 gl2 = context.getGL().getGL2();
        gl2.glViewport(0, 0, 100, 100);

        gl2.glShadeModel(GLLightingFunc.GL_SMOOTH);
        gl2.glClearColor(1.0f, 0.80f, 0.80f, 1);    // This Will Clear The Background Color
        gl2.glClearDepth(1.0);                    // Enables Clearing Of The Depth Buffer
        gl2.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl2.glLoadIdentity();                    // Reset The Projection Matrix

        final AWTGLReadBufferUtil agb = new AWTGLReadBufferUtil(glad.getGLProfile(), true);
        final BufferedImage image = agb.readPixelsToBufferedImage(context.getGL(), true);
        try {
            ImageIO.write(image, "PNG", new File(getSimpleTestName(".")+"-AWTImageIO.png"));
        } catch (final IOException e) {
            e.printStackTrace();
        }

        if(readBufferUtilRGB888.readPixels(gl2, false)) {
            readBufferUtilRGB888.write(new File(getSimpleTestName(".")+"-PNGJ-rgb_.png"));
        }
        readBufferUtilRGB888.dispose(gl2);
        if(readBufferUtilRGBA8888.readPixels(gl2, false)) {
            readBufferUtilRGBA8888.write(new File(getSimpleTestName(".")+"-PNGJ-rgba.png"));
        }
        readBufferUtilRGBA8888.dispose(gl2);

        context.destroy();
        glad.setRealized(false);
        System.out.println("Done!");
    }

    public static void main(final String[] args) {
        org.junit.runner.JUnitCore.main(TestGLOffscreenAutoDrawableBug1044AWT.class.getName());
    }
}

