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
import javax.media.opengl.GLException;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.graph.curve.text.GlyphString;
import jogamp.graph.font.FontInt;
import jogamp.graph.font.typecast.TypecastFontFactory;
import jogamp.graph.geom.plane.AffineTransform;
import jogamp.graph.geom.plane.Path2D;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.opengl.SVertex;
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
	
	private ShaderState st = new ShaderState();
	
	private PMVMatrix pmvMatrix = new PMVMatrix();
	private GLUniformData mgl_PMVMatrix;
	
	/**Sharpness is equivalent to the value of t value of texture coord
	 * on the off-curve vertex. The high value of sharpness will 
	 * result in high curvature.
	 */
    private GLUniformData mgl_sharpness = new GLUniformData("p1y", 0.5f);
    private GLUniformData mgl_alpha = new GLUniformData("g_alpha", 1.0f);
    private GLUniformData mgl_color = new GLUniformData("g_color", 3, FloatBuffer.allocate(3));
    private GLUniformData mgl_strength = new GLUniformData("a_strength", 1.8f);
    	
	private boolean initialized = false;	
	
	private int regionType = Region.SINGLE_PASS;
	
	private HashMap<String, GlyphString> strings = new HashMap<String, GlyphString>();
	private final Vertex.Factory<? extends Vertex> pointFactory;
	
	int win_width = 0;
	int win_height = 0;
	
	/** 
	 * Create a Hardware accelerated Text Renderer.
	 * @param context OpenGL rendering context
	 * @param factory optional Point.Factory for Vertex construction. Default is Vertex.Factory.
	 */
	public HwTextRenderer(Vertex.Factory<? extends Vertex> factory, int type) {
		this.pointFactory = (null != factory) ? factory : SVertex.factory();
		this.regionType = type;
	}

    public Font createFont(Vertex.Factory<? extends Vertex> factory, String name, int size) {
    	return fontFactory.createFont(factory, name, size);
    }


    public Font createFont(Vertex.Factory<? extends Vertex> factory,
    		               String[] families, 
                           String style,
                           String variant,
                           String weight,
                           String size) {
    	return fontFactory.createFont(factory, families, style, variant, weight, size);
    }
	
	/** 
	 * Initialize shaders and bindings for GPU based text Rendering, 
	 * should be called only once.
	 * Leaves the renderer enables, ie ShaderState on.
	 *  
	 * @param drawable the current drawable
	 * @param shapvalue shaprness around the off-curve vertices
	 * @return true if init succeeded, false otherwise
	 */
	public boolean init(GL2ES2 gl){
		if(initialized){
			if(DEBUG) {
				System.err.println("HWTextRenderer: Already initialized!");
			}
			return true;
		}
		
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
		
		gl.glEnable(GL2ES2.GL_BLEND);
		gl.glBlendFunc(GL2ES2.GL_SRC_ALPHA, GL2ES2.GL_ONE_MINUS_SRC_ALPHA);
		
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

        st.attachShaderProgram(gl, sp);
        gl.glBindAttribLocation(sp.id(), 0, "v_position");
        gl.glBindAttribLocation(sp.id(), 1, "texCoord");
		
		st.glUseProgram(gl, true);

		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		pmvMatrix.glLoadIdentity();
		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		pmvMatrix.glLoadIdentity();
		
		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		pmvMatrix.glLoadIdentity();
		resetMatrix(null);
		
		mgl_PMVMatrix = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());		    		
		if(!st.glUniform(gl, mgl_PMVMatrix)) {
			if(DEBUG){
				System.err.println("Error setting PMVMatrix in shader: "+st);
			}
			return false;
		}
		
		if(!st.glUniform(gl, mgl_sharpness)) {
			if(DEBUG){
				System.err.println("Error setting sharpness in shader: "+st);
			}
			return false;
		}
				
		if(!st.glUniform(gl, mgl_alpha)) {
			if(DEBUG){
				System.err.println("Error setting global alpha in shader: "+st);
			}
			return false;
		}		
		
		if(!st.glUniform(gl, mgl_color)) {
			if(DEBUG){
				System.err.println("Error setting global color in shader: "+st);
			}
			return false;
		}		
		
		if(!st.glUniform(gl, mgl_strength)) {
			System.err.println("Error setting antialias strength in shader: "+st);
		}
		
		if(DEBUG) {
			System.err.println("HWTextRenderer initialized: " + Thread.currentThread()+" "+st);
		}
		initialized = true;
		return true;
	}
	
	public void dispose(GL2ES2 gl) {
	    st.destroy(gl);
	    clearCached();
	}
	
	public float getAlpha() {
		return mgl_alpha.floatValue();
	}
	
	public ShaderState getShaderState() {
	    return st;
	}
	
	public void setAlpha(GL2ES2 gl, float alpha_t) {
	    mgl_alpha.setData(alpha_t);
	    if(null != gl && st.inUse()) {
	        st.glUniform(gl, mgl_alpha);
	    }
	}
	
	public void setColor(GL2ES2 gl, float r, float g, float b){
	    FloatBuffer fb = (FloatBuffer) mgl_color.getBuffer();
	    fb.put(0, r);
	    fb.put(1, r);
	    fb.put(2, r);
	    if(null != gl && st.inUse()) {
	        st.glUniform(gl, mgl_color);
	    }
	}
	
	public void rotate(GL2ES2 gl, float angle, float x, float y, float z){
		pmvMatrix.glRotatef(angle, x, y, z);
		if(null != gl && st.inUse()) {
		    st.glUniform(gl, mgl_PMVMatrix);
		}
	}
	public void translate(GL2ES2 gl, float x, float y, float z){
		pmvMatrix.glTranslatef(x, y, z);
		if(null != gl && st.inUse()) {
		    st.glUniform(gl, mgl_PMVMatrix);
		}
	}
	
	public  void resetMatrix(GL2ES2 gl){
		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		pmvMatrix.glLoadIdentity();
		if(null != gl && st.inUse()) {
		    st.glUniform(gl, mgl_PMVMatrix);
		}
	}
	
    public  void updateAllShaderValues(GL2ES2 gl) {
        if(null != gl && st.inUse()) {
            st.glUniform(gl, mgl_PMVMatrix);
            st.glUniform(gl, mgl_alpha);
            st.glUniform(gl, mgl_color);
            st.glUniform(gl, mgl_strength);
        }
    }
    
	/**
	 * @param gl
	 * @param angle
	 * @param ratio
	 * @param near
	 * @param far
	 * @return
	 */
	public boolean reshape(GL2ES2 gl, float angle, int width, int height, float near, float far){
		win_width = width;
		win_height = height;
		float ratio = (float)width/(float)height;
		pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		pmvMatrix.glLoadIdentity();
		pmvMatrix.gluPerspective(angle, ratio, near, far);
		
		st.glUniform(gl, mgl_PMVMatrix);
		
		return true;
	}

	private GlyphString createString(GL2ES2 gl, Font font, String str) {
		AffineTransform affineTransform = new AffineTransform(pointFactory);
		
		Path2D[] paths = new Path2D[str.length()];
		((FontInt)font).getOutline(str, affineTransform, paths);
		
		GlyphString glyphString = new GlyphString(pointFactory, font.getName(), str);
		glyphString.createfromFontPath(paths, affineTransform);
		
		glyphString.generateRegion(gl.getContext(), mgl_sharpness.floatValue(), st, regionType);
		return glyphString;
	}
	
	
	public void enable(GL2ES2 gl, boolean enable) {
	    if(null != gl) {
	        st.glUseProgram(gl, enable);
	    }
	}
	
	/** Render the String in 3D space wrt to the font provided at the position provided
	 * the outlines will be generated, if not yet generated
	 * @param font font to be used
	 * @param str text to be rendered 
	 * @param position the lower left corner of the string 
	 * @param size texture size for multipass render
	 * @throws Exception if TextRenderer not initialized
	 */
	public void renderString3D(GL2ES2 gl, Font font, String str, float[] position, int size) {
		if(!initialized){
			throw new GLException("HWTextRenderer: not initialized!");
		}
		String fontStrHash = getTextHashCode(font, str);
		GlyphString glyphString = strings.get(fontStrHash);
		if(null == glyphString) {
			glyphString = createString(gl, font, str);
			strings.put(fontStrHash, glyphString);
		}
		
		glyphString.renderString3D(pmvMatrix, win_width, win_height, size);
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
