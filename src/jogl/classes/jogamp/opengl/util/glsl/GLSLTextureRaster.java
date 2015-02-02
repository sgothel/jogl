/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package jogamp.opengl.util.glsl;

import java.nio.FloatBuffer;

import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLArrayData;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

public class GLSLTextureRaster  {
    private final boolean textureVertFlipped;
    private final int textureUnit;

    private ShaderProgram sp;
    private PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform;
    private GLUniformData activeTexUniform;
    private GLArrayDataServer interleavedVBO;

    public GLSLTextureRaster(final int textureUnit, final boolean textureVertFlipped) {
        this.textureVertFlipped = textureVertFlipped;
        this.textureUnit = textureUnit;
    }

    public int getTextureUnit() { return textureUnit; }

    static final String shaderBasename = "texture01_xxx";
    static final String shaderSrcPath = "../../shader";
    static final String shaderBinPath = "../../shader/bin";

    public void init(final GL2ES2 gl) {
        // Create & Compile the shader objects
        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(),
                                                  shaderSrcPath, shaderBinPath, shaderBasename, true);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(),
                                                  shaderSrcPath, shaderBinPath, shaderBasename, true);
        rsVp.defaultShaderCustomization(gl, true, true);
        rsFp.defaultShaderCustomization(gl, true, true);

        // Create & Link the shader program
        sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);
        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }
        sp.useProgram(gl, true);

        // setup mgl_PMVMatrix
        pmvMatrix = new PMVMatrix();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()); // P, Mv
        if( pmvMatrixUniform.setLocation(gl, sp.program()) < 0 ) {
            throw new GLException("Couldn't locate "+pmvMatrixUniform+" in shader: "+sp);
        }
        gl.glUniform(pmvMatrixUniform);

        activeTexUniform = new GLUniformData("mgl_Texture0", textureUnit);
        if( activeTexUniform.setLocation(gl, sp.program()) < 0 ) {
            throw new GLException("Couldn't locate "+activeTexUniform+" in shader: "+sp);
        }
        gl.glUniform(activeTexUniform);

        final float[] s_quadTexCoords;
        if( textureVertFlipped ) {
            s_quadTexCoords = s_quadTexCoords01;
        } else {
            s_quadTexCoords = s_quadTexCoords00;
        }

        interleavedVBO = GLArrayDataServer.createGLSLInterleaved(3+2, GL.GL_FLOAT, false, 2*4, GL.GL_STATIC_DRAW);
        {
            final GLArrayData vArrayData = interleavedVBO.addGLSLSubArray("mgl_Vertex",        3, GL.GL_ARRAY_BUFFER);
            if( vArrayData.setLocation(gl, sp.program()) < 0 ) {
                throw new GLException("Couldn't locate "+vArrayData+" in shader: "+sp);
            }
            final GLArrayData tArrayData = interleavedVBO.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);
            if( tArrayData.setLocation(gl, sp.program()) < 0 ) {
                throw new GLException("Couldn't locate "+tArrayData+" in shader: "+sp);
            }
            final FloatBuffer ib = (FloatBuffer)interleavedVBO.getBuffer();
            for(int i=0; i<4; i++) {
                ib.put(s_quadVertices,  i*3, 3);
                ib.put(s_quadTexCoords, i*2, 2);
            }
        }
        interleavedVBO.seal(gl, true);
        interleavedVBO.enableBuffer(gl, false);

        sp.useProgram(gl, false);
    }

    public void reshape(final GL2ES2 gl, final int x, final int y, final int width, final int height) {
        if(null != sp) {
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmvMatrix.glLoadIdentity();
            pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 10.0f);

            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmvMatrix.glLoadIdentity();

            sp.useProgram(gl, true);
            gl.glUniform(pmvMatrixUniform);
            sp.useProgram(gl, false);
        }
    }

    public void dispose(final GL2ES2 gl) {
        if(null != pmvMatrixUniform) {
            pmvMatrixUniform = null;
        }
        pmvMatrix=null;
        if(null != interleavedVBO) {
            interleavedVBO.destroy(gl);
            interleavedVBO=null;
        }
        if(null != sp) {
            sp.destroy(gl);
            sp=null;
        }
    }

    public void display(final GL2ES2 gl) {
        if(null != sp) {
            sp.useProgram(gl, true);
            interleavedVBO.enableBuffer(gl, true);

            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

            interleavedVBO.enableBuffer(gl, false);
            sp.useProgram(gl, false);
        }
    }

    private static final float[] s_quadVertices = {
      -1f, -1f, 0f, // LB
       1f, -1f, 0f, // RB
      -1f,  1f, 0f, // LT
       1f,  1f, 0f  // RT
    };
    private static final float[] s_quadTexCoords00 = {
        0f, 0f, // LB
        1f, 0f, // RB
        0f, 1f, // LT
        1f, 1f  // RT
    };
    private static final float[] s_quadTexCoords01 = {
        0f, 1f, // LB
        1f, 1f, // RB
        0f, 0f, // LT
        1f, 0f  // RT
    };
}

