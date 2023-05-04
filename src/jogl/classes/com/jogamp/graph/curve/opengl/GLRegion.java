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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLArrayData;
import com.jogamp.opengl.util.GLArrayDataClient;
import com.jogamp.opengl.util.GLArrayDataEditable;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.GLArrayDataWrapper;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.Vec4f;

import jogamp.graph.curve.opengl.VBORegion2PMSAAES2;
import jogamp.graph.curve.opengl.VBORegion2PVBAAES2;
import jogamp.graph.curve.opengl.VBORegionSPES2;
import jogamp.graph.curve.opengl.shader.AttributeNames;

import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;

import java.io.PrintStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.jogamp.graph.curve.OutlineShape;

/** A GLRegion is the OGL binding of one or more OutlineShapes
 *  Defined by its vertices and generated triangles. The Region
 *  defines the final shape of the OutlineShape(s), which shall produced a shaded
 *  region on the screen.
 *
 *  Implementations of the GLRegion shall take care of the OGL
 *  binding of the depending on its context, profile.
 *
 * @see Region
 * @see OutlineShape
 */
public abstract class GLRegion extends Region {

    /**
     * Heuristics with TestTextRendererNEWT00 text_1 + text_2 = 1334 chars
     * - FreeSans     ~ vertices  64/char, indices 33/char
     * - Ubuntu Light ~ vertices 100/char, indices 50/char
     * - FreeSerif    ~ vertices 115/char, indices 61/char
     *
     * However, proper initial size is pre-calculated via ..
     * - {@link GLRegion#create(GLProfile, int, TextureSequence, Font, CharSequence)}
     * - {@Link Region#countOutlineShape(OutlineShape, int[])}
     * - {@link TextRegionUtil#countStringRegion(Font, CharSequence, int[])}
     */

    /**
     * Default initial vertices count {@value}, assuming small sized shapes.
     */
    public static final int defaultVerticesCount = 64;

    /**
     * Default initial indices count {@value}, assuming small sized shapes.
     */
    public static final int defaultIndicesCount = 64;

    // private static final float growthFactor = 1.2f; // avg +5% size but 15% more overhead (34% total)
    protected static final float growthFactor = GLArrayDataClient.DEFAULT_GROWTH_FACTOR; // avg +20% size, but 15% less CPU overhead compared to 1.2 (19% total)

    /**
     * Create a GLRegion using the passed render mode
     *
     * <p> In case {@link Region#VBAA_RENDERING_BIT} is being requested the default texture unit
     * {@link Region#DEFAULT_TWO_PASS_TEXTURE_UNIT} is being used.</p>
     * @param glp intended GLProfile to use. Instance may use higher OpenGL features if indicated by GLProfile.
     * @param renderModes bit-field of modes, e.g. {@link Region#VARWEIGHT_RENDERING_BIT}, {@link Region#VBAA_RENDERING_BIT}
     * @param colorTexSeq optional {@link TextureSequence} for {@link Region#COLORTEXTURE_RENDERING_BIT} rendering mode.
     * @param initialVerticesCount initial number of vertices in the render-buffer
     * @param initialIndicesCount initial number of indices in the render-buffer
     */
    public static GLRegion create(final GLProfile glp, int renderModes, final TextureSequence colorTexSeq, final int initialVerticesCount, final int initialIndicesCount) {
        if( null != colorTexSeq ) {
            renderModes |= Region.COLORTEXTURE_RENDERING_BIT;
        } else if( Region.hasColorTexture(renderModes) ) {
            throw new IllegalArgumentException("COLORTEXTURE_RENDERING_BIT set but null TextureSequence");
        }
        if( isVBAA(renderModes) ) {
            return new VBORegion2PVBAAES2(glp, renderModes, colorTexSeq, Region.DEFAULT_TWO_PASS_TEXTURE_UNIT, initialVerticesCount, initialIndicesCount);
        } else if( isMSAA(renderModes) ) {
            return new VBORegion2PMSAAES2(glp, renderModes, colorTexSeq, Region.DEFAULT_TWO_PASS_TEXTURE_UNIT, initialVerticesCount, initialIndicesCount);
        } else {
            return new VBORegionSPES2(glp, renderModes, colorTexSeq, initialVerticesCount, initialIndicesCount);
        }
    }

