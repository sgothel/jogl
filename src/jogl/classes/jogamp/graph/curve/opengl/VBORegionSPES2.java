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

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLContext;

import com.jogamp.common.nio.Buffers;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.opengl.util.PMVMatrix;

public class VBORegionSPES2  implements Region{
	private int numVertices = 0;
	private IntBuffer vboIds;
	
	private ArrayList<Triangle> triangles = new ArrayList<Triangle>();
	private ArrayList<Vertex> vertices = new ArrayList<Vertex>();
	
	private GLContext context;
	
	private int numBuffers = 3;
	
	private boolean flipped = false;
	private boolean dirty = false;
	
	public VBORegionSPES2(GLContext context){
		this.context =context;
	}
	
	public void update(){
		GL2ES2 gl = context.getGL().getGL2ES2();
		ShortBuffer indicies = Buffers.newDirectShortBuffer(triangles.size() * 3);
		
		for(Triangle t:triangles){
			final Vertex[] t_vertices = t.getVertices();
			
			if(t_vertices[0].getId() == Integer.MAX_VALUE){
				t_vertices[0].setId(numVertices++);
				t_vertices[1].setId(numVertices++);
				t_vertices[2].setId(numVertices++);
				
				vertices.add(t.getVertices()[0]);
				vertices.add(t.getVertices()[1]);
				vertices.add(t.getVertices()[2]);

				indicies.put((short) t.getVertices()[0].getId());
				indicies.put((short) t.getVertices()[1].getId());
				indicies.put((short) t.getVertices()[2].getId());
			}
			else{
				Vertex v1 = t_vertices[0];
				Vertex v2 = t_vertices[1];
				Vertex v3 = t_vertices[2];
				
				indicies.put((short) v1.getId());
				indicies.put((short) v2.getId());
				indicies.put((short) v3.getId());
			}
		}
		indicies.rewind();
		
		FloatBuffer verticesBuffer = Buffers.newDirectFloatBuffer(vertices.size() * 3);
		for(Vertex v:vertices){
			verticesBuffer.put(v.getX());
			if(flipped){
				verticesBuffer.put(-1*v.getY());
			}
			else{
				verticesBuffer.put(v.getY());
			}
			verticesBuffer.put(v.getZ());
		}
		verticesBuffer.rewind();
		
		FloatBuffer texCoordBuffer = Buffers.newDirectFloatBuffer(vertices.size() * 2);
		for(Vertex v:vertices){
			float[] tex = v.getTexCoord();
			texCoordBuffer.put(tex[0]);
			texCoordBuffer.put(tex[1]);
		}
		texCoordBuffer.rewind();

		vboIds = IntBuffer.allocate(numBuffers);
		gl.glGenBuffers(numBuffers, vboIds);
		
		gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, vboIds.get(0)); // vertices
		gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, numVertices * 3 * Buffers.SIZEOF_FLOAT, verticesBuffer, GL2ES2.GL_STATIC_DRAW);
		gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0);
		
		gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, vboIds.get(1)); //texture
		gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, numVertices * 2 * Buffers.SIZEOF_FLOAT, texCoordBuffer, GL2ES2.GL_STATIC_DRAW);
		gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0);
		
		gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, vboIds.get(2)); //triangles
		gl.glBufferData(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, triangles.size()* 3 * Buffers.SIZEOF_SHORT, indicies, GL2ES2.GL_STATIC_DRAW);
		gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, 0);
		
		dirty = false;
	}
	
	private void render() {
		GL2ES2 gl = context.getGL().getGL2ES2();
		
		gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, vboIds.get(0));
		gl.glEnableVertexAttribArray(VERTEX_ATTR_IDX);
		gl.glVertexAttribPointer(VERTEX_ATTR_IDX, 3, GL2ES2.GL_FLOAT, false, 3 * Buffers.SIZEOF_FLOAT, 0);
		
		gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, vboIds.get(1));
		gl.glEnableVertexAttribArray(TEXCOORD_ATTR_IDX);
		gl.glVertexAttribPointer(TEXCOORD_ATTR_IDX, 2, GL2ES2.GL_FLOAT, false, 2 * Buffers.SIZEOF_FLOAT, 0);
		
		gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, vboIds.get(2));
		gl.glDrawElements(GL2ES2.GL_TRIANGLES, triangles.size() * 3, GL2ES2.GL_UNSIGNED_SHORT, 0);
		
		gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0);
	}
	
	public void render(PMVMatrix matrix, int vp_width, int vp_height, int width){
		render();
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
		GL2ES2 gl = context.getGL().getGL2ES2();
		gl.glDeleteBuffers(numBuffers, vboIds);
	}
	
	public boolean isFlipped() {
		return flipped;
	}

	public void setFlipped(boolean flipped) {
		this.flipped = flipped;
	}
}
