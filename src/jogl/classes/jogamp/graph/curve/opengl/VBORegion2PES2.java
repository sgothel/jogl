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
import javax.media.opengl.GLContext;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;

import com.jogamp.graph.curve.Region;
import com.jogamp.opengl.util.FBObject;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public class VBORegion2PES2  implements Region {
    private int numVertices = 0;
    
	private ArrayList<Triangle> triangles = new ArrayList<Triangle>();
	private ArrayList<Vertex> vertices = new ArrayList<Vertex>();
	private GLArrayDataServer verticeTxtAttr = null;
	private GLArrayDataServer texCoordTxtAttr = null;
	private GLArrayDataServer indicesTxt = null;
    private GLArrayDataServer verticeFboAttr = null;
    private GLArrayDataServer texCoordFboAttr = null;
    private GLArrayDataServer indicesFbo = null;
	
	private GLContext context;
	
	private boolean flipped = false;
	
	private boolean dirty = false;
	
	private AABBox box = null;
	private FBObject fbo = null;

	private int tex_width_c = 0;
	private int tex_height_c = 0;
	
	private ShaderState st;
	
	public VBORegion2PES2(GLContext context, ShaderState st){
		this.context =context;
		this.st = st;
		
		GL2ES2 gl = context.getGL().getGL2ES2();
		
        indicesFbo = GLArrayDataServer.createGLSL(gl, null, 3, GL2ES2.GL_SHORT, false, 
                                                  2, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);                
        indicesFbo.puts((short) 0); indicesFbo.puts((short) 1); indicesFbo.puts((short) 3);
        indicesFbo.puts((short) 1); indicesFbo.puts((short) 2); indicesFbo.puts((short) 3);
        indicesFbo.seal(gl, true);
        
        texCoordFboAttr = GLArrayDataServer.createGLSL(gl, Region.TEXCOORD_ATTR_NAME, 2, GL2ES2.GL_FLOAT, false,
                                                       4, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        texCoordFboAttr.setLocation(Region.TEXCOORD_ATTR_IDX);        
        texCoordFboAttr.putf(5); texCoordFboAttr.putf(5);        
        texCoordFboAttr.putf(5); texCoordFboAttr.putf(6);        
        texCoordFboAttr.putf(6); texCoordFboAttr.putf(6);        
        texCoordFboAttr.putf(6); texCoordFboAttr.putf(5);        
        texCoordFboAttr.seal(gl, true);
        
        verticeFboAttr = GLArrayDataServer.createGLSL(gl, Region.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT, false,
                                                      4, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER); 
        verticeFboAttr.setLocation(Region.VERTEX_ATTR_IDX);
        verticeFboAttr.putf(0f); indicesFbo.putf(0f); indicesFbo.putf(0f);
        verticeFboAttr.putf(0f); indicesFbo.putf(0f); indicesFbo.putf(0f);
        verticeFboAttr.putf(0f); indicesFbo.putf(0f); indicesFbo.putf(0f);
        verticeFboAttr.putf(0f); indicesFbo.putf(0f); indicesFbo.putf(0f);
        verticeFboAttr.seal(gl, true);
        
        verticeFboAttr.enableBuffer(gl, false);       
        texCoordFboAttr.enableBuffer(gl, false);
        indicesFbo.enableBuffer(gl, false);
        if(DEBUG) {
            System.err.println("VBORegion2PES2 Create: " + this);
        }        
	}
	
	public void update(){
		GL2ES2 gl = context.getGL().getGL2ES2();
		
        destroyTxtAttr(gl);
        
        box = new AABBox();

        indicesTxt = GLArrayDataServer.createGLSL(gl, null, 3, GL2ES2.GL_SHORT, false, 
                                               triangles.size(), GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);        
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

        verticeTxtAttr = GLArrayDataServer.createGLSL(gl, Region.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT, false,
                                                   vertices.size(), GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER); 
        verticeTxtAttr.setLocation(Region.VERTEX_ATTR_IDX);
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
		}
        verticeTxtAttr.seal(gl, true);     
		
        texCoordTxtAttr = GLArrayDataServer.createGLSL(gl, Region.TEXCOORD_ATTR_NAME, 2, GL2ES2.GL_FLOAT, false,
                                                    vertices.size(), GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        texCoordTxtAttr.setLocation(Region.TEXCOORD_ATTR_IDX);
		for(Vertex v:vertices){
			float[] tex = v.getTexCoord();
			texCoordTxtAttr.putf(tex[0]);
			texCoordTxtAttr.putf(tex[1]);
		}
        texCoordTxtAttr.seal(gl, true);

        // leave the buffers enabled for subsequent render call
        
		dirty = false;
	}
	
	public void render(PMVMatrix matrix, int vp_width, int vp_height, int width){
        GL2ES2 gl = context.getGL().getGL2ES2();
		if(null == matrix || vp_width <=0 || vp_height <= 0 || width <= 0){
			renderRegion(gl);
		} else {
			if(width != tex_width_c){
                renderRegion2FBO(gl, matrix, width);                
                setupBBox2FboAttr(gl);
			}
//			System.out.println("Scale: " + matrix.glGetMatrixf().get(1+4*3) +" " + matrix.glGetMatrixf().get(2+4*3));
			renderFBO(gl, matrix, vp_width, vp_height);
		}
	}
	
	private void renderFBO(GL2ES2 gl, PMVMatrix matrix, int width, int hight) {
	    gl.glViewport(0, 0, width, hight);
	    if(!st.glUniform(gl, new GLUniformData("mgl_PMVMatrix", 4, 4, matrix.glGetPMvMatrixf()))){
	    	System.out.println("Cnt set tex based mat");
	    }
	    gl.glEnable(GL2ES2.GL_TEXTURE_2D);
	    gl.glActiveTexture(GL2ES2.GL_TEXTURE0);
	    fbo.use(gl);
	    
	    st.glUniform(gl, new GLUniformData("texture", fbo.getTextureName()));
	    int loc = gl.glGetUniformLocation(st.shaderProgram().program(), "texture");
	    gl.glUniform1i(loc, 0);
	    
	    
        verticeFboAttr.enableBuffer(gl, true);       
        texCoordFboAttr.enableBuffer(gl, true);
        indicesFbo.enableBuffer(gl, true);
        
        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesFbo.getElementNumber() * indicesFbo.getComponentNumber(), GL2ES2.GL_UNSIGNED_SHORT, 0);         
		
        verticeFboAttr.enableBuffer(gl, false);       
        texCoordFboAttr.enableBuffer(gl, false);
        indicesFbo.enableBuffer(gl, false);
	}
	
	private void setupBBox2FboAttr(GL2ES2 gl){
        verticeFboAttr.seal(gl, false);
        verticeFboAttr.rewind();
        
        verticeFboAttr.putf(box.getLow()[0]);  verticeFboAttr.putf(box.getLow()[1]);  verticeFboAttr.putf(box.getLow()[2]);		
		verticeFboAttr.putf(box.getLow()[0]);  verticeFboAttr.putf(box.getHigh()[1]); verticeFboAttr.putf(box.getLow()[2]);			
		verticeFboAttr.putf(box.getHigh()[0]); verticeFboAttr.putf(box.getHigh()[1]); verticeFboAttr.putf(box.getLow()[2]);				
		verticeFboAttr.putf(box.getHigh()[0]); verticeFboAttr.putf(box.getLow()[1]);  verticeFboAttr.putf(box.getLow()[2]);
		
        verticeFboAttr.seal(gl, true);     
	}
	
	private void renderRegion2FBO(GL2ES2 gl, PMVMatrix m, int tex_width){
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
		PMVMatrix tex_matrix = new PMVMatrix();
	    gl.glViewport(0, 0, tex_width_c, tex_height_c);
	    tex_matrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
	    tex_matrix.glLoadIdentity();
	    tex_matrix.glOrthof(box.getLow()[0], box.getHigh()[0], box.getLow()[1], box.getHigh()[1], -1, 1);
	    
	    if(!st.glUniform(gl, new GLUniformData("mgl_PMVMatrix", 4, 4, tex_matrix.glGetPMvMatrixf()))){
	    	System.out.println("Cnt set tex based mat");
	    }
	    
	    gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	    gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT | GL2ES2.GL_DEPTH_BUFFER_BIT);
	    renderRegion(gl);

	    fbo.unbind(gl);
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
	
	public void destroy() {
	    if(DEBUG) {
	        System.err.println("VBORegion2PES2 Destroy: " + this);
	     }
		GL2ES2 gl = context.getGL().getGL2ES2();
		destroyFbo(gl);
        destroyTxtAttr(gl);
        destroyFboAttr(gl);        
        triangles.clear();
        vertices.clear();        
	}	
	final void destroyFbo(GL2ES2 gl) {
        if(null != fbo) {
            fbo.destroy(gl);
            fbo = null;
        }	    
	}
    final void destroyTxtAttr(GL2ES2 gl) {
        if(null != verticeTxtAttr) {
            verticeTxtAttr.destroy(gl);
            verticeTxtAttr = null;
        }
        if(null != texCoordTxtAttr) {
            texCoordTxtAttr.destroy(gl);
            texCoordTxtAttr = null;
        }
        if(null != indicesTxt) {
            indicesTxt.destroy(gl);
            indicesTxt = null;
        }
	}
    final void destroyFboAttr(GL2ES2 gl) {
        if(null != verticeFboAttr) {
            verticeFboAttr.destroy(gl);
            verticeFboAttr = null;
        }
        if(null != texCoordFboAttr) {
            texCoordFboAttr.destroy(gl);
            texCoordFboAttr = null;
        }
        if(null != indicesFbo) {
            indicesFbo.destroy(gl);
            indicesFbo = null;
        }
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
