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
 
package com.jogamp.opengl.test.junit.glu;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.gl2.GLUgl2;

import org.junit.Test;

/**
 * Tests for bug 463, where gluScaleImage uses up all system memory. This was due to creating millions of
 * 4-byte direct buffer objects inside the tight loops of Image.fill_image() and Image.empty_image(). Since
 * the JVM apparently can only allocate direct buffer in 4MB chunks, each 4-byte buffer cost us a million times
 * the memory it should have. Changing the constructor of Type_Widget.java back to non-direct buffer (like it
 * was in JOGL 1) solves the problem.  
 * @author Wade Walker
 */
public class TestBug463ScaleImageMemoryAWT implements GLEventListener {

    @Override
    public void init(GLAutoDrawable drawable) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        int widthin = 559;
        int heightin = 425;
        
        int widthout = 1024;
        int heightout = 512;
        
        int textureInLength = widthin * heightin * 4;
        int textureOutLength = widthout * heightout * 4;
        
        byte[] datain = new byte[textureInLength];
        byte[] dataout = new byte[textureOutLength];
        
        ByteBuffer bufferIn  = ByteBuffer.wrap(datain);
        ByteBuffer bufferOut = ByteBuffer.wrap(dataout);      
        GLUgl2 glu = new GLUgl2();
        // in the failing case, the system would run out of memory in here
        glu.gluScaleImage( GL.GL_RGBA,
                           widthin, heightin, GL.GL_UNSIGNED_BYTE, bufferIn,
                           widthout, heightout, GL.GL_UNSIGNED_BYTE, bufferOut );
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }


    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Test
    public void test01() {
        Frame frame = new Frame("Test");
        GLProfile glprofile = GLProfile.getDefault();
        GLCapabilities glCapabilities = new GLCapabilities(glprofile);
        final GLCanvas canvas = new GLCanvas(glCapabilities);
        frame.setSize(256, 256);
        frame.add(canvas);
        frame.setVisible( true );
        canvas.addGLEventListener( this );

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                System.exit(0);
            }
        });
        canvas.display();
   }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestBug463ScaleImageMemoryAWT.class.getName());
    }
}
