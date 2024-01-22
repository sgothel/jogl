/**
 * Copyright 2010-2024 JogAmp Community. All rights reserved.
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLException;

import jogamp.graph.curve.opengl.shader.AttributeNames;
import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.LongObjectHashMap;
import com.jogamp.graph.curve.Region;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.Frustum;
import com.jogamp.math.util.PMVMatrix4f;

/**
 * OpenGL {@link Region} renderer
 *
 * All {@link Region} rendering operations utilize a RegionRenderer.
 *
 * The RegionRenderer owns its {@link RenderState}, a composition.
 *
 * The RegionRenderer manages and own all used {@link ShaderProgram}s, a composition.
 *
 * At its {@link #destroy(GL2ES2) destruction}, all {@link ShaderProgram}s and its {@link RenderState}
 * will be destroyed and released.
 */
public final class RegionRenderer {
    protected static final boolean DEBUG = Region.DEBUG;
    protected static final boolean DEBUG_ALL_EVENT = Region.DEBUG_ALL_EVENT;
    protected static final boolean DEBUG_INSTANCE = Region.DEBUG_INSTANCE;
    private static final boolean DEBUG_SHADER_MAP = DEBUG;

    /**
     * May be passed to
     * {@link RegionRenderer#create(Vertex.Factory<? extends Vertex>, RenderState, com.jogamp.graph.curve.opengl.RegionRenderer.GLCallback, com.jogamp.graph.curve.opengl.RegionRenderer.GLCallback) RegionRenderer ctor},
     * e.g.
     * <ul>
     *   <li>{@link RegionRenderer#defaultBlendEnable}</li>
     *   <li>{@link RegionRenderer#defaultBlendDisable}</li>
     * </ul>
     */
    public interface GLCallback {
        /**
         * @param gl a current GL object
         * @param renderer {@link RegionRenderer} calling this method.
         */
        void run(GL gl, RegionRenderer renderer);
    }

    /**
     * Default {@link GL#GL_BLEND} <i>enable</i> {@link GLCallback},
     * turning-off depth writing via {@link GL#glDepthMask(boolean)} if {@link RenderState#BITHINT_GLOBAL_DEPTH_TEST_ENABLED} is set
     * and turning-on the {@link GL#GL_BLEND} state.
     * <p>
     * Implementation also sets {@link RegionRenderer#getRenderState() RenderState}'s {@link RenderState#BITHINT_BLENDING_ENABLED blending bit-hint},
     * which will cause {@link GLRegion#draw(GL2ES2, RegionRenderer) GLRegion's draw-method}
     * to set the proper {@link GL#glBlendFuncSeparate(int, int, int, int) blend-function}
     * and the clear-color to <i>transparent-black</i> in case of {@link Region#isTwoPass(int) multipass} FBO rendering.
     * </p>
     * @see #create(Vertex.Factory<? extends Vertex>, RenderState, GLCallback, GLCallback)
     * @see #enable(GL2ES2, boolean)
     */
    public static final GLCallback defaultBlendEnable = new GLCallback() {
        @Override
        public void run(final GL gl, final RegionRenderer renderer) {
            if( renderer.isHintMaskSet(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED) ) {
                gl.glDepthMask(false);
                // gl.glDisable(GL.GL_DEPTH_TEST);
                // gl.glDepthFunc(GL.GL_ALWAYS);
            }
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendEquation(GL.GL_FUNC_ADD); // default
            renderer.setHintMask(RenderState.BITHINT_BLENDING_ENABLED);
        }
    };

    /**
     * Default {@link GL#GL_BLEND} <i>disable</i> {@link GLCallback},
     * simply turning-off the {@link GL#GL_BLEND} state
     * and turning-on depth writing via {@link GL#glDepthMask(boolean)} if {@link RenderState#BITHINT_GLOBAL_DEPTH_TEST_ENABLED} is set.
     * <p>
     * Implementation also clears {@link RegionRenderer#getRenderState() RenderState}'s {@link RenderState#BITHINT_BLENDING_ENABLED blending bit-hint}.
     * </p>
     * @see #create(Vertex.Factory<? extends Vertex>, RenderState, GLCallback, GLCallback)
     * @see #enable(GL2ES2, boolean)
     */
    public static final GLCallback defaultBlendDisable = new GLCallback() {
        @Override
        public void run(final GL gl, final RegionRenderer renderer) {
            renderer.clearHintMask(RenderState.BITHINT_BLENDING_ENABLED);
            gl.glDisable(GL.GL_BLEND);
            if( renderer.isHintMaskSet(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED) ) {
                // gl.glEnable(GL.GL_DEPTH_TEST);
                // gl.glDepthFunc(GL.GL_LESS);
                gl.glDepthMask(true);
            }
        }
    };

    /**
     * Create a hardware accelerated RegionRenderer including its {@link RenderState} composition.
     * <p>
     * The optional {@link GLCallback}s <code>enableCallback</code> and <code>disableCallback</code>
     * maybe used to issue certain tasks at {@link #enable(GL2ES2, boolean)}.<br/>
     * For example, instances {@link #defaultBlendEnable} and {@link #defaultBlendDisable}
     * can be utilized to enable and disable {@link GL#GL_BLEND}.
     * </p>
     * @return an instance of Region Renderer
     * @see #enable(GL2ES2, boolean)
     */
    public static RegionRenderer create() {
        return new RegionRenderer(null, null, null);
    }

