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

import java.io.IOException;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.graph.curve.opengl.shader.AttributeNames;

import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.graph.curve.Region;

/**
 * OpenGL {@link Region} renderer
 * <p>
 * All OpenGL related operations regarding {@link Region}s
 * are passed through an instance of this class.
 * </p>
 */
public class RegionRenderer {
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
     * <p>
     * Implementation also sets {@link RegionRenderer#getRenderState() RenderState}'s {@link RenderState#BITHINT_BLENDING_ENABLED blending bit-hint}.
     * </p>
     * @see #create(RenderState, GLCallback, GLCallback)
     * @see #enable(GL2ES2, boolean)
     */
    public static final GLCallback defaultBlendEnable = new GLCallback() {
        @Override
        public void run(final GL gl, final RegionRenderer renderer) {
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendEquation(GL.GL_FUNC_ADD); // default
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            renderer.rs.setHintMask(RenderState.BITHINT_BLENDING_ENABLED);
        }
    };

    /**
     * Default {@link GL#GL_BLEND} <i>disable</i> {@link GLCallback},
     * simply turning off the {@link GL#GL_BLEND} state.
     * <p>
     * Implementation also clears {@link RegionRenderer#getRenderState() RenderState}'s {@link RenderState#BITHINT_BLENDING_ENABLED blending bit-hint}.
     * </p>
     * @see #create(RenderState, GLCallback, GLCallback)
     * @see #enable(GL2ES2, boolean)
     */
    public static final GLCallback defaultBlendDisable = new GLCallback() {
        @Override
        public void run(final GL gl, final RegionRenderer renderer) {
            renderer.rs.clearHintMask(RenderState.BITHINT_BLENDING_ENABLED);
            gl.glDisable(GL.GL_BLEND);
        }
    };

    /**
     * Create a Hardware accelerated Region Renderer.
     * <p>
     * The optional {@link GLCallback}s <code>enableCallback</code> and <code>disableCallback</code>
     * maybe used to issue certain tasks at {@link #enable(GL2ES2, boolean)}.<br/>
     * For example, instances {@link #defaultBlendEnable} and {@link #defaultBlendDisable}
     * can be utilized to enable and disable {@link GL#GL_BLEND}.
     * </p>
     * @param rs the used {@link RenderState}
     * @param enableCallback optional {@link GLCallback}, if not <code>null</code> will be issued at
     *                       {@link #init(GL2ES2, int) init(gl)} and {@link #enable(GL2ES2, boolean) enable(gl, true)}.
     * @param disableCallback optional {@link GLCallback}, if not <code>null</code> will be issued at
     *                        {@link #enable(GL2ES2, boolean) enable(gl, false)}.
     * @return an instance of Region Renderer
     * @see #enable(GL2ES2, boolean)
     */
    public static RegionRenderer create(final RenderState rs, final GLCallback enableCallback,
                                        final GLCallback disableCallback) {
        return new RegionRenderer(rs, enableCallback, disableCallback);
    }

    private final RenderState rs;

    private final GLCallback enableCallback;
    private final GLCallback disableCallback;

    private int vp_width;
    private int vp_height;
    private boolean initialized;
    private boolean vboSupported = false;

    public final boolean isInitialized() { return initialized; }

    /** Return width of current viewport */
    public final int getWidth() { return vp_width; }
    /** Return height of current viewport */
    public final int getHeight() { return vp_height; }

    public final PMVMatrix getMatrix() { return rs.getMatrix(); }

    //////////////////////////////////////

    /**
     * @param rs the used {@link RenderState}
     */
    protected RegionRenderer(final RenderState rs, final GLCallback enableCallback, final GLCallback disableCallback) {
        this.rs = rs;
        this.enableCallback = enableCallback;
        this.disableCallback = disableCallback;
    }

    public final boolean isVBOSupported() { return vboSupported; }

    /**
     * Initialize shader and bindings for GPU based rendering bound to the given GL object's GLContext
     * if not initialized yet.
     * <p>Leaves the renderer enabled, ie ShaderState.</p>
     * <p>Shall be called by a {@code draw()} method, e.g. {@link RegionRenderer#draw(GL2ES2, Region, int)}</p>
     *
     * @param gl referencing the current GLContext to which the ShaderState is bound to
     * @param renderModes TODO
     * @throws GLException if initialization failed
     */
    public final void init(final GL2ES2 gl, final int renderModes) throws GLException {
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
    }

    public final void destroy(GL2ES2 gl) {
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
        rs.destroy(gl);
        initialized = false;
    }

    public final RenderState getRenderState() { return rs; }

    /**
     * Enabling or disabling the {@link #getRenderState() RenderState}'s
     * {@link RenderState#getShaderProgram() shader program}.
     * <p>
     * In case enable and disable {@link GLCallback}s are setup via {@link #create(RenderState, GLCallback, GLCallback)},
     * they will be called before toggling the shader program.
     * </p>
     * @see #create(RenderState, GLCallback, GLCallback)
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
        if( !enable ) {
            final ShaderProgram sp = rs.getShaderProgram();
            if( null != sp ) {
                sp.useProgram(gl, false);
            }
        }
    }

    /** No PMVMatrix operation is performed here. PMVMatrix is marked dirty. */
    public final void reshapeNotify(int width, int height) {
        this.vp_width = width;
        this.vp_height = height;
    }

    public final void reshapePerspective(float angle, int width, int height, float near, float far) {
        this.vp_width = width;
        this.vp_height = height;
        final float ratio = (float)width/(float)height;
        final PMVMatrix p = rs.getMatrix();
        p.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        p.glLoadIdentity();
        p.gluPerspective(angle, ratio, near, far);
    }

    public final void reshapeOrtho(int width, int height, float near, float far) {
        this.vp_width = width;
        this.vp_height = height;
        final PMVMatrix p = rs.getMatrix();
        p.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        p.glLoadIdentity();
        p.glOrthof(0, width, 0, height, near, far);
    }

    //
    // Shader Management
    //

    private static final String SHADER_SRC_SUB = "";
    private static final String SHADER_BIN_SUB = "bin";

    private static String USE_COLOR_CHANNEL = "#define USE_COLOR_CHANNEL 1\n";
    private static String USE_COLOR_TEXTURE = "#define USE_COLOR_TEXTURE 1\n";
    private static String DEF_SAMPLE_COUNT = "#define SAMPLE_COUNT ";

    private String getVersionedShaderName() {
        return "curverenderer01";
    }

    // FIXME: Really required to have sampler2D def. precision ? If not, we can drop getFragmentShaderPrecision(..) and use default ShaderCode ..
    private static final String es2_precision_fp = "\nprecision mediump float;\nprecision mediump int;\nprecision mediump sampler2D;\n";

    private final String getFragmentShaderPrecision(GL2ES2 gl) {
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
        /** Pass-2: VBAA All-Equal, 2 samples */
        PASS2_VBAA_QUAL1_SAMPLES2("vbaa", "_allequal", 2),
        /** Pass-2: VBAA All-Equal, 4 samples */
        PASS2_VBAA_QUAL1_SAMPLES4("vbaa", "_allequal", 4),
        /** Pass-2: VBAA All-Equal, 6 samples */
        PASS2_VBAA_QUAL1_SAMPLES6("vbaa", "_allequal", 6),
        /** Pass-2: VBAA All-Equal, 8 samples */
        PASS2_VBAA_QUAL1_SAMPLES8("vbaa", "_allequal", 8);

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
                    if( sampleCount < 4 ) {
                        return PASS2_VBAA_QUAL1_SAMPLES2;
                    } else if( sampleCount < 6 ) {
                        return PASS2_VBAA_QUAL1_SAMPLES4;
                    } else if( sampleCount < 8 ) {
                        return PASS2_VBAA_QUAL1_SAMPLES6;
                    } else {
                        return PASS2_VBAA_QUAL1_SAMPLES8;
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
     * @param gl
     * @param renderModes
     * @param pass1
     * @param quality
     * @param sampleCount
     * @return true if a new shader program is being used and hence external uniform-data and -location,
     *         as well as the attribute-location must be updated, otherwise false.
     */
    public final boolean useShaderProgram(final GL2ES2 gl, final int renderModes,
                                          final boolean pass1, final int quality, final int sampleCount) {
        final ShaderModeSelector1 sel1 = pass1 ? ShaderModeSelector1.selectPass1(renderModes) :
                                                 ShaderModeSelector1.selectPass2(renderModes, quality, sampleCount);
        final boolean isTwoPass = Region.isTwoPass( renderModes );
        final int shaderKey = sel1.ordinal() | ( HIGH_MASK & renderModes ) | ( isTwoPass ? TWO_PASS_BIT : 0 );

        /**
        if(DEBUG) {
            System.err.printf("RegionRendererImpl01.useShaderProgram.0: renderModes %s, sel1 %s, key 0x%X (pass1 %b, q %d, samples %d) - Thread %s%n",
                    Region.getRenderModeString(renderModes), sel1, shaderKey, pass1, quality, sampleCount, Thread.currentThread());
        } */

        ShaderProgram sp = (ShaderProgram) shaderPrograms.get( shaderKey );
        if( null != sp ) {
            final boolean spChanged = getRenderState().setShaderProgram(gl, sp);
            if(DEBUG) {
                if( spChanged ) {
                    System.err.printf("RegionRendererImpl01.useShaderProgram.X1: GOT renderModes %s, sel1 %s, key 0x%X -> sp %d / %d (changed)%n", Region.getRenderModeString(renderModes), sel1, shaderKey, sp.program(), sp.id());
                } else {
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
        int posVp = rsVp.defaultShaderCustomization(gl, true, true);
        // rsFp.defaultShaderCustomization(gl, true, true);
        int posFp = rsFp.addGLSLVersion(gl);
        if( gl.isGLES2() && ! gl.isGLES3() ) {
            posFp = rsFp.insertShaderSource(0, posFp, ShaderCode.createExtensionDirective(GLExtensions.OES_standard_derivatives, ShaderCode.ENABLE));
        }
        final String rsFpDefPrecision =  getFragmentShaderPrecision(gl);
        if( null != rsFpDefPrecision ) {
            rsFp.insertShaderSource(0, posFp, rsFpDefPrecision);
        }
        if( Region.hasColorChannel( renderModes ) ) {
            posVp = rsVp.insertShaderSource(0, posVp, USE_COLOR_CHANNEL);
            posFp = rsFp.insertShaderSource(0, posFp, USE_COLOR_CHANNEL);
        }
        if( Region.hasColorTexture( renderModes ) ) {
            posVp = rsVp.insertShaderSource(0, posVp, USE_COLOR_TEXTURE);
            posFp = rsFp.insertShaderSource(0, posFp, USE_COLOR_TEXTURE);
        }
        if( !pass1 ) {
            posFp = rsFp.insertShaderSource(0, posFp, DEF_SAMPLE_COUNT+sel1.sampleCount+"\n");
        }

        final String passS = pass1 ? "-pass1-" : "-pass2-";
        final String shaderSegment = versionedBaseName+passS+sel1.tech+sel1.sub+".glsl";
        if(DEBUG) {
            System.err.printf("RegionRendererImpl01.useShaderProgram.1: segment %s%n", shaderSegment);
        }
        try {
            posFp = rsFp.insertShaderSource(0, -1, AttributeNames.class, shaderSegment);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to read: "+shaderSegment, ioe);
        }
        if( 0 > posFp ) {
            throw new RuntimeException("Failed to read: "+shaderSegment);
        }
        posFp = rsFp.insertShaderSource(0, -1, "}\n");

        sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);

        if( !sp.init(gl) ) {
            throw new GLException("RegionRenderer: Couldn't init program: "+sp);
        }

        if( !sp.link(gl, System.err) ) {
            throw new GLException("could not link program: "+sp);
        }
        getRenderState().setShaderProgram(gl, sp);

        shaderPrograms.put(shaderKey, sp);
        if(DEBUG) {
            System.err.printf("RegionRendererImpl01.useShaderProgram.X1: PUT renderModes %s, sel1 %s, key 0x%X -> sp %d / %d (changed)%n",
                    Region.getRenderModeString(renderModes), sel1, shaderKey, sp.program(), sp.id());
        }
        return true;
    }

}