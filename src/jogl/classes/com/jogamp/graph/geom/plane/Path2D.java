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
package com.jogamp.graph.geom.plane;

import java.util.NoSuchElementException;

import com.jogamp.graph.geom.SVertex;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.math.geom.AABBox;


public final class Path2D implements Cloneable {

    public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;
    public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;

    static final String invalidWindingRuleValue = "Invalid winding rule value";
    static final String iteratorOutOfBounds = "Iterator out of bounds";

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
    byte[] m_types;

    /**
     * The points buffer
     */
    float[] m_points;

    /**
     * The point's type buffer size
     */
    int m_typeSize;

    /**
     * The points buffer size
     */
    int m_pointSize;

    /**
     * The path rule
     */
    int m_rule;

    /**
     * The space amount in points buffer for different segmenet's types
     */
    static int pointShift[] = {
            2,  // MOVETO
            2,  // LINETO
            4,  // QUADTO
            6,  // CUBICTO
            0}; // CLOSE

    /*
     * GeneralPath path iterator
     */
    static class Iterator implements PathIterator {

        /**
         * The current cursor position in types buffer
         */
        int typeIndex;

        /**
         * The current cursor position in points buffer
         */
        int pointIndex;

        /**
         * The source GeneralPath object
         */
        Path2D p;

        /**
         * The path iterator transformation
         */
        AffineTransform t;

        /**
         * Constructs a new GeneralPath.Iterator for given general path
         * @param path - the source GeneralPath object
         */
        Iterator(final Path2D path) {
            this(path, null);
        }

        /**
         * Constructs a new GeneralPath.Iterator for given general path and transformation
         * @param path - the source GeneralPath object
         * @param at - the AffineTransform object to apply rectangle path
         */
        Iterator(final Path2D path, final AffineTransform at) {
            this.p = path;
            this.t = at;
        }

        @Override
        public int getWindingRule() {
            return p.getWindingRule();
        }

        @Override
        public int index() { return typeIndex; }

        @Override
        public float[] points() { return p.m_points; }

        @Override
        public int getType(final int idx) { return p.m_types[idx]; }

        @Override
        public boolean isDone() {
            return typeIndex >= p.m_typeSize;
        }

        @Override
        public void next() {
            typeIndex++;
        }

        @Override
        public int currentSegment(final float[] coords) {
            if (isDone()) {
                throw new NoSuchElementException(iteratorOutOfBounds);
            }
            final int type = p.m_types[typeIndex];
            final int count = Path2D.pointShift[type];
            System.arraycopy(p.m_points, pointIndex, coords, 0, count);
            if (t != null) {
                t.transform(coords, 0, coords, 0, count / 2);
            }
            pointIndex += count;
            return type;
        }

    }

    public float[] points() { return m_points; }
    public int getType(final int idx) { return m_types[idx]; }
    public static int getPointCount(final int type) { return pointShift[type]; }

    public Path2D() {
        this(WIND_NON_ZERO, BUFFER_SIZE);
    }

    public Path2D(final int rule) {
        this(rule, BUFFER_SIZE);
    }

    public Path2D(final int rule, final int initialCapacity) {
        setWindingRule(rule);
        m_types = new byte[initialCapacity];
        m_points = new float[initialCapacity * 2];
    }

    public Path2D(final Path2D path) {
        this(WIND_NON_ZERO, BUFFER_SIZE);
        final PathIterator p = path.iterator(null);
        setWindingRule(p.getWindingRule());
        append(p, false);
    }

