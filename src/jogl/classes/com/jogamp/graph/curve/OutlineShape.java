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
package com.jogamp.graph.curve;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.jogamp.graph.curve.tess.Triangulation;
import com.jogamp.graph.curve.tess.Triangulator;
import com.jogamp.graph.geom.Outline;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Vec3f;
import com.jogamp.math.VectorUtil;
import com.jogamp.math.Vert2fImmutable;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;
import com.jogamp.math.geom.plane.Path2F;
import com.jogamp.math.geom.plane.Winding;

import jogamp.opengl.Debug;

/**
 * A Generic shape objects which is defined by a list of Outlines.
 * This Shape can be transformed to triangulations.
 * The list of triangles generated are render-able by a Region object.
 * The triangulation produced by this Shape will define the
 * closed region defined by the outlines.
 * <p>
 * One or more OutlineShape Object can be associated to a region
 * this is left as a high-level representation of the Objects. For
 * optimizations, flexibility requirements for future features.
 * </p>
 * <p>
 * <a name="windingrules">
 * Outline shape general {@link Winding} rules
 * <ul>
 *   <li>Outer boundary-shapes are required as {@link Winding#CCW}</li>
 *   <li>Inner hole-shapes should be {@link Winding#CW}</li>
 *   <li>If unsure
 *   <ul>
 *     <li>You may check {@link Winding} via {@link #getWindingOfLastOutline()} or {@link Outline#getWinding()} (optional, might be incorrect)</li>
 *     <li>Use {@link #setWindingOfLastOutline(Winding)} before {@link #closeLastOutline(boolean)} or {@link #closePath()} } to enforce {@link Winding#CCW}, or</li>
 *     <li>use {@link Outline#setWinding(Winding)} on a specific {@link Outline} to enforce {@link Winding#CCW}.</li>
 *     <li>If e.g. the {@link Winding} has changed for an {@link Outline} by above operations, its vertices have been reversed.</li>
 *   </ul></li>
 *   <li>Safe path: Simply create all outer boundary-shapes with {@link Winding#CCW} and inner hole-shapes with {@link Winding#CW}.</li>
 * </ul>
 * </p>
 * Example to creating an Outline Shape:
 * <pre>
      addVertex(...)
      addVertex(...)
      addVertex(...)
      addEmptyOutline()
      addVertex(...)
      addVertex(...)
      addVertex(...)
 * </pre>
 *
 * <p>
 * The above will create two outlines each with three vertices. By adding these two outlines to
 * the OutlineShape, we are stating that the combination of the two outlines represent the shape.
 * </p>
 * <p>
 * To specify that the shape is curved at a region, the on-curve flag should be set to false
 * for the vertex that is in the middle of the curved region (if the curved region is defined by 3
 * vertices (quadratic curve).
 * </p>
 * <p>
 * In case the curved region is defined by 4 or more vertices the middle vertices should both have
 * the on-curve flag set to false.
 * </p>
 * Example:
 * <pre>
      addVertex(0,0, true);
      addVertex(0,1, false);
      addVertex(1,1, false);
      addVertex(1,0, true);
 * </pre>
 * <p>
 * The above snippet defines a cubic nurbs curve where (0,1 and 1,1)
 * do not belong to the final rendered shape.
 * </p>
 *
 * <i>Implementation Notes:</i><br>
 * <ul>
 *    <li> The first vertex of any outline belonging to the shape should be on-curve</li>
 *    <li> Intersections between off-curved parts of the outline is not handled</li>
 * </ul>
 *
 * @see Outline
 * @see Region
 */
public final class OutlineShape implements Comparable<OutlineShape> {
    private static final boolean FORCE_COMPLEXSHAPE = Debug.debug("graph.curve.triangulation.force.complexshape");
    private static final boolean FORCE_SIMPLESHAPE = Debug.debug("graph.curve.triangulation.force.simpleshape");

    /**
     * Outline's vertices have undefined state until transformed.
     */
    public enum VerticesState {
        UNDEFINED(0), QUADRATIC_NURBS(1);

        public final int state;

        VerticesState(final int state){
            this.state = state;
        }
    }

    /** Initial {@link #getSharpness()} value, which can be modified via {@link #setSharpness(float)}. */
    public static final float DEFAULT_SHARPNESS = 0.5f;

    private static final int DIRTY_BOUNDS = 1 << 0;
    /**
     * Modified shape, requires to update the vertices and triangles, here: vertices.
     */
    private static final int DIRTY_VERTICES  = 1 << 1;
    /**
     * Modified shape, requires to update the vertices and triangles, here: triangulation.
     */
    private static final int DIRTY_TRIANGLES  = 1 << 2;
    /**
     * Modified shape, requires to update the convex determination
     */
    private static final int DIRTY_CONVEX  = 1 << 3;
    private static final int OVERRIDE_CONVEX  = 1 << 4;

    /** The list of {@link Outline}s that are part of this
     *  outline shape.
     */
    /* pp */ final ArrayList<Outline> outlines;

    private final AABBox bbox;
    private final ArrayList<Triangle> triangles;
    private final ArrayList<Vertex> vertices;
    private int addedVerticeCount;
    private boolean complexShape;

