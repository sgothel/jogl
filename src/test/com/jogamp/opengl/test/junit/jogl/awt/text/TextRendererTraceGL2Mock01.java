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

package com.jogamp.opengl.test.junit.jogl.awt.text;

import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL2;
import javax.media.opengl.TraceGL2;

import com.jogamp.common.nio.Buffers;

/*
 * Unit tests for Bug464
 * Modified Version of TraceGL2 for unit test TestAWTTextRendererUseVertexArrayBug464. 
 * This class overrides all glFunctions related to VBO's according to 
 * http://code.google.com/p/glextensions/wiki/GL_ARB_vertex_buffer_object:
 *   glBindBuffer (glBindBufferARB)
 *   glDeleteBuffers (glDeleteBuffersARB)
 *   glGenBuffers (glGenBuffersARB)
 *   glIsBuffer (glIsBufferARB)
 *   glBufferData (glBufferDataARB)
 *   glBufferSubData (glBufferSubDataARB)
 *   glGetBufferSubData (glGetBufferSubDataARB)
 *   glMapBuffer (glMapBufferARB)
 *   glUnmapBuffer (glUnmapBufferARB)
 *   glGetBufferParameteriv (glGetBufferParameterivARB)
 *   glGetBufferPointerv (glGetBufferPointervARB)
 * Calls to the overridden methods are logged to the disallowedMethodCalls variable of
 * the GLEventListener instead of being passed to the downstreamGL object.
 * 
 * Other classes related to this test:
 *   TestAWTTextRendererUseVertexArrayBug464
 *   TextRendererGLEventListener01
 */

public class TextRendererTraceGL2Mock01 extends TraceGL2 {
    
    TextRendererGLEventListener01 listener;

    public TextRendererTraceGL2Mock01(GL2 downstreamGL2, PrintStream stream, TextRendererGLEventListener01 listener) {
        super(downstreamGL2, stream);
        this.listener = listener;
    }    

    @Override
    public void glGetBufferSubData(int arg0, long arg1, long arg2, Buffer arg3) {
        listener.disallowedMethodCalled("glGetBufferSubData");
    }

    @Override
    public ByteBuffer glMapBuffer(int arg0, int arg1) {
        listener.disallowedMethodCalled("glMapBuffer");
        return Buffers.newDirectByteBuffer(0);
    }

    @Override
    public void glGetBufferParameteriv(int arg0, int arg1, IntBuffer arg2) {
        listener.disallowedMethodCalled("glGetBufferParameteriv");
    }

    @Override
    public boolean glUnmapBuffer(int arg0) {
        listener.disallowedMethodCalled("glUnmapBuffer");
        return false;
    }
    
    @Override
    public void glGenBuffers(int arg0, IntBuffer arg1) {
        listener.disallowedMethodCalled("glGenBuffers");
    }

    @Override
    public void glGenBuffers(int arg0, int[] arg1, int arg2) {
        listener.disallowedMethodCalled("glGenBuffers");
    }

    @Override
    public boolean glIsBuffer(int arg0) {
        listener.disallowedMethodCalled("glIsBuffer");
        return false;
    }

    @Override
    public void glBindBuffer(int arg0, int arg1) {
        listener.disallowedMethodCalled("glBindBuffer");
    }

    @Override
    public void glDeleteBuffers(int arg0, int[] arg1, int arg2) {
        listener.disallowedMethodCalled("glDeleteBuffers");
    }

    @Override
    public void glBufferSubData(int arg0, long arg1, long arg2, Buffer arg3) {
        listener.disallowedMethodCalled("glBufferSubData");
    }

    @Override
    public void glGetBufferParameteriv(int arg0, int arg1, int[] arg2, int arg3) {
        listener.disallowedMethodCalled("glGetBufferParameteriv");
    }

    @Override
    public void glBufferData(int arg0, long arg1, Buffer arg2, int arg3) {
        listener.disallowedMethodCalled("glBufferData");
    }

}