    public void setWindingRule(final int rule) {
        if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO) {
            throw new NoSuchElementException(invalidWindingRuleValue);
        }
        this.m_rule = rule;
    }

    public int getWindingRule() {
        return m_rule;
    }

    /**
     * Checks points and types buffer size to add pointCount points. If necessary realloc buffers to enlarge size.
     * @param pointCount - the point count to be added in buffer
     */
    void checkBuf(final int pointCount, final boolean checkMove) {
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

    public void moveTo(final float x, final float y) {
        if (m_typeSize > 0 && m_types[m_typeSize - 1] == PathIterator.SEG_MOVETO) {
            m_points[m_pointSize - 2] = x;
            m_points[m_pointSize - 1] = y;
        } else {
            checkBuf(2, false);
            m_types[m_typeSize++] = PathIterator.SEG_MOVETO;
            m_points[m_pointSize++] = x;
            m_points[m_pointSize++] = y;
        }
    }

    public void lineTo(final float x, final float y) {
        checkBuf(2, true);
        m_types[m_typeSize++] = PathIterator.SEG_LINETO;
        m_points[m_pointSize++] = x;
        m_points[m_pointSize++] = y;
    }

    public void quadTo(final float x1, final float y1, final float x2, final float y2) {
        checkBuf(4, true);
        m_types[m_typeSize++] = PathIterator.SEG_QUADTO;
        m_points[m_pointSize++] = x1;
        m_points[m_pointSize++] = y1;
        m_points[m_pointSize++] = x2;
        m_points[m_pointSize++] = y2;
    }

    public void curveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3) {
        checkBuf(6, true);
        m_types[m_typeSize++] = PathIterator.SEG_CUBICTO;
        m_points[m_pointSize++] = x1;
        m_points[m_pointSize++] = y1;
        m_points[m_pointSize++] = x2;
        m_points[m_pointSize++] = y2;
        m_points[m_pointSize++] = x3;
        m_points[m_pointSize++] = y3;
    }

    final public int size() {
        return m_typeSize;
    }

    final public boolean isClosed() {
        return m_typeSize > 0 && m_types[m_typeSize - 1] == PathIterator.SEG_CLOSE ;
    }

    public void closePath() {
        if (!isClosed()) {
            checkBuf(0, true);
            m_types[m_typeSize++] = PathIterator.SEG_CLOSE;
        }
    }

    @Override
    public String toString() {
        return "[size "+size()+", closed "+isClosed()+"]";
    }

    public void append(final Path2D path, final boolean connect) {
        final PathIterator p = path.iterator(null);
        append(p, connect);
    }

    public void append(final PathIterator path, boolean connect) {
        final float[] points = path.points();
        while ( !path.isDone() ) {
            final int index = path.index();
            final int type = path.getType(index);
            switch ( type ) {
                case PathIterator.SEG_MOVETO:
                    if (!connect || m_typeSize == 0) {
                        moveTo(points[index+0], points[index+1]);
                        break;
                    }
                    if (m_types[m_typeSize - 1] != PathIterator.SEG_CLOSE &&
                        m_points[m_pointSize - 2] == points[index+0] &&
                        m_points[m_pointSize - 1] == points[index+1])
                    {
                        break;
                    }
                // NO BREAK;
                case PathIterator.SEG_LINETO:
                    lineTo(points[index+0], points[index+1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    quadTo(points[index+0], points[index+1], points[index+2], points[index+3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    curveTo(points[index+0], points[index+1], points[index+2], points[index+3], points[index+4], points[index+5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    closePath();
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled Segment Type: "+type);
            }
            path.next();
            connect = false;
        }
    }

    public SVertex getCurrentPoint() {
        if (m_typeSize == 0) {
            return null;
        }
        int j = m_pointSize - 2;
        if (m_types[m_typeSize - 1] == PathIterator.SEG_CLOSE) {

            for (int i = m_typeSize - 2; i > 0; i--) {
                final int type = m_types[i];
                if (type == PathIterator.SEG_MOVETO) {
                    break;
                }
                j -= pointShift[type];
            }
        }
        return new SVertex(m_points[j], m_points[j + 1], 0f, true);
    }

    public void reset() {
        m_typeSize = 0;
        m_pointSize = 0;
    }

    public void transform(final AffineTransform t) {
        t.transform(m_points, 0, m_points, 0, m_pointSize / 2);
    }

    public Path2D createTransformedShape(final AffineTransform t) {
        final Path2D p = (Path2D)clone();
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
        if (m_rule == WIND_NON_ZERO) {
            return Crossing.isInsideNonZero(cross);
        }
        return Crossing.isInsideEvenOdd(cross);
    }

    public boolean contains(final float px, final float py) {
        return isInside(Crossing.crossShape(this, px, py));
    }

    public boolean contains(final float rx, final float ry, final float rw, final float rh) {
        final int cross = Crossing.intersectShape(this, rx, ry, rw, rh);
        return cross != Crossing.CROSSING && isInside(cross);
    }

    public boolean intersects(final float rx, final float ry, final float rw, final float rh) {
        final int cross = Crossing.intersectShape(this, rx, ry, rw, rh);
        return cross == Crossing.CROSSING || isInside(cross);
    }

    public boolean contains(final Vertex p) {
        return contains(p.getX(), p.getY());
    }

    public boolean contains(final AABBox r) {
        return contains(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
    }

    public boolean intersects(final AABBox r) {
        return intersects(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
    }

    public PathIterator iterator() {
        return new Iterator(this);
    }

    public PathIterator iterator(final AffineTransform t) {
        return new Iterator(this, t);
    }

    /* public PathIterator getPathIterator(AffineTransform t, float flatness) {
        return new FlatteningPathIterator(getPathIterator(t), flatness);
    } */

    @Override
    public Object clone() {
        try {
            final Path2D p = (Path2D) super.clone();
            p.m_types = m_types.clone();
            p.m_points = m_points.clone();
            return p;
        } catch (final CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}

