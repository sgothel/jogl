/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.gl2es1.GLUgl2es1;

import org.junit.Assume;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Tests for bug 463, where gluScaleImage uses up all system memory. This was due to creating millions of
 * 4-byte direct buffer objects inside the tight loops of Image.fill_image() and Image.empty_image(). Since
 * the JVM apparently can only allocate direct buffer in 4MB chunks, each 4-byte buffer cost us a million times
 * the memory it should have. Changing the constructor of Type_Widget.java back to non-direct buffer (like it
 * was in JOGL 1) solves the problem.
 * @author Wade Walker
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug463ScaleImageMemoryAWT extends UITestCase implements GLEventListener {

    /* @Override */
    public void init(final GLAutoDrawable drawable) {
    }

    /* @Override */
    public void display(final GLAutoDrawable drawable) {
        final int widthin = 559;
        final int heightin = 425;

        final int widthout = 1024;
        final int heightout = 512;

        final int textureInLength = widthin * heightin * 4;
        final int textureOutLength = widthout * heightout * 4;

        final byte[] datain = new byte[textureInLength];
        final byte[] dataout = new byte[textureOutLength];

        final ByteBuffer bufferIn  = ByteBuffer.wrap(datain);
        final ByteBuffer bufferOut = ByteBuffer.wrap(dataout);
        final GLUgl2es1 glu = new GLUgl2es1();
        // in the failing case, the system would run out of memory in here
        glu.gluScaleImage( GL.GL_RGBA,
                           widthin, heightin, GL.GL_UNSIGNED_BYTE, bufferIn,
                           widthout, heightout, GL.GL_UNSIGNED_BYTE, bufferOut );
    }

    /* @Override */
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
    }


    /* @Override */
    public void dispose(final GLAutoDrawable drawable) {
    }

    @Test
    public void test01() throws InterruptedException {
        final GLProfile glprofile = GLProfile.getGL2ES1();
        final GLCapabilities glCapabilities = new GLCapabilities(glprofile);
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
        } catch (final Throwable t) {
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
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
   }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestBug463ScaleImageMemoryAWT.class.getName());
    }
}
