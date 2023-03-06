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
package com.jogamp.graph.curve;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jogamp.opengl.Debug;

import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Clock;
import com.jogamp.common.util.PerfCounterCtrl;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.math.geom.Frustum;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * Abstract Outline shape representation define the method an OutlineShape(s)
 * is bound and rendered.
 *
 * @see GLRegion
 */
public abstract class Region {

    /** Debug flag for region impl (graph.curve) */
    public static final boolean DEBUG = Debug.debug("graph.curve");
    public static final boolean DEBUG_INSTANCE = Debug.debug("graph.curve.Instance");

    /**
     * Rendering-Mode bit for {@link #getRenderModes() Region}
     * <p>
     * MSAA based Anti-Aliasing, a two pass region rendering, slower and more
     * resource hungry (FBO), but providing fast MSAA in case
     * the whole scene is not rendered with MSAA.
     * </p>
     */
    public static final int MSAA_RENDERING_BIT        = 1 <<  0;

    /**
     * Rendering-Mode bit for {@link #getRenderModes() Region}
     * <p>
     * View based Anti-Aliasing, a two pass region rendering, slower and more
     * resource hungry (FBO), but AA is perfect. Otherwise the default fast one
     * pass MSAA region rendering is being used.
     * </p>
     */
    public static final int VBAA_RENDERING_BIT        = 1 <<  1;

    /**
     * Rendering-Mode bit for {@link #getRenderModes() Region}
     * <p>
     * Use non uniform weights [0.0 .. 1.9] for curve region rendering.
     * Otherwise the default weight 1.0 for uniform curve region rendering is
     * being applied.
     * </p>
     */
    public static final int VARWEIGHT_RENDERING_BIT    = 1 <<  8;

    /**
     * Rendering-Mode bit for {@link #getRenderModes() Region}
     * <p>
     * If set, a color channel attribute per vertex is added to the stream,
     * otherwise only the
     * {@link com.jogamp.graph.curve.opengl.RegionRenderer#setColorStatic(com.jogamp.opengl.GL2ES2, float, float, float, float) static color}
     * is being used.
     * </p>
     */
    public static final int COLORCHANNEL_RENDERING_BIT = 1 <<  9;

    /**
     * Rendering-Mode bit for {@link #getRenderModes() Region}
     * <p>
     * If set, a color texture is used to determine the color.
     * </p>
     */
    public static final int COLORTEXTURE_RENDERING_BIT = 1 <<  10;

    /** Default maximum {@link #getQuality() quality}, {@value}. */
    public static final int MAX_QUALITY  = 1;

    public static final int DEFAULT_TWO_PASS_TEXTURE_UNIT = 0;

    protected static final int DIRTY_SHAPE    = 1 << 0 ;
    protected static final int DIRTY_STATE    = 1 << 1 ;

    private final int renderModes;
    private final boolean use_int32_idx;
    private final int max_indices;
    private int quality;
    private int dirty = DIRTY_SHAPE | DIRTY_STATE;
    private int numVertices = 0;
    protected final AABBox box = new AABBox();
    protected Frustum frustum = null;

    public static boolean isVBAA(final int renderModes) {
        return 0 != (renderModes & Region.VBAA_RENDERING_BIT);
    }

    public static boolean isMSAA(final int renderModes) {
        return 0 != (renderModes & Region.MSAA_RENDERING_BIT);
    }

    public static boolean isTwoPass(final int renderModes) {
        return 0 != ( renderModes & ( Region.VBAA_RENDERING_BIT | Region.MSAA_RENDERING_BIT) );
    }

    /**
     * Returns true if render mode capable of variable weights,
     * i.e. the bit {@link #VARWEIGHT_RENDERING_BIT} is set,
     * otherwise false.
     */
    public static boolean hasVariableWeight(final int renderModes) {
        return 0 != (renderModes & Region.VARWEIGHT_RENDERING_BIT);
    }

