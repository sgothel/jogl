/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.glu;

import java.awt.Frame;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import org.junit.Assume;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.GLPixelStorageModes;

/**
 * Demonstrates how to use {@link GLBuffers#sizeof(GL, int[], int, int, int, int, int, boolean)}
 * to determine the unpack buffer size for {@link GLU#gluScaleImage(int, int, int, int, java.nio.Buffer, int, int, int, java.nio.Buffer)}.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug694ScaleImageUnpackBufferSizeAWT extends UITestCase implements GLEventListener {

    /* @Override */
    public void init(GLAutoDrawable drawable) {
    }

    /* @Override */
    public void display(GLAutoDrawable drawable) {
        if( !testDone ) {
            testDone = true;

            final GL gl = drawable.getGL();
            final GLU glu = GLU.createGLU(gl);
            testGLUScaleImage(gl, glu, 0); // default 4
            testGLUScaleImage(gl, glu, 1);
            testGLUScaleImage(gl, glu, 4);
            testGLUScaleImage(gl, glu, 8);
        }
    }

    boolean testDone = false;

    private void testGLUScaleImage(GL gl, GLU glu, int unpackAlignment) {
        final GLPixelStorageModes psm = new GLPixelStorageModes();
        if(0 < unpackAlignment) {
            psm.setUnpackAlignment(gl, unpackAlignment);
        }

        final int widthin = 213;
        final int heightin = 213;

        final int widthout = 256;
        final int heightout = 256;

        final int glFormat = GL.GL_LUMINANCE;
        final int glType = GL.GL_UNSIGNED_BYTE;
        final int tmp[] = new int[1];

        final int unpackSizeInLen = GLBuffers.sizeof(gl, tmp, glFormat, glType, widthin, heightin, 1, false);
        final int unpackSizeOutLen = GLBuffers.sizeof(gl, tmp, glFormat, glType, widthout, heightout, 1, false);

        System.err.println("Unpack-Alignment "+unpackAlignment+":  in-size "+unpackSizeInLen);
        System.err.println("Unpack-Alignment "+unpackAlignment+": out-size "+unpackSizeOutLen);

        ByteBuffer bufferIn  = Buffers.newDirectByteBuffer(unpackSizeInLen);
        ByteBuffer bufferOut = Buffers.newDirectByteBuffer(unpackSizeOutLen);

        glu.gluScaleImage( GL.GL_LUMINANCE,
                           widthin, heightin, GL.GL_UNSIGNED_BYTE, bufferIn,
                           widthout, heightout, GL.GL_UNSIGNED_BYTE, bufferOut );

        if(0 < unpackAlignment) {
            psm.restore(gl);
        }
    }

    /* @Override */
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }


    /* @Override */
    public void dispose(GLAutoDrawable drawable) {
    }

    @Test
    public void test01() throws InterruptedException {
        GLProfile glprofile = GLProfile.getDefault();
        GLCapabilities glCapabilities = new GLCapabilities(glprofile);
        final GLCanvas canvas = new GLCanvas(glCapabilities);
        canvas.addGLEventListener( this );

        final Frame frame = new Frame("Test");
        frame.add(canvas);
        frame.setSize(256, 256);
        frame.validate();

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(true);
                }});
        } catch (Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }

        canvas.display();

        Thread.sleep(200);

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.remove(canvas);
                    frame.dispose();
                }});
        } catch (Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
   }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestBug694ScaleImageUnpackBufferSizeAWT.class.getName());
    }
}