    /**
     * Create a GLRegion using the passed render mode and default initial buffer sizes {@link #defaultVerticesCount} and {@link #defaultIndicesCount}.
     *
     * <p> In case {@link Region#VBAA_RENDERING_BIT} is being requested the default texture unit
     * {@link Region#DEFAULT_TWO_PASS_TEXTURE_UNIT} is being used.</p>
     * @param glp intended GLProfile to use. Instance may use higher OpenGL features if indicated by GLProfile.
     * @param renderModes bit-field of modes, e.g. {@link Region#VARWEIGHT_RENDERING_BIT}, {@link Region#VBAA_RENDERING_BIT}
     * @param colorTexSeq optional {@link TextureSequence} for {@link Region#COLORTEXTURE_RENDERING_BIT} rendering mode.
     */
    public static GLRegion create(final GLProfile glp, final int renderModes, final TextureSequence colorTexSeq) {
        return GLRegion.create(glp, renderModes, colorTexSeq, defaultVerticesCount, defaultIndicesCount);
    }

    /**
     * Create a GLRegion using the passed render mode and pre-calculating its buffer sizes
     * using {@link Region#countOutlineShape(OutlineShape, int[])}.
     *
     * <p> In case {@link Region#VBAA_RENDERING_BIT} is being requested the default texture unit
     * {@link Region#DEFAULT_TWO_PASS_TEXTURE_UNIT} is being used.</p>
     * @param glp intended GLProfile to use. Instance may use higher OpenGL features if indicated by GLProfile.
     * @param renderModes bit-field of modes, e.g. {@link Region#VARWEIGHT_RENDERING_BIT}, {@link Region#VBAA_RENDERING_BIT}
     * @param colorTexSeq optional {@link TextureSequence} for {@link Region#COLORTEXTURE_RENDERING_BIT} rendering mode.
     * @param shape the {@link OutlineShape} used to determine {@link GLRegion}'s buffer sizes via {@link Region#countOutlineShape(OutlineShape, int[])}
     */
    public static GLRegion create(final GLProfile glp, final int renderModes, final TextureSequence colorTexSeq, final OutlineShape shape) {
        final int[/*2*/] vertIndexCount = Region.countOutlineShape(shape, new int[2]);
        return GLRegion.create(glp, renderModes, colorTexSeq, vertIndexCount[0], vertIndexCount[1]);
    }

    /**
     * Create a GLRegion using the passed render mode and pre-calculating its buffer sizes
     * using given font's {@link Font#processString(com.jogamp.graph.font.Font.GlyphVisitor2, CharSequence)}
     * to {@link #countOutlineShape(OutlineShape, int[])}.
     *
     * <p> In case {@link Region#VBAA_RENDERING_BIT} is being requested the default texture unit
     * {@link Region#DEFAULT_TWO_PASS_TEXTURE_UNIT} is being used.</p>
     * @param glp intended GLProfile to use. Instance may use higher OpenGL features if indicated by GLProfile.
     * @param renderModes bit-field of modes, e.g. {@link Region#VARWEIGHT_RENDERING_BIT}, {@link Region#VBAA_RENDERING_BIT}
     * @param colorTexSeq optional {@link TextureSequence} for {@link Region#COLORTEXTURE_RENDERING_BIT} rendering mode.
     * @param font Font used to {@link Font#processString(com.jogamp.graph.curve.OutlineShape.Visitor2, CharSequence)} to {@link #countOutlineShape(OutlineShape, int[]) to count initial number of vertices and indices}
     * @param str the string used to to {@link #countOutlineShape(OutlineShape, int[]) to count initial number of vertices and indices}
     */
    public static GLRegion create(final GLProfile glp, final int renderModes, final TextureSequence colorTexSeq, final Font font, final CharSequence str) {
        final int[] vertIndexCount = { 0, 0 };
        final Font.GlyphVisitor2 visitor = new Font.GlyphVisitor2() {
            @Override
            public final void visit(final char symbol, final Font.Glyph glyph) {
                Region.countOutlineShape(glyph.getShape(), vertIndexCount);
            } };
        font.processString(visitor, str);
        return GLRegion.create(glp, renderModes, colorTexSeq, vertIndexCount[0], vertIndexCount[1]);
    }

