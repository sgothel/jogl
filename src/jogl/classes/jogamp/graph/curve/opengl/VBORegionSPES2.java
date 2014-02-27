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
package jogamp.graph.curve.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;

import jogamp.graph.curve.opengl.shader.AttributeNames;

import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderState;

public class VBORegionSPES2 extends GLRegion {
    private GLArrayDataServer verticeAttr = null;
    private GLArrayDataServer texCoordAttr = null;
    private GLArrayDataServer indicesBuffer = null;
    private boolean buffersAttached = false;

    public VBORegionSPES2(final int renderModes) {
        super(renderModes);
        final int initialElementCount = 256;
        indicesBuffer = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialElementCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);

        verticeAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                   false, initialElementCount, GL.GL_STATIC_DRAW);

        texCoordAttr = GLArrayDataServer.createGLSL(AttributeNames.TEXCOORD_ATTR_NAME, 2, GL2ES2.GL_FLOAT,
                                                    false, initialElementCount, GL.GL_STATIC_DRAW);
    }

    @Override
    public final void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        indicesBuffer.seal(gl, false);
        indicesBuffer.rewind();
        verticeAttr.seal(gl, false);
        verticeAttr.rewind();
        texCoordAttr.seal(gl, false);
        texCoordAttr.rewind();
    }

    @Override
    public final boolean usesIndices() { return true; }

    @Override
    public final void pushVertex(float[] coords, float[] texParams) {
        verticeAttr.putf(coords[0]);
        verticeAttr.putf(coords[1]);
        verticeAttr.putf(coords[2]);

        texCoordAttr.putf(texParams[0]);
        texCoordAttr.putf(texParams[1]);
    }

    @Override
    public final void pushIndex(int idx) {
        indicesBuffer.puts((short)idx);
    }

    @Override
    public void update(final GL2ES2 gl, final RegionRenderer renderer) {
        if( !buffersAttached ) {
            final ShaderState st = renderer.getShaderState();
            st.ownAttribute(verticeAttr, true);
            st.ownAttribute(texCoordAttr, true);
            buffersAttached = true;
        }
        // seal buffers
        indicesBuffer.seal(gl, true);
        indicesBuffer.enableBuffer(gl, false);
        verticeAttr.seal(gl, true);
        verticeAttr.enableBuffer(gl, false);
        texCoordAttr.seal(gl, true);
        texCoordAttr.enableBuffer(gl, false);
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 idx "+indicesBuffer);
            System.err.println("VBORegionSPES2 ver "+verticeAttr);
            System.err.println("VBORegionSPES2 tex "+texCoordAttr);
        }
    }

    @Override
    protected void drawImpl(final GL2ES2 gl, final RegionRenderer renderer, final int[/*1*/] texWidth) {
        verticeAttr.enableBuffer(gl, true);
        texCoordAttr.enableBuffer(gl, true);
        indicesBuffer.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesBuffer.getElementCount() * indicesBuffer.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesBuffer.bindBuffer(gl, false);
        texCoordAttr.enableBuffer(gl, false);
        verticeAttr.enableBuffer(gl, false);
    }

    @Override
    public void destroy(final GL2ES2 gl, final RegionRenderer renderer) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 Destroy: " + this);
        }
        final ShaderState st = renderer.getShaderState();
        if(null != verticeAttr) {
            st.ownAttribute(verticeAttr, false);
            verticeAttr.destroy(gl);
            verticeAttr = null;
        }
        if(null != texCoordAttr) {
            st.ownAttribute(texCoordAttr, false);
            texCoordAttr.destroy(gl);
            texCoordAttr = null;
        }
        if(null != indicesBuffer) {
            indicesBuffer.destroy(gl);
            indicesBuffer = null;
        }
    }
}
