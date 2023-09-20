/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Denis M. Kishenko
 * @author Sven Gothel
 */
package com.jogamp.math.geom.plane;

import java.io.PrintStream;
import java.util.NoSuchElementException;

import com.jogamp.math.geom.AABBox;

/**
 * Path2F represents and provides construction method for a 2D shape using float[2] points.
 */
public final class Path2F implements Cloneable {
    static final String invalidWindingRuleValue = "Invalid winding rule value";
    static final String iteratorOutOfBounds = "Iterator out of bounds";

    /** A Path2D segment type */
    public static enum SegmentType {
        MOVETO(1),
        LINETO(1),
        QUADTO(2),
        CUBICTO(3),
        CLOSE(0);

        /** Number of points associated with this segment type */
        public final int point_count;

        /** Return the integer segment type value as a byte */
        public byte integer() {
            return (byte) this.ordinal();
        }

        /** Return the SegmentType associated with the integer segment type */
        public static SegmentType valueOf(final int type) {
            switch( type ) {
                case 0: return MOVETO;
                case 1: return LINETO;
                case 2: return QUADTO;
                case 3: return CUBICTO;
                case 4: return CLOSE;
                default:
                    throw new IllegalArgumentException("Unhandled Segment Type: "+type);
            }
        }

        /** Return the number of points associated with the integer segment type */
        public static int getPointCount(final int type) {
            switch( type ) {
                case 0: return MOVETO.point_count;
                case 1: return LINETO.point_count;
                case 2: return QUADTO.point_count;
                case 3: return CUBICTO.point_count;
                case 4: return CLOSE.point_count;
                default:
                    throw new IllegalArgumentException("Unhandled Segment Type: "+type);
            }
        }

        SegmentType(final int v) {
            this.point_count = v;
        }
    }

    /**
     * The buffers size
     */
    private static final int BUFFER_SIZE = 10;

    /**
     * The buffers capacity
     */
    private static final int BUFFER_CAPACITY = 10;

    /**
     * The point's types buffer
     */
    private byte[] m_types;

    /**
     * The points buffer
     */
    private float[] m_points;

    /**
     * The point's type buffer size
     */
    private int m_typeSize;

    /**
     * The points buffer size
     */
    private int m_pointSize;

    /**
     * The winding path rule
     */
    private WindingRule m_rule;

    /*
     * GeneralPath path iterator
     */
    public static final class Iterator {

        /**
         * The source GeneralPath object
         */
        private final Path2F p;

        /**
         * The path iterator transformation
         */
        private final AffineTransform t;

        /**
         * The current cursor position in types buffer
         */
        private int typeIndex;

        /**
         * The current cursor position in points buffer
         */
        private int pointIndex;

        /**
         * Constructs a new GeneralPath.Iterator for given general path
         * @param path - the source GeneralPath object
         */
        Iterator(final Path2F path) {
            this(path, null);
        }

        /**
         * Constructs a new GeneralPath.Iterator for given general path and transformation
         * @param path - the source GeneralPath object
         * @param at - the AffineTransform object to apply rectangle path
         */
        public Iterator(final Path2F path, final AffineTransform at) {
            this.p = path;
            this.t = at;
            reset();
        }

        private void reset() {
            typeIndex = 0;
            pointIndex = 0;
        }

        /** Return the {@link WindingRule} set */
        public WindingRule getWindingRule() {
            return p.getWindingRule();
        }

        /**
         * Compute the  general winding of the vertices
         * @return CCW or CW {@link Winding}
         */
        public Winding getWinding() {
            return area() >= 0 ? Winding.CCW : Winding.CW ;
        }

        /** Returns reference of the point array covering the whole iteration of Path2D, use {@link #index()} to access the current point. */
        public float[] points() { return p.m_points; }

        /** Return the {@link #points()} index for the current segment. */
        public int index() { return pointIndex; }

        /** Return current segment type */
        public SegmentType getType() { return SegmentType.valueOf( p.m_types[typeIndex] ); }

        /**
         * Return the current segment type and copies the current segment's points to given storage
         * @param coords storage for current segment's points
         * @return current segment type
         * @see #points()
         * @see #type_index()
         * @see #getType()
         * @deprecated try to use {@link #index()}, {@link #points()} and {@link #next()} to avoid copying
         */
        @Deprecated
        public SegmentType currentSegment(final float[] coords) {
            if (!hasNext()) {
                throw new NoSuchElementException(iteratorOutOfBounds);
            }
            final SegmentType type = getType();
            final int count = type.point_count;
            System.arraycopy(p.m_points, pointIndex, coords, 0, count*2);
            if (t != null) {
                t.transform(coords, 0, coords, 0, count);
            }
            return type;
        }

