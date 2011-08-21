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
package com.jogamp.opengl.test.junit.jogl.acore;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Luz, et.al.
 */
public class TestMapBuffer01NEWT extends UITestCase {
    static final boolean DEBUG = false;
    
    @Test
    public void testWriteRead01a() throws InterruptedException {
        ByteBuffer verticiesBB = ByteBuffer.allocate(4*9);
        verticiesBB.order(ByteOrder.nativeOrder());
        testWriteRead01(verticiesBB);
    }
    @Test
    public void testWriteRead01b() throws InterruptedException {
        ByteBuffer verticiesBB = Buffers.newDirectByteBuffer(4*9);
        testWriteRead01(verticiesBB);
    }

    private void testWriteRead01(ByteBuffer verticiesBB) throws InterruptedException {
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createOffscreenWindow(GLProfile.getDefault(), 800, 600, true);
        final GL gl = winctx.context.getGL();

        int[] vertexBuffer = new int[1];
        
        verticiesBB.putFloat(0);
        verticiesBB.putFloat(0.5f);
        verticiesBB.putFloat(0);

        verticiesBB.putFloat(0.5f);
        verticiesBB.putFloat(-0.5f);
        verticiesBB.putFloat(0);

        verticiesBB.putFloat(-0.5f);
        verticiesBB.putFloat(-0.5f);
        verticiesBB.putFloat(0);
        verticiesBB.rewind();
        if(DEBUG) {
            for(int i=0; i < verticiesBB.capacity(); i+=4) {
                System.out.println("java "+i+": "+verticiesBB.getFloat(i));
            }
        }

        gl.glGenBuffers(1, vertexBuffer, 0);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexBuffer[0]);

        // gl.glBufferData(GL3.GL_ARRAY_BUFFER, verticiesBB.capacity(), verticiesBB, GL3.GL_STATIC_READ);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, verticiesBB.capacity(), verticiesBB, GL.GL_STATIC_DRAW);
        
        ByteBuffer bb = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2GL3.GL_READ_ONLY);
        // gl.glUnmapBuffer(GL3.GL_ARRAY_BUFFER);
        if(DEBUG) {
            for(int i=0; i < bb.capacity(); i+=4) {
                System.out.println("gpu "+i+": "+bb.getFloat(i));
            }
        }
        for(int i=0; i < bb.capacity(); i+=4) {
            Assert.assertEquals(verticiesBB.getFloat(i), bb.getFloat(i), 0.0);
        }
        NEWTGLContext.destroyWindow(winctx);
    }
    public static void main(String args[]) throws IOException {
        String tstname = TestMapBuffer01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
}