    /**
     * Returns true if render mode has a color channel,
     * i.e. the bit {@link #COLORCHANNEL_RENDERING_BIT} is set,
     * otherwise false.
     */
    public static boolean hasColorChannel(final int renderModes) {
        return 0 != (renderModes & Region.COLORCHANNEL_RENDERING_BIT);
    }

    /**
     * Returns true if render mode has a color texture,
     * i.e. the bit {@link #COLORTEXTURE_RENDERING_BIT} is set,
     * otherwise false.
     */
    public static boolean hasColorTexture(final int renderModes) {
        return 0 != (renderModes & Region.COLORTEXTURE_RENDERING_BIT);
    }

    public static String getRenderModeString(final int renderModes) {
        final String curveS = hasVariableWeight(renderModes) ? "-curve" : "";
        final String cChanS = hasColorChannel(renderModes) ? "-cols" : "";
        final String cTexS = hasColorTexture(renderModes) ? "-ctex" : "";
        if( Region.isVBAA(renderModes) ) {
            return "vbaa"+curveS+cChanS+cTexS;
        } else if( Region.isMSAA(renderModes) ) {
            return "msaa"+curveS+cChanS+cTexS;
        } else {
            return "norm"+curveS+cChanS+cTexS;
        }
    }

    protected Region(final int regionRenderModes, final boolean use_int32_idx) {
        this.renderModes = regionRenderModes;
        this.use_int32_idx = use_int32_idx;
        if( use_int32_idx ) {
            this.max_indices = GL_INT32_MAX / Buffers.SIZEOF_INT; // byte-size int32_t limit
        } else {
            this.max_indices = GL_UINT16_MAX;
        }
        this.quality = MAX_QUALITY;
    }

    /** Print implementation buffer stats like detailed and total size and capacity in bytes etc */
    public abstract void printBufferStats(PrintStream out);

    /**
     * Returns true if implementation uses `int32_t` sized indices implying at least a {@link GLProfile#isGL2ES3()} alike context.
     * Otherwise method returns false on {@link GLProfile#isGLES2()} using `uint16_t` sized indices.
     */
    public final boolean usesI32Idx() { return this.use_int32_idx; }

    /**
     * Allow the renderer buffers to pre-emptively grow for given vertices- and index counts.
     * @param verticeCount number of vertices to hold
     * @param indexCount number of indices to hold
     */
    public abstract void growBufferSize(int verticeCount, int indexCount);

    protected abstract void pushVertex(final float[] coords, final float[] texParams, float[] rgba);
    protected abstract void pushVertices(final float[] coords1, final float[] coords2, final float[] coords3,
                                         final float[] texParams1, final float[] texParams2, final float[] texParams3, float[] rgba);
    protected abstract void pushIndex(int idx);
    protected abstract void pushIndices(int idx1, int idx2, int idx3);

    /**
     * Return bit-field of render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence)}.
     */
    public final int getRenderModes() { return renderModes; }

    /** See {@link #MAX_QUALITY} */
    public final int getQuality() { return quality; }

    /** See {@link #MAX_QUALITY} */
    public final void setQuality(final int q) { quality=q; }

    protected void clearImpl() {
        dirty = DIRTY_SHAPE | DIRTY_STATE;
        numVertices = 0;
        box.reset();
    }

    /**
     * Returns true if capable of two pass rendering - VBAA, otherwise false.
     */
    public final boolean isVBAA() {
        return Region.isVBAA(renderModes);
    }

    /**
     * Returns true if capable of two pass rendering - MSAA, otherwise false.
     */
    public final boolean isMSAA() {
        return Region.isMSAA(renderModes);
    }

    /**
     * Returns true if capable of variable weights, otherwise false.
     */
    public final boolean hasVariableWeight() {
        return Region.hasVariableWeight(renderModes);
    }

    /**
     * Returns true if render mode has a color channel,
     * i.e. the bit {@link #COLORCHANNEL_RENDERING_BIT} is set,
     * otherwise false.
     */
    public boolean hasColorChannel() {
        return Region.hasColorChannel(renderModes);
    }

