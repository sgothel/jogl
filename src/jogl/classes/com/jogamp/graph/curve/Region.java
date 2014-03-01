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
package com.jogamp.graph.curve;

import java.util.ArrayList;
import java.util.List;

import jogamp.graph.curve.opengl.VBORegion2PVBAAES2;
import jogamp.graph.curve.opengl.VBORegion2PMSAAES2;
import jogamp.graph.curve.opengl.VBORegionSPES2;
import jogamp.graph.geom.plane.AffineTransform;
import jogamp.opengl.Debug;

import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.math.geom.AABBox;

/**
 * Abstract Outline shape representation define the method an OutlineShape(s)
 * is bound and rendered.
 *
 * @see GLRegion */
public abstract class Region {

    /** Debug flag for region impl (graph.curve) */
    public static final boolean DEBUG = Debug.debug("graph.curve");
    public static final boolean DEBUG_INSTANCE = Debug.debug("graph.curve.instance");

    /**
     * MSAA based Anti-Aliasing, a two pass region rendering, slower and more
     * resource hungry (FBO), but providing fast MSAA in case
     * the whole scene is not rendered with MSAA.
     */
    public static final int MSAA_RENDERING_BIT        = 1 << 0;

    /**
     * View based Anti-Aliasing, a two pass region rendering, slower and more
     * resource hungry (FBO), but AA is perfect. Otherwise the default fast one
     * pass MSAA region rendering is being used.
     */
    public static final int VBAA_RENDERING_BIT        = 1 << 1;

    /**
     * Use non uniform weights [0.0 .. 1.9] for curve region rendering.
     * Otherwise the default weight 1.0 for uniform curve region rendering is
     * being applied.
     */
    public static final int VARIABLE_CURVE_WEIGHT_BIT = 1 << 8;

    public static final int TWO_PASS_DEFAULT_TEXTURE_UNIT = 0;

    private final int renderModes;
    private boolean dirty = true;
    private int numVertices = 0;
    protected final AABBox box = new AABBox();

    public static boolean isVBAA(int renderModes) {
        return 0 != (renderModes & Region.VBAA_RENDERING_BIT);
    }
    public static boolean isMSAA(int renderModes) {
        return 0 != (renderModes & Region.MSAA_RENDERING_BIT);
    }
    public static String getRenderModeString(int renderModes) {
        if( Region.isVBAA(renderModes) ) {
            return "vbaa";
        } else if( Region.isMSAA(renderModes) ) {
            return "msaa";
        } else {
            return "norm" ;
        }
    }

    /**
     * Check if render mode capable of non uniform weights
     *
     * @param renderModes
     *            bit-field of modes, e.g.
     *            {@link Region#VARIABLE_CURVE_WEIGHT_BIT},
     *            {@link Region#VBAA_RENDERING_BIT}
     * @return true of capable of non uniform weights */
    public static boolean isNonUniformWeight(int renderModes) {
        return 0 != (renderModes & Region.VARIABLE_CURVE_WEIGHT_BIT);
    }

    /**
     * Create a Region using the passed render mode
     *
     * <p> In case {@link Region#VBAA_RENDERING_BIT} is being requested the default texture unit
     * {@link Region#TWO_PASS_DEFAULT_TEXTURE_UNIT} is being used.</p>
     *
     * @param rs the RenderState to be used
     * @param renderModes bit-field of modes, e.g. {@link Region#VARIABLE_CURVE_WEIGHT_BIT}, {@link Region#VBAA_RENDERING_BIT}
     */
    public static GLRegion create(int renderModes) {
        if( isVBAA(renderModes) ) {
            return new VBORegion2PVBAAES2(renderModes, Region.TWO_PASS_DEFAULT_TEXTURE_UNIT);
        } else if( isMSAA(renderModes) ) {
            return new VBORegion2PMSAAES2(renderModes, Region.TWO_PASS_DEFAULT_TEXTURE_UNIT);
        } else {
            return new VBORegionSPES2(renderModes);
        }
    }

    protected Region(int regionRenderModes) {
        this.renderModes = regionRenderModes;
    }

    // FIXME: Better handling of impl. buffer growth .. !

    protected abstract void pushVertex(float[] coords, float[] texParams);
    protected abstract void pushIndex(int idx);

    /**
     * Return bit-field of render modes, see {@link #create(int)}.
     */
    public final int getRenderModes() {
        return renderModes;
    }

    protected void clearImpl() {
        dirty = true;
        numVertices = 0;
        box.reset();
    }

    /**
     * Return  true if capable of two pass rendering - VBAA, otherwise false.
     */
    public final boolean isVBAA() {
        return isVBAA(renderModes);
    }

    /**
     * Return  true if capable of two pass rendering - MSAA, otherwise false.
     */
    public final boolean isMSAA() {
        return isMSAA(renderModes);
    }

    /**
     * Return true if capable of nonuniform weights, otherwise false.
     */
    public final boolean isNonUniformWeight() {
        return Region.isNonUniformWeight(renderModes);
    }

    final float[] coordsEx = new float[3];

