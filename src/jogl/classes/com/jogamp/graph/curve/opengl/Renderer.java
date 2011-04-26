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
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.graph.curve.opengl.RenderStateImpl;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public abstract class Renderer {
    protected static final boolean DEBUG = Region.DEBUG;
    protected static final boolean DEBUG_INSTANCE = Region.DEBUG_INSTANCE;    

    public static RenderState createRenderState(ShaderState st, Vertex.Factory<? extends Vertex> pointFactory, PMVMatrix pmvMatrix) {
        return new RenderStateImpl(st, pointFactory, pmvMatrix);
    }

    public static RenderState createRenderState(ShaderState st, Vertex.Factory<? extends Vertex> pointFactory) {
        return new RenderStateImpl(st, pointFactory);
    }
    
    /**
     * Implementation shall load, compile and link the shader program and leave it active.
     * @param gl
     * @return
     */
    protected abstract boolean initShaderProgram(GL2ES2 gl);
    
    protected abstract void disposeImpl(GL2ES2 gl);
   
    /** 
     * Flushes all cached data
     * @see #destroy(GL2ES2)
     */
    public abstract void flushCache(GL2ES2 gl);
    
    protected final RenderState rs;
    public final int renderType;    
    
    protected int vp_width = 0;
    protected int vp_height = 0;
    
    private boolean vboSupported = false; 
    private boolean initialized = false;
    
    /**
     * @param rs the used {@link RenderState} 
     * @param renderType either {@link com.jogamp.graph.curve.Region#SINGLE_PASS} or {@link com.jogamp.graph.curve.Region#TWO_PASS}
     */
    protected Renderer(RenderState rs, int renderType) {
        this.rs = rs;
        this.renderType = renderType;
    }
    
    public Vertex.Factory<? extends Vertex> getFactory() { return rs.getPointFactory(); }
    
    public final boolean isInitialized() { return initialized; }
    
    public final boolean isVBOSupported() { return vboSupported; }
    
    public final int getRenderType() { return renderType; }
    
    public final int getWidth() { return vp_width; }
    public final int getHeight() { return vp_height; }
    
    /** 
     * Initialize shader and bindings for GPU based rendering bound to the given GL object's GLContext.
     *  
     * Leaves the renderer enabled, ie ShaderState.
     *  
     * @param gl referencing the current GLContext to which the ShaderState is bound to
     * 
     * @return true if succeeded, false otherwise
     */
    public boolean init(GL2ES2 gl) {
        if(initialized){
            if(DEBUG) {
                System.err.println("TextRenderer: Already initialized!");
            }
            return true;
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
            return false;
        }
        
        rs.attachTo(gl);
        
        gl.glEnable(GL2ES2.GL_BLEND);
        gl.glBlendFunc(GL2ES2.GL_SRC_ALPHA, GL2ES2.GL_ONE_MINUS_SRC_ALPHA); // FIXME: alpha blending stage ?
        
        initialized = initShaderProgram(gl);
        if(!initialized) {
            return false;
        }
        
        if(!rs.getShaderState().uniform(gl, rs.getPMVMatrix())) {
            if(DEBUG){
                System.err.println("Error setting PMVMatrix in shader: "+rs.getShaderState());
            }
            return false;
        }
        
        if(!rs.getShaderState().uniform(gl, rs.getSharpness())) {
            if(DEBUG){
                System.err.println("Error setting sharpness in shader: "+rs.getShaderState());
            }
            return false;
        }
                
        if(!rs.getShaderState().uniform(gl, rs.getAlpha())) {
            if(DEBUG){
                System.err.println("Error setting global alpha in shader: "+rs.getShaderState());
            }
            return false;
        }        
        
        if(!rs.getShaderState().uniform(gl, rs.getColorStatic())) {
            if(DEBUG){
                System.err.println("Error setting global color in shader: "+rs.getShaderState());
            }
            return false;
        }        
        
        if(!rs.getShaderState().uniform(gl, rs.getStrength())) {
            System.err.println("Error setting antialias strength in shader: "+rs.getShaderState());
        }
                
        return initialized;
    }

    public void destroy(GL2ES2 gl) {
        if(!initialized){
            if(DEBUG_INSTANCE) {
                System.err.println("TextRenderer: Not initialized!");
            }
            return;
        }
        rs.getShaderState().useProgram(gl, false);
        flushCache(gl);
        disposeImpl(gl);
        rs.destroy(gl);
        initialized = false;        
    }
    
    public final RenderState getRenderState() { return rs; }
    public final ShaderState getShaderState() { return rs.getShaderState(); }
    
    public final void enable(GL2ES2 gl, boolean enable) { 
        rs.getShaderState().useProgram(gl, enable);
    }

    public float getSharpness() {
        return rs.getSharpness().floatValue();
    }
    
    public void setSharpness(GL2ES2 gl, float v) {
        rs.getSharpness().setData(v);
        if(null != gl && rs.getShaderState().inUse()) {
            rs.getShaderState().uniform(gl, rs.getSharpness());
        }
    }

    public float getStrength() {
        return rs.getStrength().floatValue();
    }
    
    public void setStrength(GL2ES2 gl, float v) {
        rs.getStrength().setData(v);
        if(null != gl && rs.getShaderState().inUse()) {
            rs.getShaderState().uniform(gl, rs.getStrength());
        }
    }
    
    public float getAlpha() {
        return rs.getAlpha().floatValue();
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
    
    public final PMVMatrix getMatrix() { return rs.pmvMatrix(); }

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

}