    /**
     * Create a hardware accelerated RegionRenderer including its {@link RenderState} composition.
     * <p>
     * The optional {@link GLCallback}s <code>enableCallback</code> and <code>disableCallback</code>
     * maybe used to issue certain tasks at {@link #enable(GL2ES2, boolean)}.<br/>
     * For example, instances {@link #defaultBlendEnable} and {@link #defaultBlendDisable}
     * can be utilized to enable and disable {@link GL#GL_BLEND}.
     * </p>
     * @param enableCallback optional {@link GLCallback}, if not <code>null</code> will be issued at
     *                       {@link #init(GL2ES2) init(gl)} and {@link #enable(GL2ES2, boolean) enable(gl, true)}.
     * @param disableCallback optional {@link GLCallback}, if not <code>null</code> will be issued at
     *                        {@link #enable(GL2ES2, boolean) enable(gl, false)}.
     * @return an instance of Region Renderer
     * @see #enable(GL2ES2, boolean)
     */
    public static RegionRenderer create(final GLCallback enableCallback, final GLCallback disableCallback) {
        return new RegionRenderer(enableCallback, disableCallback);
    }

    /**
     * Create a hardware accelerated RegionRenderer including its {@link RenderState} composition.
     * <p>
     * The optional {@link GLCallback}s <code>enableCallback</code> and <code>disableCallback</code>
     * maybe used to issue certain tasks at {@link #enable(GL2ES2, boolean)}.<br/>
     * For example, instances {@link #defaultBlendEnable} and {@link #defaultBlendDisable}
     * can be utilized to enable and disable {@link GL#GL_BLEND}.
     * </p>
     * @param sharedPMVMatrix optional shared {@link PMVMatrix4f} to be used for the {@link RenderState} composition.
     * @param enableCallback optional {@link GLCallback}, if not <code>null</code> will be issued at
     *                       {@link #init(GL2ES2) init(gl)} and {@link #enable(GL2ES2, boolean) enable(gl, true)}.
     * @param disableCallback optional {@link GLCallback}, if not <code>null</code> will be issued at
     *                        {@link #enable(GL2ES2, boolean) enable(gl, false)}.
     * @return an instance of Region Renderer
     * @see #enable(GL2ES2, boolean)
     */
    public static RegionRenderer create(final PMVMatrix4f sharedPMVMatrix,
                                        final GLCallback enableCallback, final GLCallback disableCallback) {
        return new RegionRenderer(sharedPMVMatrix, enableCallback, disableCallback);
    }

    private final RenderState rs;

    private final GLCallback enableCallback;
    private final GLCallback disableCallback;

    private final Recti viewport = new Recti();
    private boolean initialized;
    private boolean vboSupported = false;

    public final boolean isInitialized() { return initialized; }

    /** Copies the current Rect4i viewport in given target and returns it for chaining. */
    public final Recti getViewport(final Recti target) {
        target.set(viewport);
        return target;
    }
    /** Borrows the current Rect4i viewport w/o copying. */
    public final Recti getViewport() {
        return viewport;
    }
    /** Return width of current viewport */
    public final int getWidth() { return viewport.width(); }
    /** Return height of current viewport */
    public final int getHeight() { return viewport.height(); }

    //////////////////////////////////////

    protected RegionRenderer(final GLCallback enableCallback, final GLCallback disableCallback) {
        this(null, enableCallback, disableCallback);
    }

    protected RegionRenderer(final PMVMatrix4f sharedPMVMatrix,
                             final GLCallback enableCallback, final GLCallback disableCallback)
    {
        this.rs = new RenderState(sharedPMVMatrix);
        this.enableCallback = enableCallback;
        this.disableCallback = disableCallback;
        if( UseShaderPrograms0 ) {
            shaderPrograms0 = new LongObjectHashMap();
            shaderPrograms1 = null;
        } else {
            shaderPrograms0 = null;
            shaderPrograms1 = new HashMap<ShaderKey, ShaderProgram>();
        }
    }

    public final boolean isVBOSupported() { return vboSupported; }

    /**
     * Initialize shader and bindings for GPU based rendering bound to the given GL object's GLContext
     * if not initialized yet.
     * <p>Disables the renderer via {@link #enable(GL2ES2, boolean)} to remove any side-effects, ie ShaderState incl. shader program.</p>
     * <p>Shall be called once before at initialization before a {@code draw()} method, e.g. {@link RegionRenderer#draw(GL2ES2, Region)}</p>
     *
     * @param gl referencing the current GLContext to which the ShaderState is bound to
     * @throws GLException if initialization failed
     */
    public final void init(final GL2ES2 gl) throws GLException {
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

        initialized = true;
        enable(gl, false);
    }

    /** Deletes all {@link ShaderProgram}s and nullifies its references including {@link RenderState#destroy(GL2ES2)}. */
    public final void destroy(final GL2ES2 gl) {
        if(!initialized){
            if(DEBUG_INSTANCE) {
                System.err.println("TextRenderer: Not initialized!");
            }
            return;
        }
        if( UseShaderPrograms0 ) {
            for(final Iterator<LongObjectHashMap.Entry> i = shaderPrograms0.iterator(); i.hasNext(); ) {
                final ShaderProgram sp = (ShaderProgram) i.next().getValue();
                sp.destroy(gl);
            }
            shaderPrograms0.clear();
        } else {
            for(final Iterator<ShaderProgram> i = shaderPrograms1.values().iterator(); i.hasNext(); ) {
                final ShaderProgram sp = i.next();
                sp.destroy(gl);
            }
            shaderPrograms1.clear();
        }
        rs.detachFrom(gl);
        rs.destroy();
        initialized = false;
    }

