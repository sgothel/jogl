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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.graph.curve.Region;

/**
 * OpenGL {@link Region} renderer
 * <p>
 * All OpenGL related operations regarding {@link Region}s
 * are passed through an instance of this class.
 * </p>
 */
public abstract class RegionRenderer {
    protected static final boolean DEBUG = Region.DEBUG;
    protected static final boolean DEBUG_INSTANCE = Region.DEBUG_INSTANCE;

    public interface GLCallback {
        /**
         * @param gl a current GL object
         * @param renderer {@link RegionRenderer} calling this method.
         */
        void run(GL gl, RegionRenderer renderer);
    }

    /**
     * Default {@link GL#GL_BLEND} <i>enable</i> {@link GLCallback},
     * turning on the {@link GL#GL_BLEND} state and setting up
     * {@link GL#glBlendFunc(int, int) glBlendFunc}({@link GL#GL_SRC_ALPHA}, {@link GL#GL_ONE_MINUS_SRC_ALPHA}).
     * @see #setEnableCallback(GLCallback, GLCallback)
     * @see #enable(GL2ES2, boolean)
     */
    public static final GLCallback defaultBlendEnable = new GLCallback() {
        @Override
        public void run(final GL gl, final RegionRenderer args) {
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendEquation(GL.GL_FUNC_ADD); // default
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        }
    };

    /**
     * Default {@link GL#GL_BLEND} <i>disable</i> {@link GLCallback},
     * simply turning off the {@link GL#GL_BLEND} state.
     * @see #setEnableCallback(GLCallback, GLCallback)
     * @see #enable(GL2ES2, boolean)
     */
    public static final GLCallback defaultBlendDisable = new GLCallback() {
        @Override
        public void run(final GL gl, final RegionRenderer args) {
            gl.glDisable(GL.GL_BLEND);
        }
    };

    public static boolean isWeightValid(float v) {
        return 0.0f <= v && v <= 1.9f ;
    }

    /**
     * Create a Hardware accelerated Region Renderer.
     * <p>
     * The optional {@link GLCallback}s <code>enableCallback</code> and <code>disableCallback</code>
     * maybe used to issue certain tasks at {@link #enable(GL2ES2, boolean)}.<br/>
     * For example, instances {@link #defaultBlendEnable} and {@link #defaultBlendDisable}
     * can be utilized to enable and disable {@link GL#GL_BLEND}.
     * </p>
     * @param rs the used {@link RenderState}
     * @param renderModes bit-field of modes, e.g. {@link Region#VARIABLE_CURVE_WEIGHT_BIT}, {@link Region#VBAA_RENDERING_BIT}
     * @param enableCallback optional {@link GLCallback}, if not <code>null</code> will be issued at
     *                       {@link #init(GL2ES2) init(gl)} and {@link #enable(GL2ES2, boolean) enable(gl, true)}.
     * @param disableCallback optional {@link GLCallback}, if not <code>null</code> will be issued at
     *                        {@link #enable(GL2ES2, boolean) enable(gl, false)}.
     * @return an instance of Region Renderer
     * @see #enable(GL2ES2, boolean)
     */
    public static RegionRenderer create(final RenderState rs, final int renderModes,
                                        final GLCallback enableCallback, final GLCallback disableCallback) {
        return new jogamp.graph.curve.opengl.RegionRendererImpl01(rs, renderModes, enableCallback, disableCallback);
    }

    protected final int renderModes;
    protected final RenderState rs;

    protected final GLCallback enableCallback;
    protected final GLCallback disableCallback;

    protected int vp_width;
    protected int vp_height;
    protected boolean initialized;
    private boolean vboSupported = false;

    public final boolean isInitialized() { return initialized; }

    public final int getWidth() { return vp_width; }
    public final int getHeight() { return vp_height; }

    public final float getWeight() { return rs.getWeight().floatValue(); }
    public final float getAlpha() { return rs.getAlpha().floatValue(); }
    public final PMVMatrix getMatrix() { return rs.pmvMatrix(); }

