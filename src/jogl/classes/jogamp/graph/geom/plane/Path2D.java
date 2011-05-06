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
 */
package jogamp.graph.geom.plane;

import java.util.NoSuchElementException;

import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.opengl.SVertex;

import jogamp.graph.math.plane.Crossing;

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
    byte[] types;
    
    /**
     * The points buffer
     */
    float[] points;
    
    /**
     * The point's type buffer size
     */
    int typeSize;
    
    /**
     * The points buffer size
     */
    int pointSize;
    
    /**
     * The path rule 
     */
    int rule;

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
    class Iterator implements PathIterator {

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
        Iterator(Path2D path) {
            this(path, null);
        }

        /**
         * Constructs a new GeneralPath.Iterator for given general path and transformation
         * @param path - the source GeneralPath object
         * @param at - the AffineTransform object to apply rectangle path
         */
        Iterator(Path2D path, AffineTransform at) {
            this.p = path;
            this.t = at;
        }

        public int getWindingRule() {
            return p.getWindingRule();
        }

        public boolean isDone() {
            return typeIndex >= p.typeSize;
        }

        public void next() {
            typeIndex++;
        }

        public int currentSegment(float[] coords) {
            if (isDone()) {
                throw new NoSuchElementException(iteratorOutOfBounds);
            }
            int type = p.types[typeIndex];
            int count = Path2D.pointShift[type];
            System.arraycopy(p.points, pointIndex, coords, 0, count);
            if (t != null) {
                t.transform(coords, 0, coords, 0, count / 2);
            }
            pointIndex += count;
            return type;
        }

    }

    public Path2D() {
        this(WIND_NON_ZERO, BUFFER_SIZE);
    }

    public Path2D(int rule) {
        this(rule, BUFFER_SIZE);
    }

    public Path2D(int rule, int initialCapacity) {
        setWindingRule(rule);
        types = new byte[initialCapacity];
        points = new float[initialCapacity * 2];
    }

    public Path2D(Path2D path) {
        this(WIND_NON_ZERO, BUFFER_SIZE);
        PathIterator p = path.iterator(null);
        setWindingRule(p.getWindingRule());
        append(p, false);
    }

    public void setWindingRule(int rule) {
        if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO) {
            throw new NoSuchElementException(invalidWindingRuleValue);
        }
        this.rule = rule;
    }

    public int getWindingRule() {
        return rule;
    }

    /**
     * Checks points and types buffer size to add pointCount points. If necessary realloc buffers to enlarge size.   
     * @param pointCount - the point count to be added in buffer
     */
    void checkBuf(int pointCount, boolean checkMove) {
        if (checkMove && typeSize == 0) {
            throw new IllegalPathStateException("First segment should be SEG_MOVETO type");
        }
        if (typeSize == types.length) {
            byte tmp[] = new byte[typeSize + BUFFER_CAPACITY];
            System.arraycopy(types, 0, tmp, 0, typeSize);
            types = tmp;
        }
        if (pointSize + pointCount > points.length) {
            float tmp[] = new float[pointSize + Math.max(BUFFER_CAPACITY * 2, pointCount)];
            System.arraycopy(points, 0, tmp, 0, pointSize);
            points = tmp;
        }
    }

    public void moveTo(float x, float y) {
        if (typeSize > 0 && types[typeSize - 1] == PathIterator.SEG_MOVETO) {
            points[pointSize - 2] = x;
            points[pointSize - 1] = y;
        } else {
            checkBuf(2, false);
            types[typeSize++] = PathIterator.SEG_MOVETO;
            points[pointSize++] = x;
            points[pointSize++] = y;
        }
    }

    public void lineTo(float x, float y) {
        checkBuf(2, true);
        types[typeSize++] = PathIterator.SEG_LINETO;
        points[pointSize++] = x;
        points[pointSize++] = y;
    }

    public void quadTo(float x1, float y1, float x2, float y2) {
        checkBuf(4, true);
        types[typeSize++] = PathIterator.SEG_QUADTO;
        points[pointSize++] = x1;
        points[pointSize++] = y1;
        points[pointSize++] = x2;
        points[pointSize++] = y2;
    }

    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        checkBuf(6, true);
        types[typeSize++] = PathIterator.SEG_CUBICTO;
        points[pointSize++] = x1;
        points[pointSize++] = y1;
        points[pointSize++] = x2;
        points[pointSize++] = y2;
        points[pointSize++] = x3;
        points[pointSize++] = y3;
    }

    final public int size() {
        return typeSize;
    }
    
    final public boolean isClosed() {
        return typeSize > 0 && types[typeSize - 1] == PathIterator.SEG_CLOSE ;
    }
    
    public void closePath() {
        if (!isClosed()) {
            checkBuf(0, true);
            types[typeSize++] = PathIterator.SEG_CLOSE;
        }
    }
    
    public String toString() {
        return "[size "+size()+", closed "+isClosed()+"]";
    }

    public void append(Path2D path, boolean connect) {
        PathIterator p = path.iterator(null);
        append(p, connect);
    }

    public void append(PathIterator path, boolean connect) {
        while (!path.isDone()) {
            final float coords[] = new float[6];
            final int segmentType = path.currentSegment(coords);
            switch (segmentType) {
                case PathIterator.SEG_MOVETO:
                    if (!connect || typeSize == 0) {
                        moveTo(coords[0], coords[1]);
                        break;
                    }
                    if (types[typeSize - 1] != PathIterator.SEG_CLOSE &&
                        points[pointSize - 2] == coords[0] &&
                        points[pointSize - 1] == coords[1])
                    {
                        break;
                    }
                // NO BREAK;
                case PathIterator.SEG_LINETO:
                    lineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    quadTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    closePath();
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled Segment Type: "+segmentType);                
            }
            path.next();
            connect = false;
        }
    }

    public SVertex getCurrentPoint() {
        if (typeSize == 0) {
            return null;
        }
        int j = pointSize - 2;
        if (types[typeSize - 1] == PathIterator.SEG_CLOSE) {

            for (int i = typeSize - 2; i > 0; i--) {
                int type = types[i];
                if (type == PathIterator.SEG_MOVETO) {
                    break;
                }
                j -= pointShift[type];
            }
        }
        return new SVertex(points[j], points[j + 1], 0f, true);
    }

    public void reset() {
        typeSize = 0;
        pointSize = 0;
    }

    public void transform(AffineTransform t) {
        t.transform(points, 0, points, 0, pointSize / 2);
    }

    public Path2D createTransformedShape(AffineTransform t) {
        Path2D p = (Path2D)clone();
        if (t != null) {
            p.transform(t);
        }
        return p;
    }

    public final synchronized AABBox getBounds2D() {
        float rx1, ry1, rx2, ry2;
        if (pointSize == 0) {
            rx1 = ry1 = rx2 = ry2 = 0.0f;
        } else {
            int i = pointSize - 1;
            ry1 = ry2 = points[i--];
            rx1 = rx2 = points[i--];
            while (i > 0) {
                float y = points[i--];
                float x = points[i--];
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
    boolean isInside(int cross) {
        if (rule == WIND_NON_ZERO) {
            return Crossing.isInsideNonZero(cross);
        }
        return Crossing.isInsideEvenOdd(cross);
    }

    public boolean contains(float px, float py) {
        return isInside(Crossing.crossShape(this, px, py));
    }

    public boolean contains(float rx, float ry, float rw, float rh) {
        int cross = Crossing.intersectShape(this, rx, ry, rw, rh);
        return cross != Crossing.CROSSING && isInside(cross);
    }

    public boolean intersects(float rx, float ry, float rw, float rh) {
        int cross = Crossing.intersectShape(this, rx, ry, rw, rh);
        return cross == Crossing.CROSSING || isInside(cross);
    }

    public boolean contains(Vertex p) {
        return contains(p.getX(), p.getY());
    }

    public boolean contains(AABBox r) {
        return contains(r);
    }

    public boolean intersects(AABBox r) {
        return intersects(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
    }

    public PathIterator iterator() {
        return new Iterator(this);        
    }
       
    public PathIterator iterator(AffineTransform t) {
        return new Iterator(this, t);
    }

    /* public PathIterator getPathIterator(AffineTransform t, float flatness) {
        return new FlatteningPathIterator(getPathIterator(t), flatness);
    } */

    @Override
    public Object clone() {
        try {
            Path2D p = (Path2D) super.clone();
            p.types = types.clone();
            p.points = points.clone();
            return p;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}