    /** Return the {@link RenderState} composition. */
    public final RenderState getRenderState() { return rs; }

    //
    // RenderState forwards
    //

    /** Borrow the current {@link PMVMatrix4f}. */
    public final PMVMatrix4f getMatrix() { return rs.getMatrix(); }

    public final float getWeight() { return rs.getWeight(); }

    public final void setWeight(final float v) { rs.setWeight(v); }

    public final Vec4f getColorStatic(final Vec4f rgbaColor) { return rs.getColorStatic(rgbaColor); }

    public final void setColorStatic(final Vec4f rgbaColor){ rs.setColorStatic(rgbaColor); }

    public final void setColorStatic(final float r, final float g, final float b, final float a){ rs.setColorStatic(r, g, b, a); }

    /** Sets pass2 AA-quality rendering value clipped to the range [{@link Region#MIN_AA_QUALITY}..{@link Region#MAX_AA_QUALITY}] for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link Region#VBAA_RENDERING_BIT}. */
    public final int setAAQuality(final int v) { return rs.setAAQuality(v); }
    /** Returns pass2 AA-quality rendering value for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link Region#VBAA_RENDERING_BIT}. */
    public final int getAAQuality() { return rs.getAAQuality(); }

    /** Sets pass2 AA sample count clipped to the range [{@link Region#MIN_AA_SAMPLE_COUNT}..{@link Region#MAX_AA_SAMPLE_COUNT}] for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link #VBAA_RENDERING_BIT} or {@link Region#MSAA_RENDERING_BIT}. */
    public final int setSampleCount(final int v) { return rs.setSampleCount(v); }
    /** Returns pass2 AA sample count for Graph Region AA {@link Region#getRenderModes() render-modes}: {@link #VBAA_RENDERING_BIT} or {@link Region#MSAA_RENDERING_BIT}. */
    public final int getSampleCount() { return rs.getSampleCount(); }

    /** Set the optional clipping {@link Frustum}, which shall be pre-multiplied with the Mv-matrix or null to disable. */
    public final void setClipFrustum(final Frustum clipFrustum) { rs.setClipFrustum(clipFrustum); }
    /** Returns the optional Mv-premultiplied clipping {@link Frustum} or null if unused. */
    public final Frustum getClipFrustum() { return rs.getClipFrustum(); }

    public final boolean isHintMaskSet(final int mask) { return rs.isHintMaskSet(mask); }

    public final void setHintMask(final int mask) { rs.setHintMask(mask); }

    public final void clearHintMask(final int mask) { rs.clearHintMask(mask); }

    /**
     * Enabling or disabling the {@link #getRenderState() RenderState}'s
     * current {@link RenderState#getShaderProgram() shader program}.
     * <p>
     * {@link #useShaderProgram(GL2ES2, int, boolean, TextureSequence)}
     * generates, selects and caches the desired Curve-Graph {@link ShaderProgram}
     * and {@link RenderState#setShaderProgram(GL2ES2, ShaderProgram) sets it current} in the {@link RenderState} composition.
     * </p>
     * <p>
     * In case enable and disable {@link GLCallback}s are setup via {@link #create(Vertex.Factory<? extends Vertex>, RenderState, GLCallback, GLCallback)},
     * they will be called before toggling the shader program.
     * </p>
     * @param gl current GL object
     * @param enable if true enable the current {@link ShaderProgram}, otherwise disable.
     * @see #create(Vertex.Factory<? extends Vertex>, RenderState, GLCallback, GLCallback)
     * @see #useShaderProgram(GL2ES2, int, boolean, TextureSequence)
     * @see RenderState#setShaderProgram(GL2ES2, ShaderProgram)
     * @see RenderState#getShaderProgram()
     */
    public final void enable(final GL2ES2 gl, final boolean enable) {
        enable(gl, enable, enableCallback, disableCallback);
    }

    /**
     * Same as {@link #enable(GL2ES2, boolean)} but allowing to force {@link GLCallback}s off.
     * @param gl current GL object
     * @param enable if true enable the current {@link ShaderProgram}, otherwise disable.
     * @param enableCB explicit {@link GLCallback} for enable
     * @param disableCB explicit {@link GLCallback} for disable
     * @see #enable(GL2ES2, boolean)
     */
    public final void enable(final GL2ES2 gl, final boolean enable, final GLCallback enableCB, final GLCallback disableCB) {
        if( enable ) {
            if( null != enableCB ) {
                enableCB.run(gl, this);
            }
        } else {
            if( null != disableCB ) {
                disableCB.run(gl, this);
            }
            final ShaderProgram sp = rs.getShaderProgram();
            if( null != sp ) {
                sp.useProgram(gl, false);
            }
        }
    }

    /**
     * No PMVMatrix4f operation is performed here.
     */
    public final void reshapeNotify(final int x, final int y, final int width, final int height) {
        viewport.set(x, y, width, height);
    }

    /**
     * Perspective projection, method also calls {@link #reshapeNotify(int, int, int, int)}.
     * @param angle_rad perspective angle in radians
     * @param width viewport width
     * @param height viewport height
     * @param near projection z-near
     * @param far projection z-far
     */
    public final void reshapePerspective(final float angle_rad, final int width, final int height, final float near, final float far) {
        reshapeNotify(0, 0, width, height);
        final float ratio = (float)width/(float)height;
        final PMVMatrix4f p = getMatrix();
        p.loadPIdentity();
        p.perspectiveP(angle_rad, ratio, near, far);
    }