    /**
     * Implementation shall load, compile and link the shader program and leave it active.
     * @param gl referencing the current GLContext to which the ShaderState is bound to
     * @return
     */
    protected abstract boolean initImpl(GL2ES2 gl);

    /** Delete and clean the associated OGL objects */
    protected abstract void destroyImpl(GL2ES2 gl);

    //////////////////////////////////////

    /**
     * @param rs the used {@link RenderState}
     * @param renderModes bit-field of modes
     */
    protected RegionRenderer(final RenderState rs, final int renderModes, final GLCallback enableCallback, final GLCallback disableCallback) {
        this.rs = rs;
        this.renderModes = renderModes;
        this.enableCallback = enableCallback;
        this.disableCallback = disableCallback;
    }

    public final int getRenderModes() {
        return renderModes;
    }

    public final boolean usesVariableCurveWeight() { return Region.isNonUniformWeight(renderModes); }

    /**
     * @return true if Region's renderModes contains all bits as this Renderer's renderModes
     *         except {@link Region#VARIABLE_CURVE_WEIGHT_BIT}, otherwise false.
     */
    public final boolean areRenderModesCompatible(final Region region) {
        final int cleanRenderModes = getRenderModes() & ( Region.VARIABLE_CURVE_WEIGHT_BIT );
        return cleanRenderModes == ( region.getRenderModes() & cleanRenderModes );
    }

    public final boolean isVBOSupported() { return vboSupported; }

    /**
     * Initialize shader and bindings for GPU based rendering bound to the given GL object's GLContext
     * if not initialized yet.
     * <p>Leaves the renderer enabled, ie ShaderState.</p>
     * <p>Shall be called by a {@code draw()} method, e.g. {@link RegionRenderer#draw(GL2ES2, Region, int)}</p>
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

        if( null != enableCallback ) {
            enableCallback.run(gl, this);
        }

        initialized = initImpl(gl);
        if(!initialized) {
            throw new GLException("Shader initialization failed");
        }

        if(!rs.getShaderState().uniform(gl, rs.getPMVMatrix())) {
            throw new GLException("Error setting PMVMatrix in shader: "+rs.getShaderState());
        }

        if( Region.isNonUniformWeight( getRenderModes() ) ) {
            if(!rs.getShaderState().uniform(gl, rs.getWeight())) {
                throw new GLException("Error setting weight in shader: "+rs.getShaderState());
            }
        }

        if(!rs.getShaderState().uniform(gl, rs.getAlpha())) {
            throw new GLException("Error setting global alpha in shader: "+rs.getShaderState());
        }

        if(!rs.getShaderState().uniform(gl, rs.getColorStatic())) {
            throw new GLException("Error setting global color in shader: "+rs.getShaderState());
        }
    }

    public final void destroy(GL2ES2 gl) {
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

    /**
     * Enabling or disabling the {@link RenderState#getShaderState() RenderState}'s
     * {@link ShaderState#useProgram(GL2ES2, boolean) ShaderState program}.
     * <p>
     * In case enable and disable {@link GLCallback}s are setup via {@link #create(RenderState, int, GLCallback, GLCallback)},
     * they will be called before toggling the shader program.
     * </p>
     * @see #create(RenderState, int, GLCallback, GLCallback)
     */
    public final void enable(GL2ES2 gl, boolean enable) {
        if( enable ) {
            if( null != enableCallback ) {
                enableCallback.run(gl, this);
            }
        } else {
            if( null != disableCallback ) {
                disableCallback.run(gl, this);
            }
        }
        rs.getShaderState().useProgram(gl, enable);
    }

    public final void setWeight(GL2ES2 gl, float v) {
        if( !isWeightValid(v) ) {
        	 throw new IllegalArgumentException("Weight out of range");
        }
        rs.getWeight().setData(v);
        if(null != gl && rs.getShaderState().inUse() && Region.isNonUniformWeight( getRenderModes() ) ) {
            rs.getShaderState().uniform(gl, rs.getWeight());
        }
    }