    private void pushNewVertexImpl(final Vertex vertIn, final AffineTransform transform) {
        if( null != transform ) {
            final float[] coordsIn = vertIn.getCoord();
            transform.transform(coordsIn, coordsEx);
            coordsEx[2] = coordsIn[2];
            box.resize(coordsEx[0], coordsEx[1], coordsEx[2]);
            pushVertex(coordsEx, vertIn.getTexCoord());
        } else {
            box.resize(vertIn.getX(), vertIn.getY(), vertIn.getZ());
            pushVertex(vertIn.getCoord(), vertIn.getTexCoord());
        }
        numVertices++;
    }

    private void pushNewVertexIdxImpl(final Vertex vertIn, final AffineTransform transform) {
        pushIndex(numVertices);
        pushNewVertexImpl(vertIn, transform);
    }

    public final void addOutlineShape(final OutlineShape shape, final AffineTransform transform) {
        final List<Triangle> trisIn = shape.getTriangles(OutlineShape.VerticesState.QUADRATIC_NURBS);
        final ArrayList<Vertex> vertsIn = shape.getVertices();
        if(DEBUG_INSTANCE) {
            System.err.println("Region.addOutlineShape().0: tris: "+trisIn.size()+", verts "+vertsIn.size()+", transform "+transform);
        }
        final int idxOffset = numVertices;
        int vertsVNewIdxCount = 0, vertsTMovIdxCount = 0, vertsTNewIdxCount = 0, tris = 0;
        int vertsDupCountV = 0, vertsDupCountT = 0, vertsKnownMovedT = 0;
        if( vertsIn.size() >= 3 ) {
            if(DEBUG_INSTANCE) {
                System.err.println("Region.addOutlineShape(): Processing Vertices");
            }
            for(int i=0; i<vertsIn.size(); i++) {
                pushNewVertexImpl(vertsIn.get(i), transform);
                vertsVNewIdxCount++;
            }
            if(DEBUG_INSTANCE) {
                System.err.println("Region.addOutlineShape(): Processing Triangles");
            }
            for(int i=0; i<trisIn.size(); i++) {
                final Triangle triIn = trisIn.get(i);
                if(Region.DEBUG_INSTANCE) {
                    System.err.println("T["+i+"]: "+triIn);
                }
                // triEx.addVertexIndicesOffset(idxOffset);
                // triangles.add( triEx );
                final Vertex[] triInVertices = triIn.getVertices();
                final int tv0Idx = triInVertices[0].getId();
                if( Integer.MAX_VALUE-idxOffset > tv0Idx ) { // Integer.MAX_VALUE != i0 // FIXME: renderer uses SHORT!
                    // valid 'known' idx - move by offset
                    if(Region.DEBUG_INSTANCE) {
                        System.err.println("T["+i+"]: Moved "+tv0Idx+" + "+idxOffset+" -> "+(tv0Idx+idxOffset));
                    }
                    pushIndex(tv0Idx+idxOffset);
                    pushIndex(triInVertices[1].getId()+idxOffset);
                    pushIndex(triInVertices[2].getId()+idxOffset);
                    vertsTMovIdxCount+=3;
                } else {
                    // invalid idx - generate new one
                    if(Region.DEBUG_INSTANCE) {
                        System.err.println("T["+i+"]: New Idx "+numVertices);
                    }
                    pushNewVertexIdxImpl(triInVertices[0], transform);
                    pushNewVertexIdxImpl(triInVertices[1], transform);
                    pushNewVertexIdxImpl(triInVertices[2], transform);
                    vertsTNewIdxCount+=3;
                }
                tris++;
            }
        }
        if(DEBUG_INSTANCE) {
            System.err.println("Region.addOutlineShape().X: idxOffset "+idxOffset+", tris: "+tris+", verts [idx "+vertsTNewIdxCount+", add "+vertsTNewIdxCount+" = "+(vertsVNewIdxCount+vertsTNewIdxCount)+"]");
            System.err.println("Region.addOutlineShape().X: verts: idx[v-new "+vertsVNewIdxCount+", t-new "+vertsTNewIdxCount+" = "+(vertsVNewIdxCount+vertsTNewIdxCount)+"]");
            System.err.println("Region.addOutlineShape().X: verts: idx t-moved "+vertsTMovIdxCount+", numVertices "+numVertices);
            System.err.println("Region.addOutlineShape().X: verts: v-dups "+vertsDupCountV+", t-dups "+vertsDupCountT+", t-known "+vertsKnownMovedT);
            // int vertsDupCountV = 0, vertsDupCountT = 0;
            System.err.println("Region.addOutlineShape().X: box "+box);
        }
        setDirty(true);
    }

    public final void addOutlineShapes(final List<OutlineShape> shapes, final AffineTransform transform) {
        for (int i = 0; i < shapes.size(); i++) {
            addOutlineShape(shapes.get(i), transform);
        }
    }

    /** @return the AxisAligned bounding box of current region */
    public final AABBox getBounds() {
        return box;
    }

    /** Check if this region is dirty. A region is marked dirty when new
     * Vertices, Triangles, and or Lines are added after a call to update()
     *
     * @return true if region is Dirty, false otherwise
     *
     * @see update(GL2ES2) */
    public final boolean isDirty() {
        return dirty;
    }

    protected final void setDirty(boolean v) {
        dirty = v;
    }
}