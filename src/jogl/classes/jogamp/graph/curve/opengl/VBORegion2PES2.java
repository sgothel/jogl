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

import java.util.ArrayList;

import javax.media.opengl.GL2ES2;
// FIXME: Subsume GL2GL3.GL_DRAW_FRAMEBUFFER -> GL2ES2.GL_DRAW_FRAMEBUFFER ! 
import javax.media.opengl.GL;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.graph.curve.opengl.shader.AttributeNames;
import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.util.FBObject;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public class VBORegion2PES2  implements Region {
    private int numVertices = 0;
    
    private ArrayList<Triangle> triangles = new ArrayList<Triangle>();
    private ArrayList<Vertex> vertices = new ArrayList<Vertex>();
    private GLArrayDataServer verticeTxtAttr;
    private GLArrayDataServer texCoordTxtAttr;
    private GLArrayDataServer indicesTxt;
    private GLArrayDataServer verticeFboAttr;
    private GLArrayDataServer texCoordFboAttr;
    private GLArrayDataServer indicesFbo;
    
    private boolean flipped = false;
    
    private boolean dirty = true;
    
    private AABBox box;
    private FBObject fbo;

    private PMVMatrix fboPMVMatrix;
    GLUniformData mgl_fboPMVMatrix;
    
    private int tex_width_c = 0;
    private int tex_height_c = 0;
    GLUniformData mgl_ActiveTexture; 
    int activeTexture; // texture engine 0 == GL.GL_TEXTURE0
    
    public VBORegion2PES2(RenderState rs, int textureEngine) {
        fboPMVMatrix = new PMVMatrix();
        mgl_fboPMVMatrix = new GLUniformData(UniformNames.gcu_PMVMatrix, 4, 4, fboPMVMatrix.glGetPMvMatrixf());
        
        activeTexture = GL.GL_TEXTURE0 + textureEngine;
        mgl_ActiveTexture = new GLUniformData(UniformNames.gcu_TextureUnit, textureEngine);
        
        final int initialSize = 256;
        final ShaderState st = rs.getShaderState();
        
        indicesFbo = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialSize, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);                
        indicesFbo.puts((short) 0); indicesFbo.puts((short) 1); indicesFbo.puts((short) 3);
        indicesFbo.puts((short) 1); indicesFbo.puts((short) 2); indicesFbo.puts((short) 3);
        indicesFbo.seal(true);
        
        texCoordFboAttr = GLArrayDataServer.createGLSL(st, AttributeNames.TEXCOORD_ATTR_NAME, 2, 
                                                       GL2ES2.GL_FLOAT, false, initialSize, GL.GL_STATIC_DRAW);
        st.ownAttribute(texCoordFboAttr, true);
        texCoordFboAttr.putf(5); texCoordFboAttr.putf(5);        
        texCoordFboAttr.putf(5); texCoordFboAttr.putf(6);        
        texCoordFboAttr.putf(6); texCoordFboAttr.putf(6);        
        texCoordFboAttr.putf(6); texCoordFboAttr.putf(5);        
        texCoordFboAttr.seal(true);
        
        verticeFboAttr = GLArrayDataServer.createGLSL(st, AttributeNames.VERTEX_ATTR_NAME, 3, 
                                                      GL2ES2.GL_FLOAT, false, initialSize, GL.GL_STATIC_DRAW); 
        st.ownAttribute(verticeFboAttr, true);
        
        
        box = new AABBox();
        
        indicesTxt = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialSize, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);                
        
        verticeTxtAttr = GLArrayDataServer.createGLSL(st, AttributeNames.VERTEX_ATTR_NAME, 3, 
                                                      GL2ES2.GL_FLOAT, false, initialSize, GL.GL_STATIC_DRAW);
        st.ownAttribute(verticeTxtAttr, true);
        
        texCoordTxtAttr = GLArrayDataServer.createGLSL(st, AttributeNames.TEXCOORD_ATTR_NAME, 2, 
                                                       GL2ES2.GL_FLOAT, false, initialSize, GL.GL_STATIC_DRAW);
        st.ownAttribute(texCoordTxtAttr, true);
        
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegion2PES2 Create: " + this);
        }        
    }
    
    public void update(GL2ES2 gl){
        if(!dirty) {
            return; 
        }
        
        // process triangles
        indicesTxt.seal(gl, false);
        indicesTxt.rewind();        
        for(Triangle t:triangles){
            if(t.getVertices()[0].getId() == Integer.MAX_VALUE){
                t.getVertices()[0].setId(numVertices++);
                t.getVertices()[1].setId(numVertices++);
                t.getVertices()[2].setId(numVertices++);
                
                vertices.add(t.getVertices()[0]);
                vertices.add(t.getVertices()[1]);
                vertices.add(t.getVertices()[2]);
                
                indicesTxt.puts((short) t.getVertices()[0].getId());
                indicesTxt.puts((short) t.getVertices()[1].getId());
                indicesTxt.puts((short) t.getVertices()[2].getId());
            }
            else{
                Vertex v1 = t.getVertices()[0];
                Vertex v2 = t.getVertices()[1];
                Vertex v3 = t.getVertices()[2];
                
                indicesTxt.puts((short) v1.getId());
                indicesTxt.puts((short) v2.getId());
                indicesTxt.puts((short) v3.getId());
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
        for(Vertex v:vertices){
            verticeTxtAttr.putf(v.getX());
            if(flipped){
                verticeTxtAttr.putf(-1*v.getY());
            } else {
                verticeTxtAttr.putf(v.getY());
            }
            verticeTxtAttr.putf(v.getZ());
            if(flipped){
                box.resize(v.getX(), -1*v.getY(), v.getZ());
            } else {
                box.resize(v.getX(), v.getY(), v.getZ());
            }
            
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
        
        dirty = false;
        
        // the buffers were disabled, since due to real/fbo switching and other vbo usage
    }
    
    public void render(GL2ES2 gl, RenderState rs, int vp_width, int vp_height, int width) {
        if(vp_width <=0 || vp_height <= 0 || width <= 0){
            renderRegion(gl);
        } else {
            if(width != tex_width_c){
                renderRegion2FBO(gl, rs, width);                
            }
            // System.out.println("Scale: " + matrix.glGetMatrixf().get(1+4*3) +" " + matrix.glGetMatrixf().get(2+4*3));
            renderFBO(gl, rs, vp_width, vp_height);
        }
    }
    
    private void renderFBO(GL2ES2 gl, RenderState rs, int width, int hight) {
        final ShaderState st = rs.getShaderState();
        
        gl.glViewport(0, 0, width, hight);
        
        gl.glEnable(GL2ES2.GL_TEXTURE_2D);        
        /* setback: 
        int[] currentActiveTextureEngine = new int[1];
        gl.glGetIntegerv(GL.GL_ACTIVE_TEXTURE, currentActiveTextureEngine, 0);
        */        
        gl.glActiveTexture(activeTexture);
        st.uniform(gl, mgl_ActiveTexture);
        
        fbo.use(gl);                        
        verticeFboAttr.enableBuffer(gl, true);       
        texCoordFboAttr.enableBuffer(gl, true);
        indicesFbo.enableBuffer(gl, true);
        
        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesFbo.getElementNumber() * indicesFbo.getComponentNumber(), GL2ES2.GL_UNSIGNED_SHORT, 0);         
        
        verticeFboAttr.enableBuffer(gl, false);       
        texCoordFboAttr.enableBuffer(gl, false);
        indicesFbo.enableBuffer(gl, false);        
        fbo.unuse(gl);
        
        // setback: gl.glActiveTexture(currentActiveTextureEngine[0]);
    }
    
    private void renderRegion2FBO(GL2ES2 gl, RenderState rs, int tex_width) {
        final ShaderState st = rs.getShaderState();
        
        tex_width_c = tex_width;        
        tex_height_c = (int)(tex_width_c*box.getHeight()/box.getWidth());
        
        // System.out.println("FBO Size: "+tex_width+" -> "+tex_height_c+"x"+tex_width_c);
        // System.out.println("FBO Scale: " + m.glGetMatrixf().get(0) +" " + m.glGetMatrixf().get(5));

        if(null != fbo && fbo.getWidth() != tex_width_c && fbo.getHeight() != tex_height_c ) {
            fbo.destroy(gl);
            fbo = null;
        }
        
        if(null == fbo) {        
            fbo = new FBObject(tex_width_c, tex_height_c);
            // FIXME: shall not use bilinear, due to own AA ? However, w/o bilinear result is not smooth
            fbo.init(gl, GL2ES2.GL_LINEAR, GL2ES2.GL_LINEAR, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE); 
            // fbo.init(gl, GL2ES2.GL_NEAREST, GL2ES2.GL_NEAREST, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE);
            fbo.attachDepthBuffer(gl, GL.GL_DEPTH_COMPONENT16); // FIXME: or shall we use 24 or 32 bit depth ?
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
    }
    
    private void renderRegion(GL2ES2 gl) {
        verticeTxtAttr.enableBuffer(gl, true);       
        texCoordTxtAttr.enableBuffer(gl, true);
        indicesTxt.enableBuffer(gl, true);        
        
        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesTxt.getElementNumber() * indicesTxt.getComponentNumber(), GL2ES2.GL_UNSIGNED_SHORT, 0);        
        
        verticeTxtAttr.enableBuffer(gl, false);       
        texCoordTxtAttr.enableBuffer(gl, false);
        indicesTxt.enableBuffer(gl, false);        
    }
    
    public void addTriangles(ArrayList<Triangle> tris) {
        triangles.addAll(tris);
        dirty = true;
    }
    
    public int getNumVertices(){
        return numVertices;
    }
    
    public void addVertices(ArrayList<Vertex> verts){
        vertices.addAll(verts);
        numVertices = vertices.size();
        dirty = true;
    }
    
    public boolean isDirty(){
        return dirty;
    }
    
    public void destroy(GL2ES2 gl, RenderState rs) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegion2PES2 Destroy: " + this);
        }
        final ShaderState st = rs.getShaderState();
        if(null != fbo) {
            fbo.destroy(gl);
            fbo = null;
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
    
    public boolean isFlipped() {
        return flipped;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }
    
    public AABBox getBounds(){
        return box;
    }
}
