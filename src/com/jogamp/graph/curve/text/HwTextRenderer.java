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
package com.jogamp.graph.curve.text;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.graph.curve.text.GlyphString;
import jogamp.graph.font.typecast.TypecastFontFactory;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.graph.geom.plane.Path2D;
import com.jogamp.graph.geom.Point;
import com.jogamp.graph.geom.PointTex;
import com.jogamp.graph.geom.opengl.Vertex;
import jogamp.opengl.Debug;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class HwTextRenderer {
	protected static final boolean DEBUG = Debug.debug("TextRenderer");
	static final boolean FONTTOOL_CUSTOM = false;
	
	private static FontFactory fontFactory;
	
	static {
		FontFactory _fontFactory = null;
		
		if(FONTTOOL_CUSTOM) {
			_fontFactory = (FontFactory) ReflectionUtil.createInstance("jogamp.graph.font.ttf.TTFFontFactory", HwTextRenderer.class.getClassLoader());
			if(null!=_fontFactory) {
				System.err.println("Using custom font tool");
			}
		}
		if(null==_fontFactory) {
			_fontFactory = new TypecastFontFactory();
		}
		fontFactory = _fontFactory;
	}

	
	public static FontFactory getFontFactory() {
		return fontFactory;
	}
	
	private ShaderState st;
	private PMVMatrix pmvMatrix = new PMVMatrix();
	
	/**Sharpness is equivalent to the value of t value of texture coord
	 * on the off-curve vertex. The high value of sharpness will 
	 * result in high curvature.
	 */
	private float sharpness = 0.5f;
	private float alpha = 1.0f;
	private float strength = 1.8f;
	private boolean initialized = false;
	
	
	private int regionType = Region.SINGLE_PASS;
	private GLContext context;
	private FloatBuffer color = FloatBuffer.allocate(3);
	private HashMap<String, GlyphString> strings = new HashMap<String, GlyphString>();
	private final Point.Factory<? extends PointTex> pointFactory;
	
	int win_width = 0;
	int win_height = 0;
	
	/** Create a Hardware accelerated Text Renderer
	 * @param context OpenGL rendering context
	 * @param factory optional Point.Factory for PointTex construction. Default is Vertex.Factory.
	 */
	public HwTextRenderer(GLContext context, Point.Factory<? extends PointTex> factory, int type) {
		this.pointFactory = (null != factory) ? factory : Vertex.factory();
		this.context = context;
		this.regionType = type;
		init(context, 0.5f);
	}

    public Font createFont(Point.Factory<? extends PointTex> factory, String name, int size) {
    	return fontFactory.createFont(factory, name, size);
    }


    public Font createFont(Point.Factory<? extends PointTex> factory,
    		               String[] families, 
                           String style,
                           String variant,
                           String weight,
                           String size) {
    	return fontFactory.createFont(factory, families, style, variant, weight, size);
    }
	
	/** initialize shaders and bindings for GPU based text Rendering, should 
	 * be called only onceangle
	 * @param drawable the current drawable
	 * @param shapvalue shaprness around the off-curve vertices
	 * @return true if init succeeded, false otherwise
	 */
	private boolean init(GLContext context, float sharpvalue){
		if(initialized){
			if(DEBUG) {
				System.err.println("HWTextRenderer: Already initialized!");
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
			System.err.println("HWTextRenderer: VBO Supported = " + VBOsupported);
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
			System.err.println("HWTextRenderer initialized: " + Thread.currentThread()+" "+st);
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
	public boolean reshape(GLAutoDrawable drawable, float angle, int width, int height, float near, float far){
		win_width = width;
		win_height = height;
		float ratio = (float)width/(float)height;
		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		pmvMatrix.glLoadIdentity();
		pmvMatrix.gluPerspective(angle, ratio, near, far);
		
		if(null==st) {
			if(DEBUG){
				System.err.println("HWTextRenderer: Shader State is null, or not");
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
		ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, HwTextRenderer.class,
				"../shader", "../shader/bin", "curverenderer");
		ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, HwTextRenderer.class,
				"../shader", "../shader/bin", "curverenderer");

		ShaderProgram sp = new ShaderProgram();
		sp.add(rsVp);
		sp.add(rsFp);

		if(!sp.link(gl, System.err)) {
			throw new GLException("HWTextRenderer: Couldn't link program: "+sp);
		}

		st = new ShaderState();
		st.attachShaderProgram(gl, sp);
		gl.glBindAttribLocation(sp.id(), 0, "v_position");
		gl.glBindAttribLocation(sp.id(), 1, "texCoord");
	}
	
	private GlyphString createString(Font font, String str) {
		AffineTransform affineTransform = new AffineTransform(pointFactory);
		
		Path2D[] paths = new Path2D[str.length()];
		font.getOutline(str, affineTransform, paths);
		
		GlyphString glyphString = new GlyphString(pointFactory, font.getName(), str);
		glyphString.createfromFontPath(paths, affineTransform);
		
		glyphString.generateRegion(context, sharpness, st, regionType);
		return glyphString;
	}
	
	
	/** Render the String in 3D space wrt to the font provided at the position provided
	 * the outlines will be generated, if not yet generated
	 * @param font font to be used
	 * @param str text to be rendered 
	 * @param position the lower left corner of the string 
	 * @param size texture size for multipass render
	 * @throws Exception if TextRenderer not initialized
	 */
	public void renderString3D(Font font, String str, float[] position, int size) throws Exception{
		if(!initialized){
			throw new Exception("HWTextRenderer: not initialized!");
		}
		String fontStrHash = getTextHashCode(font, str);
		GlyphString glyphString = strings.get(fontStrHash);
		if(null == glyphString) {
			glyphString = createString(font, str);
			strings.put(fontStrHash, glyphString);
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
		glyphString.renderString3D(pmvMatrix, win_width, win_height, size);
		st.glUseProgram(gl, false);
	}
	
	private String getTextHashCode(Font font, String str){
		return "" + str.hashCode() + font.getSize();
	}

	/** Clears the cached string curves
	 *  and destorys underlying buffers
	 */
	public void clearCached() {
		Iterator<GlyphString> iterator = strings.values().iterator();
		while(iterator.hasNext()){
			GlyphString glyphString = iterator.next();
			glyphString.destroy();
		}
		strings.clear();
	}
}