    private final int gl_idx_type;
    protected final TextureSequence colorTexSeq;

    // pass-1 common data
    protected int curVerticesCap = 0;
    protected int curIndicesCap = 0;
    protected int growCount = 0;

    /** Interleaved buffer for GLSL attributes: vectices, curveParams and optionally colors */
    protected GLArrayDataServer vpc_ileave = null;
    protected GLArrayDataWrapper gca_VerticesAttr = null;
    protected GLArrayDataWrapper gca_CurveParamsAttr = null;
    protected GLArrayDataWrapper gca_ColorsAttr = null;
    protected GLArrayDataServer indicesBuffer = null;

    protected GLRegion(final GLProfile glp, final int renderModes, final TextureSequence colorTexSeq) {
        super(renderModes, glp.isGL2ES3() /* use_int32_idx */);
        this.gl_idx_type = usesI32Idx() ? GL.GL_UNSIGNED_INT : GL.GL_UNSIGNED_SHORT;
        this.colorTexSeq = colorTexSeq;
    }

    protected final int glIdxType() { return this.gl_idx_type; }

    public GLArrayDataServer createInterleaved(final boolean useMappedBuffers, final int comps, final int dataType, final boolean normalized, final int initialSize, final int vboUsage) {
        if( useMappedBuffers ) {
            return GLArrayDataServer.createGLSLInterleavedMapped(comps, dataType, normalized, initialSize, vboUsage);
        } else {
            return GLArrayDataServer.createGLSLInterleaved(comps, dataType, normalized, initialSize, vboUsage);
        }
    }

    public void addInterleavedVertexAndNormalArrays(final GLArrayDataServer array, final int components) {
        array.addGLSLSubArray("vertices", components, GL.GL_ARRAY_BUFFER);
        array.addGLSLSubArray("normals", components, GL.GL_ARRAY_BUFFER);
    }

