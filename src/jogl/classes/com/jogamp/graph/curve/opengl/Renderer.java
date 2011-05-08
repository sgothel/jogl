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

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.PMVMatrix;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.geom.Vertex;

public abstract class Renderer {
    protected static final boolean DEBUG = Region.DEBUG;
    protected static final boolean DEBUG_INSTANCE = Region.DEBUG_INSTANCE;

    public static boolean isWeightValid(float v) {
        return 0.0f <= v && v <= 1.9f ;
    }

    protected final int renderModes;
    protected int vp_width;
    protected int vp_height;
    protected boolean initialized;
    protected final RenderState rs;
    private boolean vboSupported = false; 
    
    public final boolean isInitialized() { return initialized; }

    public final int getWidth() { return vp_width; }
    public final int getHeight() { return vp_height; }

    public float getWeight() { return rs.getWeight().floatValue(); }
    public float getAlpha() { return rs.getAlpha().floatValue(); }
    public final PMVMatrix getMatrix() { return rs.pmvMatrix(); }
    
    /**
     * Implementation shall load, compile and link the shader program and leave it active.
     * @param gl referencing the current GLContext to which the ShaderState is bound to
     * @return
     */
    protected abstract boolean initShaderProgram(GL2ES2 gl);
    
    protected abstract void destroyImpl(GL2ES2 gl);
   
    /**
     * @param rs the used {@link RenderState} 
     * @param renderModes bit-field of modes
     */
    protected Renderer(RenderState rs, int renderModes) {
        this.rs = rs;
        this.renderModes = renderModes;
    }
    
    public final int getRenderModes() {
        return renderModes;
    }
    
    public boolean usesVariableCurveWeight() { return Region.usesVariableCurveWeight(renderModes); }

    /**
     * @return true if Region's renderModes contains all bits as this Renderer's renderModes
     *         except {@link Region#VARIABLE_CURVE_WEIGHT_BIT}, otherwise false.
     */
    public final boolean areRenderModesCompatible(Region region) {
        final int cleanRenderModes = getRenderModes() & ( Region.VARIABLE_CURVE_WEIGHT_BIT );
        return cleanRenderModes == ( region.getRenderModes() & cleanRenderModes ); 
    }
    
    public final boolean isVBOSupported() { return vboSupported; }
    
    /** 
     * Initialize shader and bindings for GPU based rendering bound to the given GL object's GLContext
     * if not initialized yet.
     * <p>Leaves the renderer enabled, ie ShaderState.</p>
     * <p>Shall be called by a {@code draw()} method, e.g. {@link RegionRenderer#draw(GL2ES2, Region, float[], int)}</p>
     *  
     * @param gl referencing the current GLContext to which the ShaderState is bound to
     * @throws GLException if initialization failed
     */
    public final void init(GL2ES2 gl) throws GLException {
        if(initialized){
            return;
        }
        vboSupported =  gl.isFunctionAvailable("glGenBuffers") &&
                        gl.isFunctionAvailable("glBindBuffer") &&
                        gl.isFunctionAvailable("glBufferData") &&
                        gl.isFunctionAvailable("glDrawElements") &&
                        gl.isFunctionAvailable("glVertexAttribPointer") &&
                        gl.isFunctionAvailable("glDeleteBuffers");
    
        if(DEBUG) {
            System.err.println("TextRendererImpl01: VBO Supported = " + isVBOSupported());
        }
        
        if(!vboSupported){
            throw new GLException("VBO not supported");
        }
        
        rs.attachTo(gl);
        
        gl.glEnable(GL2ES2.GL_BLEND);
        gl.glBlendFunc(GL2ES2.GL_SRC_ALPHA, GL2ES2.GL_ONE_MINUS_SRC_ALPHA); // FIXME: alpha blending stage ?
        
        initialized = initShaderProgram(gl);
        if(!initialized) {
            throw new GLException("Shader initialization failed");
        }
        
        if(!rs.getShaderState().uniform(gl, rs.getPMVMatrix())) {
            throw new GLException("Error setting PMVMatrix in shader: "+rs.getShaderState());
        }
        
        if(!rs.getShaderState().uniform(gl, rs.getWeight())) {
            throw new GLException("Error setting weight in shader: "+rs.getShaderState());
        }
                
        if(!rs.getShaderState().uniform(gl, rs.getAlpha())) {
            throw new GLException("Error setting global alpha in shader: "+rs.getShaderState());
        }        
        
        if(!rs.getShaderState().uniform(gl, rs.getColorStatic())) {
            throw new GLException("Error setting global color in shader: "+rs.getShaderState());
        }        
    }

