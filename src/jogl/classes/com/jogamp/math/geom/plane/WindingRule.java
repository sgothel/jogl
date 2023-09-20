package com.jogamp.math.geom.plane;

/**
 * Winding rule, either EVEN_ODD or NON_ZERO (like for TrueType fonts).
 */
public enum WindingRule {
    /**
     * The even-odd rule specifies that a point lies inside the path
     * if a ray drawn in any direction from that point to infinity is crossed by path segments
     * an odd number of times.
     */
    EVEN_ODD(0),

    /**
     * The non-zero rule specifies that a point lies inside the path
     * if a ray drawn in any direction from that point to infinity is crossed by path segments
     * a different number of times in the counter-clockwise direction than the clockwise direction.
     *
     * Non-zero winding rule is used by TrueType fonts.
     */
    NON_ZERO(1);

    public final int value;

    WindingRule(final int v) {
        this.value = v;
    }
}
