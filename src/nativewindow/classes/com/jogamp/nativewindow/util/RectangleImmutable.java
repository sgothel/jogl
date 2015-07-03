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

package com.jogamp.nativewindow.util;

import com.jogamp.common.type.WriteCloneable;

/** Immutable Rectangle interface */
public interface RectangleImmutable extends WriteCloneable, Comparable<RectangleImmutable> {

    int getHeight();

    int getWidth();

    int getX();

    int getY();

    /** Returns the union of this rectangle and the given rectangle. */
    RectangleImmutable union(final RectangleImmutable r);
    /** Returns the union of this rectangleand the given coordinates. */
    RectangleImmutable union(final int rx1, final int ry1, final int rx2, final int ry2);
    /** Returns the intersection of this rectangleand the given rectangle. */
    RectangleImmutable intersection(RectangleImmutable r);
    /** Returns the intersection of this rectangleand the given coordinates. */
    RectangleImmutable intersection(final int rx1, final int ry1, final int rx2, final int ry2);
    /**
     * Returns the coverage of given rectangle w/ this this one, i.e. between <code>0.0</code> and <code>1.0</code>.
     * <p>
     * Coverage is computed by:
     * <pre>
     *    isect = this.intersection(r);
     *    coverage = area( isect ) / area( this ) ;
     * </pre>
     * </p>
     */
    float coverage(RectangleImmutable r);

    /**
     * <p>
     * Compares square of size 1st, if equal the square of position.
     * </p>
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final RectangleImmutable d);

    /**
     * Checks whether two rect objects are equal. Two instances
     * of <code>Rectangle</code> are equal if the four integer values
     * of the fields <code>y</code>, <code>x</code>,
     * <code>height</code>, and <code>width</code> are all equal.
     * @return      <code>true</code> if the two rectangles are equal;
     * otherwise <code>false</code>.
     */
    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

}