    /**
     * Returns true if render mode has a color texture,
     * i.e. the bit {@link #COLORTEXTURE_RENDERING_BIT} is set,
     * otherwise false.
     */
    public boolean hasColorTexture() {
        return Region.hasColorTexture(renderModes);
    }


    /** See {@link #setFrustum(Frustum)} */
    public final Frustum getFrustum() { return frustum; }

    /**
     * Set {@link Frustum} culling for {@link #addOutlineShape(OutlineShape, AffineTransform, float[])}.
     */
    public final void setFrustum(final Frustum frustum) {
        this.frustum = frustum;
    }

    private void pushNewVertexImpl(final Vertex vertIn, final AffineTransform transform, final float[] rgba) {
        if( null != transform ) {
            final float[] coordsEx1 = new float[3];
            final float[] coordsIn = vertIn.getCoord();
            transform.transform(coordsIn, coordsEx1);
            coordsEx1[2] = coordsIn[2];
            box.resize(coordsEx1);
            pushVertex(coordsEx1, vertIn.getTexCoord(), rgba);
        } else {
            box.resize(vertIn.getCoord());
            pushVertex(vertIn.getCoord(), vertIn.getTexCoord(), rgba);
        }
        numVertices++;
    }

    private void pushNewVerticesImpl(final Vertex vertIn1, final Vertex vertIn2, final Vertex vertIn3, final AffineTransform transform, final float[] rgba) {
        if( null != transform ) {
            final float[] coordsEx1 = new float[3];
            final float[] coordsEx2 = new float[3];
            final float[] coordsEx3 = new float[3];
            final float[] coordsIn1 = vertIn1.getCoord();
            final float[] coordsIn2 = vertIn2.getCoord();
            final float[] coordsIn3 = vertIn3.getCoord();
            transform.transform(coordsIn1, coordsEx1);
            transform.transform(coordsIn2, coordsEx2);
            transform.transform(coordsIn3, coordsEx3);
            coordsEx1[2] = coordsIn1[2];
            coordsEx2[2] = coordsIn2[2];
            coordsEx3[2] = coordsIn3[2];
            box.resize(coordsEx1);
            box.resize(coordsEx2);
            box.resize(coordsEx3);
            pushVertices(coordsEx1,             coordsEx2,             coordsEx3,
                         vertIn1.getTexCoord(), vertIn2.getTexCoord(), vertIn3.getTexCoord(), rgba);
        } else {
            box.resize(vertIn1.getCoord());
            box.resize(vertIn2.getCoord());
            box.resize(vertIn3.getCoord());
            pushVertices(vertIn1.getCoord(),    vertIn2.getCoord(),    vertIn3.getCoord(),
                         vertIn1.getTexCoord(), vertIn2.getTexCoord(), vertIn3.getTexCoord(), rgba);
        }
        numVertices+=3;
    }

    @SuppressWarnings("unused")
    private void pushNewVertexIdxImpl(final Vertex vertIn, final AffineTransform transform, final float[] rgba) {
        pushIndex(numVertices);
        pushNewVertexImpl(vertIn, transform, rgba);
    }
    private void pushNewVerticesIdxImpl(final Vertex vertIn1, final Vertex vertIn2, final Vertex vertIn3, final AffineTransform transform, final float[] rgba) {
        pushIndices(numVertices, numVertices+1, numVertices+2);
        pushNewVerticesImpl(vertIn1, vertIn2, vertIn3, transform, rgba);
    }

    private final AABBox tmpBox = new AABBox();

    protected static final int GL_UINT16_MAX = 0xffff; // 65,535
    protected static final int GL_INT32_MAX = 0x7fffffff; // 2,147,483,647