        /** Returns true if the iteration has more elements. */
        public boolean hasNext() {
            return typeIndex < p.m_typeSize;
        }

        /** Returns the current segment type in the iteration, then moving to the next path segment. */
        public SegmentType next() {
            final SegmentType t = getType();
            pointIndex += 2 * t.point_count;
            ++typeIndex;
            return t;
        }

        /**
         * Computes the area of the path to check if ccw
         * @return positive area if ccw else negative area value
         */
        private float area() {
            float area = 0.0f;
            final float[] points = points();
            final float[] pCoord = new float[2];
            while ( hasNext() ) {
                final int idx = index();
                final SegmentType type = next();
                switch ( type ) {
                    case MOVETO:
                        pCoord[0] = points[idx+0];
                        pCoord[1] = points[idx+1];
                        break;
                    case LINETO:
                        area += pCoord[0] * points[idx+1] - points[idx+0] * pCoord[1];
                        pCoord[0] = points[idx+0];
                        pCoord[1] = points[idx+1];
                        break;
                    case QUADTO:
                        area += pCoord[0]     * points[idx+1] - points[idx+0] * pCoord[1];
                        area += points[idx+0] * points[idx+3] - points[idx+2] * points[idx+1];
                        pCoord[0] = points[idx+2];
                        pCoord[1] = points[idx+3];
                        break;
                    case CUBICTO:
                        area += pCoord[0]     * points[idx+1] - points[idx+0] * pCoord[1];
                        area += points[idx+0] * points[idx+3] - points[idx+2] * points[idx+1];
                        area += points[idx+2] * points[idx+5] - points[idx+4] * points[idx+3];
                        pCoord[0] = points[idx+4];
                        pCoord[1] = points[idx+5];
                        break;
                    case CLOSE:
                        break;
                }
            }
            reset();
            return area;
        }
    }

    public Path2F() {
        this(WindingRule.NON_ZERO, BUFFER_SIZE, BUFFER_SIZE);
    }

    public Path2F(final WindingRule rule) {
        this(rule, BUFFER_SIZE, BUFFER_SIZE);
    }

    public Path2F(final WindingRule rule, final int initialCapacity) {
        this(rule, initialCapacity, initialCapacity);
    }

    public Path2F(final WindingRule rule, final int initialTypeCapacity, final int initialPointCapacity) {
        setWindingRule(rule);
        m_types = new byte[initialTypeCapacity];
        m_points = new float[initialPointCapacity * 2];
    }

    public Path2F(final Path2F path) {
        this(WindingRule.NON_ZERO, BUFFER_SIZE);
        final Iterator p = path.iterator(null);
        setWindingRule(p.getWindingRule());
        append(p, false);
    }

    /** Set the {@link WindingRule} set */
    public void setWindingRule(final WindingRule rule) {
        this.m_rule = rule;
    }

    /** Return the {@link WindingRule} set */
    public WindingRule getWindingRule() {
        return m_rule;
    }

    /**
     * Checks points and types buffer size to add pointCount points. If necessary realloc buffers to enlarge size.
     * @param pointCount - the point count to be added in buffer
     */
    private void checkBuf(final int pointCount, final boolean checkMove) {
        if (checkMove && m_typeSize == 0) {
            throw new IllegalPathStateException("First segment should be SEG_MOVETO type");
        }
        if (m_typeSize == m_types.length) {
            final byte tmp[] = new byte[m_typeSize + BUFFER_CAPACITY];
            System.arraycopy(m_types, 0, tmp, 0, m_typeSize);
            m_types = tmp;
        }
        if (m_pointSize + pointCount > m_points.length) {
            final float tmp[] = new float[m_pointSize + Math.max(BUFFER_CAPACITY * 2, pointCount)];
            System.arraycopy(m_points, 0, tmp, 0, m_pointSize);
            m_points = tmp;
        }
    }

    /**
     * Start a new position for the next line segment at given point x/y (P1).
     * @param x point (P1)
     * @param y point (P1)
     */
    public void moveTo(final float x, final float y) {
        if ( m_typeSize > 0 && m_types[m_typeSize - 1] == SegmentType.MOVETO.integer() ) {
            m_points[m_pointSize - 2] = x;
            m_points[m_pointSize - 1] = y;
        } else {
            checkBuf(2, false);
            m_types[m_typeSize++] = SegmentType.MOVETO.integer();
            m_points[m_pointSize++] = x;
            m_points[m_pointSize++] = y;
        }
    }

