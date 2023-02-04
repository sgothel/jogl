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

public interface PathIterator {

    public static final int WIND_EVEN_ODD = 0;
    public static final int WIND_NON_ZERO = 1;

    public static final int SEG_MOVETO  = 0;
    public static final int SEG_LINETO  = 1;
    public static final int SEG_QUADTO  = 2;
    public static final int SEG_CUBICTO = 3;
    public static final int SEG_CLOSE   = 4;

    int getWindingRule();

    /** Return the current {@link #points()} index for the current segment. */
    int index();

    /** Returns reference of the point array for the whole Path2D */
    float[] points();

    /** Return current segment type */
    int getType(final int idx);

    /** Returns true if completed */
    boolean isDone();

    void next();

    /**
     * Return the path segment type and copies the current segment's points to given storage
     * @param coords storage for current segment's points
     * @return segment type
     * @see #points()
     * @see #index()
     * @see #getType(int)
     */
    int currentSegment(float[] coords);
}