    /**
     * Orthogonal projection, method also calls {@link #reshapeNotify(int, int, int, int)}.
     * @param width viewport width
     * @param height viewport height
     * @param near projection z-near
     * @param far projection z-far
     */
    public final void reshapeOrtho(final int width, final int height, final float near, final float far) {
        reshapeNotify(0, 0, width, height);
        final PMVMatrix4f p = getMatrix();
        p.loadPIdentity();
        p.orthoP(0, width, 0, height, near, far);
    }

    //
    // Shader Management
    //

    private static final String SHADER_SRC_SUB = "";
    private static final String SHADER_BIN_SUB = "bin";
    private static final String GLSL_PARAM_COMMENT_START = "\n// JogAmp Graph Parameter Start\n";
    private static final String GLSL_PARAM_COMMENT_END = "// JogAmp Graph Parameter End\n\n";
    private static final String GLSL_USE_COLOR_CHANNEL = "#define USE_COLOR_CHANNEL 1\n";
    private static final String GLSL_USE_COLOR_TEXTURE = "#define USE_COLOR_TEXTURE 1\n";
    private static final String GLSL_USE_FRUSTUM_CLIPPING = "#define USE_FRUSTUM_CLIPPING 1\n";
    private static final String GLSL_DEF_SAMPLE_COUNT = "#define SAMPLE_COUNT ";
    private static final String GLSL_CONST_SAMPLE_COUNT = "const float sample_count = ";
    private static final String GLSL_MAIN_BEGIN = "void main (void)\n{\n";
    private static final String gcuTexture2D = "gcuTexture2D";
    private static final String GLSL_USE_DISCARD = "#define USE_DISCARD 1\n";

    private String getVersionedShaderName() {
        return "curverenderer01";
    }

    // FIXME: Really required to have sampler2D def. precision ? If not, we can drop getFragmentShaderPrecision(..) and use default ShaderCode ..
    private static final String es2_precision_fp = "\nprecision mediump float;\nprecision mediump int;\nprecision mediump sampler2D;\n";

    private final String getFragmentShaderPrecision(final GL2ES2 gl) {
        if( gl.isGLES() ) {
            return es2_precision_fp;
        }
        if( ShaderCode.requiresGL3DefaultPrecision(gl) ) {
            return ShaderCode.gl3_default_precision_fp;
        }
        return null;
    }

    private static enum ShaderModeSelector1 {
        /** Pass-1: Curve Simple */
        PASS1_SIMPLE("curve", "_simple", 0),
        /** Pass-1: Curve Varying Weight */
        PASS1_WEIGHT("curve", "_weight", 0),
        /** Pass-2: MSAA */
        PASS2_MSAA("msaa", "", 0),
        /** Pass-2: VBAA Flipquad3, 1 sample */
        PASS2_VBAA_QUAL0_SAMPLES1("vbaa", "_flipquad3", 1),
        /** Pass-2: VBAA Flipquad3, 2 samples */
        PASS2_VBAA_QUAL0_SAMPLES2("vbaa", "_flipquad3", 2),
        /** Pass-2: VBAA Flipquad3, 4 samples */
        PASS2_VBAA_QUAL0_SAMPLES4("vbaa", "_flipquad3", 4),
        /** Pass-2: VBAA Flipquad3, 8 samples */
        PASS2_VBAA_QUAL0_SAMPLES8("vbaa", "_flipquad3", 8),

        /** Pass-2: VBAA Brute-Force, Odd, 1 samples */
        PASS2_VBAA_QUAL1_SAMPLES1("vbaa", "_bforce_odd",  1),
        /** Pass-2: VBAA Brute-Force, Even, 2 samples */
        PASS2_VBAA_QUAL1_SAMPLES2("vbaa", "_bforce_even", 2),
        /** Pass-2: VBAA Brute-Force, Odd, 3 samples */
        PASS2_VBAA_QUAL1_SAMPLES3("vbaa", "_bforce_odd",  3),
        /** Pass-2: VBAA Brute-Force, Even, 4 samples */
        PASS2_VBAA_QUAL1_SAMPLES4("vbaa", "_bforce_even", 4),
        /** Pass-2: VBAA Brute-Force, Odd, 5 samples */
        PASS2_VBAA_QUAL1_SAMPLES5("vbaa", "_bforce_odd",  5),
        /** Pass-2: VBAA Brute-Force, Even, 6 samples */
        PASS2_VBAA_QUAL1_SAMPLES6("vbaa", "_bforce_even", 6),
        /** Pass-2: VBAA Brute-Force, Odd, 7 samples */
        PASS2_VBAA_QUAL1_SAMPLES7("vbaa", "_bforce_odd",  7),
        /** Pass-2: VBAA Brute-Force, Even, 8 samples */
        PASS2_VBAA_QUAL1_SAMPLES8("vbaa", "_bforce_even", 8);

        public final String tech;
        public final String sub;
        public final int sampleCount;

        ShaderModeSelector1(final String tech, final String sub, final int sampleCount) {
            this.tech = tech;
            this.sub= sub;
            this.sampleCount = sampleCount;
        }

        public static ShaderModeSelector1 selectPass1(final int renderModes) {
            return Region.hasVariableWeight(renderModes) ? PASS1_WEIGHT : PASS1_SIMPLE;
        }