    public final void setAlpha(GL2ES2 gl, float alpha_t) {
        rs.getAlpha().setData(alpha_t);
        if(null != gl && rs.getShaderState().inUse()) {
            rs.getShaderState().uniform(gl, rs.getAlpha());
        }

    }

    public final void getColorStatic(GL2ES2 gl, float[] rgb) {
        FloatBuffer fb = (FloatBuffer) rs.getColorStatic().getBuffer();
        rgb[0] = fb.get(0);
        rgb[1] = fb.get(1);
        rgb[2] = fb.get(2);
    }

    public final void setColorStatic(GL2ES2 gl, float r, float g, float b){
        FloatBuffer fb = (FloatBuffer) rs.getColorStatic().getBuffer();
        fb.put(0, r);
        fb.put(1, g);
        fb.put(2, b);
        if(null != gl && rs.getShaderState().inUse()) {
            rs.getShaderState().uniform(gl, rs.getColorStatic());
        }
    }

    public final void rotate(GL2ES2 gl, float angle, float x, float y, float z) {
        rs.pmvMatrix().glRotatef(angle, x, y, z);
        updateMatrix(gl);
    }

    public final void translate(GL2ES2 gl, float x, float y, float z) {
        rs.pmvMatrix().glTranslatef(x, y, z);
        updateMatrix(gl);
    }

    public final void scale(GL2ES2 gl, float x, float y, float z) {
        rs.pmvMatrix().glScalef(x, y, z);
        updateMatrix(gl);
    }

    public final void resetModelview(GL2ES2 gl) {
        rs.pmvMatrix().glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        rs.pmvMatrix().glLoadIdentity();
        updateMatrix(gl);
    }

    public final void updateMatrix(GL2ES2 gl) {
        if(initialized && null != gl && rs.getShaderState().inUse()) {
            rs.getShaderState().uniform(gl, rs.getPMVMatrix());
        }
    }

    /** No PMVMatrix operation is performed here. PMVMatrix will be updated if gl is not null. */
    public final boolean reshapeNotify(GL2ES2 gl, int width, int height) {
        this.vp_width = width;
        this.vp_height = height;
        updateMatrix(gl);
        return true;
    }

    public final boolean reshapePerspective(GL2ES2 gl, float angle, int width, int height, float near, float far) {
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

    public final boolean reshapeOrtho(GL2ES2 gl, int width, int height, float near, float far) {
        this.vp_width = width;
        this.vp_height = height;
        final PMVMatrix p = rs.pmvMatrix();
        p.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        p.glLoadIdentity();
        p.glOrthof(0, width, 0, height, near, far);
        updateMatrix(gl);
        return true;
    }

    protected String getVertexShaderName() {
        return "curverenderer" + getImplVersion();
    }

    protected String getFragmentShaderName() {
        final String version = getImplVersion();
        final String pass;
        if( Region.isVBAA(renderModes) ) {
            pass = "-2pass_vbaa";
        } else if( Region.isMSAA(renderModes) ) {
            pass = "-2pass_msaa";
        } else {
            pass = "-1pass_norm" ;
        }
        final String weight = Region.isNonUniformWeight(renderModes) ? "-weight" : "" ;
        return "curverenderer" + version + pass + weight;
    }

    // FIXME: Really required to have sampler2D def. precision ? If not, we can drop getFragmentShaderPrecision(..) and use default ShaderCode ..
    public static final String es2_precision_fp = "\nprecision mediump float;\nprecision mediump int;\nprecision mediump sampler2D;\n";

    protected String getFragmentShaderPrecision(GL2ES2 gl) {
        if( gl.isGLES() ) {
            return es2_precision_fp;
        }
        if( ShaderCode.requiresGL3DefaultPrecision(gl) ) {
            return ShaderCode.gl3_default_precision_fp;
        }
        return null;
    }

    protected String getImplVersion() {
        return "01";
    }
}