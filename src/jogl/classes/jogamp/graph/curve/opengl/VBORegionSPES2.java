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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLContext;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;

public class VBORegionSPES2  implements Region {
	private int numVertices = 0;
	
	private ArrayList<Triangle> triangles = new ArrayList<Triangle>();
	private ArrayList<Vertex> vertices = new ArrayList<Vertex>();
	private GLArrayDataServer verticeAttr = null;
	private GLArrayDataServer texCoordAttr = null;
	private GLArrayDataServer indices = null;
	
	private GLContext context;
	
	private boolean flipped = false;
	private boolean dirty = false;
	
	public VBORegionSPES2(GLContext context){
		this.context =context;
	}
	
	public void update(){
		GL2ES2 gl = context.getGL().getGL2ES2();
		
        destroy(gl);        

        indices = GLArrayDataServer.createGLSL(gl, null, 3, GL2ES2.GL_SHORT, false, 
                                               triangles.size(), GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);        
		for(Triangle t:triangles){
			final Vertex[] t_vertices = t.getVertices();
			
			if(t_vertices[0].getId() == Integer.MAX_VALUE){
				t_vertices[0].setId(numVertices++);
				t_vertices[1].setId(numVertices++);
				t_vertices[2].setId(numVertices++);
								
				vertices.add(t.getVertices()[0]);
				vertices.add(t.getVertices()[1]);
				vertices.add(t.getVertices()[2]);

				indices.puts((short) t.getVertices()[0].getId());
				indices.puts((short) t.getVertices()[1].getId());
				indices.puts((short) t.getVertices()[2].getId());
			}
			else{
				Vertex v1 = t_vertices[0];
				Vertex v2 = t_vertices[1];
				Vertex v3 = t_vertices[2];
				
				indices.puts((short) v1.getId());
				indices.puts((short) v2.getId());
				indices.puts((short) v3.getId());
			}
		}
        indices.seal(gl, true);
		
		verticeAttr = GLArrayDataServer.createGLSL(gl, Region.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT, false,
		                                           vertices.size(), GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER); 
		verticeAttr.setLocation(Region.VERTEX_ATTR_IDX);
		for(Vertex v:vertices){
		    verticeAttr.putf(v.getX());
			if(flipped){
			    verticeAttr.putf(-1*v.getY());
			} else {
			    verticeAttr.putf(v.getY());
			}
			verticeAttr.putf(v.getZ());
		}
        verticeAttr.seal(gl, true);		
        
        texCoordAttr = GLArrayDataServer.createGLSL(gl, Region.TEXCOORD_ATTR_NAME, 2, GL2ES2.GL_FLOAT, false,
                                                    vertices.size(), GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        texCoordAttr.setLocation(Region.TEXCOORD_ATTR_IDX);
		for(Vertex v:vertices){
			float[] tex = v.getTexCoord();
			texCoordAttr.putf(tex[0]);
			texCoordAttr.putf(tex[1]);
		}
		texCoordAttr.seal(gl, true);
        
        verticeAttr.enableBuffer(gl, false);       
        texCoordAttr.enableBuffer(gl, false);
        indices.enableBuffer(gl, false);
        
		dirty = false;
	}
	
	private void render() {
		GL2ES2 gl = context.getGL().getGL2ES2();

        verticeAttr.enableBuffer(gl, true);       
        texCoordAttr.enableBuffer(gl, true);
        indices.enableBuffer(gl, true);
		
		gl.glDrawElements(GL2ES2.GL_TRIANGLES, indices.getElementNumber() * indices.getComponentNumber(), GL2ES2.GL_UNSIGNED_SHORT, 0); 		
		
        verticeAttr.enableBuffer(gl, false);       
        texCoordAttr.enableBuffer(gl, false);
        indices.enableBuffer(gl, false);
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
		destroy(gl);		
	}
		
    final void destroy(GL2ES2 gl) {		
        if(null != verticeAttr) {
            verticeAttr.destroy(gl);
            verticeAttr = null;
        }
        if(null != texCoordAttr) {
            texCoordAttr.destroy(gl);
            texCoordAttr = null;
        }
        if(null != indices) {
            indices.destroy(gl);
            indices = null;
        }
	}
	
	public boolean isFlipped() {
		return flipped;
	}

	public void setFlipped(boolean flipped) {
		this.flipped = flipped;
	}
}
