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
    /** Half horizontal FOV from center to left, either in {@link #inTangents} or radians. */
    public final float left;
    /** Half horizontal FOV from center to right, either in {@link #inTangents} or radians. */
    public final float right;
    /** Half vertical FOV from center to top, either in {@link #inTangents} or radians. */
    public final float top;
    /** Half vertical FOV from center to bottom, either in {@link #inTangents} or radians. */
    public final float bottom;
    /** If true, values are in tangent, otherwise radians.*/
    public final boolean inTangents;

    /**
     * Constructor for one {@link FovHVHalves} instance.
     * <p>
     * It is recommended to pass and store values in tangent
     * if used for perspective FOV calculations, since it will avoid conversion to tangent later on.
     * </p>
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
     * Returns a symmetrical centered {@link FovHVHalves} instance in {@link #inTangents}, using:
     * <pre>
        halfHorizFovTan = tan( horizontalFov / 2f );
        halfVertFovTan  = tan( verticalFov / 2f );
     * </pre>
     * @param horizontalFov whole horizontal FOV in radians
     * @param verticalFov whole vertical FOV in radians
     */
    public static FovHVHalves byRadians(final float horizontalFov, final float verticalFov) {
        final float halfHorizFovTan = FloatUtil.tan(horizontalFov/2f);
        final float halfVertFovTan = FloatUtil.tan(verticalFov/2f);
        return new FovHVHalves(halfHorizFovTan, halfHorizFovTan, halfVertFovTan, halfVertFovTan, true);
    }

    /**
     * Returns a symmetrical centered {@link FovHVHalves} instance in {@link #inTangents}, using:
     * <pre>
        top  = bottom = tan( verticalFov / 2f );
        left =  right = aspect * top;
     * </pre>
     *
     * @param verticalFov vertical FOV in radians
     * @param aspect aspect ration width / height
     */
    public static FovHVHalves byFovyRadianAndAspect(final float verticalFov, final float aspect) {
        final float halfVertFovTan = FloatUtil.tan(verticalFov/2f);
        final float halfHorizFovTan = aspect * halfVertFovTan;
        return new FovHVHalves(halfHorizFovTan, halfHorizFovTan,
                               halfVertFovTan, halfVertFovTan, true);
    }

    /**
     * Returns a custom symmetry {@link FovHVHalves} instance {@link #inTangents}, using:
     * <pre>
        left   = tan( horizontalFov * horizCenterFromLeft )
        right  = tan( horizontalFov * ( 1f - horizCenterFromLeft ) )
        top    = tan( verticalFov   * vertCenterFromTop )
        bottom = tan( verticalFov   * (1f - vertCenterFromTop ) )
     * </pre>
     * @param horizontalFov whole horizontal FOV in radians
     * @param horizCenterFromLeft horizontal center from left in [0..1]
     * @param verticalFov whole vertical FOV in radians
     * @param vertCenterFromTop vertical center from top in [0..1]
     */
    public static FovHVHalves byRadians(final float horizontalFov, final float horizCenterFromLeft,
                                        final float verticalFov, final float vertCenterFromTop) {
        return new FovHVHalves(FloatUtil.tan(horizontalFov * horizCenterFromLeft),
                               FloatUtil.tan(horizontalFov * ( 1f - horizCenterFromLeft )),
                               FloatUtil.tan(verticalFov   * vertCenterFromTop),
                               FloatUtil.tan(verticalFov   * (1f - vertCenterFromTop )),
                               true);
    }

    /**
     * Returns a custom symmetry {@link FovHVHalves} instance {@link #inTangents},
     * via computing the <code>horizontalFov</code> using:
     * <pre>
        halfVertFovTan  = tan( verticalFov / 2f );
        halfHorizFovTan = aspect * halfVertFovTan;
        horizontalFov   = atan( halfHorizFovTan ) * 2f;
        return {@link #byRadians(float, float, float, float) byRadians}(horizontalFov, horizCenterFromLeft, verticalFov, vertCenterFromTop)
     * </pre>
     * @param verticalFov whole vertical FOV in radians
     * @param vertCenterFromTop vertical center from top in [0..1]
     * @param aspect aspect ration width / height
     * @param horizCenterFromLeft horizontal center from left in [0..1]
     */
    public static FovHVHalves byFovyRadianAndAspect(final float verticalFov, final float vertCenterFromTop,
                                                    final float aspect, final float horizCenterFromLeft) {
        final float halfVertFovTan = FloatUtil.tan(verticalFov/2f);
        final float halfHorizFovTan = aspect * halfVertFovTan;
        final float horizontalFov = FloatUtil.atan(halfHorizFovTan) * 2f;
        return byRadians(horizontalFov, horizCenterFromLeft, verticalFov, vertCenterFromTop);
    }

    /**
     * Returns this instance <i>in tangent</i> values.
     * <p>
     * If this instance is {@link #inTangents} already, method returns this instance,
     * otherwise a newly created instance w/ converted values to tangent.
     * </p>
     */
    public final FovHVHalves toTangents() {
        if( inTangents ) {
            return this;
        } else {
            return new FovHVHalves(FloatUtil.tan(left), FloatUtil.tan(right), FloatUtil.tan(top), FloatUtil.tan(bottom), true);
        }
    }

    /** Returns the full horizontal FOV, i.e. {@link #left} + {@link #right}, either in {@link #inTangents} or radians. */
    public final float horzFov() { return left+right; }

    /** Returns the full vertical FOV, i.e. {@link #top} + {@link #bottom}, either in {@link #inTangents} or radians. */
    public final float vertFov() { return top+bottom; }

    public final String toString() {
        return "FovHVH["+(inTangents?"tangents":"radians")+": "+left+" l, "+right+" r, "+top+" t, "+bottom+" b]";
    }
    public final String toStringInDegrees() {
        final float f = 180.0f / FloatUtil.PI;
        final String storedAs = inTangents?"tangents":"radians";
        if( inTangents ) {
            final float aleft = FloatUtil.atan(left);
            final float aright = FloatUtil.atan(right);
            final float atop = FloatUtil.atan(top);
            final float abottom = FloatUtil.atan(bottom);
            return "FovHVH[degrees: "+aleft*f+" l, "+aright*f+" r, "+atop*f+" t, "+abottom*f+" b, stored-as: "+storedAs+"]";
        } else {
            return "FovHVH[degrees: "+left*f+" l, "+right*f+" r, "+top*f+" t, "+bottom*f+" b, stored-as: "+storedAs+"]";
        }
    }
}
