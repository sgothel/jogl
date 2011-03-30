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
package com.jogamp.graph.curve.opengl;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.RegionFactory;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;

import jogamp.opengl.Debug;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class HwRegionRenderer {
	protected static final boolean DEBUG = Debug.debug("RegionRenderer");
	
	private ShaderState st;
	private PMVMatrix pmvMatrix = new PMVMatrix();
	
	/**Sharpness is equivalent to the value of t value of texture coord
	 * on the off-curve vertex. The high value of sharpness will 
	 * result in high curvature.
	 */
	private float sharpness = 0.5f;
	private float alpha = 1.0f;
	private float strength = 3.0f;
	private boolean initialized = false;
	
	private int regionType = Region.SINGLE_PASS;
	
	private GLContext context;
	private FloatBuffer color = FloatBuffer.allocate(3);
	private HashMap<Integer, Region> regions = new HashMap<Integer, Region>();

	/** Create a Hardware accelerated Region Renderer
	 * @param context OpenGL rendering context
	 * @param factory optional Point.Factory for Vertex construction. Default is Vertex.Factory.
	 */
	public HwRegionRenderer(GLContext context) {
		this.context = context;
		init(context, 0.5f);
	}
	/** Create a Hardware accelerated Region Renderer
	 * @param context OpenGL rendering context
	 * @param type region type (single or multipass)
	 */
	public HwRegionRenderer(GLContext context, int type) {
		this.context = context;
		this.regionType = type;
		init(context, 0.5f);
	}
	
	private boolean init(GLContext context, float sharpvalue){
		if(initialized){
			if(DEBUG) {
				System.err.println("HWRegionRenderer: Already initialized!");
			}
			return true;
		}
		sharpness = sharpvalue;
		
		GL2ES2 gl = context.getGL().getGL2ES2();
		
		boolean VBOsupported = gl.isFunctionAvailable("glGenBuffers") &&
			gl.isFunctionAvailable("glBindBuffer") &&
			gl.isFunctionAvailable("glBufferData") &&
			gl.isFunctionAvailable("glDrawElements") &&
			gl.isFunctionAvailable("glVertexAttribPointer") &&
			gl.isFunctionAvailable("glDeleteBuffers");
		
		if(DEBUG) {
			System.err.println("HWRegionRenderer: VBO Supported = " + VBOsupported);
		}
		
		if(!VBOsupported){
			return false;
		}
		
		gl.setSwapInterval(1);
		
		gl.glEnable(GL2ES2.GL_BLEND);
		gl.glBlendFunc(GL2ES2.GL_SRC_ALPHA, GL2ES2.GL_ONE_MINUS_SRC_ALPHA);
		
		initShader(gl);
		
		st.glUseProgram(gl, true);

		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		pmvMatrix.glLoadIdentity();
		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		pmvMatrix.glLoadIdentity();
		
		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		pmvMatrix.glLoadIdentity();
		resetMatrix();

		if(!st.glUniform(gl, new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()))) {
			if(DEBUG){
				System.err.println("Error setting PMVMatrix in shader: "+st);
			}
			return false;
		}
		if(!st.glUniform(gl, new GLUniformData("p1y", sharpness))) {
			if(DEBUG){
				System.err.println("Error setting sharpness in shader: "+st);
			}
			return false;
		}
		if(!st.glUniform(gl, new GLUniformData("g_alpha", alpha))) {
			if(DEBUG){
				System.err.println("Error setting global alpha in shader: "+st);
			}
			return false;
		}
		if(!st.glUniform(gl, new GLUniformData("g_color", 3, color))) {
			if(DEBUG){
				System.err.println("Error setting global color in shader: "+st);
			}
			return false;
		}
		if(!st.glUniform(gl, new GLUniformData("a_strength", strength))) {
			System.err.println("Error setting antialias strength in shader: "+st);
		}
		st.glUseProgram(gl, false);
		
		if(DEBUG) {
			System.err.println("HWRegionRenderer initialized: " + Thread.currentThread()+" "+st);
		}
		initialized = true;
		return true;
	}
	
	public float getAlpha() {
		return alpha;
	}
	public void setAlpha(float alpha_t) {
		alpha = alpha_t;
	}
	
	public void setColor(float r, float g, float b){
		color.put(r);
		color.put(g);
		color.put(b);
		color.rewind();
	}
	
	public void rotate(float angle, float x, float y, float z){
		pmvMatrix.glRotatef(angle, x, y, z);
	}
	public void translate(float x, float y, float z){
		pmvMatrix.glTranslatef(x, y, z);
	}
	
	public  void resetMatrix(){
		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		pmvMatrix.glLoadIdentity();
	}
	
	/**
	 * @param drawable
	 * @param angle
	 * @param ratio
	 * @param near
	 * @param far
	 * @return
	 */
	public boolean reshape(GLAutoDrawable drawable, float angle, float ratio, float near, float far){
		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		pmvMatrix.glLoadIdentity();
		pmvMatrix.gluPerspective(angle, ratio, near, far);
		
		if(null==st) {
			if(DEBUG){
				System.err.println("HWRegionRenderer: Shader State is null, or not");
			}
			return false;
		}
		GL2ES2 gl = drawable.getGL().getGL2ES2();

		st.glUseProgram(gl, true);
		GLUniformData ud = st.getUniform("mgl_PMVMatrix");
		if(null!=ud) {
			st.glUniform(gl, ud);
		} 
		st.glUseProgram(gl, false);
		return true;
	}

	private void initShader(GL2ES2 gl) {
		ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, HwRegionRenderer.class,
				"shader", "shader/bin", "curverenderer");
		ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, HwRegionRenderer.class,
				"shader", "shader/bin", "curverenderer");

		ShaderProgram sp = new ShaderProgram();
		sp.add(rsVp);
		sp.add(rsFp);

		if(!sp.link(gl, System.err)) {
			throw new GLException("HWRegionRenderer: Couldn't link program: "+sp);
		}

		st = new ShaderState();
		st.attachShaderProgram(gl, sp);
		gl.glBindAttribLocation(sp.id(), 0, "v_position");
		gl.glBindAttribLocation(sp.id(), 1, "texCoord");
	}
	
	private Region createRegion(OutlineShape outlineShape) {
		Region region = RegionFactory.create(context, st, regionType);
		
		outlineShape.transformOutlines(OutlineShape.QUADRATIC_NURBS);
		
		ArrayList<Triangle<Vertex>> triangles = (ArrayList<Triangle<Vertex>>) outlineShape.triangulate(sharpness);
		ArrayList<Vertex> vertices = (ArrayList<Vertex>) outlineShape.getVertices();
		region.addVertices(vertices);
		region.addTriangles(triangles);
		
		region.update();
		return region;
	}
	
	private Region createRegion(OutlineShape[] outlineShapes) {
		Region region = RegionFactory.create(context, st, regionType);
		
		int numVertices = region.getNumVertices();
		
		for(OutlineShape outlineShape:outlineShapes){
			outlineShape.transformOutlines(OutlineShape.QUADRATIC_NURBS);

			ArrayList<Triangle<Vertex>> triangles = outlineShape.triangulate(sharpness);
			region.addTriangles(triangles);
			
			ArrayList<Vertex> vertices = outlineShape.getVertices();
			for(Vertex vert:vertices){
				vert.setId(numVertices++);
			}
			region.addVertices(vertices);
		}
		
		region.update();
		return region;
	}
	
	
	/** Render outline in 3D space at the position provided
	 *  the triangles of the shapes will be generated, if not yet generated
	 * @param outlineShape the OutlineShape to Render.
	 * @param position the initial translation of the outlineShape. 
	 * @throws Exception if HwRegionRenderer not initialized
	 */
	public void renderOutlineShape(OutlineShape outlineShape, float[] position) throws Exception{
		if(!initialized){
			throw new Exception("HWRegionRenderer: not initialized!");
		}
		int hashCode = getHashCode(outlineShape);
		Region region = regions.get(hashCode);
		
		if(null == region) {
			region = createRegion(outlineShape);
			regions.put(hashCode, region);
		}
		
		GL2ES2 gl = context.getGL().getGL2ES2();
		st.glUseProgram(gl, true);
		GLUniformData ud = st.getUniform("mgl_PMVMatrix");
		if(null!=ud) {
			st.glUniform(gl, ud);
		} 
		if(!st.glUniform(gl, new GLUniformData("g_alpha", alpha))) {
			System.err.println("Error setting global alpha in shader: "+st);
		}	
		GLUniformData gcolorUD = st.getUniform("g_color");
		if(null!=gcolorUD) {
			st.glUniform(gl, gcolorUD);
		} 
		if(!st.glUniform(gl, new GLUniformData("a_strength", strength))) {
			System.err.println("Error setting antialias strength in shader: "+st);
		}
		
		region.render(null, 0, 0, 0);
		st.glUseProgram(gl, false);
	}
	
	/** Render a list of Outline shapes combined in one region
	 *  at the position provided the triangles of the 
	 *  shapes will be generated, if not yet generated
	 * @param outlineShapes the list of OutlineShapes to Render.
	 * @param position the initial translation of the outlineShapes.
	 * @throws Exception if HwRegionRenderer not initialized
	 */
	public void renderOutlineShapes(OutlineShape[] outlineShapes, float[] position) throws Exception{
		if(!initialized){
			throw new Exception("HWRegionRenderer: not initialized!");
		}
		
		int hashCode = getHashCode(outlineShapes);
		Region region = regions.get(hashCode);
		
		if(null == region) {
			region = createRegion(outlineShapes);
			regions.put(hashCode, region);
		}
		
		GL2ES2 gl = context.getGL().getGL2ES2();
		st.glUseProgram(gl, true);
		GLUniformData ud = st.getUniform("mgl_PMVMatrix");
		if(null!=ud) {
			st.glUniform(gl, ud);
		} 
		if(!st.glUniform(gl, new GLUniformData("g_alpha", alpha))) {
			System.err.println("Error setting global alpha in shader: "+st);
		}	
		GLUniformData gcolorUD = st.getUniform("g_color");
		if(null!=gcolorUD) {
			st.glUniform(gl, gcolorUD);
		} 
		if(!st.glUniform(gl, new GLUniformData("a_strength", strength))) {
			System.err.println("Error setting antialias strength in shader: "+st);
		}
		region.render(null, 0, 0, 0);
		st.glUseProgram(gl, false);
	}
	
	private int getHashCode(OutlineShape outlineShape){
		return outlineShape.hashCode();
	}
	
	private int getHashCode(OutlineShape[] outlineShapes){
		int hashcode = 0;
		for(OutlineShape outlineShape:outlineShapes){
			hashcode += getHashCode(outlineShape);
		}
		return hashcode;
	}

	/** Clears the cached string curves
	 *  and destorys underlying buffers
	 */
	public void clearCached() {
		Iterator<Region> iterator = regions.values().iterator();
		while(iterator.hasNext()){
			Region region = iterator.next();
			region.destroy();
		}
		regions.clear();
	}
}
