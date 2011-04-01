package com.jogamp.graph.curve.opengl;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.opengl.Debug;

import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public abstract class Renderer {
    protected static final boolean DEBUG = Debug.debug("CurveRenderer");

    protected abstract boolean initImpl(GL2ES2 gl);
    
    protected abstract void disposeImpl(GL2ES2 gl);
   
    /** 
     * Flushes all cached data
     */
    public abstract void flushCache();
    
    public abstract float getAlpha();

    public abstract void setAlpha(GL2ES2 gl, float alpha_t);

    public abstract void setColor(GL2ES2 gl, float r, float g, float b);

    protected final Vertex.Factory<? extends Vertex> pointFactory;    
    protected ShaderState st = new ShaderState();
    protected PMVMatrix pmvMatrix = new PMVMatrix();
    protected GLUniformData mgl_PMVMatrix;
    protected int renderType;
    protected int vp_width = 0;
    protected int vp_height = 0;
    
    private boolean vboSupported = false; 
    private boolean initialized = false;
    
    /**
     * 
     * @param factory
     * @param renderType either {@link com.jogamp.graph.curve.Region#SINGLE_PASS} or {@link com.jogamp.graph.curve.Region#TWO_PASS}
     */
    protected Renderer(Vertex.Factory<? extends Vertex> factory, int renderType) {
        this.renderType = renderType;
        this.pointFactory = (null != factory) ? factory : SVertex.factory();        
    }
    
    public Vertex.Factory<? extends Vertex> getFactory() { return pointFactory; }
    
    public final boolean isInitialized() { return initialized; }
    
    public final boolean isVBOSupported() { return vboSupported; }
    
    public final int getRenderType() { return renderType; }
    
    public final int getWidth() { return vp_width; }
    public final int getHeight() { return vp_height; }
    
    /** 
     * Initialize shaders and bindings for GPU based rendering. 
     * Leaves the renderer enabled, ie ShaderState on.
     *  
     * @param gl the current GL state
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
        
        initialized = initImpl(gl);
        return initialized;
    }

    public void dispose(GL2ES2 gl) {
        if(!initialized){
            if(DEBUG) {
                System.err.println("TextRenderer: Not initialized!");
            }
            return;
        }
        disposeImpl(gl);
        st.destroy(gl);
        flushCache();        
        initialized = false;        
    }
    
    public final ShaderState getShaderState() { return st; }
    
    public final void enable(GL2ES2 gl, boolean enable) { 
        st.glUseProgram(gl, enable);
    }

    public final PMVMatrix getMatrix() { return pmvMatrix; }

    public void rotate(GL2ES2 gl, float angle, float x, float y, float z) {
        pmvMatrix.glRotatef(angle, x, y, z);
        if(initialized && null != gl && st.inUse()) {
            st.glUniform(gl, mgl_PMVMatrix);
        }
    }

    public void translate(GL2ES2 gl, float x, float y, float z) {
        pmvMatrix.glTranslatef(x, y, z);
        if(initialized && null != gl && st.inUse()) {
            st.glUniform(gl, mgl_PMVMatrix);
        }
    }

    public void resetModelview(GL2ES2 gl) {
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        if(initialized && null != gl && st.inUse()) {
            st.glUniform(gl, mgl_PMVMatrix);
        }
    }

    public void updateMatrix(GL2ES2 gl) {
        if(initialized && null != gl && st.inUse()) {
            st.glUniform(gl, mgl_PMVMatrix);
        }
    }

    public boolean reshapePerspective(GL2ES2 gl, float angle, int width, int height, float near, float far) {
        this.vp_width = width;
        this.vp_height = height;
        float ratio = (float)width/(float)height;
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(angle, ratio, near, far);
        
        if(initialized && null != gl) {
            st.glUniform(gl, mgl_PMVMatrix);
        }
        
        return true;
    }

    public boolean reshapeOrtho(GL2ES2 gl, int width, int height, float near, float far) {
        this.vp_width = width;
        this.vp_height = height;
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glOrthof(0, width, 0, height, near, far);
        
        if(initialized && null != gl) {
            st.glUniform(gl, mgl_PMVMatrix);
        }
        
        return true;        
    }

}