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
import com.jogamp.opengl.GLProfile;

import jogamp.graph.curve.opengl.VBORegion2PMSAAES2;
import jogamp.graph.curve.opengl.VBORegion2PVBAAES2;
import jogamp.graph.curve.opengl.VBORegionSPES2;

import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;

import java.io.PrintStream;

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
     * Now let's assume a minimum of 10 chars will be rendered
     */

    /**
     * Default initial vertices count based on 10 chars w/ FreeSans @ 64 vertices/char avg.
     */
    public static final int defaultVerticesCount = 10*64;

    /**
     * Default initial indices count based on 10 chars w/ FreeSans @ 33 indices/char avg.
     */
    public static final int defaultIndicesCount = 10*33;

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
     * Create a GLRegion using the passed render mode
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
     * using given font's {@link Font#processString(com.jogamp.graph.curve.OutlineShape.Visitor2, CharSequence)}
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
    public static GLRegion create(final GLProfile glp, int renderModes, final TextureSequence colorTexSeq, final Font font, final CharSequence str) {
        if( null != colorTexSeq ) {
            renderModes |= Region.COLORTEXTURE_RENDERING_BIT;
        } else if( Region.hasColorTexture(renderModes) ) {
            throw new IllegalArgumentException("COLORTEXTURE_RENDERING_BIT set but null TextureSequence");
        }
        GLRegion region;
        if( isVBAA(renderModes) ) {
            region = new VBORegion2PVBAAES2(glp, renderModes, colorTexSeq, Region.DEFAULT_TWO_PASS_TEXTURE_UNIT, 0, 0);
        } else if( isMSAA(renderModes) ) {
            region = new VBORegion2PMSAAES2(glp, renderModes, colorTexSeq, Region.DEFAULT_TWO_PASS_TEXTURE_UNIT, 0, 0);
        } else {
            region = new VBORegionSPES2(glp, renderModes, colorTexSeq, 0, 0);
        }
        final int[] vertIndexCount = { 0, 0 };
        final OutlineShape.Visitor2 visitor = new OutlineShape.Visitor2() {
            @Override
            public final void visit(final OutlineShape shape) {
                region.countOutlineShape(shape, vertIndexCount);
            } };
        font.processString(visitor, str);
        region.setBufferCapacity(vertIndexCount[0], vertIndexCount[1]);
        return region;
    }

    private final int gl_idx_type;
    protected final TextureSequence colorTexSeq;

    protected GLRegion(final GLProfile glp, final int renderModes, final TextureSequence colorTexSeq) {
        super(renderModes, glp.isGL2ES3() /* use_int32_idx */);
        this.gl_idx_type = usesI32Idx() ? GL.GL_UNSIGNED_INT : GL.GL_UNSIGNED_SHORT;
        this.colorTexSeq = colorTexSeq;
    }

    protected final int glIdxType() { return this.gl_idx_type; }

    /**
     * Updates a graph region by updating the ogl related
     * objects for use in rendering if {@link #isShapeDirty()}.
     * <p>Allocates the ogl related data and initializes it the 1st time.<p>
     * <p>Called by {@link #draw(GL2ES2, RenderState, int, int, int)}.</p>
     */
    protected abstract void updateImpl(final GL2ES2 gl);

    protected abstract void destroyImpl(final GL2ES2 gl);

    protected abstract void clearImpl(final GL2ES2 gl);

    protected static void printAndCount(final PrintStream out, final String name, final GLArrayData data, final int[] size, final int[] capacity) {
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
    public GLRegion clear(final GL2ES2 gl) {
        clearImpl(gl);
        clearImpl();
        return this;
    }

    /**
     * Delete and clear the associated OGL objects.
     * <p>
     * The {@link ShaderProgram}s references are nullified but not {@link ShaderProgram#destroy(GL2ES2) destroyed}
     * as they are owned by {@link RegionRenderer}.
     * </p>
     */
    public final void destroy(final GL2ES2 gl) {
        clear(gl);
        destroyImpl(gl);
    }

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
     * @param sampleCount desired multisampling sample count for msaa-rendering.
     *        The actual used scample-count is written back when msaa-rendering is enabled, otherwise the store is untouched.
     * @see RegionRenderer#enable(GL2ES2, boolean)
     */
    public final void draw(final GL2ES2 gl, final RegionRenderer renderer, final int[/*1*/] sampleCount) {
        if( isShapeDirty() ) {
            updateImpl(gl);
        }
        drawImpl(gl, renderer, sampleCount);
        clearDirtyBits(DIRTY_SHAPE|DIRTY_STATE);
    }

    protected abstract void drawImpl(final GL2ES2 gl, final RegionRenderer renderer, final int[/*1*/] sampleCount);
}