    protected final void initBuffer(final int verticeCount, final int indexCount) {
        indicesBuffer = GLArrayDataServer.createData(3, glIdxType(), indexCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
        indicesBuffer.setGrowthFactor(growthFactor);
        curIndicesCap = indicesBuffer.getElemCapacity();

        final boolean cc = hasColorChannel();
        final int totalCompsPerElem = 3 + 3 + (cc ? 4 : 0);
        vpc_ileave = GLArrayDataServer.createGLSLInterleaved(totalCompsPerElem, GL.GL_FLOAT, false /* normalized */, verticeCount, GL.GL_STATIC_DRAW);
        vpc_ileave.setGrowthFactor(growthFactor);

        gca_VerticesAttr = vpc_ileave.addGLSLSubArray(AttributeNames.VERTEX_ATTR_NAME, 3, GL.GL_ARRAY_BUFFER);
        gca_CurveParamsAttr = vpc_ileave.addGLSLSubArray(AttributeNames.CURVEPARAMS_ATTR_NAME, 3, GL.GL_ARRAY_BUFFER);
        if( cc ) {
            gca_ColorsAttr = vpc_ileave.addGLSLSubArray(AttributeNames.COLOR_ATTR_NAME, 4, GL.GL_ARRAY_BUFFER);
        }
        curVerticesCap = vpc_ileave.getElemCapacity();
        growCount = 0;
    }

    private static final boolean DEBUG_BUFFER = false;

    @Override
    public final boolean growBuffer(final int verticesCount, final int indicesCount) {
        boolean grown = false;
        if( curIndicesCap < indicesBuffer.elemPosition() + indicesCount ) {
            System.err.printf("XXX Buffer grow - Indices: %d < ( %d = %d + %d ); Status: %s%n",
                   curIndicesCap, indicesBuffer.elemPosition() + indicesCount, indicesBuffer.elemPosition(), indicesCount, indicesBuffer.elemStatsToString());
            indicesBuffer.growIfNeeded(indicesCount * indicesBuffer.getCompsPerElem());
            if( DEBUG_BUFFER ) {
                System.err.println("grew.indices 0x"+Integer.toHexString(hashCode())+": "+curIndicesCap+" -> "+indicesBuffer.getElemCapacity()+", "+indicesBuffer.elemStatsToString());
                Thread.dumpStack();
            }
            curIndicesCap = indicesBuffer.getElemCapacity();
            grown = true;
        }
        if( curVerticesCap < vpc_ileave.elemPosition() + verticesCount ) {
            System.err.printf("XXX Buffer grow - Verices: %d < ( %d = %d + %d ); Status: %s%n",
                    curVerticesCap, gca_VerticesAttr.elemPosition() + verticesCount, gca_VerticesAttr.elemPosition(), verticesCount, gca_VerticesAttr.elemStatsToString());
            vpc_ileave.growIfNeeded(verticesCount * vpc_ileave.getCompsPerElem());
            if( DEBUG_BUFFER ) {
                System.err.println("grew.vertices 0x"+Integer.toHexString(hashCode())+": "+curVerticesCap+" -> "+gca_VerticesAttr.getElemCapacity()+", "+gca_VerticesAttr.elemStatsToString());
            }
            curVerticesCap = vpc_ileave.getElemCapacity();
            grown = true;
        }
        if( grown ) {
            ++growCount;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final boolean setBufferCapacity(final int verticesCount, final int indicesCount) {
        boolean grown = false;
        if( curIndicesCap < indicesCount ) {
            indicesBuffer.reserve(indicesCount);
            curIndicesCap = indicesBuffer.getElemCapacity();
            grown = true;
        }
        if( curVerticesCap < verticesCount ) {
            vpc_ileave.reserve(verticesCount);
            curVerticesCap = vpc_ileave.getElemCapacity();
            grown = true;
        }
        return grown;
    }

    @Override
    public final void printBufferStats(final PrintStream out) {
        final int[] size= { 0 }, capacity= { 0 };
        out.println("GLRegion: idx32 "+usesI32Idx()+", obj 0x"+Integer.toHexString(hashCode()));
        printAndCount(out, "  indices ", indicesBuffer, size, capacity);
        out.println();
        printAndCount(out, "  ileave ", vpc_ileave, size, capacity);
        out.println();
        {
            print(out, "  - vertices ", gca_VerticesAttr);
            out.println();
            print(out, "  - params ", gca_CurveParamsAttr);
            out.println();
            print(out, "  - color ", gca_ColorsAttr);
            out.println();
        }
        final float filled = (float)size[0]/(float)capacity[0];
        out.printf("  total [bytes %,d / %,d], filled[%.1f%%, left %.1f%%], grow-cnt %d, obj 0x%x%n",
                size[0], capacity[0], filled*100f, (1f-filled)*100f, growCount, hashCode());
        // out.printf("  vpc_ileave: %s%n", vpc_ileave.toString());
        // out.printf("  - vertices: %s%n", gca_VerticesAttr.toString());
    }

    private static void printAndCount(final PrintStream out, final String name, final GLArrayData data, final int[] size, final int[] capacity) {
        out.print(name+"[");
        if( null != data ) {
            out.print(data.fillStatsToString());
            size[0] += data.getByteCount();
            capacity[0] += data.getByteCapacity();
            out.print("]");
        } else {
            out.print("null]");
        }
    }
    private static void print(final PrintStream out, final String name, final GLArrayData data) {
        out.print(name+"[");
        if( null != data ) {
            out.print(data.fillStatsToString());
            out.print("]");
        } else {
            out.print("null]");
        }
    }

    @Override
    protected final void pushVertex(final Vec3f coords, final Vec3f texParams, final Vec4f rgba) {
        // NIO array[3] is much slows than group/single
        // gca_VerticesAttr.putf(coords, 0, 3);
        // gca_CurveParamsAttr.putf(texParams, 0, 3);
        // gca_VerticesAttr.put3f(coords.x(), coords.y(), coords.z());
        put3f((FloatBuffer)vpc_ileave.getBuffer(), coords);
        put3f((FloatBuffer)vpc_ileave.getBuffer(), texParams);
        if( hasColorChannel() ) {
            if( null != rgba ) {
                put4f((FloatBuffer)vpc_ileave.getBuffer(), rgba);
            } else {
                throw new IllegalArgumentException("Null color given for COLOR_CHANNEL rendering mode");
            }
        }
    }

    @Override
    protected final void pushVertices(final Vec3f coords1, final Vec3f coords2, final Vec3f coords3,
                                      final Vec3f texParams1, final Vec3f texParams2, final Vec3f texParams3, final Vec4f rgba) {
        final boolean cc = hasColorChannel();
        if( cc && null == rgba ) {
            throw new IllegalArgumentException("Null color given for COLOR_CHANNEL rendering mode");
        }
        put3f((FloatBuffer)vpc_ileave.getBuffer(), coords1);
        put3f((FloatBuffer)vpc_ileave.getBuffer(), texParams1);
        if( cc ) {
            put4f((FloatBuffer)vpc_ileave.getBuffer(), rgba);
        }
        put3f((FloatBuffer)vpc_ileave.getBuffer(), coords2);
        put3f((FloatBuffer)vpc_ileave.getBuffer(), texParams2);
        if( cc ) {
            put4f((FloatBuffer)vpc_ileave.getBuffer(), rgba);
        }
        put3f((FloatBuffer)vpc_ileave.getBuffer(), coords3);
        put3f((FloatBuffer)vpc_ileave.getBuffer(), texParams3);
        if( cc ) {
            put4f((FloatBuffer)vpc_ileave.getBuffer(), rgba);
        }
    }

    @Override
    protected final void pushIndex(final int idx) {
        if( usesI32Idx() ) {
            indicesBuffer.puti(idx);
        } else {
            indicesBuffer.puts((short)idx);
        }
    }

    @Override
    protected final void pushIndices(final int idx1, final int idx2, final int idx3) {
        if( usesI32Idx() ) {
            // indicesBuffer.put3i(idx1, idx2, idx3);
            put3i((IntBuffer)indicesBuffer.getBuffer(), idx1, idx2, idx3);
        } else {
            // indicesBuffer.put3s((short)idx1, (short)idx2, (short)idx3);
            put3s((ShortBuffer)indicesBuffer.getBuffer(), (short)idx1, (short)idx2, (short)idx3);
        }
    }

    /**
     * Clears all buffers, i.e. triangles, vertices etc and and resets states accordingly, see {@link GLArrayDataEditable#clear(GL)}.
     * <p>
     * This method does not actually erase the data in the buffer and will most often be used when erasing the underlying memory is suitable.
     * </p>
     *
     * @param gl the current {@link GL2ES2} object
     * @return this {@link GLRegion} for chaining.
     * @see GLArrayDataEditable#clear(GL)
     */
    public final GLRegion clear(final GL2ES2 gl) {
        lastRenderModes = 0;
        if(DEBUG_INSTANCE) {
            System.err.println("GLRegion Clear: " + this);
        }
        if( null != indicesBuffer ) {
            indicesBuffer.clear(gl);
        }
        if( null != vpc_ileave ) {
            vpc_ileave.clear(gl);
        }
        clearImpl(gl);
        clearImpl();
        return this;
    }
    protected abstract void clearImpl(final GL2ES2 gl);

    /**
     * Delete and clear the associated OGL objects.
     * <p>
     * The {@link ShaderProgram}s references are nullified but not {@link ShaderProgram#destroy(GL2ES2) destroyed}
     * as they are owned by {@link RegionRenderer}.
     * </p>
     */
    public final void destroy(final GL2ES2 gl) {
        clear(gl);
        if( null != vpc_ileave ) {
            vpc_ileave.destroy(gl);
            vpc_ileave = null;
        }
        if( null != gca_VerticesAttr ) {
            gca_VerticesAttr.destroy(gl);
            gca_VerticesAttr = null;
        }
        if( null != gca_CurveParamsAttr ) {
            gca_CurveParamsAttr.destroy(gl);
            gca_CurveParamsAttr = null;
        }
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.destroy(gl);
            gca_ColorsAttr = null;
        }
        if(null != indicesBuffer) {
            indicesBuffer.destroy(gl);
            indicesBuffer = null;
        }
        curVerticesCap = 0;
        curIndicesCap = 0;
        growCount = 0;
        destroyImpl(gl);
    }
    protected abstract void destroyImpl(final GL2ES2 gl);

    /**
     * Renders the associated OGL objects specifying
     * current width/hight of window for multi pass rendering
     * of the region.
     * <p>
     * User shall consider {@link RegionRenderer#enable(GL2ES2, boolean) enabling}
     * the renderer beforehand and {@link RegionRenderer#enable(GL2ES2, boolean) disabling}
     * it afterwards when used in conjunction with other renderer.
     * </p>
     * <p>
     * Users shall also consider setting the {@link GL#glClearColor(float, float, float, float) clear-color}
     * appropriately:
     * <ul>
     *   <li>If {@link GL#GL_BLEND blending} is enabled, <i>RGB</i> shall be set to text color, otherwise
     *       blending will reduce the alpha seam's contrast and the font will appear thinner.</li>
     *   <li>If {@link GL#GL_BLEND blending} is disabled, <i>RGB</i> shall be set to the actual desired background.</li>
     * </ul>
     * The <i>alpha</i> component shall be set to zero.
     * Note: If {@link GL#GL_BLEND blending} is enabled, the
     * {@link RegionRenderer} might need to be
     * {@link RegionRenderer#create(Vertex.Factory<? extends Vertex>, RenderState, com.jogamp.graph.curve.opengl.RegionRenderer.GLCallback, com.jogamp.graph.curve.opengl.RegionRenderer.GLCallback) created}
     * with the appropriate {@link RegionRenderer.GLCallback callbacks}.
     * </p>
     * @param matrix current {@link PMVMatrix}.
     * @param renderer the {@link RegionRenderer} to be used
     * @param sampleCount desired multisampling sample count for vbaa- or msaa-rendering.
     *        Use -1 for glSelect mode, pass1 w/o any color texture nor channel, use static select color only.
     *        The actual used scample-count is written back when msaa-rendering is enabled, otherwise the store is untouched.
     * @see RegionRenderer#enable(GL2ES2, boolean)
     */
    public final void draw(final GL2ES2 gl, final RegionRenderer renderer, final int[/*1*/] sampleCount) {
        final int curRenderModes;
        if( null == sampleCount || 0 == sampleCount[0] ) {
            // no sampling, reduce to pass1
            curRenderModes = getRenderModes() & ~( VBAA_RENDERING_BIT | MSAA_RENDERING_BIT );
        } else if( 0 > sampleCount[0] ) {
            // negative sampling, hint we perform glSelect: pass1 w/o any color texture nor channel, use static select color only
            curRenderModes = getRenderModes() & ~( VBAA_RENDERING_BIT | MSAA_RENDERING_BIT | COLORCHANNEL_RENDERING_BIT | COLORTEXTURE_RENDERING_BIT );
        } else {
            // normal 2-pass sampling
            curRenderModes = getRenderModes();
        }
        if( lastRenderModes != curRenderModes ) {
            markShapeDirty();
            markStateDirty();
        }
        if( isShapeDirty() ) {
            updateImpl(gl, curRenderModes);
        }
        drawImpl(gl, renderer, curRenderModes, sampleCount);
        clearDirtyBits(DIRTY_SHAPE|DIRTY_STATE);
        lastRenderModes = curRenderModes;
    }
    private int lastRenderModes = 0;

    /**
     * Updates a graph region by updating the ogl related
     * objects for use in rendering if {@link #isShapeDirty()}.
     * <p>Allocates the ogl related data and initializes it the 1st time.<p>
     * <p>Called by {@link #draw(GL2ES2, RenderState, int, int, int)}.</p>
     * @param curRenderModes TODO
     */
    protected abstract void updateImpl(final GL2ES2 gl, int curRenderModes);

    protected abstract void drawImpl(final GL2ES2 gl, final RegionRenderer renderer, int curRenderModes, final int[/*1*/] sampleCount);
}
