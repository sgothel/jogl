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
package com.jogamp.graph.ui;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * Graph based {@link GLRegion} {@link Shape}
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * GraphUI is intended to become an immediate- and retained-mode API.
 * </p>
 * @see Scene
 */
public abstract class GraphShape extends Shape {
    protected final int renderModesReq;
    protected int renderModes;
    protected int pass2TexUnit = GLRegion.DEFAULT_TWO_PASS_TEXTURE_UNIT;
    protected GLRegion region = null;
    protected float oshapeSharpness = OutlineShape.DEFAULT_SHARPNESS;
    private int regionPass2Quality = Region.MAX_AA_QUALITY;
    private final List<GLRegion> dirtyRegions = new ArrayList<GLRegion>();

    /**
     * Create a generic Graph based {@link GLRegion} UI {@link Shape}.
     *
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     */
    protected GraphShape(final int renderModes) {
        super();
        this.renderModesReq = renderModes;
        this.renderModes = renderModes;
    }

    /** Returns requested Graph {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}. */
    public final int getRenderModesReq() { return renderModesReq; }

    /**
     * Returns validated Graph {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * <p>
     * May differ from {@link #getRenderModesReq()}, e.g. adding a {@link Region#COLORCHANNEL_RENDERING_BIT} for {@link #hasBorder()} etc.
     * </p>
     * <p>
     * Potentially modified during {@link #validate(GL2ES2)} or {@link #validate(GLProfile)}.
     * </p>
     */
    public final int getRenderModes() { return renderModes; }

    /**
     * Sets the shape's Graph {@link Region}'s pass2 AA-quality parameter. Default is {@link Region#MAX_AA_QUALITY}.
     * @param q Graph {@link Region}'s pass2 AA-quality parameter, default is {@link Region#MAX_AA_QUALITY}.
     * @return this shape for chaining.
     */
    public final GraphShape setAAQuality(final int q) {
        this.regionPass2Quality = q;
        markStateDirty();
        return this;
    }

    /** Set the 2nd pass texture unit. */
    public void setTextureUnit(final int pass2TexUnit) {
        this.pass2TexUnit = pass2TexUnit;
        if( null != region ) {
            region.setTextureUnit(pass2TexUnit);
        }
    }

    /**
     * Return the shape's Graph {@link Region}'s quality parameter.
     * @see #setAAQuality(int)
     */
    public final int getAAQuality() { return regionPass2Quality; }

    /**
     * Sets the shape's Graph {@link OutlineShape}'s sharpness parameter. Default is {@link OutlineShape#DEFAULT_SHARPNESS}.
     *
     * Method issues {@link #markShapeDirty()}.
     *
     * @param sharpness Graph {@link OutlineShape}'s sharpness value, default is {@link OutlineShape#DEFAULT_SHARPNESS}.
     * @return this shape for chaining.
     */
    public final GraphShape setSharpness(final float sharpness) {
        this.oshapeSharpness = sharpness;
        markShapeDirty();
        return this;
    }
    /**
     * Return the shape's Graph {@link OutlineShape}'s sharpness value.
     * @see #setSharpness(float)
     */
    public final float getSharpness() {
        return oshapeSharpness;
    }

    @Override
    public boolean hasColorChannel() {
        return Region.hasColorChannel(renderModes) || Region.hasColorTexture(renderModes);
    }

    private final void clearDirtyRegions(final GL2ES2 gl) {
        for(final GLRegion r : dirtyRegions) {
            r.destroy(gl);
        }
        dirtyRegions.clear();
    }