        public static ShaderModeSelector1 selectPass2(final int renderModes, final int quality, final int sampleCount) {
            if( Region.isVBAA(renderModes) ) {
                if( 0 == quality ) {
                    if( sampleCount < 2 ) {
                        return PASS2_VBAA_QUAL0_SAMPLES1;
                    } else if( sampleCount < 4 ) {
                        return PASS2_VBAA_QUAL0_SAMPLES2;
                    } else if( sampleCount < 8 ) {
                        return PASS2_VBAA_QUAL0_SAMPLES4;
                    } else {
                        return PASS2_VBAA_QUAL0_SAMPLES8;
                    }
                } else {
                    switch( sampleCount ) {
                        case 0: // Fall through intended
                        case 1:  return PASS2_VBAA_QUAL1_SAMPLES1;
                        case 2:  return PASS2_VBAA_QUAL1_SAMPLES2;
                        case 3:  return PASS2_VBAA_QUAL1_SAMPLES3;
                        case 4:  return PASS2_VBAA_QUAL1_SAMPLES4;
                        case 5:  return PASS2_VBAA_QUAL1_SAMPLES5;
                        case 6:  return PASS2_VBAA_QUAL1_SAMPLES6;
                        case 7:  return PASS2_VBAA_QUAL1_SAMPLES7;
                        default: return PASS2_VBAA_QUAL1_SAMPLES8;
                    }
                }
            } else {
                return PASS2_MSAA; // Region.isMSAA(renderModes) and default
            }
        }
    }

    private static class ShaderKey {
        final boolean isTwoPass;
        final boolean pass1;
        final ShaderModeSelector1 sms;
        final boolean hasFrustumClipping; // pass1 or pass2
        final boolean hasColorChannel; // pass1 only
        final boolean hasColorTexture; // pass1 only
        final String colorTexSeqID;

        final int hashValue;

        ShaderKey(final boolean isTwoPass, final boolean pass1, final ShaderModeSelector1 sms,
                  final boolean hasFrustumClipping, final boolean hasColorChannel,
                  final boolean hasColorTexture, final TextureSequence colorTexSeq, final int colorTexSeqHash)
        {
            this.isTwoPass = isTwoPass;
            this.pass1 = pass1;
            this.sms = sms;
            this.hasFrustumClipping = hasFrustumClipping;
            this.hasColorChannel = hasColorChannel;
            this.hasColorTexture = hasColorTexture;
            if( hasColorTexture ) {
                this.colorTexSeqID = colorTexSeq.getTextureFragmentShaderHashID();
            } else {
                this.colorTexSeqID = "";
            }
            hashValue = getShaderKey1(isTwoPass, pass1, hasFrustumClipping, hasColorChannel, hasColorTexture, sms, colorTexSeqHash);
        }
        @Override
        public final int hashCode() { return hashValue; }
        @Override
        public final boolean equals(final Object other) {
            if( this == other ) { return true; }
            if( !(other instanceof ShaderKey) ) {
                return false;
            }
            final ShaderKey o = (ShaderKey)other;
            return isTwoPass == o.isTwoPass &&
                   pass1 == o.pass1 &&
                   // pass2Quality == o.pass2Quality && // included in sms
                   // sampleCount == o.sampleCount && // included in sms
                   sms.ordinal() == o.sms.ordinal() &&
                   hasFrustumClipping == o.hasFrustumClipping &&
                   hasColorChannel == o.hasColorChannel &&
                   hasColorTexture == o.hasColorTexture &&
                   colorTexSeqID.equals(o.colorTexSeqID);
        }
        @Override
        public String toString() {
            return shaderHashToString(hashValue, isTwoPass, pass1, hasFrustumClipping, hasColorChannel, hasColorTexture, sms);
        }
    }
    private static final boolean UseShaderPrograms0 = true;
    private final LongObjectHashMap shaderPrograms0;
    private final HashMap<ShaderKey, ShaderProgram> shaderPrograms1;

    private static String shaderHashToString(final int hashCode, final boolean isTwoPass, final boolean pass1,
                                   final boolean hasFrustumClipping, final boolean hasColorChannel, final boolean hasColorTexture,
                                   final ShaderModeSelector1 sms) {
            return "ShaderHash[hash 0x"+Integer.toHexString(hashCode)+", is2Pass "+isTwoPass+", pass1 "+pass1+
                   ", has[clip "+hasFrustumClipping+", colChan "+hasColorChannel+", colTex "+hasColorTexture+"], "+sms+"]";
    }
    private static String shaderKeyToString(final long key, final boolean isTwoPass, final boolean pass1,
                                   final boolean hasFrustumClipping, final boolean hasColorChannel, final boolean hasColorTexture,
                                   final ShaderModeSelector1 sms) {
            return "ShaderKey[key 0x"+Long.toHexString(key)+", is2Pass "+isTwoPass+", pass1 "+pass1+
                   ", has[clip "+hasFrustumClipping+", colChan "+hasColorChannel+", colTex "+hasColorTexture+"], "+sms+"]";
    }