    private VerticesState outlineState;

    /** dirty bits DIRTY_BOUNDS */
    private int dirtyBits;

    private float sharpness;

    private final Vec3f tmpV1 = new Vec3f();
    private final Vec3f tmpV2 = new Vec3f();
    private final Vec3f tmpV3 = new Vec3f();
    // COLOR
    // private final Vec4f tmpC1 = new Vec4f();
    // private final Vec4f tmpC2 = new Vec4f();
    // private final Vec4f tmpC3 = new Vec4f();

    /**
     * Create a new Outline based Shape
     */
    public OutlineShape() {
        this.outlines = new ArrayList<Outline>(3);
        this.outlines.add(new Outline());
        this.outlineState = VerticesState.UNDEFINED;
        this.bbox = new AABBox();
        this.triangles = new ArrayList<Triangle>();
        this.vertices = new ArrayList<Vertex>();
        this.addedVerticeCount = 0;
        if( FORCE_COMPLEXSHAPE ) {
            complexShape = true;
        } else {
            complexShape = false;
        }
        this.dirtyBits = 0;
        this.sharpness = DEFAULT_SHARPNESS;
    }

    /**
     * Return the number of newly added vertices during {@link #getTriangles(VerticesState)}
     * while transforming the outlines to {@link VerticesState#QUADRATIC_NURBS} and triangulation.
     * @see #setIsQuadraticNurbs()
     */
    public final int getAddedVerticeCount() {
        return addedVerticeCount;
    }

    /** Sharpness value, defaults to {@link #DEFAULT_SHARPNESS}. */
    public final float getSharpness() { return sharpness; }

    /** Sets sharpness, defaults to {@link #DEFAULT_SHARPNESS}. */
    public final void setSharpness(final float s) {
        if( this.sharpness != s ) {
            clearCache();
            sharpness=s;
        }
    }

    /** Clears all data and reset all states as if this instance was newly created */
    public final void clear() {
        outlines.clear();
        outlines.add(new Outline());
        outlineState = VerticesState.UNDEFINED;
        bbox.reset();
        vertices.clear();
        triangles.clear();
        addedVerticeCount = 0;
        if( FORCE_COMPLEXSHAPE ) {
            complexShape = true;
        } else {
            complexShape = false;
        }
        dirtyBits = 0;
    }

    /** Clears cached triangulated data, i.e. {@link #getTriangles(VerticesState)} and {@link #getVertices()}.  */
    public final void clearCache() {
        vertices.clear();
        triangles.clear();
        dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES | DIRTY_CONVEX;
    }

    /** Returns the number of {@link Outline}s. */
    public final int getOutlineCount() {
        return outlines.size();
    }

    /** Returns the total {@link Outline#getVertexCount() vertex number} of all {@link Outline}s. */
    public final int getVertexCount() {
        int res = 0;
        for(final Outline o : outlines) {
            res += o.getVertexCount();
        }
        return res;
    }

    /**
     * Compute the {@link Winding} of the {@link #getLastOutline()} using the {@link VectorUtil#area(ArrayList)} function over all of its vertices.
     * @return {@link Winding#CCW} or {@link Winding#CW}
     */
    public final Winding getWindingOfLastOutline() {
        return getLastOutline().getWinding();
    }

    /**
     * Sets the enforced {@link Winding} of the {@link #getLastOutline()}.
     */
    public final void setWindingOfLastOutline(final Winding enforced) {
        getLastOutline().setWinding(enforced);
    }

    /**
     * Returns cached or computed result if at least one {@code polyline} of {@link #getOutline(int)} is a complex shape, see {@link Outline#isComplex()}.
     * <p>
     * A polyline with less than 3 elements is marked a simple shape for simplicity.
     * </p>
     * <p>
     * The result is cached.
     * </p>
     * @see #setOverrideConvex(boolean)
     * @see #clearOverrideConvex()
     */
    public boolean isComplex() {
        if( !FORCE_COMPLEXSHAPE && !FORCE_SIMPLESHAPE &&
            0 == ( OVERRIDE_CONVEX & dirtyBits ) &&
            0 != ( DIRTY_CONVEX & dirtyBits ) )
        {
            complexShape = false;
            final int sz = this.getOutlineCount();
            for(int i=0; i<sz && !complexShape; ++i) {
                complexShape = getOutline(i).isComplex();
            }
            dirtyBits &= ~DIRTY_CONVEX;
        }
        return complexShape;
    }
    /**
     * Overrides {@link #isComplex()} using the given value instead of computing via {@link Outline#isComplex()}.
     * @see #clearOverrideConvex()
     * @see #isComplex()
     */
    public void setOverrideConvex(final boolean convex) {
        if( !FORCE_COMPLEXSHAPE && !FORCE_SIMPLESHAPE ) {
            dirtyBits |= OVERRIDE_CONVEX;
            complexShape = convex;
        }
    }
    /**
     * Clears the {@link #isComplex()} override done by {@link #setOverrideConvex(boolean)}
     * @see #setOverrideConvex(boolean)
     * @see #isComplex()
     */
    public void clearOverrideConvex() {
        dirtyBits &= ~OVERRIDE_CONVEX;
        dirtyBits |= DIRTY_CONVEX;
    }

