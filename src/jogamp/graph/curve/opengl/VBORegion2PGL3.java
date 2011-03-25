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
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.common.nio.Buffers;

import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.PointTex;

import com.jogamp.graph.curve.Region;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public class VBORegion2PGL3  implements Region{
	private int numVertices = 0;
	private IntBuffer vboIds;
	
	private IntBuffer t_vboIds;
	
	private ArrayList<Triangle<PointTex>> triangles = new ArrayList<Triangle<PointTex>>();
	private ArrayList<PointTex> vertices = new ArrayList<PointTex>();
	private GLContext context;
	
	private int numBuffers = 3;
	
	private boolean flipped = false;
	
	private boolean dirty = false;
	
	private AABBox box = null;
	private IntBuffer texture = IntBuffer.allocate(1);
	private IntBuffer fbo = IntBuffer.allocate(1);
	private IntBuffer rbo = IntBuffer.allocate(1);
	private boolean texInitialized = false;

	private int tex_width_c = 0;
	private int tex_height_c = 0;
	
	private ShaderState st;
	
	public VBORegion2PGL3(GLContext context, ShaderState st){
		this.context =context;
		this.st = st;
	}
	
	public void update(){
		box = new AABBox();
		
		GL3 gl = context.getGL().getGL3();
		ShortBuffer indicies = Buffers.newDirectShortBuffer(triangles.size() * 3);
		
		for(Triangle<PointTex> t:triangles){
			if(t.getVertices()[0].getId() == Integer.MAX_VALUE){
				t.getVertices()[0].setId(numVertices++);
				t.getVertices()[1].setId(numVertices++);
				t.getVertices()[2].setId(numVertices++);
				
				vertices.add(t.getVertices()[0]);
				vertices.add(t.getVertices()[1]);
				vertices.add(t.getVertices()[2]);
				
				indicies.put((short) t.getVertices()[0].getId());
				indicies.put((short) t.getVertices()[1].getId());
				indicies.put((short) t.getVertices()[2].getId());
			}
			else{
				PointTex v1 = t.getVertices()[0];
				PointTex v2 = t.getVertices()[1];
				PointTex v3 = t.getVertices()[2];
				
				indicies.put((short) v1.getId());
				indicies.put((short) v2.getId());
				indicies.put((short) v3.getId());
			}
		}
		indicies.rewind();
		
		FloatBuffer verticesBuffer = Buffers.newDirectFloatBuffer(vertices.size() * 3);
		for(PointTex v:vertices){
			verticesBuffer.put(v.getX());
			if(flipped){
				verticesBuffer.put(-1*v.getY());
			}
			else{
				verticesBuffer.put(v.getY());
			}
			verticesBuffer.put(v.getZ());
			
			box.resize(v.getX(), -1*v.getY(), v.getZ());
		}
		verticesBuffer.rewind();
		
		FloatBuffer texCoordBuffer = Buffers.newDirectFloatBuffer(vertices.size() * 2);
		for(PointTex v:vertices){
			float[] tex = v.getTexCoord();
			texCoordBuffer.put(tex[0]);
			texCoordBuffer.put(tex[1]);
		}
		texCoordBuffer.rewind();

		vboIds = IntBuffer.allocate(numBuffers);
		gl.glGenBuffers(numBuffers, vboIds);
		
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds.get(0)); // vertices
		gl.glBufferData(GL3.GL_ARRAY_BUFFER, numVertices * 3 * Buffers.SIZEOF_FLOAT, verticesBuffer, GL3.GL_STATIC_DRAW);
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
		
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds.get(1)); //texture
		gl.glBufferData(GL3.GL_ARRAY_BUFFER, numVertices * 2 * Buffers.SIZEOF_FLOAT, texCoordBuffer, GL3.GL_STATIC_DRAW);
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
		
		gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboIds.get(2)); //triangles
		gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER, triangles.size()* 3 * Buffers.SIZEOF_SHORT, indicies, GL3.GL_STATIC_DRAW);
		gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
		
		dirty = false;
	}
	
	public void render(PMVMatrix matrix, int vp_width, int vp_height, int width){
		if(null == matrix || vp_width <=0 || vp_height <= 0 || width <= 0){
			renderRegion();
		}
		else {
			if(width != tex_width_c){
				texInitialized = false;
				tex_width_c = width;
			}
			if(!texInitialized){
				initFBOTexture(matrix,vp_width, vp_height);
				texInitialized = true;
			}
//			System.out.println("Scale: " + matrix.glGetMatrixf().get(1+4*3) +" " + matrix.glGetMatrixf().get(2+4*3));
			renderTexture(matrix, vp_width, vp_height);
		}
	}
	
	private void renderTexture(PMVMatrix matrix, int width, int hight){
		GL3 gl = context.getGL().getGL3();
	    gl.glViewport(0, 0, width, hight);
	    if(!st.glUniform(gl, new GLUniformData("mgl_PMVMatrix", 4, 4, matrix.glGetPMvMatrixf()))){
	    	System.out.println("Cnt set tex based mat");
	    }
	    gl.glEnable(GL3.GL_TEXTURE_2D);
	    gl.glActiveTexture(GL3.GL_TEXTURE0);
	    gl.glBindTexture(GL3.GL_TEXTURE_2D, texture.get(0));
	    
	    st.glUniform(gl, new GLUniformData("texture", texture.get(0)));
	    int loc = gl.glGetUniformLocation(st.shaderProgram().id(), "texture");
	    gl.glUniform1i(loc, 0);
	    
	    
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, t_vboIds.get(0));
		gl.glEnableVertexAttribArray(VERTEX_POS_INDX);
		gl.glVertexAttribPointer(VERTEX_POS_INDX, 3, GL3.GL_FLOAT, false, 3 * Buffers.SIZEOF_FLOAT, 0);
		
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, t_vboIds.get(1));
		gl.glEnableVertexAttribArray(TEX_COORD);
		gl.glVertexAttribPointer(TEX_COORD, 2, GL3.GL_FLOAT, false, 2 * Buffers.SIZEOF_FLOAT, 0);
		
		gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, t_vboIds.get(2));
		gl.glDrawElements(GL3.GL_TRIANGLES, 2 * 3, GL3.GL_UNSIGNED_SHORT, 0);
		
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
	}
	
	private void setupBoundingBuffers(){
		GL3 gl = context.getGL().getGL3();
		ShortBuffer indicies = Buffers.newDirectShortBuffer(6);
		indicies.put((short) 0); indicies.put((short) 1); indicies.put((short) 3);
		indicies.put((short) 1); indicies.put((short) 2); indicies.put((short) 3);
		indicies.rewind();
		
		FloatBuffer verticesBuffer = Buffers.newDirectFloatBuffer(4 * 3);
		FloatBuffer texCoordBuffer = Buffers.newDirectFloatBuffer(4 * 2);
		
		verticesBuffer.put(box.getLow()[0]);
		verticesBuffer.put(box.getLow()[1]);
		verticesBuffer.put(box.getLow()[2]);
		texCoordBuffer.put(5);
		texCoordBuffer.put(5);
		
		verticesBuffer.put(box.getLow()[0]);
		verticesBuffer.put(box.getHigh()[1]);
		verticesBuffer.put(box.getLow()[2]);
		
		texCoordBuffer.put(5);
		texCoordBuffer.put(6);
		
		verticesBuffer.put(box.getHigh()[0]);
		verticesBuffer.put(box.getHigh()[1]);
		verticesBuffer.put(box.getLow()[2]);
		
		texCoordBuffer.put(6);
		texCoordBuffer.put(6);
		
		verticesBuffer.put(box.getHigh()[0]);
		verticesBuffer.put(box.getLow()[1]);
		verticesBuffer.put(box.getLow()[2]);
		
		texCoordBuffer.put(6);
		texCoordBuffer.put(5);
			
		verticesBuffer.rewind();
		texCoordBuffer.rewind();

		t_vboIds = IntBuffer.allocate(3);
		gl.glGenBuffers(numBuffers, t_vboIds);
		
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, t_vboIds.get(0)); // vertices
		gl.glBufferData(GL3.GL_ARRAY_BUFFER, 4 * 3 * Buffers.SIZEOF_FLOAT, verticesBuffer, GL3.GL_STATIC_DRAW);
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
		
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, t_vboIds.get(1)); //texture
		gl.glBufferData(GL3.GL_ARRAY_BUFFER, 4 * 2 * Buffers.SIZEOF_FLOAT, texCoordBuffer, GL3.GL_STATIC_DRAW);
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
		
		gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, t_vboIds.get(2)); //triangles
		gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER, 4 * 3 * Buffers.SIZEOF_SHORT, indicies, GL3.GL_STATIC_DRAW);
		gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	
	private void initFBOTexture(PMVMatrix m, int width, int hight){
		tex_height_c = (int)(tex_width_c*box.getHeight()/box.getWidth());
		System.out.println("Scale: " + m.glGetMatrixf().get(0) +" " + m.glGetMatrixf().get(5));
		GL3 gl = context.getGL().getGL3();
		
		gl.glDeleteFramebuffers(1, fbo);
		gl.glDeleteTextures(1, texture);
		
		gl.glGenTextures(1, texture);
		gl.glBindTexture(GL3.GL_TEXTURE_2D, texture.get(0));
		gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA, tex_width_c, 
				tex_height_c, 0, GL3.GL_RGBA, GL3.GL_UNSIGNED_BYTE, null);
		
		gl.glTexParameterf(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
		gl.glTexParameterf(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
		gl.glTexParameterf(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameterf(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
		
	    gl.glGenRenderbuffers(1,rbo);
	    gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, rbo.get(0));
	    gl.glRenderbufferStorage(GL3.GL_RENDERBUFFER, GL3.GL_DEPTH_COMPONENT, tex_width_c, tex_height_c);
	    
	    gl.glGenFramebuffers(1, fbo);
	    gl.glBindFramebuffer(GL3.GL_DRAW_FRAMEBUFFER, fbo.get(0));
	    gl.glFramebufferTexture2D(GL3.GL_DRAW_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0, 
	    		GL3.GL_TEXTURE_2D, texture.get(0), 0);
	    gl.glFramebufferRenderbuffer(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_COMPONENT, GL3.GL_RENDERBUFFER, rbo.get(0));

	    int status = gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER);
	    if(status != GL3.GL_FRAMEBUFFER_COMPLETE){
	    	System.out.println("FRAAAAAAAAAAAAAAME");
	    }
	    
	    //render texture
		PMVMatrix tex_matrix = new PMVMatrix();
	    gl.glBindFramebuffer(GL3.GL_DRAW_FRAMEBUFFER, fbo.get(0));
	    gl.glViewport(0, 0, tex_width_c, tex_height_c);
	    tex_matrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
	    tex_matrix.glLoadIdentity();
	    tex_matrix.glOrthof(box.getLow()[0], box.getHigh()[0], box.getLow()[1], box.getHigh()[1], -1, 1);
	    
	    if(!st.glUniform(gl, new GLUniformData("mgl_PMVMatrix", 4, 4, tex_matrix.glGetPMvMatrixf()))){
	    	System.out.println("Cnt set tex based mat");
	    }
	    
	    gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	    gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
	    renderRegion();

	    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
	    gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);
	    
	    setupBoundingBuffers();
	}
	
	private void renderRegion(){
		GL3 gl = context.getGL().getGL3();
		
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds.get(0));
		gl.glEnableVertexAttribArray(VERTEX_POS_INDX);
		gl.glVertexAttribPointer(VERTEX_POS_INDX, 3, GL3.GL_FLOAT, false, 3 * Buffers.SIZEOF_FLOAT, 0);
		
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboIds.get(1));
		gl.glEnableVertexAttribArray(TEX_COORD);
		gl.glVertexAttribPointer(TEX_COORD, 2, GL3.GL_FLOAT, false, 2 * Buffers.SIZEOF_FLOAT, 0);
		
		gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, vboIds.get(2));
		gl.glDrawElements(GL3.GL_TRIANGLES, triangles.size() * 3, GL3.GL_UNSIGNED_SHORT, 0);
		
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
	}
	
	public void addTriangles(ArrayList<Triangle<PointTex>> tris) {
		triangles.addAll(tris);
		dirty = true;
	}
	
	public int getNumVertices(){
		return numVertices;
	}
	
	public void addVertices(ArrayList<PointTex> verts){
		vertices.addAll(verts);
		numVertices = vertices.size();
		dirty = true;
	}
	
	public boolean isDirty(){
		return dirty;
	}
	
	public void destroy() {
		GL3 gl = context.getGL().getGL3();
		gl.glDeleteBuffers(numBuffers, vboIds);
		gl.glDeleteFramebuffers(1, fbo);
		gl.glDeleteTextures(1, texture);
	}
	
	public boolean isFlipped() {
		return flipped;
	}

	public void setFlipped(boolean flipped) {
		this.flipped = flipped;
	}
}