    static class Perf {
        Instant t0 = null, t1 = null, t2 = null;
        // all td_ values are in [ns]
        long td_vertices = 0;
        long td_tri_push_idx = 0;
        long td_tri_push_vertidx = 0;
        long td_tri_misc = 0;
        long td_tri_total = 0; // incl tac_ns_tri_push_vertidx + tac_ns_tri_push_idx + tac_ns_tri_misc
        long td_total = 0;     // incl tac_ns_triangles + tac_ns_vertices
        long count = 0;

        public void print(final PrintStream out) {
            final long tac_ns_triangles_self = td_tri_total - td_tri_push_vertidx - td_tri_push_idx - td_tri_misc;
            final long tac_ns_total_self = td_total - td_tri_total - td_vertices;
            out.printf("Region.add(): count %,3d, total %,5d [ms], per-add %,4.2f [ns]%n", count, TimeUnit.NANOSECONDS.toMillis(td_total), ((double)td_total/count));
            out.printf("                total self %,5d [ms], per-add %,4.2f [ns]%n", TimeUnit.NANOSECONDS.toMillis(tac_ns_total_self), ((double)tac_ns_total_self/count));
            out.printf("                  vertices %,5d [ms], per-add %,4.2f [ns]%n", TimeUnit.NANOSECONDS.toMillis(td_vertices), ((double)td_vertices/count));
            out.printf("           triangles total %,5d [ms], per-add %,4.2f [ns]%n", TimeUnit.NANOSECONDS.toMillis(td_tri_total), ((double)td_tri_total/count));
            out.printf("            triangles self %,5d [ms], per-add %,4.2f [ns]%n", TimeUnit.NANOSECONDS.toMillis(tac_ns_triangles_self), ((double)tac_ns_triangles_self/count));
            out.printf("                  tri misc %,5d [ms], per-add %,4.2f [ns]%n", TimeUnit.NANOSECONDS.toMillis(td_tri_misc), ((double)td_tri_misc/count));
            out.printf("                 tri p-idx %,5d [ms], per-add %,4.2f [ns]%n", TimeUnit.NANOSECONDS.toMillis(td_tri_push_idx), ((double)td_tri_push_idx/count));
            out.printf("             tri p-vertidx %,5d [ms], per-add %,4.2f [ns]%n", TimeUnit.NANOSECONDS.toMillis(td_tri_push_vertidx), ((double)td_tri_push_vertidx/count));
        }

        public void clear() {
            t0 = null; t1 = null; t2 = null;
            td_vertices = 0;
            td_tri_push_idx = 0;
            td_tri_push_vertidx = 0;
            td_tri_misc = 0;
            td_tri_total = 0;
            td_total = 0;
            count = 0;
        }
    }
    private Perf perf = null;

    private final PerfCounterCtrl perfCounterCtrl = new PerfCounterCtrl() {
        @Override
        public void enable(final boolean enable) {
            if( enable ) {
                if( null != perf ) {
                    perf.clear();
                } else {
                    perf = new Perf();
                }
            } else {
                perf = null;
            }
        }

        @Override
        public void clear() {
            if( null != perf ) {
                perf.clear();
            }
        }

        @Override
        public Duration getTotalDuration() {
            if( null != perf ) {
                return Duration.ofNanos(perf.td_total);
            } else {
                return Duration.ZERO;
            }
        }

        @Override
        public void print(final PrintStream out) {
            if( null != perf ) {
                perf.print(out);
            }
        } };
    public PerfCounterCtrl perfCounter() { return perfCounterCtrl; }

