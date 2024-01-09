/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
import java.util.Iterator;

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
import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.graph.curve.Region;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
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
     * which will cause {@link GLRegion#draw(GL2ES2, RegionRenderer, int[]) GLRegion's draw-method}
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
    }

    public final boolean isVBOSupported() { return vboSupported; }

    /**
     * Initialize shader and bindings for GPU based rendering bound to the given GL object's GLContext
     * if not initialized yet.
     * <p>Disables the renderer via {@link #enable(GL2ES2, boolean)} to remove any side-effects, ie ShaderState incl. shader program.</p>
     * <p>Shall be called once before at initialization before a {@code draw()} method, e.g. {@link RegionRenderer#draw(GL2ES2, Region, int)}</p>
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
        for(final Iterator<IntObjectHashMap.Entry> i = shaderPrograms.iterator(); i.hasNext(); ) {
            final ShaderProgram sp = (ShaderProgram) i.next().getValue();
            sp.destroy(gl);
        }
        shaderPrograms.clear();
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

    public final void setClipBBox(final AABBox clipBBox) { rs.setClipBBox(clipBBox); }
    public final AABBox getClipBBox() { return rs.getClipBBox(); }

    public final boolean isHintMaskSet(final int mask) { return rs.isHintMaskSet(mask); }

    public final void setHintMask(final int mask) { rs.setHintMask(mask); }

    public final void clearHintMask(final int mask) { rs.clearHintMask(mask); }

    /**
     * Enabling or disabling the {@link #getRenderState() RenderState}'s
     * current {@link RenderState#getShaderProgram() shader program}.
     * <p>
     * {@link #useShaderProgram(GL2ES2, int, boolean, int, int, TextureSequence)}
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
     * @see #useShaderProgram(GL2ES2, int, boolean, int, int, TextureSequence)
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
    private static final String GLSL_USE_COLOR_CHANNEL = "#define USE_COLOR_CHANNEL 1\n";
    private static final String GLSL_USE_COLOR_TEXTURE = "#define USE_COLOR_TEXTURE 1\n";
    private static final String GLSL_USE_AABBOX_CLIPPING = "#define USE_AABBOX_CLIPPING 1\n";
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
            if( Region.isMSAA(renderModes) ) {
                return PASS2_MSAA;
            } else if( Region.isVBAA(renderModes) ) {
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
                return null;
            }
        }
    }
    private final IntObjectHashMap shaderPrograms = new IntObjectHashMap();

    private static final int HIGH_MASK = Region.COLORCHANNEL_RENDERING_BIT | Region.COLORTEXTURE_RENDERING_BIT;
    private static final int TWO_PASS_BIT = 1 <<  31;

    /**
     * Generate, selects and caches the desired Curve-Graph {@link ShaderProgram} according to the given parameters.
     *
     * The newly generated or cached {@link ShaderProgram} is {@link RenderState#setShaderProgram(GL2ES2, ShaderProgram) set current} in the {@link RenderState} composition
     * and can be retrieved via {@link RenderState#getShaderProgram()}.
     *
     * @param gl
     * @param renderModes
     * @param pass1
     * @param quality
     * @param sampleCount
     * @param colorTexSeq
     * @return true if a new shader program is being used and hence external uniform-data and -location,
     *         as well as the attribute-location must be updated, otherwise false.
     * @see #enable(GL2ES2, boolean)
     * @see RenderState#setShaderProgram(GL2ES2, ShaderProgram)
     * @see RenderState#getShaderProgram()
     */
    public final boolean useShaderProgram(final GL2ES2 gl, final int renderModes,
                                          final boolean pass1, final int quality, final int sampleCount, final TextureSequence colorTexSeq) {
        final ShaderModeSelector1 sel1 = pass1 ? ShaderModeSelector1.selectPass1(renderModes) :
                                                 ShaderModeSelector1.selectPass2(renderModes, quality, sampleCount);
        final boolean hasAABBoxClipping = null != getClipBBox();
        final boolean isTwoPass = Region.isTwoPass( renderModes );
        final boolean hasColorChannel = Region.hasColorChannel( renderModes );
        final boolean hasColorTexture = Region.hasColorTexture( renderModes ) && null != colorTexSeq;
        final boolean isPass1ColorTexSeq = pass1 && hasColorTexture;
        final int colorTexSeqHash;
        final String texLookupFuncName;
        if( isPass1ColorTexSeq ) {
            texLookupFuncName = colorTexSeq.setTextureLookupFunctionName(gcuTexture2D);
            colorTexSeqHash = colorTexSeq.getTextureFragmentShaderHashCode();
        } else {
            texLookupFuncName = null;
            colorTexSeqHash = 0;
        }
        final int shaderKey;
        {
            // 31 * x == (x << 5) - x
            int hash = 31 + colorTexSeqHash;
            hash = ((hash << 5) - hash) + sel1.ordinal();
            hash = ((hash << 5) - hash) + ( HIGH_MASK & renderModes );
            hash = ((hash << 5) - hash) + ( hasAABBoxClipping ? 1 : 0 );
            hash = ((hash << 5) - hash) + ( isTwoPass ? TWO_PASS_BIT : 0 );
            shaderKey = hash;
        }

        /**
        if(DEBUG) {
            System.err.printf("RegionRendererImpl01.useShaderProgram.0: renderModes %s, sel1 %s, key 0x%X (pass1 %b, q %d, samples %d) - Thread %s%n",
                    Region.getRenderModeString(renderModes), sel1, shaderKey, pass1, quality, sampleCount, Thread.currentThread());
        } */

        ShaderProgram sp = (ShaderProgram) shaderPrograms.get( shaderKey );
        if( null != sp ) {
            final boolean spChanged = rs.setShaderProgram(gl, sp);
            if( DEBUG ) {
                if( spChanged ) {
                    System.err.printf("RegionRendererImpl01.useShaderProgram.X1: GOT renderModes %s, sel1 %s, key 0x%X -> sp %d / %d (changed)%n", Region.getRenderModeString(renderModes), sel1, shaderKey, sp.program(), sp.id());
                } else if( DEBUG_ALL_EVENT ) {
                    System.err.printf("RegionRendererImpl01.useShaderProgram.X1: GOT renderModes %s, sel1 %s, key 0x%X -> sp %d / %d (keep)%n", Region.getRenderModeString(renderModes), sel1, shaderKey, sp.program(), sp.id());
                }
            }
            return spChanged;
        }
        final String versionedBaseName = getVersionedShaderName();
        final String vertexShaderName;
        if( isTwoPass ) {
            vertexShaderName = versionedBaseName+"-pass"+(pass1?1:2);
        } else {
            vertexShaderName = versionedBaseName+"-single";
        }
        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, AttributeNames.class, SHADER_SRC_SUB, SHADER_BIN_SUB, vertexShaderName, true);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, AttributeNames.class, SHADER_SRC_SUB, SHADER_BIN_SUB, versionedBaseName+"-segment-head", true);

        if( isPass1ColorTexSeq && GLES2.GL_TEXTURE_EXTERNAL_OES == colorTexSeq.getTextureTarget() ) {
            if( !gl.isExtensionAvailable(GLExtensions.OES_EGL_image_external) ) {
                throw new GLException(GLExtensions.OES_EGL_image_external+" requested but not available");
            }
        }
        boolean supressGLSLVersionES30 = false;
        if( isPass1ColorTexSeq && GLES2.GL_TEXTURE_EXTERNAL_OES == colorTexSeq.getTextureTarget() ) {
            if( Platform.OSType.ANDROID == Platform.getOSType() && gl.isGLES3() ) {
                // Bug on Nexus 10, ES3 - Android 4.3, where
                // GL_OES_EGL_image_external extension directive leads to a failure _with_ '#version 300 es' !
                //   P0003: Extension 'GL_OES_EGL_image_external' not supported
                supressGLSLVersionES30 = true;
            }
        }
        //
        // GLSL customization at top
        //
        int posVp = rsVp.defaultShaderCustomization(gl, !supressGLSLVersionES30, true);
        // rsFp.defaultShaderCustomization(gl, true, true);
        int posFp = supressGLSLVersionES30 ? 0 : rsFp.addGLSLVersion(gl);
        if( isPass1ColorTexSeq ) {
            posFp = rsFp.insertShaderSource(0, posFp, colorTexSeq.getRequiredExtensionsShaderStub());
        }
        if( pass1 && supressGLSLVersionES30 || ( gl.isGLES2() && !gl.isGLES3() ) ) {
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

        if( !gl.getContext().hasRendererQuirk(GLRendererQuirks.GLSLBuggyDiscard) ) {
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_USE_DISCARD);
        }

        if( hasAABBoxClipping ) {
            posVp = rsVp.insertShaderSource(0, posVp, GLSL_USE_AABBOX_CLIPPING);
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_USE_AABBOX_CLIPPING);
        }

        if( hasColorChannel ) {
            posVp = rsVp.insertShaderSource(0, posVp, GLSL_USE_COLOR_CHANNEL);
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_USE_COLOR_CHANNEL);
        }
        if( isPass1ColorTexSeq ) {
                    rsVp.insertShaderSource(0, posVp, GLSL_USE_COLOR_TEXTURE);
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_USE_COLOR_TEXTURE);
        }
        if( !pass1 ) {
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_DEF_SAMPLE_COUNT+sel1.sampleCount+"\n");
            posFp = rsFp.insertShaderSource(0, posFp, GLSL_CONST_SAMPLE_COUNT+sel1.sampleCount+".0;\n");
        }

        try {
            if( isPass1ColorTexSeq || hasAABBoxClipping ) {
                posFp = rsFp.insertShaderSource(0, posFp, AttributeNames.class, "functions.glsl");
            }
            posFp = rsFp.insertShaderSource(0, posFp, AttributeNames.class, "uniforms.glsl");
            posFp = rsFp.insertShaderSource(0, posFp, AttributeNames.class, "varyings.glsl");
        } catch (final IOException ioe) {
            throw new RuntimeException("Failed to read: includes", ioe);
        }
        if( 0 > posFp ) {
            throw new RuntimeException("Failed to read: includes");
        }

        if( isPass1ColorTexSeq ) {
            posFp = rsFp.insertShaderSource(0, posFp, "uniform "+colorTexSeq.getTextureSampler2DType()+" "+UniformNames.gcu_ColorTexUnit+";\n");
            posFp = rsFp.insertShaderSource(0, posFp, colorTexSeq.getTextureLookupFragmentShaderImpl());
        }

        posFp = rsFp.insertShaderSource(0, posFp, GLSL_MAIN_BEGIN);

        final String passS = pass1 ? "-pass1-" : "-pass2-";
        final String shaderSegment = versionedBaseName+passS+sel1.tech+sel1.sub+".glsl";
        if(DEBUG) {
            System.err.printf("RegionRendererImpl01.useShaderProgram.1: segment %s%n", shaderSegment);
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

        if( isPass1ColorTexSeq ) {
            rsFp.replaceInShaderSource(gcuTexture2D, texLookupFuncName);
        }

        sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);

        if( !sp.init(gl) ) {
            throw new GLException("RegionRenderer: Couldn't init program: "+sp);
        }

        if( !sp.link(gl, System.err) ) {
            throw new GLException("could not link program: "+sp);
        }
        rs.setShaderProgram(gl, sp);

        shaderPrograms.put(shaderKey, sp);
        if( DEBUG ) {
            System.err.printf("RegionRendererImpl01.useShaderProgram.X1: PUT renderModes %s, sel1 %s, key 0x%X -> sp %d / %d (changed, new)%n",
                    Region.getRenderModeString(renderModes), sel1, shaderKey, sp.program(), sp.id());
            // rsFp.dumpShaderSource(System.err);
        }
        return true;
    }
}