    @Override
    protected final void clearImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        clearImpl(gl, renderer);
        clearDirtyRegions(gl);
        if( null != region ) {
            region.clear(gl);
        }
    }

    @Override
    protected final void destroyImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        destroyImpl(gl, renderer);
        clearDirtyRegions(gl);
        if( null != region ) {
            region.destroy(gl);
            region = null;
        }
    }

    @Override
    protected final void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount, final Vec4f rgba) {
        renderer.setColorStatic(rgba);
        region.draw(gl, renderer, regionPass2Quality, sampleCount);
    }

    @Override
    protected final void drawToSelectImpl0(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        region.draw(gl, renderer, 0, sampleCount);
    }

    /**
     * Reset the {@link GLRegion} and reserving its buffers to have a free capacity for `vertexCount` and `indexCount` elements.
     *
     * In case {@link GLRegion} is `null`, a new instance is being created.
     *
     * In case the {@link GLRegion} already exists, it will be either {@link GLRegion#clear(GL2ES2) cleared} if the {@link GL2ES2} `gl`
     * instance is not `null` or earmarked for deletion at a later time and a new instance is being created.
     *
     * Method shall be invoked by the {@link #addShapeToRegion(GLProfile, GL2ES2)} implementation
     * before actually adding the {@link OutlineShape} to the {@link GLRegion}.
     *
     * {@link #addShapeToRegion(GLProfile, GL2ES2)} is capable to determine initial `vertexCount` and `indexCount` buffer sizes,
     * as it composes the {@link OutlineShape}s to be added.
     *
     * {@link #resetGLRegion(GLProfile, GL2ES2, TextureSequence, OutlineShape)} maybe used for convenience.
     *
     * @param glp the used GLProfile, never `null`
     * @param gl the optional current {@link GL2ES2} instance, maybe `null`.
     * @param colorTexSeq optional {@link TextureSequence} for {@link Region#COLORTEXTURE_RENDERING_BIT} rendering mode.
     * @param vertexCount the initial {@link GLRegion} vertex buffer size
     * @param indexCount the initial {@link GLRegion} index buffer size
     * @see #resetGLRegion(GLProfile, GL2ES2, TextureSequence, OutlineShape)
     */
    protected final void resetGLRegion(final GLProfile glp, final GL2ES2 gl, final TextureSequence colorTexSeq, int vertexCount, int indexCount) {
        if( hasBorder() ) {
            vertexCount += 8;
            indexCount += 24;
        }
        if( null == region ) {
            region = GLRegion.create(glp, renderModes, colorTexSeq, pass2TexUnit, vertexCount, indexCount);
        } else if( null == gl ) {
            dirtyRegions.add(region);
            region = GLRegion.create(glp, renderModes, colorTexSeq, pass2TexUnit, vertexCount, indexCount);
        } else {
            region.clear(gl);
            region.setBufferCapacity(vertexCount, indexCount);
        }
    }
    /**
     * Convenient {@link #resetGLRegion(GLProfile, GL2ES2, TextureSequence, int, int)} variant determining initial
     * {@link GLRegion} buffer sizes via {@link Region#countOutlineShape(OutlineShape, int[])}.
     *
     * @param glp the used GLProfile, never `null`
     * @param gl the optional current {@link GL2ES2} instance, maybe `null`.
     * @param colorTexSeq optional {@link TextureSequence} for {@link Region#COLORTEXTURE_RENDERING_BIT} rendering mode.
     * @param shape the {@link OutlineShape} used to determine {@link GLRegion}'s buffer sizes via {@link Region#countOutlineShape(OutlineShape, int[])}
     * @see #resetGLRegion(GLProfile, GL2ES2, TextureSequence, int, int)
     */
    protected final void resetGLRegion(final GLProfile glp, final GL2ES2 gl, final TextureSequence colorTexSeq, final OutlineShape shape) {
        final int[/*2*/] vertIndexCount = Region.countOutlineShape(shape, new int[2]);
        resetGLRegion(glp, gl, colorTexSeq, vertIndexCount[0], vertIndexCount[1]);
    }

    @Override
    protected final void validateImpl(final GLProfile glp, final GL2ES2 gl) {
        if( null != gl ) {
            clearDirtyRegions(gl);
        }
        if( isShapeDirty() ) {
            if( hasBorder() && !rgbaColor.isEqual(getBorderColor()) && !Region.hasColorChannel(renderModes) ) {
                renderModes |= Region.COLORCHANNEL_RENDERING_BIT;
            }
            // box has been reset
            addShapeToRegion(glp, gl); // calls updateGLRegion(..)
            if( hasBorder() ) {
                // Also takes padding into account
                addRectangle(region, oshapeSharpness, box, getPadding(), getBorderThickness(), getBorderColor());
                setRotationPivot( box.getCenter() );
            } else if( hasPadding() ) {
                final Padding p = getPadding();
                final Vec3f l = box.getLow();
                final Vec3f h = box.getHigh();
                box.resize(l.x() - p.left, l.y() - p.bottom, l.z());
                box.resize(h.x() + p.right, h.y() + p.top, l.z());
                setRotationPivot( box.getCenter() );
            }
        } else if( isStateDirty() ) {
            region.markStateDirty();
        }
    }

    static protected void addRectangle(final Region region, final float sharpness, final AABBox box, final Padding padding, final float borderThickness, final Vec4f color) {
        final OutlineShape shape = new OutlineShape();
        final Padding p = null != padding ? padding : Padding.None;
        final float x1 = box.getMinX() - p.left;
        final float x2 = box.getMaxX() + p.right;
        final float y1 = box.getMinY() - p.bottom;
        final float y2 = box.getMaxY() + p.top;
        final float z = box.getMaxZ(); // on top
        {
            // Outer OutlineShape as Winding.CCW.
            shape.moveTo(x1, y1, z);
            shape.lineTo(x2, y1, z);
            shape.lineTo(x2, y2, z);
            shape.lineTo(x1, y2, z);
            shape.lineTo(x1, y1, z);
            shape.closeLastOutline(true);
            shape.addEmptyOutline();
        }
        {
            // Inner OutlineShape as Winding.CW.
            final float dxy = borderThickness;
            shape.moveTo(x1+dxy, y1+dxy, z);
            shape.lineTo(x1+dxy, y2-dxy, z);
            shape.lineTo(x2-dxy, y2-dxy, z);
            shape.lineTo(x2-dxy, y1+dxy, z);
            shape.lineTo(x1+dxy, y1+dxy, z);
            shape.closeLastOutline(true);
        }
        shape.setIsQuadraticNurbs();
        shape.setSharpness(sharpness);
        region.addOutlineShape(shape, null, color);
        box.resize(shape.getBounds()); // border <-> shape = padding, and part of shape size
    }

    protected void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) { }

    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) { }

    protected abstract void addShapeToRegion(GLProfile glp, GL2ES2 gl);

    @Override
    public String getSubString() {
        if( renderModesReq != renderModes ) {
            return super.getSubString()+", renderMode[req "+Region.getRenderModeString(renderModesReq) + ", has "+Region.getRenderModeString(renderModes)+"]";
        } else {
            return super.getSubString()+", renderMode "+Region.getRenderModeString(renderModesReq);
        }
    }
}