    /**
     * Add a line segment, intersecting the last point and the given point x/y (P1).
     * @param x final point (P1)
     * @param y final point (P1)
     */
    public void lineTo(final float x, final float y) {
        checkBuf(2, true);
        m_types[m_typeSize++] = SegmentType.LINETO.integer();
        m_points[m_pointSize++] = x;
        m_points[m_pointSize++] = y;
    }

    /**
     * Add a quadratic curve segment, intersecting the last point and the second given point x2/y2 (P2).
     * @param x1 quadratic parametric control point (P1)
     * @param y1 quadratic parametric control point (P1)
     * @param x2 final interpolated control point (P2)
     * @param y2 final interpolated control point (P2)
     */
    public void quadTo(final float x1, final float y1, final float x2, final float y2) {
        checkBuf(4, true);
        m_types[m_typeSize++] = SegmentType.QUADTO.integer();
        m_points[m_pointSize++] = x1;
        m_points[m_pointSize++] = y1;
        m_points[m_pointSize++] = x2;
        m_points[m_pointSize++] = y2;
    }

    /**
     * Add a cubic Bézier curve segment, intersecting the last point and the second given point x3/y3 (P3).
     * @param x1 Bézier control point (P1)
     * @param y1 Bézier control point (P1)
     * @param x2 Bézier control point (P2)
     * @param y2 Bézier control point (P2)
     * @param x3 final interpolated control point (P3)
     * @param y3 final interpolated control point (P3)
     */
    public void cubicTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3) {
        checkBuf(6, true);
        m_types[m_typeSize++] = SegmentType.CUBICTO.integer();
        m_points[m_pointSize++] = x1;
        m_points[m_pointSize++] = y1;
        m_points[m_pointSize++] = x2;
        m_points[m_pointSize++] = y2;
        m_points[m_pointSize++] = x3;
        m_points[m_pointSize++] = y3;
    }

    /**
     * Closes the current sub-path segment by drawing a straight line back to the coordinates of the last moveTo. If the path is already closed then this method has no effect.
     */
    public void closePath() {
        if (!isClosed()) {
            checkBuf(0, true);
            m_types[m_typeSize++] = SegmentType.CLOSE.integer();
        }
    }

    final public int size() {
        return m_typeSize;
    }

    /**
     * Returns true if the last sub-path is closed, otherwise false.
     */
    final public boolean isClosed() {
        return m_typeSize > 0 && m_types[m_typeSize - 1] == SegmentType.CLOSE.integer() ;
    }

    /**
     * Compute the  general winding of the vertices
     * @param vertices array of Vertices
     * @return CCW or CW {@link Winding}
     */
    public Winding getWinding() {
        return iterator(null).getWinding();
    }

    @Override
    public String toString() {
        return "[size "+size()+", closed "+isClosed()+", winding[rule "+getWindingRule()+", "+getWinding()+"]]";
    }

    /**
     * Append the given path geometry to this instance
     * @param path the {@link Path2F} to append to this instance
     * @param connect pass true to turn an initial moveTo segment into a lineTo segment to connect the new geometry to the existing path, otherwise pass false.
     */
    public void append(final Path2F path, final boolean connect) {
        append(path.iterator(null), connect);
    }

    /**
     * Append the given path geometry to this instance
     * @param path the {@link Path2F.Iterator} to append to this instance
     * @param connect pass true to turn an initial moveTo segment into a lineTo segment to connect the new geometry to the existing path, otherwise pass false.
     */
    public void append(final Iterator path, boolean connect) {
        final float[] points = path.points();
        while ( path.hasNext() ) {
            final int idx = path.index();
            final SegmentType type = path.next();
            switch ( type ) {
                case MOVETO:
                    if ( !connect || 0 == m_typeSize ) {
                        moveTo(points[idx+0], points[idx+1]);
                        break;
                    }
                    if ( m_types[m_typeSize - 1] != SegmentType.CLOSE.integer() &&
                         m_points[m_pointSize - 2] == points[idx+0] &&
                         m_points[m_pointSize - 1] == points[idx+1]
                       )
                    {
                        break;
                    }
                    // fallthrough: MOVETO -> LINETO
                case LINETO:
                    lineTo(points[idx+0], points[idx+1]);
                    break;
                case QUADTO:
                    quadTo(points[idx+0], points[idx+1], points[idx+2], points[idx+3]);
                    break;
                case CUBICTO:
                    cubicTo(points[idx+0], points[idx+1], points[idx+2], points[idx+3], points[idx+4], points[idx+5]);
                    break;
                case CLOSE:
                    closePath();
                    break;
            }
            connect = false;
        }
    }

    public void printSegments(final PrintStream out) {
        final Iterator path = iterator();
        final float[] points = path.points();
        int i = 0;
        while ( path.hasNext() ) {
            final int idx = path.index();
            final SegmentType type = path.next();
            switch ( type ) {
                case MOVETO:
                    out.printf("%2d: moveTo(%.4f/%.4f)%n", i, points[idx+0], points[idx+1]);
                    break;
                case LINETO:
                    out.printf("%2d: lineTo(%.4f/%.4f)%n", i, points[idx+0], points[idx+1]);
                    break;
                case QUADTO:
                    out.printf("%2d: quadTo(%.4f/%.4f, %.4f/%.4f)%n", i, points[idx+0], points[idx+1], points[idx+2], points[idx+3]);
                    break;
                case CUBICTO:
                    out.printf("%2d: cubicTo(%.4f/%.4f, %.4f/%.4f, %.4f/%.4f)%n", i, points[idx+0], points[idx+1], points[idx+2], points[idx+3], points[idx+4], points[idx+5]);
                    break;
                case CLOSE:
                    out.printf("%2d: closePath()%n", i);
                    break;
            }
            ++i;
        }
    }

    public void reset() {
        m_typeSize = 0;
        m_pointSize = 0;
    }

    public void transform(final AffineTransform t) {
        t.transform(m_points, 0, m_points, 0, m_pointSize / 2);
    }

    public Path2F createTransformedShape(final AffineTransform t) {
        final Path2F p = (Path2F)clone();
        if (t != null) {
            p.transform(t);
        }
        return p;
    }

    public final synchronized AABBox getBounds2D() {
        float rx1, ry1, rx2, ry2;
        if (m_pointSize == 0) {
            rx1 = ry1 = rx2 = ry2 = 0.0f;
        } else {
            int i = m_pointSize - 1;
            ry1 = ry2 = m_points[i--];
            rx1 = rx2 = m_points[i--];
            while (i > 0) {
                final float y = m_points[i--];
                final float x = m_points[i--];
                if (x < rx1) {
                    rx1 = x;
                } else
                    if (x > rx2) {
                        rx2 = x;
                    }
                if (y < ry1) {
                    ry1 = y;
                } else
                    if (y > ry2) {
                        ry2 = y;
                    }
            }
        }
        return new AABBox(rx1, ry1, 0f, rx2, ry2, 0f);
    }

    /**
     * Checks cross count according to path rule to define is it point inside shape or not.
     * @param cross - the point cross count
     * @return true if point is inside path, or false otherwise
     */
    boolean isInside(final int cross) {
        if (m_rule == WindingRule.NON_ZERO) {
            return Crossing2F.isInsideNonZero(cross);
        }
        return Crossing2F.isInsideEvenOdd(cross);
    }

    public boolean contains(final float px, final float py) {
        return isInside(Crossing2F.crossShape(this, px, py));
    }

    public boolean contains(final float rx, final float ry, final float rw, final float rh) {
        final int cross = Crossing2F.intersectShape(this, rx, ry, rw, rh);
        return cross != Crossing2F.CROSSING && isInside(cross);
    }

    public boolean intersects(final float rx, final float ry, final float rw, final float rh) {
        final int cross = Crossing2F.intersectShape(this, rx, ry, rw, rh);
        return cross == Crossing2F.CROSSING || isInside(cross);
    }

    public boolean contains(final AABBox r) {
        return contains(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
    }

    public boolean intersects(final AABBox r) {
        return intersects(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
    }

    public Iterator iterator() {
        return new Iterator(this);
    }

    public Iterator iterator(final AffineTransform t) {
        return new Iterator(this, t);
    }

    /* public Path2F.Iterator getPathIterator(AffineTransform t, float flatness) {
        return new FlatteningPathIterator(getPathIterator(t), flatness);
    } */

    @Override
    public Object clone() {
        try {
            final Path2F p = (Path2F) super.clone();
            p.m_types = m_types.clone();
            p.m_points = m_points.clone();
            return p;
        } catch (final CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}