    /**
     * Add the given {@link OutlineShape} to this region with the given optional {@link AffineTransform}.
     * <p>
     * In case {@link #setFrustum(Frustum) frustum culling is set}, the {@link OutlineShape}
     * is dropped if it's {@link OutlineShape#getBounds() bounding-box} is fully outside of the frustum.
     * The optional {@link AffineTransform} is applied to the bounding-box beforehand.
     * </p>
     * @param rgbaColor TODO
     */
    public final void addOutlineShape(final OutlineShape shape, final AffineTransform t, final float[] rgbaColor) {
        if( null != perf ) {
            ++perf.count;
            perf.t0 = Clock.getMonotonicTime();
        }
        if( null != frustum ) {
            final AABBox shapeBox = shape.getBounds();
            final AABBox shapeBoxT;
            if( null != t ) {
                t.transform(shapeBox, tmpBox);
                shapeBoxT = tmpBox;
            } else {
                shapeBoxT = shapeBox;
            }
            if( frustum.isAABBoxOutside(shapeBoxT) ) {
                if(DEBUG_INSTANCE) {
                    System.err.println("Region.addOutlineShape(): Dropping outside shapeBoxT: "+shapeBoxT);
                }
                return;
            }
        }
        final List<Triangle> trisIn = shape.getTriangles(OutlineShape.VerticesState.QUADRATIC_NURBS);
        final ArrayList<Vertex> vertsIn = shape.getVertices();
        {
            final int addedVerticeCount = shape.getAddedVerticeCount();
            final int verticeCount = vertsIn.size() + addedVerticeCount;
            final int indexCount = trisIn.size() * 3;
            if(DEBUG_INSTANCE) {
                System.err.println("Region.addOutlineShape().0: tris: "+trisIn.size()+", verts "+vertsIn.size()+", transform "+t);
                System.err.println("Region.addOutlineShape().0: VerticeCount "+vertsIn.size()+" + "+addedVerticeCount+" = "+verticeCount);
                System.err.println("Region.addOutlineShape().0: IndexCount "+indexCount);
            }
            growBufferSize(verticeCount, indexCount);
        }

        final int idxOffset = numVertices;
        int vertsVNewIdxCount = 0, vertsTMovIdxCount = 0, vertsTNewIdxCount = 0, tris = 0;
        final int vertsDupCountV = 0, vertsDupCountT = 0, vertsKnownMovedT = 0;
        if( vertsIn.size() >= 3 ) {
            if(DEBUG_INSTANCE) {
                System.err.println("Region.addOutlineShape(): Processing Vertices");
            }
            for(int i=0; i<vertsIn.size(); i++) {
                pushNewVertexImpl(vertsIn.get(i), t, rgbaColor);
                vertsVNewIdxCount++;
            }
            if( null != perf ) {
                perf.t1 = Clock.getMonotonicTime();
                perf.td_vertices += Duration.between(perf.t0, perf.t1).toNanos();
            }
            if(DEBUG_INSTANCE) {
                System.err.println("Region.addOutlineShape(): Processing Triangles");
            }
            for(final Triangle triIn : trisIn) {
                if( null != perf ) {
                    perf.t2 = Clock.getMonotonicTime();
                }
                // if(Region.DEBUG_INSTANCE) {
                //     System.err.println("T["+i+"]: "+triIn);
                // }
                // triEx.addVertexIndicesOffset(idxOffset);
                // triangles.add( triEx );
                final Vertex[] triInVertices = triIn.getVertices();
                final int tv0Idx = triInVertices[0].getId();

                if( null != perf ) {
                    perf.td_tri_misc += Duration.between(perf.t2, Clock.getMonotonicTime()).toNanos();
                }
                if ( max_indices - idxOffset > tv0Idx ) {
                    // valid 'known' idx - move by offset
                    // if(Region.DEBUG_INSTANCE) {
                    //     System.err.println("T["+i+"]: Moved "+tv0Idx+" + "+idxOffset+" -> "+(tv0Idx+idxOffset));
                    // }
                    if( null != perf ) {
                        final Instant tpi = Clock.getMonotonicTime();
                        pushIndices(tv0Idx+idxOffset,
                                    triInVertices[1].getId()+idxOffset,
                                    triInVertices[2].getId()+idxOffset);
                        perf.td_tri_push_idx += Duration.between(tpi, Clock.getMonotonicTime()).toNanos();
                    } else {
                        pushIndices(tv0Idx+idxOffset,
                                    triInVertices[1].getId()+idxOffset,
                                    triInVertices[2].getId()+idxOffset);
                    }
                    vertsTMovIdxCount+=3;
                } else {
                    // FIXME: Invalid idx - generate new one
                    // if( Region.DEBUG_INSTANCE) {
                    //    System.err.println("T["+i+"]: New Idx "+numVertices);
                    // }
                    if( null != perf ) {
                        final Instant tpvi = Clock.getMonotonicTime();
                        pushNewVerticesIdxImpl(triInVertices[0], triInVertices[1], triInVertices[2], t, rgbaColor);
                        perf.td_tri_push_vertidx += Duration.between(tpvi, Clock.getMonotonicTime()).toNanos();
                    } else {
                        pushNewVerticesIdxImpl(triInVertices[0], triInVertices[1], triInVertices[2], t, rgbaColor);
                    }
                    vertsTNewIdxCount+=3;
                }
                tris++;
            }
            if( null != perf ) {
                final Instant ttriX = Clock.getMonotonicTime();
                perf.td_tri_total += Duration.between(perf.t1, ttriX).toNanos();
                perf.td_total += Duration.between(perf.t0, ttriX).toNanos();
            }
        }
        if(DEBUG_INSTANCE) {
            System.err.println("Region.addOutlineShape().X: idx[ui32 "+usesI32Idx()+", offset "+idxOffset+"], tris: "+tris+", verts [idx "+vertsTNewIdxCount+", add "+vertsTNewIdxCount+" = "+(vertsVNewIdxCount+vertsTNewIdxCount)+"]");
            System.err.println("Region.addOutlineShape().X: verts: idx[v-new "+vertsVNewIdxCount+", t-new "+vertsTNewIdxCount+" = "+(vertsVNewIdxCount+vertsTNewIdxCount)+"]");
            System.err.println("Region.addOutlineShape().X: verts: idx t-moved "+vertsTMovIdxCount+", numVertices "+numVertices);
            System.err.println("Region.addOutlineShape().X: verts: v-dups "+vertsDupCountV+", t-dups "+vertsDupCountT+", t-known "+vertsKnownMovedT);
            // int vertsDupCountV = 0, vertsDupCountT = 0;
            System.err.println("Region.addOutlineShape().X: box "+box);
            printBufferStats(System.err);
        }
        markShapeDirty();
    }