    private static long getShaderKey0(final boolean isTwoPass, final boolean pass1,
                                      final boolean hasFrustumClipping, final boolean hasColorChannel, final boolean hasColorTexture,
                                      final ShaderModeSelector1 sms, final long colorTexSeqHash) {
        //  # |  s |
        //  0 |  1 | isTwoPass
        //  1 |  1 | pass1
        //  2 |  5 | ShaderModeSelector1
        //  7 |  1 | hasFrustumClipping
        //  8 |  1 | hasColorChannel
        //  9 |  1 | hasColorTexture
        // 32 | 32 | colorTexSeqHash
        long hash =             ( isTwoPass ? 1 : 0 );
        hash = ( hash << 1 ) | ( pass1 ? 1 : 0 ) ;
        hash = ( hash << 1 ) | sms.ordinal(); // incl. pass2Quality + sampleCount
        hash = ( hash << 5 ) | ( hasFrustumClipping ? 1 : 0 );
        hash = ( hash << 1 ) | ( hasColorChannel ? 1 : 0 );
        hash = ( hash << 1 ) | ( hasColorTexture ? 1 : 0 );
        hash = ( hash << 1 ) | ( ( colorTexSeqHash & 0xFFFFFFL ) << 32 );
        return hash;
    }
    private static int getShaderKey1(final boolean isTwoPass, final boolean pass1,
                                     final boolean hasFrustumClipping, final boolean hasColorChannel, final boolean hasColorTexture,
                                     final ShaderModeSelector1 sms, final int colorTexSeqHash) {
        // 31 * x == (x << 5) - x
        int hash = 31 * ( isTwoPass ? 1 : 0 );
        hash = ((hash << 5) - hash) + ( pass1 ? 1 : 0 ) ;
        // hash = ((hash << 5) - hash) + pass2Quality; // included in sms
        // hash = ((hash << 5) - hash) + sampleCount; // included in sms
        hash = ((hash << 5) - hash) + sms.ordinal();
        hash = ((hash << 5) - hash) + ( hasFrustumClipping ? 1 : 0 );
        hash = ((hash << 5) - hash) + ( hasColorChannel ? 1 : 0 );
        hash = ((hash << 5) - hash) + ( hasColorTexture ? 1 : 0 );
        hash = ((hash << 5) - hash) + colorTexSeqHash;
        return hash;
    }

