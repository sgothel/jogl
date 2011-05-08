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
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderState;

public class VBORegionSPES2 extends GLRegion {
    private GLArrayDataServer verticeAttr = null;
    private GLArrayDataServer texCoordAttr = null;
    private GLArrayDataServer indices = null;
        
    protected VBORegionSPES2(int renderModes) { 
        super(renderModes);
    }
    
    protected void update(GL2ES2 gl, RenderState rs) {
        if(!isDirty()) {
            return; 
        }
        
        if(null == indices) {
            final int initialSize = 256;
            final ShaderState st = rs.getShaderState();
            
            indices = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialSize, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
            
            verticeAttr = GLArrayDataServer.createGLSL(st, AttributeNames.VERTEX_ATTR_NAME, 3, 
                                                       GL2ES2.GL_FLOAT, false, initialSize, GL.GL_STATIC_DRAW);         
            st.ownAttribute(verticeAttr, true);
            
            texCoordAttr = GLArrayDataServer.createGLSL(st, AttributeNames.TEXCOORD_ATTR_NAME, 2, 
                                                        GL2ES2.GL_FLOAT, false, initialSize, GL.GL_STATIC_DRAW);
            st.ownAttribute(texCoordAttr, true);
            
            if(DEBUG_INSTANCE) {
                System.err.println("VBORegionSPES2 Create: " + this);
            }
        }
        
        // process triangles
        indices.seal(gl, false);
        indices.rewind();        
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

                indices.puts((short) t_vertices[0].getId());
                indices.puts((short) t_vertices[1].getId());
                indices.puts((short) t_vertices[2].getId());
            } else {
                indices.puts((short) t_vertices[0].getId());
                indices.puts((short) t_vertices[1].getId());
                indices.puts((short) t_vertices[2].getId());
            }
        }
        indices.seal(gl, true);
        indices.enableBuffer(gl, false);
        
        // process vertices and update bbox
        box.reset();
        verticeAttr.seal(gl, false); 
        verticeAttr.rewind();
        texCoordAttr.seal(gl, false);
        texCoordAttr.rewind();
        for(int i=0; i<vertices.size(); i++) {
            final Vertex v = vertices.get(i);
            final float ysign = isFlipped() ? -1.0f : 1.0f ; 
            verticeAttr.putf(        v.getX());
            verticeAttr.putf(ysign * v.getY());
            verticeAttr.putf(        v.getZ());            
            box.resize(v.getX(), ysign*v.getY(), v.getZ());
            
            final float[] tex = v.getTexCoord();
            texCoordAttr.putf(tex[0]);
            texCoordAttr.putf(tex[1]);
        }
        verticeAttr.seal(gl, true);        
        verticeAttr.enableBuffer(gl, false);
        texCoordAttr.seal(gl, true);
        texCoordAttr.enableBuffer(gl, false);
        
        // update all bbox related data: nope
        
        setDirty(false);
        
        // the buffers were disabled, since due to real/fbo switching and other vbo usage
    }
        
    protected void drawImpl(GL2ES2 gl, RenderState rs, int vp_width, int vp_height, int width) {
        verticeAttr.enableBuffer(gl, true);       
        texCoordAttr.enableBuffer(gl, true);
        indices.enableBuffer(gl, true);
        
        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indices.getElementNumber() * indices.getComponentNumber(), GL2ES2.GL_UNSIGNED_SHORT, 0);         
        
        verticeAttr.enableBuffer(gl, false);       
        texCoordAttr.enableBuffer(gl, false);
        indices.enableBuffer(gl, false);
    }    
    
    public final void destroy(GL2ES2 gl, RenderState rs) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 Destroy: " + this);
        }        
        final ShaderState st = rs.getShaderState();
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
        if(null != indices) {
            indices.destroy(gl);
            indices = null;
        }
    }    
}