    public final void flushCache(GL2ES2 gl) {  
        // FIXME: REMOVE !
    }
        
    public void destroy(GL2ES2 gl) {
        if(!initialized){
            if(DEBUG_INSTANCE) {
                System.err.println("TextRenderer: Not initialized!");
            }
            return;
        }
        rs.getShaderState().useProgram(gl, false);
        destroyImpl(gl);
        rs.destroy(gl);
        initialized = false;        
    }
    
    public final RenderState getRenderState() { return rs; }
    public final ShaderState getShaderState() { return rs.getShaderState(); }
    
    public final void enable(GL2ES2 gl, boolean enable) { 
        rs.getShaderState().useProgram(gl, enable);
    }

    public void setWeight(GL2ES2 gl, float v) {
        if( !isWeightValid(v) ) {
        	 throw new IllegalArgumentException("Weight out of range");
        }
        rs.getWeight().setData(v);
        if(null != gl && rs.getShaderState().inUse()) {
            rs.getShaderState().uniform(gl, rs.getWeight());
        }
    }
    
    public void setAlpha(GL2ES2 gl, float alpha_t) {
        rs.getAlpha().setData(alpha_t);
        if(null != gl && rs.getShaderState().inUse()) {
            rs.getShaderState().uniform(gl, rs.getAlpha());
        }

    }

    public void getColorStatic(GL2ES2 gl, float[] rgb) {
        FloatBuffer fb = (FloatBuffer) rs.getColorStatic().getBuffer();
        rgb[0] = fb.get(0); 
        rgb[1] = fb.get(1); 
        rgb[2] = fb.get(2); 
    }
    
    public void setColorStatic(GL2ES2 gl, float r, float g, float b){
        FloatBuffer fb = (FloatBuffer) rs.getColorStatic().getBuffer();
        fb.put(0, r);
        fb.put(1, g);
        fb.put(2, b);
        if(null != gl && rs.getShaderState().inUse()) {
            rs.getShaderState().uniform(gl, rs.getColorStatic());
        }
    }
    
    public void rotate(GL2ES2 gl, float angle, float x, float y, float z) {
        rs.pmvMatrix().glRotatef(angle, x, y, z);
        updateMatrix(gl);
    }

    public void translate(GL2ES2 gl, float x, float y, float z) {
        rs.pmvMatrix().glTranslatef(x, y, z);
        updateMatrix(gl);
    }
    
    public void scale(GL2ES2 gl, float x, float y, float z) {
        rs.pmvMatrix().glScalef(x, y, z);
        updateMatrix(gl);
    }

    public void resetModelview(GL2ES2 gl) {
        rs.pmvMatrix().glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        rs.pmvMatrix().glLoadIdentity();
        updateMatrix(gl);
    }

    public void updateMatrix(GL2ES2 gl) {
        if(initialized && null != gl && rs.getShaderState().inUse()) {
            rs.getShaderState().uniform(gl, rs.getPMVMatrix());
        }
    }

    public boolean reshapePerspective(GL2ES2 gl, float angle, int width, int height, float near, float far) {
        this.vp_width = width;
        this.vp_height = height;
        final float ratio = (float)width/(float)height;
        final PMVMatrix p = rs.pmvMatrix();
        p.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        p.glLoadIdentity();
        p.gluPerspective(angle, ratio, near, far);
        updateMatrix(gl);
        return true;
    }

    public boolean reshapeOrtho(GL2ES2 gl, int width, int height, float near, float far) {
        this.vp_width = width;
        this.vp_height = height;
        final PMVMatrix p = rs.pmvMatrix();
        p.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        p.glLoadIdentity();
        p.glOrthof(0, width, 0, height, near, far);
        updateMatrix(gl);
        return true;        
    }

    protected String getVertexShaderName(GL2ES2 gl) {
        return "curverenderer01" + getShaderGLVersionSuffix(gl);
    }
    
    protected String getFragmentShaderName(GL2ES2 gl) {
        return "curverenderer01" + getShaderGLVersionSuffix(gl);
    }
        
    protected String getShaderGLVersionSuffix(GL2ES2 gl) {
        if(gl.isGLES2()) {
            return "-es2";
        }
        return "-gl2";
    }    
    
}