    /**
     * Generate, selects and caches the desired Curve-Graph {@link ShaderProgram} according to the given parameters.
     *
     * The newly generated or cached {@link ShaderProgram} is {@link RenderState#setShaderProgram(GL2ES2, ShaderProgram) set current} in the {@link RenderState} composition
     * and can be retrieved via {@link RenderState#getShaderProgram()}.
     *
     * @param gl
     * @param renderModes
     * @param pass1
     * @param colorTexSeq
     * @return true if a new shader program is being used and hence external uniform-data and -location,
     *         as well as the attribute-location must be updated, otherwise false.
     * @see #enable(GL2ES2, boolean)
     * @see RenderState#setShaderProgram(GL2ES2, ShaderProgram)
     * @see RenderState#getShaderProgram()
     */
    public final boolean useShaderProgram(final GL2ES2 gl, final int renderModes, final boolean pass1, final TextureSequence colorTexSeq) {
        final boolean isTwoPass = Region.isTwoPass( renderModes );
        final ShaderModeSelector1 sms = pass1 ? ShaderModeSelector1.selectPass1(renderModes) :
                                        ShaderModeSelector1.selectPass2(renderModes, getAAQuality(), getSampleCount());
        final boolean hasFrustumClipping = ( null != getClipFrustum() ) && ( ( !isTwoPass && pass1 ) || ( isTwoPass && !pass1 ) );
        final boolean hasColorChannel = pass1 && Region.hasColorChannel( renderModes );
        final boolean hasColorTexture = pass1 && Region.hasColorTexture( renderModes ) && null != colorTexSeq;
        final int colorTexSeqHash;
        final String colTexLookupFuncName;
        if( hasColorTexture ) {
            colTexLookupFuncName = colorTexSeq.setTextureLookupFunctionName(gcuTexture2D);
            colorTexSeqHash = colorTexSeq.getTextureFragmentShaderHashCode();
        } else {
            colTexLookupFuncName = "";
            colorTexSeqHash = 0;
        }

        if( UseShaderPrograms0 ) {
            return useShaderProgram0(gl, renderModes, isTwoPass, pass1, sms, hasFrustumClipping, hasColorChannel,
                                     hasColorTexture, colorTexSeq, colTexLookupFuncName, colorTexSeqHash);
        } else {
            return useShaderProgram1(gl, renderModes, isTwoPass, pass1, sms, hasFrustumClipping, hasColorChannel,
                                     hasColorTexture, colorTexSeq, colTexLookupFuncName, colorTexSeqHash);
        }
    }
    private final boolean useShaderProgram0(final GL2ES2 gl, final int renderModes,
                                            final boolean isTwoPass, final boolean pass1, final ShaderModeSelector1 sms,
                                            final boolean hasFrustumClipping, final boolean hasColorChannel,
                                            final boolean hasColorTexture, final TextureSequence colorTexSeq,
                                            final String colTexLookupFuncName, final int colorTexSeqHash)
    {
        final long shaderKey = getShaderKey0(isTwoPass, pass1, hasFrustumClipping, hasColorChannel, hasColorTexture, sms, colorTexSeqHash);
        /**
        if(DEBUG) {
            System.err.println("XXX "+Region.getRenderModeString(renderModes, getAAQuality(), getSampleCount(), 0)+", "+
                shaderKeyToString(shaderHashCode, isTwoPass, pass1, hasFrustumClipping, hasColorChannel, hasColorTexture, sms));
        } */

        ShaderProgram sp = (ShaderProgram) shaderPrograms0.get( shaderKey );
        if( null != sp ) {
            final boolean spChanged = rs.setShaderProgram(gl, sp);
            if( DEBUG_SHADER_MAP ) {
                if( spChanged ) {
                    System.err.printf("RegionRenderer.useShaderProgram0.X1: GOT renderModes %s, %s -> sp %d / %d (changed)%n",
                            Region.getRenderModeString(renderModes),
                            shaderKeyToString(shaderKey, isTwoPass, pass1, hasFrustumClipping, hasColorChannel, hasColorTexture, sms),
                                              sp.program(), sp.id());
                } else if( DEBUG_ALL_EVENT ) {
                    System.err.printf("RegionRenderer.useShaderProgram0.X1: GOT renderModes %s, %s -> sp %d / %d (keep)%n",
                            Region.getRenderModeString(renderModes),
                            shaderKeyToString(shaderKey, isTwoPass, pass1, hasFrustumClipping, hasColorChannel, hasColorTexture, sms),
                                              sp.program(), sp.id());
                }
            }
            return spChanged;
        }
        sp = createShaderProgram(gl, renderModes, isTwoPass, pass1, sms, hasFrustumClipping, hasColorChannel,
                                 hasColorTexture, colorTexSeq, colTexLookupFuncName, colorTexSeqHash);

        rs.setShaderProgram(gl, sp);

        {
            // shaderPrograms0.containsKey(shaderHashCode);
            final ShaderProgram spOld = (ShaderProgram) shaderPrograms0.put(shaderKey, sp);
            if( null != spOld ) {
                // COLLISION!
                final String msg = String.format((Locale)null,
                        "RegionRenderer.useShaderProgram0: WARNING Shader-HashCode Collision: hash 0x%s: %s, %s -> sp %d / %d (changed, new)%n",
                        Long.toHexString(shaderKey),
                        Region.getRenderModeString(renderModes),
                        shaderKeyToString(shaderKey, isTwoPass, pass1, hasFrustumClipping, hasColorChannel, hasColorTexture, sms),
                        sp.program(), sp.id());
                throw new RuntimeException(msg);
            }
        }

        if( DEBUG_SHADER_MAP ) {
            System.err.printf("RegionRenderer.useShaderProgram0.X1: PUT renderModes %s, %s -> sp %d / %d (changed, new)%n",
                    Region.getRenderModeString(renderModes), shaderKey, sp.program(), sp.id());
            // rsFp.dumpShaderSource(System.err);
        }
        return true;
    }
    private final boolean useShaderProgram1(final GL2ES2 gl, final int renderModes,
                                            final boolean isTwoPass, final boolean pass1, final ShaderModeSelector1 sms,
                                            final boolean hasFrustumClipping, final boolean hasColorChannel,
                                            final boolean hasColorTexture, final TextureSequence colorTexSeq,
                                            final String colTexLookupFuncName, final int colorTexSeqHash) {
        final ShaderKey shaderKey = new ShaderKey(isTwoPass, pass1, sms, hasFrustumClipping, hasColorChannel,
                                                  hasColorTexture, colorTexSeq, colorTexSeqHash);
        /**
        if(DEBUG) {
            System.err.println("XXX "+Region.getRenderModeString(renderModes, getAAQuality(), getSampleCount(), 0)+", "+shaderKey);
        } */

        ShaderProgram sp = shaderPrograms1.get( shaderKey );
        if( null != sp ) {
            final boolean spChanged = rs.setShaderProgram(gl, sp);
            if( DEBUG_SHADER_MAP ) {
                if( spChanged ) {
                    System.err.printf("RegionRenderer.useShaderProgram1.X1: GOT renderModes %s, %s -> sp %d / %d (changed)%n",
                            Region.getRenderModeString(renderModes), shaderKey, sp.program(), sp.id());
                } else if( DEBUG_ALL_EVENT ) {
                    System.err.printf("RegionRenderer.useShaderProgram1.X1: GOT renderModes %s, %s -> sp %d / %d (keep)%n",
                            Region.getRenderModeString(renderModes), shaderKey, sp.program(), sp.id());
                }
            }
            return spChanged;
        }
        sp = createShaderProgram(gl, renderModes, isTwoPass, pass1, sms, hasFrustumClipping, hasColorChannel,
                                 hasColorTexture, colorTexSeq, colTexLookupFuncName, colorTexSeqHash);

        rs.setShaderProgram(gl, sp);

        shaderPrograms1.put(shaderKey, sp);
        if( DEBUG_SHADER_MAP ) {
            System.err.printf("RegionRenderer.useShaderProgram1.X1: PUT renderModes %s, %s -> sp %d / %d (changed, new)%n",
                    Region.getRenderModeString(renderModes), shaderKey, sp.program(), sp.id());
            // rsFp.dumpShaderSource(System.err);
        }
        return true;
    }
    @SuppressWarnings("unused")
    private final ShaderProgram createShaderProgram(final GL2ES2 gl, final int renderModes,
                                                    final boolean isTwoPass, final boolean pass1, final ShaderModeSelector1 sms,
                                                    final boolean hasFrustumClipping, final boolean hasColorChannel,
                                                    final boolean hasColorTexture, final TextureSequence colorTexSeq,
                                                    final String colTexLookupFuncName, final int colorTexSeqHash)
    {
        final String versionedBaseName = getVersionedShaderName();
        final String vertexShaderName;
        if( isTwoPass ) {
            vertexShaderName = versionedBaseName+"-pass"+(pass1?1:2);
        } else {
            vertexShaderName = versionedBaseName+"-single";
        }
        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, AttributeNames.class, SHADER_SRC_SUB, SHADER_BIN_SUB, vertexShaderName, true);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, AttributeNames.class, SHADER_SRC_SUB, SHADER_BIN_SUB, versionedBaseName+"-segment-head", true);

        if( hasColorTexture && GLES2.GL_TEXTURE_EXTERNAL_OES == colorTexSeq.getTextureTarget() ) {
            if( !gl.isExtensionAvailable(GLExtensions.OES_EGL_image_external) ) {
                throw new GLException(GLExtensions.OES_EGL_image_external+" requested but not available");
            }
        }
        boolean preludeGLSLVersion = true;
        if( hasColorTexture && GLES2.GL_TEXTURE_EXTERNAL_OES == colorTexSeq.getTextureTarget() ) {
            if( Platform.OSType.ANDROID == Platform.getOSType() && gl.isGLES3() ) {
                // Bug on Nexus 10, ES3 - Android 4.3, where
                // GL_OES_EGL_image_external extension directive leads to a failure _with_ '#version 300 es' !
                //   P0003: Extension 'GL_OES_EGL_image_external' not supported
                preludeGLSLVersion = false;
            }
        }
        //
        // GLSL customization at top
        //
        int posVp = rsVp.defaultShaderCustomization(gl, preludeGLSLVersion, true);
        // rsFp.defaultShaderCustomization(gl, true, true);
        int posFp = preludeGLSLVersion ? rsFp.addGLSLVersion(gl) : 0;
        if( hasColorTexture ) {
            posFp = rsFp.insertShaderSource(0, posFp, colorTexSeq.getRequiredExtensionsShaderStub());
        }
        if( pass1 && !preludeGLSLVersion || ( gl.isGLES2() && !gl.isGLES3() ) ) {
            posFp = rsFp.insertShaderSource(0, posFp, ShaderCode.createExtensionDirective(GLExtensions.OES_standard_derivatives, ShaderCode.ENABLE));
        }
        if( false ) {
            final String rsFpDefPrecision =  getFragmentShaderPrecision(gl);
            if( null != rsFpDefPrecision ) {
                posFp = rsFp.insertShaderSource(0, posFp, rsFpDefPrecision);
            }
        } else {
            posFp = rsFp.addDefaultShaderPrecision(gl, posFp);
        }

        //
        // GLSL append from here on
        posFp = -1;

        posVp = rsVp.insertShaderSource(0, posVp, GLSL_PARAM_COMMENT_START);
        posFp = rsFp.insertShaderSource(0, posFp, GLSL_PARAM_COMMENT_START);

        if( !gl.getContext().hasRendererQuirk(GLRendererQuirks.GLSLBuggyDiscard) ) {
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_USE_DISCARD);
        }

        if( hasFrustumClipping ) {
            posVp = rsVp.insertShaderSource(0, posVp, GLSL_USE_FRUSTUM_CLIPPING);
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_USE_FRUSTUM_CLIPPING);
        }

        if( hasColorChannel ) {
            posVp = rsVp.insertShaderSource(0, posVp, GLSL_USE_COLOR_CHANNEL);
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_USE_COLOR_CHANNEL);
        }
        if( hasColorTexture ) {
                    rsVp.insertShaderSource(0, posVp, GLSL_USE_COLOR_TEXTURE);
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_USE_COLOR_TEXTURE);
        }
        if( !pass1 ) {
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_DEF_SAMPLE_COUNT+sms.sampleCount+"\n");
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_CONST_SAMPLE_COUNT+sms.sampleCount+".0;\n");
        }

        posVp = rsVp.insertShaderSource(0, posVp, GLSL_PARAM_COMMENT_END);
        posFp = rsFp.insertShaderSource(0, posFp, GLSL_PARAM_COMMENT_END);

        try {
            posFp = rsFp.insertShaderSource(0, posFp, AttributeNames.class, "uniforms.glsl");
            posFp = rsFp.insertShaderSource(0, posFp, AttributeNames.class, "varyings.glsl");
            if( hasColorTexture || hasFrustumClipping ) {
                posFp = rsFp.insertShaderSource(0, posFp, AttributeNames.class, "functions.glsl");
            }
        } catch (final IOException ioe) {
            throw new RuntimeException("Failed to read: includes", ioe);
        }
        if( 0 > posFp ) {
            throw new RuntimeException("Failed to read: includes");
        }

        if( hasColorTexture ) {
            posFp = rsFp.insertShaderSource(0, posFp, "uniform "+colorTexSeq.getTextureSampler2DType()+" "+UniformNames.gcu_ColorTexUnit+";\n");
            posFp = rsFp.insertShaderSource(0, posFp, colorTexSeq.getTextureLookupFragmentShaderImpl());
        }

        posFp = rsFp.insertShaderSource(0, posFp, GLSL_MAIN_BEGIN);

        final String passS = pass1 ? "-pass1-" : "-pass2-";
        final String shaderSegment = versionedBaseName+passS+sms.tech+sms.sub+".glsl";
        if(DEBUG) {
            System.err.printf("RegionRenderer.createShaderProgram.1: segment %s%n", shaderSegment);
        }
        try {
            posFp = rsFp.insertShaderSource(0, posFp, AttributeNames.class, shaderSegment);
        } catch (final IOException ioe) {
            throw new RuntimeException("Failed to read: "+shaderSegment, ioe);
        }
        if( 0 > posFp ) {
            throw new RuntimeException("Failed to read: "+shaderSegment);
        }
        posFp = rsFp.insertShaderSource(0, posFp, "}\n");

        if( hasColorTexture ) {
            rsFp.replaceInShaderSource(gcuTexture2D, colTexLookupFuncName);
        }

        final ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);

        if( !sp.init(gl) ) {
            throw new GLException("RegionRenderer: Couldn't init program: "+sp);
        }

        if( !sp.link(gl, System.err) ) {
            throw new GLException("could not link program: "+sp);
        }
        return sp;
    }
}

