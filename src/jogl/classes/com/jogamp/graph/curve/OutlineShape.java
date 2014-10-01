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
import java.util.Collections;
import java.util.Comparator;

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.tess.Triangulation;
import com.jogamp.graph.curve.tess.Triangulator;
import com.jogamp.graph.geom.Outline;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.math.geom.AABBox;


/**
 * A Generic shape objects which is defined by a list of Outlines.
 * This Shape can be transformed to triangulations.
 * The list of triangles generated are render-able by a Region object.
 * The triangulation produced by this Shape will define the
 * closed region defined by the outlines.
 *
 * One or more OutlineShape Object can be associated to a region
 * this is left as a high-level representation of the Objects. For
 * optimizations, flexibility requirements for future features.
 *
 * <br><br>
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
 * The above will create two outlines each with three vertices. By adding these two outlines to
 * the OutlineShape, we are stating that the combination of the two outlines represent the shape.
 * <br>
 *
 * To specify that the shape is curved at a region, the on-curve flag should be set to false
 * for the vertex that is in the middle of the curved region (if the curved region is defined by 3
 * vertices (quadratic curve).
 * <br>
 * In case the curved region is defined by 4 or more vertices the middle vertices should both have
 * the on-curve flag set to false.
 *
 * <br>Example: <br>
 * <pre>
      addVertex(0,0, true);
      addVertex(0,1, false);
      addVertex(1,1, false);
      addVertex(1,0, true);
 * </pre>
 *
 * The above snippet defines a cubic nurbs curve where (0,1 and 1,1)
 * do not belong to the final rendered shape.
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
public class OutlineShape implements Comparable<OutlineShape> {
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

    public static final int DIRTY_BOUNDS = 1 << 0;
    /**
     * Modified shape, requires to update the vertices and triangles, here: vertices.
     */
    public static final int DIRTY_VERTICES  = 1 << 1;
    /**
     * Modified shape, requires to update the vertices and triangles, here: triangulation.
     */
    public static final int DIRTY_TRIANGLES  = 1 << 2;

    private final Vertex.Factory<? extends Vertex> vertexFactory;

    /** The list of {@link Outline}s that are part of this
     *  outline shape.
     */
    /* pp */ final ArrayList<Outline> outlines;

    private final AABBox bbox;
    private final ArrayList<Triangle> triangles;
    private final ArrayList<Vertex> vertices;
    private int addedVerticeCount;

    private VerticesState outlineState;

    /** dirty bits DIRTY_BOUNDS */
    private int dirtyBits;

    private float sharpness;

    private final float[] tmpV1 = new float[3];
    private final float[] tmpV2 = new float[3];
    private final float[] tmpV3 = new float[3];

    /** Create a new Outline based Shape
     */
    public OutlineShape(final Vertex.Factory<? extends Vertex> factory) {
        this.vertexFactory = factory;
        this.outlines = new ArrayList<Outline>(3);
        this.outlines.add(new Outline());
        this.outlineState = VerticesState.UNDEFINED;
        this.bbox = new AABBox();
        this.triangles = new ArrayList<Triangle>();
        this.vertices = new ArrayList<Vertex>();
        this.addedVerticeCount = 0;
        this.dirtyBits = 0;
        this.sharpness = DEFAULT_SHARPNESS;
    }

    /**
     * Return the number of newly added vertices during {@link #getTriangles(VerticesState)}
     * while transforming the outlines to {@link VerticesState#QUADRATIC_NURBS} and triangulation.
     * @see #setIsQuadraticNurbs()
     */
    public int getAddedVerticeCount() {
        return addedVerticeCount;
    }

    /** Sharpness value, defaults to {@link #DEFAULT_SHARPNESS}. */
    public float getSharpness() { return sharpness; }

    /** Sets sharpness, defaults to {@link #DEFAULT_SHARPNESS}. */
    public void setSharpness(final float s) {
        if( this.sharpness != s ) {
            clearCache();
            sharpness=s;
        }
    }

    /** Clears all data and reset all states as if this instance was newly created */
    public void clear() {
        outlines.clear();
        outlines.add(new Outline());
        outlineState = VerticesState.UNDEFINED;
        bbox.reset();
        vertices.clear();
        triangles.clear();
        addedVerticeCount = 0;
        dirtyBits = 0;
    }

    /** Clears cached triangulated data, i.e. {@link #getTriangles(VerticesState)} and {@link #getVertices()}.  */
    public void clearCache() {
        vertices.clear();
        triangles.clear();
        dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES;
    }

    /**
     * Returns the associated vertex factory of this outline shape
     * @return Vertex.Factory object
     */
    public final Vertex.Factory<? extends Vertex> vertexFactory() { return vertexFactory; }

    public final int getOutlineNumber() {
        return outlines.size();
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
                dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES;
                return;
            }
        }
        outlines.add(position, outline);
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            bbox.resize(outline.getBounds());
        }
        dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES;
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
        for(int i=0; i<outlineShape.getOutlineNumber(); i++) {
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
        dirtyBits |= DIRTY_BOUNDS | DIRTY_TRIANGLES | DIRTY_VERTICES;
    }

    /**
     * Removes the {@link Outline} element at the given {@code position}.
     * <p>Sets the bounding box dirty, hence a next call to {@link #getBounds()} will validate it.</p>
     *
     * @param position of the to be removed Outline
     * @throws IndexOutOfBoundsException if position is out of range (position < 0 || position >= getOutlineNumber())
     */
    public final Outline removeOutline(final int position) throws IndexOutOfBoundsException {
        dirtyBits |= DIRTY_BOUNDS | DIRTY_TRIANGLES | DIRTY_VERTICES;
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
     * @param v the vertex to be added to the OutlineShape
     */
    public final void addVertex(final Vertex v) {
        final Outline lo = getLastOutline();
        lo.addVertex(v);
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            bbox.resize(v.getCoord());
        }
        // vertices.add(v); // FIXME: can do and remove DIRTY_VERTICES ?
        dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES;
    }

    /**
     * Adds a vertex to the last open outline to the shape at {@code position}
     * @param position indx at which the vertex will be added
     * @param v the vertex to be added to the OutlineShape
     */
    public final void addVertex(final int position, final Vertex v) {
        final Outline lo = getLastOutline();
        lo.addVertex(position, v);
        if( 0 == ( dirtyBits & DIRTY_BOUNDS ) ) {
            bbox.resize(v.getCoord());
        }
        dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES;
    }

    /**
     * Add a 2D {@link Vertex} to the last outline by defining the coordinate attribute
     * of the vertex. The 2D vertex will be represented as Z=0.
     *
     * @param x the x coordinate
     * @param y the y coordniate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     */
    public final void addVertex(final float x, final float y, final boolean onCurve) {
        addVertex(vertexFactory.create(x, y, 0f, onCurve));
    }

    /**
     * Add a 3D {@link Vertex} to the last outline by defining the coordniate attribute
     * of the vertex.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     */
    public final void addVertex(final float x, final float y, final float z, final boolean onCurve) {
        addVertex(vertexFactory.create(x, y, z, onCurve));
    }

    /**
     * Add a vertex to the last outline by passing a float array and specifying the
     * offset and length in which. The attributes of the vertex are located.
     * The attributes should be continuous (stride = 0).
     * Attributes which value are not set (when length less than 3)
     * are set implicitly to zero.
     * @param coordsBuffer the coordinate array where the vertex attributes are to be picked from
     * @param offset the offset in the buffer to the x coordinate
     * @param length the number of attributes to pick from the buffer (maximum 3)
     * @param onCurve flag if this vertex is on the final curve or defines a curved region
     * of the shape around this vertex.
     */
    public final void addVertex(final float[] coordsBuffer, final int offset, final int length, final boolean onCurve) {
        addVertex(vertexFactory.create(coordsBuffer, offset, length, onCurve));
    }

    /**
     * Closes the last outline in the shape.
     * <p>
     * Checks whether the last vertex equals to the first of the last outline.
     * If not equal, it either appends a clone of the first vertex
     * or prepends a clone of the last vertex, depending on <code>closeTail</code>.
     * </p>
     * @param closeTail if true, a clone of the first vertex will be appended,
     *                  otherwise a clone of the last vertex will be prepended.
     */
    public final void closeLastOutline(final boolean closeTail) {
        if( getLastOutline().setClosed(true) ) {
            dirtyBits |= DIRTY_TRIANGLES | DIRTY_VERTICES;
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
        VectorUtil.midVec3(tmpV1, a.getCoord(), b.getCoord());
        VectorUtil.midVec3(tmpV3, b.getCoord(), c.getCoord());
        VectorUtil.midVec3(tmpV2, tmpV1, tmpV3);

        //drop off-curve vertex to image on the curve
        b.setCoord(tmpV2, 0, 3);
        b.setOnCurve(true);

        outline.addVertex(index, vertexFactory.create(tmpV1, 0, 3, false));
        outline.addVertex(index+2, vertexFactory.create(tmpV3, 0, 3, false));

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
        final int count = getOutlineNumber();
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
                        if( overlaps.contains(currentVertex) || overlap != null ) {
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
        final int count = getOutlineNumber();
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

                if( VectorUtil.isVec3InTriangle3(a.getCoord(), b.getCoord(), c.getCoord(),
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
    @SuppressWarnings("unused")
    private Vertex checkTriOverlaps1(final Vertex a, final Vertex b, final Vertex c) {
        final int count = getOutlineNumber();
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

                if( VectorUtil.isVec3InTriangle3(a.getCoord(), b.getCoord(), c.getCoord(),
                                                 current.getCoord(), nextV.getCoord(), prevV.getCoord(),
                                                 tmpV1, tmpV2, tmpV3, FloatUtil.EPSILON) ) {
                    return current;
                }
                if(VectorUtil.testTri2SegIntersection(a, b, c, prevV, current, FloatUtil.EPSILON) ||
                   VectorUtil.testTri2SegIntersection(a, b, c, current, nextV, FloatUtil.EPSILON) ||
                   VectorUtil.testTri2SegIntersection(a, b, c, prevV, nextV, FloatUtil.EPSILON) ) {
                    return current;
                }
            }
        }
        return null;
    }

    private void cleanupOutlines() {
        final boolean transformOutlines2Quadratic = VerticesState.QUADRATIC_NURBS != outlineState;
        int count = getOutlineNumber();
        for (int cc = 0; cc < count; cc++) {
            final Outline outline = getOutline(cc);
            int vertexCount = outline.getVertexCount();

            if( transformOutlines2Quadratic ) {
                for(int i=0; i < vertexCount; i++) {
                    final Vertex currentVertex = outline.getVertex(i);
                    final int j = (i+1)%vertexCount;
                    final Vertex nextVertex = outline.getVertex(j);
                    if ( !currentVertex.isOnCurve() && !nextVertex.isOnCurve() ) {
                        VectorUtil.midVec3(tmpV1, currentVertex.getCoord(), nextVertex.getCoord());
                        System.err.println("XXX: Cubic: "+i+": "+currentVertex+", "+j+": "+nextVertex);
                        final Vertex v = vertexFactory.create(tmpV1, 0, 3, true);
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
            } else  if( 0 < vertexCount &&
                        VectorUtil.isVec3Equal( outline.getVertex(0).getCoord(), 0, outline.getLastVertex().getCoord(), 0, FloatUtil.EPSILON )) {
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
        final boolean updated;
        if( 0 != ( DIRTY_VERTICES & dirtyBits ) ) {
            vertices.clear();
            for(int i=0; i<outlines.size(); i++) {
                vertices.addAll(outlines.get(i).getVertices());
            }
            dirtyBits &= ~DIRTY_VERTICES;
            updated = true;
        } else {
            updated = false;
        }
        if(Region.DEBUG_INSTANCE) {
            System.err.println("OutlineShape.getVertices(): o "+outlines.size()+", v "+vertices.size()+", updated "+updated);
        }
        return vertices;
    }

    private void triangulateImpl() {
        if( 0 < outlines.size() ) {
            sortOutlines();
            generateVertexIds();

            triangles.clear();
            final Triangulator triangulator2d = Triangulation.create();
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
    public ArrayList<Triangle> getTriangles(final VerticesState destinationType) {
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
        final OutlineShape newOutlineShape = new OutlineShape(vertexFactory);
        final int osize = outlines.size();
        for(int i=0; i<osize; i++) {
            newOutlineShape.addOutline( outlines.get(i).transform(t, vertexFactory) );
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
        if( FloatUtil.isEqual(thisSize, otherSize, FloatUtil.EPSILON) ) {
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
    public boolean equals(final Object obj) {
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
        if(getOutlineNumber() != o.getOutlineNumber()) {
            return false;
        }
        if( !getBounds().equals( o.getBounds() ) ) {
            return false;
        }
        for (int i=getOutlineNumber()-1; i>=0; i--) {
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
}
