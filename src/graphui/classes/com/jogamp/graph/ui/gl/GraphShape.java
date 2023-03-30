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
package com.jogamp.graph.ui.gl;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * Graph based {@link GLRegion} UI {@link Shape}
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * GraphUI is intended to become an immediate- and retained-mode API.
 * </p>
 * @see Scene
 */
public abstract class GraphShape extends Shape {
    protected final Factory<? extends Vertex> vertexFactory;

    protected final int renderModes;
    protected GLRegion region = null;
    protected float oshapeSharpness = OutlineShape.DEFAULT_SHARPNESS;
    private int regionQuality = Region.MAX_QUALITY;
    private final List<GLRegion> dirtyRegions = new ArrayList<GLRegion>();

    /**
     * Create a Graph based {@link GLRegion} UI {@link Shape}.
     *
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     */
    public GraphShape(final int renderModes) {
        super();
        this.vertexFactory = OutlineShape.getDefaultVertexFactory();
        this.renderModes = renderModes;
    }

    /** Return Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}. */
    public final int getRenderModes() { return renderModes; }

    public final int getQuality() { return regionQuality; }
    public final void setQuality(final int q) {
        this.regionQuality = q;
        if( null != region ) {
            region.setQuality(q);
        }
    }
    public final void setSharpness(final float sharpness) {
        this.oshapeSharpness = sharpness;
        markShapeDirty();
    }
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
    protected final void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount, final float[] rgba) {
        if( null != rgba ) {
            renderer.getRenderState().setColorStatic(rgba);
        }
        region.draw(gl, renderer, sampleCount);
    }

    protected GLRegion createGLRegion(final GLProfile glp) {
        return GLRegion.create(glp, renderModes, null);
    }

    @Override
    protected final void validateImpl(final GLProfile glp, final GL2ES2 gl) {
        if( null != gl ) {
            clearDirtyRegions(gl);
        }
        if( isShapeDirty() || null == region ) {
            if( null == region ) {
                region = createGLRegion(glp);
            } else if( null == gl ) {
                dirtyRegions.add(region);
                region = createGLRegion(glp);
            } else {
                region.clear(gl);
            }
            addShapeToRegion();
            region.setQuality(regionQuality);
        } else if( isStateDirty() ) {
            region.markStateDirty();
        }
    }

    protected OutlineShape createDebugOutline(final OutlineShape shape, final AABBox box) {
        final float d = 0.025f;
        final float tw = box.getWidth() + d*2f;
        final float th = box.getHeight() + d*2f;

        final float minX = box.getMinX() - d;
        final float minY = box.getMinY() - d;
        final float z = 0; // box.getMinZ() + 0.025f;

        // CCW!
        shape.moveTo(minX, minY, z);
        shape.lineTo(minX+tw, minY, z);
        shape.lineTo(minX+tw, minY + th, z);
        shape.lineTo(minX,    minY + th, z);
        shape.closePath();

        // shape.addVertex(minX,    minY,      z, true);
        // shape.addVertex(minX+tw, minY,      z, true);
        // shape.addVertex(minX+tw, minY + th, z, true);
        // shape.addVertex(minX,    minY + th, z, true);
        // shape.closeLastOutline(true);

        return shape;
    }

    protected void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) { }

    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) { }


}