    public final void addOutlineShapes(final List<OutlineShape> shapes, final AffineTransform transform, final float[] rgbaColor) {
        for (int i = 0; i < shapes.size(); i++) {
            addOutlineShape(shapes.get(i), transform, rgbaColor);
        }
    }

    /** @return the AxisAligned bounding box of current region */
    public final AABBox getBounds() {
        return box;
    }

    /**
     * Mark this region's shape dirty, i.e. it's
     * Vertices, Triangles, and or Lines changed.
     */
    public final void markShapeDirty() {
        dirty |= DIRTY_SHAPE;
    }
    /** Returns true if this region's shape are dirty, see {@link #markShapeDirty()}. */
    public final boolean isShapeDirty() {
        return 0 != ( dirty & DIRTY_SHAPE ) ;
    }
    /**
     * Mark this region's state dirty, i.e.
     * it's render attributes or parameters changed.
     */
    public final void markStateDirty() {
        dirty |= DIRTY_STATE;
    }
    /** Returns true if this region's state is dirty, see {@link #markStateDirty()}. */
    public final boolean isStateDirty() {
        return 0 != ( dirty & DIRTY_STATE ) ;
    }

    /**
     * See {@link #markShapeDirty()} and {@link #markStateDirty()}.
     */
    protected final void clearDirtyBits(final int v) {
        dirty &= ~v;
    }
    protected final int getDirtyBits() { return dirty; }

    @Override
    public String toString() {
        return "Region["+getRenderModeString(this.renderModes)+", q "+quality+", dirty "+dirty+", vertices "+numVertices+", box "+box+"]";
    }
}