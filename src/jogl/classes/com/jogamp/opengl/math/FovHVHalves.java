/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.math;

/**
 * Horizontal and vertical field of view (FOV) halves,
 * allowing a non-centered projection.
 * <p>
 * The values might be either in tangent or radians.
 * </p>
 */
public final class FovHVHalves {
    /** Half horizontal FOV from center to left. */
    public final float left;
    /** Half horizontal FOV from center to right. */
    public final float right;
    /** Half vertical FOV from center to top. */
    public final float top;
    /** Half vertical FOV from center to bottom. */
    public final float bottom;
    /** If true, values are in tangent, otherwise radians.*/
    public final boolean inTangents;

    /**
     * Constructor for one {@link FovHVHalves} instance.
     *
     * @param left half horizontal FOV, left side, in tangent or radians
     * @param right half horizontal FOV, right side, in tangent or radians
     * @param top half vertical FOV, top side, in tangent or radians
     * @param bottom half vertical FOV, bottom side, in tangent or radians
     * @param inTangents if true, values are in tangent, otherwise radians
     */
    public FovHVHalves(final float left, final float right, final float top, final float bottom, final boolean inTangents) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
        this.inTangents = inTangents;
    }

    /**
     * Returns a symmetrical centered {@link FovHVHalves} instance in tangents, using:
     * <pre>
        final float halfHorizFovTan = (float)Math.tan(horizontalFov/2f);
        final float halfVertFovTan = (float)Math.tan(verticalFov/2f);
     * </pre>
     * @param horizontalFov whole horizontal FOV in radians
     * @param verticalFov whole vertical FOV in radians
     */
    public static FovHVHalves createByRadians(final float horizontalFov, final float verticalFov) {
        final float halfHorizFovTan = (float)Math.tan(horizontalFov/2f);
        final float halfVertFovTan = (float)Math.tan(verticalFov/2f);
        return new FovHVHalves(halfHorizFovTan, halfHorizFovTan, halfVertFovTan, halfVertFovTan, true);
    }

    /** Returns the full horizontal FOV, i.e. {@link #left} + {@link #right}. */
    public final float horzFov() { return left+right; }

    /** Returns the full vertical FOV, i.e. {@link #top} + {@link #bottom}. */
    public final float vertFov() { return top+bottom; }

    public final String toString() {
        return "FovHVHalves["+(inTangents?"tangents":"radians")+": "+left+" l, "+right+" r, "+top+" t, "+bottom+" b]";
    }
}
