/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

package com.jogamp.graph.curve.tess;

import java.util.List;

import com.jogamp.graph.geom.Outline;
import com.jogamp.graph.geom.Triangle;

/** Interface to the triangulation algorithms provided
 *  A triangulation of 2D outlines where you can
 *  provides an easy one or more outlines to be triangulated
 *
 *  example usage:
 *      addCurve(o1);
 *      addCurve(o2);
 *      addCurve(o3);
 *      generate();
 *      reset();
 *
 * @see Outline
 * @see Triangulation
 */
public interface Triangulator {

    /**
     * Add a curve to the list of Outlines
     * describing the shape
     * @param sink list where the generated triangles will be added
     * @param outline a bounding {@link Outline}
     * @param sharpness TODO
     */
    public void addCurve(List<Triangle> sink, Outline outline, float sharpness);

    /** Generate the triangulation of the provided
     *  List of {@link Outline}s
     * @param sink list where the generated triangles will be added
     */
    public void generate(List<Triangle> sink);

    /**
     * Reset the triangulation to initial state
     * Clearing cached data
     */
    public void reset();

    /**
     * Return the number of newly added vertices during {@link #addCurve(List, Outline, float)}.
     */
    public int getAddedVerticeCount();
}
