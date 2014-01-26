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

import java.nio.FloatBuffer;

import javax.media.opengl.GL2ES2;
// FIXME: Subsume GL2GL3.GL_DRAW_FRAMEBUFFER -> GL2ES2.GL_DRAW_FRAMEBUFFER !
import javax.media.opengl.GL;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.graph.curve.opengl.shader.AttributeNames;
import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.common.nio.Buffers;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;

import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public class VBORegion2PES2  extends GLRegion {
    private GLArrayDataServer verticeTxtAttr;
    private GLArrayDataServer texCoordTxtAttr;
    private GLArrayDataServer indicesTxt;
    private GLArrayDataServer verticeFboAttr;
    private GLArrayDataServer texCoordFboAttr;
    private GLArrayDataServer indicesFbo;


    private FBObject fbo;
    private TextureAttachment texA;
    private PMVMatrix fboPMVMatrix;
    GLUniformData mgl_fboPMVMatrix;

    private int tex_width_c = 0;
    private int tex_height_c = 0;
    GLUniformData mgl_ActiveTexture;
    GLUniformData mgl_TextureSize; // if GLSL < 1.30

    public VBORegion2PES2(int renderModes, int textureEngine) {
        super(renderModes);
        fboPMVMatrix = new PMVMatrix();
        mgl_fboPMVMatrix = new GLUniformData(UniformNames.gcu_PMVMatrix, 4, 4, fboPMVMatrix.glGetPMvMatrixf());
        mgl_ActiveTexture = new GLUniformData(UniformNames.gcu_TextureUnit, textureEngine);
    }

    @Override
    public void update(GL2ES2 gl, RenderState rs) {
        if(!isDirty()) {
            return;
        }

        if(null == indicesFbo) {
            final int initialElementCount = 256;
            final ShaderState st = rs.getShaderState();

            indicesFbo = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialElementCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
            indicesFbo.puts((short) 0); indicesFbo.puts((short) 1); indicesFbo.puts((short) 3);
            indicesFbo.puts((short) 1); indicesFbo.puts((short) 2); indicesFbo.puts((short) 3);
            indicesFbo.seal(true);

            texCoordFboAttr = GLArrayDataServer.createGLSL(AttributeNames.TEXCOORD_ATTR_NAME, 2, GL2ES2.GL_FLOAT,
                                                           false, initialElementCount, GL.GL_STATIC_DRAW);
            st.ownAttribute(texCoordFboAttr, true);
            texCoordFboAttr.putf(5); texCoordFboAttr.putf(5);
            texCoordFboAttr.putf(5); texCoordFboAttr.putf(6);
            texCoordFboAttr.putf(6); texCoordFboAttr.putf(6);
            texCoordFboAttr.putf(6); texCoordFboAttr.putf(5);
            texCoordFboAttr.seal(true);

            verticeFboAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                          false, initialElementCount, GL.GL_STATIC_DRAW);
            st.ownAttribute(verticeFboAttr, true);


            indicesTxt = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialElementCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);

            verticeTxtAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                          false, initialElementCount, GL.GL_STATIC_DRAW);
            st.ownAttribute(verticeTxtAttr, true);

            texCoordTxtAttr = GLArrayDataServer.createGLSL(AttributeNames.TEXCOORD_ATTR_NAME, 2, GL2ES2.GL_FLOAT,
                                                           false, initialElementCount, GL.GL_STATIC_DRAW);
            st.ownAttribute(texCoordTxtAttr, true);

            if(DEBUG_INSTANCE) {
                System.err.println("VBORegion2PES2 Create: " + this);
            }
        }
        // process triangles
        indicesTxt.seal(gl, false);
        indicesTxt.rewind();
        for(int i=0; i<triangles.size(); i++) {
            final Triangle t = triangles.get(i);
            final Vertex[] t_vertices = t.getVertices();

            if(t_vertices[0].getId() == Integer.MAX_VALUE){
                t_vertices[0].setId(numVertices++);
                t_vertices[1].setId(numVertices++);
                t_vertices[2].setId(numVertices++);

                vertices.add(t_vertices[0]);
                vertices.add(t_vertices[1]);
                vertices.add(t_vertices[2]);

                indicesTxt.puts((short) t_vertices[0].getId());
                indicesTxt.puts((short) t_vertices[1].getId());
                indicesTxt.puts((short) t_vertices[2].getId());
            } else {
                indicesTxt.puts((short) t_vertices[0].getId());
                indicesTxt.puts((short) t_vertices[1].getId());
                indicesTxt.puts((short) t_vertices[2].getId());
            }
        }
        indicesTxt.seal(gl, true);
        indicesTxt.enableBuffer(gl, false);

        // process vertices and update bbox
        box.reset();
        verticeTxtAttr.seal(gl, false);
        verticeTxtAttr.rewind();
        texCoordTxtAttr.seal(gl, false);
        texCoordTxtAttr.rewind();
        for(int i=0; i<vertices.size(); i++) {
            final Vertex v = vertices.get(i);
            verticeTxtAttr.putf(v.getX());
            verticeTxtAttr.putf(v.getY());
            verticeTxtAttr.putf(v.getZ());
            box.resize(v.getX(), v.getY(), v.getZ());

            final float[] tex = v.getTexCoord();
            texCoordTxtAttr.putf(tex[0]);
            texCoordTxtAttr.putf(tex[1]);
        }
        texCoordTxtAttr.seal(gl, true);
        texCoordTxtAttr.enableBuffer(gl, false);
        verticeTxtAttr.seal(gl, true);
        verticeTxtAttr.enableBuffer(gl, false);

        // update all bbox related data
        verticeFboAttr.seal(gl, false);
        verticeFboAttr.rewind();
        verticeFboAttr.putf(box.getLow()[0]);  verticeFboAttr.putf(box.getLow()[1]);  verticeFboAttr.putf(box.getLow()[2]);
        verticeFboAttr.putf(box.getLow()[0]);  verticeFboAttr.putf(box.getHigh()[1]); verticeFboAttr.putf(box.getLow()[2]);
        verticeFboAttr.putf(box.getHigh()[0]); verticeFboAttr.putf(box.getHigh()[1]); verticeFboAttr.putf(box.getLow()[2]);
        verticeFboAttr.putf(box.getHigh()[0]); verticeFboAttr.putf(box.getLow()[1]);  verticeFboAttr.putf(box.getLow()[2]);
        verticeFboAttr.seal(gl, true);
        verticeFboAttr.enableBuffer(gl, false);

        fboPMVMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        fboPMVMatrix.glLoadIdentity();
        fboPMVMatrix.glOrthof(box.getLow()[0], box.getHigh()[0], box.getLow()[1], box.getHigh()[1], -1, 1);

        // push data 2 GPU ..
        indicesFbo.seal(gl, true);
        indicesFbo.enableBuffer(gl, false);

        setDirty(false);

        // the buffers were disabled, since due to real/fbo switching and other vbo usage
    }

    int[] maxTexSize = new int[] { -1 } ;

    @Override
    protected void drawImpl(GL2ES2 gl, RenderState rs, int vp_width, int vp_height, int[/*1*/] texWidth) {
        if(vp_width <=0 || vp_height <= 0 || null==texWidth || texWidth[0] <= 0){
            renderRegion(gl);
        } else {
            if(0 > maxTexSize[0]) {
                gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, maxTexSize, 0);
            }
            if(texWidth[0] != tex_width_c) {
                if(texWidth[0] > maxTexSize[0]) {
                    texWidth[0] = maxTexSize[0]; // clip to max - write-back user value!
                }
                renderRegion2FBO(gl, rs, texWidth);
            }
            // System.out.println("Scale: " + matrix.glGetMatrixf().get(1+4*3) +" " + matrix.glGetMatrixf().get(2+4*3));
            renderFBO(gl, rs, vp_width, vp_height);
        }
    }

    private void renderFBO(GL2ES2 gl, RenderState rs, int width, int hight) {
        final ShaderState st = rs.getShaderState();

        gl.glViewport(0, 0, width, hight);
        st.uniform(gl, mgl_ActiveTexture);
        gl.glActiveTexture(GL.GL_TEXTURE0 + mgl_ActiveTexture.intValue());
        fbo.use(gl, texA);
        verticeFboAttr.enableBuffer(gl, true);
        texCoordFboAttr.enableBuffer(gl, true);
        indicesFbo.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesFbo.getElementCount() * indicesFbo.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesFbo.bindBuffer(gl, false);
        texCoordFboAttr.enableBuffer(gl, false);
        verticeFboAttr.enableBuffer(gl, false);
        fbo.unuse(gl);

        // setback: gl.glActiveTexture(currentActiveTextureEngine[0]);
    }

    private void renderRegion2FBO(GL2ES2 gl, RenderState rs, int[/*1*/] texWidth) {
        final ShaderState st = rs.getShaderState();

        if(0>=texWidth[0]) {
            throw new IllegalArgumentException("texWidth must be greater than 0: "+texWidth[0]);
        }

        tex_width_c  = texWidth[0];
        tex_height_c = (int) ( ( ( tex_width_c * box.getHeight() ) / box.getWidth() ) + 0.5f );

        // System.out.println("FBO Size: "+texWidth[0]+" -> "+tex_width_c+"x"+tex_height_c);
        // System.out.println("FBO Scale: " + m.glGetMatrixf().get(0) +" " + m.glGetMatrixf().get(5));

        if(null != fbo && fbo.getWidth() != tex_width_c && fbo.getHeight() != tex_height_c ) {
            fbo.reset(gl, tex_width_c, tex_height_c);
        }

        if(null == fbo) {
            fbo = new FBObject();
            fbo.reset(gl, tex_width_c, tex_height_c);
            // FIXME: shall not use bilinear, due to own AA ? However, w/o bilinear result is not smooth
            texA = fbo.attachTexture2D(gl, 0, true, GL2ES2.GL_LINEAR, GL2ES2.GL_LINEAR, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE);
            // texA = fbo.attachTexture2D(gl, 0, GL2ES2.GL_NEAREST, GL2ES2.GL_NEAREST, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE);
            fbo.attachRenderbuffer(gl, Attachment.Type.DEPTH, 24);
        } else {
            fbo.bind(gl);
        }

        //render texture
        gl.glViewport(0, 0, tex_width_c, tex_height_c);
        st.uniform(gl, mgl_fboPMVMatrix); // use orthogonal matrix

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT | GL2ES2.GL_DEPTH_BUFFER_BIT);
        renderRegion(gl);
        fbo.unbind(gl);

        st.uniform(gl, rs.getPMVMatrix()); // switch back to real PMV matrix

        // if( !gl.isGL3() ) {
            // GLSL < 1.30
            if(null == mgl_TextureSize) {
                mgl_TextureSize = new GLUniformData(UniformNames.gcu_TextureSize, 2, Buffers.newDirectFloatBuffer(2));
            }
            final FloatBuffer texSize = (FloatBuffer) mgl_TextureSize.getBuffer();
            texSize.put(0, (float)fbo.getWidth());
            texSize.put(1, (float)fbo.getHeight());
            st.uniform(gl, mgl_TextureSize);
        //}
    }

    private void renderRegion(GL2ES2 gl) {
        verticeTxtAttr.enableBuffer(gl, true);
        texCoordTxtAttr.enableBuffer(gl, true);
        indicesTxt.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesTxt.getElementCount() * indicesTxt.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesTxt.bindBuffer(gl, false);
        texCoordTxtAttr.enableBuffer(gl, false);
        verticeTxtAttr.enableBuffer(gl, false);
    }

    @Override
    public void destroy(GL2ES2 gl, RenderState rs) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegion2PES2 Destroy: " + this);
        }
        final ShaderState st = rs.getShaderState();
        if(null != fbo) {
            fbo.destroy(gl);
            fbo = null;
            texA = null;
        }
        if(null != verticeTxtAttr) {
            st.ownAttribute(verticeTxtAttr, false);
            verticeTxtAttr.destroy(gl);
            verticeTxtAttr = null;
        }
        if(null != texCoordTxtAttr) {
            st.ownAttribute(texCoordTxtAttr, false);
            texCoordTxtAttr.destroy(gl);
            texCoordTxtAttr = null;
        }
        if(null != indicesTxt) {
            indicesTxt.destroy(gl);
            indicesTxt = null;
        }
        if(null != verticeFboAttr) {
            st.ownAttribute(verticeFboAttr, false);
            verticeFboAttr.destroy(gl);
            verticeFboAttr = null;
        }
        if(null != texCoordFboAttr) {
            st.ownAttribute(texCoordFboAttr, false);
            texCoordFboAttr.destroy(gl);
            texCoordFboAttr = null;
        }
        if(null != indicesFbo) {
            indicesFbo.destroy(gl);
            indicesFbo = null;
        }
        triangles.clear();
        vertices.clear();
    }
}