    /**
     * Add a new empty {@link Outline}
     * to the end of this shape's outline list.
     * <p>If the {@link #getLastOutline()} is empty already, no new one will be added.</p>
     *
     * After a call to this function all new vertices added
     * will belong to the new outline
     */
    public final void addEmptyOutline() {
        if( !getLastOutline().isEmpty() ) {
            outlines.add(new Outline());
        }
    }

    /**
     * Appends the {@link Outline} element to the end,
     * ensuring a clean tail.
     *
     * <p>A clean tail is ensured, no double empty Outlines are produced
     * and a pre-existing empty outline will be replaced with the given one. </p>
     *
     * @param outline Outline object to be added
     * @throws NullPointerException if the  {@link Outline} element is null
     */
    public final void addOutline(final Outline outline) throws NullPointerException {
        addOutline(outlines.size(), outline);
    }

    /**
     * Insert the {@link Outline} element at the given {@code position}.
     *
     * <p>If the {@code position} indicates the end of this list,
     * a clean tail is ensured, no double empty Outlines are produced
     * and a pre-existing empty outline will be replaced with the given one. </p>
     *
     * @param position of the added Outline
     * @param outline Outline object to be added
     * @throws NullPointerException if the  {@link Outline} element is null
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position > getOutlineNumber())
     */
    public final void addOutline(final int position, final Outline outline) throws NullPointerException, IndexOutOfBoundsException {
        if (null == outline) {
            throw new NullPointerException("outline is null");
        }
        if( outlines.size() == position ) {
            final Outline lastOutline = getLastOutline();
            if( outline.isEmpty() && lastOutline.isEmpty() ) {
                return;
            }
            if( lastOutline.isEmpty() ) {
                outlines.set(position-1, outline);
                if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
                    bbox.resize(outline.getBounds());
                }
                // vertices.addAll(outline.getVertices()); // FIXME: can do and remove DIRTY_VERTICES ?
                dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES | DIRTY_CONVEX;
                return;
            }
        }
        outlines.add(position, outline);
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            bbox.resize(outline.getBounds());
        }
        dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES | DIRTY_CONVEX;
    }

    /**
     * Insert the {@link OutlineShape} elements of type {@link Outline}, .. at the end of this shape,
     * using {@link #addOutline(Outline)} for each element.
     * <p>Closes the current last outline via {@link #closeLastOutline(boolean)} before adding the new ones.</p>
     * @param outlineShape OutlineShape elements to be added.
     * @throws NullPointerException if the  {@link OutlineShape} is null
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position > getOutlineNumber())
     */
    public final void addOutlineShape(final OutlineShape outlineShape) throws NullPointerException {
        if (null == outlineShape) {
            throw new NullPointerException("OutlineShape is null");
        }
        closeLastOutline(true);
        for(int i=0; i<outlineShape.getOutlineCount(); i++) {
            addOutline(outlineShape.getOutline(i));
        }
    }

    /**
     * Replaces the {@link Outline} element at the given {@code position}.
     * <p>Sets the bounding box dirty, hence a next call to {@link #getBounds()} will validate it.</p>
     *
     * @param position of the replaced Outline
     * @param outline replacement Outline object
     * @throws NullPointerException if the  {@link Outline} element is null
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getOutlineNumber())
     */
    public final void setOutline(final int position, final Outline outline) throws NullPointerException, IndexOutOfBoundsException {
        if (null == outline) {
            throw new NullPointerException("outline is null");
        }
        outlines.set(position, outline);
        dirtyBits |= DIRTY_BOUNDS | DIRTY_TRIANGLES | DIRTY_VERTICES | DIRTY_CONVEX;
    }

    /**
     * Removes the {@link Outline} element at the given {@code position}.
     * <p>Sets the bounding box dirty, hence a next call to {@link #getBounds()} will validate it.</p>
     *
     * @param position of the to be removed Outline
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getOutlineNumber())
     */
    public final Outline removeOutline(final int position) throws IndexOutOfBoundsException {
        dirtyBits |= DIRTY_BOUNDS | DIRTY_TRIANGLES | DIRTY_VERTICES | DIRTY_CONVEX;
        return outlines.remove(position);
    }

    /**
     * Get the last added outline to the list
     * of outlines that define the shape
     * @return the last outline
     */
    public final Outline getLastOutline() {
        return outlines.get(outlines.size()-1);
    }

    /**
     * Returns the {@code Outline} at {@code position}
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getOutlineNumber())
     */
    public final Outline getOutline(final int position) throws IndexOutOfBoundsException {
        return outlines.get(position);
    }

    /**
     * Adds a vertex to the last open outline to the shape's tail.
     *
     * @param v the vertex to be added to the OutlineShape
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void addVertex(final Vertex v) {
        final Outline lo = getLastOutline();
        lo.addVertex(v);
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            bbox.resize(v.getCoord());
        }
        // vertices.add(v); // FIXME: can do and remove DIRTY_VERTICES ?
        dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES | DIRTY_CONVEX;
    }

    /**
     * Adds a vertex to the last open outline to the shape at {@code position}
     *
     * @param position index within the last open outline, at which the vertex will be added
     * @param v the vertex to be added to the OutlineShape
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void addVertex(final int position, final Vertex v) {
        final Outline lo = getLastOutline();
        lo.addVertex(position, v);
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            bbox.resize(v.getCoord());
        }
        dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES | DIRTY_CONVEX;
    }

    /**
     * Add a 2D {@link Vertex} to the last open outline to the shape's tail.
     * The 2D vertex will be represented as Z=0.
     *
     * @param x the x coordinate
     * @param y the y coordniate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region of the shape around this vertex.
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void addVertex(final float x, final float y, final boolean onCurve) {
        addVertex(new Vertex(x, y, 0f, onCurve));
    }

    /**
     * Add a 2D {@link Vertex} to the last open outline to the shape at {@code position}.
     * The 2D vertex will be represented as Z=0.
     *
     * @param position index within the last open outline, at which the vertex will be added
     * @param x the x coordinate
     * @param y the y coordniate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region of the shape around this vertex.
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void addVertex(final int position, final float x, final float y, final boolean onCurve) {
        addVertex(position, new Vertex(x, y, 0f, onCurve));
    }

    /**
     * Add a 3D {@link Vertex} to the last open outline to the shape's tail.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region of the shape around this vertex.
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void addVertex(final float x, final float y, final float z, final boolean onCurve) {
        addVertex(new Vertex(x, y, z, onCurve));
    }

    /**
     * Add a 3D {@link Vertex} to the last open outline to the shape at {@code position}.
     *
     * @param position index within the last open outline, at which the vertex will be added
     * @param x the x coordinate
     * @param y the y coordniate
     * @param z the z coordinate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region of the shape around this vertex.
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void addVertex(final int position, final float x, final float y, final float z, final boolean onCurve) {
        addVertex(position, new Vertex(x, y, z, onCurve));
    }

    /**
     * Add a vertex to the last open outline to the shape's tail.
     *
     * The vertex is passed as a float array and its offset where its attributes are located.
     * The attributes should be continuous (stride = 0).
     * Attributes which value are not set (when length less than 3)
     * are set implicitly to zero.
     * @param coordsBuffer the coordinate array where the vertex attributes are to be picked from
     * @param offset the offset in the buffer to the x coordinate
     * @param length the number of attributes to pick from the buffer (maximum 3)
     * @param onCurve flag if this vertex is on the final curve or defines a curved region of the shape around this vertex.
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void addVertex(final float[] coordsBuffer, final int offset, final int length, final boolean onCurve) {
        addVertex(new Vertex(coordsBuffer, offset, length, onCurve));
    }

    /**
     * Add a vertex to the last open outline to the shape at {@code position}.
     *
     * The vertex is passed as a float array and its offset where its attributes are located.
     * The attributes should be continuous (stride = 0).
     * Attributes which value are not set (when length less than 3)
     * are set implicitly to zero.
     * @param position index within the last open outline, at which the vertex will be added
     * @param coordsBuffer the coordinate array where the vertex attributes are to be picked from
     * @param offset the offset in the buffer to the x coordinate
     * @param length the number of attributes to pick from the buffer (maximum 3)
     * @param onCurve flag if this vertex is on the final curve or defines a curved region of the shape around this vertex.
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void addVertex(final int position, final float[] coordsBuffer, final int offset, final int length, final boolean onCurve) {
        addVertex(position, new Vertex(coordsBuffer, offset, length, onCurve));
    }

    /**
     * Closes the last outline in the shape.
     * <p>
     * Checks whether the last vertex equals to the first of the last outline.
     * If not equal, it either appends a copy of the first vertex
     * or prepends a copy of the last vertex, depending on <code>closeTail</code>.
     * </p>
     * @param closeTail if true, a copy of the first vertex will be appended,
     *                  otherwise a copy of the last vertex will be prepended.
     */
    public final void closeLastOutline(final boolean closeTail) {
        if( getLastOutline().setClosed( closeTail ) ) {
            dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES | DIRTY_CONVEX;
        }
    }

    /**
     * Append the given path geometry to this outline shape.
     *
     * The given path geometry should be {@link Winding#CCW}.
     *
     * If the given path geometry is {@link Winding#CW}, use {@link #addPathRev(Path2F, boolean)}.
     *
     * @param path the {@link Path2F} to append to this outline shape, should be {@link Winding#CCW}.
     * @param connect pass true to turn an initial moveTo segment into a lineTo segment to connect the new geometry to the existing path, otherwise pass false.
     * @see Path2F#getWinding()
     */
    public void addPath(final Path2F path, final boolean connect) {
        addPath(path.iterator(null), connect);
    }

    /**
     * Add the given {@link Path2F.Iterator} to this outline shape.
     *
     * The given path geometry should be {@link Winding#CCW}.
     *
     * If the given path geometry is {@link Winding#CW}, use {@link #addPathRev(Path2F.Iterator, boolean).
     *
     * @param pathI the {@link Path2F.Iterator} to append to this outline shape, should be {@link Winding#CCW}.
     * @param connect pass true to turn an initial moveTo segment into a lineTo segment to connect the new geometry to the existing path, otherwise pass false.
     * @see Path2F.Iterator#getWinding()
     */
    public final void addPath(final Path2F.Iterator pathI, boolean connect) {
        final float[] points = pathI.points();
        while ( pathI.hasNext() ) {
            final int idx = pathI.index();
            final Path2F.SegmentType type = pathI.next();
            switch(type) {
                case MOVETO:
                    final Outline lo = this.getLastOutline();
                    final int lo_sz = lo.getVertexCount();
                    if ( 0 == lo_sz ) {
                        addVertex(points, idx,   2, true);
                        break;
                    } else if ( !connect ) {
                        closeLastOutline(false);
                        addEmptyOutline();
                        addVertex(points, idx,   2, true);
                        break;
                    }
                    {
                        // Skip if last vertex in last outline matching this point -> already connected.
                        final Vert2fImmutable llc = lo.getVertex(lo_sz-1);
                        if( llc.x() == points[idx+0] &&
                            llc.y() == points[idx+1] ) {
                            break;
                        }
                    }
                    // fallthrough: MOVETO -> LINETO
                case LINETO:
                    addVertex(points, idx,   2, true);
                    break;
                case QUADTO:
                    addVertex(points, idx,   2, false);
                    addVertex(points, idx+2, 2, true);
                    break;
                case CUBICTO:
                    addVertex(points, idx,   2, false);
                    addVertex(points, idx+2, 2, false);
                    addVertex(points, idx+4, 2, true);
                    break;
                case CLOSE:
                    closeLastOutline(true);
                    addEmptyOutline();
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled Segment Type: "+type);
            }
            connect = false;
        }
    }

    /**
     * Append the given path geometry to this outline shape in reverse order.
     *
     * The given path geometry should be {@link Winding#CW}.
     *
     * If the given path geometry is {@link Winding#CCW}, use {@link #addPath(Path2F, boolean)}.
     *
     * @param path the {@link Path2F} to append to this outline shape, should be {@link Winding#CW}.
     * @param connect pass true to turn an initial moveTo segment into a lineTo segment to connect the new geometry to the existing path, otherwise pass false.
     */
    public void addPathRev(final Path2F path, final boolean connect) {
        addPathRev(path.iterator(null), connect);
    }

    /**
     * Add the given {@link Path2F.Iterator} to this outline shape in reverse order.
     *
     * The given path geometry should be {@link Winding#CW}.
     *
     * If the given path geometry is {@link Winding#CCW}, use {@link #addPath(Path2F.Iterator, boolean).
     *
     * @param pathI the {@link Path2F.Iterator} to append to this outline shape, should be {@link Winding#CW}.
     * @param connect pass true to turn an initial moveTo segment into a lineTo segment to connect the new geometry to the existing path, otherwise pass false.
     */
    public final void addPathRev(final Path2F.Iterator pathI, boolean connect) {
        final float[] points = pathI.points();
        while ( pathI.hasNext() ) {
            final int idx = pathI.index();
            final Path2F.SegmentType type = pathI.next();
            switch(type) {
                case MOVETO:
                    final Outline lo = this.getLastOutline();
                    final int lo_sz = lo.getVertexCount();
                    if ( 0 == lo_sz ) {
                        addVertex(0, points, idx,   2, true);
                        break;
                    } else if ( !connect ) {
                        closeLastOutline(false);
                        addEmptyOutline();
                        addVertex(0, points, idx,   2, true);
                        break;
                    }
                    {
                        // Skip if last vertex in last outline matching this point -> already connected.
                        final Vert2fImmutable llc = lo.getVertex(0);
                        if( llc.x() == points[idx+0] &&
                            llc.y() == points[idx+1] ) {
                            break;
                        }
                    }
                    // fallthrough: MOVETO -> LINETO
                case LINETO:
                    addVertex(0, points, idx,   2, true);
                    break;
                case QUADTO:
                    addVertex(0, points, idx,   2, false);
                    addVertex(0, points, idx+2, 2, true);
                    break;
                case CUBICTO:
                    addVertex(0, points, idx,   2, false);
                    addVertex(0, points, idx+2, 2, false);
                    addVertex(0, points, idx+4, 2, true);
                    break;
                case CLOSE:
                    closeLastOutline(true);
                    addEmptyOutline();
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled Segment Type: "+type);
            }
            connect = false;
        }
    }

    /**
     * Start a new position for the next line segment at given point x/y (P1).
     *
     * @param x point (P1)
     * @param y point (P1)
     * @param z point (P1)
     * @see Path2F#moveTo(float, float)
     * @see #addPath(com.jogamp.math.geom.plane.Path2F.Iterator, boolean)
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void moveTo(final float x, final float y, final float z) {
        if ( 0 == getLastOutline().getVertexCount() ) {
            addVertex(x, y, z, true);
        } else {
            closeLastOutline(false);
            addEmptyOutline();
            addVertex(x, y, z, true);
        }
    }

    /**
     * Add a line segment, intersecting the last point and the given point x/y (P1).
     *
     * @param x final point (P1)
     * @param y final point (P1)
     * @param z final point (P1)
     * @see Path2F#lineTo(float, float)
     * @see #addPath(com.jogamp.math.geom.plane.Path2F.Iterator, boolean)
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void lineTo(final float x, final float y, final float z) {
        addVertex(x, y, z, true);
    }

    /**
     * Add a quadratic curve segment, intersecting the last point and the second given point x2/y2 (P2).
     *
     * @param x1 quadratic parametric control point (P1)
     * @param y1 quadratic parametric control point (P1)
     * @param z1 quadratic parametric control point (P1)
     * @param x2 final interpolated control point (P2)
     * @param y2 final interpolated control point (P2)
     * @param z2 quadratic parametric control point (P2)
     * @see Path2F#quadTo(float, float, float, float)
     * @see #addPath(com.jogamp.math.geom.plane.Path2F.Iterator, boolean)
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void quadTo(final float x1, final float y1, final float z1, final float x2, final float y2, final float z2) {
        addVertex(x1, y1, z1, false);
        addVertex(x2, y2, z2, true);
    }

    /**
     * Add a cubic Bézier curve segment, intersecting the last point and the second given point x3/y3 (P3).
     *
     * @param x1 Bézier control point (P1)
     * @param y1 Bézier control point (P1)
     * @param z1 Bézier control point (P1)
     * @param x2 Bézier control point (P2)
     * @param y2 Bézier control point (P2)
     * @param z2 Bézier control point (P2)
     * @param x3 final interpolated control point (P3)
     * @param y3 final interpolated control point (P3)
     * @param z3 final interpolated control point (P3)
     * @see Path2F#cubicTo(float, float, float, float, float, float)
     * @see #addPath(com.jogamp.math.geom.plane.Path2F.Iterator, boolean)
     * @see <a href="#windingrules">see winding rules</a>
     */
    public final void cubicTo(final float x1, final float y1, final float z1, final float x2, final float y2, final float z2, final float x3, final float y3, final float z3) {
        addVertex(x1, y1, z1, false);
        addVertex(x2, y2, z2, false);
        addVertex(x3, y3, z3, true);
    }

    /**
     * Closes the current sub-path segment by drawing a straight line back to the coordinates of the last moveTo. If the path is already closed then this method has no effect.
     * @see Path2F#closePath()
     * @see #addPath(com.jogamp.math.geom.plane.Path2F.Iterator, boolean)
     */
    public final void closePath() {
        if ( 0 < getLastOutline().getVertexCount() ) {
            closeLastOutline(true);
            addEmptyOutline();
        }
    }

    /**
     * Return the outline's vertices state, {@link OutlineShape.VerticesState}
     */
    public final VerticesState getOutlineState() {
        return outlineState;
    }

    /**
     * Claim this outline's vertices are all {@link OutlineShape.VerticesState#QUADRATIC_NURBS},
     * hence no cubic transformations will be performed.
     */
    public final void setIsQuadraticNurbs() {
        outlineState = VerticesState.QUADRATIC_NURBS;
        // checkPossibleOverlaps = false;
    }

    private void subdivideTriangle(final Outline outline, final Vertex a, final Vertex b, final Vertex c, final int index){
        VectorUtil.midpoint(tmpV1, a.getCoord(), b.getCoord());
        VectorUtil.midpoint(tmpV3, b.getCoord(), c.getCoord());
        VectorUtil.midpoint(tmpV2, tmpV1, tmpV3);

        // COLOR
        // tmpC1.set(a.getColor()).add(b.getColor()).scale(0.5f);
        // tmpC3.set(b.getColor()).add(b.getColor()).scale(0.5f);
        // tmpC2.set(tmpC1).add(tmpC1).scale(0.5f);

        //drop off-curve vertex to image on the curve
        b.setCoord(tmpV2);
        b.setOnCurve(true);

        outline.addVertex(index, new Vertex(tmpV1, false));
        outline.addVertex(index+2, new Vertex(tmpV3, false));

        addedVerticeCount += 2;
    }

    /**
     * Check overlaps between curved triangles
     * first check if any vertex in triangle a is in triangle b
     * second check if edges of triangle a intersect segments of triangle b
     * if any of the two tests is true we divide current triangle
     * and add the other to the list of overlaps
     *
     * Loop until overlap array is empty. (check only in first pass)
     */
    private void checkOverlaps() {
        final ArrayList<Vertex> overlaps = new ArrayList<Vertex>(3);
        final int count = getOutlineCount();
        boolean firstpass = true;
        do {
            for (int cc = 0; cc < count; cc++) {
                final Outline outline = getOutline(cc);
                int vertexCount = outline.getVertexCount();
                for(int i=0; i < outline.getVertexCount(); i++) {
                    final Vertex currentVertex = outline.getVertex(i);
                    if ( !currentVertex.isOnCurve()) {
                        final Vertex nextV = outline.getVertex((i+1)%vertexCount);
                        final Vertex prevV = outline.getVertex((i+vertexCount-1)%vertexCount);
                        final Vertex overlap;

                        // check for overlap even if already set for subdivision
                        // ensuring both triangular overlaps get divided
                        // for pref. only check in first pass
                        // second pass to clear the overlaps array(reduces precision errors)
                        if( firstpass ) {
                            overlap = checkTriOverlaps0(prevV, currentVertex, nextV);
                        } else {
                            overlap = null;
                        }
                        if( null != overlap || overlaps.contains(currentVertex) ) {
                            overlaps.remove(currentVertex);

                            subdivideTriangle(outline, prevV, currentVertex, nextV, i);
                            i+=3;
                            vertexCount+=2;
                            addedVerticeCount+=2;

                            if(overlap != null && !overlap.isOnCurve()) {
                                if(!overlaps.contains(overlap)) {
                                    overlaps.add(overlap);
                                }
                            }
                        }
                    }
                }
            }
            firstpass = false;
        } while( !overlaps.isEmpty() );
    }

    private Vertex checkTriOverlaps0(final Vertex a, final Vertex b, final Vertex c) {
        final int count = getOutlineCount();
        for (int cc = 0; cc < count; cc++) {
            final Outline outline = getOutline(cc);
            final int vertexCount = outline.getVertexCount();
            for(int i=0; i < vertexCount; i++) {
                final Vertex current = outline.getVertex(i);
                if(current.isOnCurve() || current == a || current == b || current == c) {
                    continue;
                }
                final Vertex nextV = outline.getVertex((i+1)%vertexCount);
                final Vertex prevV = outline.getVertex((i+vertexCount-1)%vertexCount);

                //skip neighboring triangles
                if(prevV == c || nextV == a) {
                    continue;
                }

                if( VectorUtil.isInTriangle3(a.getCoord(), b.getCoord(), c.getCoord(),
                                                 current.getCoord(), nextV.getCoord(), prevV.getCoord(),
                                                 tmpV1, tmpV2, tmpV3) ) {
                    return current;
                }
                if(VectorUtil.testTri2SegIntersection(a, b, c, prevV, current) ||
                   VectorUtil.testTri2SegIntersection(a, b, c, current, nextV) ||
                   VectorUtil.testTri2SegIntersection(a, b, c, prevV, nextV) ) {
                    return current;
                }
            }
        }
        return null;
    }

    private void cleanupOutlines() {
        final boolean transformOutlines2Quadratic = VerticesState.QUADRATIC_NURBS != outlineState;
        int count = getOutlineCount();
        for (int cc = 0; cc < count; cc++) {
            final Outline outline = getOutline(cc);
            int vertexCount = outline.getVertexCount();

            if( transformOutlines2Quadratic ) {
                for(int i=0; i < vertexCount; i++) {
                    final Vertex currentVertex = outline.getVertex(i);
                    final int j = (i+1)%vertexCount;
                    final Vertex nextVertex = outline.getVertex(j);
                    if ( !currentVertex.isOnCurve() && !nextVertex.isOnCurve() ) {
                        VectorUtil.midpoint(tmpV1, currentVertex.getCoord(), nextVertex.getCoord());
                        System.err.println("XXX: Cubic: "+i+": "+currentVertex+", "+j+": "+nextVertex);
                        final Vertex v = new Vertex(tmpV1, true);
                        // COLOR: tmpC1.set(currentVertex.getColor()).add(nextVertex.getColor()).scale(0.5f)
                        i++;
                        vertexCount++;
                        addedVerticeCount++;
                        outline.addVertex(i, v);
                    }
                }
            }
            if( 0 >= vertexCount ) {
                outlines.remove(outline);
                cc--;
                count--;
            } else if( 0 < vertexCount &&
                       outline.getVertex(0).getCoord().isEqual( outline.getLastVertex().getCoord() ) ) {
                outline.removeVertex(vertexCount-1);
            }
        }
        outlineState = VerticesState.QUADRATIC_NURBS;
        checkOverlaps();
    }

    private int generateVertexIds() {
        int maxVertexId = 0;
        for(int i=0; i<outlines.size(); i++) {
            final ArrayList<Vertex> vertices = outlines.get(i).getVertices();
            for(int pos=0; pos<vertices.size(); pos++) {
                vertices.get(pos).setId(maxVertexId++);
            }
        }
        return maxVertexId;
    }

    /**
     * Return list of concatenated vertices associated with all
     * {@code Outline}s of this object.
     * <p>
     * Vertices are cached until marked dirty.
     * </p>
     * <p>
     * Should always be called <i>after</i> {@link #getTriangles(VerticesState)},
     * since the latter will mark all cached vertices dirty!
     * </p>
     */
    public final ArrayList<Vertex> getVertices() {
        // final boolean updated;
        if( 0 != ( DIRTY_VERTICES & dirtyBits ) ) {
            vertices.clear();
            for(int i=0; i<outlines.size(); i++) {
                vertices.addAll(outlines.get(i).getVertices());
            }
            dirtyBits &= ~DIRTY_VERTICES;
            // updated = true;
        // } else {
        //    updated = false;
        }
        // if(Region.DEBUG_INSTANCE) {
        //    System.err.println("OutlineShape.getVertices(): o "+outlines.size()+", v "+vertices.size()+", updated "+updated);
        // }
        return vertices;
    }

    public static void printPerf(final PrintStream out) {
        // jogamp.graph.curve.tess.Loop.printPerf(out);
    }
    private void triangulateImpl() {
        if( 0 < outlines.size() ) {
            sortOutlines();
            generateVertexIds();

            triangles.clear();
            final Triangulator triangulator2d = Triangulation.create();
            triangulator2d.setComplexShape( isComplex() );
            for(int index = 0; index<outlines.size(); index++) {
                triangulator2d.addCurve(triangles, outlines.get(index), sharpness);
            }
            triangulator2d.generate(triangles);
            addedVerticeCount += triangulator2d.getAddedVerticeCount();
            triangulator2d.reset();
        }
    }

    /**
     * Triangulate the {@link OutlineShape} generating a list of triangles,
     * while {@link #transformOutlines(VerticesState)} beforehand.
     * <p>
     * Triangles are cached until marked dirty.
     * </p>
     * @return an arraylist of triangles representing the filled region
     * which is produced by the combination of the outlines
     */
    public final ArrayList<Triangle> getTriangles(final VerticesState destinationType) {
        final boolean updated;
        if(destinationType != VerticesState.QUADRATIC_NURBS) {
            throw new IllegalStateException("destinationType "+destinationType.name()+" not supported (currently "+outlineState.name()+")");
        }
        if( 0 != ( DIRTY_TRIANGLES & dirtyBits ) ) {
            cleanupOutlines();
            triangulateImpl();
            updated = true;
            dirtyBits |= DIRTY_VERTICES;
            dirtyBits &= ~DIRTY_TRIANGLES;
        } else {
            updated = false;
        }
        if(Region.DEBUG_INSTANCE) {
            System.err.println("OutlineShape.getTriangles().X: "+triangles.size()+", updated "+updated);
            if( updated ) {
                int i=0;
                for(final Triangle t : triangles) {
                    System.err.printf("- [%d]: %s%n", i++, t);
                }
            }
        }
        return triangles;
    }

    /**
     * Return a transformed instance with all {@link Outline}s are copied and transformed.
     * <p>
     * Note: Triangulated data is lost in returned instance!
     * </p>
     */
    public final OutlineShape transform(final AffineTransform t) {
        final OutlineShape newOutlineShape = new OutlineShape();
        final int osize = outlines.size();
        for(int i=0; i<osize; i++) {
            newOutlineShape.addOutline( outlines.get(i).transform(t) );
        }
        return newOutlineShape;
    }

    /**
     * Sort the outlines from large
     * to small depending on the AABox
     */
    private void sortOutlines() {
        Collections.sort(outlines, reversSizeComparator);
    }

    private static Comparator<Outline> reversSizeComparator = new Comparator<Outline>() {
        @Override
        public int compare(final Outline o1, final Outline o2) {
            return o2.compareTo(o1); // reverse !
        } };

    /**
     * Compare two outline shape's Bounding Box size.
     * @see AABBox#getSize()
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public final int compareTo(final OutlineShape other) {
        final float thisSize = getBounds().getSize();
        final float otherSize = other.getBounds().getSize();
        if( FloatUtil.isEqual2(thisSize, otherSize) ) {
            return 0;
        } else if( thisSize < otherSize ){
            return -1;
        } else {
            return 1;
        }
    }

    private void validateBoundingBox() {
        dirtyBits &= ~DIRTY_BOUNDS;
        bbox.reset();
        for (int i=0; i<outlines.size(); i++) {
            bbox.resize(outlines.get(i).getBounds());
        }
    }

    public final AABBox getBounds() {
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            validateBoundingBox();
        }
        return bbox;
    }

    /**
     * @param obj the Object to compare this OutlineShape with
     * @return true if {@code obj} is an OutlineShape, not null,
     *                 same outlineState, equal bounds and equal outlines in the same order
     */
    @Override
    public final boolean equals(final Object obj) {
        if( obj == this) {
            return true;
        }
        if( null == obj || !(obj instanceof OutlineShape) ) {
            return false;
        }
        final OutlineShape o = (OutlineShape) obj;
        if(getOutlineState() != o.getOutlineState()) {
            return false;
        }
        if(getOutlineCount() != o.getOutlineCount()) {
            return false;
        }
        if( !getBounds().equals( o.getBounds() ) ) {
            return false;
        }
        for (int i=getOutlineCount()-1; i>=0; i--) {
            if( ! getOutline(i).equals( o.getOutline(i) ) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final int hashCode() {
        throw new InternalError("hashCode not designed");
    }

    @Override
    public String toString() {
        // Avoid calling this.hashCode() !
        return getClass().getName() + "@" + Integer.toHexString(super.hashCode());
    }

    public void print(final PrintStream out) {
        final int oc = getOutlineCount();
        for (int oi = 0; oi < oc; oi++) {
            final Outline outline = getOutline(oi);
            final int vc = outline.getVertexCount();
            out.printf("- OL[%d]: %s%n", vc, outline.getWinding());
            for(int vi=0; vi < vc; vi++) {
                final Vertex v = outline.getVertex(vi);
                out.printf("-- OS[%d][%d]: %s%n", oi, vi, v);
            }
        }
    }
}
